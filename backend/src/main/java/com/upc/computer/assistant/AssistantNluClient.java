package com.upc.computer.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.config.AiProperties;
import com.upc.computer.config.AssistantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自然语言理解：把用户中文口令解析为 {@link NluResult}，动作码覆盖全 MES 模块。
 * 动作枚举段由 {@link MesActionCatalog#nluCatalogText()} 动态生成，新注册动作自动进入 prompt。
 * 一句话串联多个动作（"受理…并启动协查""关闭报警然后通知生产"）时输出 steps 数组，由编排层逐步闸门执行。
 * 优先调用百炼文本模型（qwen-plus，复用 ai.api-key/ai.base-url）；调用异常或 assistant.nlu.mock=true
 * 时回退规则解析，保证离线也能跑。实体消歧（案例/报警/派工等定位）交给 AssistantService。
 */
@Component
public class AssistantNluClient {

    private static final Logger log = LoggerFactory.getLogger(AssistantNluClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** 业务单号：AS202607002 一类的连写号，或 ALM-2026-001 / PO-2026-003 一类的连字符号 */
    private static final Pattern ENTITY_NO = Pattern.compile("(?i)\\b([A-Z]{2,6}(?:-\\d+)+|[A-Z]{2}\\d{6,})\\b");
    /** 多步指令切分：然后/接着/并且/分号，以及"并+动作动词"（避免误切"合并"之类） */
    private static final Pattern STEP_SPLIT = Pattern.compile(
            "然后|接着|并且|；|;|，并|,并|并(?=启动|通知|提醒|告诉|派|接收|解除|关闭|开始|受理|标记|确认)");

    /** 老版售后动作码（模型可能仍输出裸码）→ 统一前缀 */
    private static final Set<String> LEGACY_AFTERSALE = Set.of(
            "query_list", "query_trace", "query_detail", "accept", "resolve", "close");

    private static final String SYSTEM_TMPL = """
        你是显示器制造 MES 的全局语音指令解析器，覆盖售后、设备安灯、生产派工、采购、仓储、质量、订单等模块。
        只能解析为下列动作码之一，禁止编造数据。
        动作枚举（动作码  说明与触发例句）：
        %s
        上下文 context 里有 currentModule（用户当前所在页面模块），意图含糊时优先归到该模块的动作；
        context 里可能还有 currentCase（刚提到的售后案例），用户说"它/这单/这个"时指的就是它。
        输出严格 JSON（不要 Markdown、不要解释），字段：
        {
          "action": "上述动作码之一",
          "caseNo": "用户报的案例号或业务单号（如 AS202607002、ALM-2026-001、PO-2026-003），否则空串",
          "caseClue": {"customerName":"客户名片段(售后用)", "problemType":"问题类型词(售后用)", "keyword":"其它定位关键词（设备名/工序/物料/供应商等）"},
          "params": {"solution":"", "traceResult":"", "remark":"", "qty":0, "departments":"", "targetModule":""},
          "steps": [],
          "missingSlots": ["aftersale.resolve 缺 solution 时填 'solution'，否则空数组"],
          "confidence": 0.0,
          "reply": "给用户的一句自然中文回应"
        }
        规则：只要用户表达了写操作意图（受理/解决/关闭/接收/解除/开工/到货/入库/派任务等），即使没指定具体单号也要归到对应动作码，由系统列出候选；
        "接收"类意图按宾语区分：报警/安灯→device.alarm_receive，派工/任务→production.dispatch_accept，都没说则按 currentModule 判断；
        问"怎么处理/怎么办/给建议"→aftersale.advise；问"为什么/怎么来的/查追溯"→aftersale.query_trace；说"启动协查/派协同任务"→aftersale.rca_dispatch；
        问设备"健康/该保养吗/状态怎么样"→device.diagnose（问的是数量/概况才用 device.overview）；
        问库存明细/某物料有多少/查库存/库存查询→warehouse.query_inventory（caseClue.keyword 填物料名或 MAT 编码片段；只说"查库存"则 keyword 留空列出主要库存）；
        说"下单/下订单/新建订单/订购…台…"→order.create：caseClue.customerName 填客户名，caseClue.keyword 填产品型号（如 27寸4K显示器），params.qty 填台数，params.remark 填交期日期，缺什么就留空由系统追问；
        说"通知/告诉/提醒某模块…"→notify.send，params.targetModule 填目标模块码（aftersale/device/production/purchase/warehouse/quality/order/cost/system），params.remark 填要转达的内容原文；
        一句话串联多个动作时（"受理星辰这单并启动根因协查""关闭EQ-003的报警，然后通知生产暂缓派工"），把每个动作按顺序拆进 steps（2~3 项，每项含 action/caseNo/caseClue/params，不再嵌套 steps），顶层 action 填第一步，reply 概括整个流程；单动作时 steps 留空数组；
        qty 只在采购到货说了数量时抽取（"到了500片"→500）；departments 只在 rca_dispatch 提到部门时填（采购/质检/设备/生产/成本，逗号分隔）；
        missingSlots 只在 aftersale.resolve 缺 solution 时填 ['solution'] 并在 reply 追问，其余一律空数组。
        """;

    private final AiProperties aiProps;
    private final AssistantProperties props;
    private final MesActionCatalog catalog;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public AssistantNluClient(AiProperties aiProps, AssistantProperties props, MesActionCatalog catalog) {
        this.aiProps = aiProps;
        this.props = props;
        this.catalog = catalog;
    }

    public NluResult interpret(String userText, String contextJson) {
        if (userText == null || userText.isBlank()) {
            return new NluResult("unknown", "", "", "", "", "", "", "", 0, "", "",
                    List.of(), List.of(), 0.0, "没听清，请再说一遍。");
        }
        String apiKey = firstNonBlank(props.getNlu().getApiKey(), aiProps.getApiKey());
        if (props.getNlu().isMock() || apiKey == null || apiKey.isBlank()) {
            return ruleParse(userText, contextJson);
        }
        try {
            return callModel(userText, contextJson == null ? "{}" : contextJson, apiKey);
        } catch (Exception e) {
            log.warn("[NLU] 大模型解析失败，回退规则解析：{}", e.getMessage());
            return ruleParse(userText, contextJson);
        }
    }

    /** 老版裸售后动作码 → aftersale.* 前缀，其余原样。 */
    public static String normalizeAction(String action) {
        if (action == null || action.isBlank()) return "unknown";
        return LEGACY_AFTERSALE.contains(action) ? "aftersale." + action : action;
    }

    // ── 百炼 qwen-plus 解析 ───────────────────────────────────────
    private NluResult callModel(String userText, String contextJson, String apiKey) throws Exception {
        String baseUrl = firstNonBlank(props.getNlu().getBaseUrl(), aiProps.getBaseUrl());
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_TMPL.formatted(catalog.nluCatalogText())),
                Map.of("role", "user", "content", "context:" + contextJson + "\n用户说:" + userText));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getNlu().getModel());
        body.put("temperature", 0.1);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", messages);

        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + resp.statusCode());
        String respBody = new String(resp.body(), StandardCharsets.UTF_8);
        JsonNode content = MAPPER.readTree(respBody).path("choices").path(0).path("message").path("content");
        if (!content.isTextual()) throw new IllegalStateException("模型返回内容为空");
        return fromJson(MAPPER.readTree(content.asText()), userText);
    }

    private NluResult fromJson(JsonNode root, String userText) {
        List<NluResult> steps = new ArrayList<>();
        JsonNode stepsNode = root.path("steps");
        if (stepsNode.isArray() && stepsNode.size() >= 2) {
            stepsNode.forEach(n -> steps.add(parseOne(n, userText, List.of())));
        }
        return parseOne(root, userText, steps);
    }

    /** 单条指令 JSON → NluResult；steps 由调用方传入（子指令固定空） */
    private NluResult parseOne(JsonNode root, String userText, List<NluResult> steps) {
        JsonNode clue = root.path("caseClue");
        JsonNode params = root.path("params");
        List<String> missing = new ArrayList<>();
        root.path("missingSlots").forEach(n -> { if (n.isTextual() && !n.asText().isBlank()) missing.add(n.asText()); });
        String caseNo = root.path("caseNo").asText("");
        if (caseNo.isBlank() && steps.isEmpty()) caseNo = extractEntityNo(userText);
        return new NluResult(
                normalizeAction(root.path("action").asText("unknown")),
                caseNo,
                clue.path("customerName").asText(""),
                clue.path("problemType").asText(""),
                clue.path("keyword").asText(""),
                params.path("solution").asText(""),
                params.path("traceResult").asText(""),
                params.path("remark").asText(""),
                params.path("qty").asInt(0),
                params.path("departments").asText(""),
                normalizeModuleWord(params.path("targetModule").asText("")),
                steps,
                missing,
                root.path("confidence").asDouble(0.7),
                root.path("reply").asText(""));
    }

    // ── 规则兜底解析（离线/降级） ──────────────────────────────────
    private NluResult ruleParse(String text, String contextJson) {
        String t = text.trim();
        // 多步：切分后每段独立解析，≥2 段有效则输出 steps
        String[] segs = STEP_SPLIT.split(t);
        if (segs.length >= 2) {
            List<NluResult> steps = new ArrayList<>();
            String module = extractContextModule(contextJson);
            for (String seg : segs) {
                String s = seg.trim();
                if (s.length() < 2) continue;
                NluResult one = ruleParseSingle(s, module);
                if (!"unknown".equals(one.action())) steps.add(one);
            }
            if (steps.size() >= 2) {
                NluResult first = steps.get(0);
                return new NluResult(first.action(), first.caseNo(), first.customerName(), first.problemTypeCn(),
                        first.keyword(), first.solution(), first.traceResult(), first.remark(), first.qty(),
                        first.departments(), first.targetModule(), steps, first.missingSlots(), 0.6,
                        "收到，共 " + steps.size() + " 步指令。");
            }
        }
        return ruleParseSingle(t, extractContextModule(contextJson));
    }

    private NluResult ruleParseSingle(String t, String module) {
        String action = detectAction(t, module);
        String caseNo = extractEntityNo(t);
        String problemTypeCn = detectProblemType(t);
        String keyword = stripFillers(t);
        String solution = "";
        String remark = "";
        String targetModule = "";
        int qty = extractQty(t);
        List<String> missing = new ArrayList<>();
        String reply;

        if ("notify.send".equals(action)) {
            String[] tm = extractNotify(t);
            targetModule = tm[0];
            remark = tm[1];
            reply = "收到，帮你发协办通知。";
        } else if ("aftersale.resolve".equals(action)) {
            solution = extractSolution(t);
            if (solution.isBlank()) { missing.add("solution"); reply = "标记解决需要解决方案，请说一下是怎么处理的？"; }
            else reply = "好的，帮你标记解决并确认方案。";
        } else if ("unknown".equals(action)) {
            reply = "我能处理售后受理/解决/关闭、研判建议、根因协查，接收/解除报警、设备诊断，接收派工/开工、采购到货、确认入库、跨模块通知，以及各模块概况查询。请换个说法。";
        } else {
            reply = "收到。";
        }
        double confidence = "unknown".equals(action) ? 0.3 : 0.6;
        return new NluResult(action, caseNo, "", problemTypeCn, keyword, solution, "", remark, qty,
                "", targetModule, List.of(), missing, confidence, reply);
    }

    /**
     * 关键词 → 全局动作码。宾语（报警/派工/采购…）优先于当前模块；
     * 只有动词含糊（如光说"接收"）时才用 currentModule 消歧。
     */
    private String detectAction(String t, String module) {
        // 跨模块通知：动词 + 目标模块词（最先判，避免内容里的"关闭/接收"误触发其它动作）
        if (containsAny(t, "通知", "告诉", "提醒", "转告") && !extractNotify(t)[0].isBlank()) return "notify.send";

        boolean aboutAlarm    = containsAny(t, "报警", "安灯");
        boolean aboutDispatch = containsAny(t, "派工", "工单任务");
        boolean aboutPurchase = containsAny(t, "采购", "到货", "收货");
        boolean aboutInbound  = containsAny(t, "入库");
        boolean aboutCase     = containsAny(t, "案例", "售后", "客户");

        if (containsAny(t, "怎么处理", "怎么办", "如何处理", "处理建议", "给个建议", "你觉得")) return "aftersale.advise";
        if (containsAny(t, "协查", "协同任务", "派协同", "跨部门") || (containsAny(t, "根因") && containsAny(t, "启动", "派"))) return "aftersale.rca_dispatch";
        if (containsAny(t, "健康", "该保养", "诊断") || (containsAny(t, "设备") && containsAny(t, "怎么样", "状态如何"))) {
            if (!containsAny(t, "多少", "几台", "概况")) return "device.diagnose";
        }

        if (containsAny(t, "接收", "接单", "受理", "确认")) {
            if (aboutAlarm) return "device.alarm_receive";
            if (aboutDispatch) return "production.dispatch_accept";
            if (aboutPurchase) return "purchase.receive";
            if (aboutInbound) return "warehouse.inbound_confirm";
            if (containsAny(t, "受理") || aboutCase || "aftersale".equals(module)) return "aftersale.accept";
            if ("device".equals(module)) return "device.alarm_receive";
            if ("production".equals(module)) return "production.dispatch_accept";
            if ("purchase".equals(module)) return "purchase.receive";
            if ("warehouse".equals(module)) return "warehouse.inbound_confirm";
        }
        if (containsAny(t, "解除", "消除") && aboutAlarm) return "device.alarm_close";
        if (containsAny(t, "开工", "开始生产", "开始执行")) return "production.dispatch_start";
        if (aboutPurchase && containsAny(t, "到货", "收货", "到了")) return "purchase.receive";
        if (aboutInbound && !containsAny(t, "多少", "概况", "几个")) return "warehouse.inbound_confirm";

        if (containsAny(t, "标记解决", "解决", "已解决", "处理好", "搞定", "解决了")) return "aftersale.resolve";
        if (containsAny(t, "关闭", "结案", "关掉", "关单")) return aboutAlarm ? "device.alarm_close" : "aftersale.close";
        if (containsAny(t, "追溯", "溯源", "怎么来的", "根因", "为什么")) return "aftersale.query_trace";
        if (containsAny(t, "有哪些问题", "什么问题", "详情", "它", "这单", "这个案例")) return "aftersale.query_detail";

        // 对话下单：下单/下订单/新建订单/订购（放在"接收/确认"之前，避免"下单"里的"单"误触发）
        if (containsAny(t, "下单", "下订单", "下一个订单", "新建订单", "创建订单", "我要订购", "订购")) {
            return "order.create";
        }

        // 库存明细：查库存 / 某物料有多少（走数据库快照，非仅统计条数）
        if (containsAny(t, "库存", "仓库", "库位", "物料仓")
                && containsAny(t, "查", "查询", "多少", "有几", "还剩", "剩余", "有没有", "查一下", "看一下", "列出", "清单", "明细")) {
            if (aboutInbound && containsAny(t, "待入库", "入库任务", "入库单") && !containsAny(t, "物料", "材料", "MAT")) {
                return "warehouse.overview";
            }
            return "warehouse.query_inventory";
        }

        // 概况查询：按宾语，再按当前模块
        if (containsAny(t, "kpi", "KPI", "统计", "多少", "几条", "列表", "清单", "概况", "情况", "看一下", "查一下", "有没有")) {
            if (aboutAlarm || containsAny(t, "设备")) return "device.overview";
            if (aboutDispatch || containsAny(t, "生产", "工单")) return "production.overview";
            if (containsAny(t, "质检", "不合格", "缺陷")) return "quality.overview";
            if (aboutPurchase) return "purchase.overview";
            if (containsAny(t, "库存", "仓库") || aboutInbound) return "warehouse.overview";
            if (containsAny(t, "订单")) return "order.overview";
            if (containsAny(t, "成本", "结算")) return "cost.overview";
            if (containsAny(t, "用户", "日志")) return "system.overview";
            if (aboutCase || "aftersale".equals(module) || module == null || module.isBlank()) return "aftersale.query_list";
            return module + ".overview";
        }
        return "unknown";
    }

    /** 中文模块词表：通知目标解析 + LLM 输出归一共用 */
    private static final Map<String, String> MODULE_WORDS = Map.ofEntries(
            Map.entry("售后", "aftersale"), Map.entry("设备", "device"), Map.entry("维保", "device"),
            Map.entry("生产", "production"), Map.entry("车间", "production"),
            Map.entry("采购", "purchase"), Map.entry("仓储", "warehouse"), Map.entry("仓库", "warehouse"),
            Map.entry("质检", "quality"), Map.entry("质量", "quality"),
            Map.entry("订单", "order"), Map.entry("销售", "order"),
            Map.entry("成本", "cost"), Map.entry("财务", "cost"), Map.entry("系统", "system"));

    /** "通知生产EQ-003故障了…" → [production, "EQ-003故障了…"]；没识别出目标返回 ["",...]。 */
    private String[] extractNotify(String t) {
        for (String verb : new String[]{"通知", "告诉", "提醒", "转告"}) {
            int vi = t.indexOf(verb);
            if (vi < 0) continue;
            String rest = t.substring(vi + verb.length());
            for (Map.Entry<String, String> e : MODULE_WORDS.entrySet()) {
                if (rest.startsWith(e.getKey())) {
                    String content = rest.substring(e.getKey().length());
                    // 掉头部的"模块/部门/组"后缀与标点
                    content = content.replaceFirst("^(模块|部门|组|那边|人员)?[，,：:、\\s]*", "").trim();
                    return new String[]{e.getValue(), content};
                }
            }
        }
        return new String[]{"", ""};
    }

    /** LLM 可能输出中文模块名，归一为模块码 */
    private String normalizeModuleWord(String m) {
        if (m == null || m.isBlank()) return "";
        String s = m.trim();
        if (MODULE_WORDS.containsValue(s)) return s;
        return MODULE_WORDS.getOrDefault(s.replace("模块", "").replace("部门", ""), "");
    }

    private String extractContextModule(String contextJson) {
        if (contextJson == null || contextJson.isBlank()) return "";
        try {
            return MAPPER.readTree(contextJson).path("currentModule").asText("");
        } catch (Exception e) { return ""; }
    }

    private String detectProblemType(String t) {
        if (containsAny(t, "色差", "偏色", "色彩", "颜色", "色准", "色域")) return "色彩";
        if (containsAny(t, "亮点", "坏点", "暗点", "亮斑")) return "亮点";
        if (containsAny(t, "亮线", "暗线", "花屏", "黑屏", "不显示", "显示")) return "显示";
        if (containsAny(t, "接口", "HDMI", "hdmi", "typec", "DP", "usb", "USB")) return "接口";
        if (containsAny(t, "外观", "划痕", "掉漆", "磕碰", "刮伤")) return "外观";
        if (containsAny(t, "电源", "开不了机", "不通电", "无法开机")) return "电源";
        if (containsAny(t, "性能", "卡顿", "延迟", "响应")) return "性能";
        return "";
    }

    private String stripFillers(String t) {
        String s = t;
        for (String w : new String[]{"受理", "标记解决", "解决", "关闭", "结案", "追溯", "溯源", "接收", "解除",
                "开工", "开始生产", "到货", "入库", "确认", "报警", "安灯", "派工", "健康", "诊断", "怎么样",
                "一下", "帮我", "帮忙", "请", "把", "这个", "那个", "案例", "单子", "工单", "的", "查", "看看"}) {
            s = s.replace(w, "");
        }
        return s.trim();
    }

    private String extractSolution(String t) {
        String[] markers = {"方案是", "方案为", "方案：", "方案:", "处理为", "处理方式是", "解决方式是", "解决方式为"};
        for (String m : markers) {
            int i = t.indexOf(m);
            if (i >= 0) return t.substring(i + m.length()).trim();
        }
        Matcher done = Pattern.compile("已[^，。,\\.]{2,}").matcher(t);
        if (done.find()) return done.group().trim();
        return "";
    }

    private int extractQty(String t) {
        Matcher m = Pattern.compile("(\\d+)\\s*(台|件|个|片|块|套)").matcher(t);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private String extractEntityNo(String t) {
        Matcher m = ENTITY_NO.matcher(t.toUpperCase());
        return m.find() ? m.group(1) : "";
    }

    private boolean containsAny(String t, String... kws) {
        for (String k : kws) if (t.contains(k)) return true;
        return false;
    }

    private String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
