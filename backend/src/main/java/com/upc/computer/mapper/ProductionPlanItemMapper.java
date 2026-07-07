package com.upc.computer.mapper;

import com.upc.computer.entity.ProductionPlanItem;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface ProductionPlanItemMapper {

    // 查询所有计划明细
    @Select("SELECT plan_item_id, plan_id, order_item_id, material_id, planned_quantity, completed_quantity, planned_start_date, planned_end_date, item_status, created_at, updated_at FROM production_plan_item")
    public ArrayList<ProductionPlanItem> planItemList();

    // 根据主键查询计划明细
    @Select("SELECT plan_item_id, plan_id, order_item_id, material_id, planned_quantity, completed_quantity, planned_start_date, planned_end_date, item_status, created_at, updated_at FROM production_plan_item WHERE plan_item_id = #{planItemId}")
    public ProductionPlanItem getPlanItemById(Long planItemId);

    // 新增计划明细
    @Insert("INSERT INTO production_plan_item (plan_item_id, plan_id, order_item_id, material_id, planned_quantity, completed_quantity, planned_start_date, planned_end_date, item_status, created_at, updated_at) VALUES (#{planItemId}, #{planId}, #{orderItemId}, #{materialId}, #{plannedQuantity}, #{completedQuantity}, #{plannedStartDate}, #{plannedEndDate}, #{itemStatus}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "planItemId")
    public void insertPlanItem(ProductionPlanItem planItem);

    // 修改计划明细
    @Update("UPDATE production_plan_item SET plan_id=#{planId}, order_item_id=#{orderItemId}, material_id=#{materialId}, planned_quantity=#{plannedQuantity}, completed_quantity=#{completedQuantity}, planned_start_date=#{plannedStartDate}, planned_end_date=#{plannedEndDate}, item_status=#{itemStatus}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE plan_item_id = #{planItemId}")
    public void updatePlanItem(ProductionPlanItem planItem);

    // 删除计划明细
    @Delete("DELETE FROM production_plan_item WHERE plan_item_id = #{planItemId}")
    public void deletePlanItem(Long planItemId);

}
