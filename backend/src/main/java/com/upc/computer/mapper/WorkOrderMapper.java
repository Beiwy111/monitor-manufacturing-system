package com.upc.computer.mapper;

import com.upc.computer.entity.WorkOrder;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface WorkOrderMapper {

    // 查询所有工单
    @Select("SELECT work_order_id, work_order_no, plan_id, plan_item_id, material_id, route_id, planned_quantity, completed_quantity, qualified_quantity, unqualified_quantity, planned_start_time, planned_end_time, actual_start_time, actual_end_time, status, created_by, released_by, released_at, remark, created_at, updated_at FROM work_order")
    public ArrayList<WorkOrder> workOrderList();

    // 根据主键查询工单
    @Select("SELECT work_order_id, work_order_no, plan_id, plan_item_id, material_id, route_id, planned_quantity, completed_quantity, qualified_quantity, unqualified_quantity, planned_start_time, planned_end_time, actual_start_time, actual_end_time, status, created_by, released_by, released_at, remark, created_at, updated_at FROM work_order WHERE work_order_id = #{workOrderId}")
    public WorkOrder getWorkOrderById(Long workOrderId);

    // 新增工单
    @Insert("INSERT INTO work_order (work_order_id, work_order_no, plan_id, plan_item_id, material_id, route_id, planned_quantity, completed_quantity, qualified_quantity, unqualified_quantity, planned_start_time, planned_end_time, actual_start_time, actual_end_time, status, created_by, released_by, released_at, remark, created_at, updated_at) VALUES (#{workOrderId}, #{workOrderNo}, #{planId}, #{planItemId}, #{materialId}, #{routeId}, #{plannedQuantity}, #{completedQuantity}, #{qualifiedQuantity}, #{unqualifiedQuantity}, #{plannedStartTime}, #{plannedEndTime}, #{actualStartTime}, #{actualEndTime}, #{status}, #{createdBy}, #{releasedBy}, #{releasedAt}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "workOrderId")
    public void insertWorkOrder(WorkOrder workOrder);

    // 修改工单
    @Update("UPDATE work_order SET work_order_no=#{workOrderNo}, plan_id=#{planId}, plan_item_id=#{planItemId}, material_id=#{materialId}, route_id=#{routeId}, planned_quantity=#{plannedQuantity}, completed_quantity=#{completedQuantity}, qualified_quantity=#{qualifiedQuantity}, unqualified_quantity=#{unqualifiedQuantity}, planned_start_time=#{plannedStartTime}, planned_end_time=#{plannedEndTime}, actual_start_time=#{actualStartTime}, actual_end_time=#{actualEndTime}, status=#{status}, created_by=#{createdBy}, released_by=#{releasedBy}, released_at=#{releasedAt}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE work_order_id = #{workOrderId}")
    public void updateWorkOrder(WorkOrder workOrder);

    // 删除工单
    @Delete("DELETE FROM work_order WHERE work_order_id = #{workOrderId}")
    public void deleteWorkOrder(Long workOrderId);

    @Select("SELECT work_order_id, work_order_no, plan_id, plan_item_id, material_id, route_id, planned_quantity, completed_quantity, qualified_quantity, unqualified_quantity, planned_start_time, planned_end_time, actual_start_time, actual_end_time, status, created_by, released_by, released_at, remark, created_at, updated_at FROM work_order WHERE status IN ('RUNNING','RELEASED')")
    ArrayList<WorkOrder> listActiveWorkOrders();

    @Update("UPDATE work_order SET completed_quantity=#{completedQuantity}, qualified_quantity=#{qualifiedQuantity}, unqualified_quantity=#{unqualifiedQuantity}, status=#{status}, updated_at=NOW() WHERE work_order_id=#{workOrderId}")
    void updateProductionProgress(WorkOrder workOrder);

}
