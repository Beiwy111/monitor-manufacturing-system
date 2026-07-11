package com.upc.computer.dto;

import java.util.List;

public class AiDocumentConfirmRequest {

    private String supplierName;
    private String paymentMethod;
    private List<AiDocumentItemDTO> items;

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public List<AiDocumentItemDTO> getItems() { return items; }
    public void setItems(List<AiDocumentItemDTO> items) { this.items = items; }
}
