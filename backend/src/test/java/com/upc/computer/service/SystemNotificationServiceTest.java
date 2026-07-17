package com.upc.computer.service;

import com.upc.computer.mapper.SystemNotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemNotificationServiceTest {

    @Mock private SystemNotificationMapper mapper;
    private SystemNotificationService service;

    @BeforeEach
    void setUp() {
        service = new SystemNotificationService(mapper);
    }

    @Test
    void routesPendingOrderReminderToOrderAuditors() {
        when(mapper.insert(org.mockito.ArgumentMatchers.anyMap())).thenAnswer(invocation -> {
            Map<String, Object> row = invocation.getArgument(0);
            row.put("notificationId", 99L);
            return 1;
        });

        Map<String, Object> result = service.sendGlobalAnalysisAction(Map.of(
                "priority", "P1",
                "department", "订单管理部",
                "action", "立即处理待审核订单",
                "basis", "当前有10个订单待审核"
        ), 1L, "admin");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mapper).insert(captor.capture());
        assertEquals("ORDER", captor.getValue().get("receiverRole"));
        assertEquals("/order/audit", captor.getValue().get("targetPath"));
        assertEquals("HIGH", captor.getValue().get("level"));
        assertEquals("订单管理部", result.get("targetDepartment"));
        assertEquals(99L, result.get("notificationId"));
    }
}
