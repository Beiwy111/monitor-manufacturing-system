package com.upc.computer.ai.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.service.AfterSalesService;
import com.upc.computer.service.AttendanceService;
import com.upc.computer.service.CostService;
import com.upc.computer.service.CustomerPortalService;
import com.upc.computer.service.EquipmentService;
import com.upc.computer.service.MesSnapshotService;
import com.upc.computer.service.MaterialService;
import com.upc.computer.service.OrderService;
import com.upc.computer.service.ProductionService;
import com.upc.computer.service.ProductionProcessService;
import com.upc.computer.service.PurchaseService;
import com.upc.computer.service.QualityService;
import com.upc.computer.service.SystemService;
import com.upc.computer.service.SupplierService;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/** 生成方案时和确认时读取相同业务状态，用于拒绝过期方案。 */
@Service
public class AgentActionStateService {

    private final AgentActionCatalog catalog;
    private final MesSnapshotService mesSnapshotService;
    private final SystemService systemService;
    private final ProductionProcessService productionProcessService;
    private final QualityService qualityService;
    private final PurchaseService purchaseService;
    private final EquipmentService equipmentService;
    private final AfterSalesService afterSalesService;
    private final CostService costService;
    private final CustomerPortalService customerPortalService;
    private final OrderService orderService;
    private final ProductionService productionService;
    private final MaterialService materialService;
    private final AttendanceService attendanceService;
    private final SupplierService supplierService;
    private final ObjectMapper objectMapper;

    public AgentActionStateService(AgentActionCatalog catalog,
                                   MesSnapshotService mesSnapshotService,
                                   SystemService systemService,
                                   ProductionProcessService productionProcessService,
                                   QualityService qualityService,
                                   PurchaseService purchaseService,
                                   EquipmentService equipmentService,
                                   AfterSalesService afterSalesService,
                                   CostService costService,
                                   CustomerPortalService customerPortalService,
                                   OrderService orderService,
                                   ProductionService productionService,
                                   MaterialService materialService,
                                   AttendanceService attendanceService,
                                   SupplierService supplierService,
                                   ObjectMapper objectMapper) {
        this.catalog = catalog;
        this.mesSnapshotService = mesSnapshotService;
        this.systemService = systemService;
        this.productionProcessService = productionProcessService;
        this.qualityService = qualityService;
        this.purchaseService = purchaseService;
        this.equipmentService = equipmentService;
        this.afterSalesService = afterSalesService;
        this.costService = costService;
        this.customerPortalService = customerPortalService;
        this.orderService = orderService;
        this.productionService = productionService;
        this.materialService = materialService;
        this.attendanceService = attendanceService;
        this.supplierService = supplierService;
        this.objectMapper = objectMapper;
    }

    public String fingerprint(AgentActionDefinition action, Map<String, Object> p, LoginResponse session) {
        Object state = currentState(action.code(), p, session);
        try {
            byte[] json = objectMapper.writeValueAsBytes(state == null ? Map.of("missing", true) : state);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法生成业务状态快照", e);
        }
    }

    private Object currentState(String code, Map<String, Object> p, LoginResponse session) {
        if (code.startsWith("crud.")) return crudState(code);
        if (catalog.isMesWorkflowAction(code)) return workflowState(code);
        return switch (code) {
            case "admin.user.update", "admin.user.delete" -> systemService.getUserById(longVal(p, "userId"));
            case "admin.role.update", "admin.role.delete" -> systemService.getRoleById(longVal(p, "roleId"));
            case "admin.menu.update", "admin.menu.delete" -> systemService.getMenuById(longVal(p, "menuId"));
            case "admin.permission.update", "admin.permission.delete" -> systemService.getPermissionById(longVal(p, "permissionId"));
            case "admin.operationLog.delete" -> systemService.getOperationLogById(longVal(p, "logId"));
            case "process.route.save", "process.route.disable", "process.step.save", "process.step.disable", "process.step.reorder" -> productionProcessService.snapshot();
            case "quality.items.generate", "quality.items.save", "quality.evaluate", "quality.pass", "quality.fail",
                 "quality.recheck", "quality.recheckPass", "quality.recheckFail", "quality.close", "quality.sampling.update" ->
                    qualityService.getInspectionById(longVal(p, "inspectionId"));
            case "quality.nonconforming.handle" -> qualityService.getNonconformingById(longVal(p, "nonconformingId"));
            case "purchase.requirements.calculate", "purchase.orders.generate" -> Map.of(
                    "requirements", purchaseService.workbenchList(null, null, null, "all"),
                    "orders", purchaseService.purchaseOrderList());
            case "purchase.requirement.select", "purchase.requirement.cancel" -> purchaseService.workbenchDetail(longVal(p, "requirementId"));
            case "purchase.arrival.confirm", "purchase.arrival.confirmSlots", "purchase.order.revoke", "purchase.order.saveDraft" ->
                    purchaseService.getPurchaseOrderById(longVal(p, "purchaseOrderId"));
            case "equipment.alarm.trigger", "equipment.maintenance.start" -> equipmentService.getEquipmentById(longVal(p, "equipmentId"));
            case "equipment.alarm.receive", "equipment.alarm.resolve" -> equipmentService.getAlarmById(longVal(p, "alarmId"));
            case "equipment.maintenance.finish" -> equipmentService.getMaintenanceById(longVal(p, "maintenanceId"));
            case "aftersale.case.accept", "aftersale.case.resolve", "aftersale.case.close", "aftersale.rca.dispatch",
                 "aftersale.rca.confirm", "aftersale.case.advance", "aftersale.closure.save",
                 "aftersale.closure.confirmCustomer", "aftersale.closure.close" -> afterSalesService.getAfterSalesCaseById(text(p, "caseNo"));
            case "cost.settlement.save", "cost.settlement.confirm", "cost.settlement.export" ->
                    p.get("settlementId") == null ? costService.settlementList() : costService.getSettlementById(longVal(p, "settlementId"));
            case "customer.profile.update" -> customerPortalService.getProfile(session);
            case "attendance.checkIn", "attendance.checkOut" -> Map.of(
                    "today", String.valueOf(java.time.LocalDate.now()),
                    "record", java.util.Optional.<Object>ofNullable(attendanceService.todayRecord(session.getUserId())).orElse("NONE"));
            case "attendance.schedule.save", "attendance.schedule.delete" -> attendanceService.scheduleList(
                    java.time.LocalDate.now().minusMonths(1), java.time.LocalDate.now().plusMonths(3));
            case "warehouse.barcode.rule.save", "warehouse.barcode.generate", "warehouse.barcode.scan" -> warehouseState();
            default -> Map.of("action", code, "parameters", p);
        };
    }

    private Object workflowState(String code) {
        if (SetGroups.ORDER.contains(code)) {
            return Map.of("orders", orderService.customerOrderList(), "items", orderService.orderItemList(),
                    "deliveries", orderService.deliveryList(), "materials", materialService.materialList());
        }
        if (SetGroups.QUALITY.contains(code)) {
            return Map.of("inspections", qualityService.inspectionList(), "defects", qualityService.nonconformingList(),
                    "workOrders", productionService.workOrderList(), "reports", productionService.reportList());
        }
        if (SetGroups.PURCHASE.contains(code)) {
            return Map.of("orders", purchaseService.purchaseOrderList(), "items", purchaseService.purchaseOrderItemList(),
                    "materials", materialService.materialList(), "inventory", materialService.inventoryList());
        }
        if (SetGroups.WAREHOUSE.contains(code)) return warehouseState();
        if (SetGroups.DEVICE.contains(code)) {
            return Map.of("equipment", equipmentService.equipmentList(), "alarms", equipmentService.alarmList(),
                    "maintenance", equipmentService.maintenanceList());
        }
        if (SetGroups.AFTERSALES.contains(code)) {
            return Map.of("cases", afterSalesService.afterSalesCaseList(), "orders", orderService.customerOrderList());
        }
        if (SetGroups.COST.contains(code)) return costService.settlementList();
        if (SetGroups.ADMIN.contains(code)) return Map.of("users", systemService.userList(), "roles", systemService.roleList());
        return Map.of("plans", productionService.planList(), "planItems", productionService.planItemList(),
                "workOrders", productionService.workOrderList(), "dispatches", productionService.dispatchList(),
                "reports", productionService.reportList(), "progress", productionService.progressList(),
                "orders", orderService.customerOrderList());
    }

    private Object crudState(String code) {
        if (code.startsWith("crud.order.")) return workflowState("createOrder");
        if (code.startsWith("crud.production.")) return workflowState("createPlan");
        if (code.startsWith("crud.material.")) return warehouseState();
        if (code.startsWith("crud.quality.")) return workflowState("submitInspection");
        if (code.startsWith("crud.purchase.supplier")) return supplierService.listAll();
        if (code.startsWith("crud.purchase.")) return workflowState("createPurchaseOrder");
        if (code.startsWith("crud.equipment.")) return workflowState("createAlarm");
        if (code.startsWith("crud.aftersale.")) return workflowState("createAftersale");
        if (code.startsWith("crud.cost.")) return costService.settlementList();
        return Map.of("action", code);
    }

    private Object warehouseState() {
        Map<String, Object> snapshot = mesSnapshotService.buildSnapshot();
        return Map.of(
                "inventory", snapshot.getOrDefault("inventory", java.util.List.of()),
                "stockFlows", snapshot.getOrDefault("stockFlows", java.util.List.of()),
                "inboundTasks", snapshot.getOrDefault("inboundTasks", java.util.List.of()),
                "issueTasks", snapshot.getOrDefault("issueTasks", java.util.List.of()),
                "deliveries", snapshot.getOrDefault("deliveries", java.util.List.of())
        );
    }

    private Long longVal(Map<String, Object> p, String key) {
        Object value = p.get(key);
        if (value instanceof Number n) return n.longValue();
        if (value == null || String.valueOf(value).isBlank()) return null;
        return Long.parseLong(String.valueOf(value));
    }

    private String text(Map<String, Object> p, String key) {
        Object value = p.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static final class SetGroups {
        private static final java.util.Set<String> ORDER = java.util.Set.of(
                "createOrder", "auditOrder", "submitOrder", "submitOrderToPlanner", "deleteOrder");
        private static final java.util.Set<String> QUALITY = java.util.Set.of(
                "submitInspection", "generateQualityReport", "scrapDefect", "reworkDefect", "deleteInspection", "deleteDefect");
        private static final java.util.Set<String> PURCHASE = java.util.Set.of(
                "createPurchaseOrder", "receivePurchase", "deletePurchaseOrder");
        private static final java.util.Set<String> WAREHOUSE = java.util.Set.of(
                "confirmInbound", "issueMaterial", "shipDelivery", "deleteDelivery", "deleteInboundTask", "deleteIssueTask");
        private static final java.util.Set<String> DEVICE = java.util.Set.of(
                "createAlarm", "handleAlarm", "updateEquipment", "deleteAlarm");
        private static final java.util.Set<String> AFTERSALES = java.util.Set.of(
                "createAftersale", "processAftersale", "deleteAftersale");
        private static final java.util.Set<String> COST = java.util.Set.of(
                "confirmCostSettlement", "exportCostSettlement", "deleteCostSettlement");
        private static final java.util.Set<String> ADMIN = java.util.Set.of(
                "saveUser", "toggleUserStatus", "resetUserPassword");
    }
}
