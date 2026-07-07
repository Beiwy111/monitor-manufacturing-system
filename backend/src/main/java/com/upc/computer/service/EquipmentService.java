package com.upc.computer.service;

import com.upc.computer.entity.Equipment;
import com.upc.computer.entity.AndonAlarm;
import com.upc.computer.entity.EquipmentMaintenanceRecord;
import java.util.ArrayList;

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

}
