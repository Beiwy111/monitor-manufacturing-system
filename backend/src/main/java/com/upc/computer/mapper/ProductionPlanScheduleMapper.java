package com.upc.computer.mapper;

import com.upc.computer.entity.ProductionPlanSchedule;
import org.apache.ibatis.annotations.*;

import java.util.ArrayList;

@Mapper
public interface ProductionPlanScheduleMapper {

    @Select("SELECT schedule_id, plan_id, step_id, step_no, step_name, workshop, equipment_id, equipment_code, planned_quantity, planned_start, planned_end, standard_hours, sort_no, created_at, updated_at FROM production_plan_schedule WHERE plan_id = #{planId} ORDER BY sort_no, step_no")
    ArrayList<ProductionPlanSchedule> listByPlanId(Long planId);

    @Insert("INSERT INTO production_plan_schedule (plan_id, step_id, step_no, step_name, workshop, equipment_id, equipment_code, planned_quantity, planned_start, planned_end, standard_hours, sort_no, created_at, updated_at) VALUES (#{planId}, #{stepId}, #{stepNo}, #{stepName}, #{workshop}, #{equipmentId}, #{equipmentCode}, #{plannedQuantity}, #{plannedStart}, #{plannedEnd}, #{standardHours}, #{sortNo}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "scheduleId")
    void insertSchedule(ProductionPlanSchedule schedule);

    @Delete("DELETE FROM production_plan_schedule WHERE plan_id = #{planId}")
    void deleteByPlanId(Long planId);
}
