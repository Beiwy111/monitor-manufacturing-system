package com.upc.computer.ai.tool;

import com.upc.computer.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgentToolSchemaTest {

    @Test
    void adminToolSetShouldProduceUniqueSpringAiToolSchemas() {
        Object[] tools = {
                new AdminAgentTools(mock(SystemService.class), mock(AttendanceService.class),
                        mock(RoleWorkbenchDashboardService.class)),
                new OrderAgentTools(mock(OrderService.class)),
                new ProductionQueryAgentTools(mock(ProductionService.class)),
                new PlannerAgentTools(mock(MesPlannerSchedulingService.class)),
                new ManagerAgentTools(mock(MesDashboardService.class), mock(MesDispatchRecommendService.class)),
                new EquipmentAgentTools(mock(EquipmentService.class)),
                new MaterialWarehouseAgentTools(mock(MaterialService.class), mock(WarehouseBarcodeService.class),
                        mock(WarehouseLocationService.class), mock(WarehouseSlotService.class)),
                new QualityAgentTools(mock(QualityService.class)),
                new PurchaseAgentTools(mock(PurchaseService.class)),
                new AfterSalesAgentTools(mock(AfterSalesService.class)),
                new FinanceAgentTools(mock(CostService.class), mock(FinanceService.class))
        };

        ToolCallback[] callbacks = MethodToolCallbackProvider.builder().toolObjects(tools).build().getToolCallbacks();

        assertThat(callbacks).hasSizeGreaterThan(40);
        assertThat(Arrays.stream(callbacks).map(callback -> callback.getToolDefinition().name()))
                .doesNotHaveDuplicates()
                .allSatisfy(name -> assertThat(name).matches("[a-z][a-z0-9_]*"));
        assertThat(Arrays.stream(callbacks).map(callback -> callback.getToolDefinition().inputSchema()))
                .allSatisfy(schema -> assertThat(schema).isNotBlank());
    }
}
