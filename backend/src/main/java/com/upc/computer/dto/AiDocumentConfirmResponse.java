package com.upc.computer.dto;

public class AiDocumentConfirmResponse {

    private Long purchaseOrderId;
    private String purchaseOrderNo;
    private String status;
    private String message;

    public AiDocumentConfirmResponse() {}

    public AiDocumentConfirmResponse(Long purchaseOrderId, String purchaseOrderNo, String status, String message) {
        this.purchaseOrderId = purchaseOrderId;
        this.purchaseOrderNo = purchaseOrderNo;
        this.status = status;
        this.message = message;
    }

    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(Long purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }

    public String getPurchaseOrderNo() { return purchaseOrderNo; }
    public void setPurchaseOrderNo(String purchaseOrderNo) { this.purchaseOrderNo = purchaseOrderNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
