package com.upc.computer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 一键生成采购单请求。
 *
 * 勾选的 requirementIds 会按物料绑定的默认供应商自动分组，
 * 每组生成一张独立采购单。
 *
 * supplierOverrides 允许前端按供应商ID覆盖联系信息和期望到货日期：
 *   key = supplierId（字符串形式），value = SupplierOverride
 * 未配置 override 的供应商使用 supplier 表中的信息。
 * 无供应商绑定的需求统一归入 supplierId=0 的"未分配"组。
 */
public class GeneratePurchaseRequest {

    private List<Long> requirementIds;

    /**
     * 按供应商的覆盖信息，key 为 supplierId 字符串（"0" 表示无供应商组）。
     */
    private Map<String, SupplierOverride> supplierOverrides;

    /** 全局备注，各采购单共用（可为空） */
    private String remark;

    /** 采购员ID */
    private Long purchaserId;

    /**
     * 按需求ID覆盖采购数量，key=requirementId。
     * 库存充足时采购员可自定义备库数量。
     */
    private Map<Long, BigDecimal> quantityOverrides;

    /**
     * 指定供应商：设置后所有勾选需求合并为一张采购单，由采购员自选供应商。
     * 未设置时仍按物料默认供应商自动拆单。
     */
    private Long forceSupplierId;

    public List<Long> getRequirementIds() { return requirementIds; }
    public void setRequirementIds(List<Long> requirementIds) { this.requirementIds = requirementIds; }

    public Map<String, SupplierOverride> getSupplierOverrides() { return supplierOverrides; }
    public void setSupplierOverrides(Map<String, SupplierOverride> supplierOverrides) { this.supplierOverrides = supplierOverrides; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Long getPurchaserId() { return purchaserId; }
    public void setPurchaserId(Long purchaserId) { this.purchaserId = purchaserId; }

    public Long getForceSupplierId() { return forceSupplierId; }
    public void setForceSupplierId(Long forceSupplierId) { this.forceSupplierId = forceSupplierId; }

    public Map<Long, BigDecimal> getQuantityOverrides() { return quantityOverrides; }
    public void setQuantityOverrides(Map<Long, BigDecimal> quantityOverrides) { this.quantityOverrides = quantityOverrides; }

    public static class SupplierOverride {
        private String supplierName;
        private String supplierContact;
        private String supplierPhone;
        private LocalDate expectedArrivalDate;

        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

        public String getSupplierContact() { return supplierContact; }
        public void setSupplierContact(String supplierContact) { this.supplierContact = supplierContact; }

        public String getSupplierPhone() { return supplierPhone; }
        public void setSupplierPhone(String supplierPhone) { this.supplierPhone = supplierPhone; }

        public LocalDate getExpectedArrivalDate() { return expectedArrivalDate; }
        public void setExpectedArrivalDate(LocalDate expectedArrivalDate) { this.expectedArrivalDate = expectedArrivalDate; }
    }
}
