package com.upc.computer.mapper;

import com.upc.computer.entity.WorkReport;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface WorkReportMapper {

    // 查询所有报工
    @Select("SELECT report_id, report_no, work_order_id, dispatch_id, step_id, operator_id, equipment_id, report_date, start_time, end_time, completed_quantity, qualified_quantity, unqualified_quantity, work_hours, report_status, confirmed_by, confirmed_at, remark, created_at, updated_at FROM work_report")
    public ArrayList<WorkReport> reportList();

    // 根据主键查询报工
    @Select("SELECT report_id, report_no, work_order_id, dispatch_id, step_id, operator_id, equipment_id, report_date, start_time, end_time, completed_quantity, qualified_quantity, unqualified_quantity, work_hours, report_status, confirmed_by, confirmed_at, remark, created_at, updated_at FROM work_report WHERE report_id = #{reportId}")
    public WorkReport getReportById(Long reportId);

    // 新增报工
    @Insert("INSERT INTO work_report (report_id, report_no, work_order_id, dispatch_id, step_id, operator_id, equipment_id, report_date, start_time, end_time, completed_quantity, qualified_quantity, unqualified_quantity, work_hours, report_status, confirmed_by, confirmed_at, remark, created_at, updated_at) VALUES (#{reportId}, #{reportNo}, #{workOrderId}, #{dispatchId}, #{stepId}, #{operatorId}, #{equipmentId}, #{reportDate}, #{startTime}, #{endTime}, #{completedQuantity}, #{qualifiedQuantity}, #{unqualifiedQuantity}, #{workHours}, #{reportStatus}, #{confirmedBy}, #{confirmedAt}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "reportId")
    public void insertReport(WorkReport report);

    // 修改报工
    @Update("UPDATE work_report SET report_no=#{reportNo}, work_order_id=#{workOrderId}, dispatch_id=#{dispatchId}, step_id=#{stepId}, operator_id=#{operatorId}, equipment_id=#{equipmentId}, report_date=#{reportDate}, start_time=#{startTime}, end_time=#{endTime}, completed_quantity=#{completedQuantity}, qualified_quantity=#{qualifiedQuantity}, unqualified_quantity=#{unqualifiedQuantity}, work_hours=#{workHours}, report_status=#{reportStatus}, confirmed_by=#{confirmedBy}, confirmed_at=#{confirmedAt}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE report_id = #{reportId}")
    public void updateReport(WorkReport report);

    // 删除报工
    @Delete("DELETE FROM work_report WHERE report_id = #{reportId}")
    public void deleteReport(Long reportId);

}
