package com.upc.computer.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "assistant.nlu.mock=true")
class AssistantInventoryFallbackIntegrationTest {

    @Autowired private AssistantService assistantService;

    @Test
    void returnsFullInventoryForConversationalOverviewWhenModelIsUnavailable() {
        Map<String, Object> response = assistantService.interpret(
                "inventory-fallback-test", "warehouse", "现在的库存怎么样？", List.of());

        String reply = String.valueOf(response.get("reply"));
        assertTrue(reply.contains("当前库存一览"));
        assertTrue(reply.contains("数据库共"));
        assertFalse(reply.contains("我能处理"));
        assertFalse(reply.contains("请换个说法"));
    }
}
