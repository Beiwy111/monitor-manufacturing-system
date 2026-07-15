package com.upc.computer.assistant;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * 全 MES 语音动作注册表：售后之外的模块动作都在这里声明（写=经人工闸门后走
 * MesWorkflowService.execute；读=对 MesSnapshotService 快照做概况统计）。
 * NLU 的动作枚举 prompt 由 {@link #nluCatalogText()} 动态生成，新增动作只需加一条注册。
 */
@Component
public class MesActionCatalog {

    /** 一条可被语音调度的动作声明。写动作带实体定位/状态守卫/执行 payload；读动作带概况统计配置。 */
    public static class ActionSpec {
        public final String code;         // 全局动作码，如 device.alarm_receive
        public final String module;       // 审计模块名（agent_flow_step.module）
        public final String moduleCn;     // 模块中文名（提示语用）
        public final String nameCn;       // 动作中文名，如 接收报警
        public final boolean write;
        public final String nluHint;      // 动态 prompt 里的说明 + 触发例句

        // ── 写动作专用 ──
        public final String snapshotKey;  // 实体所在快照列表 key
        public final String entityNoun;   // 实体名词：报警/派工任务/采购单/入库任务
        public final List<String> matchKeys;   // 实体模糊匹配字段
        public final List<String> displayKeys; // 提议卡/候选列表展示字段
        public final Predicate<Map<String, Object>> actionable; // 当前状态可操作
        public final String mesAction;    // MesWorkflowService 动作名
        public final BiFunction<Map<String, Object>, Map<String, Object>, Map<String, Object>> payloadBuilder; // (实体行, 语音参数) -> payload
        public final String editableKey;         // 确认卡可编辑参数（null=无）
        public final String editablePlaceholder;

        // ── 读动作专用：概况统计 {标签, 快照key, 状态字段} ──
        public final List<String[]> summaries;

        private ActionSpec(String code, String module, String moduleCn, String nameCn, boolean write, String nluHint,
                           String snapshotKey, String entityNoun, List<String> matchKeys, List<String> displayKeys,
                           Predicate<Map<String, Object>> actionable, String mesAction,
                           BiFunction<Map<String, Object>, Map<String, Object>, Map<String, Object>> payloadBuilder,
                           String editableKey, String editablePlaceholder, List<String[]> summaries) {
            this.code = code; this.module = module; this.moduleCn = moduleCn; this.nameCn = nameCn;
            this.write = write; this.nluHint = nluHint;
            this.snapshotKey = snapshotKey; this.entityNoun = entityNoun;
            this.matchKeys = matchKeys; this.displayKeys = displayKeys;
            this.actionable = actionable; this.mesAction = mesAction; this.payloadBuilder = payloadBuilder;
            this.editableKey = editableKey; this.editablePlaceholder = editablePlaceholder;
            this.summaries = summaries;
        }

        static ActionSpec write(String code, String module, String moduleCn, String nameCn, String nluHint,
                                String snapshotKey, String entityNoun,
                                List<String> matchKeys, List<String> displayKeys,
                                Predicate<Map<String, Object>> actionable, String mesAction,
                                BiFunction<Map<String, Object>, Map<String, Object>, Map<String, Object>> payloadBuilder,
                                String editableKey, String editablePlaceholder) {
            return new ActionSpec(code, module, moduleCn, nameCn, true, nluHint, snapshotKey, entityNoun,
                    matchKeys, displayKeys, actionable, mesAction, payloadBuilder,
                    editableKey, editablePlaceholder, List.of());
        }

        static ActionSpec overview(String code, String module, String moduleCn, String nluHint, List<String[]> summaries) {
            return new ActionSpec(code, module, moduleCn, "查询" + moduleCn + "概况", false, nluHint,
                    null, null, List.of(), List.of(), r -> true, null, null, null, null, summaries);
        }
    }

    private final Map<String, ActionSpec> specs = new LinkedHashMap<>();

    public MesActionCatalog() {
        // ════ 设备 · 安灯 ════
        register(ActionSpec.write("device.alarm_receive", "DEVICE", "设备安灯", "接收报警",
                "接收/确认一条安灯报警（\"接收ALM-2026-001\"\"接收贴合机的报警\"）",
                "alarms", "报警",
                List.of("id", "equipmentName", "source", "type", "description"),
                List.of("source", "type", "level"),
                r -> in(str(r, "statusCode"), "OPEN", "REPORTED"), "handleAlarm",
                (row, params) -> Map.of("alarmId", str(row, "id"), "action", "receive"),
                null, null));
        register(ActionSpec.write("device.alarm_close", "DEVICE", "设备安灯", "解除报警",
                "解除/关闭一条安灯报警（\"解除贴合机的报警\"\"关闭ALM-2026-001\"）",
                "alarms", "报警",
                List.of("id", "equipmentName", "source", "type", "description"),
                List.of("source", "type", "level"),
                r -> !"CLOSED".equals(str(r, "statusCode")), "handleAlarm",
                (row, params) -> Map.of("alarmId", str(row, "id"), "action", "close",
                        "handleResult", orDefault(str(params, "remark"), "语音助手确认解除")),
                "remark", "可补充处理结果（选填）"));
        register(ActionSpec.overview("device.overview", "DEVICE", "设备安灯",
                "设备/安灯概况（\"现在有多少报警\"\"设备什么情况\"）",
                List.of(new String[]{"安灯报警", "alarms", "status"},
                        new String[]{"设备", "equipment", "status"})));

        // ════ 生产 · 派工 ════
        register(ActionSpec.write("production.dispatch_accept", "PRODUCTION", "生产管理", "接收派工",
                "操作员接收派工任务（\"接收派工DSP-xxx\"\"接收贴合工序的派工\"）",
                "dispatches", "派工任务",
                List.of("id", "workOrderId", "processStep", "equipment", "operatorName"),
                List.of("processStep", "operatorName", "planQty"),
                r -> "已分配".equals(str(r, "status")), "acceptDispatch",
                (row, params) -> Map.of("dispatchId", str(row, "id")),
                null, null));
        register(ActionSpec.write("production.dispatch_start", "PRODUCTION", "生产管理", "开始生产",
                "开始执行已接收的派工（\"开工\"\"开始生产DSP-xxx\"）",
                "dispatches", "派工任务",
                List.of("id", "workOrderId", "processStep", "equipment", "operatorName"),
                List.of("processStep", "operatorName", "planQty"),
                r -> "已接收".equals(str(r, "status")), "startDispatch",
                (row, params) -> Map.of("dispatchId", str(row, "id")),
                null, null));
        register(ActionSpec.overview("production.overview", "PRODUCTION", "生产管理",
                "生产工单/派工概况（\"生产什么情况\"\"有多少工单\"）",
                List.of(new String[]{"生产工单", "workOrders", "status"},
                        new String[]{"派工任务", "dispatches", "status"})));

        // ════ 采购 ════
        register(ActionSpec.write("purchase.receive", "PURCHASE", "采购管理", "确认到货",
                "确认采购订单到货入库（\"PO-2026-001到货了\"\"确认面板的采购到货\"），params.qty 为到货数量，不说默认剩余全部",
                "purchaseOrders", "采购单",
                List.of("id", "supplier", "materialCode", "materialName"),
                List.of("supplier", "materialName", "quantity"),
                r -> !in(str(r, "status"), "已到货", "已取消"), "receivePurchase",
                (row, params) -> Map.of("poId", str(row, "id"), "qty", resolveQty(row, params)),
                null, null));
        register(ActionSpec.overview("purchase.overview", "PURCHASE", "采购管理",
                "采购需求/订单概况（\"采购什么情况\"）",
                List.of(new String[]{"采购需求", "purchaseDemands", "status"},
                        new String[]{"采购订单", "purchaseOrders", "status"})));

        // ════ 仓储 ════
        register(ActionSpec.write("warehouse.inbound_confirm", "WAREHOUSE", "仓储管理", "确认入库",
                "确认成品入库任务（\"IN-001入库\"\"确认入库\"）",
                "inboundTasks", "入库任务",
                List.of("id", "workOrderId", "productModel", "batchNo", "refNo"),
                List.of("productModel", "quantity", "sourceType"),
                r -> "待入库".equals(str(r, "status")), "confirmInbound",
                (row, params) -> Map.of("taskId", str(row, "id")),
                null, null));
        register(ActionSpec.overview("warehouse.overview", "WAREHOUSE", "仓储管理",
                "库存/入库概况（\"库存什么情况\"\"有多少待入库\"）",
                List.of(new String[]{"库存物料", "inventory", "status"},
                        new String[]{"待入库任务", "inboundTasks", "status"})));

        // ════ 其余模块概况 ════
        register(ActionSpec.overview("quality.overview", "QUALITY", "质量管理",
                "质检/不合格品概况（\"质检什么情况\"\"有多少不合格\"）",
                List.of(new String[]{"质检任务", "inspections", "status"},
                        new String[]{"不合格品", "defects", "status"})));
        register(ActionSpec.overview("order.overview", "ORDER", "订单管理",
                "客户订单概况（\"订单什么情况\"）",
                List.<String[]>of(new String[]{"客户订单", "orders", "status"})));
        register(ActionSpec.overview("cost.overview", "COST", "成本管理",
                "成本结算概况（\"成本结算什么情况\"）",
                List.<String[]>of(new String[]{"成本结算", "costSettlements", "status"})));
        register(ActionSpec.overview("system.overview", "SYSTEM", "系统管理",
                "系统用户/日志概况（\"有多少用户\"）",
                List.of(new String[]{"系统用户", "sysUsers", "status"},
                        new String[]{"操作日志", "operationLogs", "module"})));
    }

    private void register(ActionSpec spec) { specs.put(spec.code, spec); }

    public ActionSpec get(String code) { return specs.get(code); }

    /** 动态生成 NLU prompt 的动作枚举段（含售后/设备诊断的专用动作，保持单一事实来源）。 */
    public String nluCatalogText() {
        StringBuilder sb = new StringBuilder();
        sb.append("  aftersale.query_list    查询售后案例列表/KPI统计（\"有多少售后\"\"看看待处理的\"）\n");
        sb.append("  aftersale.query_trace   查询某售后案例的质量追溯链（\"追溯一下星辰\"\"这单怎么来的\"）\n");
        sb.append("  aftersale.query_detail  售后案例详情/什么状态（\"它有哪些问题\"\"这单什么情况\"）\n");
        sb.append("  aftersale.advise        售后处理研判/建议（\"你觉得应该怎么处理\"\"这单怎么办\"\"给个处理建议\"）——给出根因嫌疑排序、预估损失和建议行动\n");
        sb.append("  aftersale.rca_dispatch  启动跨部门根因协查并派任务（\"启动根因协查\"\"给采购和质检派协同任务\"），params.departments 填提到的部门（如\"采购,质检\"），没提留空\n");
        sb.append("  aftersale.accept        受理售后案例（前提：待受理）\n");
        sb.append("  aftersale.resolve       售后标记解决（前提：处理中；必须有 solution 解决方案）\n");
        sb.append("  aftersale.close         关闭售后案例\n");
        sb.append("  device.diagnose         设备健康诊断/保养建议（\"EQ-003健康怎么样\"\"哪台设备该保养了\"\"设备健康总览\"）\n");
        sb.append("  warehouse.query_inventory 查询库存明细（\"查库存\"\"库存有多少\"\"MAT-001库存多少\"\"玻璃基板还有多少\"）——从数据库实时读取物料现存、库位与预警状态\n");
        sb.append("  order.create            对话下单/新建客户订单（\"给深圳华创下200台27寸4K显示器\"\"我要下一个订单\"），caseClue.customerName 填客户名，caseClue.keyword 填产品型号，params.qty 填数量，params.remark 填交期(YYYY-MM-DD，没说留空)\n");
        sb.append("  notify.send             跨模块协办通知（\"通知生产EQ-003故障相关派工暂缓\"\"提醒质检今天抽检\"），params.targetModule 填目标模块码，params.remark 填要转达的内容——通知只进对方通知中心，不改对方业务数据\n");
        for (ActionSpec s : specs.values()) {
            sb.append("  ").append(String.format("%-23s", s.code)).append(' ').append(s.nluHint).append('\n');
        }
        sb.append("  unknown                 完全无法归类");
        return sb.toString();
    }

    // ── 小工具 ──
    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v == null ? "" : v.toString();
    }

    private static boolean in(String v, String... options) {
        for (String o : options) if (o.equals(v)) return true;
        return false;
    }

    private static String orDefault(String v, String fb) { return v == null || v.isBlank() ? fb : v; }

    /** 采购到货数量：语音说了用语音的，否则默认剩余未到货量（至少 1）。 */
    private static int resolveQty(Map<String, Object> row, Map<String, Object> params) {
        Object said = params.get("qty");
        if (said instanceof Number n && n.intValue() > 0) return n.intValue();
        int total = numInt(row.get("quantity")), arrived = numInt(row.get("arrivedQty"));
        return Math.max(total - arrived, 1);
    }

    private static int numInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}
