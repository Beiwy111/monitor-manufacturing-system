package com.upc.computer.mapper;

import com.upc.computer.entity.QualityInspection;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface QualityInspectionMapper {

    // 查询所有质量检验
    @Select("SELECT inspection_id, inspection_no, work_order_id, work_report_id, material_id, batch_no, inspection_type, sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspector_id, inspected_at, remark, created_at, updated_at FROM quality_inspection")
    public ArrayList<QualityInspection> inspectionList();

    // 根据主键查询质量检验
    @Select("SELECT inspection_id, inspection_no, work_order_id, work_report_id, material_id, batch_no, inspection_type, sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspector_id, inspected_at, remark, created_at, updated_at FROM quality_inspection WHERE inspection_id = #{inspectionId}")
    public QualityInspection getInspectionById(Long inspectionId);

    // 新增质量检验
    @Insert("INSERT INTO quality_inspection (inspection_id, inspection_no, work_order_id, work_report_id, material_id, batch_no, inspection_type, sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspector_id, inspected_at, remark, created_at, updated_at) VALUES (#{inspectionId}, #{inspectionNo}, #{workOrderId}, #{workReportId}, #{materialId}, #{batchNo}, #{inspectionType}, #{sampleQuantity}, #{qualifiedQuantity}, #{unqualifiedQuantity}, #{inspectionResult}, #{inspectorId}, #{inspectedAt}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "inspectionId")
    public void insertInspection(QualityInspection inspection);

    // 修改质量检验
    @Update("UPDATE quality_inspection SET inspection_no=#{inspectionNo}, work_order_id=#{workOrderId}, work_report_id=#{workReportId}, material_id=#{materialId}, batch_no=#{batchNo}, inspection_type=#{inspectionType}, sample_quantity=#{sampleQuantity}, qualified_quantity=#{qualifiedQuantity}, unqualified_quantity=#{unqualifiedQuantity}, inspection_result=#{inspectionResult}, inspector_id=#{inspectorId}, inspected_at=#{inspectedAt}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE inspection_id = #{inspectionId}")
    public void updateInspection(QualityInspection inspection);

    // 删除质量检验
    @Delete("DELETE FROM quality_inspection WHERE inspection_id = #{inspectionId}")
    public void deleteInspection(Long inspectionId);

}
