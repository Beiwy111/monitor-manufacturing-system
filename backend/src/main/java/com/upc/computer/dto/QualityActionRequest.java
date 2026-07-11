package com.upc.computer.dto;

import java.math.BigDecimal;

public class QualityActionRequest {

    private Long inspectionId;
    private Long nonconformingId;
    private String remark;
    private String reason;
    private String operator;

    // 不合格/复检不通过时填写
    private String defectType;
    private String defectReason;
    private BigDecimal defectQuantity;
    private String severity;

    // 不良品处置
    private String handleMethod;

    public Long getInspectionId() { return inspectionId; }
    public void setInspectionId(Long inspectionId) { this.inspectionId = inspectionId; }

    public Long getNonconformingId() { return nonconformingId; }
    public void setNonconformingId(Long nonconformingId) { this.nonconformingId = nonconformingId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getDefectType() { return defectType; }
    public void setDefectType(String defectType) { this.defectType = defectType; }

    public String getDefectReason() { return defectReason; }
    public void setDefectReason(String defectReason) { this.defectReason = defectReason; }

    public BigDecimal getDefectQuantity() { return defectQuantity; }
    public void setDefectQuantity(BigDecimal defectQuantity) { this.defectQuantity = defectQuantity; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getHandleMethod() { return handleMethod; }
    public void setHandleMethod(String handleMethod) { this.handleMethod = handleMethod; }
}
