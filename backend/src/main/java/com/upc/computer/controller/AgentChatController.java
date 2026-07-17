package com.upc.computer.controller;

import com.upc.computer.ai.dto.AgentChatRequest;
import com.upc.computer.ai.dto.AgentChatResponse;
import com.upc.computer.ai.dto.AgentPlanConfirmRequest;
import com.upc.computer.ai.action.AgentActionPlanService;
import com.upc.computer.ai.service.AgentService;
import com.upc.computer.common.BusinessException;
import com.upc.computer.common.JwtUtil;
import com.upc.computer.common.Result;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Spring AI MES Agent 统一入口。
 */
@RestController
@RequestMapping("/agent")
@ConditionalOnProperty(prefix = "mes.ai", name = "enabled", havingValue = "true")
public class AgentChatController {

    private final AgentService agentService;
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final AgentActionPlanService actionPlanService;

    @Autowired
    public AgentChatController(AgentService agentService, AuthService authService, JwtUtil jwtUtil,
                               AgentActionPlanService actionPlanService) {
        this.agentService = agentService;
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.actionPlanService = actionPlanService;
    }

    public AgentChatController(AgentService agentService, AuthService authService, JwtUtil jwtUtil) {
        this(agentService, authService, jwtUtil, null);
    }

    @PostMapping("/chat")
    public Result<AgentChatResponse> chat(
            @Valid @RequestBody AgentChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(agentService.chat(request, requireLoginSession(authorization)));
    }

    @PostMapping("/plans/{planId}/confirm")
    public Result<Map<String, Object>> confirm(
            @PathVariable String planId,
            @Valid @RequestBody AgentPlanConfirmRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (actionPlanService == null) throw new BusinessException(503, "Agent 写操作服务未启用");
        return Result.success(actionPlanService.confirm(planId, request.decision(), request.finalParams(),
                requireLoginSession(authorization)));
    }

    private LoginResponse requireLoginSession(String authorization) {
        String token = jwtUtil.extractTokenFromHeader(authorization);
        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "未登录或令牌无效");
        }
        LoginResponse session = authService.getLoginSession(token);
        if (session == null) {
            throw new BusinessException(401, "登录已失效，请重新登录");
        }
        return session;
    }
}
