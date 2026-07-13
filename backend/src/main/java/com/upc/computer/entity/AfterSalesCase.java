package com.upc.computer.entity;

import java.time.LocalDateTime;

/**
 * 售后案例实体，对应表 after_sales_case
 * 状态流转：OPEN -> PROCESSING -> RESOLVED -> CLOSED
 */
public class AfterSalesCase {

    private String caseNo;
    private Long orderId;
    private Long deliveryId;
    private Long materialId;
    private String batchNo;
    /** 关联质检单ID，用于售后问题反向追溯 */
    private Long qualityInspectionId;
    private String customerName;
    private String contactName;
    private String contactPhone;
    private String problemDescription;
    private String attachmentUrls;
    private String problemType;
    private String caseLevel;
    private String caseStatus;
    private String traceResult;
    private String handleResult;
    private Long serviceUserId;
    private LocalDateTime openedAt;
    private LocalDateTime processingAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getCaseNo() { return caseNo; }
    public void setCaseNo(String caseNo) { this.caseNo = caseNo; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getDeliveryId() { return deliveryId; }
    public void setDeliveryId(Long deliveryId) { this.deliveryId = deliveryId; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public Long getQualityInspectionId() { return qualityInspectionId; }
    public void setQualityInspectionId(Long qualityInspectionId) { this.qualityInspectionId = qualityInspectionId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }

    public String getAttachmentUrls() { return attachmentUrls; }
    public void setAttachmentUrls(String attachmentUrls) { this.attachmentUrls = attachmentUrls; }

    public String getProblemType() { return problemType; }
    public void setProblemType(String problemType) { this.problemType = problemType; }

    public String getCaseLevel() { return caseLevel; }
    public void setCaseLevel(String caseLevel) { this.caseLevel = caseLevel; }

    public String getCaseStatus() { return caseStatus; }
    public void setCaseStatus(String caseStatus) { this.caseStatus = caseStatus; }

    public String getTraceResult() { return traceResult; }
    public void setTraceResult(String traceResult) { this.traceResult = traceResult; }

    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }

    public Long getServiceUserId() { return serviceUserId; }
    public void setServiceUserId(Long serviceUserId) { this.serviceUserId = serviceUserId; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public LocalDateTime getProcessingAt() { return processingAt; }
    public void setProcessingAt(LocalDateTime processingAt) { this.processingAt = processingAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
