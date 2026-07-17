package com.upc.computer.ai.action;

import com.upc.computer.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Agent 可规划的写动作白名单。目录之外的动作永远不能执行。 */
@Component
public class AgentActionCatalog {

    private static final Set<String> ALL_ROLES = Set.of(
            "ORDER", "PLANNER", "MANAGER", "OPERATOR", "QC", "PURCHASER",
            "WAREHOUSE", "DEVICE", "SERVICE", "COST", "CUSTOMER");

    private final Map<String, AgentActionDefinition> definitions = new LinkedHashMap<>();

    public AgentActionCatalog() {
        // 订单人员
        add("createOrder", "新建客户订单", "订单", roles("ORDER"),
                "customerName:客户名称", "productModel:产品型号", "quantity:订购数量", "deliveryDate:要求交期(yyyy-MM-dd)");
        add("auditOrder", "审核客户订单", "订单", roles("ORDER"), "orderId:订单编号", "action:审核动作(pass/reject/supplement/defer)");
        add("submitOrder", "提交订单审核", "订单", roles("ORDER"), "orderId:订单编号");
        add("submitOrderToPlanner", "提交订单给计划员", "订单", roles("ORDER"), "orderId:订单编号");
        add("deleteOrder", "删除客户订单", "订单", roles("ORDER"), "orderId:订单编号");

        // 计划员，智能排产复用现有计划员服务
        add("createPlan", "新建生产计划", "生产计划", roles("PLANNER"), "orderId:订单编号", "planStart:计划开始日期", "planEnd:计划结束日期");
        add("updatePlan", "修改生产计划", "生产计划", roles("PLANNER"), "planId:计划编号");
        add("agentCreatePlan", "智能排产并创建计划", "智能排产", roles("PLANNER"), "orderId:订单编号", "planStart:计划开始日期", "planEnd:计划结束日期");
        add("generateSmartPlans", "批量智能生成生产计划", "智能排产", roles("PLANNER"));
        add("saveProductionPlan", "保存手工排产方案", "智能排产", roles("PLANNER"), "orderId:订单编号", "schedule:排产明细");
        add("saveBatchProductionPlans", "批量保存排产方案", "智能排产", roles("PLANNER"), "plans:排产方案列表");
        add("copyProductionPlan", "复制生产计划", "生产计划", roles("PLANNER"), "planId:计划编号");
        add("publishPlan", "发布生产计划", "生产计划", roles("PLANNER"), "planId:计划编号");
        add("submitPlanToManager", "提交计划给生产主管", "生产计划", roles("PLANNER"), "planId:计划编号");
        add("deletePlan", "删除生产计划", "生产计划", roles("PLANNER"), "planId:计划编号");
        add("process.route.save", "新增或修改工艺路线", "工艺设置", roles("PLANNER"), "materialId:成品物料ID", "routeCode:路线编码", "routeName:路线名称");
        add("process.route.disable", "停用工艺路线", "工艺设置", roles("PLANNER"), "routeId:路线ID");
        add("process.step.save", "新增或修改工序", "工艺设置", roles("PLANNER"), "routeId:路线ID", "stepNo:工序序号", "stepCode:工序编码", "stepName:工序名称");
        add("process.step.disable", "停用工序", "工艺设置", roles("PLANNER"), "stepId:工序ID");
        add("process.step.reorder", "调整工序顺序", "工艺设置", roles("PLANNER"), "routeId:路线ID", "stepIds:按新顺序排列的工序ID列表");

        // 生产主管，智能派工复用现有主管服务
        add("confirmSmartDispatch", "确认智能派工", "智能派工", roles("MANAGER"), "planId:计划编号");
        add("agentBatchDispatch", "按建议批量派工", "智能派工", roles("MANAGER"), "planId:计划编号", "suggestions:派工建议列表");
        add("createWorkOrder", "创建生产工单", "生产管理", roles("MANAGER"), "planId:计划编号");
        add("releaseWorkOrder", "下达生产工单", "生产管理", roles("MANAGER"), "woId:工单编号");
        add("createDispatch", "创建派工记录", "生产管理", roles("MANAGER"), "workOrderId:工单编号", "processStep:工序名称", "operator:操作员用户名", "planQty:派工数量");
        add("confirmReport", "确认或驳回报工", "生产管理", roles("MANAGER"), "reportId:报工编号", "pass:是否通过(true/false)");
        add("deleteWorkOrder", "删除生产工单", "生产管理", roles("MANAGER"), "workOrderId:工单编号");
        add("deleteDispatch", "删除派工记录", "生产管理", roles("MANAGER"), "dispatchId:派工编号");

        // 生产操作员
        add("acceptDispatch", "接收派工", "现场作业", roles("OPERATOR"), "dispatchId:派工编号");
        add("startDispatch", "开始生产", "现场作业", roles("OPERATOR"), "dispatchId:派工编号");
        add("pickMaterial", "领取单项物料", "现场作业", roles("OPERATOR"), "dispatchId:派工编号", "taskId:领料任务ID", "qty:领取数量");
        add("pickAllMaterials", "领取全部物料", "现场作业", roles("OPERATOR"), "dispatchId:派工编号");
        add("submitReport", "提交工序报工", "现场作业", roles("OPERATOR"), "dispatchId:派工编号", "reportQty:报工数量");
        add("submitToInspection", "提交质检", "现场作业", roles("OPERATOR"), "dispatchId:派工编号");
        add("deleteReport", "删除报工记录", "现场作业", roles("OPERATOR", "MANAGER"), "reportId:报工编号");

        // 质检员
        add("submitInspection", "提交质检结果", "质量管理", roles("QC"), "qcId:质检编号", "result:质检结果(合格/不合格/让步接收)",
                "sampleQty:抽检数量", "qualifiedQty:合格数量", "unqualifiedQty:不合格数量");
        add("generateQualityReport", "生成质量报告", "质量管理", roles("QC"), "qcId:质检编号");
        add("scrapDefect", "报废不合格品", "质量管理", roles("QC"), "defectId:不合格品编号");
        add("reworkDefect", "派发返修", "质量管理", roles("QC"), "defectId:不合格品编号");
        add("deleteInspection", "删除质检记录", "质量管理", roles("QC"), "qcId:质检编号");
        add("deleteDefect", "删除不合格品记录", "质量管理", roles("QC"), "defectId:不合格品编号");
        add("quality.items.generate", "生成默认检测项", "质量管理", roles("QC"), "inspectionId:质检ID");
        add("quality.items.save", "保存检测项", "质量管理", roles("QC"), "inspectionId:质检ID", "items:检测项列表");
        add("quality.evaluate", "计算质检判定", "质量管理", roles("QC"), "inspectionId:质检ID");
        add("quality.pass", "质检通过", "质量管理", roles("QC"), "inspectionId:质检ID");
        add("quality.fail", "质检不通过", "质量管理", roles("QC"), "inspectionId:质检ID", "defectType:缺陷类型", "defectReason:缺陷原因");
        add("quality.recheck", "要求复检", "质量管理", roles("QC"), "inspectionId:质检ID", "reason:复检原因");
        add("quality.recheckPass", "复检通过", "质量管理", roles("QC"), "inspectionId:质检ID");
        add("quality.recheckFail", "复检不通过", "质量管理", roles("QC"), "inspectionId:质检ID", "defectType:缺陷类型", "defectReason:缺陷原因");
        add("quality.close", "关闭质检单", "质量管理", roles("QC"), "inspectionId:质检ID");
        add("quality.nonconforming.handle", "处置不合格品", "质量管理", roles("QC"), "nonconformingId:不合格品ID", "handleMethod:处置方式");
        add("quality.incoming.create", "登记来料检", "质量管理", roles("QC"), "materialId:物料ID", "batchNo:批次号", "lotQuantity:批次数量", "sampleQuantity:抽样数量");
        add("quality.sampling.update", "更新抽检数量", "质量管理", roles("QC"), "inspectionId:质检ID", "sampleQuantity:抽样数量", "qualifiedQuantity:合格数量", "unqualifiedQuantity:不合格数量");

        // 采购员
        add("createPurchaseOrder", "新建采购订单", "采购管理", roles("PURCHASER"), "materialCode:物料编码", "quantity:采购数量", "unitPrice:采购单价", "supplier:供应商", "expectedDate:预计到货日期");
        add("receivePurchase", "确认采购到货", "采购管理", roles("PURCHASER"), "poId:采购单编号", "qty:本次到货数量");
        add("deletePurchaseOrder", "删除采购订单", "采购管理", roles("PURCHASER"), "purchaseOrderId:采购单编号");
        add("purchase.requirements.calculate", "重新计算采购需求", "采购管理", roles("PURCHASER"));
        add("purchase.orders.generate", "从需求生成采购单", "采购管理", roles("PURCHASER"), "requirementIds:采购需求ID列表");
        add("purchase.requirement.select", "选中采购需求", "采购管理", roles("PURCHASER"), "requirementId:采购需求ID");
        add("purchase.requirement.cancel", "取消采购需求", "采购管理", roles("PURCHASER"), "requirementId:采购需求ID");
        add("purchase.arrival.confirm", "确认采购单到货", "采购管理", roles("PURCHASER"), "purchaseOrderId:采购单ID");
        add("purchase.arrival.confirmSlots", "确认采购到货并分配库位", "采购入库", roles("WAREHOUSE"), "purchaseOrderId:采购单ID", "assignments:库位分配列表");
        add("purchase.order.revoke", "撤销采购单", "采购管理", roles("PURCHASER"), "purchaseOrderId:采购单ID");
        add("purchase.order.saveDraft", "修改采购单草稿", "采购管理", roles("PURCHASER"), "purchaseOrderId:采购单ID", "items:采购明细列表");

        // 仓储
        add("confirmInbound", "确认成品入库", "仓储管理", roles("WAREHOUSE"), "taskId:入库任务ID", "slotCode:库位编码");
        add("issueMaterial", "确认生产领料", "仓储管理", roles("WAREHOUSE"), "taskId:领料任务ID", "qty:领料数量");
        add("shipDelivery", "确认发货出库", "仓储管理", roles("WAREHOUSE"), "dlvId:发货单编号");
        add("deleteDelivery", "删除发货单", "仓储管理", roles("WAREHOUSE"), "dlvId:发货单编号");
        add("deleteInboundTask", "删除入库任务", "仓储管理", roles("WAREHOUSE"), "taskId:入库任务ID");
        add("deleteIssueTask", "删除领料任务", "仓储管理", roles("WAREHOUSE"), "taskId:领料任务ID");
        add("warehouse.barcode.rule.save", "保存条码规则", "仓储条码", roles("WAREHOUSE"), "ruleCode:规则编码", "businessType:业务类型", "prefix:条码前缀");
        add("warehouse.barcode.generate", "生成条码号", "仓储条码", roles("WAREHOUSE"), "businessType:业务类型");
        add("warehouse.barcode.scan", "执行条码扫描", "仓储条码", roles("WAREHOUSE"), "barcodeNo:条码号", "scanType:扫描类型");

        // 设备人员
        add("createAlarm", "触发安灯报警", "设备管理", roles("DEVICE"), "equipmentId:设备ID", "description:报警描述");
        add("handleAlarm", "处理安灯报警", "设备管理", roles("DEVICE"), "alarmId:报警编号", "action:处理动作(receive/assign/processing/close)");
        add("updateEquipment", "更新设备状态", "设备管理", roles("DEVICE"), "eqId:设备编号");
        add("deleteAlarm", "删除报警记录", "设备管理", roles("DEVICE"), "alarmId:报警编号");
        add("equipment.alarm.trigger", "触发设备报警", "设备管理", roles("DEVICE"), "equipmentId:设备ID", "description:报警描述");
        add("equipment.alarm.receive", "接收设备报警", "设备管理", roles("DEVICE"), "alarmId:报警ID");
        add("equipment.alarm.resolve", "解除设备报警", "设备管理", roles("DEVICE"), "alarmId:报警ID", "remark:处理结果");
        add("equipment.maintenance.start", "开始设备维保", "设备管理", roles("DEVICE"), "equipmentId:设备ID", "faultDescription:故障描述");
        add("equipment.maintenance.finish", "完成设备维保", "设备管理", roles("DEVICE"), "maintenanceId:维保记录ID", "result:维保结果");

        // 售后
        add("createAftersale", "登记售后案例", "售后管理", roles("SERVICE"), "orderId:订单编号", "feedback:问题描述");
        add("processAftersale", "处理售后案例", "售后管理", roles("SERVICE"), "caseId:案例编号");
        add("deleteAftersale", "删除售后案例", "售后管理", roles("SERVICE"), "caseId:案例编号");
        add("aftersale.case.accept", "受理售后案例", "售后管理", roles("SERVICE"), "caseNo:案例编号");
        add("aftersale.case.resolve", "解决售后案例", "售后管理", roles("SERVICE"), "caseNo:案例编号", "solution:解决方案");
        add("aftersale.case.close", "关闭售后案例", "售后管理", roles("SERVICE"), "caseNo:案例编号");
        add("aftersale.rca.dispatch", "派发根因分析任务", "售后管理", roles("SERVICE"), "caseNo:案例编号", "departments:协同部门列表");
        add("aftersale.rca.confirm", "确认最终根因", "售后管理", roles("SERVICE"), "caseNo:案例编号", "rootCause:最终根因");
        add("aftersale.rca.task.update", "更新根因分析任务", "售后管理", roles("SERVICE"), "taskId:任务ID", "status:任务状态");
        add("aftersale.plan.save", "保存售后方案", "售后管理", roles("SERVICE"), "caseNo:案例编号", "planContent:方案内容");
        add("aftersale.plan.submit", "提交售后方案", "售后管理", roles("SERVICE"), "planId:方案ID");
        add("aftersale.plan.approve", "审批通过售后方案", "售后管理", roles("SERVICE"), "planId:方案ID");
        add("aftersale.plan.reject", "驳回售后方案", "售后管理", roles("SERVICE"), "planId:方案ID", "remark:驳回原因");
        add("aftersale.task.update", "更新售后执行任务", "售后管理", roles("SERVICE"), "taskId:任务ID", "status:任务状态");
        add("aftersale.case.advance", "推进售后案例状态", "售后管理", roles("SERVICE"), "caseNo:案例编号", "targetStatus:目标状态");
        add("aftersale.closure.save", "保存售后闭环信息", "售后管理", roles("SERVICE"), "caseNo:案例编号");
        add("aftersale.closure.confirmCustomer", "记录客户确认", "售后管理", roles("SERVICE"), "caseNo:案例编号");
        add("aftersale.closure.close", "完成售后闭环", "售后管理", roles("SERVICE"), "caseNo:案例编号");

        // 成本
        add("confirmCostSettlement", "确认成本结算", "成本管理", roles("COST"), "csId:结算编号");
        add("exportCostSettlement", "导出并锁定成本结算", "成本管理", roles("COST"), "csId:结算编号");
        add("deleteCostSettlement", "删除成本结算", "成本管理", roles("COST"), "settlementId:结算编号");
        add("cost.settlement.save", "新增或修改成本结算", "成本管理", roles("COST"), "sourceType:来源类型", "sourceId:来源ID");
        add("cost.settlement.confirm", "确认成本结算记录", "成本管理", roles("COST"), "settlementId:结算ID");
        add("cost.settlement.export", "导出成本结算记录", "成本管理", roles("COST"), "settlementId:结算ID");

        // 管理员系统 CRUD 与排班
        add("admin.user.create", "新增系统用户", "系统管理", roles("ADMIN"), "username:用户名", "passwordHash:初始密码", "realName:姓名", "roleId:角色ID");
        add("admin.user.update", "修改系统用户", "系统管理", roles("ADMIN"), "userId:用户ID");
        add("admin.user.delete", "删除系统用户", "系统管理", roles("ADMIN"), "userId:用户ID");
        add("admin.role.create", "新增系统角色", "系统管理", roles("ADMIN"), "roleCode:角色编码", "roleName:角色名称");
        add("admin.role.update", "修改系统角色", "系统管理", roles("ADMIN"), "roleId:角色ID");
        add("admin.role.delete", "删除系统角色", "系统管理", roles("ADMIN"), "roleId:角色ID");
        add("admin.menu.create", "新增系统菜单", "系统管理", roles("ADMIN"), "menuCode:菜单编码", "menuName:菜单名称");
        add("admin.menu.update", "修改系统菜单", "系统管理", roles("ADMIN"), "menuId:菜单ID");
        add("admin.menu.delete", "删除系统菜单", "系统管理", roles("ADMIN"), "menuId:菜单ID");
        add("admin.permission.create", "新增系统权限", "系统管理", roles("ADMIN"), "roleId:角色ID", "permissionCode:权限编码", "permissionName:权限名称");
        add("admin.permission.update", "修改系统权限", "系统管理", roles("ADMIN"), "permissionId:权限ID");
        add("admin.permission.delete", "删除系统权限", "系统管理", roles("ADMIN"), "permissionId:权限ID");
        add("admin.operationLog.delete", "删除操作日志", "系统管理", roles("ADMIN"), "logId:日志ID");
        add("attendance.schedule.save", "新增或修改排班", "考勤管理", roles("ADMIN"), "scheduleDate:排班日期", "userId:用户ID", "shiftType:班次");
        add("attendance.schedule.delete", "删除排班", "考勤管理", roles("ADMIN"), "scheduleId:排班ID");

        // 后台原始 CRUD：按各角色已有模块边界开放，管理员自动拥有全部非客户动作
        crud("crud.order.customer", "客户订单", "订单", roles("ORDER"), "orderId", "orderNo:订单编号", "customerName:客户名称");
        crud("crud.order.item", "订单明细", "订单", roles("ORDER"), "orderItemId", "orderId:订单ID", "productName:产品名称", "quantity:数量");
        crud("crud.order.delivery", "发货单", "订单", roles("ORDER", "WAREHOUSE"), "deliveryId", "deliveryNo:发货单编号", "orderId:订单ID");
        crud("crud.production.plan", "生产计划", "生产计划", roles("PLANNER"), "planId", "planNo:计划编号", "sourceOrderId:来源订单ID");
        crud("crud.production.planItem", "计划明细", "生产计划", roles("PLANNER"), "planItemId", "planId:计划ID", "materialId:物料ID", "plannedQuantity:计划数量");
        crud("crud.production.route", "工艺路线", "工艺设置", roles("PLANNER"), "routeId", "materialId:物料ID", "routeCode:路线编码", "routeName:路线名称");
        crud("crud.production.step", "生产工序", "工艺设置", roles("PLANNER"), "stepId", "routeId:路线ID", "stepCode:工序编码", "stepName:工序名称");
        crud("crud.production.workOrder", "生产工单", "生产管理", roles("MANAGER"), "workOrderId", "workOrderNo:工单编号", "planId:计划ID");
        crud("crud.production.dispatch", "派工记录", "生产管理", roles("MANAGER"), "dispatchId", "dispatchNo:派工编号", "workOrderId:工单ID");
        crud("crud.production.report", "报工记录", "现场作业", roles("OPERATOR", "MANAGER"), "reportId", "reportNo:报工编号", "dispatchId:派工ID");
        crud("crud.production.progress", "生产进度", "现场作业", roles("OPERATOR", "MANAGER"), "progressId", "workOrderId:工单ID");
        crud("crud.material.material", "物料档案", "物料管理", roles("WAREHOUSE", "PURCHASER"), "materialId", "materialCode:物料编码", "materialName:物料名称", "materialType:物料类型");
        crud("crud.material.bom", "物料清单", "物料管理", roles("PLANNER", "WAREHOUSE"), "bomId", "parentMaterialId:父物料ID", "childMaterialId:子物料ID", "quantity:用量");
        crud("crud.material.inventory", "库存记录", "仓储管理", roles("WAREHOUSE"), "inventoryId", "materialId:物料ID", "warehouseCode:仓库编码");
        crud("crud.material.transaction", "库存流水", "仓储管理", roles("WAREHOUSE"), "transactionId", "inventoryId:库存ID", "transactionType:流水类型", "quantity:数量");
        crud("crud.quality.inspection", "质检记录", "质量管理", roles("QC"), "inspectionId", "inspectionNo:质检编号", "inspectionType:质检类型");
        crud("crud.quality.nonconforming", "不合格品", "质量管理", roles("QC"), "nonconformingId", "nonconformingNo:不合格品编号", "inspectionId:质检ID");
        crud("crud.purchase.order", "采购订单", "采购管理", roles("PURCHASER"), "purchaseOrderId", "purchaseOrderNo:采购单编号", "supplierName:供应商");
        crud("crud.purchase.item", "采购明细", "采购管理", roles("PURCHASER"), "purchaseOrderItemId", "purchaseOrderId:采购单ID", "materialId:物料ID", "quantity:采购数量");
        crud("crud.equipment.asset", "设备台账", "设备管理", roles("DEVICE"), "equipmentId", "equipmentCode:设备编码", "equipmentName:设备名称");
        crud("crud.equipment.alarm", "报警记录", "设备管理", roles("DEVICE"), "alarmId", "alarmNo:报警编号", "equipmentId:设备ID");
        crud("crud.equipment.maintenance", "维保记录", "设备管理", roles("DEVICE"), "maintenanceId", "maintenanceNo:维保编号", "equipmentId:设备ID");
        crud("crud.aftersale.case", "售后案例", "售后管理", roles("SERVICE"), "caseNo", "caseNo:案例编号", "problemDescription:问题描述");
        crud("crud.cost.settlement", "成本结算", "成本管理", roles("COST"), "settlementId", "settlementNo:结算编号", "sourceType:来源类型", "sourceId:来源ID");
        crud("crud.purchase.supplier", "供应商", "采购管理", roles("PURCHASER"), "supplierId", "supplierName:供应商名称");

        // 客户自助，仅限当前客户会话
        add("customer.order.create", "客户提交新订单", "客户门户", roles("CUSTOMER"),
                "materialId:产品物料ID", "quantity:订购数量", "requiredDeliveryDate:要求交期", "receiverName:收货人", "receiverPhone:联系电话", "receiverAddress:收货地址");
        add("customer.feedback.create", "客户提交售后反馈", "客户门户", roles("CUSTOMER"), "problemType:问题类型", "problemDescription:问题描述");
        add("customer.profile.update", "客户修改个人资料", "客户门户", roles("CUSTOMER"));

        add("attendance.checkIn", "本人签到", "考勤", ALL_ROLES);
        add("attendance.checkOut", "本人签退", "考勤", ALL_ROLES);
    }

    public AgentActionDefinition requireAllowed(String actionCode, String roleCode) {
        String code = actionCode == null ? "" : actionCode.trim();
        AgentActionDefinition definition = definitions.get(code);
        if (definition == null) {
            throw new BusinessException(400, "不支持的 Agent 动作：" + code);
        }
        String role = normalizeRole(roleCode);
        boolean adminAllowed = "ADMIN".equals(role) && !definition.roles().equals(Set.of("CUSTOMER"));
        if (!adminAllowed && !definition.roles().contains(role)) {
            throw new BusinessException(403, "当前角色无权执行：" + definition.title());
        }
        return definition;
    }

    public List<Map<String, Object>> allowedActions(String roleCode) {
        String role = normalizeRole(roleCode);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentActionDefinition definition : definitions.values()) {
            boolean allowed = definition.roles().contains(role)
                    || ("ADMIN".equals(role) && !definition.roles().equals(Set.of("CUSTOMER")));
            if (!allowed) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("actionCode", definition.code());
            item.put("title", definition.title());
            item.put("module", definition.module());
            item.put("requiredFields", definition.requiredFields());
            result.add(item);
        }
        return result;
    }

    public boolean isMesWorkflowAction(String code) {
        return code != null && !code.contains(".");
    }

    private void add(String code, String title, String module, Set<String> roles, String... fields) {
        List<AgentRequiredField> required = new ArrayList<>();
        for (String field : fields) {
            String[] parts = field.split(":", 2);
            required.add(new AgentRequiredField(parts[0], parts.length > 1 ? parts[1] : parts[0]));
        }
        definitions.put(code, new AgentActionDefinition(code, title, module,
                Set.copyOf(new LinkedHashSet<>(roles)), List.copyOf(required)));
    }

    private void crud(String prefix, String title, String module, Set<String> roles,
                      String idField, String... createFields) {
        add(prefix + ".create", "新增" + title, module, roles, createFields);
        add(prefix + ".update", "修改" + title, module, roles, idField + ":" + title + "主键");
        add(prefix + ".delete", "删除" + title, module, roles, idField + ":" + title + "主键");
    }

    private Set<String> roles(String... roles) {
        Set<String> result = new LinkedHashSet<>();
        for (String role : roles) result.add(normalizeRole(role));
        return result;
    }

    private String normalizeRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new BusinessException(403, "当前用户尚未分配角色");
        }
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }
}
