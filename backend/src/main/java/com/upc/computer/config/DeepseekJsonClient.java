package com.upc.computer.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.common.BusinessException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 文本 JSON 客户端。
 *
 * <p>只读取 deepseek-finance.* 独立配置，不会回退到原有 deepseek.*、
 * 视觉模型或其他供应商的 API Key，用于财务和管理员全局结构化分析。</p>
 */
@Component
public class DeepseekJsonClient {

    private final FinanceDeepseekProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public DeepseekJsonClient(FinanceDeepseekProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateJson(String systemPrompt, Object evidencePayload) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException(503, "AI 分析未配置 DEEPSEEK_FINANCE_API_KEY");
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.getModel());
            body.put("temperature", properties.getTemperature());
            body.put("max_tokens", properties.getMaxTokens());
            body.put("response_format", Map.of("type", "json_object"));
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", mapper.writeValueAsString(evidencePayload))
            ));

            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getApiUrl()))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2) {
                throw new BusinessException(503, "DeepSeek 服务调用失败（HTTP " + response.statusCode() + "）");
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new BusinessException(503, "DeepSeek 返回的分析内容为空");
            }
            Object value = mapper.readValue(extractJson(content.asText()), Object.class);
            if (!(value instanceof Map<?, ?> map)) {
                throw new BusinessException(503, "DeepSeek 返回的分析格式不正确");
            }
            return (Map<String, Object>) map;
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(503, "AI 分析请求已中断，请稍后重试");
        } catch (Exception e) {
            throw new BusinessException(503, "AI 分析服务暂不可用：" + safeMessage(e));
        }
    }

    private String extractJson(String text) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```")) {
            int firstLine = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                value = value.substring(firstLine + 1, lastFence).trim();
            }
        }
        return value;
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) return "未知错误";
        return message.length() > 160 ? message.substring(0, 160) : message;
    }
}
