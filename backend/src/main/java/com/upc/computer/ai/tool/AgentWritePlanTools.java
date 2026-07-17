package com.upc.computer.ai.tool;

import com.upc.computer.ai.action.AgentActionPlanService;
import com.upc.computer.ai.action.AgentPlanOutcome;
import com.upc.computer.dto.LoginResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Map;

/** 每次聊天请求创建一个实例，使模型只能为当前服务端会话生成待确认方案。 */
public class AgentWritePlanTools {

    private final AgentActionPlanService planService;
    private final LoginResponse session;
    private final String sessionId;
    private AgentPlanOutcome latestOutcome;

    public AgentWritePlanTools(AgentActionPlanService planService, LoginResponse session, String sessionId) {
        this.planService = planService;
        this.session = session;
        this.sessionId = sessionId;
    }

    @Tool(name = "agent_list_allowed_write_actions",
            description = "查询当前登录角色可以通过智能助手规划的新增、修改、删除、提交和状态流转动作，以及每个动作的必填参数")
    public Object listAllowedWriteActions() {
        return planService.allowedActions(session);
    }

    @Tool(name = "agent_prepare_write_action",
            description = "为当前角色准备一个业务写操作。该工具只校验和保存待确认方案，不会修改生产数据。信息不全时返回 missingFields，必须向用户逐项询问；信息完整时返回确认方案。actionCode 必须来自 agent_list_allowed_write_actions，parameters 必须使用动作目录中的字段名")
    public AgentPlanOutcome prepareWriteAction(
            @ToolParam(description = "严格白名单中的动作编码") String actionCode,
            @ToolParam(description = "动作参数对象；多轮补充时只需传本轮新增字段") Map<String, Object> parameters) {
        latestOutcome = planService.preview(actionCode, parameters, session, sessionId);
        return latestOutcome;
    }

    public AgentPlanOutcome latestOutcome() {
        return latestOutcome;
    }
}
