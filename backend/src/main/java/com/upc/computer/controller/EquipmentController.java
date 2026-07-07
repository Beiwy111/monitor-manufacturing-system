package com.upc.computer.controller;

import com.upc.computer.service.EquipmentService;
import com.upc.computer.entity.Equipment;
import com.upc.computer.entity.AndonAlarm;
import com.upc.computer.entity.EquipmentMaintenanceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;

@RestController
@RequestMapping("/equipment")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    // 查询设备列表
    @RequestMapping("/equipment/list")
    public ArrayList<Equipment> equipmentList() {
        return equipmentService.equipmentList();
    }

    // 根据主键查询设备
    @RequestMapping("/equipment/get")
    public Equipment getEquipmentById(Long equipmentId) {
        return equipmentService.getEquipmentById(equipmentId);
    }

    // 新增设备
    @RequestMapping("/equipment/insert")
    public void insertEquipment(Equipment equipment) {
        equipmentService.insertEquipment(equipment);
    }

    // 修改设备
    @RequestMapping("/equipment/update")
    public void updateEquipment(Equipment equipment) {
        equipmentService.updateEquipment(equipment);
    }

    // 删除设备
    @RequestMapping("/equipment/delete")
    public void deleteEquipment(Long equipmentId) {
        equipmentService.deleteEquipment(equipmentId);
    }

    // 查询安灯报警列表
    @RequestMapping("/alarm/list")
    public ArrayList<AndonAlarm> alarmList() {
        return equipmentService.alarmList();
    }

    // 根据主键查询安灯报警
    @RequestMapping("/alarm/get")
    public AndonAlarm getAlarmById(Long alarmId) {
        return equipmentService.getAlarmById(alarmId);
    }

    // 新增安灯报警
    @RequestMapping("/alarm/insert")
    public void insertAlarm(AndonAlarm alarm) {
        equipmentService.insertAlarm(alarm);
    }

    // 修改安灯报警
    @RequestMapping("/alarm/update")
    public void updateAlarm(AndonAlarm alarm) {
        equipmentService.updateAlarm(alarm);
    }

    // 删除安灯报警
    @RequestMapping("/alarm/delete")
    public void deleteAlarm(Long alarmId) {
        equipmentService.deleteAlarm(alarmId);
    }

    // 查询设备维护记录列表
    @RequestMapping("/maintenance/list")
    public ArrayList<EquipmentMaintenanceRecord> maintenanceList() {
        return equipmentService.maintenanceList();
    }

    // 根据主键查询设备维护记录
    @RequestMapping("/maintenance/get")
    public EquipmentMaintenanceRecord getMaintenanceById(Long maintenanceId) {
        return equipmentService.getMaintenanceById(maintenanceId);
    }

    // 新增设备维护记录
    @RequestMapping("/maintenance/insert")
    public void insertMaintenance(EquipmentMaintenanceRecord maintenance) {
        equipmentService.insertMaintenance(maintenance);
    }

    // 修改设备维护记录
    @RequestMapping("/maintenance/update")
    public void updateMaintenance(EquipmentMaintenanceRecord maintenance) {
        equipmentService.updateMaintenance(maintenance);
    }

    // 删除设备维护记录
    @RequestMapping("/maintenance/delete")
    public void deleteMaintenance(Long maintenanceId) {
        equipmentService.deleteMaintenance(maintenanceId);
    }

}
