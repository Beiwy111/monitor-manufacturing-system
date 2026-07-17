package com.upc.computer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.ai.dto.AgentChatRequest;
import com.upc.computer.ai.dto.AgentChatResponse;
import com.upc.computer.ai.dto.AgentConversationMessage;
import com.upc.computer.ai.service.AgentService;
import com.upc.computer.ai.action.AgentActionPlanService;
import com.upc.computer.assistant.AsrClient;
import com.upc.computer.assistant.AssistantService;
import com.upc.computer.common.BusinessException;
import com.upc.computer.common.JwtUtil;
import com.upc.computer.common.Result;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 全 MES 语音助手编排接口（原售后专用入口升级，旧路径 /afterSales/assistant 保留兼容）。
 * ① /asr        语音 → 文本（阿里云一句话识别，mock 模式返回样例）
 * ② /interpret  文本 → 意图（读→直接应答；写→返回待确认提议；body.module 为当前页面模块，供 NLU 意图偏置）
 * ③ /execute    人工闸门决策 → 执行真实业务接口（售后专用 Service 或 MesWorkflowService）
 *
 * 也可直接调 /interpret 用文本驱动，无需录音，便于联调。
 */
@RestController
@RequestMapping({"/assistant", "/afterSales/assistant"})
public class AssistantController {

    @Autowired private AsrClient asrClient;
    @Autowired private AssistantService assistant;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ObjectProvider<AgentService> agentServiceProvider;
    @Autowired private ObjectProvider<AgentActionPlanService> actionPlanServiceProvider;
    @Autowired private AuthService authService;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/asr")
    public Result<Map<String, Object>> asr(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new BusinessException("录音为空");
        String text = asrClient.recognize(file.getBytes(), file.getContentType());
        return Result.success(Map.of("text", text));
    }

    @PostMapping("/interpret")
    public Result<Map<String, Object>> interpret(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String sessionId = str(body, "sessionId");
        String module = str(body, "module");            // 当前页面模块（device/production/...），意图偏置用
        String text = str(body, "text");
        AgentService agentService = agentServiceProvider.getIfAvailable();
        if (agentService != null) {
            AgentChatResponse response = agentService.chat(
                    new AgentChatRequest(text, sessionId, agentConversation(body.get("conversation"))),
                    requireLoginSession(authorization));
            return Result.success(agentResult(response, false));
        }
        return Result.success(assistant.interpret(sessionId, module, text, conversation(body.get("conversation"))));
    }

    /** POST NDJSON 流：delta 事件逐段输出纯文本，result 事件承载确认卡或最终元数据。 */
    @PostMapping(value = "/interpret/stream", produces = "application/x-ndjson")
    public ResponseEntity<StreamingResponseBody> interpretStream(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String sessionId = str(body, "sessionId");
        String module = str(body, "module");
        String text = str(body, "text");
        List<Map<String, String>> history = conversation(body.get("conversation"));
        AgentService springAgent = agentServiceProvider.getIfAvailable();
        LoginResponse loginSession = springAgent != null ? requireLoginSession(authorization) : null;

        StreamingResponseBody responseBody = output -> {
            Consumer<String> deltaConsumer = delta -> {
                try {
                    writeStreamEvent(output, Map.of("type", "delta", "text", delta));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };

            try {
                Map<String, Object> result;
                if (springAgent != null) {
                    AgentChatResponse response = springAgent.chat(
                            new AgentChatRequest(text, sessionId, toAgentConversation(history)), loginSession);
                    deltaConsumer.accept(response.reply());
                    result = agentResult(response, true);
                } else {
                    result = assistant.interpretStreaming(sessionId, module, text, history, deltaConsumer);
                }
                writeStreamEvent(output, Map.of("type", "result", "data", result));
                writeStreamEvent(output, Map.of("type", "done"));
            } catch (UncheckedIOException disconnected) {
                throw disconnected.getCause();
            } catch (Exception e) {
                writeStreamEvent(output, Map.of(
                        "type", "error",
                        "message", "智能对话流式请求失败：" + safeMessage(e)));
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().noTransform());
        headers.set("X-Accel-Buffering", "no");
        return ResponseEntity.ok().headers(headers).body(responseBody);
    }

    @PostMapping("/execute")
    public Result<Map<String, Object>> execute(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String proposalId = str(body, "proposalId");
        String decision = str(body, "decision");        // APPROVE / MODIFY / SKIP
        String operator = str(body, "operator");        // 前端带当前登录用户名
        String roleKey = str(body, "roleKey");          // 前端带当前角色，MES 通用动作执行时透传
        if (proposalId.isBlank()) throw new BusinessException("proposalId 不能为空");
        if (proposalId.startsWith("AIP-")) {
            AgentActionPlanService actionPlanService = actionPlanServiceProvider.getIfAvailable();
            if (actionPlanService == null) throw new BusinessException(503, "Agent 写操作服务未启用");
            return Result.success(actionPlanService.confirm(proposalId, decision, body.get("finalParams"),
                    requireLoginSession(authorization)));
        }
        return Result.success(assistant.execute(proposalId, decision, body.get("finalParams"), operator, roleKey));
    }

    /** 跨模块协办通知列表（GlobalBusinessMonitor 轮询，按角色并入通知中心；target 过滤目标模块，可空） */
    @GetMapping("/notices")
    public Result<List<Map<String, Object>>> notices(@RequestParam(required = false) String target) {
        return Result.success(assistant.listNotices(target));
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : "";
    }

    private void writeStreamEvent(OutputStream output, Map<String, Object> event) throws IOException {
        output.write(objectMapper.writeValueAsBytes(event));
        output.write('\n');
        output.flush();
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) return "未知错误";
        return message.length() > 160 ? message.substring(0, 160) : message;
    }

    private Map<String, Object> agentResult(AgentChatResponse response, boolean streamed) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (response.action() != null) result.putAll(response.action());
        result.putIfAbsent("type", "answer");
        result.put("reply", response.reply());
        result.put("sessionId", response.sessionId());
        result.put("roleCode", response.roleCode());
        result.put("model", response.model());
        result.put("streamed", streamed);
        return result;
    }

    private LoginResponse requireLoginSession(String authorization) {
        String token = jwtUtil.extractTokenFromHeader(authorization);
        if (token == null || token.isBlank() || !jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "未登录或令牌无效");
        }
        LoginResponse session = authService.getLoginSession(token);
        if (session == null) {
            throw new BusinessException(401, "登录已失效，请重新登录");
        }
        return session;
    }

    /** 只接受 user/assistant 历史，防止客户端伪造 system 消息覆盖助手规则。 */
    private List<Map<String, String>> conversation(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, String>> messages = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            String role = map.get("role") == null ? "" : String.valueOf(map.get("role")).trim();
            if (!"user".equals(role) && !"assistant".equals(role)) continue;
            Object rawText = map.containsKey("text") ? map.get("text") : map.get("content");
            String text = rawText == null ? "" : String.valueOf(rawText).trim();
            if (text.isBlank()) continue;
            Map<String, String> message = new LinkedHashMap<>();
            message.put("role", role);
            message.put("content", text);
            messages.add(message);
        }
        return messages;
    }

    private List<AgentConversationMessage> agentConversation(Object value) {
        return toAgentConversation(conversation(value));
    }

    private List<AgentConversationMessage> toAgentConversation(List<Map<String, String>> messages) {
        return messages.stream()
                .map(message -> new AgentConversationMessage(message.get("role"), message.get("content")))
                .toList();
    }
}
