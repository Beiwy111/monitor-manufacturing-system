package com.upc.computer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.common.BusinessException;
import com.upc.computer.config.AiClient;
import com.upc.computer.config.AiProperties;
import com.upc.computer.dto.*;
import com.upc.computer.entity.PurchaseOrder;
import com.upc.computer.entity.PurchaseOrderItem;
import com.upc.computer.mapper.PurchaseOrderItemMapper;
import com.upc.computer.mapper.PurchaseOrderMapper;
import com.upc.computer.service.AiDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AiDocumentServiceImpl implements AiDocumentService {

    private static final Logger log = LoggerFactory.getLogger(AiDocumentServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String PROMPT =
        "你是采购单据结构化解析助手。请从上传的采购单据图片中提取采购相关字段，并严格返回 JSON，不要返回 Markdown，不要解释。\n" +
        "需要提取：supplierName（供应商名称）、paymentMethod（付款方式）、items（物料行数组）。\n" +
        "每条物料行字段：materialCode（物料编码，无法识别则为空字符串）、materialName（物料名称）、" +
        "specification（规格型号）、unitPrice（单价，数字，无法识别则为null）、quantity（数量，数字，无法识别则为null）、" +
        "deliveryDate（交期，格式yyyy-MM-dd，无法识别则为空字符串）、remark（备注）、confidence（置信度0到1）。\n" +
        "只返回如下JSON：{\"supplierName\":\"\",\"paymentMethod\":\"\",\"items\":[{\"materialCode\":\"\",\"materialName\":\"\"," +
        "\"specification\":\"\",\"unitPrice\":null,\"quantity\":null,\"deliveryDate\":\"\",\"remark\":\"\",\"confidence\":0.8}]}";

    private static final List<String> ALLOWED_EXTS = Arrays.asList("jpg", "jpeg", "png");

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private AiClient aiClient;

    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;

    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    @Override
    public AiDocumentParseResult parseDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传文件");
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.') + 1) : "";
        if (!ALLOWED_EXTS.contains(ext)) {
            throw new BusinessException("当前演示版仅支持图片格式（jpg、jpeg、png），不支持：" + (ext.isEmpty() ? "未知格式" : ext));
        }

        if (aiProperties.isMock()) {
            return buildMockResult();
        }

        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BusinessException("AI_API_KEY 未配置，请在 application.yaml 的 ai.api-key 填写密钥");
        }
        if (aiProperties.getBaseUrl() == null || aiProperties.getBaseUrl().isBlank()) {
            throw new BusinessException("AI_BASE_URL 未配置，请在 application.yaml 的 ai.base-url 填写接口地址");
        }
        if (aiProperties.getModel() == null || aiProperties.getModel().isBlank()) {
            throw new BusinessException("AI_MODEL 未配置，请在 application.yaml 的 ai.model 填写模型名称");
        }

        try {
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.contains("jpeg") && !contentType.contains("png"))) {
                contentType = ext.equals("png") ? "image/png" : "image/jpeg";
            }
            String aiReply = aiClient.callVision(bytes, contentType, PROMPT);
            return parseAiReply(aiReply);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI document parse failed. model={}, baseUrl={}", aiProperties.getModel(), aiProperties.getBaseUrl(), e);
            throw new BusinessException("AI 调用失败：" + resolveAiErrorMessage(e));
        }
    }

    @Override
    @Transactional
    public AiDocumentConfirmResponse confirmDocument(AiDocumentConfirmRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("至少需要一条物料行");
        }
        for (AiDocumentItemDTO item : request.getItems()) {
            if (item.getMaterialName() == null || item.getMaterialName().isBlank()) {
                throw new BusinessException("物料名称不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("物料【" + item.getMaterialName() + "】数量必须大于0");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String orderNo = "AI" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        PurchaseOrder order = new PurchaseOrder();
        order.setPurchaseOrderNo(orderNo);
        order.setSupplierName(request.getSupplierName());
        order.setRemark("付款方式：" + (request.getPaymentMethod() != null ? request.getPaymentMethod() : ""));
        order.setStatus("DRAFT");
        order.setPurchaseDate(LocalDate.now());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        BigDecimal total = BigDecimal.ZERO;
        for (AiDocumentItemDTO item : request.getItems()) {
            if (item.getUnitPrice() != null && item.getQuantity() != null) {
                total = total.add(item.getUnitPrice().multiply(item.getQuantity()));
            }
        }
        order.setTotalAmount(total);
        purchaseOrderMapper.insertPurchaseOrder(order);

        Long orderId = order.getPurchaseOrderId();
        for (AiDocumentItemDTO item : request.getItems()) {
            PurchaseOrderItem poi = new PurchaseOrderItem();
            poi.setPurchaseOrderId(orderId);
            poi.setQuantity(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
            poi.setReceivedQuantity(BigDecimal.ZERO);
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            poi.setUnitPrice(unitPrice);
            poi.setUnit(item.getUnit() != null && !item.getUnit().isBlank() ? item.getUnit() : "件");
            BigDecimal qty = poi.getQuantity();
            poi.setLineAmount(unitPrice.multiply(qty));
            poi.setItemStatus("PENDING");
            poi.setCreatedAt(now);
            poi.setUpdatedAt(now);
            purchaseOrderItemMapper.insertPurchaseOrderItem(poi);
        }

        return new AiDocumentConfirmResponse(orderId, orderNo, "DRAFT", "AI解析结果已确认录入为采购单草稿");
    }

    private AiDocumentParseResult parseAiReply(String aiReply) {
        String jsonStr = aiClient.extractJson(aiReply);
        if (jsonStr == null) {
            throw new BusinessException("AI 返回内容无法解析为 JSON，原始内容：" + truncate(aiReply, 300));
        }
        try {
            JsonNode root = MAPPER.readTree(jsonStr);
            AiDocumentParseResult result = new AiDocumentParseResult();
            result.setSupplierName(root.path("supplierName").asText(""));
            result.setPaymentMethod(root.path("paymentMethod").asText(""));
            result.setRawJson(jsonStr);
            result.setRawText(aiReply);

            List<AiDocumentItemDTO> items = new ArrayList<>();
            JsonNode itemsNode = root.path("items");
            if (itemsNode.isArray()) {
                for (JsonNode n : itemsNode) {
                    AiDocumentItemDTO item = new AiDocumentItemDTO();
                    item.setMaterialCode(n.path("materialCode").asText(""));
                    item.setMaterialName(n.path("materialName").asText(""));
                    item.setSpecification(n.path("specification").asText(""));
                    item.setDeliveryDate(n.path("deliveryDate").asText(""));
                    item.setRemark(n.path("remark").asText(""));
                    if (!n.path("unitPrice").isNull() && n.path("unitPrice").isNumber()) {
                        item.setUnitPrice(n.path("unitPrice").decimalValue());
                    }
                    if (!n.path("quantity").isNull() && n.path("quantity").isNumber()) {
                        item.setQuantity(n.path("quantity").decimalValue());
                    }
                    item.setConfidence(n.path("confidence").isNumber() ? n.path("confidence").asDouble(0.8) : 0.8);
                    items.add(item);
                }
            }
            result.setItems(items);
            return result;
        } catch (Exception e) {
            throw new BusinessException("JSON 解析失败：" + e.getMessage() + "，原始内容：" + truncate(jsonStr, 200));
        }
    }

    private AiDocumentParseResult buildMockResult() {
        AiDocumentParseResult result = new AiDocumentParseResult();
        result.setSupplierName("华东机电供应商（Mock）");
        result.setPaymentMethod("月结30天");
        result.setRawText("【Mock数据】ai.mock=true 时返回演示数据，不调用真实AI接口");
        result.setRawJson("{\"mock\":true}");

        List<AiDocumentItemDTO> items = new ArrayList<>();

        AiDocumentItemDTO i1 = new AiDocumentItemDTO();
        i1.setMaterialCode("M001");
        i1.setMaterialName("深沟球轴承");
        i1.setSpecification("6205-2RS");
        i1.setUnitPrice(new BigDecimal("12.50"));
        i1.setQuantity(new BigDecimal("100"));
        i1.setDeliveryDate("2026-08-01");
        i1.setRemark("Mock示例行1");
        i1.setConfidence(0.95);
        items.add(i1);

        AiDocumentItemDTO i2 = new AiDocumentItemDTO();
        i2.setMaterialCode("M002");
        i2.setMaterialName("橡胶密封圈");
        i2.setSpecification("O型 φ30×3");
        i2.setUnitPrice(new BigDecimal("0.80"));
        i2.setQuantity(new BigDecimal("500"));
        i2.setDeliveryDate("2026-08-05");
        i2.setRemark("Mock示例行2");
        i2.setConfidence(0.88);
        items.add(i2);

        result.setItems(items);
        return result;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private String resolveAiErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        if (e instanceof java.net.http.HttpTimeoutException) {
            return "AI 接口请求超时，请检查网络、模型响应时间或调大 ai.timeout-seconds";
        }
        if (e instanceof java.net.ConnectException) {
            return "无法连接 AI 接口，请检查 ai.base-url、网络或防火墙";
        }
        if (e instanceof java.net.UnknownHostException) {
            return "AI 接口域名无法解析，请检查 ai.base-url 或 DNS";
        }
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return "AI 请求被中断";
        }
        Throwable cause = e.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return e.getClass().getName();
    }
}
