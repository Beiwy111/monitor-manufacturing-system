package com.upc.computer.mapper;

import org.apache.ibatis.annotations.*;

import java.util.Map;

/**
 * Agent 流程审计 Mapper，复用 agent_flow.sql 的 agent_flow_run / agent_flow_step 两表。
 * 语音助手每条写指令 = 一条 run + 一条 step（提议→闸门→执行全程留痕）。
 * 说明：JSON 列以合法 JSON 字符串写入，MySQL 自动转换；调用方对建表缺失做 best-effort 容错。
 */
@Mapper
public interface AgentFlowMapper {

    @Insert("INSERT INTO agent_flow_run (flow_no, template_code, goal, status, current_step_no, context_json, created_by) " +
            "VALUES (#{flowNo}, #{templateCode}, #{goal}, #{status}, #{currentStepNo}, #{contextJson}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "flowId")
    void insertRun(Map<String, Object> run);

    @Insert("INSERT INTO agent_flow_step (flow_id, step_no, module, action_code, title, status, reason, proposal_json) " +
            "VALUES (#{flowId}, #{stepNo}, #{module}, #{actionCode}, #{title}, #{status}, #{reason}, #{proposalJson})")
    @Options(useGeneratedKeys = true, keyProperty = "stepId")
    void insertStep(Map<String, Object> step);

    @Select("SELECT step_id AS stepId, flow_id AS flowId, module, action_code AS actionCode, status, " +
            "proposal_json AS proposalJson, final_params_json AS finalParamsJson FROM agent_flow_step WHERE step_id = #{stepId}")
    Map<String, Object> getStep(Long stepId);

    @Update("UPDATE agent_flow_step SET status = #{status}, decision = #{decision}, decision_by = #{decisionBy}, " +
            "decision_at = NOW(), final_params_json = #{finalParamsJson}, result_json = #{resultJson}, error_msg = #{errorMsg} " +
            "WHERE step_id = #{stepId}")
    void updateStepDecision(Map<String, Object> step);
}
