package com.upc.computer.ai.tool;

import com.upc.computer.dto.LoginResponse;
import com.upc.computer.service.CustomerPortalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AgentToolRegistryTest {

    private AdminAgentTools adminTools;
    private OrderAgentTools orderTools;
    private ProductionQueryAgentTools productionTools;
    private PlannerAgentTools plannerTools;
    private ManagerAgentTools managerTools;
    private EquipmentAgentTools equipmentTools;
    private MaterialWarehouseAgentTools warehouseTools;
    private QualityAgentTools qualityTools;
    private PurchaseAgentTools purchaseTools;
    private AfterSalesAgentTools afterSalesTools;
    private FinanceAgentTools financeTools;
    private CustomerPortalService customerPortalService;
    private AgentToolRegistry registry;

    @BeforeEach
    void setUp() {
        adminTools = mock(AdminAgentTools.class);
        orderTools = mock(OrderAgentTools.class);
        productionTools = mock(ProductionQueryAgentTools.class);
        plannerTools = mock(PlannerAgentTools.class);
        managerTools = mock(ManagerAgentTools.class);
        equipmentTools = mock(EquipmentAgentTools.class);
        warehouseTools = mock(MaterialWarehouseAgentTools.class);
        qualityTools = mock(QualityAgentTools.class);
        purchaseTools = mock(PurchaseAgentTools.class);
        afterSalesTools = mock(AfterSalesAgentTools.class);
        financeTools = mock(FinanceAgentTools.class);
        customerPortalService = mock(CustomerPortalService.class);
        registry = new AgentToolRegistry(
                adminTools, orderTools, productionTools, plannerTools, managerTools,
                equipmentTools, warehouseTools, qualityTools, purchaseTools,
                afterSalesTools, financeTools, customerPortalService);
    }

    @Test
    void adminShouldReceiveAllBackendToolGroups() {
        Object[] tools = registry.toolsFor(session("ADMIN"));

        assertThat(tools).hasSize(11);
        assertThat(Arrays.asList(tools)).contains(
                adminTools, orderTools, productionTools, plannerTools, managerTools,
                equipmentTools, warehouseTools, qualityTools, purchaseTools,
                afterSalesTools, financeTools);
    }

    @Test
    void plannerShouldReceivePlanningButNotAdminOrFinanceTools() {
        Object[] tools = registry.toolsFor(session("PLANNER"));

        assertThat(Arrays.asList(tools))
                .contains(orderTools, productionTools, plannerTools, warehouseTools)
                .doesNotContain(adminTools, managerTools, financeTools);
    }

    @Test
    void customerShouldReceiveSessionBoundCustomerToolsOnly() {
        Object[] tools = registry.toolsFor(session("CUSTOMER"));

        assertThat(tools).hasSize(1);
        assertThat(tools[0]).isInstanceOf(CustomerAgentTools.class);
    }

    @Test
    void unsupportedRoleShouldBeRejected() {
        assertThatThrownBy(() -> registry.toolsFor(session("UNKNOWN")))
                .hasMessageContaining("未配置智能 Agent 工具");
    }

    private LoginResponse session(String roleCode) {
        LoginResponse session = new LoginResponse();
        session.setUserId(1L);
        session.setUsername("tester");
        session.setRoleCode(roleCode);
        return session;
    }
}
