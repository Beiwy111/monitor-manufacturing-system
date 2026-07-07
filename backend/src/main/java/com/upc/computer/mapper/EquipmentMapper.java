package com.upc.computer.mapper;

import com.upc.computer.entity.Equipment;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface EquipmentMapper {

    // 查询所有设备
    @Select("SELECT equipment_id, equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at, created_at, updated_at FROM equipment")
    public ArrayList<Equipment> equipmentList();

    // 根据主键查询设备
    @Select("SELECT equipment_id, equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at, created_at, updated_at FROM equipment WHERE equipment_id = #{equipmentId}")
    public Equipment getEquipmentById(Long equipmentId);

    // 新增设备
    @Insert("INSERT INTO equipment (equipment_id, equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at, created_at, updated_at) VALUES (#{equipmentId}, #{equipmentCode}, #{equipmentName}, #{equipmentType}, #{workshop}, #{workstation}, #{manufacturer}, #{model}, #{purchaseDate}, #{status}, #{lastMaintenanceAt}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "equipmentId")
    public void insertEquipment(Equipment equipment);

    // 修改设备
    @Update("UPDATE equipment SET equipment_code=#{equipmentCode}, equipment_name=#{equipmentName}, equipment_type=#{equipmentType}, workshop=#{workshop}, workstation=#{workstation}, manufacturer=#{manufacturer}, model=#{model}, purchase_date=#{purchaseDate}, status=#{status}, last_maintenance_at=#{lastMaintenanceAt}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE equipment_id = #{equipmentId}")
    public void updateEquipment(Equipment equipment);

    // 删除设备
    @Delete("DELETE FROM equipment WHERE equipment_id = #{equipmentId}")
    public void deleteEquipment(Long equipmentId);

}
