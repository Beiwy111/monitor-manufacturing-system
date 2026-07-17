package com.upc.computer.controller;

import com.upc.computer.ai.dto.AgentChatRequest;
import com.upc.computer.ai.dto.AgentChatResponse;
import com.upc.computer.ai.service.AgentService;
import com.upc.computer.common.BusinessException;
import com.upc.computer.common.JwtUtil;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.service.AuthService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentChatControllerTest {

    @Test
    void shouldUseServerSideLoginSessionInsteadOfClientRole() {
        AgentService agentService = mock(AgentService.class);
        AuthService authService = mock(AuthService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AgentChatController controller = new AgentChatController(agentService, authService, jwtUtil);
        LoginResponse session = session();
        AgentChatRequest request = new AgentChatRequest("查询库存", "s1");
        AgentChatResponse response = new AgentChatResponse("s1", "库存正常", "admin", "ADMIN", "deepseek-v4-pro");
        when(jwtUtil.extractTokenFromHeader("Bearer token")).thenReturn("token");
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(authService.getLoginSession("token")).thenReturn(session);
        when(agentService.chat(request, session)).thenReturn(response);

        assertThat(controller.chat(request, "Bearer token").getData()).isEqualTo(response);
    }

    @Test
    void invalidJwtShouldBeRejectedBeforeAgentCall() {
        AgentChatController controller = new AgentChatController(
                mock(AgentService.class), mock(AuthService.class), mock(JwtUtil.class));

        assertThatThrownBy(() -> controller.chat(new AgentChatRequest("查询库存", null), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("令牌无效");
    }

    private LoginResponse session() {
        LoginResponse session = new LoginResponse();
        session.setUserId(1L);
        session.setUsername("admin");
        session.setRoleCode("ADMIN");
        return session;
    }
}
