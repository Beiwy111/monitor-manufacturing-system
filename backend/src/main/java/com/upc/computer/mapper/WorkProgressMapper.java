package com.upc.computer.mapper;

import com.upc.computer.entity.WorkProgress;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface WorkProgressMapper {

    // 查询所有生产进度
    @Select("SELECT progress_id, work_order_id, dispatch_id, progress_date, progress_percent, completed_quantity, current_status, progress_description, recorded_by, recorded_at FROM work_progress")
    public ArrayList<WorkProgress> progressList();

    // 根据主键查询生产进度
    @Select("SELECT progress_id, work_order_id, dispatch_id, progress_date, progress_percent, completed_quantity, current_status, progress_description, recorded_by, recorded_at FROM work_progress WHERE progress_id = #{progressId}")
    public WorkProgress getProgressById(Long progressId);

    // 新增生产进度
    @Insert("INSERT INTO work_progress (progress_id, work_order_id, dispatch_id, progress_date, progress_percent, completed_quantity, current_status, progress_description, recorded_by, recorded_at) VALUES (#{progressId}, #{workOrderId}, #{dispatchId}, #{progressDate}, #{progressPercent}, #{completedQuantity}, #{currentStatus}, #{progressDescription}, #{recordedBy}, #{recordedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "progressId")
    public void insertProgress(WorkProgress progress);

    // 修改生产进度
    @Update("UPDATE work_progress SET work_order_id=#{workOrderId}, dispatch_id=#{dispatchId}, progress_date=#{progressDate}, progress_percent=#{progressPercent}, completed_quantity=#{completedQuantity}, current_status=#{currentStatus}, progress_description=#{progressDescription}, recorded_by=#{recordedBy}, recorded_at=#{recordedAt} WHERE progress_id = #{progressId}")
    public void updateProgress(WorkProgress progress);

    // 删除生产进度
    @Delete("DELETE FROM work_progress WHERE progress_id = #{progressId}")
    public void deleteProgress(Long progressId);

}
