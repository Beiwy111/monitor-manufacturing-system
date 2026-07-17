package com.upc.computer.ai.action;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AgentActionPlan(
        String planId,
        String actionCode,
        String title,
        String module,
        Long userId,
        String username,
        String roleCode,
        String sessionId,
        Map<String, Object> parameters,
        List<String> affectedResources,
        String snapshotHash,
        AgentPlanStatus status,
        Object result,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime executedAt
) {
    public AgentActionPlan withStatus(AgentPlanStatus nextStatus, Object nextResult,
                                      String nextError, LocalDateTime nextExecutedAt) {
        return new AgentActionPlan(planId, actionCode, title, module, userId, username, roleCode,
                sessionId, parameters, affectedResources, snapshotHash, nextStatus, nextResult,
                nextError, createdAt, expiresAt, nextExecutedAt);
    }
}
