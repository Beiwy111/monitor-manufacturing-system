package com.upc.computer.mapper;

import com.upc.computer.entity.QualityInspection;
import org.apache.ibatis.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper
public interface QualityInspectionMapper {

    @Select("SELECT inspection_id, inspection_no, work_order_id, work_report_id, material_id, batch_no, inspection_type, sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status, inspection_category, inspector_id, inspected_at, remark, created_at, updated_at FROM quality_inspection ORDER BY created_at DESC")
    ArrayList<QualityInspection> inspectionList();

    @Select("SELECT inspection_id, inspection_no, work_order_id, work_report_id, material_id, batch_no, inspection_type, sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status, inspection_category, inspector_id, inspected_at, remark, created_at, updated_at FROM quality_inspection WHERE inspection_id = #{inspectionId}")
    QualityInspection getInspectionById(Long inspectionId);

    @Insert("INSERT INTO quality_inspection (inspection_no, work_order_id, work_report_id, material_id, batch_no, inspection_type, sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status, inspection_category, inspector_id, inspected_at, remark, created_at, updated_at) VALUES (#{inspectionNo}, #{workOrderId}, #{workReportId}, #{materialId}, #{batchNo}, #{inspectionType}, #{sampleQuantity}, #{qualifiedQuantity}, #{unqualifiedQuantity}, #{inspectionResult}, #{inspectionStatus}, #{inspectionCategory}, #{inspectorId}, #{inspectedAt}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "inspectionId")
    void insertInspection(QualityInspection inspection);

    @Update("UPDATE quality_inspection SET inspection_no=#{inspectionNo}, work_order_id=#{workOrderId}, work_report_id=#{workReportId}, material_id=#{materialId}, batch_no=#{batchNo}, inspection_type=#{inspectionType}, sample_quantity=#{sampleQuantity}, qualified_quantity=#{qualifiedQuantity}, unqualified_quantity=#{unqualifiedQuantity}, inspection_result=#{inspectionResult}, inspection_status=#{inspectionStatus}, inspection_category=#{inspectionCategory}, inspector_id=#{inspectorId}, inspected_at=#{inspectedAt}, remark=#{remark}, updated_at=#{updatedAt} WHERE inspection_id = #{inspectionId}")
    void updateInspection(QualityInspection inspection);

    @Delete("DELETE FROM quality_inspection WHERE inspection_id = #{inspectionId}")
    void deleteInspection(Long inspectionId);

    // 列表视图 —— 用子查询避免 JOIN user 表权限问题，inspectorName 在 Service 层组装
    @Select("SELECT inspection_id AS inspectionId, inspection_no AS inspectionNo, work_order_id AS workOrderId, work_report_id AS workReportId, material_id AS materialId, batch_no AS batchNo, inspection_type AS inspectionType, inspection_status AS inspectionStatus, inspection_category AS inspectionCategory, sample_quantity AS sampleQuantity, qualified_quantity AS qualifiedQuantity, unqualified_quantity AS unqualifiedQuantity, inspection_result AS inspectionResult, inspector_id AS inspectorId, inspected_at AS inspectedAt, remark AS remark, created_at AS createdAt, updated_at AS updatedAt FROM quality_inspection ORDER BY created_at DESC")
    List<Map<String, Object>> listInspectionViews();

    @Select("SELECT inspection_id AS inspectionId, inspection_no AS inspectionNo, work_order_id AS workOrderId, work_report_id AS workReportId, material_id AS materialId, batch_no AS batchNo, inspection_type AS inspectionType, inspection_status AS inspectionStatus, inspection_category AS inspectionCategory, sample_quantity AS sampleQuantity, qualified_quantity AS qualifiedQuantity, unqualified_quantity AS unqualifiedQuantity, inspection_result AS inspectionResult, inspector_id AS inspectorId, inspected_at AS inspectedAt, remark AS remark, updated_at AS updatedAt FROM quality_inspection WHERE inspection_status = 'RECHECK_REQUIRED' ORDER BY updated_at DESC")
    List<Map<String, Object>> listRecheckViews();

    @Select("SELECT inspection_id AS inspectionId, inspection_no AS inspectionNo, work_order_id AS workOrderId, work_report_id AS workReportId, material_id AS materialId, batch_no AS batchNo, inspection_type AS inspectionType, inspection_status AS inspectionStatus, inspection_category AS inspectionCategory, sample_quantity AS sampleQuantity, qualified_quantity AS qualifiedQuantity, unqualified_quantity AS unqualifiedQuantity, inspection_result AS inspectionResult, inspector_id AS inspectorId, inspected_at AS inspectedAt, remark AS remark, updated_at AS updatedAt FROM quality_inspection WHERE inspection_id = #{inspectionId}")
    Map<String, Object> getInspectionDetailView(Long inspectionId);

    @Select("SELECT COUNT(*) AS total, SUM(CASE WHEN inspection_status='PENDING' THEN 1 ELSE 0 END) AS pending, SUM(CASE WHEN inspection_status='PASSED' THEN 1 ELSE 0 END) AS passed, SUM(CASE WHEN inspection_status='FAILED' THEN 1 ELSE 0 END) AS failed, SUM(CASE WHEN inspection_status='RECHECK_REQUIRED' THEN 1 ELSE 0 END) AS recheck, SUM(CASE WHEN inspection_category='SEMI_FINISHED' THEN 1 ELSE 0 END) AS semiFinished, SUM(CASE WHEN inspection_category='FINISHED_PRODUCT' THEN 1 ELSE 0 END) AS finishedProduct FROM quality_inspection")
    Map<String, Object> inspectionKpi();
}
