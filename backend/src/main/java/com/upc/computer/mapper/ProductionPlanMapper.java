package com.upc.computer.mapper;

import com.upc.computer.entity.ProductionPlan;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProductionPlanMapper {

    @Select("SELECT plan_id, plan_no, plan_name, source_order_id, planned_start_date, planned_end_date, priority, plan_status, planner_id, approved_by, approved_at, remark, version_no, parent_plan_no, adjust_reason, scheduling_mode, created_at, updated_at FROM production_plan ORDER BY created_at DESC, plan_id DESC")
    ArrayList<ProductionPlan> planList();

    @Select("SELECT plan_id, plan_no, plan_name, source_order_id, planned_start_date, planned_end_date, priority, plan_status, planner_id, approved_by, approved_at, remark, version_no, parent_plan_no, adjust_reason, scheduling_mode, created_at, updated_at FROM production_plan WHERE plan_id = #{planId}")
    ProductionPlan getPlanById(Long planId);

    @Insert("INSERT INTO production_plan (plan_id, plan_no, plan_name, source_order_id, planned_start_date, planned_end_date, priority, plan_status, planner_id, approved_by, approved_at, remark, version_no, parent_plan_no, adjust_reason, scheduling_mode, created_at, updated_at) VALUES (#{planId}, #{planNo}, #{planName}, #{sourceOrderId}, #{plannedStartDate}, #{plannedEndDate}, #{priority}, #{planStatus}, #{plannerId}, #{approvedBy}, #{approvedAt}, #{remark}, #{versionNo}, #{parentPlanNo}, #{adjustReason}, #{schedulingMode}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "planId")
    void insertPlan(ProductionPlan plan);

    @Update("UPDATE production_plan SET plan_no=#{planNo}, plan_name=#{planName}, source_order_id=#{sourceOrderId}, planned_start_date=#{plannedStartDate}, planned_end_date=#{plannedEndDate}, priority=#{priority}, plan_status=#{planStatus}, planner_id=#{plannerId}, approved_by=#{approvedBy}, approved_at=#{approvedAt}, remark=#{remark}, version_no=#{versionNo}, parent_plan_no=#{parentPlanNo}, adjust_reason=#{adjustReason}, scheduling_mode=#{schedulingMode}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE plan_id = #{planId}")
    void updatePlan(ProductionPlan plan);

    @Delete("DELETE FROM production_plan WHERE plan_id = #{planId}")
    void deletePlan(Long planId);
}
