package com.upc.computer.service;

import com.upc.computer.entity.*;
import com.upc.computer.mapper.*;
import com.upc.computer.service.MesRuntimeStore.MesRuntimeState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 构建前端兼容的 MES 快照
 */
@Service
public class MesSnapshotService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private CustomerOrderMapper customerOrderMapper;
    @Autowired
    private CustomerOrderItemMapper customerOrderItemMapper;
    @Autowired
    private ProductionPlanMapper productionPlanMapper;
    @Autowired
    private ProductionPlanItemMapper productionPlanItemMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;
    @Autowired
    private WorkReportMapper workReportMapper;
    @Autowired
    private QualityInspectionMapper qualityInspectionMapper;
    @Autowired
    private NonconformingProductMapper nonconformingProductMapper;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private BomMapper bomMapper;
    @Autowired
    private InventoryMapper inventoryMapper;
    @Autowired
    private InventoryTransactionMapper inventoryTransactionMapper;
    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;
    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;
    @Autowired
    private DeliveryOrderMapper deliveryOrderMapper;
    @Autowired
    private EquipmentMapper equipmentMapper;
    @Autowired
    private AndonAlarmMapper andonAlarmMapper;
    @Autowired
    private AfterSalesCaseMapper afterSalesCaseMapper;
    @Autowired
    private CostSettlementMapper costSettlementMapper;
    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private ProcessStepMapper processStepMapper;
    @Autowired
    private ProcessRouteMapper processRouteMapper;
    @Autowired
    private EquipmentMaintenanceRecordMapper equipmentMaintenanceRecordMapper;
    @Autowired
    private MesRuntimeStore mesRuntimeStore;
    @Autowired
    private OrderOcrService orderOcrService;

    public Map<String, Object> buildSnapshot() {
        List<User> users = userMapper.userList();
        List<Role> roles = roleMapper.roleList();
        List<CustomerOrder> orders = customerOrderMapper.customerOrderList();
        List<CustomerOrderItem> orderItems = customerOrderItemMapper.orderItemList();
        List<ProductionPlan> plans = productionPlanMapper.planList();
        List<ProductionPlanItem> planItems = productionPlanItemMapper.planItemList();
        List<WorkOrder> workOrders = workOrderMapper.workOrderList();
        List<DispatchTask> dispatches = dispatchTaskMapper.dispatchList();
        List<WorkReport> reports = workReportMapper.reportList();
        List<QualityInspection> inspections = qualityInspectionMapper.inspectionList();
        List<NonconformingProduct> defects = nonconformingProductMapper.nonconformingList();
        List<Material> materials = materialMapper.materialList();
        List<Inventory> inventories = inventoryMapper.inventoryList();
        List<InventoryTransaction> transactions = inventoryTransactionMapper.transactionList();
        List<PurchaseOrder> purchaseOrders = purchaseOrderMapper.purchaseOrderList();
        List<PurchaseOrderItem> purchaseItems = purchaseOrderItemMapper.purchaseOrderItemList();
        List<DeliveryOrder> deliveries = deliveryOrderMapper.deliveryList();
        List<Equipment> equipmentList = equipmentMapper.equipmentList();
        List<AndonAlarm> alarms = andonAlarmMapper.alarmList();
        List<AfterSalesCase> aftersaleCases = afterSalesCaseMapper.afterSalesCaseList();
        List<CostSettlement> settlements = costSettlementMapper.settlementList();
        List<OperationLog> dbLogs = operationLogMapper.operationLogList();
        List<ProcessStep> steps = processStepMapper.stepList();
        List<ProcessRoute> routes = processRouteMapper.routeList();
        List<EquipmentMaintenanceRecord> maintenanceRecords = equipmentMaintenanceRecordMapper.maintenanceList();

        MesRuntimeState runtime = mesRuntimeStore.load();

        Map<Long, User> userById = users.stream().collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));
        Map<Long, Role> roleById = roles.stream().collect(Collectors.toMap(Role::getRoleId, r -> r, (a, b) -> a));
        Map<Long, CustomerOrder> orderById = orders.stream().collect(Collectors.toMap(CustomerOrder::getOrderId, o -> o, (a, b) -> a));
        Map<String, CustomerOrder> orderByNo = orders.stream().collect(Collectors.toMap(CustomerOrder::getOrderNo, o -> o, (a, b) -> a));
        Map<Long, List<CustomerOrderItem>> itemsByOrderId = orderItems.stream().collect(Collectors.groupingBy(CustomerOrderItem::getOrderId));
        Map<Long, ProductionPlan> planById = plans.stream().collect(Collectors.toMap(ProductionPlan::getPlanId, p -> p, (a, b) -> a));
        Map<Long, ProductionPlan> planByOrderId = plans.stream()
                .filter(p -> p.getSourceOrderId() != null)
                .collect(Collectors.toMap(ProductionPlan::getSourceOrderId, p -> p, (a, b) -> a));
        Map<Long, WorkOrder> woById = workOrders.stream().collect(Collectors.toMap(WorkOrder::getWorkOrderId, w -> w, (a, b) -> a));
        Map<Long, WorkOrder> woByPlanId = workOrders.stream()
                .filter(w -> w.getPlanId() != null)
                .collect(Collectors.toMap(WorkOrder::getPlanId, w -> w, (a, b) -> a));
        Map<Long, Material> materialById = materials.stream().collect(Collectors.toMap(Material::getMaterialId, m -> m, (a, b) -> a));
        Map<Long, ProcessStep> stepById = steps.stream().collect(Collectors.toMap(ProcessStep::getStepId, s -> s, (a, b) -> a));
        Map<Long, Equipment> equipmentById = equipmentList.stream().collect(Collectors.toMap(Equipment::getEquipmentId, e -> e, (a, b) -> a));
        Map<Long, PurchaseOrder> poById = purchaseOrders.stream().collect(Collectors.toMap(PurchaseOrder::getPurchaseOrderId, p -> p, (a, b) -> a));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("sysUsers", mapUsers(users, roleById));
        snapshot.put("sysRoles", mapRoles(roles, users));
        snapshot.put("orders", mapOrders(orders, itemsByOrderId, planByOrderId, woByPlanId, deliveries, userById, materialById, runtime));
        snapshot.put("plans", mapPlans(plans, planItems, orderById, itemsByOrderId, userById, runtime));
        snapshot.put("workOrders", mapWorkOrders(workOrders, planById, orderById, itemsByOrderId, userById, dispatches, stepById, equipmentById));
        snapshot.put("dispatches", mapDispatches(dispatches, woById, userById, stepById, equipmentById, runtime));
        snapshot.put("workReports", mapReports(reports, woById, dispatches, userById, stepById));
        snapshot.put("inspections", mapInspections(inspections, woById, planById, itemsByOrderId, orderById, userById, runtime));
        snapshot.put("defects", mapDefects(defects, woById, inspections, dispatches, userById, runtime));
        snapshot.put("qualityReports", mapQualityReports(runtime));
        snapshot.put("purchaseOrders", mapPurchaseOrders(purchaseOrders, purchaseItems, materialById, userById, runtime));
        snapshot.put("inventory", mapInventory(inventories, materialById));
        snapshot.put("stockFlows", mapStockFlows(transactions, materialById, poById, woById, userById));
        snapshot.put("deliveries", mapDeliveries(deliveries, orderById, itemsByOrderId));
        snapshot.put("equipment", mapEquipment(equipmentList, runtime));
        snapshot.put("alarms", mapAlarms(alarms, woById, equipmentById, userById, runtime));
        snapshot.put("maintenanceRecords", mapMaintenanceRecords(maintenanceRecords, equipmentById, userById));
        snapshot.put("aftersaleCases", mapAftersaleCases(aftersaleCases, orderById, itemsByOrderId, userById));
        snapshot.put("costSettlements", mapCostSettlements(settlements, woById, itemsByOrderId, orderById));
        snapshot.put("operationLogs", mapDbLogs(dbLogs, userById));
        snapshot.put("customers", CustomerCatalog.buildList(orders, deliveries, aftersaleCases));
        snapshot.put("suppliers", buildSuppliers(purchaseOrders));
        snapshot.put("purchaseDemands", buildPurchaseDemands(inventories, materialById));
        snapshot.put("processGuide", buildProcessGuide(materials, materialById, routes, steps));
        snapshot.put("bomGuide", buildBomGuide(materials, materialById));
        snapshot.put("productModels", buildProductModels(materials));
        snapshot.put("processSteps", buildProcessStepNames());

        mesRuntimeStore.mergeIntoSnapshot(snapshot, runtime);
        return snapshot;
    }

    private List<Map<String, Object>> mapUsers(List<User> users, Map<Long, Role> roleById) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : users) {
            Role role = roleById.get(u.getRoleId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getUserId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("roleKey", role != null ? MesStatusMapper.toRoleKey(role.getRoleCode()) : "");
            m.put("roleName", role != null ? role.getRoleName() : "待分配");
            m.put("pendingRole", role == null);
            m.put("phone", u.getPhone());
            m.put("department", u.getDepartment() != null ? u.getDepartment() : "");
            m.put("email", u.getEmail() != null ? u.getEmail() : "");
            m.put("status", u.getStatus() != null && u.getStatus() == 1 ? "启用" : "禁用");
            m.put("createdAt", fmt(u.getCreatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapRoles(List<Role> roles, List<User> users) {
        Map<String, Long> countByKey = new HashMap<>();
        Map<Long, Role> roleById = roles.stream().collect(Collectors.toMap(Role::getRoleId, r -> r, (a, b) -> a));
        for (User u : users) {
            Role r = roleById.get(u.getRoleId());
            if (r != null) {
                String key = MesStatusMapper.toRoleKey(r.getRoleCode());
                countByKey.merge(key, 1L, Long::sum);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Role r : roles) {
            String roleKey = MesStatusMapper.toRoleKey(r.getRoleCode());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getRoleId());
            m.put("roleKey", roleKey);
            m.put("roleName", r.getRoleName());
            m.put("permCount", 10);
            m.put("userCount", countByKey.getOrDefault(roleKey, 0L).intValue());
            m.put("status", r.getStatus() != null && r.getStatus() == 1 ? "启用" : "禁用");
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapOrders(List<CustomerOrder> orders,
                                                  Map<Long, List<CustomerOrderItem>> itemsByOrderId,
                                                  Map<Long, ProductionPlan> planByOrderId,
                                                  Map<Long, WorkOrder> woByPlanId,
                                                  List<DeliveryOrder> deliveries,
                                                  Map<Long, User> userById,
                                                  Map<Long, Material> materialById,
                                                  MesRuntimeState runtime) {
        Set<Long> shippedOrderIds = deliveries.stream()
                .filter(d -> "SHIPPED".equals(d.getDeliveryStatus()))
                .map(DeliveryOrder::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Map<String, Object>> list = new ArrayList<>();
        for (CustomerOrder o : orders) {
            CustomerOrderItem item = firstItem(itemsByOrderId.get(o.getOrderId()));
            ProductionPlan plan = planByOrderId.get(o.getOrderId());
            WorkOrder wo = plan != null ? woByPlanId.get(plan.getPlanId()) : null;
            Map<String, Object> extra = runtime.getExtras().getOrDefault("order:" + o.getOrderNo(), Map.of());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getOrderNo());
            m.put("customerId", extra.getOrDefault("customerId", customerId(o.getCustomerName())));
            m.put("customerName", o.getCustomerName());
            m.put("productModel", item != null ? item.getProductName() : extra.getOrDefault("productModel", ""));
            m.put("specification", item != null && item.getSpecification() != null ? item.getSpecification() : "");
            m.put("panelType", extra.getOrDefault("panelType", panelType(item)));
            m.put("quantity", item != null ? intVal(item.getQuantity()) : extra.getOrDefault("quantity", 0));
            m.put("unitPrice", item != null && item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0);
            if (item != null && item.getMaterialId() != null) {
                Material mat = materialById.get(item.getMaterialId());
                if (mat != null) {
                    m.put("materialCode", mat.getMaterialCode());
                    if (m.get("specification") == null || String.valueOf(m.get("specification")).isBlank()) {
                        m.put("specification", mat.getSpecification());
                    }
                }
            }
            m.put("deliveryDate", o.getRequiredDeliveryDate() != null ? o.getRequiredDeliveryDate().format(DATE_FMT) : "");
            m.put("status", deriveOrderStatus(o, plan, wo, shippedOrderIds.contains(o.getOrderId())));
            m.put("amount", o.getOrderAmount() != null ? o.getOrderAmount().doubleValue() : 0);
            User creator = userById.get(o.getCreatedBy());
            m.put("salesPerson", creator != null ? creator.getRealName() : extra.getOrDefault("salesPerson", ""));
            m.put("remark", o.getRemark());
            User auditor = userById.get(o.getAuditUserId());
            m.put("auditOpinion", o.getAuditOpinion() != null ? o.getAuditOpinion() : "");
            m.put("auditAt", o.getAuditAt() != null ? fmt(o.getAuditAt()) : "");
            m.put("auditor", auditor != null ? auditor.getRealName() : "");
            m.put("attachments", resolveOrderAttachments(o.getOrderNo(), extra));
            m.put("auditRecords", extra.getOrDefault("auditRecords", List.of()));
            m.put("auditFlag", extra.getOrDefault("auditFlag", ""));
            if (plan != null) {
                m.put("planId", plan.getPlanNo());
            }
            if (wo != null) {
                m.put("workOrderId", wo.getWorkOrderNo());
            }
            m.put("createdAt", fmt(o.getCreatedAt()));
            m.put("updatedAt", fmt(o.getUpdatedAt()));
            list.add(m);
        }
        return list;
    }

    public static String deriveOrderStatus(CustomerOrder order, ProductionPlan plan, WorkOrder wo, boolean shipped) {
        if ("REJECTED".equals(order.getAuditStatus())) {
            return "已作废";
        }
        if ("PENDING".equals(order.getAuditStatus())) {
            return "待审核";
        }
        if ("PLAN_PENDING".equals(order.getAuditStatus())) {
            return "待计划";
        }
        if (shipped || "SHIPPED".equals(order.getAuditStatus())) {
            return "已发货";
        }
        if (wo != null || "PRODUCING".equals(order.getAuditStatus())) {
            return "生产中";
        }
        if (plan != null || "PLANNED".equals(order.getAuditStatus())) {
            return "已计划";
        }
        if ("APPROVED".equals(order.getAuditStatus())) {
            return "已审核";
        }
        return MesStatusMapper.toOrderCn(order.getAuditStatus());
    }

    private List<Map<String, Object>> mapPlans(List<ProductionPlan> plans,
                                               List<ProductionPlanItem> planItems,
                                               Map<Long, CustomerOrder> orderById,
                                               Map<Long, List<CustomerOrderItem>> itemsByOrderId,
                                               Map<Long, User> userById,
                                               MesRuntimeState runtime) {
        Map<Long, ProductionPlanItem> planItemByPlanId = planItems.stream()
                .collect(Collectors.toMap(ProductionPlanItem::getPlanId, pi -> pi, (a, b) -> a));
        List<Map<String, Object>> list = new ArrayList<>();
        for (ProductionPlan p : plans) {
            CustomerOrder order = orderById.get(p.getSourceOrderId());
            CustomerOrderItem item = order != null ? firstItem(itemsByOrderId.get(order.getOrderId())) : null;
            ProductionPlanItem planItem = planItemByPlanId.get(p.getPlanId());
            User planner = userById.get(p.getPlannerId());
            Map<String, Object> extra = runtime.getExtras().getOrDefault("plan:" + p.getPlanNo(), Map.of());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getPlanNo());
            m.put("orderId", order != null ? order.getOrderNo() : "");
            m.put("orderNo", order != null ? order.getOrderNo() : "");
            m.put("productModel", item != null ? item.getProductName() : "");
            m.put("orderQuantity", item != null ? intVal(item.getQuantity()) : 0);
            m.put("quantity", planItem != null ? intVal(planItem.getPlannedQuantity())
                    : (item != null ? intVal(item.getQuantity()) : 0));
            m.put("planStart", p.getPlannedStartDate() != null ? p.getPlannedStartDate().format(DATE_FMT) : "");
            m.put("planEnd", p.getPlannedEndDate() != null ? p.getPlannedEndDate().format(DATE_FMT) : "");
            m.put("status", MesStatusMapper.toPlanCn(p.getPlanStatus()));
            m.put("priority", p.getPriority());
            m.put("planner", planner != null ? planner.getRealName() : "");
            m.put("remark", p.getRemark());
            m.put("versionNo", p.getVersionNo() != null ? p.getVersionNo() : "V1");
            m.put("parentPlanNo", p.getParentPlanNo() != null ? p.getParentPlanNo() : "");
            m.put("adjustReason", p.getAdjustReason() != null ? p.getAdjustReason() : "");
            m.put("schedulingMode", p.getSchedulingMode() != null ? p.getSchedulingMode() : "MANUAL");
            if (p.getApprovedAt() != null) {
                m.put("submittedAt", fmt(p.getApprovedAt()));
            }
            m.put("createdAt", fmt(p.getCreatedAt()));
            m.put("updatedAt", fmt(p.getUpdatedAt()));

            if (!extra.isEmpty()) {
                m.put("agentGenerated", Boolean.TRUE.equals(extra.get("agentGenerated")));
                if (extra.get("agentRecommendation") != null) {
                    m.put("agentRecommendation", extra.get("agentRecommendation"));
                }
                if (extra.get("dispatchSuggestions") != null) {
                    m.put("dispatchSuggestions", extra.get("dispatchSuggestions"));
                }
            }
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapWorkOrders(List<WorkOrder> workOrders,
                                                    Map<Long, ProductionPlan> planById,
                                                    Map<Long, CustomerOrder> orderById,
                                                    Map<Long, List<CustomerOrderItem>> itemsByOrderId,
                                                    Map<Long, User> userById,
                                                    List<DispatchTask> dispatches,
                                                    Map<Long, ProcessStep> stepById,
                                                    Map<Long, Equipment> equipmentById) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (WorkOrder wo : workOrders) {
            ProductionPlan plan = planById.get(wo.getPlanId());
            CustomerOrder order = plan != null ? orderById.get(plan.getSourceOrderId()) : null;
            CustomerOrderItem item = order != null ? firstItem(itemsByOrderId.get(order.getOrderId())) : null;
            User manager = userById.get(wo.getCreatedBy());
            List<DispatchTask> woDispatches = dispatches.stream()
                    .filter(d -> wo.getWorkOrderId().equals(d.getWorkOrderId()))
                    .toList();
            int finishedQty = ProductionWorkshopCatalog.finishedGoodsQty(woDispatches, stepById);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", wo.getWorkOrderNo());
            m.put("planId", plan != null ? plan.getPlanNo() : "");
            m.put("orderId", order != null ? order.getOrderNo() : "");
            m.put("orderNo", order != null ? order.getOrderNo() : "");
            m.put("productModel", item != null ? item.getProductName() : "");
            m.put("quantity", intVal(wo.getPlannedQuantity()));
            m.put("completedQty", finishedQty);
            m.put("qualifiedQty", intVal(wo.getQualifiedQuantity()));
            m.put("status", MesStatusMapper.toWorkOrderCn(wo.getStatus()));
            m.put("line", resolveWorkOrderLine(woDispatches, equipmentById));
            m.put("manager", manager != null ? manager.getRealName() : "");
            m.put("createdAt", fmt(wo.getCreatedAt()));
            m.put("updatedAt", fmt(wo.getUpdatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapDispatches(List<DispatchTask> dispatches,
                                                    Map<Long, WorkOrder> woById,
                                                    Map<Long, User> userById,
                                                    Map<Long, ProcessStep> stepById,
                                                    Map<Long, Equipment> equipmentById,
                                                    MesRuntimeState runtime) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DispatchTask d : dispatches) {
            WorkOrder wo = woById.get(d.getWorkOrderId());
            ProcessStep step = stepById.get(d.getStepId());
            Equipment eq = equipmentById.get(d.getEquipmentId());
            User op = userById.get(d.getOperatorId());
            Map<String, Object> extra = runtime.getExtras().getOrDefault("dispatch:" + d.getDispatchNo(), Map.of());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getDispatchNo());
            m.put("workOrderId", wo != null ? wo.getWorkOrderNo() : "");
            m.put("workOrderNo", wo != null ? wo.getWorkOrderNo() : "");
            m.put("processStep", extra.getOrDefault("processStep", step != null ? step.getStepName() : ""));
            m.put("productionStep", ProductionWorkshopCatalog.isProductionStep(step));
            m.put("finalProductionStep", ProductionWorkshopCatalog.isFinalProductionStep(step));
            ProductionWorkshopCatalog.ProcessStageDef stage = ProductionWorkshopCatalog.stageForStep(step);
            ProductionWorkshopCatalog.WorkshopDef opWs = OperatorWorkshopCatalog.workshopForOperator(
                    op != null ? op.getUsername() : String.valueOf(extra.getOrDefault("operator", "")));
            String workshopName = extra.containsKey("workshopName")
                    ? String.valueOf(extra.get("workshopName"))
                    : (opWs != null ? opWs.workshopName() : resolveWorkshopName(step, eq));
            m.put("workshopName", workshopName);
            m.put("operatorWorkshop", opWs != null ? opWs.workshopName() : workshopName);
            m.put("stageName", stage != null ? stage.stepName() : "");
            m.put("stageOrder", stage != null ? stage.stepOrder() : 0);
            m.put("totalStages", ProductionWorkshopCatalog.PRODUCTION_STAGES.size());
            m.put("equipment", extra.getOrDefault("equipment", eq != null ? eq.getEquipmentName() : ""));
            m.put("equipmentId", d.getEquipmentId());
            m.put("dispatchId", d.getDispatchId());
            m.put("dispatchNo", d.getDispatchNo());
            m.put("operator", op != null ? op.getUsername()
                    : String.valueOf(extra.getOrDefault("operator", "")));
            m.put("operatorName", extra.getOrDefault("operatorName", op != null ? op.getRealName() : ""));
            m.put("planQty", intVal(d.getAssignedQuantity()));
            m.put("completedQty", intVal(d.getCompletedQuantity()));
            m.put("status", MesStatusMapper.toDispatchCn(d.getStatus()));
            m.put("planStart", extra.getOrDefault("planStart", fmt(d.getAssignedAt())));
            m.put("planEnd", extra.getOrDefault("planEnd", ""));
            if (extra.containsKey("defectId")) {
                m.put("defectId", extra.get("defectId"));
            }
            m.put("createdAt", fmt(d.getCreatedAt()));
            m.put("updatedAt", fmt(d.getUpdatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapReports(List<WorkReport> reports,
                                                 Map<Long, WorkOrder> woById,
                                                 List<DispatchTask> dispatches,
                                                 Map<Long, User> userById,
                                                 Map<Long, ProcessStep> stepById) {
        Map<Long, DispatchTask> dispatchById = dispatches.stream()
                .collect(Collectors.toMap(DispatchTask::getDispatchId, d -> d, (a, b) -> a));
        List<Map<String, Object>> list = new ArrayList<>();
        for (WorkReport r : reports) {
            WorkOrder wo = woById.get(r.getWorkOrderId());
            DispatchTask d = dispatchById.get(r.getDispatchId());
            ProcessStep step = stepById.get(r.getStepId());
            User op = userById.get(r.getOperatorId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getReportNo());
            m.put("dispatchId", d != null ? d.getDispatchNo() : "");
            m.put("workOrderId", wo != null ? wo.getWorkOrderNo() : "");
            m.put("processStep", step != null ? step.getStepName() : "");
            m.put("operator", op != null ? op.getUsername() : "");
            m.put("operatorName", op != null ? op.getRealName() : "");
            m.put("reportQty", intVal(r.getCompletedQuantity()));
            m.put("qualifiedQty", intVal(r.getQualifiedQuantity()));
            m.put("unqualifiedQty", intVal(r.getUnqualifiedQuantity()));
            m.put("workHours", r.getWorkHours() != null ? r.getWorkHours().doubleValue() : 0);
            m.put("startTime", r.getStartTime() != null ? fmt(r.getStartTime()) : "");
            m.put("endTime", r.getEndTime() != null ? fmt(r.getEndTime()) : "");
            m.put("status", MesStatusMapper.toReportCn(r.getReportStatus()));
            m.put("remark", r.getRemark());
            m.put("createdAt", fmt(r.getCreatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapInspections(List<QualityInspection> inspections,
                                                     Map<Long, WorkOrder> woById,
                                                     Map<Long, ProductionPlan> planById,
                                                     Map<Long, List<CustomerOrderItem>> itemsByOrderId,
                                                     Map<Long, CustomerOrder> orderById,
                                                     Map<Long, User> userById,
                                                     MesRuntimeState runtime) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (QualityInspection qi : inspections) {
            WorkOrder wo = woById.get(qi.getWorkOrderId());
            CustomerOrder order = null;
            CustomerOrderItem item = null;
            if (wo != null && wo.getPlanId() != null) {
                ProductionPlan plan = planById.get(wo.getPlanId());
                if (plan != null) {
                    order = orderById.get(plan.getSourceOrderId());
                    if (order != null) {
                        item = firstItem(itemsByOrderId.get(order.getOrderId()));
                    }
                }
            }
            User inspector = userById.get(qi.getInspectorId());
            Map<String, Object> extra = runtime.getExtras().getOrDefault("inspection:" + qi.getInspectionNo(), Map.of());
            String resultCn = MesStatusMapper.inspectionResultToCn(qi.getInspectionResult());
            boolean pending = qi.getInspectionResult() == null || qi.getInspectionResult().isBlank()
                    || "PENDING".equals(qi.getInspectionResult());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", qi.getInspectionNo());
            m.put("reportId", extra.get("reportId"));
            m.put("dispatchId", extra.getOrDefault("dispatchId", ""));
            m.put("defectId", extra.getOrDefault("defectId", ""));
            m.put("workOrderId", wo != null ? wo.getWorkOrderNo() : "");
            m.put("batchNo", qi.getBatchNo());
            m.put("productModel", item != null ? item.getProductName() : extra.getOrDefault("productModel", ""));
            m.put("qcType", qi.getInspectionType());
            m.put("qcItems", extra.getOrDefault("qcItems", List.of("外观检查", "点亮测试", "坏点检测")));
            m.put("submitQty", extra.getOrDefault("submitQty", intVal(qi.getQualifiedQuantity())));
            m.put("sampleQty", intVal(qi.getSampleQuantity()));
            m.put("qualifiedQty", intVal(qi.getQualifiedQuantity()));
            m.put("unqualifiedQty", intVal(qi.getUnqualifiedQuantity()));
            m.put("result", pending ? "" : resultCn);
            m.put("status", pending ? "待检" : resultCn);
            m.put("operatorName", extra.getOrDefault("operatorName", ""));
            m.put("inspector", inspector != null ? inspector.getUsername() : "");
            m.put("inspectorName", extra.getOrDefault("inspectorName", inspector != null ? inspector.getRealName() : ""));
            m.put("remark", qi.getRemark());
            m.put("createdAt", fmt(qi.getCreatedAt()));
            m.put("updatedAt", fmt(qi.getUpdatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapDefects(List<NonconformingProduct> defects,
                                                 Map<Long, WorkOrder> woById,
                                                 List<QualityInspection> inspections,
                                                 List<DispatchTask> dispatches,
                                                 Map<Long, User> userById,
                                                 MesRuntimeState runtime) {
        Map<Long, QualityInspection> inspById = inspections.stream()
                .collect(Collectors.toMap(QualityInspection::getInspectionId, i -> i, (a, b) -> a));
        Map<Long, DispatchTask> dispatchById = dispatches.stream()
                .collect(Collectors.toMap(DispatchTask::getDispatchId, d -> d, (a, b) -> a));
        List<Map<String, Object>> list = new ArrayList<>();
        for (NonconformingProduct d : defects) {
            WorkOrder wo = woById.get(d.getWorkOrderId());
            QualityInspection qi = inspById.get(d.getInspectionId());
            Map<String, Object> extra = runtime.getExtras().getOrDefault("defect:" + d.getNonconformingNo(), Map.of());
            User handler = userById.get(d.getHandledBy());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getNonconformingNo());
            m.put("qcId", qi != null ? qi.getInspectionNo() : "");
            m.put("dispatchId", extra.getOrDefault("dispatchId", ""));
            m.put("workOrderId", wo != null ? wo.getWorkOrderNo() : "");
            m.put("batchNo", d.getBatchNo());
            m.put("productModel", extra.getOrDefault("productModel", ""));
            m.put("quantity", intVal(d.getQuantity()));
            m.put("defectLocation", extra.getOrDefault("defectLocation", d.getDefectType()));
            m.put("failedItems", extra.getOrDefault("failedItems", List.of()));
            m.put("severity", d.getSeverity());
            m.put("description", d.getDefectDescription());
            m.put("disposition", d.getHandleMethod());
            m.put("status", MesStatusMapper.toDefectCn(d.getHandleStatus()));
            m.put("operator", extra.getOrDefault("operator", ""));
            m.put("operatorName", extra.getOrDefault("operatorName", ""));
            m.put("handler", handler != null ? handler.getUsername() : "");
            m.put("createdAt", fmt(d.getCreatedAt()));
            m.put("updatedAt", fmt(d.getUpdatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapQualityReports(MesRuntimeState runtime) {
        return runtime.getExtras().entrySet().stream()
                .filter(e -> e.getKey().startsWith("qualityReport:"))
                .map(Map.Entry::getValue)
                .sorted((a, b) -> String.valueOf(b.getOrDefault("createdAt", ""))
                        .compareTo(String.valueOf(a.getOrDefault("createdAt", ""))))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Map<String, Object>> mapPurchaseOrders(List<PurchaseOrder> purchaseOrders,
                                                        List<PurchaseOrderItem> purchaseItems,
                                                        Map<Long, Material> materialById,
                                                        Map<Long, User> userById,
                                                        MesRuntimeState runtime) {
        Map<Long, PurchaseOrderItem> itemByPoId = purchaseItems.stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getPurchaseOrderId, i -> i, (a, b) -> a));
        List<Map<String, Object>> list = new ArrayList<>();
        for (PurchaseOrder po : purchaseOrders) {
            PurchaseOrderItem item = itemByPoId.get(po.getPurchaseOrderId());
            Material mat = item != null ? materialById.get(item.getMaterialId()) : null;
            Map<String, Object> extra = runtime.getExtras().getOrDefault("purchase:" + po.getPurchaseOrderNo(), Map.of());
            User buyer = userById.get(po.getPurchaserId());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", po.getPurchaseOrderNo());
            m.put("supplier", po.getSupplierName());
            m.put("materialCode", mat != null ? mat.getMaterialCode() : extra.getOrDefault("materialCode", ""));
            m.put("materialName", mat != null ? mat.getMaterialName() : extra.getOrDefault("materialName", ""));
            m.put("quantity", item != null ? intVal(item.getQuantity()) : extra.getOrDefault("quantity", 0));
            m.put("unitPrice", item != null && item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : extra.getOrDefault("unitPrice", 0));
            m.put("totalAmount", po.getTotalAmount() != null ? po.getTotalAmount().doubleValue() : 0);
            m.put("status", MesStatusMapper.toPurchaseOrderCn(po.getStatus()));
            m.put("expectedDate", po.getExpectedArrivalDate() != null ? po.getExpectedArrivalDate().format(DATE_FMT) : "");
            m.put("arrivedQty", item != null ? intVal(item.getReceivedQuantity()) : extra.getOrDefault("arrivedQty", 0));
            m.put("buyer", buyer != null ? buyer.getRealName() : "");
            m.put("createdAt", fmt(po.getCreatedAt()));
            m.put("updatedAt", fmt(po.getUpdatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapInventory(List<Inventory> inventories, Map<Long, Material> materialById) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Inventory inv : inventories) {
            Material mat = materialById.get(inv.getMaterialId());
            BigDecimal qty = inv.getQuantityOnHand() != null ? inv.getQuantityOnHand() : BigDecimal.ZERO;
            BigDecimal safe = mat != null && mat.getSafetyStock() != null ? mat.getSafetyStock() : BigDecimal.valueOf(100);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", "INV-" + (mat != null ? mat.getMaterialCode() : inv.getInventoryId()));
            m.put("materialCode", mat != null ? mat.getMaterialCode() : "");
            m.put("materialName", mat != null ? mat.getMaterialName() : "");
            m.put("assemblyGroup", resolveAssemblyGroup(mat));
            m.put("specification", mat != null ? mat.getSpecification() : "");
            m.put("unit", mat != null ? mat.getUnit() : "个");
            m.put("quantity", qty.intValue());
            m.put("safeQty", safe.intValue());
            m.put("status", qty.compareTo(safe) < 0 ? "预警" : MesStatusMapper.toInventoryCn(inv.getInventoryStatus()));
            m.put("location", inv.getLocationCode() != null ? inv.getLocationCode() : inv.getWarehouseName());
            m.put("updatedAt", fmt(inv.getUpdatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapStockFlows(List<InventoryTransaction> transactions,
                                                    Map<Long, Material> materialById,
                                                    Map<Long, PurchaseOrder> poById,
                                                    Map<Long, WorkOrder> woById,
                                                    Map<Long, User> userById) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (InventoryTransaction t : transactions) {
            Material mat = materialById.get(t.getMaterialId());
            User handler = userById.get(t.getHandledBy());
            String refNo = "";
            if (t.getRelatedPurchaseOrderId() != null && poById.containsKey(t.getRelatedPurchaseOrderId())) {
                refNo = poById.get(t.getRelatedPurchaseOrderId()).getPurchaseOrderNo();
            } else if (t.getRelatedWorkOrderId() != null && woById.containsKey(t.getRelatedWorkOrderId())) {
                refNo = woById.get(t.getRelatedWorkOrderId()).getWorkOrderNo();
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getTransactionNo());
            m.put("flowType", flowTypeCn(t.getTransactionType()));
            m.put("materialCode", mat != null ? mat.getMaterialCode() : "");
            m.put("materialName", mat != null ? mat.getMaterialName() : "");
            m.put("quantity", intVal(t.getQuantity()));
            m.put("direction", direction(t.getTransactionType()));
            m.put("refNo", refNo);
            m.put("operator", handler != null ? handler.getRealName() : "");
            m.put("createdAt", fmt(t.getHandledAt() != null ? t.getHandledAt() : t.getCreatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapDeliveries(List<DeliveryOrder> deliveries,
                                                    Map<Long, CustomerOrder> orderById,
                                                    Map<Long, List<CustomerOrderItem>> itemsByOrderId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DeliveryOrder d : deliveries) {
            CustomerOrder order = orderById.get(d.getOrderId());
            CustomerOrderItem item = order != null ? firstItem(itemsByOrderId.get(order.getOrderId())) : null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getDeliveryNo());
            m.put("orderId", order != null ? order.getOrderNo() : "");
            m.put("orderNo", order != null ? order.getOrderNo() : "");
            m.put("customerName", d.getCustomerName());
            m.put("productModel", item != null ? item.getProductName() : "");
            m.put("quantity", intVal(d.getDeliveryQuantity()));
            m.put("status", MesStatusMapper.toDeliveryCn(d.getDeliveryStatus()));
            m.put("shipDate", d.getDeliveryDate() != null ? d.getDeliveryDate().format(DATE_FMT) : "");
            m.put("trackingNo", d.getLogisticsNo());
            m.put("createdAt", fmt(d.getCreatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapEquipment(List<Equipment> equipmentList, MesRuntimeState runtime) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Equipment e : equipmentList) {
            Map<String, Object> extra = runtime.getExtras().getOrDefault("equipment:" + e.getEquipmentCode(), Map.of());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getEquipmentCode());
            m.put("name", e.getEquipmentName());
            m.put("type", e.getEquipmentType());
            m.put("line", e.getWorkshop() != null ? e.getWorkshop() : e.getWorkstation());
            m.put("status", MesStatusMapper.toEquipmentCn(e.getStatus()));
            m.put("lastMaint", e.getLastMaintenanceAt() != null ? e.getLastMaintenanceAt().format(DATE_FMT) : "");
            m.put("downtimeHours", extra.getOrDefault("downtimeHours", 0));
            m.put("updatedAt", fmt(e.getUpdatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapAlarms(List<AndonAlarm> alarms,
                                                Map<Long, WorkOrder> woById,
                                                Map<Long, Equipment> equipmentById,
                                                Map<Long, User> userById,
                                                MesRuntimeState runtime) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AndonAlarm a : alarms) {
            WorkOrder wo = woById.get(a.getWorkOrderId());
            Equipment eq = equipmentById.get(a.getEquipmentId());
            User reporter = userById.get(a.getReportedBy());
            User assignee = userById.get(a.getReceivedBy());
            Map<String, Object> extra = runtime.getExtras().getOrDefault("alarm:" + a.getAlarmNo(), Map.of());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getAlarmNo());
            m.put("type", alarmTypeCn(a.getAlarmType()));
            m.put("typeCode", a.getAlarmType());
            m.put("source", buildAlarmSource(eq, extra));
            m.put("workshop", eq != null && eq.getWorkshop() != null ? eq.getWorkshop() : extra.getOrDefault("workshop", ""));
            m.put("equipmentName", eq != null ? eq.getEquipmentName() : extra.getOrDefault("equipmentName", ""));
            m.put("workOrderId", wo != null ? wo.getWorkOrderNo() : "");
            m.put("level", alarmLevelCn(a.getAlarmLevel()));
            m.put("levelTone", alarmLevelTone(a.getAlarmLevel()));
            m.put("status", alarmStatusCn(a.getAlarmStatus(), assignee));
            m.put("statusCode", a.getAlarmStatus());
            m.put("reporter", reporter != null ? reporter.getUsername() : "");
            m.put("reporterName", extra.getOrDefault("reporterName", reporter != null ? reporter.getRealName() : ""));
            m.put("assignee", assignee != null ? assignee.getUsername() : "");
            m.put("assigneeName", extra.getOrDefault("assigneeName", assignee != null ? assignee.getRealName() : ""));
            m.put("handlerName", extra.getOrDefault("handlerName", assignee != null ? assignee.getRealName() : ""));
            m.put("description", a.getAlarmDescription());
            m.put("handleResult", extra.getOrDefault("handleResult", ""));
            m.put("durationMinutes", alarmDurationMinutes(a));
            m.put("durationText", formatDuration(alarmDurationMinutes(a)));
            m.put("occurredAt", fmt(a.getReportedAt() != null ? a.getReportedAt() : a.getCreatedAt()));
            m.put("createdAt", fmt(a.getReportedAt() != null ? a.getReportedAt() : a.getCreatedAt()));
            m.put("updatedAt", fmt(a.getUpdatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapMaintenanceRecords(List<EquipmentMaintenanceRecord> records,
                                                            Map<Long, Equipment> equipmentById,
                                                            Map<Long, User> userById) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (EquipmentMaintenanceRecord r : records) {
            Equipment eq = equipmentById.get(r.getEquipmentId());
            User op = userById.get(r.getMaintainerId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getMaintenanceNo());
            m.put("equipmentId", eq != null ? eq.getEquipmentCode() : "");
            m.put("equipmentName", eq != null ? eq.getEquipmentName() : "");
            m.put("content", r.getMaintenanceContent());
            m.put("downtimeHours", r.getDowntimeMinutes() != null ? r.getDowntimeMinutes() / 60.0 : 0);
            m.put("operator", op != null ? op.getRealName() : "");
            m.put("createdAt", fmt(r.getCreatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapAftersaleCases(List<AfterSalesCase> cases,
                                                        Map<Long, CustomerOrder> orderById,
                                                        Map<Long, List<CustomerOrderItem>> itemsByOrderId,
                                                        Map<Long, User> userById) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AfterSalesCase c : cases) {
            CustomerOrder order = orderById.get(c.getOrderId());
            CustomerOrderItem item = order != null ? firstItem(itemsByOrderId.get(order.getOrderId())) : null;
            User handler = userById.get(c.getServiceUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getCaseNo());
            m.put("orderId", order != null ? order.getOrderNo() : "");
            m.put("batchNo", c.getBatchNo());
            m.put("productModel", item != null ? item.getProductName() : "");
            m.put("customerName", c.getCustomerName());
            m.put("feedback", c.getProblemDescription());
            m.put("status", MesStatusMapper.toAftersaleCn(c.getCaseStatus()));
            m.put("handler", handler != null ? handler.getUsername() : "");
            m.put("result", c.getHandleResult());
            m.put("createdAt", fmt(c.getCreatedAt()));
            m.put("updatedAt", fmt(c.getUpdatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapCostSettlements(List<CostSettlement> settlements,
                                                       Map<Long, WorkOrder> woById,
                                                       Map<Long, List<CustomerOrderItem>> itemsByOrderId,
                                                       Map<Long, CustomerOrder> orderById) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (CostSettlement cs : settlements) {
            WorkOrder wo = woById.get(cs.getWorkOrderId());
            CustomerOrder order = orderById.get(cs.getOrderId());
            CustomerOrderItem item = order != null ? firstItem(itemsByOrderId.get(order.getOrderId())) : null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", cs.getSettlementNo());
            m.put("workOrderId", wo != null ? wo.getWorkOrderNo() : "");
            m.put("productModel", item != null ? item.getProductName() : "");
            m.put("materialCost", dbl(cs.getMaterialCost()));
            m.put("laborCost", dbl(cs.getLaborCost()));
            m.put("equipmentCost", dbl(cs.getEquipmentCost()));
            m.put("qualityCost", dbl(cs.getQualityCost()));
            m.put("totalCost", dbl(cs.getTotalCost()));
            m.put("status", MesStatusMapper.toCostCn(cs.getSettlementStatus()));
            m.put("createdAt", fmt(cs.getCreatedAt()));
            m.put("updatedAt", fmt(cs.getUpdatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> mapDbLogs(List<OperationLog> dbLogs, Map<Long, User> userById) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OperationLog log : dbLogs) {
            User u = userById.get(log.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", log.getLogId());
            m.put("module", log.getModuleName());
            m.put("action", log.getOperationType());
            m.put("target", log.getOperationContent());
            m.put("operator", u != null ? u.getRealName() : "系统");
            m.put("roleKey", u != null && u.getRoleId() != null ? "system" : "system");
            m.put("createdAt", fmt(log.getOperatedAt()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> buildSuppliers(List<PurchaseOrder> purchaseOrders) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        int id = 1;
        for (PurchaseOrder po : purchaseOrders) {
            if (po.getSupplierName() == null || map.containsKey(po.getSupplierName())) {
                continue;
            }
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("id", id++);
            s.put("name", po.getSupplierName());
            s.put("contact", po.getSupplierContact());
            s.put("materials", "");
            s.put("phone", po.getSupplierPhone());
            map.put(po.getSupplierName(), s);
        }
        return new ArrayList<>(map.values());
    }

    private List<Map<String, Object>> buildPurchaseDemands(List<Inventory> inventories, Map<Long, Material> materialById) {
        List<Map<String, Object>> list = new ArrayList<>();
        int seq = 1;
        LocalDateTime now = LocalDateTime.now();
        for (Inventory inv : inventories) {
            Material mat = materialById.get(inv.getMaterialId());
            if (mat == null) {
                continue;
            }
            BigDecimal qty = inv.getQuantityOnHand() != null ? inv.getQuantityOnHand() : BigDecimal.ZERO;
            BigDecimal safe = mat.getSafetyStock() != null ? mat.getSafetyStock() : BigDecimal.valueOf(100);
            if (qty.compareTo(safe) >= 0) {
                continue;
            }
            int gap = safe.subtract(qty).intValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", "PD-" + String.format("%03d", seq++));
            m.put("materialCode", mat.getMaterialCode());
            m.put("materialName", mat.getMaterialName());
            m.put("requiredQty", safe.intValue());
            m.put("stockQty", qty.intValue());
            m.put("gapQty", gap);
            m.put("status", "待采购");
            m.put("sourceOrder", "");
            m.put("createdAt", fmt(now));
            list.add(m);
        }
        return list;
    }

    private Map<String, Object> buildProcessGuide(List<Material> materials, Map<Long, Material> materialById,
                                                  List<ProcessRoute> routes, List<ProcessStep> steps) {
        Map<Long, List<ProcessStep>> stepsByRoute = steps.stream()
                .filter(s -> s.getStatus() == null || s.getStatus() == 1)
                .collect(Collectors.groupingBy(ProcessStep::getRouteId));
        Map<String, Object> guide = new LinkedHashMap<>();
        for (Material mat : materials) {
            if (!"FINISHED".equals(mat.getMaterialType())) {
                continue;
            }
            ProcessRoute route = routes.stream()
                    .filter(r -> Objects.equals(r.getMaterialId(), mat.getMaterialId()))
                    .filter(r -> r.getStatus() == null || r.getStatus() == 1)
                    .findFirst()
                    .orElse(null);
            List<String> stepNames = route != null
                    ? stepsByRoute.getOrDefault(route.getRouteId(), List.of()).stream()
                    .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                    .map(ProcessStep::getStepName)
                    .filter(Objects::nonNull)
                    .toList()
                    : steps.stream()
                    .filter(s -> s.getStatus() == null || s.getStatus() == 1)
                    .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                    .map(ProcessStep::getStepName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("steps", stepNames);
            g.put("keyPoints", mat.getMaterialName() + "：" + (route != null ? route.getRouteName() + " " + route.getVersionNo()
                    : (mat.getSpecification() != null ? mat.getSpecification() : "按工艺路线执行")));
            guide.put(mat.getMaterialCode(), g);
            guide.put(mat.getMaterialName(), g);
        }
        return guide;
    }

    private String resolveWorkshopName(ProcessStep step, Equipment eq) {
        if (eq != null && eq.getWorkshop() != null && !eq.getWorkshop().isBlank()) {
            return eq.getWorkshop().trim();
        }
        ProductionWorkshopCatalog.WorkshopDef ws = ProductionWorkshopCatalog.workshopForStep(step);
        return ws != null ? ws.workshopName() : "";
    }

    private List<Map<String, Object>> buildProductModels(List<Material> materials) {
        return materials.stream()
                .filter(m -> "FINISHED".equals(m.getMaterialType()))
                .map(m -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("code", m.getMaterialCode());
                    row.put("name", m.getMaterialName());
                    row.put("specification", m.getSpecification());
                    row.put("panelType", m.getSpecification() != null && m.getSpecification().toUpperCase().contains("OLED")
                            ? "OLED" : "LCD");
                    return row;
                })
                .toList();
    }

    private List<String> buildProcessStepNames() {
        return processStepMapper.stepList().stream()
                .filter(s -> s.getStatus() == null || s.getStatus() == 1)
                .sorted(Comparator.comparing(ProcessStep::getStepNo, Comparator.nullsLast(Integer::compareTo)))
                .map(ProcessStep::getStepName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private CustomerOrderItem firstItem(List<CustomerOrderItem> items) {
        return items != null && !items.isEmpty() ? items.get(0) : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveOrderAttachments(String orderNo, Map<String, Object> extra) {
        Object raw = extra.get("attachments");
        if (raw instanceof List<?> list && !list.isEmpty()) {
            return (List<Map<String, Object>>) raw;
        }
        return orderOcrService.defaultAttachmentsForOrder(orderNo);
    }

    private int customerId(String customerName) {
        if (customerName == null) {
            return 0;
        }
        return Math.abs(customerName.hashCode() % 10000) + 1;
    }

    private String panelType(CustomerOrderItem item) {
        if (item == null || item.getSpecification() == null) {
            return "LCD";
        }
        String spec = item.getSpecification().toUpperCase();
        if (spec.contains("OLED")) {
            return "OLED";
        }
        return "LCD";
    }

    private String flowTypeCn(String type) {
        if (type == null) {
            return "库存变动";
        }
        return switch (type) {
            case "PURCHASE_IN" -> "采购入库";
            case "PRODUCTION_OUT" -> "生产领料";
            case "FINISHED_IN" -> "成品入库";
            case "DELIVERY_OUT" -> "发货出库";
            default -> type;
        };
    }

    private String direction(String type) {
        if (type == null) {
            return "入";
        }
        return type.endsWith("OUT") ? "出" : "入";
    }

    private String alarmLevelCn(String level) {
        if (level == null) return "一般";
        return switch (level.toUpperCase()) {
            case "HIGH", "URGENT", "IMPORTANT" -> "严重";
            case "LOW", "GENERAL" -> "一般";
            default -> "较重";
        };
    }

    private String alarmLevelTone(String level) {
        if (level == null) return "warning";
        return switch (level.toUpperCase()) {
            case "HIGH", "URGENT", "IMPORTANT" -> "danger";
            default -> "warning";
        };
    }

    private String alarmTypeCn(String type) {
        if (type == null || type.isBlank()) return "设备故障";
        if (type.contains("设备") || "EQUIPMENT".equalsIgnoreCase(type)) return "设备故障";
        if (type.contains("物料") || "MATERIAL".equalsIgnoreCase(type)) return "物料短缺";
        if (type.contains("质量") || "QUALITY".equalsIgnoreCase(type)) return "质量异常";
        if (type.contains("进度") || type.contains("延期") || "PROCESS".equalsIgnoreCase(type)) return "进度延期";
        if (type.contains("人员") || "SAFETY".equalsIgnoreCase(type)) return "人员异常";
        return type;
    }

    private String alarmStatusCn(String status, User assignee) {
        if (status == null) return "待确认";
        return switch (status) {
            case "REPORTED", "OPEN" -> "待确认";
            case "RECEIVED" -> assignee != null ? "已指派" : "已确认";
            case "PROCESSING" -> "处理中";
            case "CLOSED" -> "已关闭";
            default -> MesStatusMapper.toAlarmCn(status);
        };
    }

    private String buildAlarmSource(Equipment eq, Map<String, Object> extra) {
        Object ws = extra.get("workshop");
        if (ws != null && !String.valueOf(ws).isBlank()) {
            String eqName = eq != null ? eq.getEquipmentName() : String.valueOf(extra.getOrDefault("equipmentName", ""));
            return String.valueOf(ws) + (eqName.isBlank() ? "" : " / " + eqName);
        }
        if (eq != null) {
            return (eq.getWorkshop() != null ? eq.getWorkshop() + " / " : "") + eq.getEquipmentName();
        }
        return String.valueOf(extra.getOrDefault("source", "—"));
    }

    private long alarmDurationMinutes(AndonAlarm a) {
        LocalDateTime start = a.getReportedAt() != null ? a.getReportedAt() : a.getCreatedAt();
        if (start == null) return 0;
        LocalDateTime end = "CLOSED".equals(a.getAlarmStatus()) && a.getClosedAt() != null
                ? a.getClosedAt() : LocalDateTime.now();
        return java.time.Duration.between(start, end).toMinutes();
    }

    private String formatDuration(long minutes) {
        if (minutes < 60) return minutes + " 分钟";
        return (minutes / 60) + " 小时 " + (minutes % 60) + " 分";
    }

    private String resolveWorkOrderLine(List<DispatchTask> woDispatches, Map<Long, Equipment> equipmentById) {
        for (DispatchTask d : woDispatches) {
            if (d.getEquipmentId() != null) {
                Equipment eq = equipmentById.get(d.getEquipmentId());
                if (eq != null && eq.getWorkshop() != null && !eq.getWorkshop().isBlank()) {
                    return eq.getWorkshop();
                }
            }
        }
        return "—";
    }

    private int intVal(BigDecimal v) {
        return v != null ? v.intValue() : 0;
    }

    private double dbl(BigDecimal v) {
        return v != null ? v.doubleValue() : 0;
    }

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(DT_FMT) : LocalDateTime.now().format(DT_FMT);
    }

    private String resolveAssemblyGroup(Material mat) {
        if (mat == null) {
            return "其他";
        }
        if ("FINISHED".equals(mat.getMaterialType())) {
            return "成品";
        }
        String code = mat.getMaterialCode() != null ? mat.getMaterialCode() : "";
        if (code.startsWith("MAT-P")) {
            return "显示面板";
        }
        if (code.startsWith("MAT-B")) {
            return "背光模组";
        }
        if (code.startsWith("MAT-M")) {
            return "主控电路";
        }
        if (code.startsWith("MAT-S")) {
            return "结构附件";
        }
        return "其他";
    }

    private Map<String, Object> buildBomGuide(List<Material> materials, Map<Long, Material> materialById) {
        List<Map<String, Object>> groups = List.of(
                assemblyGroupDef("panel", "① 显示面板", "MAT-P"),
                assemblyGroupDef("backlight", "② 背光模组", "MAT-B"),
                assemblyGroupDef("mainboard", "③ 主控电路", "MAT-M"),
                assemblyGroupDef("structure", "④ 结构附件", "MAT-S")
        );

        Map<Long, List<Bom>> bomByParent = bomMapper.bomList().stream()
                .filter(b -> b.getStatus() == null || b.getStatus() == 1)
                .collect(Collectors.groupingBy(Bom::getParentMaterialId, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> groupOptions = new ArrayList<>();
        for (Material mat : materials) {
            if (!"RAW".equals(mat.getMaterialType())) {
                continue;
            }
            Map<String, Object> opt = new LinkedHashMap<>();
            opt.put("materialId", mat.getMaterialId());
            opt.put("materialCode", mat.getMaterialCode());
            opt.put("materialName", mat.getMaterialName());
            opt.put("specification", mat.getSpecification());
            opt.put("assemblyGroup", resolveAssemblyGroup(mat));
            opt.put("unit", mat.getUnit());
            groupOptions.add(opt);
        }

        List<Map<String, Object>> products = new ArrayList<>();
        for (Material mat : materials) {
            if (!"FINISHED".equals(mat.getMaterialType())) {
                continue;
            }
            List<Bom> lines = bomByParent.getOrDefault(mat.getMaterialId(), List.of());
            List<Map<String, Object>> components = new ArrayList<>();
            for (Bom bom : lines) {
                Material child = materialById.get(bom.getChildMaterialId());
                if (child == null) {
                    continue;
                }
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("materialCode", child.getMaterialCode());
                comp.put("materialName", child.getMaterialName());
                comp.put("assemblyGroup", bom.getRemark() != null && !bom.getRemark().isBlank()
                        ? bom.getRemark().replace("主控电路-驱动芯片", "主控电路")
                        : resolveAssemblyGroup(child));
                comp.put("quantity", bom.getQuantity() != null ? bom.getQuantity().doubleValue() : 1.0);
                comp.put("unit", child.getUnit());
                comp.put("specification", child.getSpecification());
                components.add(comp);
            }
            Map<String, Object> product = new LinkedHashMap<>();
            product.put("productCode", mat.getMaterialCode());
            product.put("productName", mat.getMaterialName());
            product.put("specification", mat.getSpecification());
            product.put("components", components);
            products.add(product);
        }

        Map<String, Object> guide = new LinkedHashMap<>();
        guide.put("groups", groups);
        guide.put("options", groupOptions);
        guide.put("products", products);
        guide.put("summary", "每台显示器由 4 类组装部件构成，每类有 2~3 种规格可选，不同型号选用不同组合");
        return guide;
    }

    private Map<String, Object> assemblyGroupDef(String key, String name, String codePrefix) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("key", key);
        g.put("name", name);
        g.put("codePrefix", codePrefix);
        return g;
    }
}
