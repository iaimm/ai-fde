package com.example.demo.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.demo.tools.NoteRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@RestController
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final Counter questionCounter;
    private final Timer ttftTimer;

    private final NoteRetriever retriever;

    public ChatController(NoteRetriever retriever, MeterRegistry registry) {
        this.retriever = retriever;
        this.questionCounter = registry.counter("notesgpt.questions.total");
        this.ttftTimer = registry.timer("notesgpt.ttft");
    }

    public record ChatRequest(String question) {}   // Java 21 record：一行顶一个类

    private static final String API_URL = "https://api.moonshot.cn/v1/chat/completions";
    private static final String API_KEY = System.getenv("MOONSHOT_API_KEY");  // 从环境变量读，不写死
    private static final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    @PostMapping("/api/chat")
    public String chat(@RequestBody ChatRequest req) throws Exception {
        // 用 Jackson 安全拼 JSON（防止问题里带引号把 JSON 弄炸）
        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", "kimi-k2.6");
        ArrayNode messages = payload.putArray("messages");
        messages.addObject().put("role", "user").put("content", req.question());
        payload.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            return "调用失败 " + resp.statusCode() + ": " + resp.body();
        }
        // 从返回 JSON 里抠出回答文本
        return mapper.readTree(resp.body())
                .path("choices").get(0)
                .path("message").path("content")
                .asText();
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.simple(1, Duration.ofMinutes(1)))
                .build());
    }


    @GetMapping(path = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public  ResponseEntity<?> stream(@RequestParam String question, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);      // 5分钟超时，长回答够用
        if (question.equals("xss-test")) {
            try {
                emitter.send(mapper.writeValueAsString("<script>alert(1)</script>"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return ResponseEntity.ok(emitter);
        }
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)          // ← 别忘了，Day 1 的坑
                    .body(Map.of("error", "问题不能为空"));
        }
        if (question.length() > 500) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "问题过长（最多500字）"));
        }
        Bucket bucket = resolveBucket(request.getRemoteAddr());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(429).contentType(MediaType.APPLICATION_JSON) 
                    .body(Map.of("error", "请求过于频繁，请稍后再试"));
        }
       
        Thread.startVirtualThread(() -> forwardToLlm(question, emitter));
        return ResponseEntity.ok(emitter);
    }

    private void forwardToLlm(String question, SseEmitter emitter) {
        MDC.put("traceId", UUID.randomUUID().toString().substring(0, 8)); 
        long totalStart = System.currentTimeMillis();          // 总耗时起点
        try {
            questionCounter.increment();  
             // ===== 计时 1：检索 =====
            long searchStart = System.currentTimeMillis();
            var hits = retriever.top3(question);
            long searchMs = System.currentTimeMillis() - searchStart;
            String ctx = java.util.stream.IntStream.range(0, hits.size())
                .mapToObj(i -> "[" + (i + 1) + "] " + hits.get(i).src() + ": " + hits.get(i).text())
                .collect(java.util.stream.Collectors.joining("\n---\n"));

            String prompt = "基于以下笔记回答问题，回答中用[编号]标注来源：\n" + ctx + "\n\n问题：" + question;
            ObjectNode payload = mapper.createObjectNode();
            payload.put("model", "kimi-k2.6");
            ArrayNode messages = payload.putArray("messages");
            messages.addObject().put("role", "user").put("content", prompt);
            payload.put("stream", true);                       // ← 关键：要流式

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .version(HttpClient.Version.HTTP_1_1)      // ← SSE 走 HTTP/1.1，避开 h2 兼容坑
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();
              // ===== 计时 1：检索 =====
            long requestStart = System.currentTimeMillis();

            HttpResponse<java.io.InputStream> resp = client.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            if (resp.statusCode() != 200) {
                emitter.send("调用失败 " + resp.statusCode());
                emitter.complete();
                return;
            }
            // ===== 计时 2：TTFT =====
            long requestTime = System.currentTimeMillis() - requestStart;
            log.info("[PERF] MODEL REQUEST=" + requestTime + "ms");
            boolean firstToken = true;                          // 首 token 旗标
            // 逐行读 kimi 的 SSE 流，剥 data: 前缀，转给前端
            long readStart = System.currentTimeMillis();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resp.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;   // 跳过空行和别的字段
                    String data = line.substring(5).trim();
                    if (data.equals("[DONE]")) break;          // ← 结束帧，W1 的老朋友
                    String delta = mapper.readTree(data)
                            .path("choices").get(0)
                            .path("delta").path("content").asText("");
                    if (!delta.isEmpty()) {
                        if (firstToken) {
                            // ===== 计时 2：TTFT =====
                            long ttftMs = System.currentTimeMillis() - totalStart;
                            log.info("[PERF] TTFT=" + ttftMs + "ms");
                            firstToken = false;
                        }
                        emitter.send(mapper.writeValueAsString(delta));                   // 收到一个字就发一个字
                    }
                }
            }
            long readTotal = System.currentTimeMillis() - readStart;
            log.info("[PERF] READER REQUEST=" + readTotal + "ms");
            ObjectNode refsPayload = mapper.createObjectNode();
            ArrayNode refsArr = refsPayload.putArray("refs");
            for (var h : hits) {
                ObjectNode r = refsArr.addObject();
                r.put("src", h.src());
                r.put("text", h.text().substring(0, Math.min(200, h.text().length())));  // 截断防过长
            }
            emitter.send(SseEmitter.event()
                    .name("refs")
                    .data(mapper.writeValueAsString(refsPayload)));
            emitter.complete();                                // 流正常结束，关门
            long totalMs = System.currentTimeMillis() - totalStart;   // ===== 计时 3：总耗时 =====
            log.info("[PERF] 检索={}ms | TTFT={}ms | 总耗时={}ms", searchMs, totalMs, totalMs);
        } catch (Exception e) {
            emitter.completeWithError(e);                      // 异常也要关门
        } finally {
             MDC.clear();
        }
    }
}