package com.upc.computer.ai.config;

import com.upc.computer.controller.AiHealthController;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SpringAiRetryAutoConfiguration.class,
                    ToolCallingAutoConfiguration.class,
                    OpenAiChatAutoConfiguration.class,
                    ChatClientAutoConfiguration.class
            ))
            .withUserConfiguration(AiModelConfiguration.class, AiHealthController.class)
            .withPropertyValues(
                    "mes.ai.enabled=true",
                    "mes.ai.provider=deepseek",
                    "mes.ai.api-key=test-key",
                    "mes.ai.base-url=https://api.deepseek.com",
                    "mes.ai.model=deepseek-chat",
                    "mes.ai.temperature=0.2",
                    "mes.ai.max-tokens=4096",
                    "spring.ai.model.chat=openai",
                    "spring.ai.openai.api-key=test-key",
                    "spring.ai.openai.base-url=https://api.deepseek.com",
                    "spring.ai.openai.chat.options.model=deepseek-chat"
            );

    @Test
    void shouldBindPropertiesAndInitializeChatClient() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AiProperties.class);
            assertThat(context).hasSingleBean(ChatClient.class);
            assertThat(context).hasSingleBean(AiHealthController.class);

            AiProperties properties = context.getBean(AiProperties.class);
            assertThat(properties.getProvider()).isEqualTo("deepseek");
            assertThat(properties.getModel()).isEqualTo("deepseek-chat");

            AiHealthController.AiHealthStatus status = context
                    .getBean(AiHealthController.class)
                    .health()
                    .getData();
            assertThat(status.status()).isEqualTo("UP");
            assertThat(status.chatClientInitialized()).isTrue();
        });
    }

    @Test
    void shouldNotInitializeAgentBeansWhenDisabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(AiModelConfiguration.class, AiHealthController.class)
                .withPropertyValues("mes.ai.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ChatClient.class);
                    assertThat(context).doesNotHaveBean(AiHealthController.class);
                });
    }

    @Test
    void aiProfileShouldReuseConfiguredDeepseekKeyAndV4ProModel() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(
                "application-ai.yml",
                new ClassPathResource("application-ai.yml")
        );

        assertThat(sources).hasSize(1);
        PropertySource<?> source = sources.get(0);
        assertThat(source.getProperty("spring.config.activate.on-profile")).isEqualTo("ai");
        assertThat(source.getProperty("spring.ai.model.chat")).isEqualTo("openai");
        assertThat(source.getProperty("spring.ai.openai.api-key")).isEqualTo("${mes.ai.api-key}");
        assertThat(source.getProperty("mes.ai.api-key"))
                .isEqualTo("${deepseek.api-key}");
        assertThat(source.getProperty("mes.ai.model")).isEqualTo("deepseek-v4-pro");
    }
}
