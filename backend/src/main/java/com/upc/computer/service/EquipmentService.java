package com.upc.computer.service;

import com.upc.computer.entity.Equipment;
import com.upc.computer.entity.AndonAlarm;
import com.upc.computer.entity.EquipmentMaintenanceRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface EquipmentService {

    public ArrayList<Equipment> equipmentList();

    public Equipment getEquipmentById(Long equipmentId);

    public void insertEquipment(Equipment equipment);

    public void updateEquipment(Equipment equipment);

    public void deleteEquipment(Long equipmentId);

    public ArrayList<AndonAlarm> alarmList();

    public AndonAlarm getAlarmById(Long alarmId);

    public void insertAlarm(AndonAlarm alarm);

    public void updateAlarm(AndonAlarm alarm);

    public void deleteAlarm(Long alarmId);

    public ArrayList<EquipmentMaintenanceRecord> maintenanceList();

    public EquipmentMaintenanceRecord getMaintenanceById(Long maintenanceId);

    public void insertMaintenance(EquipmentMaintenanceRecord maintenance);

    public void updateMaintenance(EquipmentMaintenanceRecord maintenance);

    public void deleteMaintenance(Long maintenanceId);

    // ===== 安灯报警 + 维保闭环 =====

    /** 设备台账视图（含状态中文、未闭环报警数、是否维保中） */
    List<Map<String, Object>> equipmentViews();

    /** 设备维保 KPI 汇总 */
    Map<String, Object> equipmentKpi();

    /** 安灯报警视图（含设备名、上报人、状态中文） */
    List<Map<String, Object>> alarmViews();

    /** 维护记录视图（含设备名、维护人、结果中文） */
    List<Map<String, Object>> maintenanceViews();

    /** 八道生产工序 × 19 车间总览（与生产主管大屏同一数据源） */
    Map<String, Object> workshopOverview();

    /** 触发安灯报警：设备置为故障(FAULT)，创建 OPEN 报警 */
    AndonAlarm triggerAlarm(Long equipmentId, String alarmType, String alarmLevel, String description, String operator);

    /** 接收报警：OPEN -> RECEIVED */
    AndonAlarm receiveAlarm(Long alarmId, String operator);

    /** 解除报警：-> CLOSED（无关联维保时可让设备恢复空闲） */
    AndonAlarm resolveAlarm(Long alarmId, String closeResult, String operator);

    /** 开始维保：设备 FAULT -> MAINTAINING，创建维保记录 */
    EquipmentMaintenanceRecord startMaintenance(Long equipmentId, Long alarmId, String maintenanceType,
                                                String faultDescription, String maintenanceContent, String operator);

    /** 完成维保：设备 MAINTAINING -> IDLE，记录收尾并关闭关联报警 */
    EquipmentMaintenanceRecord finishMaintenance(Long maintenanceId, String result, BigDecimal costAmount,
                                                 String maintenanceContent, String operator);
}
