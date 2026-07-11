package com.upc.computer.mapper;

import com.upc.computer.entity.QualityInspectionItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QualityInspectionItemMapper {

    @Select("SELECT inspection_item_id, inspection_id, item_code, item_name, standard_value, measured_value, unit, result, defect_level, sort_order, remark, created_at, updated_at FROM quality_inspection_item WHERE inspection_id = #{inspectionId} ORDER BY sort_order")
    List<QualityInspectionItem> listByInspectionId(Long inspectionId);

    @Select("SELECT inspection_item_id, inspection_id, item_code, item_name, standard_value, measured_value, unit, result, defect_level, sort_order, remark, created_at, updated_at FROM quality_inspection_item WHERE inspection_item_id = #{id}")
    QualityInspectionItem getById(Long id);

    @Insert("INSERT INTO quality_inspection_item (inspection_id, item_code, item_name, standard_value, measured_value, unit, result, defect_level, sort_order, remark, created_at, updated_at) VALUES (#{inspectionId}, #{itemCode}, #{itemName}, #{standardValue}, #{measuredValue}, #{unit}, #{result}, #{defectLevel}, #{sortOrder}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "inspectionItemId")
    void insert(QualityInspectionItem item);

    @Update("UPDATE quality_inspection_item SET item_code=#{itemCode}, item_name=#{itemName}, standard_value=#{standardValue}, measured_value=#{measuredValue}, unit=#{unit}, result=#{result}, defect_level=#{defectLevel}, sort_order=#{sortOrder}, remark=#{remark}, updated_at=#{updatedAt} WHERE inspection_item_id=#{inspectionItemId}")
    void update(QualityInspectionItem item);

    @Delete("DELETE FROM quality_inspection_item WHERE inspection_item_id=#{id}")
    void deleteById(Long id);

    @Delete("DELETE FROM quality_inspection_item WHERE inspection_id=#{inspectionId}")
    void deleteByInspectionId(Long inspectionId);
}
