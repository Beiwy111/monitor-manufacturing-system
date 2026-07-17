package com.upc.computer.ai.tool;

import com.upc.computer.service.MesDispatchRecommendService;
import com.upc.computer.service.MesPlannerSchedulingService;
import com.upc.computer.service.OrderService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentToolServiceDelegationTest {

    @Test
    void orderToolShouldDelegateToExistingOrderService() {
        OrderService service = mock(OrderService.class);
        OrderAgentTools tools = new OrderAgentTools(service);

        tools.listCustomerOrders();

        verify(service).customerOrderList();
    }

    @Test
    void plannerToolShouldReuseExistingSmartSchedulingService() {
        MesPlannerSchedulingService service = mock(MesPlannerSchedulingService.class);
        PlannerAgentTools tools = new PlannerAgentTools(service);
        when(service.compareSchemes("CO-001", LocalDate.parse("2026-07-20"),
                LocalDate.parse("2026-07-30"), 100)).thenReturn(Map.of("ok", true));

        Object result = tools.compareSchemes("CO-001", "2026-07-20", "2026-07-30", 100);

        assertThat(result).isEqualTo(Map.of("ok", true));
        verify(service).compareSchemes("CO-001", LocalDate.parse("2026-07-20"),
                LocalDate.parse("2026-07-30"), 100);
    }

    @Test
    void managerToolShouldReuseExistingSmartDispatchService() {
        MesDispatchRecommendService recommendService = mock(MesDispatchRecommendService.class);
        ManagerAgentTools tools = new ManagerAgentTools(mock(com.upc.computer.service.MesDashboardService.class),
                recommendService);

        tools.previewSmartDispatch("PLAN-001");

        verify(recommendService).generateRecommendations("PLAN-001");
    }

    @Test
    void toolInputShouldRejectInvalidDateAndQuantity() {
        PlannerAgentTools tools = new PlannerAgentTools(mock(MesPlannerSchedulingService.class));

        assertThatThrownBy(() -> tools.compareSchemes("CO-001", "bad-date", "2026-07-30", 100))
                .hasMessageContaining("yyyy-MM-dd");
        assertThatThrownBy(() -> tools.compareSchemes("CO-001", "2026-07-20", "2026-07-30", 0))
                .hasMessageContaining("必须大于零");
    }
}
