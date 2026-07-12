package com.motiflab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 通过 java.net.http.HttpClient 调用 OpenAI 兼容 Chat Completions API。
 * 不依赖 Spring AI 自动配置；DeepSeek 等只需改 baseUrl。
 * 关联：ProviderService、AnimationGenerator。
 */
@Component
public class LlmClient {

    private final ProviderService providers;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public LlmClient(ProviderService providers) {
        this.providers = providers;
    }

    /** 发送 system + user，返回助手文本；未配置/未启用时抛明确异常 */
    public String complete(String system, String user) {
        ProviderService.ResolvedProvider p = providers.resolveForCall();
        if (p == null || !p.enabled()) {
            throw new IllegalStateException("请先在设置中配置并启用 LLM Provider");
        }
        if (p.apiKey() == null || p.apiKey().isBlank()) {
            throw new IllegalStateException("请先在设置中填写 API Key");
        }
        if (p.baseUrl() == null || p.baseUrl().isBlank()) {
            throw new IllegalStateException("请先在设置中填写 baseUrl");
        }
        if (p.model() == null || p.model().isBlank()) {
            throw new IllegalStateException("请先在设置中填写 model");
        }

        String url = joinChatCompletions(p.baseUrl());
        ObjectNode body = mapper.createObjectNode();
        body.put("model", p.model());
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", system == null ? "" : system);
        messages.addObject().put("role", "user").put("content", user == null ? "" : user);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(3))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + p.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "LLM 调用失败 HTTP " + response.statusCode() + ": " + truncate(response.body()));
            }
            return extractContent(response.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException("LLM 网络错误: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM 调用被中断", e);
        }
    }

    /** baseUrl 去尾斜杠后拼接 /chat/completions */
    private static String joinChatCompletions(String baseUrl) {
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base + "/chat/completions";
    }

    private String extractContent(String json) throws IOException {
        JsonNode root = mapper.readTree(json);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull() || content.asText().isBlank()) {
            throw new IllegalStateException("LLM 返回无内容: " + truncate(json));
        }
        return content.asText();
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 200 ? s : s.substring(0, 200) + "…";
    }
}
