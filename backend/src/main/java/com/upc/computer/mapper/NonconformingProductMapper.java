package com.upc.computer.mapper;

import com.upc.computer.entity.NonconformingProduct;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface NonconformingProductMapper {

    // 查询所有不合格品
    @Select("SELECT nonconforming_id, nonconforming_no, inspection_id, work_order_id, work_report_id, material_id, batch_no, defect_type, defect_description, quantity, severity, handle_method, handle_status, registered_by, registered_at, handled_by, handled_at, remark, created_at, updated_at FROM nonconforming_product")
    public ArrayList<NonconformingProduct> nonconformingList();

    // 根据主键查询不合格品
    @Select("SELECT nonconforming_id, nonconforming_no, inspection_id, work_order_id, work_report_id, material_id, batch_no, defect_type, defect_description, quantity, severity, handle_method, handle_status, registered_by, registered_at, handled_by, handled_at, remark, created_at, updated_at FROM nonconforming_product WHERE nonconforming_id = #{nonconformingId}")
    public NonconformingProduct getNonconformingById(Long nonconformingId);

    // 新增不合格品
    @Insert("INSERT INTO nonconforming_product (nonconforming_id, nonconforming_no, inspection_id, work_order_id, work_report_id, material_id, batch_no, defect_type, defect_description, quantity, severity, handle_method, handle_status, registered_by, registered_at, handled_by, handled_at, remark, created_at, updated_at) VALUES (#{nonconformingId}, #{nonconformingNo}, #{inspectionId}, #{workOrderId}, #{workReportId}, #{materialId}, #{batchNo}, #{defectType}, #{defectDescription}, #{quantity}, #{severity}, #{handleMethod}, #{handleStatus}, #{registeredBy}, #{registeredAt}, #{handledBy}, #{handledAt}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "nonconformingId")
    public void insertNonconforming(NonconformingProduct nonconforming);

    // 修改不合格品
    @Update("UPDATE nonconforming_product SET nonconforming_no=#{nonconformingNo}, inspection_id=#{inspectionId}, work_order_id=#{workOrderId}, work_report_id=#{workReportId}, material_id=#{materialId}, batch_no=#{batchNo}, defect_type=#{defectType}, defect_description=#{defectDescription}, quantity=#{quantity}, severity=#{severity}, handle_method=#{handleMethod}, handle_status=#{handleStatus}, registered_by=#{registeredBy}, registered_at=#{registeredAt}, handled_by=#{handledBy}, handled_at=#{handledAt}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE nonconforming_id = #{nonconformingId}")
    public void updateNonconforming(NonconformingProduct nonconforming);

    // 删除不合格品
    @Delete("DELETE FROM nonconforming_product WHERE nonconforming_id = #{nonconformingId}")
    public void deleteNonconforming(Long nonconformingId);

}
