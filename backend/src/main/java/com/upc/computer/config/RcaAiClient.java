package com.upc.computer.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Component
public class RcaAiClient {
    private final DeepseekProperties properties;
    private final AiProperties fallbackProperties;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public RcaAiClient(DeepseekProperties properties, AiProperties fallbackProperties, ObjectMapper mapper) {
        this.properties = properties;
        this.fallbackProperties = fallbackProperties;
        this.mapper = mapper;
    }

    public Map<String, Object> generate(Map<String, Object> evidencePayload) throws Exception {
        String system = "你是制造业质量工程师。只能依据用户提供的结构化证据分析，不得虚构数据，不得修改嫌疑分和金额。"
                + "请只返回JSON对象，字段为 conclusion(string)、actions(array，每项包含department,title,priority,reason)、customerReply(string)。"
                + "结论必须说明这是辅助排查，最终根因需人工确认。";
        return callWithFallback(List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", mapper.writeValueAsString(evidencePayload))));
    }

    public Map<String, Object> triage(Map<String, Object> payload) throws Exception {
        String system = "你是显示器制造企业的售后分诊助手。你需要准确理解客户的各种口语化描述，提取故障现象，并区分采购总量与实际故障数量。"
                + "不得因为文本中出现‘采购’就直接判为业务咨询；只要客户描述产品故障，就必须按售后故障理解。"
                + "当文本同时出现多个数量，例如‘采购20台，3台亮点’，采购总量是20，实际故障数量是3，不得混淆。"
                + "qualityRelated表示是否涉及产品本体、制造、物料、质检或产品性能问题；物流破损和单纯询价交期通常为false。"
                + "你只能依据输入内容，不得虚构质检结果、设备报警、批次记录、根因或责任部门。"
                + "请只返回JSON对象，字段必须为："
                + "category(string，只能是 BUSINESS_CONSULT、TECH_SUPPORT、LOGISTICS_DAMAGE、SINGLE_REPAIR、BATCH_QUALITY 之一)，"
                + "categoryName(string)，qualityRelated(boolean)，affectedQuantity(integer，无法确认时为0)，"
                + "understanding(string)，faultPhenomena(array of string)，missingInformation(array of string)，"
                + "followUpQuestions(array of string，最多4项)，customerReply(string)，summary(string)，"
                + "reasons(array，每项包含name、evidence、impact，其中impact为整数)，"
                + "actions(array，每项包含department、title、priority、reason)。";
        return callWithFallback(List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", mapper.writeValueAsString(payload))));
    }

    private Map<String, Object> callWithFallback(List<Map<String, String>> messages) throws Exception {
        Exception primary = new IllegalStateException("未配置 DeepSeek API Key");
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            try {
                return call(properties.getApiUrl(), properties.getApiKey(), properties.getModel(), messages,
                        properties.getTemperature(), properties.getMaxTokens());
            } catch (Exception e) {
                primary = e;
            }
        }
        if (fallbackProperties.getApiKey() == null || fallbackProperties.getApiKey().isBlank()) throw primary;
        try {
            return call(fallbackProperties.getBaseUrl() + "/chat/completions", fallbackProperties.getApiKey(),
                    fallbackProperties.getModel(), messages, 0.2, 2048);
        } catch (Exception fallback) {
            throw new IllegalStateException("DeepSeek: " + primary.getMessage() + "; Qwen: " + fallback.getMessage());
        }
    }

    private Map<String, Object> call(String url, String apiKey, String model, List<Map<String, String>> messages,
                                     double temperature, int maxTokens) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", messages);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + response.statusCode());
        String responseBody = new String(response.body(), StandardCharsets.UTF_8);
        JsonNode content = mapper.readTree(responseBody).path("choices").path(0).path("message").path("content");
        if (!content.isTextual()) throw new IllegalStateException("大模型返回内容为空");
        return mapper.readValue(content.asText(), Map.class);
    }
}
