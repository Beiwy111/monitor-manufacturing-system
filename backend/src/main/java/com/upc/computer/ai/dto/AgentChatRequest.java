package com.upc.computer.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * MES Agent 统一聊天请求。
 */
public record AgentChatRequest(
        @NotBlank(message = "消息不能为空")
        @Size(max = 2000, message = "消息不能超过 2000 个字符")
        String message,
        @Size(max = 64, message = "会话编号不能超过 64 个字符")
        String sessionId,
        @Size(max = 20, message = "最多携带 20 条历史消息")
        List<AgentConversationMessage> conversation
) {
    public AgentChatRequest(String message, String sessionId) {
        this(message, sessionId, List.of());
    }
}
