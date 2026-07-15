package com.upc.computer.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.assistant.MesActionCatalog.ActionSpec;
import com.upc.computer.common.BusinessException;
import com.upc.computer.dto.MesActionRequest;
import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.mapper.AgentFlowMapper;
import com.upc.computer.service.AfterSalesService;
import com.upc.computer.service.EquipmentService;
import com.upc.computer.service.MesRuntimeStore;
import com.upc.computer.service.MesSnapshotService;
import com.upc.computer.service.MesWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 全 MES 语音助手编排：NLU → 实体消歧 → 状态机校验 → 槽位追问 → 生成提议(写)/直接应答(读)。
 * 售后动作走本类专用流程（客户名模糊消歧等）；其余模块动作由 {@link MesActionCatalog} 注册表
 * 驱动通用流程：快照定位实体 → 候选/守卫 → 提议 → 闸门后调 MesWorkflowService.execute。
 * 写操作永不自动执行，一律经 execute() 的人工闸门确认后才打真实接口。
 *
 * 治理边界：写操作（控制）锁定在当前页面所属模块内，跨模块只允许"通知"（notify.send，
 * 只进对方通知中心、不改对方业务数据）；查询/研判类全局可用。
 * 一句话多步指令（NLU steps）由 Flow 引擎顺序推进：读步骤直接执行，写步骤逐个过闸门。
 * 提议以内存 Map 为准（execute 依赖），并 best-effort 落 agent_flow_run/step 做审计（多步共用一条 run）。
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired private AssistantNluClient nlu;
    @Autowired private AfterSalesService afterSales;
    @Autowired private AssistantSessionStore sessions;
    @Autowired private AgentFlowMapper agentFlow;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MesActionCatalog catalog;
    @Autowired private MesWorkflowService mesWorkflow;
    @Autowired private MesSnapshotService mesSnapshot;
    @Autowired private EquipmentService equipmentService;
    @Autowired private MesRuntimeStore mesRuntime;

    private final Map<String, Proposal> proposals = new ConcurrentHashMap<>();
    private final Map<String, Flow> flows = new ConcurrentHashMap<>();
    private final AtomicLong proposalSeq = new AtomicLong(1000);

    /** 内存提议。售后动作 action 为裸码（accept/resolve/close/rca_dispatch），MES 通用动作为全码且 specCode 非空。 */
    static class Proposal {
        String id, action, entityNo, humanReadable, status = "PROPOSED", userText;
        String specCode;                       // 非空 = MES 通用动作
        String flowId;                         // 非空 = 隶属多步流，执行后自动推进下一步
        Map<String, Object> entityRow;         // MES 通用动作的实体快照行
        Map<String, Object> params = new LinkedHashMap<>();
        Long auditStepId;
    }

    /** 一句话多步流：步骤顺序执行，读步骤直接过，写步骤逐个闸门。 */
    static class Flow {
        String id, sessionId, module, userText;
        List<NluResult> steps;
        int idx = 0, total;
        Long auditRunId;
    }

    // ════════════════════════ 解析 ════════════════════════
    public Map<String, Object> interpret(String sessionId, String module, String text) {
        if (text == null || text.isBlank()) return ask("没听清，请再说一遍（可含单号、客户名或设备名）。");
        module = normalizeModule(module);
        if (!blank(module)) sessions.put(sessionId, "currentModule", module);
        else module = nz(sessions.get(sessionId, "currentModule"));

        // 多轮：若上一轮卡在候选选择 / 槽位追问，优先按补充语处理
        Map<String, Object> pending = resumePending(sessionId, text);
        if (pending != null) return pending;

        NluResult r = nlu.interpret(text, contextJson(sessionId));
        String action = AssistantNluClient.normalizeAction(r.action());

        if ("unknown".equals(action)) return ask(blankOr(r.reply(), capabilityLine()));

        // 一句话多步指令 → Flow 引擎（内部逐步做边界校验）
        if (r.steps() != null && r.steps().size() >= 2) return startFlow(sessionId, module, r, text);

        // 治理边界：写操作只能控制本模块，跨模块请用通知
        String guard = boundaryGuard(action, module);
        if (guard != null) return ask(guard);

        return dispatchAction(sessionId, action, r, text);
    }

    /** 单动作分发（边界校验已在调用方完成；Flow 引擎逐步复用本方法）。 */
    private Map<String, Object> dispatchAction(String sessionId, String action, NluResult r, String text) {
        // 售后模块：专用流程（客户名消歧/追溯链/KPI/研判）
        if (action.startsWith("aftersale.")) {
            String sub = action.substring("aftersale.".length());
            if ("query_list".equals(sub)) return answerKpi();
            if ("query_trace".equals(sub)) return answerTrace(sessionId, r);
            if ("query_detail".equals(sub)) return answerDetail(sessionId, r);
            if ("advise".equals(sub)) return answerAdvise(sessionId, r);
            if ("rca_dispatch".equals(sub)) return startRcaDispatch(sessionId, r, text);
            return startAftersaleWrite(sessionId, sub, r, text);
        }

        // 跨模块协办通知（唯一放行的跨模块"动词"）
        if ("notify.send".equals(action)) return startNotify(sessionId, r, text);

        // 设备健康诊断：接健康分引擎（读）
        if ("device.diagnose".equals(action)) return answerDiagnose(r);

        // 库存明细查询：读数据库快照中的 inventory 列表
        if ("warehouse.query_inventory".equals(action)) return answerInventoryQuery(r);

        // 对话下单：槽位齐全 → 提议卡；缺槽位 → 追问
        if ("order.create".equals(action)) return startOrderCreate(sessionId, r, text);

        // 其余模块：注册表驱动的通用流程
        ActionSpec spec = catalog.get(action);
        if (spec == null) return ask(capabilityLine());
        if (!spec.write) return answerOverview(spec);
        return startMesWrite(sessionId, spec, r, text);
    }

    private String capabilityLine() {
        return "我能处理：售后受理/解决/关闭与追溯、处理研判（\"你觉得怎么处理\"）、启动根因协查派单，接收/解除安灯报警，设备健康诊断，查库存/物料库存明细（\"查库存\"\"MAT-001有多少\"），接收派工/开始生产，采购确认到货，成品确认入库，各模块概况查询，跨模块协办通知（\"通知生产…\"），还支持一句话串多步（\"受理星辰这单并启动根因协查\"）。注意：写操作只能在所属模块页面执行。";
    }

    // ════════════════════════ 模块治理边界 ════════════════════════
    /** 模块码 → 中文名 */
    private static final Map<String, String> MODULE_CN = Map.ofEntries(
            Map.entry("aftersale", "售后"), Map.entry("device", "设备"), Map.entry("production", "生产"),
            Map.entry("purchase", "采购"), Map.entry("warehouse", "仓储"), Map.entry("quality", "质量"),
            Map.entry("order", "订单"), Map.entry("cost", "成本"), Map.entry("system", "系统"));

    /** 注册表 spec.module（大写审计名）→ 前端模块码 */
    private static final Map<String, String> SPEC_MODULE_KEY = Map.of(
            "DEVICE", "device", "PRODUCTION", "production", "PURCHASE", "purchase",
            "WAREHOUSE", "warehouse", "QUALITY", "quality", "ORDER", "order",
            "COST", "cost", "SYSTEM", "system", "AFTERSALES", "aftersale");

    /** 前端传来的模块/角色码归一：生产口三个角色都算生产模块，admin 算系统。 */
    private String normalizeModule(String m) {
        if (blank(m)) return "";
        return switch (m) {
            case "manager", "operator", "planner", "monitor" -> "production";
            case "admin" -> "system";
            default -> m;
        };
    }

    /** 写动作的归属模块码；读/研判/通知动作返回 null（全局可用）。 */
    private String ownerModule(String action) {
        if ("notify.send".equals(action)) return null;
        if ("order.create".equals(action)) return "order";
        if (action.startsWith("aftersale.")) {
            String sub = action.substring("aftersale.".length());
            return ("accept".equals(sub) || "resolve".equals(sub) || "close".equals(sub)
                    || "rca_dispatch".equals(sub)) ? "aftersale" : null;
        }
        ActionSpec spec = catalog.get(action);
        if (spec == null || !spec.write) return null;
        return SPEC_MODULE_KEY.get(spec.module);
    }

    private String actionDisplayName(String action) {
        if ("notify.send".equals(action)) return "发协办通知";
        if ("device.diagnose".equals(action)) return "设备诊断";
        if ("warehouse.query_inventory".equals(action)) return "查库存";
        if ("order.create".equals(action)) return "对话下单";
        if (action.startsWith("aftersale.")) return actionCn(action.substring("aftersale.".length()));
        ActionSpec spec = catalog.get(action);
        return spec != null ? spec.nameCn : action;
    }

    /** 跨模块控制拦截：null=放行；否则给出拒绝话术并引导改用协办通知。 */
    private String boundaryGuard(String action, String module) {
        String owner = ownerModule(action);
        if (owner == null || owner.equals(module)) return null;
        if ("system".equals(module)) return null;   // 系统管理员全模块放行
        if ("order.create".equals(action) && "customer".equals(module)) return null;   // 客户可对话下单
        String ownerCn = MODULE_CN.getOrDefault(owner, owner);
        String hereCn = blank(module) ? "当前" : MODULE_CN.getOrDefault(module, module);
        return "「" + actionDisplayName(action) + "」是" + ownerCn + "模块的操作，我在" + hereCn
                + "页面不能跨模块控制（跨模块只能通知，不能替对方执行）。可以到" + ownerCn + "模块页面再说一次；"
                + "或者说\"通知" + ownerCn + " + 要转达的内容\"，我把它作为协办通知发到对方通知中心。";
    }

    // ════════════════════════ 多步流引擎 ════════════════════════
    /** 一句话多步：先整体做边界预检（写步骤必须全部属于本模块），再顺序推进。 */
    private Map<String, Object> startFlow(String sessionId, String module, NluResult r, String text) {
        List<NluResult> steps = r.steps();
        for (int i = 0; i < steps.size(); i++) {
            String a = AssistantNluClient.normalizeAction(steps.get(i).action());
            if ("unknown".equals(a)) {
                return ask("第 " + (i + 1) + " 步没听懂，整串指令先不执行。请拆开说，或换个说法。");
            }
            String guard = boundaryGuard(a, module);
            if (guard != null) {
                return ask("这串指令的第 " + (i + 1) + " 步被拦下了：" + guard + "\n整串指令未执行。");
            }
        }
        Flow f = new Flow();
        f.id = "F" + proposalSeq.incrementAndGet();
        f.sessionId = sessionId;
        f.module = module;
        f.steps = steps;
        f.total = steps.size();
        f.userText = text;
        flows.put(f.id, f);
        auditFlowRun(f);

        StringBuilder prefix = new StringBuilder("收到，共 ").append(f.total)
                .append(" 步。查询直接执行，写操作会逐步请你确认：\n\n");
        return advanceFlow(f, prefix);
    }

    /**
     * 推进流程：读步骤立刻执行并累积回复；写步骤生成提议（闸门）后暂停，
     * execute() 批准后从下一步继续。候选/槽位追问会挂起在会话里，用户补话后自动接回流程。
     */
    private Map<String, Object> advanceFlow(Flow f, StringBuilder prefix) {
        while (f.idx < f.total) {
            NluResult step = f.steps.get(f.idx);
            String action = AssistantNluClient.normalizeAction(step.action());
            String tag = "【第" + (f.idx + 1) + "/" + f.total + "步 · " + actionDisplayName(action) + "】";
            sessions.put(f.sessionId, "flowBind", f.id);
            Map<String, Object> resp = dispatchAction(f.sessionId, action, step,
                    f.userText + "（第" + (f.idx + 1) + "步）");
            String type = str(resp, "type");

            if ("answer".equals(type)) {                       // 读步骤：直接完成
                sessions.put(f.sessionId, "flowBind", null);
                prefix.append(tag).append('\n').append(str(resp, "reply")).append("\n\n");
                f.idx++;
                continue;
            }
            if ("confirm".equals(type)) {                      // 写步骤：闸门，等确认
                sessions.put(f.sessionId, "flowBind", null);   // 提议已在 bindFlow 拿走绑定，这里兜底
                resp.put("reply", prefix + tag + " " + str(resp, "reply"));
                resp.put("flowStep", (f.idx + 1) + "/" + f.total);
                return resp;
            }
            // ask：若挂起了候选/槽位多轮 → 等用户补话（flowBind 保留，补话生成提议时接回流程）
            if (sessions.get(f.sessionId, "pendingAction") != null) {
                resp.put("reply", prefix + tag + " " + str(resp, "reply"));
                return resp;
            }
            // 硬失败（定位不到/状态不允许）→ 终止流程
            abortFlow(f);
            resp.put("reply", prefix + tag + " " + str(resp, "reply")
                    + "\n⛔ 这一步走不下去，流程终止；前面已完成的步骤保持有效。");
            return resp;
        }
        flows.remove(f.id);
        sessions.put(f.sessionId, "flowBind", null);
        return answer(prefix.append("✅ 全部 ").append(f.total).append(" 步已完成。").toString(),
                Map.of("flowId", f.id, "total", f.total));
    }

    private void abortFlow(Flow f) {
        flows.remove(f.id);
        sessions.put(f.sessionId, "flowBind", null);
    }

    /** 用户在流程等待中说了不相干的新指令 → 视为放弃流程。 */
    private void abortFlowIfBound(String sessionId) {
        Object fb = sessions.get(sessionId, "flowBind");
        if (fb != null) {
            Flow f = flows.get(fb.toString());
            if (f != null) abortFlow(f);
            sessions.put(sessionId, "flowBind", null);
        }
    }

    /** 提议生成时若处于流程分发中，则绑定 flowId（execute 后自动推进下一步）。 */
    private void bindFlow(String sessionId, Proposal p) {
        Object fb = sessions.get(sessionId, "flowBind");
        if (fb != null) {
            p.flowId = fb.toString();
            sessions.put(sessionId, "flowBind", null);
        }
    }

    /** 多步流建一条 agent_flow_run，各步提议以 stepNo 挂到同一条 run 下。 */
    private void auditFlowRun(Flow f) {
        try {
            Map<String, Object> run = new LinkedHashMap<>();
            run.put("flowNo", "ASF" + LocalDateTime.now().format(FMT) + f.id);
            run.put("templateCode", "MES_VOICE_FLOW");
            run.put("goal", f.userText);
            run.put("status", "PAUSED");
            run.put("currentStepNo", 1);
            run.put("contextJson", null);
            run.put("createdBy", null);
            agentFlow.insertRun(run);
            f.auditRunId = asLong(run.get("flowId"));
        } catch (Exception e) {
            log.warn("[Assistant] 多步流审计落库跳过：{}", e.getMessage());
        }
    }

    // ════════════════════════ 跨模块协办通知 ════════════════════════
    /** 中文模块词 → 模块码（notify 目标追问用） */
    private static final Map<String, String> WORD_TO_MODULE = Map.ofEntries(
            Map.entry("售后", "aftersale"), Map.entry("设备", "device"), Map.entry("维保", "device"),
            Map.entry("生产", "production"), Map.entry("车间", "production"),
            Map.entry("采购", "purchase"), Map.entry("仓储", "warehouse"), Map.entry("仓库", "warehouse"),
            Map.entry("质检", "quality"), Map.entry("质量", "quality"),
            Map.entry("订单", "order"), Map.entry("销售", "order"),
            Map.entry("成本", "cost"), Map.entry("财务", "cost"), Map.entry("系统", "system"));

    private String moduleFromWord(String text) {
        if (blank(text)) return "";
        for (Map.Entry<String, String> e : WORD_TO_MODULE.entrySet()) {
            if (text.contains(e.getKey())) return e.getValue();
        }
        return MODULE_CN.containsKey(text.trim()) ? text.trim() : "";
    }

    /** "通知生产 EQ-003 故障相关派工暂缓" → 提议（闸门）→ 写入对方通知中心。 */
    private Map<String, Object> startNotify(String sessionId, NluResult r, String userText) {
        Map<String, Object> params = new LinkedHashMap<>();
        String content = blankOr(r.remark(), userText);
        params.put("content", content);
        params.put("sourceModule", nz(sessions.get(sessionId, "currentModule")));

        String target = r.targetModule();
        if (blank(target)) target = moduleFromWord(nz(r.keyword()));
        if (blank(target)) {
            sessions.put(sessionId, "pendingAction", "notify.send");
            sessions.put(sessionId, "pendingParams", params);
            return ask("要通知哪个模块？可以说：生产 / 采购 / 质检 / 设备 / 仓储 / 订单 / 售后 / 成本 / 系统。");
        }
        return makeNotifyProposal(sessionId, target, params, userText);
    }

    private Map<String, Object> makeNotifyProposal(String sessionId, String target, Map<String, Object> params,
                                                   String userText) {
        clearPending(sessionId);
        Proposal p = new Proposal();
        p.id = String.valueOf(proposalSeq.incrementAndGet());
        p.action = "notify.send";
        p.entityNo = target;
        params.put("targetModule", target);
        p.params = params;
        p.userText = userText;
        String targetCn = MODULE_CN.getOrDefault(target, target);
        p.humanReadable = "我将【发送协办通知】→ " + targetCn + "模块"
                + "\n内容：" + str(params, "content")
                + "\n（通知进入对方通知中心，不改动对方业务数据。）确认?";
        bindFlow(sessionId, p);
        proposals.put(p.id, p);
        auditPropose(p, userText, null);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("type", "confirm");
        resp.put("proposalId", p.id);
        resp.put("action", "notify.send");
        resp.put("entityNo", target);
        resp.put("humanReadable", p.humanReadable);
        resp.put("editable", Map.of("key", "content", "placeholder", "可修改通知内容（选填）"));
        resp.put("reply", "给" + targetCn + "模块发协办通知，确认吗？");
        return resp;
    }

    /** 通知落到 Redis 运行时存储，前端 GlobalBusinessMonitor 轮询并入目标模块角色的通知中心。 */
    private Map<String, Object> appendNotice(String target, String content, String sourceModule, String operator) {
        MesRuntimeStore.MesRuntimeState st = mesRuntime.load();
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("id", "VN" + LocalDateTime.now().format(FMT) + proposalSeq.incrementAndGet());
        n.put("targetModule", target);
        n.put("targetModuleCn", MODULE_CN.getOrDefault(target, target));
        n.put("sourceModule", sourceModule);
        n.put("sourceModuleCn", MODULE_CN.getOrDefault(sourceModule, sourceModule));
        n.put("title", blank(sourceModule)
                ? "语音助手协办通知"
                : "来自" + MODULE_CN.getOrDefault(sourceModule, sourceModule) + "模块的协办通知");
        n.put("content", content);
        n.put("from", (blank(operator) ? "" : operator + " · ") + "语音助手");
        n.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        st.getVoiceNotices().add(0, n);
        if (st.getVoiceNotices().size() > 100) {
            st.setVoiceNotices(new ArrayList<>(st.getVoiceNotices().subList(0, 100)));
        }
        mesRuntime.save(st);
        return n;
    }

    /** 协办通知列表（target 为空返回全部，前端按角色过滤展示）。 */
    public List<Map<String, Object>> listNotices(String target) {
        List<Map<String, Object>> all = mesRuntime.load().getVoiceNotices();
        if (blank(target)) return all;
        return all.stream().filter(n -> target.equals(str(n, "targetModule"))).toList();
    }

    // ════════════════════════ MES 通用写流程 ════════════════════════
    private Map<String, Object> startMesWrite(String sessionId, ActionSpec spec, NluResult r, String userText) {
        Map<String, Object> snapshot = mesSnapshot.buildSnapshot();
        List<Map<String, Object>> all = rows(snapshot, spec.snapshotKey);
        List<Map<String, Object>> pool = all.stream().filter(spec.actionable).toList();
        if (pool.isEmpty()) return ask("当前没有可" + spec.nameCn + "的" + spec.entityNoun + "。");

        String clue = !blank(r.caseNo()) ? r.caseNo() : r.keyword();
        List<Map<String, Object>> hits = weakClue(clue) ? List.of() : matchRows(pool, spec, clue);

        // 有明确线索但可操作池里没有 → 查全量，给出状态原因而不是干巴巴没找到
        if (hits.isEmpty() && !weakClue(clue)) {
            List<Map<String, Object>> fullHits = matchRows(all, spec, clue);
            if (!fullHits.isEmpty()) {
                Map<String, Object> row = fullHits.get(0);
                return ask(str(row, "id") + " 当前是「" + str(row, "status") + "」，不能" + spec.nameCn + "。");
            }
        }

        Map<String, Object> target = null;
        if (hits.size() == 1) target = hits.get(0);
        else if (hits.isEmpty() && weakClue(clue) && pool.size() == 1) target = pool.get(0);

        if (target == null) {
            List<Map<String, Object>> candidates = hits.isEmpty() ? pool : hits;
            sessions.put(sessionId, "candidates", candidates);
            sessions.put(sessionId, "pendingAction", spec.code);
            sessions.put(sessionId, "pendingParams", voiceParams(r));
            return askMesCandidates(spec, candidates);
        }
        return makeMesProposal(sessionId, spec, target, voiceParams(r), userText);
    }

    private Map<String, Object> makeMesProposal(String sessionId, ActionSpec spec, Map<String, Object> row,
                                                Map<String, Object> params, String userText) {
        clearPending(sessionId);
        Proposal p = new Proposal();
        p.id = String.valueOf(proposalSeq.incrementAndGet());
        p.action = spec.code;
        p.specCode = spec.code;
        p.entityNo = str(row, "id");
        p.entityRow = row;
        p.params = params;
        p.userText = userText;
        p.humanReadable = mesHumanReadable(spec, row, params);
        bindFlow(sessionId, p);
        proposals.put(p.id, p);
        auditPropose(p, userText, spec);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("type", "confirm");
        resp.put("proposalId", p.id);
        resp.put("action", spec.code);
        resp.put("entityNo", p.entityNo);
        resp.put("humanReadable", p.humanReadable);
        if (spec.editableKey != null) {
            resp.put("editable", Map.of("key", spec.editableKey, "placeholder", spec.editablePlaceholder));
        }
        resp.put("reply", "帮你定位到 " + p.entityNo + "，" + spec.nameCn + "吗？");
        return resp;
    }

    private String mesHumanReadable(ActionSpec spec, Map<String, Object> row, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("我将【").append(spec.nameCn).append("】").append(str(row, "id"));
        for (String k : spec.displayKeys) {
            String v = str(row, k);
            if (!v.isBlank()) sb.append(" · ").append(v);
        }
        sb.append(" · 当前").append(str(row, "status"));
        Map<String, Object> payload = spec.payloadBuilder.apply(row, params);
        if (payload.containsKey("qty")) sb.append("\n到货数量：").append(payload.get("qty"));
        return sb.append("。确认?").toString();
    }

    private Map<String, Object> askMesCandidates(ActionSpec spec, List<Map<String, Object>> hits) {
        StringBuilder sb = new StringBuilder("找到多条可").append(spec.nameCn).append("的")
                .append(spec.entityNoun).append("，请说编号：");
        for (int i = 0; i < hits.size() && i < 5; i++) {
            Map<String, Object> row = hits.get(i);
            sb.append("\n").append(i + 1).append(". ").append(str(row, "id"));
            for (String k : spec.displayKeys) {
                String v = str(row, k);
                if (!v.isBlank()) sb.append(" · ").append(v);
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "ask");
        m.put("reply", sb.toString());
        m.put("data", hits);
        return m;
    }

    /** 双向 contains 模糊匹配：单号精确命中优先，其余在 matchKeys 上匹配。 */
    private List<Map<String, Object>> matchRows(List<Map<String, Object>> rows, ActionSpec spec, String clue) {
        String c = clue.trim().toLowerCase();
        for (Map<String, Object> row : rows) {
            if (str(row, "id").equalsIgnoreCase(clue.trim())) return List.of(row);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            for (String k : spec.matchKeys) {
                String v = str(row, k).toLowerCase();
                if (v.length() < 2) continue;
                if (v.contains(c) || c.contains(v)) { out.add(row); break; }
            }
        }
        return out;
    }

    private Map<String, Object> voiceParams(NluResult r) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (!blank(r.solution())) p.put("solution", r.solution());
        if (!blank(r.traceResult())) p.put("traceResult", r.traceResult());
        if (!blank(r.remark())) p.put("remark", r.remark());
        if (r.qty() > 0) p.put("qty", r.qty());
        return p;
    }

    // ════════════════════════ 库存明细查询（读库） ════════════════════════
    /** 从 MesSnapshotService 快照（inventory 表）查物料现存、库位与预警。 */
    private Map<String, Object> answerInventoryQuery(NluResult r) {
        Map<String, Object> snapshot = mesSnapshot.buildSnapshot();
        List<Map<String, Object>> all = rows(snapshot, "inventory");
        if (all.isEmpty()) {
            return ask("数据库里没有库存记录。请确认 inventory 表是否已初始化（可执行 sql/init/seed_data.sql）。");
        }
        String clue = resolveInventoryClue(r);
        if (!weakClue(clue)) {
            String c = clue.trim().toLowerCase();
            List<Map<String, Object>> hits = new ArrayList<>();
            for (Map<String, Object> row : all) {
                String code = str(row, "materialCode").toLowerCase();
                String name = str(row, "materialName").toLowerCase();
                String loc = str(row, "location").toLowerCase();
                if (!code.isBlank() && code.equals(c)) {
                    hits = List.of(row);
                    break;
                }
                if ((!code.isBlank() && (code.contains(c) || c.contains(code)))
                        || (!name.isBlank() && (name.contains(c) || c.contains(name)))
                        || (!loc.isBlank() && loc.contains(c))) {
                    hits.add(row);
                }
            }
            if (hits.size() == 1) return inventoryDetailReply(hits.get(0));
            if (hits.size() > 1) {
                return inventoryListReply(hits, "匹配到 " + hits.size() + " 条库存，请说更具体的物料编码或名称：");
            }
            return ask("没找到「" + clue + "」的库存记录。可以说「查库存」看全部，或换物料编码/名称再试。");
        }
        return inventoryListReply(all, "当前库存一览（数据库共 " + all.size() + " 条）：");
    }

    private String resolveInventoryClue(NluResult r) {
        String caseNo = r.caseNo();
        if (!blank(caseNo) && caseNo.toUpperCase().startsWith("MAT")) return caseNo;
        if (!blank(r.keyword())) return r.keyword().trim();
        return "";
    }

    private Map<String, Object> inventoryDetailReply(Map<String, Object> row) {
        StringBuilder sb = new StringBuilder();
        sb.append(str(row, "materialName")).append("（").append(str(row, "materialCode")).append("）库存：");
        sb.append("\n现存 ").append(str(row, "quantity")).append(' ').append(blankOr(str(row, "unit"), "件"));
        sb.append("，安全库存 ").append(str(row, "safeQty"));
        sb.append("，状态 ").append(str(row, "status"));
        if (!str(row, "location").isBlank()) sb.append("\n库位 ").append(str(row, "location"));
        if (!str(row, "assemblyGroup").isBlank()) sb.append(" · ").append(str(row, "assemblyGroup"));
        return answer(sb.toString(), row);
    }

    private Map<String, Object> inventoryListReply(List<Map<String, Object>> hits, String header) {
        StringBuilder sb = new StringBuilder(header);
        int limit = Math.min(hits.size(), 12);
        for (int i = 0; i < limit; i++) {
            Map<String, Object> row = hits.get(i);
            sb.append('\n').append(i + 1).append(". ")
                    .append(str(row, "materialName")).append(' ').append(str(row, "materialCode"))
                    .append(" · ").append(str(row, "quantity")).append(blankOr(str(row, "unit"), "件"))
                    .append(" · ").append(str(row, "status"));
            if (!str(row, "location").isBlank()) sb.append(" · ").append(str(row, "location"));
        }
        if (hits.size() > limit) sb.append("\n…还有 ").append(hits.size() - limit).append(" 条未列出");
        return answer(sb.toString(), Map.of("inventory", hits.subList(0, limit)));
    }

    // ════════════════════════ 对话下单（写） ════════════════════════
    private Map<String, Object> startOrderCreate(String sessionId, NluResult r, String userText) {
        Map<String, Object> snapshot = mesSnapshot.buildSnapshot();
        List<Map<String, Object>> models = rows(snapshot, "productModels");
        if (models.isEmpty()) return ask("系统里还没有成品物料，无法下单。请先在物料主数据中维护成品。");

        String customerName = blankOr(r.customerName(), r.caseNo());
        String productClue = blankOr(r.keyword(), "");
        int qty = r.qty();

        // 产品型号解析：按编码/名称/规格双向 contains 匹配
        Map<String, Object> model = null;
        if (!blank(productClue)) {
            String c = productClue.trim().toLowerCase();
            List<Map<String, Object>> hits = new ArrayList<>();
            for (Map<String, Object> m : models) {
                String code = str(m, "code").toLowerCase();
                String name = str(m, "name").toLowerCase();
                String spec = str(m, "specification").toLowerCase();
                if (code.equals(c)) { hits = List.of(m); break; }
                if ((name.length() >= 2 && (name.contains(c) || c.contains(name)))
                        || (spec.length() >= 2 && (spec.contains(c) || c.contains(spec)))
                        || (code.length() >= 3 && code.contains(c))) {
                    hits.add(m);
                }
            }
            if (hits.size() == 1) model = hits.get(0);
            else if (hits.size() > 1) {
                StringBuilder sb = new StringBuilder("「" + productClue + "」匹配到多个产品，请说全称再下一次：");
                for (int i = 0; i < hits.size() && i < 5; i++) {
                    sb.append('\n').append(i + 1).append(". ").append(str(hits.get(i), "name"))
                            .append("（").append(str(hits.get(i), "code")).append("）");
                }
                return ask(sb.toString());
            }
        }

        // 槽位不全 → 一次性列出还缺什么，并给可选产品清单
        List<String> missing = new ArrayList<>();
        if (blank(customerName)) missing.add("客户名");
        if (model == null) missing.add("产品型号");
        if (qty <= 0) missing.add("数量");
        if (!missing.isEmpty()) {
            StringBuilder sb = new StringBuilder("好的，下单还差：").append(String.join("、", missing))
                    .append("。请一句话说全，例如「给深圳华创下 200 台 ")
                    .append(str(models.get(0), "name")).append("」。");
            if (model == null) {
                sb.append("\n可选产品：");
                for (int i = 0; i < models.size() && i < 6; i++) {
                    sb.append('\n').append(i + 1).append(". ").append(str(models.get(i), "name"))
                            .append("（").append(str(models.get(i), "code")).append("）");
                }
            }
            return ask(sb.toString());
        }

        clearPending(sessionId);
        Proposal p = new Proposal();
        p.id = String.valueOf(proposalSeq.incrementAndGet());
        p.action = "order.create";
        p.entityNo = "新客户订单";
        p.params = new LinkedHashMap<>();
        p.params.put("customerName", customerName);
        p.params.put("productModel", str(model, "name"));
        p.params.put("panelType", str(model, "specification"));
        p.params.put("quantity", qty);
        if (!blank(r.remark())) p.params.put("deliveryDate", r.remark().trim());
        p.userText = userText;
        p.humanReadable = "我将创建一张客户订单：\n客户：" + customerName
                + "\n产品：" + str(model, "name") + "（" + str(model, "code") + "）"
                + "\n数量：" + qty + " 台"
                + (blank(r.remark()) ? "" : "\n交期：" + r.remark().trim())
                + "\n提交后进入订单审核流程。确认下单？";
        bindFlow(sessionId, p);
        proposals.put(p.id, p);
        auditPropose(p, userText, null);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("type", "confirm");
        resp.put("proposalId", p.id);
        resp.put("action", p.action);
        resp.put("entityNo", p.entityNo);
        resp.put("humanReadable", p.humanReadable);
        resp.put("editable", Map.of("key", "remark", "placeholder", "可补充备注（如交期要求），不填直接确认"));
        resp.put("reply", "订单信息齐了：" + customerName + " · " + str(model, "name") + " · " + qty + " 台。确认下单吗？");
        return resp;
    }

    // ════════════════════════ 概况应答（读） ════════════════════════
    private Map<String, Object> answerOverview(ActionSpec spec) {
        Map<String, Object> snapshot = mesSnapshot.buildSnapshot();
        StringBuilder reply = new StringBuilder();
        Map<String, Object> data = new LinkedHashMap<>();
        for (String[] s : spec.summaries) {
            String label = s[0], key = s[1], statusKey = s[2];
            List<Map<String, Object>> list = rows(snapshot, key);
            Map<String, Integer> groups = new LinkedHashMap<>();
            for (Map<String, Object> row : list) {
                String g = blankOr(str(row, statusKey), "未知");
                groups.merge(g, 1, Integer::sum);
            }
            if (reply.length() > 0) reply.append('\n');
            reply.append(label).append("共 ").append(list.size()).append(" 条");
            if (!groups.isEmpty()) {
                reply.append("：");
                List<String> parts = new ArrayList<>();
                groups.forEach((k, v) -> parts.add(k + " " + v));
                reply.append(String.join("，", parts));
            }
            reply.append('。');
            data.put(key, groups);
        }
        return answer(reply.toString(), data);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Map<String, Object> snapshot, String key) {
        Object v = snapshot.get(key);
        return v instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMaps(Object v) {
        return v instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    // ════════════════════════ 售后研判（RCA 引擎） ════════════════════════
    /** "你觉得怎么处理" → 根因嫌疑排序 + 预估损失 + 建议行动，并引导一句话派协查。 */
    private Map<String, Object> answerAdvise(String sessionId, NluResult r) {
        String caseNo = resolveReadCase(sessionId, r);
        if (caseNo == null) {
            List<Map<String, Object>> hits = locateForRead(r);
            if (hits.isEmpty()) return ask("要研判哪个案例？请说客户名或案例号。");
            if (hits.size() > 1) return askCandidates(hits);
            caseNo = str(hits.get(0), "caseNo");
        }
        AfterSalesCase c = afterSales.getAfterSalesCaseById(caseNo);
        if (c == null) return ask("案例 " + caseNo + " 不存在。");
        setCurrent(sessionId, caseNo, c.getCustomerName());

        Map<String, Object> analysis;
        try {
            analysis = afterSales.buildRcaAnalysis(caseNo, false);
        } catch (BusinessException e) {
            return ask("这单暂时没法研判：" + e.getMessage());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(caseNo).append(" · ").append(nz(c.getCustomerName())).append(" 处理研判：");
        String conclusion = str(analysis, "conclusion");
        if (!conclusion.isBlank()) sb.append('\n').append(conclusion);

        List<Map<String, Object>> causes = listOfMaps(analysis.get("causes"));
        if (!causes.isEmpty()) {
            sb.append("\n疑因排查优先级：");
            for (int i = 0; i < causes.size() && i < 3; i++) {
                Map<String, Object> cm = causes.get(i);
                sb.append('\n').append(i + 1).append(". ").append(str(cm, "name"))
                  .append("（").append(deptCn(str(cm, "department")))
                  .append("，嫌疑分 ").append(str(cm, "score")).append("/100）");
            }
        }
        sb.append("\n预估质量损失：¥").append(str(analysis, "totalLoss"));
        String completeness = str(analysis, "dataCompleteness");
        if (!completeness.isBlank()) {
            try { sb.append("（数据完备度 ").append(Math.round(Double.parseDouble(completeness) * 100)).append("%）"); }
            catch (NumberFormatException ignore) { }
        }
        List<Map<String, Object>> actions = listOfMaps(analysis.get("actions"));
        if (!actions.isEmpty()) {
            sb.append("\n建议行动：");
            for (int i = 0; i < actions.size() && i < 4; i++) {
                Map<String, Object> am = actions.get(i);
                sb.append("\n· [").append(deptCn(str(am, "department"))).append("] ").append(str(am, "title"));
            }
        }
        Map<String, Object> progress = afterSales.rcaTaskProgress(caseNo);
        if (num(progress, "total") > 0) {
            sb.append("\n协查进度：共 ").append(v(progress, "total")).append(" 个任务，已完成 ")
              .append(v(progress, "completed")).append("。");
        } else {
            sb.append("\n说\"启动根因协查\"，我就按上面的建议给相关部门派协同任务（会先请你确认）。");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("caseNo", caseNo);
        data.put("conclusion", analysis.get("conclusion"));
        data.put("causes", analysis.get("causes"));
        data.put("totalLoss", analysis.get("totalLoss"));
        data.put("actions", analysis.get("actions"));
        data.put("dataCompleteness", analysis.get("dataCompleteness"));
        return answer(sb.toString(), data);
    }

    /** "启动根因协查/给采购质检派任务" → 定位案例 → 提议（部门可指定，默认引擎建议的四部门）。 */
    private Map<String, Object> startRcaDispatch(String sessionId, NluResult r, String userText) {
        Map<String, Object> params = voiceParams(r);
        List<String> departments = parseDepartments(r.departments(), userText);
        if (!departments.isEmpty()) params.put("departments", departments);

        List<Map<String, Object>> hits;
        if (!r.caseNo().isBlank() && r.caseNo().matches("(?i)AS\\d{6,}")) {
            AfterSalesCase c = afterSales.getAfterSalesCaseById(r.caseNo());
            if (c == null) return ask("没找到案例 " + r.caseNo() + "，请确认案例号或说客户名。");
            hits = List.of(caseToRow(c));
        } else {
            hits = findCase(r.customerName(), r.problemTypeCn(), r.keyword(), null, false);
            if (hits.isEmpty()) {
                Object cur = sessions.get(sessionId, "currentCaseNo");   // 刚研判过的案例
                if (cur != null) {
                    AfterSalesCase c = afterSales.getAfterSalesCaseById(cur.toString());
                    if (c != null) hits = List.of(caseToRow(c));
                }
            }
        }
        if (hits.isEmpty()) {
            List<Map<String, Object>> all = actionableCases("rca_dispatch");
            if (all.isEmpty()) return ask("当前没有售后案例可协查。");
            sessions.put(sessionId, "candidates", all);
            sessions.put(sessionId, "pendingAction", "aftersale.rca_dispatch");
            sessions.put(sessionId, "pendingParams", params);
            return askCandidates(all);
        }
        if (hits.size() > 1) {
            sessions.put(sessionId, "candidates", hits);
            sessions.put(sessionId, "pendingAction", "aftersale.rca_dispatch");
            sessions.put(sessionId, "pendingParams", params);
            return askCandidates(hits);
        }
        return makeProposal(sessionId, "rca_dispatch", hits.get(0), params, userText);
    }

    private static final Map<String, String> DEPT_CN = Map.of(
            "PURCHASE", "采购", "QUALITY", "质检", "DEVICE", "设备", "PRODUCTION", "生产", "COST", "成本");

    private String deptCn(String code) { return DEPT_CN.getOrDefault(code, code); }

    /** 口令/NLU 部门词 → 引擎部门码（保持提及顺序，去重）。 */
    private List<String> parseDepartments(String departments, String userText) {
        String hay = nz(departments) + " " + nz(userText);
        List<String> codes = new ArrayList<>();
        if (hay.contains("采购")) codes.add("PURCHASE");
        if (hay.contains("质检") || hay.contains("质量")) codes.add("QUALITY");
        if (hay.contains("设备")) codes.add("DEVICE");
        if (hay.contains("生产")) codes.add("PRODUCTION");
        if (hay.contains("成本") || hay.contains("财务")) codes.add("COST");
        return codes;
    }

    @SuppressWarnings("unchecked")
    private List<String> departmentsParam(Map<String, Object> params) {
        Object v = params.get("departments");
        return v instanceof List<?> l ? (List<String>) l : List.of();
    }

    private String deptListCn(List<String> codes) {
        if (codes.isEmpty()) return "采购、质检、设备、成本（引擎默认）";
        return String.join("、", codes.stream().map(this::deptCn).toList());
    }

    // ════════════════════════ 设备健康诊断 ════════════════════════
    /** "EQ-003 健康怎么样 / 哪台该保养" → 健康分引擎：单台扣分明细+建议，或全厂最差 TOP。 */
    private Map<String, Object> answerDiagnose(NluResult r) {
        List<Map<String, Object>> list = equipmentService.calcHealthList();   // 已按健康分升序
        if (list.isEmpty()) return ask("暂无设备健康数据。");
        String clue = !blank(r.caseNo()) ? r.caseNo() : r.keyword();
        if (!weakClue(clue)) {
            String c = clue.trim().toLowerCase();
            List<Map<String, Object>> hits = new ArrayList<>();
            for (Map<String, Object> e : list) {
                String code = str(e, "equipmentCode").toLowerCase();
                String name = str(e, "equipmentName").toLowerCase();
                if (code.equals(c)) { hits = List.of(e); break; }
                if ((code.length() >= 2 && (code.contains(c) || c.contains(code)))
                        || (name.length() >= 2 && (name.contains(c) || c.contains(name)))) hits.add(e);
            }
            if (hits.size() == 1) return diagnoseDetail(hits.get(0));
            if (hits.size() > 1) {
                StringBuilder sb = new StringBuilder("匹配到多台设备，请说完整编号：");
                for (int i = 0; i < hits.size() && i < 5; i++) {
                    Map<String, Object> e = hits.get(i);
                    sb.append('\n').append(i + 1).append(". ").append(str(e, "equipmentCode")).append(' ')
                      .append(str(e, "equipmentName")).append(" · 健康分 ").append(str(e, "healthScore"));
                }
                return ask(sb.toString());
            }
            // 线索没对上具体设备 → 落到全厂总览
        }
        return diagnoseFleet(list);
    }

    private Map<String, Object> diagnoseDetail(Map<String, Object> e) {
        StringBuilder sb = new StringBuilder();
        sb.append(str(e, "equipmentCode")).append(" · ").append(str(e, "equipmentName"));
        if (!str(e, "workshop").isBlank()) sb.append("（").append(str(e, "workshop")).append("）");
        sb.append(" 健康诊断：");
        sb.append("\n健康分 ").append(str(e, "healthScore"))
          .append("（").append(healthLevelCn(str(e, "healthLevel"))).append("）· 状态 ")
          .append(blankOr(str(e, "statusCn"), str(e, "status")));
        List<String> deducts = new ArrayList<>();
        if (num(e, "deductRun") > 0)   deducts.add("超时运行 -" + num(e, "deductRun") + "（累计运行 " + str(e, "runHours") + "h）");
        if (num(e, "deductAlarm") > 0) deducts.add("高频报警 -" + num(e, "deductAlarm") + "（近7天 " + str(e, "alarm7d") + " 次）");
        if (num(e, "deductMaint") > 0) deducts.add("逾期保养 -" + num(e, "deductMaint") + "（已 " + str(e, "daysSinceMaint") + " 天未维保）");
        if (num(e, "deductNc") > 0)    deducts.add("关联缺陷 -" + num(e, "deductNc") + "（近30天故障维修 " + str(e, "faultCount30d") + " 次）");
        sb.append(deducts.isEmpty() ? "\n各项指标正常，无扣分项。" : "\n扣分项：" + String.join("；", deducts));
        if (!str(e, "advice").isBlank()) sb.append("\n建议：").append(str(e, "advice"));
        Map<String, Object> data = new LinkedHashMap<>(e);
        data.remove("alarmList7d");
        data.remove("maintList");
        return answer(sb.toString(), data);
    }

    private Map<String, Object> diagnoseFleet(List<Map<String, Object>> list) {
        Map<String, Integer> byLevel = new LinkedHashMap<>();
        for (Map<String, Object> e : list) byLevel.merge(healthLevelCn(str(e, "healthLevel")), 1, Integer::sum);
        StringBuilder sb = new StringBuilder("设备健康总览：共 ").append(list.size()).append(" 台");
        List<String> parts = new ArrayList<>();
        byLevel.forEach((k, n) -> parts.add(k + " " + n));
        sb.append("：").append(String.join("，", parts)).append("。");
        sb.append("\n最需要关注：");
        for (int i = 0; i < list.size() && i < 3; i++) {
            Map<String, Object> e = list.get(i);
            sb.append('\n').append(i + 1).append(". ").append(str(e, "equipmentCode")).append(' ')
              .append(str(e, "equipmentName")).append(" · 健康分 ").append(str(e, "healthScore"))
              .append(" · ").append(worstDeductReason(e));
        }
        sb.append("\n可以说\"EQ-xxx 健康怎么样\"看单台诊断。");
        List<Map<String, Object>> top = new ArrayList<>();
        for (int i = 0; i < list.size() && i < 3; i++) {
            Map<String, Object> slim = new LinkedHashMap<>(list.get(i));
            slim.remove("alarmList7d");
            slim.remove("maintList");
            top.add(slim);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("levelCounts", byLevel);
        data.put("worst", top);
        return answer(sb.toString(), data);
    }

    private String worstDeductReason(Map<String, Object> e) {
        int dr = num(e, "deductRun"), da = num(e, "deductAlarm"), dm = num(e, "deductMaint"), dn = num(e, "deductNc");
        int max = Math.max(Math.max(dr, da), Math.max(dm, dn));
        if (max <= 0) return "整体正常";
        if (max == da) return "近7天报警 " + str(e, "alarm7d") + " 次";
        if (max == dm) return "已 " + str(e, "daysSinceMaint") + " 天未维保";
        if (max == dr) return "累计运行 " + str(e, "runHours") + "h 超时";
        return "近30天故障 " + str(e, "faultCount30d") + " 次";
    }

    private String healthLevelCn(String level) {
        return switch (level) {
            case "GOOD" -> "优良"; case "WARN" -> "需关注"; case "ALERT" -> "预警"; case "DANGER" -> "危险";
            default -> level;
        };
    }

    private int num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    // ════════════════════════ 售后写流程（专用消歧） ════════════════════════
    /** 处理售后"写"意图：定位案例 → 状态校验 → 槽位 → 生成提议。action 为裸码。 */
    private Map<String, Object> startAftersaleWrite(String sessionId, String action, NluResult r, String userText) {
        String requiredStatus = requiredStatus(action);
        boolean excludeClosed = "close".equals(action);

        // 1) 定位案例
        List<Map<String, Object>> hits;
        if (!r.caseNo().isBlank() && r.caseNo().matches("(?i)AS\\d{6,}")) {
            AfterSalesCase c = afterSales.getAfterSalesCaseById(r.caseNo());
            if (c == null) return ask("没找到案例 " + r.caseNo() + "，请确认案例号或说客户名。");
            hits = List.of(caseToRow(c));
        } else {
            hits = findCase(r.customerName(), r.problemTypeCn(), r.keyword(), requiredStatus, excludeClosed);
        }
        if (hits.isEmpty()) {
            // 没匹配上：若还有可操作的案例，直接列出让用户选，而不是干巴巴地拒绝
            List<Map<String, Object>> actionable = actionableCases(action);
            if (actionable.isEmpty()) return ask("当前没有可" + actionCn(action) + "的案例。");
            sessions.put(sessionId, "candidates", actionable);
            sessions.put(sessionId, "pendingAction", "aftersale." + action);
            sessions.put(sessionId, "pendingParams", voiceParams(r));
            return askCandidates(actionable);
        }
        if (hits.size() > 1) {
            sessions.put(sessionId, "candidates", hits);
            sessions.put(sessionId, "pendingAction", "aftersale." + action);
            sessions.put(sessionId, "pendingParams", voiceParams(r));
            return askCandidates(hits);
        }

        Map<String, Object> cv = hits.get(0);
        String caseNo = str(cv, "caseNo");
        String status = str(cv, "caseStatus");

        // 2) 状态机校验
        String guard = statusGuard(action, status);
        if (guard != null) return ask(guard);

        // 3) 槽位（resolve 需 solution）
        Map<String, Object> params = voiceParams(r);
        if ("resolve".equals(action) && blank(str(params, "solution"))) {
            sessions.put(sessionId, "pendingAction", "aftersale.resolve");
            sessions.put(sessionId, "pendingCaseNo", caseNo);
            sessions.put(sessionId, "pendingParams", params);
            return ask(blankOr(r.reply(), "标记解决需要解决方案，请说一下是怎么处理的？"));
        }

        // 4) 生成提议
        return makeProposal(sessionId, action, cv, params, userText);
    }

    /** 多轮续接：候选选择 或 槽位补充。pendingAction 存全码。 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resumePending(String sessionId, String text) {
        Map<String, Object> s = sessions.get(sessionId);
        String pendingAction = (String) s.get("pendingAction");
        if (pendingAction == null) return null;

        // a) 候选选择
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) s.get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            Map<String, Object> params = (Map<String, Object>) s.getOrDefault("pendingParams", new LinkedHashMap<>());

            // —— MES 通用动作候选 ——
            if (!pendingAction.startsWith("aftersale.")) {
                ActionSpec spec = catalog.get(pendingAction);
                if (spec == null) { clearPending(sessionId); return null; }
                Map<String, Object> picked = pickMesCandidate(candidates, spec, text);
                if (picked == null) {
                    if (looksLikeCommand(text)) { abortFlowIfBound(sessionId); clearPending(sessionId); return null; }
                    return ask("没对上，请说编号（如 1）或" + spec.entityNoun + "关键词。");
                }
                s.remove("candidates");
                if (!spec.actionable.test(picked)) {
                    clearPending(sessionId);
                    return ask(str(picked, "id") + " 当前是「" + str(picked, "status") + "」，不能" + spec.nameCn + "。");
                }
                return makeMesProposal(sessionId, spec, picked, params, text);
            }

            // —— 售后候选 ——
            String sub = pendingAction.substring("aftersale.".length());
            Map<String, Object> picked = pickCandidate(candidates, text);
            if (picked == null) {
                if (looksLikeCommand(text)) { abortFlowIfBound(sessionId); clearPending(sessionId); return null; }
                return ask("没对上，请说编号（如 1）或客户名。");
            }
            s.remove("candidates");
            String status = str(picked, "caseStatus");
            String guard = statusGuard(sub, status);
            if (guard != null) { clearPending(sessionId); return ask(guard); }
            if ("resolve".equals(sub) && blank(str(params, "solution"))) {
                s.put("pendingCaseNo", str(picked, "caseNo"));
                return ask("请说一下 " + str(picked, "customerName") + " 这单的解决方案？");
            }
            return makeProposal(sessionId, sub, picked, params, text);
        }

        // b) 槽位补充（当前只有售后 resolve 的 solution）
        if ("aftersale.resolve".equals(pendingAction) && s.get("pendingCaseNo") != null) {
            // 若这句像新指令（含明确动作关键词）则不当作方案，走正常解析
            if (looksLikeCommand(text)) { abortFlowIfBound(sessionId); clearPending(sessionId); return null; }
            String caseNo = (String) s.get("pendingCaseNo");
            AfterSalesCase c = afterSales.getAfterSalesCaseById(caseNo);
            if (c == null) { clearPending(sessionId); return ask("案例 " + caseNo + " 不存在了。"); }
            Map<String, Object> params = (Map<String, Object>) s.getOrDefault("pendingParams", new LinkedHashMap<>());
            params.put("solution", text.trim());
            return makeProposal(sessionId, "resolve", caseToRow(c), params, text);
        }

        // c) 通知目标模块补充（"要通知哪个模块？" → "生产"）
        if ("notify.send".equals(pendingAction)) {
            String target = moduleFromWord(text);
            if (blank(target)) {
                if (looksLikeCommand(text)) { abortFlowIfBound(sessionId); clearPending(sessionId); return null; }
                return ask("没听出目标模块，请说：生产 / 采购 / 质检 / 设备 / 仓储 / 订单 / 售后 / 成本 / 系统。");
            }
            Map<String, Object> params = (Map<String, Object>) s.getOrDefault("pendingParams", new LinkedHashMap<>());
            return makeNotifyProposal(sessionId, target, params, text);
        }
        return null;
    }

    private Map<String, Object> pickMesCandidate(List<Map<String, Object>> candidates, ActionSpec spec, String text) {
        int idx = parseIndex(text);
        if (idx >= 1 && idx <= candidates.size()) return candidates.get(idx - 1);
        List<Map<String, Object>> hits = matchRows(candidates, spec, text);
        return hits.size() == 1 ? hits.get(0) : null;
    }

    // ════════════════════════ 执行（闸门） ════════════════════════
    public Map<String, Object> execute(String proposalId, String decision, Object finalParams,
                                       String operator, String roleKey) {
        Proposal p = proposals.get(proposalId);
        if (p == null) throw new BusinessException("提议不存在或已过期，请重新说一次指令");
        if (!"PROPOSED".equals(p.status)) throw new BusinessException("该提议已处理（" + p.status + "）");
        Flow flow = p.flowId != null ? flows.get(p.flowId) : null;

        if ("SKIP".equalsIgnoreCase(decision)) {
            p.status = "SKIPPED";
            auditDecision(p, "SKIP", operator, null, null);
            if (flow != null) {
                abortFlow(flow);
                return Map.of("ok", false, "reply",
                        "已取消。该多步流程剩余 " + (flow.total - flow.idx) + " 步一并终止，已完成的步骤保持有效。");
            }
            return Map.of("ok", false, "reply", "已取消，未执行。");
        }
        if ("MODIFY".equalsIgnoreCase(decision) && finalParams instanceof Map<?, ?> fp) {
            fp.forEach((k, v) -> { if (v != null) p.params.put(String.valueOf(k), v); });
        }

        try {
            String reply;
            Map<String, Object> result;
            if (p.specCode != null) {
                ActionSpec spec = catalog.get(p.specCode);
                MesActionRequest req = new MesActionRequest();
                req.setAction(spec.mesAction);
                req.setPayload(spec.payloadBuilder.apply(p.entityRow, p.params));
                req.setOperator(operator);
                req.setRoleKey(blank(roleKey) ? "system" : roleKey);
                mesWorkflow.execute(req);
                reply = "已" + spec.nameCn + " " + p.entityNo + "。";
                result = Map.of("entityNo", p.entityNo, "module", spec.module);
            } else if ("notify.send".equals(p.action)) {
                String target = str(p.params, "targetModule");
                String content = blankOr(str(p.params, "content"), p.userText);
                Map<String, Object> notice = appendNotice(target, content, str(p.params, "sourceModule"), operator);
                reply = "已把协办通知发给" + MODULE_CN.getOrDefault(target, target) + "模块：「" + content
                        + "」。对方将在通知中心收到（仅通知，不改动对方业务数据）。";
                result = Map.of("noticeId", str(notice, "id"), "targetModule", target);
            } else if ("order.create".equals(p.action)) {
                MesActionRequest req = new MesActionRequest();
                req.setAction("createOrder");
                Map<String, Object> payload = new LinkedHashMap<>(p.params);
                if (payload.containsKey("remark") && !payload.containsKey("deliveryDate")) {
                    // 确认卡上补充的备注若像日期就当交期用
                    String rmk = str(payload, "remark");
                    if (rmk.matches("\\d{4}-\\d{2}-\\d{2}")) payload.put("deliveryDate", rmk);
                }
                req.setPayload(payload);
                req.setOperator(operator);
                req.setRoleKey(blank(roleKey) ? "system" : roleKey);
                Object created = mesWorkflow.execute(req);
                String orderNo = created instanceof Map<?, ?> cm && cm.get("id") != null ? String.valueOf(cm.get("id")) : "";
                reply = "已创建客户订单" + (blank(orderNo) || "null".equals(orderNo) ? "" : " " + orderNo)
                        + "：" + str(p.params, "customerName") + " · " + str(p.params, "productModel")
                        + " · " + p.params.get("quantity") + " 台，已进入订单审核流程。";
                result = Map.of("orderNo", blankOr(orderNo, ""), "module", "order");
            } else if ("rca_dispatch".equals(p.action)) {
                // 派单前确保内存里有分析快照，任务文案才带得上引擎建议；分析失败不阻断派单
                try { afterSales.buildRcaAnalysis(p.entityNo, false); } catch (Exception ignore) { }
                List<String> departments = departmentsParam(p.params);
                Map<String, Object> receipt = afterSales.dispatchRcaTasks(p.entityNo, departments);
                int n = num(receipt, "taskCount");
                reply = "已为 " + p.entityNo + " 启动跨部门根因协查，派出 " + n + " 个任务（"
                        + deptListCn(departments) + "）。各部门将在协同工作台收到通知。";
                result = Map.of("caseNo", p.entityNo, "taskCount", n);
            } else {
                AfterSalesCase updated = dispatchAftersale(p, operator);
                reply = "已" + actionCn(p.action) + " " + p.entityNo + "，状态：" + statusCn(updated.getCaseStatus());
                result = Map.of("caseNo", p.entityNo, "caseStatus", updated.getCaseStatus());
            }
            p.status = "EXECUTED";
            auditDecision(p, "MODIFY".equalsIgnoreCase(decision) ? "MODIFY" : "APPROVE", operator, result, null);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("reply", reply);
            out.put("data", result);
            if (flow != null) {                    // 多步流：本步完成，自动推进（读步直接过，写步弹下一张确认卡）
                flow.idx++;
                out.put("next", advanceFlow(flow, new StringBuilder()));
            }
            return out;
        } catch (BusinessException e) {
            p.status = "FAILED";
            auditDecision(p, "APPROVE", operator, null, e.getMessage());
            String reply = "执行失败：" + e.getMessage();
            if (flow != null) {
                abortFlow(flow);
                reply += "\n⛔ 多步流程终止，已完成的步骤保持有效。";
            }
            return Map.of("ok", false, "reply", reply);
        }
    }

    private AfterSalesCase dispatchAftersale(Proposal p, String operator) {
        return switch (p.action) {
            case "accept" -> afterSales.acceptCase(p.entityNo, operator);
            case "resolve" -> afterSales.resolveCase(p.entityNo, str(p.params, "solution"),
                    str(p.params, "traceResult"), operator);
            case "close" -> afterSales.closeCase(p.entityNo, str(p.params, "remark"), operator);
            default -> throw new BusinessException("未知动作：" + p.action);
        };
    }

    // ════════════════════════ 提议 + 审计（售后） ════════════════════════
    private Map<String, Object> makeProposal(String sessionId, String action, Map<String, Object> cv,
                                             Map<String, Object> params, String userText) {
        clearPending(sessionId);
        Proposal p = new Proposal();
        p.id = String.valueOf(proposalSeq.incrementAndGet());
        p.action = action;
        p.entityNo = str(cv, "caseNo");
        p.params = params;
        p.userText = userText;
        p.humanReadable = humanReadable(action, cv, params);
        bindFlow(sessionId, p);
        proposals.put(p.id, p);
        setCurrent(sessionId, p.entityNo, str(cv, "customerName"));
        auditPropose(p, userText, null);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("type", "confirm");
        resp.put("proposalId", p.id);
        resp.put("action", "aftersale." + action);
        resp.put("caseNo", p.entityNo);
        resp.put("humanReadable", p.humanReadable);
        if ("resolve".equals(action)) {
            resp.put("editable", Map.of("key", "solution", "placeholder", "可修改解决方案（选填）"));
        } else if ("close".equals(action)) {
            resp.put("editable", Map.of("key", "remark", "placeholder", "可补充关闭备注（选填）"));
        }
        resp.put("reply", "帮你定位到 " + str(cv, "customerName") + " 的案例，" + actionCn(action) + "吗？");
        return resp;
    }

    private void auditPropose(Proposal p, String userText, ActionSpec spec) {
        try {
            // 多步流：各步挂到 startFlow 建的同一条 run 下；单步指令自建一条 run
            Long runId = null;
            int stepNo = 1;
            if (p.flowId != null) {
                Flow f = flows.get(p.flowId);
                if (f != null && f.auditRunId != null) { runId = f.auditRunId; stepNo = f.idx + 1; }
            }
            if (runId == null) {
                Map<String, Object> run = new LinkedHashMap<>();
                run.put("flowNo", "ASV" + LocalDateTime.now().format(FMT) + p.id);
                run.put("templateCode", spec != null ? "MES_VOICE" : "AFTERSALE_VOICE");
                run.put("goal", userText);
                run.put("status", "PAUSED");
                run.put("currentStepNo", 1);
                run.put("contextJson", null);
                run.put("createdBy", null);
                agentFlow.insertRun(run);
                runId = asLong(run.get("flowId"));
            }

            String module = spec != null ? spec.module
                    : ("notify.send".equals(p.action) ? "NOTIFY" : "AFTERSALES");
            String actionCode = spec != null ? spec.code
                    : ("notify.send".equals(p.action) ? "notify.send" : "aftersale." + p.action);
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("flowId", runId);
            step.put("stepNo", stepNo);
            step.put("module", module);
            step.put("actionCode", actionCode);
            step.put("title", p.humanReadable);
            step.put("status", "PROPOSED");
            step.put("reason", "语音指令：" + userText);
            step.put("proposalJson", toJson(p.params));
            agentFlow.insertStep(step);
            p.auditStepId = asLong(step.get("stepId"));
        } catch (Exception e) {
            log.warn("[Assistant] 审计落库跳过（agent_flow 表可能未建）：{}", e.getMessage());
        }
    }

    private void auditDecision(Proposal p, String decision, String operator,
                               Map<String, Object> result, String errorMsg) {
        if (p.auditStepId == null) return;
        try {
            Map<String, Object> up = new LinkedHashMap<>();
            up.put("stepId", p.auditStepId);
            up.put("status", p.status);
            up.put("decision", decision);
            up.put("decisionBy", null);
            up.put("finalParamsJson", toJson(p.params));
            up.put("resultJson", result == null ? null : toJson(result));
            up.put("errorMsg", errorMsg);
            agentFlow.updateStepDecision(up);
        } catch (Exception e) {
            log.warn("[Assistant] 审计更新跳过：{}", e.getMessage());
        }
    }

    // ════════════════════════ 售后实体消歧 ════════════════════════
    /** 按客户名/问题类型/关键词模糊定位候选案例；requiredStatus 非空时按状态过滤；excludeClosed 时排除已关闭。 */
    public List<Map<String, Object>> findCase(String customerName, String problemTypeCn,
                                              String keyword, String requiredStatus, boolean excludeClosed) {
        List<Map<String, Object>> views = afterSales.listCaseViews();
        List<Map<String, Object>> pool = new ArrayList<>();
        for (Map<String, Object> cv : views) {
            String st = str(cv, "caseStatus");
            if (requiredStatus != null && !requiredStatus.equals(st)) continue;
            if (excludeClosed && "CLOSED".equals(st)) continue;
            pool.add(cv);
        }
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> cv : pool) {
            int score = score(cv, customerName, problemTypeCn, keyword);
            if (score > 0) { cv.put("_score", score); scored.add(cv); }
        }
        // 有线索但一个没匹配上 → 空；线索基本为空且候选唯一 → 直接给这一条
        if (scored.isEmpty()) {
            boolean noClue = blank(customerName) && blank(problemTypeCn) && weakClue(keyword);
            if (noClue && pool.size() == 1) return pool;
            return List.of();
        }
        scored.sort((a, b) -> Integer.compare((int) b.get("_score"), (int) a.get("_score")));
        // 最高分明显领先 → 只取第一条
        if (scored.size() == 1) return scored;
        int top = (int) scored.get(0).get("_score");
        int second = (int) scored.get(1).get("_score");
        if (top - second >= 30) return List.of(scored.get(0));
        return scored;
    }

    private int score(Map<String, Object> cv, String customerName, String problemTypeCn, String keyword) {
        String cn = str(cv, "customerName");
        String ptype = str(cv, "problemTypeCn");
        String desc = str(cv, "problemDescription");
        String batch = str(cv, "batchNo");
        int score = 0;
        if (!blank(customerName) && cn.contains(customerName)) score += 40;
        if (!blank(problemTypeCn) && problemTypeCn.equals(ptype)) score += 30;
        if (!blank(keyword)) {
            if (customerHitInText(cn, keyword)) score += 40;
            String hay = (cn + desc + batch).toLowerCase();
            if (keyword.length() >= 2 && hay.contains(keyword.toLowerCase())) score += 10;
        }
        return score;
    }

    /** keyword(整句)里是否出现客户名的显著片段（2~4字滑窗，跳过通用词） */
    private boolean customerHitInText(String cn, String keyword) {
        if (blank(cn) || blank(keyword)) return false;
        List<String> stop = List.of("科技", "公司", "有限", "电子", "采购", "中心", "集团", "股份", "显示器", "办公");
        for (int len = 4; len >= 2; len--) {
            for (int i = 0; i + len <= cn.length(); i++) {
                String win = cn.substring(i, i + len);
                if (stop.contains(win)) continue;
                if (keyword.contains(win)) return true;
            }
        }
        return false;
    }

    private Map<String, Object> pickCandidate(List<Map<String, Object>> candidates, String text) {
        // 编号：1/2/3、一/二/三、①②③、第一/第二
        int idx = parseIndex(text);
        if (idx >= 1 && idx <= candidates.size()) return candidates.get(idx - 1);
        // 客户名片段
        for (Map<String, Object> cv : candidates) {
            if (customerHitInText(str(cv, "customerName"), text)) return cv;
        }
        return null;
    }

    private int parseIndex(String t) {
        t = t.trim();
        Map<String, Integer> word = Map.of("一", 1, "二", 2, "三", 3, "四", 4,
                "①", 1, "②", 2, "③", 3, "④", 4);
        for (Map.Entry<String, Integer> e : word.entrySet()) if (t.contains(e.getKey())) return e.getValue();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(t);
        if (m.find()) try { return Integer.parseInt(m.group(1)); } catch (Exception ignore) {}
        return -1;
    }

    // ════════════════════════ 售后读操作应答 ════════════════════════
    private Map<String, Object> answerKpi() {
        Map<String, Object> kpi = afterSales.caseKpi();
        String reply = String.format("售后概况：共 %s 条，待受理 %s、处理中 %s、已解决 %s、已关闭 %s。",
                v(kpi, "total"), v(kpi, "open"), v(kpi, "processing"), v(kpi, "resolved"), v(kpi, "closed"));
        return answer(reply, kpi);
    }

    private Map<String, Object> answerTrace(String sessionId, NluResult r) {
        String caseNo = resolveReadCase(sessionId, r);
        if (caseNo == null) {
            List<Map<String, Object>> hits = locateForRead(r);
            if (hits.isEmpty()) return ask("要查哪个案例的追溯？请说客户名或案例号。");
            return askCandidates(hits);
        }
        Map<String, Object> d = afterSales.getTraceDetail(caseNo);
        if (d == null || d.isEmpty()) return ask("没查到 " + caseNo + " 的追溯信息。");
        setCurrent(sessionId, caseNo, str(d, "customerName"));
        StringBuilder sb = new StringBuilder();
        sb.append(caseNo).append(" · ").append(str(d, "customerName"))
          .append("\n问题：").append(str(d, "problemTypeCn")).append(" · ").append(statusCn(str(d, "caseStatus")));
        Object chain = d.get("traceChain");
        if (chain instanceof List<?> steps && !steps.isEmpty()) {
            sb.append("\n追溯链（从后往前）：");
            for (Object s : steps) {
                if (s instanceof Map<?, ?> m) {
                    if (Boolean.TRUE.equals(m.get("missing"))) continue;   // 未记录的环节不播报
                    String desc = nz(m.get("desc"));
                    sb.append("\n· ").append(nz(m.get("title"))).append(" ").append(nz(m.get("no")))
                      .append(desc.isBlank() ? "" : "（" + desc + "）");
                    if (m.get("people") instanceof List<?> ps && !ps.isEmpty()) {
                        List<String> names = new ArrayList<>();
                        for (Object p : ps) {
                            if (p instanceof Map<?, ?> pm) names.add(nz(pm.get("role")) + " " + nz(pm.get("name")));
                        }
                        sb.append(" — ").append(String.join("、", names));
                    }
                }
            }
        }
        return answer(sb.toString(), d);
    }

    private Map<String, Object> answerDetail(String sessionId, NluResult r) {
        String caseNo = resolveReadCase(sessionId, r);
        if (caseNo == null) {
            List<Map<String, Object>> hits = locateForRead(r);
            if (hits.isEmpty()) return ask("你指的是哪个案例？请说客户名或案例号。");
            if (hits.size() > 1) return askCandidates(hits);
            caseNo = str(hits.get(0), "caseNo");
        }
        AfterSalesCase c = afterSales.getAfterSalesCaseById(caseNo);
        if (c == null) return ask("案例 " + caseNo + " 不存在。");
        setCurrent(sessionId, caseNo, c.getCustomerName());
        StringBuilder sb = new StringBuilder();
        sb.append(caseNo).append(" · ").append(nz(c.getCustomerName()))
          .append("\n类型：").append(problemTypeCn(c.getProblemType()))
          .append(" · 状态：").append(statusCn(c.getCaseStatus()));
        if (!blank(c.getProblemDescription())) sb.append("\n问题描述：").append(c.getProblemDescription());
        if (!blank(c.getHandleResult())) sb.append("\n处理结果：").append(c.getHandleResult());
        if (!blank(c.getTraceResult())) sb.append("\n追溯结论：").append(c.getTraceResult());
        return answer(sb.toString(), caseToRow(c));
    }

    /** 读操作定位 caseNo：显式号 > 唯一模糊命中 > 上下文 currentCase；定位不了返回 null */
    private String resolveReadCase(String sessionId, NluResult r) {
        if (!blank(r.caseNo()) && r.caseNo().matches("(?i)AS\\d{6,}")) return r.caseNo();
        List<Map<String, Object>> hits = locateForRead(r);
        if (hits.size() == 1) return str(hits.get(0), "caseNo");
        if (hits.isEmpty()) {
            Object cur = sessions.get(sessionId, "currentCaseNo");
            if (cur != null) return cur.toString();
        }
        return null;
    }

    private List<Map<String, Object>> locateForRead(NluResult r) {
        if (blank(r.customerName()) && blank(r.problemTypeCn()) && weakClue(r.keyword())) return List.of();
        return findCase(r.customerName(), r.problemTypeCn(), r.keyword(), null, false);
    }

    private void setCurrent(String sessionId, String caseNo, String customer) {
        sessions.put(sessionId, "currentCaseNo", caseNo);
        sessions.put(sessionId, "currentCustomer", customer);
    }

    private Map<String, Object> answer(String reply, Object data) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("type", "answer");
        resp.put("reply", reply);
        resp.put("data", data);
        return resp;
    }

    private String nz(Object o) { return o == null ? "" : o.toString(); }

    /** 某售后动作当前所有可操作的案例（accept→OPEN，resolve→PROCESSING，close→非 CLOSED） */
    private List<Map<String, Object>> actionableCases(String action) {
        String rs = requiredStatus(action);
        boolean exCl = "close".equals(action);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> cv : afterSales.listCaseViews()) {
            String st = str(cv, "caseStatus");
            if (rs != null && !rs.equals(st)) continue;
            if (exCl && "CLOSED".equals(st)) continue;
            out.add(cv);
        }
        return out;
    }

    // ════════════════════════ 工具 ════════════════════════
    private String requiredStatus(String action) {
        return switch (action) {
            case "accept" -> "OPEN";
            case "resolve" -> "PROCESSING";
            default -> null;                        // close：任意非 CLOSED
        };
    }

    private String statusGuard(String action, String status) {
        return switch (action) {
            case "accept" -> "OPEN".equals(status) ? null
                    : "该案例当前是「" + statusCn(status) + "」，只有待受理的才能受理。";
            case "resolve" -> "PROCESSING".equals(status) ? null
                    : "该案例当前是「" + statusCn(status) + "」，只有处理中的才能标记解决。";
            case "close" -> "CLOSED".equals(status) ? "该案例已经关闭了。" : null;
            default -> null;
        };
    }

    private String humanReadable(String action, Map<String, Object> cv, Map<String, Object> params) {
        String base = "我将【" + actionCn(action) + "】" + str(cv, "caseNo")
                + " · " + str(cv, "customerName")
                + " · " + str(cv, "problemTypeCn")
                + " · 当前" + statusCn(str(cv, "caseStatus"));
        if ("resolve".equals(action) && !blank(str(params, "solution")))
            base += "\n方案：" + str(params, "solution");
        if ("close".equals(action) && !blank(str(params, "remark")))
            base += "\n备注：" + str(params, "remark");
        if ("rca_dispatch".equals(action))
            base += "\n派单部门：" + deptListCn(departmentsParam(params)) + "，各部门生成协查任务并通知";
        return base + "。确认?";
    }

    private Map<String, Object> caseToRow(AfterSalesCase c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("caseNo", c.getCaseNo());
        m.put("customerName", c.getCustomerName());
        m.put("caseStatus", c.getCaseStatus());
        m.put("problemType", c.getProblemType());
        m.put("problemTypeCn", problemTypeCn(c.getProblemType()));
        m.put("problemDescription", c.getProblemDescription());
        m.put("batchNo", c.getBatchNo());
        return m;
    }

    private Map<String, Object> ask(String reply) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "ask");
        m.put("reply", reply);
        return m;
    }

    private Map<String, Object> askCandidates(List<Map<String, Object>> hits) {
        StringBuilder sb = new StringBuilder("找到多条，请说编号：");
        for (int i = 0; i < hits.size() && i < 5; i++) {
            Map<String, Object> cv = hits.get(i);
            sb.append("\n").append(i + 1).append(". ")
              .append(str(cv, "customerName")).append(" · ")
              .append(str(cv, "problemTypeCn")).append(" · ")
              .append(statusCn(str(cv, "caseStatus")));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "ask");
        m.put("reply", sb.toString());
        m.put("data", hits);
        return m;
    }

    private void clearPending(String sessionId) {
        Map<String, Object> s = sessions.get(sessionId);
        s.remove("pendingAction");
        s.remove("pendingCaseNo");
        s.remove("pendingParams");
        s.remove("candidates");
    }

    private String contextJson(String sessionId) {
        Map<String, Object> s = sessions.get(sessionId);
        Map<String, Object> ctx = new LinkedHashMap<>();
        if (s.get("currentModule") != null) ctx.put("currentModule", s.get("currentModule"));
        if (s.get("pendingAction") != null) ctx.put("pendingAction", s.get("pendingAction"));
        if (s.get("currentCaseNo") != null) {
            Map<String, Object> cur = new LinkedHashMap<>();
            cur.put("caseNo", s.get("currentCaseNo"));
            cur.put("customer", s.get("currentCustomer"));
            ctx.put("currentCase", cur);
        }
        return toJson(ctx);
    }

    private boolean looksLikeCommand(String t) {
        return t.contains("受理") || t.contains("关闭") || t.contains("结案")
                || t.contains("列表") || t.contains("KPI") || t.contains("kpi") || t.contains("追溯")
                || t.contains("报警") || t.contains("派工") || t.contains("开工")
                || t.contains("到货") || t.contains("入库") || t.contains("概况")
                || t.contains("协查") || t.contains("诊断");
    }

    private boolean weakClue(String k) {
        return k == null || k.trim().length() < 2;
    }

    private String toJson(Object o) {
        try { return objectMapper.writeValueAsString(o == null ? Map.of() : o); }
        catch (Exception e) { return "{}"; }
    }

    private Long asLong(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static String blankOr(String s, String fb) { return blank(s) ? fb : s; }
    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : "";
    }
    private static String v(Map<String, Object> m, String k) {
        Object x = m.get(k); return x == null ? "0" : x.toString();
    }

    private String actionCn(String action) {
        return switch (action) {
            case "accept" -> "受理";
            case "resolve" -> "标记解决";
            case "close" -> "关闭";
            case "rca_dispatch" -> "启动根因协查";
            case "query_list" -> "查询";
            case "query_trace" -> "查追溯";
            default -> "处理";
        };
    }

    private String statusCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "OPEN" -> "待受理";
            case "PROCESSING" -> "处理中";
            case "RESOLVED" -> "已解决";
            case "CLOSED" -> "已关闭";
            default -> s;
        };
    }

    private String problemTypeCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "DISPLAY_DEFECT" -> "显示缺陷";
            case "COLOR_ISSUE" -> "色彩问题";
            case "DEAD_PIXEL" -> "坏点亮点";
            case "INTERFACE_FAULT" -> "接口故障";
            case "APPEARANCE" -> "外观损伤";
            case "POWER_ISSUE" -> "电源问题";
            case "OTHER" -> "其他";
            default -> s;
        };
    }
}
