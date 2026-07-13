package com.upc.computer.mapper;

import com.upc.computer.entity.ProductionPlanHistory;
import org.apache.ibatis.annotations.*;

import java.util.ArrayList;

@Mapper
public interface ProductionPlanHistoryMapper {

    @Select("SELECT history_id, plan_id, plan_no, version_no, action_type, reason, snapshot_json, operator_name, created_at FROM production_plan_history WHERE plan_no = #{planNo} ORDER BY created_at DESC")
    ArrayList<ProductionPlanHistory> listByPlanNo(String planNo);

    @Insert("INSERT INTO production_plan_history (plan_id, plan_no, version_no, action_type, reason, snapshot_json, operator_name, created_at) VALUES (#{planId}, #{planNo}, #{versionNo}, #{actionType}, #{reason}, #{snapshotJson}, #{operatorName}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "historyId")
    void insertHistory(ProductionPlanHistory history);
}
