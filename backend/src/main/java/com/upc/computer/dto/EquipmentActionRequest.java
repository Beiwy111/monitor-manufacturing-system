package com.upc.computer.dto;

import java.math.BigDecimal;

/**
 * 设备维保闭环动作请求。
 * 触发报警 / 接收报警 / 解除报警 / 开始维保 / 完成维保 复用此 DTO。
 */
public class EquipmentActionRequest {

    private Long equipmentId;
    private Long alarmId;
    private Long maintenanceId;

    /** 报警类型：EQUIPMENT/QUALITY/MATERIAL/PROCESS/SAFETY/OTHER，默认 EQUIPMENT */
    private String alarmType;
    /** 报警级别：GENERAL/IMPORTANT/URGENT，默认 GENERAL */
    private String alarmLevel;
    /** 报警描述 / 通用描述 */
    private String description;

    /** 故障描述（维保） */
    private String faultDescription;
    /** 维护类型：INSPECTION/REPAIR/MAINTENANCE/OVERHAUL，默认 REPAIR */
    private String maintenanceType;
    /** 维护内容 */
    private String maintenanceContent;
    /** 维保结果：COMPLETED/UNRESOLVED/TEMPORARY_FIXED，默认 COMPLETED */
    private String result;
    /** 维护成本 */
    private BigDecimal costAmount;

    /** 备注 / 处理结果 */
    private String remark;
    /** 操作人用户名（前端登录态传入） */
    private String operator;

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

    public Long getAlarmId() { return alarmId; }
    public void setAlarmId(Long alarmId) { this.alarmId = alarmId; }

    public Long getMaintenanceId() { return maintenanceId; }
    public void setMaintenanceId(Long maintenanceId) { this.maintenanceId = maintenanceId; }

    public String getAlarmType() { return alarmType; }
    public void setAlarmType(String alarmType) { this.alarmType = alarmType; }

    public String getAlarmLevel() { return alarmLevel; }
    public void setAlarmLevel(String alarmLevel) { this.alarmLevel = alarmLevel; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFaultDescription() { return faultDescription; }
    public void setFaultDescription(String faultDescription) { this.faultDescription = faultDescription; }

    public String getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }

    public String getMaintenanceContent() { return maintenanceContent; }
    public void setMaintenanceContent(String maintenanceContent) { this.maintenanceContent = maintenanceContent; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public BigDecimal getCostAmount() { return costAmount; }
    public void setCostAmount(BigDecimal costAmount) { this.costAmount = costAmount; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
