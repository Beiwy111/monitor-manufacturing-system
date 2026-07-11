package com.upc.computer.dto;

import java.util.List;

public class AiDocumentParseResult {

    private String supplierName;
    private String paymentMethod;
    private List<AiDocumentItemDTO> items;
    private String rawText;
    private String rawJson;

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public List<AiDocumentItemDTO> getItems() { return items; }
    public void setItems(List<AiDocumentItemDTO> items) { this.items = items; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
