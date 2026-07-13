package com.upc.computer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.config.QwenProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通义千问大模型调用（DashScope OpenAI 兼容接口）。
 */
@Service
public class QwenAiService {

    private static final Logger log = LoggerFactory.getLogger(QwenAiService.class);

    @Autowired
    private QwenProperties qwenProperties;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 调用千问生成文本，失败时返回 null（由调用方降级为模板）。
     */
    public String chat(String systemPrompt, String userPrompt) {
        if (!qwenProperties.isConfigured()) {
            log.debug("Qwen API 未配置，跳过 AI 调用");
            return null;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", qwenProperties.getModel());
            body.put("temperature", qwenProperties.getTemperature());
            body.put("max_tokens", qwenProperties.getMaxTokens());
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(qwenProperties.getApiUrl()))
                    .timeout(Duration.ofMillis(qwenProperties.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + qwenProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(qwenProperties.getTimeoutMs()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Qwen API 调用失败，status={}, body={}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                log.warn("Qwen API 返回内容为空：{}", response.body());
                return null;
            }
            return content.asText().trim();
        } catch (Exception e) {
            log.warn("Qwen API 调用异常：{}", e.getMessage());
            return null;
        }
    }
}
