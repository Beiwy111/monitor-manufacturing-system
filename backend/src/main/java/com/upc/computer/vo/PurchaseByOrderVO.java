package com.upc.computer.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单维度缺料视图：一个来源订单/工单下展开全部缺料零部件
 */
public class PurchaseByOrderVO {

    /** 来源类型 ORDER/WORK_ORDER */
    private String sourceType;

    /** 来源订单/工单ID */
    private Long sourceId;

    /** 来源订单/工单编号 */
    private String sourceNo;

    /** 该订单缺料零部件明细 */
    private List<Line> lines;

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceNo() {
        return sourceNo;
    }

    public void setSourceNo(String sourceNo) {
        this.sourceNo = sourceNo;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    /**
     * 缺料零部件行
     */
    public static class Line {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal requiredQuantity;
        private BigDecimal shortageQuantity;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public String getMaterialCode() {
            return materialCode;
        }

        public void setMaterialCode(String materialCode) {
            this.materialCode = materialCode;
        }

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public BigDecimal getRequiredQuantity() {
            return requiredQuantity;
        }

        public void setRequiredQuantity(BigDecimal requiredQuantity) {
            this.requiredQuantity = requiredQuantity;
        }

        public BigDecimal getShortageQuantity() {
            return shortageQuantity;
        }

        public void setShortageQuantity(BigDecimal shortageQuantity) {
            this.shortageQuantity = shortageQuantity;
        }
    }

}
