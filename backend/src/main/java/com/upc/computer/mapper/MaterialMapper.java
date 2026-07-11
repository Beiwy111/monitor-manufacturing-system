package com.upc.computer.mapper;

import com.upc.computer.entity.Material;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MaterialMapper {

    @Select("SELECT material_id, material_code, material_name, material_type, specification, unit, safety_stock, standard_cost, supplier_id, status, created_at, updated_at FROM material")
    ArrayList<Material> materialList();

    @Select("SELECT material_id, material_code, material_name, material_type, specification, unit, safety_stock, standard_cost, supplier_id, status, created_at, updated_at FROM material WHERE material_id = #{materialId}")
    Material getMaterialById(Long materialId);

    @Insert("INSERT INTO material (material_code, material_name, material_type, specification, unit, safety_stock, standard_cost, supplier_id, status, created_at, updated_at) VALUES (#{materialCode}, #{materialName}, #{materialType}, #{specification}, #{unit}, #{safetyStock}, #{standardCost}, #{supplierId}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "materialId")
    void insertMaterial(Material material);

    @Update("UPDATE material SET material_code=#{materialCode}, material_name=#{materialName}, material_type=#{materialType}, specification=#{specification}, unit=#{unit}, safety_stock=#{safetyStock}, standard_cost=#{standardCost}, supplier_id=#{supplierId}, status=#{status}, updated_at=#{updatedAt} WHERE material_id = #{materialId}")
    void updateMaterial(Material material);

    @Delete("DELETE FROM material WHERE material_id = #{materialId}")
    void deleteMaterial(Long materialId);

    @Update("UPDATE material SET supplier_id = #{supplierId}, updated_at = NOW() WHERE material_id = #{materialId}")
    void updateSupplier(@Param("materialId") Long materialId, @Param("supplierId") Long supplierId);
}
