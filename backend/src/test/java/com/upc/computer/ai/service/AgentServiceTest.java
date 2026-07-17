package com.upc.computer.ai.service;

import com.upc.computer.ai.config.AiProperties;
import com.upc.computer.ai.dto.AgentChatRequest;
import com.upc.computer.ai.dto.AgentChatResponse;
import com.upc.computer.ai.tool.AgentToolRegistry;
import com.upc.computer.common.BusinessException;
import com.upc.computer.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentServiceTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;
    private AgentToolRegistry toolRegistry;
    private AgentService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);
        toolRegistry = mock(AgentToolRegistry.class);
        AiProperties properties = new AiProperties();
        properties.setModel("deepseek-v4-pro");

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(toolRegistry.toolsFor(any(LoginResponse.class))).thenReturn(new Object[]{new EmptyTools()});
        service = new AgentService(chatClient, toolRegistry, properties);
    }

    @Test
    void shouldReturnUnifiedResponseWithoutCallingRealModel() {
        when(responseSpec.content()).thenReturn("当前有 3 个生产工单正在执行。");

        AgentChatResponse response = service.chat(new AgentChatRequest("查询生产情况", "session-1"), session());

        assertThat(response.sessionId()).isEqualTo("session-1");
        assertThat(response.reply()).contains("3 个生产工单");
        assertThat(response.roleCode()).isEqualTo("ADMIN");
        assertThat(response.model()).isEqualTo("deepseek-v4-pro");
    }

    @Test
    void timeoutShouldReturnClearModelError() {
        when(responseSpec.content()).thenThrow(new RuntimeException("read timed out"));

        assertThatThrownBy(() -> service.chat(new AgentChatRequest("查询库存", null), session()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请求超时");
    }

    private LoginResponse session() {
        LoginResponse session = new LoginResponse();
        session.setUserId(1L);
        session.setUsername("admin");
        session.setRealName("系统管理员");
        session.setRoleCode("ADMIN");
        session.setRoleName("系统管理员");
        return session;
    }

    static class EmptyTools {
    }
}
