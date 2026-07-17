package com.upc.computer.mapper;

import com.upc.computer.entity.NonconformingProduct;
import org.apache.ibatis.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper
public interface NonconformingProductMapper {

    @Select("SELECT nonconforming_id, nonconforming_no, inspection_id, work_order_id, work_report_id, material_id, batch_no, defect_type, defect_description, quantity, severity, handle_method, handle_status, registered_by, registered_at, handled_by, handled_at, remark, created_at, updated_at FROM nonconforming_product ORDER BY created_at DESC, nonconforming_id DESC")
    ArrayList<NonconformingProduct> nonconformingList();

    @Select("SELECT nonconforming_id, nonconforming_no, inspection_id, work_order_id, work_report_id, material_id, batch_no, defect_type, defect_description, quantity, severity, handle_method, handle_status, registered_by, registered_at, handled_by, handled_at, remark, created_at, updated_at FROM nonconforming_product WHERE nonconforming_id = #{nonconformingId}")
    NonconformingProduct getNonconformingById(Long nonconformingId);

    @Insert("INSERT INTO nonconforming_product (nonconforming_no, inspection_id, work_order_id, work_report_id, material_id, batch_no, defect_type, defect_description, quantity, severity, handle_method, handle_status, registered_by, registered_at, remark, created_at, updated_at) VALUES (#{nonconformingNo}, #{inspectionId}, #{workOrderId}, #{workReportId}, #{materialId}, #{batchNo}, #{defectType}, #{defectDescription}, #{quantity}, #{severity}, #{handleMethod}, #{handleStatus}, #{registeredBy}, #{registeredAt}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "nonconformingId")
    void insertNonconforming(NonconformingProduct nonconforming);

    @Update("UPDATE nonconforming_product SET nonconforming_no=#{nonconformingNo}, inspection_id=#{inspectionId}, work_order_id=#{workOrderId}, work_report_id=#{workReportId}, material_id=#{materialId}, batch_no=#{batchNo}, defect_type=#{defectType}, defect_description=#{defectDescription}, quantity=#{quantity}, severity=#{severity}, handle_method=#{handleMethod}, handle_status=#{handleStatus}, registered_by=#{registeredBy}, registered_at=#{registeredAt}, handled_by=#{handledBy}, handled_at=#{handledAt}, remark=#{remark}, updated_at=#{updatedAt} WHERE nonconforming_id = #{nonconformingId}")
    void updateNonconforming(NonconformingProduct nonconforming);

    @Delete("DELETE FROM nonconforming_product WHERE nonconforming_id = #{nonconformingId}")
    void deleteNonconforming(Long nonconformingId);

    @Select("""
        SELECT
          np.nonconforming_id   AS nonconformingId,
          np.nonconforming_no   AS nonconformingNo,
          np.inspection_id      AS inspectionId,
          wo.work_order_no      AS workOrderNo,
          qi.inspection_no      AS inspectionNo,
          qi.inspection_category AS inspectionCategory,
          m.material_name       AS materialName,
          np.batch_no           AS batchNo,
          np.defect_type        AS defectType,
          np.defect_description AS defectDescription,
          np.quantity           AS quantity,
          np.severity           AS severity,
          np.handle_method      AS handleMethod,
          np.handle_status      AS handleStatus,
          np.remark             AS remark,
          np.registered_at      AS registeredAt,
          np.handled_at         AS handledAt
        FROM nonconforming_product np
        LEFT JOIN quality_inspection qi ON qi.inspection_id = np.inspection_id
        LEFT JOIN work_order wo         ON wo.work_order_id = np.work_order_id
        LEFT JOIN material m            ON m.material_id = np.material_id
        ORDER BY np.created_at DESC
        """)
    List<Map<String, Object>> listNonconformingViews();

    @Select("SELECT nonconforming_id, nonconforming_no, inspection_id, work_order_id, material_id, batch_no, defect_type, defect_description, quantity, severity, handle_method, handle_status, remark, registered_at, handled_at, created_at, updated_at FROM nonconforming_product WHERE inspection_id = #{inspectionId}")
    List<NonconformingProduct> listByInspectionId(Long inspectionId);
}
