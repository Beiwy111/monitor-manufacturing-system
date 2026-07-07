package com.upc.computer.mapper;

import com.upc.computer.entity.ProductionPlan;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface ProductionPlanMapper {

    // 查询所有生产计划
    @Select("SELECT plan_id, plan_no, plan_name, source_order_id, planned_start_date, planned_end_date, priority, plan_status, planner_id, approved_by, approved_at, remark, created_at, updated_at FROM production_plan")
    public ArrayList<ProductionPlan> planList();

    // 根据主键查询生产计划
    @Select("SELECT plan_id, plan_no, plan_name, source_order_id, planned_start_date, planned_end_date, priority, plan_status, planner_id, approved_by, approved_at, remark, created_at, updated_at FROM production_plan WHERE plan_id = #{planId}")
    public ProductionPlan getPlanById(Long planId);

    // 新增生产计划
    @Insert("INSERT INTO production_plan (plan_id, plan_no, plan_name, source_order_id, planned_start_date, planned_end_date, priority, plan_status, planner_id, approved_by, approved_at, remark, created_at, updated_at) VALUES (#{planId}, #{planNo}, #{planName}, #{sourceOrderId}, #{plannedStartDate}, #{plannedEndDate}, #{priority}, #{planStatus}, #{plannerId}, #{approvedBy}, #{approvedAt}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "planId")
    public void insertPlan(ProductionPlan plan);

    // 修改生产计划
    @Update("UPDATE production_plan SET plan_no=#{planNo}, plan_name=#{planName}, source_order_id=#{sourceOrderId}, planned_start_date=#{plannedStartDate}, planned_end_date=#{plannedEndDate}, priority=#{priority}, plan_status=#{planStatus}, planner_id=#{plannerId}, approved_by=#{approvedBy}, approved_at=#{approvedAt}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE plan_id = #{planId}")
    public void updatePlan(ProductionPlan plan);

    // 删除生产计划
    @Delete("DELETE FROM production_plan WHERE plan_id = #{planId}")
    public void deletePlan(Long planId);

}
