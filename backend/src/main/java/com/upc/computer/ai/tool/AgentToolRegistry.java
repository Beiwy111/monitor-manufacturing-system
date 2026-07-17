package com.upc.computer.ai.tool;

import com.upc.computer.ai.action.AgentActionPlanService;
import com.upc.computer.common.BusinessException;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.service.CustomerPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * 根据服务端登录角色分配 Tool。管理员拥有全部后台查询工具，普通角色只获得职责范围内工具。
 */
@Component
public class AgentToolRegistry {

    private final AdminAgentTools adminTools;
    private final OrderAgentTools orderTools;
    private final ProductionQueryAgentTools productionTools;
    private final PlannerAgentTools plannerTools;
    private final ManagerAgentTools managerTools;
    private final EquipmentAgentTools equipmentTools;
    private final MaterialWarehouseAgentTools materialWarehouseTools;
    private final QualityAgentTools qualityTools;
    private final PurchaseAgentTools purchaseTools;
    private final AfterSalesAgentTools afterSalesTools;
    private final FinanceAgentTools financeTools;
    private final CustomerPortalService customerPortalService;
    private final AgentActionPlanService actionPlanService;

    @Autowired
    public AgentToolRegistry(AdminAgentTools adminTools,
                             OrderAgentTools orderTools,
                             ProductionQueryAgentTools productionTools,
                             PlannerAgentTools plannerTools,
                             ManagerAgentTools managerTools,
                             EquipmentAgentTools equipmentTools,
                             MaterialWarehouseAgentTools materialWarehouseTools,
                             QualityAgentTools qualityTools,
                             PurchaseAgentTools purchaseTools,
                             AfterSalesAgentTools afterSalesTools,
                             FinanceAgentTools financeTools,
                             CustomerPortalService customerPortalService,
                             AgentActionPlanService actionPlanService) {
        this.adminTools = adminTools;
        this.orderTools = orderTools;
        this.productionTools = productionTools;
        this.plannerTools = plannerTools;
        this.managerTools = managerTools;
        this.equipmentTools = equipmentTools;
        this.materialWarehouseTools = materialWarehouseTools;
        this.qualityTools = qualityTools;
        this.purchaseTools = purchaseTools;
        this.afterSalesTools = afterSalesTools;
        this.financeTools = financeTools;
        this.customerPortalService = customerPortalService;
        this.actionPlanService = actionPlanService;
    }

    /** 保留给不需要写规划工具的单元测试和兼容调用。 */
    AgentToolRegistry(AdminAgentTools adminTools,
                      OrderAgentTools orderTools,
                      ProductionQueryAgentTools productionTools,
                      PlannerAgentTools plannerTools,
                      ManagerAgentTools managerTools,
                      EquipmentAgentTools equipmentTools,
                      MaterialWarehouseAgentTools materialWarehouseTools,
                      QualityAgentTools qualityTools,
                      PurchaseAgentTools purchaseTools,
                      AfterSalesAgentTools afterSalesTools,
                      FinanceAgentTools financeTools,
                      CustomerPortalService customerPortalService) {
        this(adminTools, orderTools, productionTools, plannerTools, managerTools, equipmentTools,
                materialWarehouseTools, qualityTools, purchaseTools, afterSalesTools, financeTools,
                customerPortalService, null);
    }

    public Object[] toolsFor(LoginResponse session) {
        String roleCode = normalizeRole(session != null ? session.getRoleCode() : null);
        return switch (roleCode) {
            case "ADMIN" -> new Object[]{
                    adminTools, orderTools, productionTools, plannerTools, managerTools,
                    equipmentTools, materialWarehouseTools, qualityTools, purchaseTools,
                    afterSalesTools, financeTools
            };
            case "ORDER" -> new Object[]{orderTools};
            case "PLANNER" -> new Object[]{orderTools, productionTools, plannerTools, materialWarehouseTools};
            case "MANAGER" -> new Object[]{productionTools, managerTools, equipmentTools};
            case "OPERATOR" -> new Object[]{productionTools, materialWarehouseTools};
            case "QC" -> new Object[]{qualityTools, productionTools};
            case "PURCHASER" -> new Object[]{purchaseTools, materialWarehouseTools, orderTools};
            case "WAREHOUSE" -> new Object[]{materialWarehouseTools, purchaseTools};
            case "DEVICE" -> new Object[]{equipmentTools, productionTools};
            case "SERVICE" -> new Object[]{afterSalesTools, orderTools, qualityTools};
            case "COST" -> new Object[]{financeTools, orderTools, productionTools};
            case "CUSTOMER" -> new Object[]{new CustomerAgentTools(customerPortalService, session)};
            default -> throw new BusinessException(403, "当前角色未配置智能 Agent 工具：" + roleCode);
        };
    }

    public AgentToolSet toolSetFor(LoginResponse session, String sessionId) {
        Object[] queryTools = toolsFor(session);
        if (actionPlanService == null) return new AgentToolSet(queryTools, null);
        AgentWritePlanTools writeTools = new AgentWritePlanTools(actionPlanService, session, sessionId);
        Object[] allTools = Arrays.copyOf(queryTools, queryTools.length + 1);
        allTools[queryTools.length] = writeTools;
        return new AgentToolSet(allTools, writeTools);
    }

    public record AgentToolSet(Object[] tools, AgentWritePlanTools writeTools) {
    }

    private String normalizeRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new BusinessException(403, "当前用户尚未分配角色，不能使用智能 Agent");
        }
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }
}
