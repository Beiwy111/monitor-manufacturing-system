package com.upc.computer.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AssistantFactoryContextIntegrationTest {

    @Autowired private AssistantNluClient nluClient;

    @Test
    void realFactoryContextCanBeSerializedIntoModelPayload() throws Exception {
        List<Map<String, String>> messages = nluClient.buildModelMessages(
                "当前工厂有多少用户？", "{\"currentModule\":\"system\"}", List.of());
        String payloadText = messages.get(1).get("content");
        JsonNode payload = new ObjectMapper().findAndRegisterModules().readTree(payloadText);

        assertTrue(payload.path("currentFactory").path("sysUsers").isArray());
        assertTrue(payload.path("currentFactory").path("permissions").isArray());
        assertTrue(payload.path("currentFactory").path("menus").isArray());
        assertTrue(payload.path("currentFactory").path("roleMenus").isArray());
        assertTrue(payload.path("currentFactory").path("attendanceRecords").isArray());
        assertFalse(payloadText.toLowerCase().contains("passwordhash"));
        assertFalse(payloadText.toLowerCase().contains("sessiontoken"));
    }
}
