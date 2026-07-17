package com.upc.computer.ai.dto;

import java.util.Map;

/**
 * MES Agent 统一聊天响应。
 */
public record AgentChatResponse(
        String sessionId,
        String reply,
        String username,
        String roleCode,
        String model,
        Map<String, Object> action
) {
    public AgentChatResponse(String sessionId, String reply, String username, String roleCode, String model) {
        this(sessionId, reply, username, roleCode, model, null);
    }
}
