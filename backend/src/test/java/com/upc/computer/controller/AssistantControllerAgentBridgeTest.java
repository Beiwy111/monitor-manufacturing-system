package com.upc.computer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.ai.dto.AgentChatRequest;
import com.upc.computer.ai.dto.AgentChatResponse;
import com.upc.computer.ai.service.AgentService;
import com.upc.computer.assistant.AssistantService;
import com.upc.computer.common.JwtUtil;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AssistantControllerAgentBridgeTest {

    private AssistantController controller;
    private AgentService agentService;
    private AssistantService legacyAssistant;
    private LoginResponse session;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        controller = new AssistantController();
        agentService = mock(AgentService.class);
        legacyAssistant = mock(AssistantService.class);
        AuthService authService = mock(AuthService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        ObjectProvider<AgentService> provider = mock(ObjectProvider.class);
        session = new LoginResponse();
        session.setUserId(1L);
        session.setUsername("admin");
        session.setRoleCode("ADMIN");

        when(provider.getIfAvailable()).thenReturn(agentService);
        when(jwtUtil.extractTokenFromHeader("Bearer token")).thenReturn("token");
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(authService.getLoginSession("token")).thenReturn(session);
        ReflectionTestUtils.setField(controller, "assistant", legacyAssistant);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(controller, "agentServiceProvider", provider);
        ReflectionTestUtils.setField(controller, "authService", authService);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwtUtil);
    }

    @Test
    void existingInterpretEndpointShouldUseSpringAgentWhenAiProfileIsEnabled() {
        AgentChatRequest request = new AgentChatRequest("查询库存", "s1");
        when(agentService.chat(request, session)).thenReturn(
                new AgentChatResponse("s1", "库存正常", "admin", "ADMIN", "deepseek-v4-pro"));

        Map<String, Object> data = controller.interpret(
                Map.of("text", "查询库存", "sessionId", "s1"), "Bearer token").getData();

        assertThat(data).containsEntry("type", "answer").containsEntry("reply", "库存正常");
        verifyNoInteractions(legacyAssistant);
    }

    @Test
    void existingStreamingEndpointShouldKeepFrontendNdjsonContract() throws Exception {
        AgentChatRequest request = new AgentChatRequest("查询库存", "s1");
        when(agentService.chat(request, session)).thenReturn(
                new AgentChatResponse("s1", "库存正常", "admin", "ADMIN", "deepseek-v4-pro"));

        ResponseEntity<StreamingResponseBody> response = controller.interpretStream(
                Map.of("text", "查询库存", "sessionId", "s1"), "Bearer token");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.getBody().writeTo(output);
        String ndjson = output.toString(StandardCharsets.UTF_8);

        assertThat(ndjson).contains("\"type\":\"delta\"")
                .contains("库存正常")
                .contains("\"type\":\"result\"")
                .contains("\"type\":\"done\"");
        verifyNoInteractions(legacyAssistant);
    }
}
