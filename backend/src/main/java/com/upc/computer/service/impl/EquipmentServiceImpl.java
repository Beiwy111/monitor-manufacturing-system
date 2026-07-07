package com.upc.computer.service.impl;

import com.upc.computer.service.EquipmentService;
import com.upc.computer.entity.Equipment;
import com.upc.computer.mapper.EquipmentMapper;
import com.upc.computer.entity.AndonAlarm;
import com.upc.computer.mapper.AndonAlarmMapper;
import com.upc.computer.entity.EquipmentMaintenanceRecord;
import com.upc.computer.mapper.EquipmentMaintenanceRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
public class EquipmentServiceImpl implements EquipmentService {

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private AndonAlarmMapper andonAlarmMapper;

    @Autowired
    private EquipmentMaintenanceRecordMapper equipmentMaintenanceRecordMapper;

    // 查询所有设备
    @Override
    public ArrayList<Equipment> equipmentList() {
        return equipmentMapper.equipmentList();
    }

    // 根据主键查询设备
    @Override
    public Equipment getEquipmentById(Long equipmentId) {
        return equipmentMapper.getEquipmentById(equipmentId);
    }

    // 新增设备
    @Override
    public void insertEquipment(Equipment equipment) {
        equipmentMapper.insertEquipment(equipment);
    }

    // 修改设备
    @Override
    public void updateEquipment(Equipment equipment) {
        equipmentMapper.updateEquipment(equipment);
    }

    // 删除设备
    @Override
    public void deleteEquipment(Long equipmentId) {
        equipmentMapper.deleteEquipment(equipmentId);
    }

    // 查询所有安灯报警
    @Override
    public ArrayList<AndonAlarm> alarmList() {
        return andonAlarmMapper.alarmList();
    }

    // 根据主键查询安灯报警
    @Override
    public AndonAlarm getAlarmById(Long alarmId) {
        return andonAlarmMapper.getAlarmById(alarmId);
    }

    // 新增安灯报警
    @Override
    public void insertAlarm(AndonAlarm alarm) {
        andonAlarmMapper.insertAlarm(alarm);
    }

    // 修改安灯报警
    @Override
    public void updateAlarm(AndonAlarm alarm) {
        andonAlarmMapper.updateAlarm(alarm);
    }

    // 删除安灯报警
    @Override
    public void deleteAlarm(Long alarmId) {
        andonAlarmMapper.deleteAlarm(alarmId);
    }

    // 查询所有设备维护记录
    @Override
    public ArrayList<EquipmentMaintenanceRecord> maintenanceList() {
        return equipmentMaintenanceRecordMapper.maintenanceList();
    }

    // 根据主键查询设备维护记录
    @Override
    public EquipmentMaintenanceRecord getMaintenanceById(Long maintenanceId) {
        return equipmentMaintenanceRecordMapper.getMaintenanceById(maintenanceId);
    }

    // 新增设备维护记录
    @Override
    public void insertMaintenance(EquipmentMaintenanceRecord maintenance) {
        equipmentMaintenanceRecordMapper.insertMaintenance(maintenance);
    }

    // 修改设备维护记录
    @Override
    public void updateMaintenance(EquipmentMaintenanceRecord maintenance) {
        equipmentMaintenanceRecordMapper.updateMaintenance(maintenance);
    }

    // 删除设备维护记录
    @Override
    public void deleteMaintenance(Long maintenanceId) {
        equipmentMaintenanceRecordMapper.deleteMaintenance(maintenanceId);
    }

}
