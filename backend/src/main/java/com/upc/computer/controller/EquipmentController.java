package com.upc.computer.controller;

import com.upc.computer.common.BusinessException;
import com.upc.computer.common.Result;
import com.upc.computer.dto.EquipmentActionRequest;
import com.upc.computer.entity.AndonAlarm;
import com.upc.computer.entity.Equipment;
import com.upc.computer.entity.EquipmentMaintenanceRecord;
import com.upc.computer.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/equipment")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    // ===== 基础 CRUD（保留，向后兼容） =====
    @RequestMapping("/equipment/list")
    public ArrayList<Equipment> equipmentList() { return equipmentService.equipmentList(); }

    @RequestMapping("/equipment/get")
    public Equipment getEquipmentById(Long equipmentId) { return equipmentService.getEquipmentById(equipmentId); }

    @RequestMapping("/equipment/insert")
    public void insertEquipment(Equipment equipment) { equipmentService.insertEquipment(equipment); }

    @RequestMapping("/equipment/update")
    public void updateEquipment(Equipment equipment) { equipmentService.updateEquipment(equipment); }

    @RequestMapping("/equipment/delete")
    public void deleteEquipment(Long equipmentId) { equipmentService.deleteEquipment(equipmentId); }

    @RequestMapping("/alarm/list")
    public ArrayList<AndonAlarm> alarmList() { return equipmentService.alarmList(); }

    @RequestMapping("/alarm/get")
    public AndonAlarm getAlarmById(Long alarmId) { return equipmentService.getAlarmById(alarmId); }

    @RequestMapping("/alarm/insert")
    public void insertAlarm(AndonAlarm alarm) { equipmentService.insertAlarm(alarm); }

    @RequestMapping("/alarm/update")
    public void updateAlarm(AndonAlarm alarm) { equipmentService.updateAlarm(alarm); }

    @RequestMapping("/alarm/delete")
    public void deleteAlarm(Long alarmId) { equipmentService.deleteAlarm(alarmId); }

    @RequestMapping("/maintenance/list")
    public ArrayList<EquipmentMaintenanceRecord> maintenanceList() { return equipmentService.maintenanceList(); }

    @RequestMapping("/maintenance/get")
    public EquipmentMaintenanceRecord getMaintenanceById(Long maintenanceId) { return equipmentService.getMaintenanceById(maintenanceId); }

    @RequestMapping("/maintenance/insert")
    public void insertMaintenance(EquipmentMaintenanceRecord maintenance) { equipmentService.insertMaintenance(maintenance); }

    @RequestMapping("/maintenance/update")
    public void updateMaintenance(EquipmentMaintenanceRecord maintenance) { equipmentService.updateMaintenance(maintenance); }

    @RequestMapping("/maintenance/delete")
    public void deleteMaintenance(Long maintenanceId) { equipmentService.deleteMaintenance(maintenanceId); }

    // ===== 视图 & KPI =====
    @GetMapping("/equipment/views")
    public Result<List<Map<String, Object>>> equipmentViews() {
        return Result.success(equipmentService.equipmentViews());
    }

    @GetMapping("/kpi")
    public Result<Map<String, Object>> kpi() {
        return Result.success(equipmentService.equipmentKpi());
    }

    @GetMapping("/alarm/views")
    public Result<List<Map<String, Object>>> alarmViews() {
        return Result.success(equipmentService.alarmViews());
    }

    @GetMapping("/maintenance/views")
    public Result<List<Map<String, Object>>> maintenanceViews() {
        return Result.success(equipmentService.maintenanceViews());
    }

    // ===== 安灯报警 + 维保闭环动作 =====
    @PostMapping("/triggerAlarm")
    public Result<AndonAlarm> triggerAlarm(@RequestBody EquipmentActionRequest req) {
        require(req.getEquipmentId(), "equipmentId");
        return Result.success("安灯报警已触发，设备转为故障",
                equipmentService.triggerAlarm(req.getEquipmentId(), req.getAlarmType(),
                        req.getAlarmLevel(), req.getDescription(), req.getOperator()));
    }

    @PostMapping("/alarm/receive")
    public Result<AndonAlarm> receiveAlarm(@RequestBody EquipmentActionRequest req) {
        require(req.getAlarmId(), "alarmId");
        return Result.success("已接收报警", equipmentService.receiveAlarm(req.getAlarmId(), req.getOperator()));
    }

    @PostMapping("/alarm/resolve")
    public Result<AndonAlarm> resolveAlarm(@RequestBody EquipmentActionRequest req) {
        require(req.getAlarmId(), "alarmId");
        return Result.success("报警已解除",
                equipmentService.resolveAlarm(req.getAlarmId(), req.getRemark(), req.getOperator()));
    }

    @PostMapping("/startMaintenance")
    public Result<EquipmentMaintenanceRecord> startMaintenance(@RequestBody EquipmentActionRequest req) {
        require(req.getEquipmentId(), "equipmentId");
        return Result.success("已开始维保，设备转为维保中",
                equipmentService.startMaintenance(req.getEquipmentId(), req.getAlarmId(), req.getMaintenanceType(),
                        req.getFaultDescription(), req.getMaintenanceContent(), req.getOperator()));
    }

    @PostMapping("/maintenance/finish")
    public Result<EquipmentMaintenanceRecord> finishMaintenance(@RequestBody EquipmentActionRequest req) {
        require(req.getMaintenanceId(), "maintenanceId");
        return Result.success("维保完成，设备已恢复正常",
                equipmentService.finishMaintenance(req.getMaintenanceId(), req.getResult(),
                        req.getCostAmount(), req.getMaintenanceContent(), req.getOperator()));
    }

    private void require(Long id, String field) {
        if (id == null || id <= 0) throw new BusinessException(field + " 不能为空");
    }
}
