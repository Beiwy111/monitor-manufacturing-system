package com.upc.computer.mapper;

import com.upc.computer.entity.Material;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface MaterialMapper {

    // 查询所有物料
    @Select("SELECT material_id, material_code, material_name, material_type, specification, unit, safety_stock, standard_cost, status, created_at, updated_at FROM material")
    public ArrayList<Material> materialList();

    // 根据主键查询物料
    @Select("SELECT material_id, material_code, material_name, material_type, specification, unit, safety_stock, standard_cost, status, created_at, updated_at FROM material WHERE material_id = #{materialId}")
    public Material getMaterialById(Long materialId);

    // 新增物料
    @Insert("INSERT INTO material (material_id, material_code, material_name, material_type, specification, unit, safety_stock, standard_cost, status, created_at, updated_at) VALUES (#{materialId}, #{materialCode}, #{materialName}, #{materialType}, #{specification}, #{unit}, #{safetyStock}, #{standardCost}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "materialId")
    public void insertMaterial(Material material);

    // 修改物料
    @Update("UPDATE material SET material_code=#{materialCode}, material_name=#{materialName}, material_type=#{materialType}, specification=#{specification}, unit=#{unit}, safety_stock=#{safetyStock}, standard_cost=#{standardCost}, status=#{status}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE material_id = #{materialId}")
    public void updateMaterial(Material material);

    // 删除物料
    @Delete("DELETE FROM material WHERE material_id = #{materialId}")
    public void deleteMaterial(Long materialId);

}
