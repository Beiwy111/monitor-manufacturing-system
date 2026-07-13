package com.upc.computer.entity;

import java.time.LocalDateTime;

/** 条码规则配置，对应 barcode_rule。 */
public class BarcodeRule {
    private Long ruleId;
    private String ruleCode;
    private String businessType;
    private String prefix;
    private String datePattern;
    private Integer serialLength;
    private Long currentSerial;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getDatePattern() { return datePattern; }
    public void setDatePattern(String datePattern) { this.datePattern = datePattern; }
    public Integer getSerialLength() { return serialLength; }
    public void setSerialLength(Integer serialLength) { this.serialLength = serialLength; }
    public Long getCurrentSerial() { return currentSerial; }
    public void setCurrentSerial(Long currentSerial) { this.currentSerial = currentSerial; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
