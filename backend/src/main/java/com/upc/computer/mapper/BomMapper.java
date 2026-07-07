package com.upc.computer.mapper;

import com.upc.computer.entity.Bom;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface BomMapper {

    // 查询所有BOM
    @Select("SELECT bom_id, parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, expire_date, status, remark, created_at, updated_at FROM bom")
    public ArrayList<Bom> bomList();

    // 根据主键查询BOM
    @Select("SELECT bom_id, parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, expire_date, status, remark, created_at, updated_at FROM bom WHERE bom_id = #{bomId}")
    public Bom getBomById(Long bomId);

    // 新增BOM
    @Insert("INSERT INTO bom (bom_id, parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, expire_date, status, remark, created_at, updated_at) VALUES (#{bomId}, #{parentMaterialId}, #{childMaterialId}, #{quantity}, #{lossRate}, #{versionNo}, #{effectiveDate}, #{expireDate}, #{status}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "bomId")
    public void insertBom(Bom bom);

    // 修改BOM
    @Update("UPDATE bom SET parent_material_id=#{parentMaterialId}, child_material_id=#{childMaterialId}, quantity=#{quantity}, loss_rate=#{lossRate}, version_no=#{versionNo}, effective_date=#{effectiveDate}, expire_date=#{expireDate}, status=#{status}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE bom_id = #{bomId}")
    public void updateBom(Bom bom);

    // 删除BOM
    @Delete("DELETE FROM bom WHERE bom_id = #{bomId}")
    public void deleteBom(Long bomId);

}
