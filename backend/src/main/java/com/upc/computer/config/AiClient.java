package com.upc.computer.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 视觉模型 HTTP 客户端。
 * 使用 JDK 11+ 内置 HttpClient，不引入额外依赖。
 * 支持 base64 图片发送给兼容 OpenAI vision API 的接口。
 */
@Component
public class AiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*}");

    private final AiProperties props;
    private final HttpClient httpClient;

    public AiClient(AiProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * 发送图片（字节数组）和提示词，返回模型的文本回复。
     *
     * @param imageBytes  图片字节
     * @param mimeType    image/jpeg 或 image/png
     * @param systemPrompt 系统提示词
     * @return 模型回复文本
     */
    public String callVision(byte[] imageBytes, String mimeType, String systemPrompt) throws IOException, InterruptedException {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64Image;

        String requestBody = MAPPER.writeValueAsString(new java.util.LinkedHashMap<String, Object>() {{
            put("model", props.getModel());
            put("max_tokens", 4096);
            put("messages", new Object[]{
                new java.util.LinkedHashMap<String, Object>() {{
                    put("role", "user");
                    put("content", new Object[]{
                        new java.util.LinkedHashMap<String, Object>() {{
                            put("type", "text");
                            put("text", systemPrompt);
                        }},
                        new java.util.LinkedHashMap<String, Object>() {{
                            put("type", "image_url");
                            put("image_url", new java.util.LinkedHashMap<String, Object>() {{
                                put("url", dataUrl);
                            }});
                        }}
                    });
                }}
            });
        }});

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + props.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("AI 接口返回异常状态码: " + response.statusCode() + "，body: " + response.body());
        }

        JsonNode root = MAPPER.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IOException("AI 接口返回格式异常，无 choices 字段");
        }
        return choices.get(0).path("message").path("content").asText("");
    }

    /**
     * 从模型返回的文本中提取 JSON 字符串。
     * 优先提取 ```json ... ``` 代码块，其次提取第一个完整 { } 对象。
     */
    public String extractJson(String text) {
        if (text == null || text.isBlank()) return null;

        Matcher blockMatcher = JSON_BLOCK.matcher(text);
        if (blockMatcher.find()) {
            return blockMatcher.group(1).trim();
        }

        Matcher objectMatcher = JSON_OBJECT.matcher(text);
        if (objectMatcher.find()) {
            return objectMatcher.group().trim();
        }

        return null;
    }
}
