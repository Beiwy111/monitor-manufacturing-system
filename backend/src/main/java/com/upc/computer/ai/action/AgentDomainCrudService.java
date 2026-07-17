package com.upc.computer.ai.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.entity.AndonAlarm;
import com.upc.computer.entity.Bom;
import com.upc.computer.entity.CostSettlement;
import com.upc.computer.entity.CustomerOrder;
import com.upc.computer.entity.CustomerOrderItem;
import com.upc.computer.entity.DeliveryOrder;
import com.upc.computer.entity.DispatchTask;
import com.upc.computer.entity.Equipment;
import com.upc.computer.entity.EquipmentMaintenanceRecord;
import com.upc.computer.entity.Inventory;
import com.upc.computer.entity.InventoryTransaction;
import com.upc.computer.entity.Material;
import com.upc.computer.entity.NonconformingProduct;
import com.upc.computer.entity.ProcessRoute;
import com.upc.computer.entity.ProcessStep;
import com.upc.computer.entity.ProductionPlan;
import com.upc.computer.entity.ProductionPlanItem;
import com.upc.computer.entity.PurchaseOrder;
import com.upc.computer.entity.PurchaseOrderItem;
import com.upc.computer.entity.QualityInspection;
import com.upc.computer.entity.Supplier;
import com.upc.computer.entity.WorkOrder;
import com.upc.computer.entity.WorkProgress;
import com.upc.computer.entity.WorkReport;
import com.upc.computer.service.AfterSalesService;
import com.upc.computer.service.CostService;
import com.upc.computer.service.EquipmentService;
import com.upc.computer.service.MaterialService;
import com.upc.computer.service.OrderService;
import com.upc.computer.service.ProductionService;
import com.upc.computer.service.PurchaseService;
import com.upc.computer.service.QualityService;
import com.upc.computer.service.SupplierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** 后台已有实体 CRUD 的受控应用服务。所有分支仍委托现有领域 Service。 */
@Service
public class AgentDomainCrudService {

    private final OrderService orderService;
    private final ProductionService productionService;
    private final MaterialService materialService;
    private final QualityService qualityService;
    private final PurchaseService purchaseService;
    private final EquipmentService equipmentService;
    private final AfterSalesService afterSalesService;
    private final CostService costService;
    private final SupplierService supplierService;
    private final ObjectMapper objectMapper;

    public AgentDomainCrudService(OrderService orderService, ProductionService productionService,
                                  MaterialService materialService, QualityService qualityService,
                                  PurchaseService purchaseService, EquipmentService equipmentService,
                                  AfterSalesService afterSalesService, CostService costService,
                                  SupplierService supplierService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.productionService = productionService;
        this.materialService = materialService;
        this.qualityService = qualityService;
        this.purchaseService = purchaseService;
        this.equipmentService = equipmentService;
        this.afterSalesService = afterSalesService;
        this.costService = costService;
        this.supplierService = supplierService;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Object execute(String code, Map<String, Object> p) {
        return switch (code) {
            case "crud.order.customer.create" -> create(p, CustomerOrder.class, orderService::insertCustomerOrder);
            case "crud.order.customer.update" -> update(p, "orderId", orderService::getCustomerOrderById, orderService::updateCustomerOrder);
            case "crud.order.customer.delete" -> delete(p, "orderId", orderService::deleteCustomerOrder);
            case "crud.order.item.create" -> create(p, CustomerOrderItem.class, orderService::insertOrderItem);
            case "crud.order.item.update" -> update(p, "orderItemId", orderService::getOrderItemById, orderService::updateOrderItem);
            case "crud.order.item.delete" -> delete(p, "orderItemId", orderService::deleteOrderItem);
            case "crud.order.delivery.create" -> create(p, DeliveryOrder.class, orderService::insertDelivery);
            case "crud.order.delivery.update" -> update(p, "deliveryId", orderService::getDeliveryById, orderService::updateDelivery);
            case "crud.order.delivery.delete" -> delete(p, "deliveryId", orderService::deleteDelivery);

            case "crud.production.plan.create" -> create(p, ProductionPlan.class, productionService::insertPlan);
            case "crud.production.plan.update" -> update(p, "planId", productionService::getPlanById, productionService::updatePlan);
            case "crud.production.plan.delete" -> delete(p, "planId", productionService::deletePlan);
            case "crud.production.planItem.create" -> create(p, ProductionPlanItem.class, productionService::insertPlanItem);
            case "crud.production.planItem.update" -> update(p, "planItemId", productionService::getPlanItemById, productionService::updatePlanItem);
            case "crud.production.planItem.delete" -> delete(p, "planItemId", productionService::deletePlanItem);
            case "crud.production.route.create" -> create(p, ProcessRoute.class, productionService::insertRoute);
            case "crud.production.route.update" -> update(p, "routeId", productionService::getRouteById, productionService::updateRoute);
            case "crud.production.route.delete" -> delete(p, "routeId", productionService::deleteRoute);
            case "crud.production.step.create" -> create(p, ProcessStep.class, productionService::insertStep);
            case "crud.production.step.update" -> update(p, "stepId", productionService::getStepById, productionService::updateStep);
            case "crud.production.step.delete" -> delete(p, "stepId", productionService::deleteStep);
            case "crud.production.workOrder.create" -> create(p, WorkOrder.class, productionService::insertWorkOrder);
            case "crud.production.workOrder.update" -> update(p, "workOrderId", productionService::getWorkOrderById, productionService::updateWorkOrder);
            case "crud.production.workOrder.delete" -> delete(p, "workOrderId", productionService::deleteWorkOrder);
            case "crud.production.dispatch.create" -> create(p, DispatchTask.class, productionService::insertDispatch);
            case "crud.production.dispatch.update" -> update(p, "dispatchId", productionService::getDispatchById, productionService::updateDispatch);
            case "crud.production.dispatch.delete" -> delete(p, "dispatchId", productionService::deleteDispatch);
            case "crud.production.report.create" -> create(p, WorkReport.class, productionService::insertReport);
            case "crud.production.report.update" -> update(p, "reportId", productionService::getReportById, productionService::updateReport);
            case "crud.production.report.delete" -> delete(p, "reportId", productionService::deleteReport);
            case "crud.production.progress.create" -> create(p, WorkProgress.class, productionService::insertProgress);
            case "crud.production.progress.update" -> update(p, "progressId", productionService::getProgressById, productionService::updateProgress);
            case "crud.production.progress.delete" -> delete(p, "progressId", productionService::deleteProgress);

            case "crud.material.material.create" -> create(p, Material.class, materialService::insertMaterial);
            case "crud.material.material.update" -> update(p, "materialId", materialService::getMaterialById, materialService::updateMaterial);
            case "crud.material.material.delete" -> delete(p, "materialId", materialService::deleteMaterial);
            case "crud.material.bom.create" -> create(p, Bom.class, materialService::insertBom);
            case "crud.material.bom.update" -> update(p, "bomId", materialService::getBomById, materialService::updateBom);
            case "crud.material.bom.delete" -> delete(p, "bomId", materialService::deleteBom);
            case "crud.material.inventory.create" -> create(p, Inventory.class, materialService::insertInventory);
            case "crud.material.inventory.update" -> update(p, "inventoryId", materialService::getInventoryById, materialService::updateInventory);
            case "crud.material.inventory.delete" -> delete(p, "inventoryId", materialService::deleteInventory);
            case "crud.material.transaction.create" -> create(p, InventoryTransaction.class, materialService::insertTransaction);
            case "crud.material.transaction.update" -> update(p, "transactionId", materialService::getTransactionById, materialService::updateTransaction);
            case "crud.material.transaction.delete" -> delete(p, "transactionId", materialService::deleteTransaction);

            case "crud.quality.inspection.create" -> create(p, QualityInspection.class, qualityService::insertInspection);
            case "crud.quality.inspection.update" -> update(p, "inspectionId", qualityService::getInspectionById, qualityService::updateInspection);
            case "crud.quality.inspection.delete" -> delete(p, "inspectionId", qualityService::deleteInspection);
            case "crud.quality.nonconforming.create" -> create(p, NonconformingProduct.class, qualityService::insertNonconforming);
            case "crud.quality.nonconforming.update" -> update(p, "nonconformingId", qualityService::getNonconformingById, qualityService::updateNonconforming);
            case "crud.quality.nonconforming.delete" -> delete(p, "nonconformingId", qualityService::deleteNonconforming);

            case "crud.purchase.order.create" -> create(p, PurchaseOrder.class, purchaseService::insertPurchaseOrder);
            case "crud.purchase.order.update" -> update(p, "purchaseOrderId", purchaseService::getPurchaseOrderById, purchaseService::updatePurchaseOrder);
            case "crud.purchase.order.delete" -> delete(p, "purchaseOrderId", purchaseService::deletePurchaseOrder);
            case "crud.purchase.item.create" -> create(p, PurchaseOrderItem.class, purchaseService::insertPurchaseOrderItem);
            case "crud.purchase.item.update" -> update(p, "purchaseOrderItemId", purchaseService::getPurchaseOrderItemById, purchaseService::updatePurchaseOrderItem);
            case "crud.purchase.item.delete" -> delete(p, "purchaseOrderItemId", purchaseService::deletePurchaseOrderItem);

            case "crud.equipment.asset.create" -> create(p, Equipment.class, equipmentService::insertEquipment);
            case "crud.equipment.asset.update" -> update(p, "equipmentId", equipmentService::getEquipmentById, equipmentService::updateEquipment);
            case "crud.equipment.asset.delete" -> delete(p, "equipmentId", equipmentService::deleteEquipment);
            case "crud.equipment.alarm.create" -> create(p, AndonAlarm.class, equipmentService::insertAlarm);
            case "crud.equipment.alarm.update" -> update(p, "alarmId", equipmentService::getAlarmById, equipmentService::updateAlarm);
            case "crud.equipment.alarm.delete" -> delete(p, "alarmId", equipmentService::deleteAlarm);
            case "crud.equipment.maintenance.create" -> create(p, EquipmentMaintenanceRecord.class, equipmentService::insertMaintenance);
            case "crud.equipment.maintenance.update" -> update(p, "maintenanceId", equipmentService::getMaintenanceById, equipmentService::updateMaintenance);
            case "crud.equipment.maintenance.delete" -> delete(p, "maintenanceId", equipmentService::deleteMaintenance);

            case "crud.aftersale.case.create" -> create(p, AfterSalesCase.class, afterSalesService::insertAfterSalesCase);
            case "crud.aftersale.case.update" -> updateText(p, "caseNo", afterSalesService::getAfterSalesCaseById, afterSalesService::updateAfterSalesCase);
            case "crud.aftersale.case.delete" -> deleteText(p, "caseNo", afterSalesService::deleteAfterSalesCase);
            case "crud.cost.settlement.create" -> create(p, CostSettlement.class, costService::insertSettlement);
            case "crud.cost.settlement.update" -> update(p, "settlementId", costService::getSettlementById, costService::updateSettlement);
            case "crud.cost.settlement.delete" -> delete(p, "settlementId", costService::deleteSettlement);

            case "crud.purchase.supplier.create" -> supplierService.create(convert(p, Supplier.class));
            case "crud.purchase.supplier.update" -> supplierService.update(mergeExisting(
                    requireExisting(supplierService.getById(longVal(p, "supplierId"))), p));
            case "crud.purchase.supplier.delete" -> delete(p, "supplierId", supplierService::delete);
            default -> throw new BusinessException(400, "CRUD 动作尚未配置：" + code);
        };
    }

    private <T> T create(Map<String, Object> p, Class<T> type, Writer<T> writer) {
        T entity = convert(p, type);
        writer.write(entity);
        return entity;
    }

    private <T> T update(Map<String, Object> p, String idKey, Reader<Long, T> reader, Writer<T> writer) {
        T entity = mergeExisting(requireExisting(reader.read(longVal(p, idKey))), p);
        writer.write(entity);
        return entity;
    }

    private <T> T updateText(Map<String, Object> p, String idKey, Reader<String, T> reader, Writer<T> writer) {
        T entity = mergeExisting(requireExisting(reader.read(text(p, idKey))), p);
        writer.write(entity);
        return entity;
    }

    private Object delete(Map<String, Object> p, String idKey, Writer<Long> writer) {
        writer.write(longVal(p, idKey));
        return Map.of("success", true);
    }

    private Object deleteText(Map<String, Object> p, String idKey, Writer<String> writer) {
        writer.write(text(p, idKey));
        return Map.of("success", true);
    }

    private <T> T convert(Map<String, Object> p, Class<T> type) {
        try {
            return objectMapper.convertValue(p, type);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "CRUD 参数格式错误：" + e.getMessage());
        }
    }

    private <T> T mergeExisting(T entity, Map<String, Object> p) {
        try {
            objectMapper.updateValue(entity, p);
            return entity;
        } catch (Exception e) {
            throw new BusinessException(400, "CRUD 修改参数格式错误：" + e.getMessage());
        }
    }

    private <T> T requireExisting(T entity) {
        if (entity == null) throw new BusinessException(404, "要修改的业务数据不存在");
        return entity;
    }

    private Long longVal(Map<String, Object> p, String key) {
        Object value = p.get(key);
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            throw new BusinessException(400, key + " 必须是整数");
        }
    }

    private String text(Map<String, Object> p, String key) {
        Object value = p.get(key);
        if (value == null || String.valueOf(value).isBlank()) throw new BusinessException(400, key + " 不能为空");
        return String.valueOf(value).trim();
    }

    @FunctionalInterface
    private interface Writer<T> { void write(T value); }

    @FunctionalInterface
    private interface Reader<I, T> { T read(I id); }
}
