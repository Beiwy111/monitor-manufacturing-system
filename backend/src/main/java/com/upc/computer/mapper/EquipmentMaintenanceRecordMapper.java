package com.upc.computer.mapper;

import com.upc.computer.entity.EquipmentMaintenanceRecord;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface EquipmentMaintenanceRecordMapper {

    // 查询所有设备维护记录
    @Select("SELECT maintenance_id, maintenance_no, equipment_id, alarm_id, maintenance_type, fault_description, maintenance_content, start_time, end_time, downtime_minutes, maintenance_result, maintainer_id, cost_amount, remark, created_at, updated_at FROM equipment_maintenance_record")
    public ArrayList<EquipmentMaintenanceRecord> maintenanceList();

    // 根据主键查询设备维护记录
    @Select("SELECT maintenance_id, maintenance_no, equipment_id, alarm_id, maintenance_type, fault_description, maintenance_content, start_time, end_time, downtime_minutes, maintenance_result, maintainer_id, cost_amount, remark, created_at, updated_at FROM equipment_maintenance_record WHERE maintenance_id = #{maintenanceId}")
    public EquipmentMaintenanceRecord getMaintenanceById(Long maintenanceId);

    // 新增设备维护记录
    @Insert("INSERT INTO equipment_maintenance_record (maintenance_id, maintenance_no, equipment_id, alarm_id, maintenance_type, fault_description, maintenance_content, start_time, end_time, downtime_minutes, maintenance_result, maintainer_id, cost_amount, remark, created_at, updated_at) VALUES (#{maintenanceId}, #{maintenanceNo}, #{equipmentId}, #{alarmId}, #{maintenanceType}, #{faultDescription}, #{maintenanceContent}, #{startTime}, #{endTime}, #{downtimeMinutes}, #{maintenanceResult}, #{maintainerId}, #{costAmount}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "maintenanceId")
    public void insertMaintenance(EquipmentMaintenanceRecord maintenance);

    // 修改设备维护记录
    @Update("UPDATE equipment_maintenance_record SET maintenance_no=#{maintenanceNo}, equipment_id=#{equipmentId}, alarm_id=#{alarmId}, maintenance_type=#{maintenanceType}, fault_description=#{faultDescription}, maintenance_content=#{maintenanceContent}, start_time=#{startTime}, end_time=#{endTime}, downtime_minutes=#{downtimeMinutes}, maintenance_result=#{maintenanceResult}, maintainer_id=#{maintainerId}, cost_amount=#{costAmount}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE maintenance_id = #{maintenanceId}")
    public void updateMaintenance(EquipmentMaintenanceRecord maintenance);

    // 删除设备维护记录
    @Delete("DELETE FROM equipment_maintenance_record WHERE maintenance_id = #{maintenanceId}")
    public void deleteMaintenance(Long maintenanceId);

}
