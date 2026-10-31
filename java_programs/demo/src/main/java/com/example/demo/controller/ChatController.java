package com.example.demo.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class ChatController {

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
}