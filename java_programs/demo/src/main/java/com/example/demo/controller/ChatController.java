package com.example.demo.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.http.MediaType;
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

@RestController
public class ChatController {

    private final NoteRetriever retriever;

    public ChatController(NoteRetriever retriever) {
        this.retriever = retriever;
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

    @GetMapping(path = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String question) {
        SseEmitter emitter = new SseEmitter(300_000L);      // 5分钟超时，长回答够用
        Thread.startVirtualThread(() -> forwardToLlm(question, emitter));
        return emitter;
    }

    private void forwardToLlm(String question, SseEmitter emitter) {
        try {
            var hits = retriever.top3(question);
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

            HttpResponse<java.io.InputStream> resp = client.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            if (resp.statusCode() != 200) {
                emitter.send("调用失败 " + resp.statusCode());
                emitter.complete();
                return;
            }

            // 逐行读 kimi 的 SSE 流，剥 data: 前缀，转给前端
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
                        emitter.send(mapper.writeValueAsString(delta));                   // 收到一个字就发一个字
                    }
                }
            }
            emitter.complete();                                // 流正常结束，关门
        } catch (Exception e) {
            emitter.completeWithError(e);                      // 异常也要关门
        }
    }
}