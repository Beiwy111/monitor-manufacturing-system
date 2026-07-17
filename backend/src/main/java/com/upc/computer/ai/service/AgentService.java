package com.upc.computer.ai.service;

import com.upc.computer.ai.config.AiProperties;
import com.upc.computer.ai.dto.AgentChatRequest;
import com.upc.computer.ai.dto.AgentChatResponse;
import com.upc.computer.ai.dto.AgentConversationMessage;
import com.upc.computer.ai.tool.AgentToolRegistry;
import com.upc.computer.ai.tool.AgentWritePlanTools;
import com.upc.computer.ai.action.AgentPlanOutcome;
import com.upc.computer.common.BusinessException;
import com.upc.computer.dto.LoginResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

/**
 * Spring AI Agent 编排服务。模型只能看到按当前角色分配的业务 Tool。
 */
@Service
@ConditionalOnProperty(prefix = "mes.ai", name = "enabled", havingValue = "true")
public class AgentService {

    static final String SYSTEM_PROMPT = "你是电脑显示器制造 MES 智能助手。"
            + "你需要理解用户的自然语言需求，并调用系统提供的工具完成操作。"
            + "实时数据必须通过工具查询，不得编造数据。"
            + "只能调用已有工具，不得执行 SQL、Shell 或代码。"
            + "工具执行失败时必须明确说明失败原因。";

    private final ChatClient chatClient;
    private final AgentToolRegistry toolRegistry;
    private final AiProperties properties;

    public AgentService(ChatClient chatClient, AgentToolRegistry toolRegistry, AiProperties properties) {
        this.chatClient = chatClient;
        this.toolRegistry = toolRegistry;
        this.properties = properties;
    }

    public AgentChatResponse chat(AgentChatRequest request, LoginResponse session) {
        if (session == null || session.getUserId() == null) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }

        String sessionId = normalizeSessionId(request.sessionId());
        AgentToolRegistry.AgentToolSet toolSet = toolRegistry.toolSetFor(session, sessionId);
        if (toolSet == null) {
            toolSet = new AgentToolRegistry.AgentToolSet(toolRegistry.toolsFor(session), null);
        }
        Object[] tools = toolSet.tools();
        String rolePrompt = SYSTEM_PROMPT
                + "\n当前登录用户：" + safe(session.getRealName(), session.getUsername())
                + "，角色：" + safe(session.getRoleName(), session.getRoleCode()) + "（" + session.getRoleCode() + "）。"
                + "只能使用当前请求提供的工具；如果没有对应工具，请明确告知用户当前角色不支持该操作。"
                + "查询工具可直接调用。写操作只能调用 agent_prepare_write_action 生成待确认方案，绝不能声称已经修改生产数据。"
                + "准备写操作前应调用 agent_list_allowed_write_actions 确认动作字段。工具返回 NEEDS_INPUT 时，必须用自然语言向用户询问 missingFields；"
                + "返回 PENDING 时，必须告诉用户需要在确认卡片中明确确认后才会执行。";

        try {
            String reply = chatClient.prompt()
                    .system(rolePrompt)
                    .user(buildUserPrompt(request))
                    .tools(tools)
                    .call()
                    .content();
            if (reply == null || reply.isBlank()) {
                throw new BusinessException(502, "智能 Agent 未返回有效内容，请稍后重试");
            }
            AgentWritePlanTools writeTools = toolSet.writeTools();
            AgentPlanOutcome outcome = writeTools != null ? writeTools.latestOutcome() : null;
            return new AgentChatResponse(
                    sessionId,
                    reply,
                    session.getUsername(),
                    session.getRoleCode(),
                    properties.getModel(),
                    outcome != null && outcome.pending() ? outcome.confirmation() : null
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(502, modelErrorMessage(ex));
        }
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId.trim();
    }

    private String buildUserPrompt(AgentChatRequest request) {
        StringBuilder prompt = new StringBuilder();
        if (request.conversation() != null && !request.conversation().isEmpty()) {
            prompt.append("以下是同一会话最近的对话，仅用于理解用户正在补充的业务参数：\n");
            request.conversation().stream()
                    .filter(message -> message != null && allowedRole(message.role()))
                    .skip(Math.max(0, request.conversation().size() - 12L))
                    .forEach(message -> prompt.append("user".equals(message.role()) ? "用户：" : "助手：")
                            .append(limit(message.content(), 1000)).append('\n'));
            prompt.append("当前用户输入：\n");
        }
        return prompt.append(request.message().trim()).toString();
    }

    private boolean allowedRole(String role) {
        return "user".equals(role) || "assistant".equals(role);
    }

    private String limit(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String modelErrorMessage(Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage() == null ? "未知错误" : root.getMessage();
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("401") || normalized.contains("unauthorized") || normalized.contains("api key")) {
            return "智能 Agent 调用失败：DeepSeek API Key 无效或没有模型访问权限";
        }
        if (normalized.contains("timeout") || normalized.contains("timed out")) {
            return "智能 Agent 调用失败：DeepSeek 模型请求超时，请稍后重试";
        }
        if (normalized.contains("connection") || normalized.contains("connect")) {
            return "智能 Agent 调用失败：无法连接 DeepSeek 模型服务";
        }
        String detail = message.length() > 180 ? message.substring(0, 180) : message;
        return "智能 Agent 调用失败：" + detail;
    }

    private String safe(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }
}
