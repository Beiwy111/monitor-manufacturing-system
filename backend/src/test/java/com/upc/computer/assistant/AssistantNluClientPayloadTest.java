package com.upc.computer.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.config.AiProperties;
import com.upc.computer.config.AssistantProperties;
import com.upc.computer.config.DeepseekProperties;
import com.upc.computer.entity.AttendanceRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantNluClientPayloadTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void modelPayloadContainsFactoryConversationAndCurrentMessageInOrder() throws Exception {
        MesActionCatalog catalog = mock(MesActionCatalog.class);
        AssistantFactoryContextService factoryContext = mock(AssistantFactoryContextService.class);
        when(catalog.nluCatalogText()).thenReturn("system.overview 系统概况");
        AttendanceRecord attendance = new AttendanceRecord();
        attendance.setRecordId(1L);
        attendance.setAttendanceDate(LocalDate.of(2026, 7, 16));
        when(factoryContext.buildCurrentFactory()).thenReturn(Map.of(
                "sysUsers", List.of(Map.of("username", "admin")),
                "orders", List.of(Map.of("id", "ORD-1")),
                "attendanceRecords", List.of(attendance)
        ));

        AssistantNluClient client = new AssistantNluClient(
                new AiProperties(), new DeepseekProperties(), new AssistantProperties(), catalog, factoryContext);

        List<Map<String, String>> history = List.of(
                Map.of("role", "user", "content", "上一轮问题"),
                Map.of("role", "assistant", "content", "上一轮回答")
        );
        List<Map<String, String>> messages = client.buildModelMessages(
                "这轮有多少订单？", "{\"currentModule\":\"system\"}", history);

        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("user", messages.get(1).get("role"));

        JsonNode payload = mapper.readTree(messages.get(1).get("content"));
        List<String> fieldNames = new ArrayList<>();
        payload.fieldNames().forEachRemaining(fieldNames::add);
        assertEquals(List.of("currentFactory", "currentConversation", "message"), fieldNames);
        assertEquals("admin", payload.path("currentFactory").path("sysUsers").path(0).path("username").asText());
        assertEquals("2026-07-16", payload.path("currentFactory").path("attendanceRecords").path(0).path("attendanceDate").asText());
        assertEquals(2, payload.path("currentConversation").path("messages").size());
        assertEquals("system", payload.path("currentConversation").path("runtimeState").path("currentModule").asText());
        assertEquals("这轮有多少订单？", payload.path("message").asText());
        assertTrue(messages.get(0).get("content").contains("factory.query"));
        assertTrue(messages.get(0).get("content").contains("禁止只重复上一轮摘要"));
        assertTrue(messages.get(0).get("content").contains("所有只读查询默认详细回答"));
    }

    @Test
    void localFallbackRecognizesConversationalInventoryOverview() {
        MesActionCatalog catalog = mock(MesActionCatalog.class);
        AssistantFactoryContextService factoryContext = mock(AssistantFactoryContextService.class);
        AssistantProperties properties = new AssistantProperties();
        properties.getNlu().setMock(true);
        AssistantNluClient client = new AssistantNluClient(
                new AiProperties(), new DeepseekProperties(), properties, catalog, factoryContext);

        NluResult result = client.interpret("现在的库存怎么样？", "{}");

        assertEquals("warehouse.query_inventory", result.action());
        assertTrue(result.keyword().isBlank());
    }

    @Test
    void directQuestionPayloadUsesNaturalAnswerPromptAndAllThreeContextSections() throws Exception {
        MesActionCatalog catalog = mock(MesActionCatalog.class);
        AssistantFactoryContextService factoryContext = mock(AssistantFactoryContextService.class);
        when(factoryContext.buildCurrentFactory()).thenReturn(Map.of(
                "orders", List.of(Map.of("id", "ORD-1", "customerName", "星辰俱乐部")),
                "inventory", List.of(Map.of("materialCode", "MAT-001", "quantity", 120))
        ));
        AssistantNluClient client = new AssistantNluClient(
                new AiProperties(), new DeepseekProperties(), new AssistantProperties(), catalog, factoryContext);

        List<Map<String, String>> messages = client.buildQuestionMessages(
                "订单最多的客户是谁？", "{\"currentModule\":\"system\"}",
                List.of(Map.of("role", "user", "content", "先看一下订单")));

        assertEquals(2, messages.size());
        assertTrue(messages.get(0).get("content").contains("直接回答 message"));
        assertTrue(messages.get(0).get("content").contains("按 currentFactory.orders 的 customerName 聚合"));
        JsonNode payload = mapper.readTree(messages.get(1).get("content"));
        assertEquals("ORD-1", payload.path("currentFactory").path("orders").path(0).path("id").asText());
        assertEquals(1, payload.path("currentConversation").path("messages").size());
        assertEquals("订单最多的客户是谁？", payload.path("message").asText());
    }
}
