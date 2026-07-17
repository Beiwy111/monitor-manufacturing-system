package com.upc.computer.ai.action;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.common.BusinessException;
import com.upc.computer.dto.CustomerCreateOrderRequest;
import com.upc.computer.dto.CustomerFeedbackRequest;
import com.upc.computer.dto.CustomerProfileUpdateRequest;
import com.upc.computer.dto.GeneratePurchaseRequest;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.dto.MesActionRequest;
import com.upc.computer.dto.ShiftScheduleSaveRequest;
import com.upc.computer.dto.UpdatePurchaseOrderDraftRequest;
import com.upc.computer.entity.BarcodeRule;
import com.upc.computer.entity.CostSettlement;
import com.upc.computer.entity.Permission;
import com.upc.computer.entity.ProcessRoute;
import com.upc.computer.entity.ProcessStep;
import com.upc.computer.entity.QualityInspectionItem;
import com.upc.computer.entity.Role;
import com.upc.computer.entity.SysMenu;
import com.upc.computer.entity.User;
import com.upc.computer.service.AfterSalesService;
import com.upc.computer.service.AfterSalesWorkflowService;
import com.upc.computer.service.AttendanceService;
import com.upc.computer.service.CostService;
import com.upc.computer.service.CustomerPortalService;
import com.upc.computer.service.EquipmentService;
import com.upc.computer.service.MesWorkflowService;
import com.upc.computer.service.ProductionProcessService;
import com.upc.computer.service.PurchaseService;
import com.upc.computer.service.QualityService;
import com.upc.computer.service.SystemService;
import com.upc.computer.service.WarehouseBarcodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 确认后唯一的动作执行入口；这里只能调用明确列出的业务 Service。 */
@Service
public class AgentActionExecutionService {

    private final AgentActionCatalog catalog;
    private final MesWorkflowService mesWorkflowService;
    private final SystemService systemService;
    private final AttendanceService attendanceService;
    private final ProductionProcessService productionProcessService;
    private final QualityService qualityService;
    private final PurchaseService purchaseService;
    private final EquipmentService equipmentService;
    private final WarehouseBarcodeService warehouseBarcodeService;
    private final AfterSalesService afterSalesService;
    private final AfterSalesWorkflowService afterSalesWorkflowService;
    private final CostService costService;
    private final CustomerPortalService customerPortalService;
    private final ObjectMapper objectMapper;
    private final AgentDomainCrudService domainCrudService;

    public AgentActionExecutionService(AgentActionCatalog catalog,
                                       MesWorkflowService mesWorkflowService,
                                       SystemService systemService,
                                       AttendanceService attendanceService,
                                       ProductionProcessService productionProcessService,
                                       QualityService qualityService,
                                       PurchaseService purchaseService,
                                       EquipmentService equipmentService,
                                       WarehouseBarcodeService warehouseBarcodeService,
                                       AfterSalesService afterSalesService,
                                       AfterSalesWorkflowService afterSalesWorkflowService,
                                       CostService costService,
                                       CustomerPortalService customerPortalService,
                                       ObjectMapper objectMapper,
                                       AgentDomainCrudService domainCrudService) {
        this.catalog = catalog;
        this.mesWorkflowService = mesWorkflowService;
        this.systemService = systemService;
        this.attendanceService = attendanceService;
        this.productionProcessService = productionProcessService;
        this.qualityService = qualityService;
        this.purchaseService = purchaseService;
        this.equipmentService = equipmentService;
        this.warehouseBarcodeService = warehouseBarcodeService;
        this.afterSalesService = afterSalesService;
        this.afterSalesWorkflowService = afterSalesWorkflowService;
        this.costService = costService;
        this.customerPortalService = customerPortalService;
        this.objectMapper = objectMapper;
        this.domainCrudService = domainCrudService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Object execute(AgentActionPlan plan, LoginResponse session) {
        Map<String, Object> p = plan.parameters();
        String code = plan.actionCode();
        if (code.startsWith("crud.")) {
            return domainCrudService.execute(code, p);
        }
        if (catalog.isMesWorkflowAction(code)) {
            MesActionRequest request = new MesActionRequest();
            request.setAction(code);
            request.setPayload(p);
            request.setOperator(session.getUsername());
            request.setRoleKey(roleKey(session.getRoleCode()));
            return mesWorkflowService.execute(request);
        }

        return switch (code) {
            case "admin.user.create" -> createUser(p);
            case "admin.user.update" -> updateUser(p);
            case "admin.user.delete" -> done(() -> systemService.deleteUser(longVal(p, "userId")));
            case "admin.role.create" -> createRole(p);
            case "admin.role.update" -> updateRole(p);
            case "admin.role.delete" -> done(() -> systemService.deleteRole(longVal(p, "roleId")));
            case "admin.menu.create" -> createMenu(p);
            case "admin.menu.update" -> updateMenu(p);
            case "admin.menu.delete" -> done(() -> systemService.deleteMenu(longVal(p, "menuId")));
            case "admin.permission.create" -> createPermission(p);
            case "admin.permission.update" -> updatePermission(p);
            case "admin.permission.delete" -> done(() -> systemService.deletePermission(longVal(p, "permissionId")));
            case "admin.operationLog.delete" -> done(() -> systemService.deleteOperationLog(longVal(p, "logId")));
            case "attendance.checkIn" -> attendanceService.checkIn(session.getUserId());
            case "attendance.checkOut" -> attendanceService.checkOut(session.getUserId());
            case "attendance.schedule.save" -> saveSchedule(p, session);
            case "attendance.schedule.delete" -> done(() -> attendanceService.deleteSchedule(longVal(p, "scheduleId")));

            case "process.route.save" -> productionProcessService.saveRoute(convert(p, ProcessRoute.class), session.getUsername());
            case "process.route.disable" -> done(() -> productionProcessService.disableRoute(longVal(p, "routeId")));
            case "process.step.save" -> productionProcessService.saveStep(convert(p, ProcessStep.class));
            case "process.step.disable" -> done(() -> productionProcessService.disableStep(longVal(p, "stepId")));
            case "process.step.reorder" -> reorderSteps(p);

            case "quality.items.generate" -> qualityService.generateDefaultItems(longVal(p, "inspectionId"));
            case "quality.items.save" -> saveQualityItems(p);
            case "quality.evaluate" -> qualityService.evaluate(longVal(p, "inspectionId"));
            case "quality.pass" -> qualityService.passInspection(longVal(p, "inspectionId"), text(p, "remark"),
                    session.getUsername(), intVal(p, "sampleQuantity"), intVal(p, "qualifiedQuantity"), intVal(p, "unqualifiedQuantity"));
            case "quality.fail" -> qualityService.failInspection(longVal(p, "inspectionId"), text(p, "defectType"),
                    text(p, "defectReason"), decimal(p, "defectQuantity", BigDecimal.ONE), text(p, "severity"),
                    text(p, "remark"), session.getUsername());
            case "quality.recheck" -> qualityService.requireRecheck(longVal(p, "inspectionId"), text(p, "reason"), session.getUsername());
            case "quality.recheckPass" -> qualityService.recheckPass(longVal(p, "inspectionId"), text(p, "remark"), session.getUsername());
            case "quality.recheckFail" -> qualityService.recheckFail(longVal(p, "inspectionId"), text(p, "defectType"),
                    text(p, "defectReason"), decimal(p, "defectQuantity", BigDecimal.ONE), text(p, "severity"),
                    text(p, "remark"), session.getUsername());
            case "quality.close" -> qualityService.closeInspection(longVal(p, "inspectionId"), text(p, "remark"), session.getUsername());
            case "quality.nonconforming.handle" -> qualityService.handleNonconforming(longVal(p, "nonconformingId"),
                    text(p, "handleMethod"), text(p, "remark"), session.getUsername());
            case "quality.incoming.create" -> qualityService.createIncomingInspection(longVal(p, "materialId"), text(p, "batchNo"),
                    requiredInt(p, "lotQuantity"), requiredInt(p, "sampleQuantity"), session.getUsername());
            case "quality.sampling.update" -> qualityService.updateSampling(longVal(p, "inspectionId"),
                    requiredInt(p, "sampleQuantity"), requiredInt(p, "qualifiedQuantity"), requiredInt(p, "unqualifiedQuantity"));

            case "purchase.requirements.calculate" -> purchaseService.calculateRequirements();
            case "purchase.orders.generate" -> purchaseService.generatePurchaseOrder(convert(p, GeneratePurchaseRequest.class));
            case "purchase.requirement.select" -> done(() -> purchaseService.selectRequirement(longVal(p, "requirementId")));
            case "purchase.requirement.cancel" -> done(() -> purchaseService.cancelRequirement(longVal(p, "requirementId")));
            case "purchase.arrival.confirm" -> done(() -> purchaseService.confirmArrival(longVal(p, "purchaseOrderId")));
            case "purchase.arrival.confirmSlots" -> confirmArrivalWithSlots(p);
            case "purchase.order.revoke" -> done(() -> purchaseService.revokePurchaseOrder(longVal(p, "purchaseOrderId")));
            case "purchase.order.saveDraft" -> purchaseService.savePurchaseOrderDraft(convert(p, UpdatePurchaseOrderDraftRequest.class));

            case "warehouse.barcode.rule.save" -> warehouseBarcodeService.saveRule(convert(p, BarcodeRule.class));
            case "warehouse.barcode.generate" -> Map.of("barcodeNo", warehouseBarcodeService.generateBarcode(text(p, "businessType")));
            case "warehouse.barcode.scan" -> warehouseBarcodeService.scanBarcode(p, session.getUsername());

            case "equipment.alarm.trigger" -> equipmentService.triggerAlarm(longVal(p, "equipmentId"),
                    defaultText(text(p, "alarmType"), "EQUIPMENT"), defaultText(text(p, "alarmLevel"), "GENERAL"),
                    text(p, "description"), session.getUsername());
            case "equipment.alarm.receive" -> equipmentService.receiveAlarm(longVal(p, "alarmId"), session.getUsername());
            case "equipment.alarm.resolve" -> equipmentService.resolveAlarm(longVal(p, "alarmId"), text(p, "remark"), session.getUsername());
            case "equipment.maintenance.start" -> equipmentService.startMaintenance(longVal(p, "equipmentId"), nullableLong(p, "alarmId"),
                    defaultText(text(p, "maintenanceType"), "REPAIR"), text(p, "faultDescription"),
                    text(p, "maintenanceContent"), session.getUsername());
            case "equipment.maintenance.finish" -> equipmentService.finishMaintenance(longVal(p, "maintenanceId"),
                    defaultText(text(p, "result"), "COMPLETED"), decimal(p, "costAmount", BigDecimal.ZERO),
                    text(p, "maintenanceContent"), session.getUsername());

            case "aftersale.case.accept" -> afterSalesService.acceptCase(text(p, "caseNo"), session.getUsername());
            case "aftersale.case.resolve" -> afterSalesService.resolveCase(text(p, "caseNo"), text(p, "solution"),
                    text(p, "traceResult"), session.getUsername());
            case "aftersale.case.close" -> afterSalesService.closeCase(text(p, "caseNo"), text(p, "remark"), session.getUsername());
            case "aftersale.rca.dispatch" -> afterSalesService.dispatchRcaTasks(text(p, "caseNo"), stringList(p.get("departments")));
            case "aftersale.rca.confirm" -> afterSalesService.confirmRootCause(withOperator(p, session));
            case "aftersale.rca.task.update" -> afterSalesService.updateRcaTask(withOperator(p, session));
            case "aftersale.plan.save" -> afterSalesWorkflowService.savePlan(withOperator(p, session));
            case "aftersale.plan.submit" -> afterSalesWorkflowService.submitPlan(longVal(p, "planId"));
            case "aftersale.plan.approve" -> afterSalesWorkflowService.approvePlan(longVal(p, "planId"), session.getUsername());
            case "aftersale.plan.reject" -> afterSalesWorkflowService.rejectPlan(longVal(p, "planId"), text(p, "remark"));
            case "aftersale.task.update" -> afterSalesWorkflowService.updateTask(withOperator(p, session));
            case "aftersale.case.advance" -> afterSalesWorkflowService.advanceCase(text(p, "caseNo"), text(p, "targetStatus"));
            case "aftersale.closure.save" -> afterSalesWorkflowService.saveClosure(withOperator(p, session));
            case "aftersale.closure.confirmCustomer" -> afterSalesWorkflowService.confirmCustomer(text(p, "caseNo"));
            case "aftersale.closure.close" -> afterSalesWorkflowService.closeWithClosure(text(p, "caseNo"), session.getUsername());

            case "cost.settlement.save" -> saveSettlement(p);
            case "cost.settlement.confirm" -> costService.confirmSettlement(longVal(p, "settlementId"), session.getUsername());
            case "cost.settlement.export" -> costService.exportSettlement(longVal(p, "settlementId"), session.getUsername());

            case "customer.order.create" -> customerPortalService.createOrder(session, convert(p, CustomerCreateOrderRequest.class));
            case "customer.feedback.create" -> customerPortalService.submitFeedback(session, convert(p, CustomerFeedbackRequest.class));
            case "customer.profile.update" -> customerPortalService.updateProfile(session, convert(p, CustomerProfileUpdateRequest.class));
            default -> throw new BusinessException(400, "动作尚未配置执行器：" + code);
        };
    }

    private Object createUser(Map<String, Object> p) {
        User user = convert(p, User.class);
        systemService.insertUser(user);
        return user;
    }

    private Object updateUser(Map<String, Object> p) {
        User user = requireExisting(systemService.getUserById(longVal(p, "userId")), "用户不存在");
        update(user, p);
        systemService.updateUser(user);
        return user;
    }

    private Object createRole(Map<String, Object> p) {
        Role role = convert(p, Role.class);
        systemService.insertRole(role);
        return role;
    }

    private Object updateRole(Map<String, Object> p) {
        Role role = requireExisting(systemService.getRoleById(longVal(p, "roleId")), "角色不存在");
        update(role, p);
        systemService.updateRole(role);
        return role;
    }

    private Object createMenu(Map<String, Object> p) {
        SysMenu menu = convert(p, SysMenu.class);
        systemService.insertMenu(menu);
        return menu;
    }

    private Object updateMenu(Map<String, Object> p) {
        SysMenu menu = requireExisting(systemService.getMenuById(longVal(p, "menuId")), "菜单不存在");
        update(menu, p);
        systemService.updateMenu(menu);
        return menu;
    }

    private Object createPermission(Map<String, Object> p) {
        Permission permission = convert(p, Permission.class);
        systemService.insertPermission(permission);
        return permission;
    }

    private Object updatePermission(Map<String, Object> p) {
        Permission permission = requireExisting(systemService.getPermissionById(longVal(p, "permissionId")), "权限不存在");
        update(permission, p);
        systemService.updatePermission(permission);
        return permission;
    }

    private Object saveSchedule(Map<String, Object> p, LoginResponse session) {
        ShiftScheduleSaveRequest request = convert(p, ShiftScheduleSaveRequest.class);
        request.setCreatedBy(session.getUsername());
        attendanceService.saveSchedule(request);
        return Map.of("saved", true);
    }

    private Object reorderSteps(Map<String, Object> p) {
        productionProcessService.reorderSteps(longVal(p, "routeId"), longList(p.get("stepIds")));
        return Map.of("reordered", true);
    }

    private Object saveQualityItems(Map<String, Object> p) {
        List<QualityInspectionItem> items = objectMapper.convertValue(p.get("items"),
                new TypeReference<List<QualityInspectionItem>>() { });
        qualityService.saveItems(longVal(p, "inspectionId"), items);
        return Map.of("saved", true, "count", items.size());
    }

    private Object confirmArrivalWithSlots(Map<String, Object> p) {
        List<Map<String, Object>> assignments = objectMapper.convertValue(p.get("assignments"),
                new TypeReference<List<Map<String, Object>>>() { });
        purchaseService.confirmArrivalWithSlots(longVal(p, "purchaseOrderId"), assignments);
        return Map.of("confirmed", true);
    }

    private Object saveSettlement(Map<String, Object> p) {
        Long id = nullableLong(p, "settlementId");
        CostSettlement settlement;
        if (id == null) {
            settlement = convert(p, CostSettlement.class);
            costService.insertSettlement(settlement);
        } else {
            settlement = requireExisting(costService.getSettlementById(id), "成本结算不存在");
            update(settlement, p);
            costService.updateSettlement(settlement);
        }
        return settlement;
    }

    private Map<String, Object> withOperator(Map<String, Object> source, LoginResponse session) {
        Map<String, Object> result = new java.util.LinkedHashMap<>(source);
        result.put("operator", session.getUsername());
        return result;
    }

    private Object done(Runnable runnable) {
        runnable.run();
        return Map.of("success", true);
    }

    private <T> T convert(Map<String, Object> value, Class<T> type) {
        try {
            return objectMapper.convertValue(value, type);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "动作参数格式错误：" + e.getMessage());
        }
    }

    private void update(Object target, Map<String, Object> value) {
        try {
            objectMapper.updateValue(target, value);
        } catch (Exception e) {
            throw new BusinessException(400, "修改参数格式错误：" + e.getMessage());
        }
    }

    private <T> T requireExisting(T value, String message) {
        if (value == null) throw new BusinessException(404, message);
        return value;
    }

    private Long longVal(Map<String, Object> p, String key) {
        Long value = nullableLong(p, key);
        if (value == null || value <= 0) throw new BusinessException(400, key + " 必须是正整数");
        return value;
    }

    private Long nullableLong(Map<String, Object> p, String key) {
        Object value = p.get(key);
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            return value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BusinessException(400, key + " 必须是整数");
        }
    }

    private Integer intVal(Map<String, Object> p, String key) {
        if (!p.containsKey(key) || p.get(key) == null || String.valueOf(p.get(key)).isBlank()) return null;
        return requiredInt(p, key);
    }

    private int requiredInt(Map<String, Object> p, String key) {
        try {
            Object value = p.get(key);
            return value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            throw new BusinessException(400, key + " 必须是整数");
        }
    }

    private BigDecimal decimal(Map<String, Object> p, String key, BigDecimal fallback) {
        Object value = p.get(key);
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BusinessException(400, key + " 必须是数字");
        }
    }

    private String text(Map<String, Object> p, String key) {
        Object value = p.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<Long> longList(Object value) {
        if (!(value instanceof List<?> list)) throw new BusinessException(400, "参数必须是 ID 列表");
        try {
            return list.stream().map(v -> v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v))).toList();
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "ID 列表格式错误");
        }
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(String::valueOf).filter(s -> !s.isBlank()).toList();
    }

    private String roleKey(String roleCode) {
        if (roleCode == null) return "system";
        return switch (roleCode.toUpperCase()) {
            case "ADMIN" -> "admin";
            case "ORDER" -> "order";
            case "PLANNER" -> "planner";
            case "MANAGER" -> "manager";
            case "OPERATOR" -> "operator";
            case "QC" -> "quality";
            case "PURCHASER" -> "purchase";
            case "WAREHOUSE" -> "warehouse";
            case "DEVICE" -> "device";
            case "SERVICE" -> "aftersale";
            case "COST" -> "cost";
            case "CUSTOMER" -> "customer";
            default -> "system";
        };
    }
}
