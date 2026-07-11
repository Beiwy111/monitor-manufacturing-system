package com.upc.computer.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单附件 OCR 识别（当前为模拟实现，预留真实 OCR 对接）。
 */
@Service
public class OrderOcrService {

    private static final Map<String, Map<String, Object>> MOCK_BY_FILE = Map.of(
            "CO202607004_contract.svg", mockFields("广州教育设备有限公司", "15.6寸商用显示器", 100,
                    "2026-08-10", 68000.0, "教育行业采购合同"),
            "CO202607002_po.svg", mockFields("北京星辰电竞俱乐部", "23.8寸电竞显示器", 150,
                    "2026-07-25", 192000.0, "电竞显示器采购单"),
            "CO202607003_spec.svg", mockFields("上海视觉设计工作室", "27寸4K显示器", 40,
                    "2026-08-05", 84000.0, "4K显示器技术规格书")
    );

    public Map<String, Object> recognize(String fileName, String orderId) {
        String key = fileName != null ? fileName.trim() : "";
        Map<String, Object> fields = MOCK_BY_FILE.getOrDefault(key, defaultMock(orderId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", fileName);
        result.put("orderId", orderId);
        result.put("engine", "mock-ocr-v1");
        result.put("confidence", 0.92);
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
}
