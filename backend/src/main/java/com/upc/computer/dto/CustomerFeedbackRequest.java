package com.upc.computer.dto;

public class CustomerFeedbackRequest {

    private Long orderId;
    private String serialNo;
    private Long materialId;
    private String problemType;
    private String problemDescription;
    private java.util.List<String> attachmentUrls;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getProblemType() { return problemType; }
    public void setProblemType(String problemType) { this.problemType = problemType; }

    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }

    public java.util.List<String> getAttachmentUrls() { return attachmentUrls; }
    public void setAttachmentUrls(java.util.List<String> attachmentUrls) { this.attachmentUrls = attachmentUrls; }
}
