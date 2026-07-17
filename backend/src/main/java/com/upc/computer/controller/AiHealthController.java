package com.upc.computer.controller;

import com.upc.computer.ai.config.AiProperties;
import com.upc.computer.common.Result;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring AI 基础设施健康检查，不向模型发送请求，也不会产生模型调用费用。
 */
@RestController
@RequestMapping("/ai")
@ConditionalOnProperty(prefix = "mes.ai", name = "enabled", havingValue = "true")
public class AiHealthController {

    private final ChatClient chatClient;
    private final AiProperties properties;

    public AiHealthController(ChatClient chatClient, AiProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    @GetMapping("/health")
    public Result<AiHealthStatus> health() {
        return Result.success(new AiHealthStatus(
                "UP",
                properties.getProvider(),
                properties.getModel(),
                chatClient != null
        ));
    }

    public record AiHealthStatus(
            String status,
            String provider,
            String model,
            boolean chatClientInitialized
    ) {
    }
}
