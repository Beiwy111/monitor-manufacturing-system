package com.upc.computer.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MES Agent 的模型基础设施。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProperties.class)
@ConditionalOnProperty(prefix = "mes.ai", name = "enabled", havingValue = "true")
public class AiModelConfiguration {

    @Bean
    public ChatClient mesAgentChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是电脑显示器制造 MES 智能助手。你需要理解用户的自然语言需求，并调用系统提供的工具完成操作。"
                        + "实时数据必须通过工具查询，不得编造数据。只能调用已有工具，不得执行 SQL、Shell 或代码。"
                        + "工具执行失败时必须明确说明失败原因。")
                .build();
    }
}
