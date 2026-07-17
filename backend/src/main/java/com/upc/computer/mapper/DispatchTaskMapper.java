package com.upc.computer.mapper;

import com.upc.computer.entity.DispatchTask;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface DispatchTaskMapper {

    // 查询所有派工任务
    @Select("SELECT dispatch_id, dispatch_no, work_order_id, step_id, operator_id, equipment_id, assigned_quantity, accepted_quantity, completed_quantity, assigned_by, assigned_at, accepted_at, status, remark, created_at, updated_at FROM dispatch_task ORDER BY created_at DESC, dispatch_id DESC")
    public ArrayList<DispatchTask> dispatchList();

    // 根据主键查询派工任务
    @Select("SELECT dispatch_id, dispatch_no, work_order_id, step_id, operator_id, equipment_id, assigned_quantity, accepted_quantity, completed_quantity, assigned_by, assigned_at, accepted_at, status, remark, created_at, updated_at FROM dispatch_task WHERE dispatch_id = #{dispatchId}")
    public DispatchTask getDispatchById(Long dispatchId);

    // 新增派工任务
    @Insert("INSERT INTO dispatch_task (dispatch_id, dispatch_no, work_order_id, step_id, operator_id, equipment_id, assigned_quantity, accepted_quantity, completed_quantity, assigned_by, assigned_at, accepted_at, status, remark, created_at, updated_at) VALUES (#{dispatchId}, #{dispatchNo}, #{workOrderId}, #{stepId}, #{operatorId}, #{equipmentId}, #{assignedQuantity}, #{acceptedQuantity}, #{completedQuantity}, #{assignedBy}, #{assignedAt}, #{acceptedAt}, #{status}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "dispatchId")
    public void insertDispatch(DispatchTask dispatch);

    // 修改派工任务
    @Update("UPDATE dispatch_task SET dispatch_no=#{dispatchNo}, work_order_id=#{workOrderId}, step_id=#{stepId}, operator_id=#{operatorId}, equipment_id=#{equipmentId}, assigned_quantity=#{assignedQuantity}, accepted_quantity=#{acceptedQuantity}, completed_quantity=#{completedQuantity}, assigned_by=#{assignedBy}, assigned_at=#{assignedAt}, accepted_at=#{acceptedAt}, status=#{status}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE dispatch_id = #{dispatchId}")
    public void updateDispatch(DispatchTask dispatch);

    // 删除派工任务
    @Delete("DELETE FROM dispatch_task WHERE dispatch_id = #{dispatchId}")
    public void deleteDispatch(Long dispatchId);

}
