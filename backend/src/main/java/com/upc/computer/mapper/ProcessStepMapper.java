package com.upc.computer.mapper;

import com.upc.computer.entity.ProcessStep;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface ProcessStepMapper {

    // 查询所有工序
    @Select("SELECT step_id, route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status, created_at, updated_at FROM process_step")
    public ArrayList<ProcessStep> stepList();

    // 根据主键查询工序
    @Select("SELECT step_id, route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status, created_at, updated_at FROM process_step WHERE step_id = #{stepId}")
    public ProcessStep getStepById(Long stepId);

    @Select("SELECT step_id, route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status, created_at, updated_at FROM process_step WHERE route_id = #{routeId} ORDER BY step_no")
    public ArrayList<ProcessStep> listByRouteId(Long routeId);

    // 新增工序
    @Insert("INSERT INTO process_step (step_id, route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status, created_at, updated_at) VALUES (#{stepId}, #{routeId}, #{stepNo}, #{stepCode}, #{stepName}, #{standardWorkHours}, #{standardEquipmentType}, #{qualityRequired}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "stepId")
    public void insertStep(ProcessStep step);

    // 修改工序
    @Update("UPDATE process_step SET route_id=#{routeId}, step_no=#{stepNo}, step_code=#{stepCode}, step_name=#{stepName}, standard_work_hours=#{standardWorkHours}, standard_equipment_type=#{standardEquipmentType}, quality_required=#{qualityRequired}, status=#{status}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE step_id = #{stepId}")
    public void updateStep(ProcessStep step);

    // 删除工序
    @Delete("DELETE FROM process_step WHERE step_id = #{stepId}")
    public void deleteStep(Long stepId);

    @Update("UPDATE process_step SET status = 0, updated_at = NOW() WHERE step_id = #{stepId}")
    public void disableStep(Long stepId);

    @Update("UPDATE process_step SET status = 0, updated_at = NOW() WHERE route_id = #{routeId}")
    public void disableByRouteId(Long routeId);

}
