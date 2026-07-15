package com.upc.computer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.common.BusinessException;
import com.upc.computer.config.AiClient;
import com.upc.computer.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单 AI 识图：微信聊天截图 → 结构化订单字段；订单附件 OCR 演示比对。
 */
@Service
public class OrderOcrService {

    private static final Logger log = LoggerFactory.getLogger(OrderOcrService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> ALLOWED_EXTS = Arrays.asList("jpg", "jpeg", "png");

    private static final String WECHAT_SCREENSHOT_PROMPT =
            "你是客户订单录入助手。用户上传的是微信聊天截图（客户下单、询价、确认数量的对话）。" +
            "请从截图中识别并提取订单信息，严格返回 JSON，不要 Markdown，不要解释。\n" +
            "字段：customerName（客户/公司名称）、productModel（产品型号或名称，如27寸4K显示器）、" +
            "quantity（订购数量，整数）、deliveryDate（要求交期 yyyy-MM-dd，未提及留空字符串）、" +
            "amount（订单总金额数字，未提及填0）、contactPhone（联系人电话，未提及留空）、" +
            "remark（备注，可含聊天中的特殊要求）、rawText（截图关键原文摘要，150字内）、confidence（0到1）。\n" +
            "只返回：{\"customerName\":\"\",\"productModel\":\"\",\"quantity\":0,\"deliveryDate\":\"\"," +
            "\"amount\":0,\"contactPhone\":\"\",\"remark\":\"\",\"rawText\":\"\",\"confidence\":0.85}";

    @Autowired
    private AiProperties aiProperties;
    @Autowired
    private AiClient aiClient;

    private static final Map<String, Map<String, Object>> MOCK_BY_FILE = Map.of(
            "CO202607004_contract.svg", mockFields("广州教育设备有限公司", "15.6寸商用显示器", 100,
                    "2026-08-10", 68000.0, "教育行业采购合同"),
            "CO202607002_po.svg", mockFields("北京星辰电竞俱乐部", "23.8寸电竞显示器", 150,
                    "2026-07-25", 192000.0, "电竞显示器采购单"),
            "CO202607003_spec.svg", mockFields("上海视觉设计工作室", "27寸4K显示器", 40,
                    "2026-08-05", 84000.0, "4K显示器技术规格书")
    );

    /** 按系统附件文件名识别（演示数据，兼容旧流程） */
    public Map<String, Object> recognize(String fileName, String orderId) {
        String key = fileName != null ? fileName.trim() : "";
        Map<String, Object> fields = MOCK_BY_FILE.getOrDefault(key, defaultMock(orderId));
        return wrapResult(fileName, orderId, fields, "mock-ocr-v1", 0.92);
    }

    /** 上传微信截图 → AI 视觉识别订单字段 */
    public Map<String, Object> recognizeUpload(MultipartFile file, String orderId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传图片");
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.') + 1) : "";
        if (!ALLOWED_EXTS.contains(ext)) {
            throw new BusinessException("仅支持 JPG、PNG 图片");
        }

        if (aiProperties.isMock()) {
            Map<String, Object> fields = mockFields(
                    "深圳华创科技有限公司", "27寸4K显示器", 200,
                    "2026-09-15", 136000.0,
                    "【Mock】微信对话：张经理您好，我们需要200台27寸4K显示器，9月中旬交货，请帮忙下单");
            fields.put("contactPhone", "13800138000");
            fields.put("remark", "微信客户张总确认，加急");
            return wrapResult(originalName, orderId, fields, "mock-wechat-vision", 0.91);
        }

        validateAiConfig();
        try {
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.contains("jpeg") && !contentType.contains("png"))) {
                contentType = "png".equals(ext) ? "image/png" : "image/jpeg";
            }
            String aiReply = aiClient.callVision(bytes, contentType, WECHAT_SCREENSHOT_PROMPT);
            ParsedOrderFields parsed = parseAiReply(aiReply);
            Map<String, Object> fields = parsed.toMap();
            return wrapResult(originalName, orderId, fields, "ai-wechat-vision", parsed.confidence);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Order AI OCR failed. orderId={}", orderId, e);
            throw new BusinessException("AI 识别失败：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private void validateAiConfig() {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BusinessException("AI_API_KEY 未配置，请在 application.yaml 的 ai.api-key 填写密钥");
        }
        if (aiProperties.getBaseUrl() == null || aiProperties.getBaseUrl().isBlank()) {
            throw new BusinessException("AI_BASE_URL 未配置");
        }
        if (aiProperties.getModel() == null || aiProperties.getModel().isBlank()) {
            throw new BusinessException("AI_MODEL 未配置");
        }
    }

    private ParsedOrderFields parseAiReply(String aiReply) {
        String jsonStr = aiClient.extractJson(aiReply);
        if (jsonStr == null) {
            throw new BusinessException("AI 返回无法解析为 JSON：" + truncate(aiReply, 200));
        }
        try {
            JsonNode root = MAPPER.readTree(jsonStr);
            ParsedOrderFields p = new ParsedOrderFields();
            p.customerName = root.path("customerName").asText("");
            p.productModel = root.path("productModel").asText("");
            p.quantity = root.path("quantity").isNumber() ? root.path("quantity").asInt(0) : 0;
            p.deliveryDate = root.path("deliveryDate").asText("");
            p.amount = root.path("amount").isNumber() ? root.path("amount").asDouble(0) : 0;
            p.contactPhone = root.path("contactPhone").asText("");
            p.remark = root.path("remark").asText("");
            p.rawText = root.path("rawText").asText(aiReply);
            p.confidence = root.path("confidence").isNumber() ? root.path("confidence").asDouble(0.85) : 0.85;
            return p;
        } catch (Exception e) {
            throw new BusinessException("JSON 解析失败：" + e.getMessage());
        }
    }

    private Map<String, Object> wrapResult(String fileName, String orderId, Map<String, Object> fields,
                                           String engine, double confidence) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", fileName);
        result.put("orderId", orderId);
        result.put("engine", engine);
        result.put("confidence", confidence);
        result.put("fields", fields);
        result.put("rawText", fields.getOrDefault("rawText", ""));
        return result;
    }

    private static Map<String, Object> defaultMock(String orderId) {
        Map<String, Object> m = mockFields("", "", 0, "", 0.0, "模拟识别文本");
        m.put("orderIdHint", orderId);
        return m;
    }

    private static Map<String, Object> mockFields(String customer, String product, int qty,
                                                  String delivery, double amount, String raw) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customerName", customer);
        m.put("productModel", product);
        m.put("quantity", qty);
        m.put("deliveryDate", delivery);
        m.put("amount", amount);
        m.put("rawText", raw);
        return m;
    }

    public List<Map<String, Object>> defaultAttachmentsForOrder(String orderNo) {
        return switch (orderNo != null ? orderNo : "") {
            case "CO202607004" -> List.of(attachment("contract", "合同扫描件", "CO202607004_contract.svg", "image/svg+xml"));
            case "CO202607002" -> List.of(attachment("purchase", "采购单", "CO202607002_po.svg", "image/svg+xml"));
            case "CO202607003" -> List.of(
                    attachment("spec", "规格说明书", "CO202607003_spec.svg", "image/svg+xml"),
                    attachment("contract", "框架协议", "CO202607003_spec.svg", "image/svg+xml"));
            default -> List.of();
        };
    }

    private static Map<String, Object> attachment(String type, String label, String fileName, String mime) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", fileName);
        a.put("type", type);
        a.put("label", label);
        a.put("fileName", fileName);
        a.put("mimeType", mime);
        a.put("url", "/mock/order/" + fileName);
        return a;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static class ParsedOrderFields {
        String customerName = "";
        String productModel = "";
        int quantity = 0;
        String deliveryDate = "";
        double amount = 0;
        String contactPhone = "";
        String remark = "";
        String rawText = "";
        double confidence = 0.85;

        Map<String, Object> toMap() {
            Map<String, Object> m = mockFields(customerName, productModel, quantity, deliveryDate, amount, rawText);
            m.put("contactPhone", contactPhone);
            m.put("remark", remark);
            return m;
        }
    }
}
