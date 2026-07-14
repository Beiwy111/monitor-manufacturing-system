package com.upc.computer.service.impl;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.entity.CostSettlement;
import com.upc.computer.entity.User;
import com.upc.computer.config.RcaAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.computer.mapper.AfterSalesRcaMapper;
import com.upc.computer.mapper.AfterSalesCaseMapper;
import com.upc.computer.mapper.UserMapper;
import com.upc.computer.service.AfterSalesService;
import com.upc.computer.service.CostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AfterSalesServiceImpl implements AfterSalesService {

    @Autowired private AfterSalesCaseMapper caseMapper;
    @Autowired private AfterSalesRcaMapper rcaMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private RcaAiClient rcaAiClient;
    @Autowired private ObjectMapper objectMapper;
    private final Map<String, Map<String,Object>> latestAnalyses = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Map<String,Object>> latestTriages = new java.util.concurrent.ConcurrentHashMap<>();
    @Autowired @Lazy private CostService costService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── 售后案例 CRUD ─────────────────────────────────────────
    @Override public ArrayList<AfterSalesCase> afterSalesCaseList() { return caseMapper.afterSalesCaseList(); }
    @Override public AfterSalesCase getAfterSalesCaseById(String caseNo) { return caseMapper.getAfterSalesCaseById(caseNo); }
    @Override public void insertAfterSalesCase(AfterSalesCase c) { c.setCreatedAt(LocalDateTime.now()); c.setUpdatedAt(LocalDateTime.now()); if (c.getCaseStatus()==null) c.setCaseStatus("OPEN"); caseMapper.insertAfterSalesCase(c); }
    @Override public void updateAfterSalesCase(AfterSalesCase c) { c.setUpdatedAt(LocalDateTime.now()); caseMapper.updateAfterSalesCase(c); }
    @Override public void deleteAfterSalesCase(String caseNo) { caseMapper.deleteAfterSalesCase(caseNo); }

    // ── Settlement 委托给 CostService ─────────────────────────
    @Override public ArrayList<CostSettlement> settlementList() { return costService.settlementList(); }
    @Override public CostSettlement getSettlementById(Long id)  { return costService.getSettlementById(id); }
    @Override public void insertSettlement(CostSettlement s)    { costService.insertSettlement(s); }
    @Override public void updateSettlement(CostSettlement s)    { costService.updateSettlement(s); }
    @Override public void deleteSettlement(Long id)             { costService.deleteSettlement(id); }

    // ── 视图查询 ──────────────────────────────────────────────
    @Override
    public List<Map<String, Object>> listCaseViews() {
        List<Map<String, Object>> list = caseMapper.listCaseViews();
        list.forEach(row -> {
            row.put("caseStatusCn",  statusCn(str(row, "caseStatus")));
            row.put("caseLevelCn",   levelCn(str(row, "caseLevel")));
            row.put("problemTypeCn", problemTypeCn(str(row, "problemType")));
            fmtTime(row, "openedAt"); fmtTime(row, "processingAt");
            fmtTime(row, "resolvedAt"); fmtTime(row, "closedAt"); fmtTime(row, "updatedAt");
        });
        return list;
    }

    @Override
    public Map<String, Object> getTraceDetail(String caseNo) {
        Map<String, Object> detail = caseMapper.getTraceDetail(caseNo);
        if (detail == null) return Map.of();
        detail.put("caseStatusCn",       statusCn(str(detail, "caseStatus")));
        detail.put("caseLevelCn",        levelCn(str(detail, "caseLevel")));
        detail.put("problemTypeCn",      problemTypeCn(str(detail, "problemType")));
        detail.put("inspectionStatusCn", inspectionStatusCn(str(detail, "inspectionStatus")));
        detail.put("severityCn",         severityCn(str(detail, "severity")));
        detail.put("ncHandleStatusCn",   ncHandleStatusCn(str(detail, "ncHandleStatus")));
        fmtTime(detail, "openedAt"); fmtTime(detail, "processingAt");
        fmtTime(detail, "resolvedAt"); fmtTime(detail, "closedAt");

        // 固定 8 环反向链的数据：发货 / 成品入库 / 报工 / 领料 / 物料质检（复用 RCA 上下文查询）
        Map<String, Object> ctx = rcaMapper.context(caseNo);
        if (ctx == null) ctx = Map.of();
        Long workOrderId = longValue(ctx.get("workOrderId"));
        Long deliveryId  = longValue(ctx.get("deliveryId"));
        Long orderId     = longValue(ctx.get("orderId"));
        Map<String, Object> delivery = (deliveryId == null && orderId == null) ? null
                : rcaMapper.deliveryForCase(deliveryId, orderId);
        Map<String, Object> inbound  = workOrderId == null ? null : rcaMapper.productInbound(workOrderId);
        List<Map<String, Object>> reports   = workOrderId == null ? List.of() : rcaMapper.reports(workOrderId);
        List<Map<String, Object>> materials = workOrderId == null ? List.of() : rcaMapper.consumedMaterials(workOrderId);
        List<Map<String, Object>> matInsp   = workOrderId == null ? List.of() : rcaMapper.materialInspections(workOrderId);
        detail.put("traceChain", buildTraceChain(detail, ctx, delivery, inbound, reports, materials, matInsp));
        return detail;
    }

    @Override
    public Map<String, Object> caseKpi() {
        Map<String, Object> kpi = caseMapper.caseKpi();
        return kpi != null ? kpi : Map.of();
    }

    @Override
    public Map<String, Object> buildRcaAnalysis(String caseNo, boolean force) {
        require(caseNo);
        if (!force) {
            Map<String,Object> memory = latestAnalyses.get(caseNo);
            if (memory != null) return markCached(memory);
            Map<String,Object> stored = rcaMapper.latestAnalysis(caseNo);
            if (stored != null && stored.get("snapshotJson") != null) {
                try {
                    Map<String,Object> snapshot = objectMapper.readValue(String.valueOf(stored.get("snapshotJson")), Map.class);
                    latestAnalyses.put(caseNo, snapshot);
                    return markCached(snapshot);
                } catch (Exception e) {
                    System.err.println("RCA snapshot read failed: " + e.getMessage());
                }
            }
        }
        Map<String, Object> ctx = rcaMapper.context(caseNo);
        if (ctx == null) throw new BusinessException("无法读取售后追溯上下文");
        Long workOrderId = longValue(ctx.get("workOrderId"));
        Long inspectionId = longValue(ctx.get("inspectionId"));
        Long materialId = longValue(ctx.get("materialId"));
        List<Map<String, Object>> reports = workOrderId == null ? List.of() : rcaMapper.reports(workOrderId);
        List<Map<String, Object>> alarms = workOrderId == null ? List.of() : rcaMapper.alarms(workOrderId,
                ctx.get("workStartTime"), ctx.get("workEndTime"));
        List<Map<String, Object>> items = inspectionId == null ? List.of() : rcaMapper.inspectionItems(inspectionId);
        List<Map<String, Object>> materials = workOrderId == null ? List.of() : rcaMapper.consumedMaterials(workOrderId);
        Map<String, Object> settlement = workOrderId == null ? null : rcaMapper.settlement(workOrderId);
        Map<String, Object> stats = rcaMapper.caseStats(materialId, str(ctx, "batchNo"), str(ctx,"problemType"));

        Map<String,Object> qualityCard = qualityScoreCard(ctx, items);
        Map<String,Object> equipmentCard = equipmentScoreCard(reports, alarms, ctx);
        Map<String,Object> processCard = processScoreCard(ctx, reports);
        Map<String,Object> supplierCard = supplierScoreCard(materials, stats, ctx);
        int qualityScore = (Integer) qualityCard.get("score");
        int equipmentScore = (Integer) equipmentCard.get("score");
        int processScore = (Integer) processCard.get("score");
        int supplierScore = (Integer) supplierCard.get("score");
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        nodes.add(node("case", "售后工单", caseNo, "AFTER_SALES_CASE", "AFTERSALE", 0,
                str(ctx,"problemDescription"), List.of("客户："+str(ctx,"customerName"),"问题类型："+str(ctx,"problemType"))));
        nodes.add(node("batch", "成品批次", fallback(str(ctx,"batchNo"),"未记录"), "PRODUCT_BATCH", "PRODUCTION", 0,
                str(ctx,"materialName"), List.of("当前数据库未建立逐台序列号表，按成品批次追溯")));
        links.add(link("case","batch","绑定批次",.9));
        if (!str(ctx,"orderNo").isBlank()) { nodes.add(node("order","客户订单",str(ctx,"orderNo"),"CUSTOMER_ORDER","ORDER",0,str(ctx,"customerName"),List.of("真实订单关联"))); links.add(link("batch","order","所属订单",.95)); }
        if (inspectionId != null) { nodes.add(node("inspection","成品质检",str(ctx,"inspectionNo"),"QUALITY_INSPECTION","QUALITY",qualityScore,
                "状态 "+str(ctx,"inspectionStatus")+"，抽检 "+decimal(ctx,"sampleQuantity")+"，不合格 "+decimal(ctx,"unqualifiedQuantity"), qualityEvidence(ctx,items))); links.add(link("batch","inspection","关联质检",.98)); }
        if (workOrderId != null) { nodes.add(node("workOrder","生产工单",str(ctx,"workOrderNo"),"WORK_ORDER","PRODUCTION",processScore,
                "完成 "+decimal(ctx,"completedQuantity")+"，不合格 "+decimal(ctx,"workUnqualifiedQuantity"),processEvidence(ctx,reports))); links.add(link("batch","workOrder","生产来源",.98)); }
        List<Map<String,Object>> equipmentReports = reports.stream().filter(r -> r.get("equipmentId") != null).toList();
        if (!equipmentReports.isEmpty()) {
            String codes = equipmentReports.stream().map(r -> str(r,"equipmentCode")).filter(s -> !s.isBlank()).distinct().reduce((a,b) -> a + " / " + b).orElse("-");
            nodes.add(node("equipment","生产设备",codes,"EQUIPMENT","DEVICE",equipmentScore,
                    "本工单使用 " + equipmentReports.stream().map(r -> String.valueOf(r.get("equipmentId"))).distinct().count() + " 台设备，关联 " + alarms.size() + " 条报警",
                    aggregateEquipmentEvidence(equipmentReports,alarms)));
            links.add(link("workOrder","equipment","报工设备",.95));
        }
        int materialIndex=0;
        for (Map<String,Object> material:materials) { String mid="material"+(++materialIndex); int score=materialIndex==1?supplierScore:materialNodeScore(material,ctx); nodes.add(node(mid,"领用物料批次",fallback(str(material,"materialBatchNo"),"未记录批次"),"MATERIAL_BATCH","PURCHASE",score,str(material,"materialName"),materialEvidence(material))); links.add(link("workOrder",mid,"实际领料",.98)); if(material.get("purchaseOrderId")!=null){String sid="supplier"+materialIndex;nodes.add(node(sid,"采购供应商",fallback(str(material,"supplierName"),"供应商未匹配"),"SUPPLIER","PURCHASE",score,"采购单 "+str(material,"purchaseOrderNo"),List.of("采购状态："+str(material,"purchaseStatus"),"采购单价："+decimal(material,"unitPrice"))));links.add(link(mid,sid,"采购入库来源",.98));} }

        attachScoreCard(nodes,"inspection",qualityCard);
        attachScoreCard(nodes,"workOrder",processCard);
        attachScoreCard(nodes,"equipment",equipmentCard);
        attachScoreCard(nodes,"material1",supplierCard);

        List<Map<String,Object>> causes=new ArrayList<>();
        causes.add(cause("供应商/物料异常","PURCHASE",supplierCard,materialSummary(materials),materials.isEmpty()?"workOrder":"material1"));
        causes.add(cause("设备异常","DEVICE",equipmentCard,alarmSummary(alarms),!equipmentReports.isEmpty()?"equipment":"workOrder"));
        causes.add(cause("质量检验风险","QUALITY",qualityCard,qualitySummary(ctx,items),inspectionId!=null?"inspection":"batch"));
        causes.add(cause("生产工艺风险","PRODUCTION",processCard,processSummary(ctx,reports),workOrderId!=null?"workOrder":"batch"));
        causes.sort((a,b)->Integer.compare((Integer)b.get("score"),(Integer)a.get("score")));
        List<Map<String,Object>> losses=realLosses(ctx,settlement,alarms);
        BigDecimal totalLoss=losses.stream().map(x->(BigDecimal)x.get("amount")).reduce(BigDecimal.ZERO,BigDecimal::add);
        List<Map<String,Object>> actions=ruleActions(causes,materials,reports);
        String conclusion=ruleConclusion(causes,ctx);
        String modelType="RULE";
        Map<String,Object> aiPayload=new LinkedHashMap<>();
        aiPayload.put("case",Map.of("caseNo",caseNo,"problem",str(ctx,"problemDescription"),"product",str(ctx,"materialName"),"batch",str(ctx,"batchNo")));
        aiPayload.put("rootCauses",causes); aiPayload.put("losses",losses); aiPayload.put("totalLoss",totalLoss); aiPayload.put("availableActions",actions);
        try { Map<String,Object> ai=rcaAiClient.generate(aiPayload); if(ai.get("conclusion")!=null) conclusion=String.valueOf(ai.get("conclusion")); if(ai.get("actions") instanceof List<?> list && !list.isEmpty()) actions=(List<Map<String,Object>>)(List<?>)list; modelType="RULE_LLM"; } catch(Exception e) { System.err.println("RCA AI fallback: "+e.getMessage()); }

        int present=4+(inspectionId!=null?2:0)+(workOrderId!=null?2:0)+(!reports.isEmpty()?2:0)+(!materials.isEmpty()?2:0)+(settlement!=null?1:0);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("analysisNo","RCA-"+caseNo+"-"+System.currentTimeMillis()); result.put("caseNo",caseNo);
        result.put("serialNo","批次追溯："+fallback(str(ctx,"batchNo"),"未记录")); result.put("status","COMPLETED");
        result.put("algorithmVersion","RCA_EVIDENCE_V3"); result.put("modelType",modelType); result.put("cached",false); result.put("analyzedAt",LocalDateTime.now().format(FMT));
        result.put("dataCompleteness",BigDecimal.valueOf(present/13.0).setScale(2,RoundingMode.HALF_UP)); result.put("nodes",nodes); result.put("links",links);
        result.put("causes",causes); result.put("losses",losses); result.put("totalLoss",totalLoss); result.put("actions",actions); result.put("conclusion",conclusion);
        result.put("disclaimer","分析基于当前 MES 真实数据；缺失的序列号、检测实测值等数据会降低置信度，最终根因需人工确认");
        latestAnalyses.put(caseNo,result);
        try { Map<String,Object>top=causes.get(0);Map<String,Object>row=new LinkedHashMap<>();row.put("analysisNo",result.get("analysisNo"));row.put("caseNo",caseNo);row.put("algorithmVersion",result.get("algorithmVersion"));row.put("modelType",modelType);row.put("topCause",top.get("name"));row.put("topDepartment",top.get("department"));row.put("topScore",top.get("score"));row.put("dataCompleteness",result.get("dataCompleteness"));row.put("estimatedLoss",totalLoss);row.put("conclusion",conclusion);row.put("snapshotJson",objectMapper.writeValueAsString(result));rcaMapper.insertAnalysis(row);}catch(Exception e){System.err.println("RCA snapshot persist failed: "+e.getMessage());}
        return result;
    }

    private Map<String,Object> markCached(Map<String,Object> source){Map<String,Object> copy=new LinkedHashMap<>(source);copy.put("cached",true);return copy;}

    @Override
    public Map<String, Object> dispatchRcaTasks(String caseNo, List<String> departments) {
        require(caseNo);
        List<String> target = departments == null || departments.isEmpty()
                ? List.of("PURCHASE", "QUALITY", "DEVICE", "COST") : departments;
        List<Map<String, Object>> tasks = new ArrayList<>();
        Map<String,Object> analysis=latestAnalyses.get(caseNo);
        String analysisNo=analysis==null?null:String.valueOf(analysis.get("analysisNo"));
        List<Map<String,Object>> actions=analysis!=null&&analysis.get("actions") instanceof List<?> list?(List<Map<String,Object>>)(List<?>)list:List.of();
        for (int i = 0; i < target.size(); i++) {
            String department = target.get(i);
            Map<String,Object>matched=actions.stream().filter(a->department.equals(String.valueOf(a.get("department")))).findFirst().orElse(Map.of());
            Map<String,Object>task=new LinkedHashMap<>();task.put("taskNo","RCA-TASK-"+caseNo+"-"+department+"-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));task.put("caseNo",caseNo);task.put("analysisNo",analysisNo);task.put("department",department);task.put("title",matched.getOrDefault("title","售后根因协查任务"));task.put("content",matched.getOrDefault("reason","请依据根因分析证据完成协查并回流结论"));task.put("priority",matched.getOrDefault("priority","HIGH"));task.put("targetPath",departmentPath(department));rcaMapper.insertTask(task);rcaMapper.insertNotification(task);task.put("status","PENDING");task.put("createdAt",LocalDateTime.now().format(FMT));tasks.add(task);
        }
        return Map.of("caseNo", caseNo, "taskCount", tasks.size(), "tasks", tasks);
    }

    @Override public List<Map<String,Object>> listRcaTasks(String department){return rcaMapper.tasksByDepartment(department);}

    @Override public Map<String,Object> confirmRootCause(Map<String,Object> request){String analysisNo=String.valueOf(request.getOrDefault("analysisNo",""));String cause=String.valueOf(request.getOrDefault("cause",""));String department=String.valueOf(request.getOrDefault("department",""));String remark=String.valueOf(request.getOrDefault("remark",""));if(analysisNo.isBlank()||cause.isBlank()||department.isBlank())throw new BusinessException("分析编号、最终根因和责任部门不能为空");Map<String,Object>row=Map.of("analysisNo",analysisNo,"cause",cause,"department",department,"remark",remark);if(rcaMapper.confirmRootCause(row)==0)throw new BusinessException("分析记录不存在");return row;}
    @Override public Map<String,Object> updateRcaTask(Map<String,Object> request){Long taskId=longValue(request.get("taskId"));String status=String.valueOf(request.getOrDefault("status",""));String result=String.valueOf(request.getOrDefault("result",""));if(taskId==null||!List.of("ACCEPTED","PROCESSING","COMPLETED","REJECTED").contains(status))throw new BusinessException("任务参数不正确");Map<String,Object>row=Map.of("taskId",taskId,"status",status,"result",result);if(rcaMapper.updateTask(row)==0)throw new BusinessException("任务不存在");return row;}
    @Override public Map<String,Object> rcaTaskProgress(String caseNo){Map<String,Object>p=rcaMapper.taskProgress(caseNo);return p==null?Map.of("total",0,"pending",0,"processing",0,"completed",0):p;}

    @Override
    public Map<String,Object> triageCase(String caseNo, boolean force) {
        require(caseNo);
        if (!force) {
            Map<String,Object> memory=latestTriages.get(caseNo);if(memory!=null)return markTriageCached(memory);
            Map<String,Object> stored=rcaMapper.latestTriage(caseNo);if(stored!=null&&stored.get("snapshotJson")!=null)try{Map<String,Object>s=objectMapper.readValue(String.valueOf(stored.get("snapshotJson")),Map.class);latestTriages.put(caseNo,s);return markTriageCached(s);}catch(Exception ignored){}
        }
        Map<String,Object>c=rcaMapper.context(caseNo);if(c==null)throw new BusinessException("无法读取售后工单");Long workOrderId=longValue(c.get("workOrderId"));List<Map<String,Object>>reports=workOrderId==null?List.of():rcaMapper.reports(workOrderId);List<Map<String,Object>>alarms=workOrderId==null?List.of():rcaMapper.alarms(workOrderId,c.get("workStartTime"),c.get("workEndTime"));Map<String,Object>stats=rcaMapper.caseStats(longValue(c.get("materialId")),str(c,"batchNo"),str(c,"problemType"));
        String text=(str(c,"problemDescription")+" "+str(c,"problemType")).toLowerCase();
        List<Integer> quantityMentions=parseQuantityMentions(text);
        Map<String,Object>aiPayload=new LinkedHashMap<>();
        aiPayload.put("caseNo",caseNo);aiPayload.put("customerDescription",str(c,"problemDescription"));aiPayload.put("problemType",str(c,"problemType"));aiPayload.put("caseLevel",str(c,"caseLevel"));aiPayload.put("product",str(c,"materialName"));aiPayload.put("batchNo",str(c,"batchNo"));aiPayload.put("quantityMentions",quantityMentions);aiPayload.put("quantityInstruction","数量按原文理解，必须区分采购总数与实际故障数，不得把第一个数量直接视为受影响数量");
        aiPayload.put("mesEvidence",Map.of(
                "sameBatchCases",longNumber(stats==null?null:stats.get("sameBatchCases")),
                "sameProblemBatchCases",longNumber(stats==null?null:stats.get("sameProblemBatchCases")),
                "inspectionStatus",str(c,"inspectionStatus"),
                "inspectionUnqualifiedQuantity",bd(c.get("unqualifiedQuantity")),
                "productionAlarmCount",alarms.size(),
                "workReportCount",reports.size()));
        Map<String,Object>ai;
        try{ai=rcaAiClient.triage(aiPayload);}catch(Exception e){throw new BusinessException("AI 分诊服务暂不可用，请稍后重试："+e.getMessage());}
        String category=allowedValue(ai.get("category"),List.of("BUSINESS_CONSULT","TECH_SUPPORT","LOGISTICS_DAMAGE","SINGLE_REPAIR","BATCH_QUALITY"),"SINGLE_REPAIR");
        int score=clamp(intValue(ai.get("escalationScore"),30));
        long sameProblemCases=longNumber(stats==null?null:stats.get("sameProblemBatchCases"));
        String inspectionStatus=str(c,"inspectionStatus");
        BigDecimal inspectionBad=bd(c.get("unqualifiedQuantity"));
        List<Map<String,Object>>verifiedFacts=new ArrayList<>();
        if(sameProblemCases>=2)verifiedFacts.add(fact("同批同类售后重复","MES售后记录","同一批次同类问题工单共 "+sameProblemCases+" 条"));
        if(List.of("FAILED","RECHECK_REQUIRED","UNQUALIFIED").contains(inspectionStatus)||inspectionBad.signum()>0)verifiedFacts.add(fact("关联质检异常","MES质检记录","质检状态 "+fallback(inspectionStatus,"未记录")+"，不合格数量 "+inspectionBad.stripTrailingZeros().toPlainString()));
        if(!alarms.isEmpty())verifiedFacts.add(fact("生产窗口设备报警","MES安灯记录","关联生产窗口存在 "+alarms.size()+" 条设备报警"));
        List<Map<String,Object>>customerClaims=List.of(fact("客户故障陈述","客户反馈",str(c,"problemDescription")));
        boolean aiSuggestsBatch=booleanValue(ai.get("needRca"))||"BATCH_QUALITY".equals(category);
        boolean verifiedQualityRisk=!verifiedFacts.isEmpty();
        String decisionStatus=verifiedQualityRisk?"VERIFIED_RISK":aiSuggestsBatch?"PENDING_EVIDENCE":"ROUTINE";
        boolean needRca=verifiedQualityRisk;
        if(aiSuggestsBatch){category="BATCH_QUALITY";score=Math.max(score,verifiedQualityRisk?60:40);}
        String categoryName=fallback(String.valueOf(ai.getOrDefault("categoryName","")),categoryName(category));
        String risk=verifiedQualityRisk?"HIGH":score>=30?"MEDIUM":"LOW";
        List<Map<String,Object>>reasons=mapList(ai.get("reasons"));
        List<Map<String,Object>>actions=mapList(ai.get("actions"));
        if(reasons.isEmpty())reasons=List.of(triageReason("AI综合研判",String.valueOf(ai.getOrDefault("understanding","已结合客户描述与MES证据完成判断")),score));
        if(actions.isEmpty())actions=defaultTriageActions(category);
        String summary=decisionStatus.equals("VERIFIED_RISK")?"已发现可核验的 MES 质量风险证据，建议启动根因追溯。":decisionStatus.equals("PENDING_EVIDENCE")?"客户描述疑似质量问题，但 MES 证据不足，需先补充序列号、图片和批次信息后再判断。":String.valueOf(ai.getOrDefault("summary","AI已生成推荐处理路径。"));
        Map<String,Object>result=new LinkedHashMap<>();result.put("triageNo","TRIAGE-"+caseNo+"-"+System.currentTimeMillis());result.put("caseNo",caseNo);result.put("category",category);result.put("categoryName",categoryName);result.put("escalationScore",score);result.put("riskLevel",risk);result.put("needRca",needRca);result.put("decisionStatus",decisionStatus);result.put("decisionStatusName",decisionStatusName(decisionStatus));result.put("verifiedFacts",verifiedFacts);result.put("customerClaims",customerClaims);result.put("evidenceSufficient",verifiedQualityRisk);result.put("reasons",reasons);result.put("actions",actions);result.put("summary",summary);result.put("modelType","LLM_EVIDENCE_GATED");
        result.put("aiUnderstanding",ai.getOrDefault("understanding",summary));result.put("faultPhenomena",ai.getOrDefault("faultPhenomena",List.of()));result.put("missingInformation",ai.getOrDefault("missingInformation",List.of()));result.put("followUpQuestions",ai.getOrDefault("followUpQuestions",List.of()));result.put("customerReply",ai.getOrDefault("customerReply","您好，我们已收到您的反馈，正在结合产品批次与生产质量记录进行分析。"));
        boolean qualityRelated=booleanValue(ai.get("qualityRelated"))||"BATCH_QUALITY".equals(category)||verifiedQualityRisk;
        String urgency=score>=60?"RED":score>=30?"YELLOW":"GREEN";
        result.put("qualityRelated",qualityRelated);result.put("urgency",urgency);result.put("urgencyName",switch(urgency){case"RED"->"紧急";case"YELLOW"->"关注";default->"一般";});result.put("affectedQuantity",intValue(ai.get("affectedQuantity"),0));
        result.put("cached",false);result.put("createdAt",LocalDateTime.now().format(FMT));latestTriages.put(caseNo,result);
        try{Map<String,Object>row=new LinkedHashMap<>();row.putAll(result);row.put("reasonJson",objectMapper.writeValueAsString(reasons));row.put("actionJson",objectMapper.writeValueAsString(actions));row.put("snapshotJson",objectMapper.writeValueAsString(result));rcaMapper.insertTriage(row);}catch(Exception e){System.err.println("Triage persist failed: "+e.getMessage());}return result;
    }

    private Map<String,Object> markTriageCached(Map<String,Object>s){Map<String,Object>m=new LinkedHashMap<>(s);m.put("cached",true);return m;}
    private String allowedValue(Object value,List<String> allowed,String fallback){String s=value==null?"":String.valueOf(value);return allowed.contains(s)?s:fallback;}
    private int intValue(Object value,int fallback){if(value instanceof Number n)return n.intValue();try{return Integer.parseInt(String.valueOf(value));}catch(Exception e){return fallback;}}
    private boolean booleanValue(Object value){return value instanceof Boolean b?b:Boolean.parseBoolean(String.valueOf(value));}
    private List<Map<String,Object>> mapList(Object value){if(!(value instanceof List<?> list))return List.of();List<Map<String,Object>>result=new ArrayList<>();for(Object item:list)if(item instanceof Map<?,?> raw){Map<String,Object>mapped=new LinkedHashMap<>();raw.forEach((k,v)->mapped.put(String.valueOf(k),v));result.add(mapped);}return result;}
    private String categoryName(String category){return switch(category){case"BUSINESS_CONSULT"->"业务/采购咨询";case"TECH_SUPPORT"->"技术支持/兼容诊断";case"LOGISTICS_DAMAGE"->"物流损坏";case"BATCH_QUALITY"->"批次质量风险";default->"单机维修";};}
    private List<Map<String,Object>> defaultTriageActions(String category){return switch(category){case"BUSINESS_CONSULT"->List.of(action("PURCHASE","转采购/订单客服处理","NORMAL","回复价格、库存或交期"));case"TECH_SUPPORT"->List.of(action("AFTERSALE","发起远程技术诊断","NORMAL","核验接口、线材、驱动和设置"));case"LOGISTICS_DAMAGE"->List.of(action("AFTERSALE","留存物流证据并安排换货","HIGH","核验包装和签收损伤"));case"BATCH_QUALITY"->List.of(action("QUALITY","冻结同批次待发产品并扩大复检","URGENT","启动跨部门根因追溯"));default->List.of(action("AFTERSALE","创建维修工单","HIGH","优先恢复客户设备使用"));};}
    private boolean containsAny(String text,String... values){for(String v:values)if(text.contains(v.toLowerCase()))return true;return false;}
    private List<Integer> parseQuantityMentions(String text){List<Integer>values=new ArrayList<>();java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)\\s*台").matcher(text);while(m.find())try{values.add(Integer.parseInt(m.group(1)));}catch(Exception ignored){}return values;}
    private Map<String,Object> fact(String name,String source,String evidence){return Map.of("name",name,"source",source,"evidence",evidence);}
    private String decisionStatusName(String status){return switch(status){case"VERIFIED_RISK"->"已验证质量风险";case"PENDING_EVIDENCE"->"疑似质量问题·待补证";default->"常规售后处理";};}
    private int parseAffectedQuantity(String text){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)\\s*台").matcher(text);if(m.find())try{return Integer.parseInt(m.group(1));}catch(Exception ignored){}return 1;}
    private Map<String,Object> triageReason(String name,String evidence,int impact){return Map.of("name",name,"evidence",evidence,"impact",impact);}
    private List<String> defaultFollowUpQuestions(String category){return switch(category){case"TECH_SUPPORT"->List.of("请确认使用的接口、线材和显卡型号。","问题是否在更换线材或恢复默认设置后仍存在？");case"LOGISTICS_DAMAGE"->List.of("请提供外包装、缓冲材料和设备损伤照片。","签收时外包装是否已出现破损？");case"BUSINESS_CONSULT"->List.of("请确认对应订单号和期望交付时间。");default->List.of("故障首次出现的时间和使用场景是什么？","目前影响几台设备，是否属于同一批次？","请提供故障照片或视频。","重启或更换线材后问题是否仍存在？");};}

    private String departmentPath(String d){return switch(d){case"PURCHASE"->"/dashboard/purchase";case"QUALITY"->"/dashboard/quality";case"DEVICE"->"/dashboard/device";case"COST"->"/dashboard/cost";case"PRODUCTION"->"/system/board";default->"/dashboard/aftersale";};}

    // ── 状态流转 ──────────────────────────────────────────────
    @Override
    @Transactional
    public AfterSalesCase acceptCase(String caseNo, String operator) {
        AfterSalesCase c = require(caseNo);
        if (!"OPEN".equals(c.getCaseStatus()))
            throw new BusinessException("仅 OPEN 状态可受理（当前：" + statusCn(c.getCaseStatus()) + "）");
        c.setCaseStatus("PROCESSING");
        c.setProcessingAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        applyOperator(c, operator);
        caseMapper.updateAfterSalesCase(c);
        return c;
    }

    @Override
    @Transactional
    public AfterSalesCase resolveCase(String caseNo, String solution, String traceResult, String operator) {
        AfterSalesCase c = require(caseNo);
        if (!"PROCESSING".equals(c.getCaseStatus()))
            throw new BusinessException("仅受理中状态可标记解决（当前：" + statusCn(c.getCaseStatus()) + "）");
        if (solution == null || solution.isBlank())
            throw new BusinessException("解决方案不能为空");
        c.setCaseStatus("RESOLVED");
        c.setHandleResult(solution);
        if (traceResult != null && !traceResult.isBlank()) c.setTraceResult(traceResult);
        c.setResolvedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        applyOperator(c, operator);
        caseMapper.updateAfterSalesCase(c);
        return c;
    }

    @Override
    @Transactional
    public AfterSalesCase closeCase(String caseNo, String remark, String operator) {
        AfterSalesCase c = require(caseNo);
        if ("CLOSED".equals(c.getCaseStatus()))
            throw new BusinessException("案例已关闭");
        c.setCaseStatus("CLOSED");
        if (remark != null && !remark.isBlank()) {
            String old = c.getHandleResult();
            c.setHandleResult(old == null ? remark : old + " | " + remark);
        }
        c.setClosedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        applyOperator(c, operator);
        caseMapper.updateAfterSalesCase(c);
        return c;
    }

    // ── 工具 ──────────────────────────────────────────────────
    private AfterSalesCase require(String caseNo) {
        if (caseNo == null || caseNo.isBlank()) throw new BusinessException("案例编号不能为空");
        AfterSalesCase c = caseMapper.getAfterSalesCaseById(caseNo);
        if (c == null) throw new BusinessException("售后案例不存在：" + caseNo);
        return c;
    }

    /** 将操作人用户名解析为 user_id 并写入 serviceUserId，补全操作审计。解析不到则不覆盖原值。 */
    private void applyOperator(AfterSalesCase c, String operator) {
        Long uid = resolveUserId(operator);
        if (uid != null) c.setServiceUserId(uid);
    }

    /** operator(用户名) -> user_id；空或查无此人返回 null（不做"兜底第一个用户"这类误归属）。 */
    private Long resolveUserId(String operator) {
        if (operator == null || operator.isBlank()) return null;
        User u = userMapper.getUserByUsername(operator.trim());
        return u != null ? u.getUserId() : null;
    }

    /**
     * 反向追溯链：客户反馈 → 仓储发货 → 成品入库 → 成品质检 → 四道生产工序 → 生产领料 → 物料质检 → 供应商采购。
     * 生产环节按真实报工记录展开，每道工序只绑定自己的操作员与设备；查无数据时 missing=true。
     * 责任人（发货员/入库仓库员/质检员/操作员/物料质检员/采购员）放 people，键值详情放 details，多行明细放 rows。
     * title/no/desc/type 与旧版字段兼容（语音助手、CaseView 直接复用）。
     */
    private List<Map<String, Object>> buildTraceChain(Map<String, Object> d, Map<String, Object> ctx,
            Map<String, Object> delivery, Map<String, Object> inbound,
            List<Map<String, Object>> reports, List<Map<String, Object>> materials,
            List<Map<String, Object>> matInsp) {
        List<Map<String, Object>> chain = new ArrayList<>();

        // ① 客户反馈（产品）
        Map<String, Object> fb = chainStep("客户反馈", str(d, "caseNo"),
                str(d, "problemTypeCn") + " · " + str(d, "caseStatusCn"), "feedback", false);
        addDetail(fb, "客户", str(d, "customerName"));
        addDetail(fb, "产品型号", str(d, "materialName"));
        addDetail(fb, "成品批次", str(d, "batchNo"));
        addDetail(fb, "关联订单", str(d, "orderNo"));
        addDetail(fb, "问题描述", str(d, "problemDescription"));
        addDetail(fb, "反馈时间", str(d, "openedAt"));
        chain.add(fb);

        // ② 仓储发货
        boolean noDlv = delivery == null || delivery.isEmpty();
        Map<String, Object> dlv = chainStep("仓储发货", noDlv ? "" : str(delivery, "deliveryNo"),
                noDlv ? "" : timeText(delivery.get("deliveryDate")) + " · " + fallback(str(delivery, "logisticsCompany"), "自提"),
                "delivery", noDlv);
        if (!noDlv) {
            addPerson(dlv, "发货员", str(delivery, "shippedByName"));
            addDetail(dlv, "发货数量", decimal(delivery, "deliveryQuantity") + " 台");
            addDetail(dlv, "发货批次", str(delivery, "batchNo"));
            addDetail(dlv, "物流单号", str(delivery, "logisticsNo"));
            addDetail(dlv, "收货人", str(delivery, "receiverName"));
            addDetail(dlv, "发货状态", deliveryStatusCn(str(delivery, "deliveryStatus")));
        }
        chain.add(dlv);

        // ③ 成品入库
        boolean noInb = inbound == null || inbound.isEmpty();
        Map<String, Object> inb = chainStep("成品入库", noInb ? "" : str(inbound, "transactionNo"),
                noInb ? "" : decimal(inbound, "quantity") + " 台 · " + fallback(str(inbound, "warehouseCode"), "成品仓"),
                "inbound", noInb);
        if (!noInb) {
            addPerson(inb, "入库仓库员", str(inbound, "handledByName"));
            addDetail(inb, "仓库", str(inbound, "warehouseCode"));
            addDetail(inb, "库位", str(inbound, "locationCode"));
            addDetail(inb, "入库批次", str(inbound, "batchNo"));
            addDetail(inb, "入库时间", timeText(inbound.get("handledAt")));
        }
        chain.add(inb);

        // ④ 成品质检（不良品记录并入本环）
        boolean noQc = str(ctx, "inspectionNo").isBlank();
        Map<String, Object> qc = chainStep("成品质检", str(ctx, "inspectionNo"),
                noQc ? "" : resultCn(str(ctx, "inspectionResult")) + " · 不合格 " + decimal(ctx, "unqualifiedQuantity"),
                "quality", noQc);
        if (!noQc) {
            addPerson(qc, "质检员", str(ctx, "inspectorName"));
            addDetail(qc, "检验类型", str(ctx, "inspectionType"));
            addDetail(qc, "抽检数量", decimal(ctx, "sampleQuantity"));
            addDetail(qc, "合格数量", decimal(ctx, "qualifiedQuantity"));
            addDetail(qc, "不合格数量", decimal(ctx, "unqualifiedQuantity"));
            addDetail(qc, "检验时间", timeText(ctx.get("inspectedAt")));
            if (!str(d, "nonconformingNo").isBlank()) {
                addRow(qc, "不良品 " + str(d, "nonconformingNo") + " · " + fallback(str(d, "defectType"), "缺陷类型未记录")
                        + " · " + str(d, "severityCn") + " · " + str(d, "ncHandleStatusCn")
                        + (str(d, "ncHandleMethod").isBlank() ? "" : " · 处置：" + str(d, "ncHandleMethod")));
            }
        }
        chain.add(qc);

        // ⑤ 生产工序（每道工序独立展示自己的操作员与设备）
        boolean noWo = str(ctx, "workOrderNo").isBlank() && reports.isEmpty();
        Map<String, Map<String, Object>> distinctReports = new LinkedHashMap<>();
        for (Map<String, Object> report : reports) {
            String key = fallback(str(report, "stepId"), str(report, "stepName"));
            distinctReports.putIfAbsent(key, report);
        }
        List<Map<String, Object>> processReports = new ArrayList<>(distinctReports.values());
        if (processReports.size() > 4) processReports = processReports.subList(0, 4);
        for (int i = 0; i < 4; i++) {
            Map<String, Object> report = i < processReports.size() ? processReports.get(i) : null;
            boolean missingReport = report == null;
            String stepName = missingReport ? "生产工序 " + (i + 1)
                    : fallback(str(report, "stepName"), "生产工序 " + (i + 1));
            String operator = missingReport ? "未记录" : fallback(str(report, "operatorName"), "未记录");
            String equipment = missingReport ? "设备未记录" : fallback(str(report, "equipmentCode"), "设备未记录");
            Map<String, Object> prod = chainStep(stepName, missingReport ? "" : str(report, "reportNo"),
                    missingReport ? "" : "操作员 " + operator + " · " + equipment, "production", missingReport);
            if (missingReport) {
                addDetail(prod, "所属工单", str(ctx, "workOrderNo"));
                addDetail(prod, "工序序号", String.valueOf(i + 1) + " / 4");
            } else {
                addPerson(prod, "操作员", operator);
                addDetail(prod, "所属工单", str(ctx, "workOrderNo"));
                addDetail(prod, "工序序号", String.valueOf(i + 1) + " / 4");
                addDetail(prod, "设备编号", equipment);
                addDetail(prod, "设备名称", str(report, "equipmentName"));
                addDetail(prod, "开始时间", timeText(report.get("startTime")));
                addDetail(prod, "结束时间", timeText(report.get("endTime")));
                addDetail(prod, "完成数量", decimal(report, "completedQuantity"));
                addDetail(prod, "合格数量", decimal(report, "qualifiedQuantity"));
                addDetail(prod, "不合格数量", decimal(report, "unqualifiedQuantity"));
                addDetail(prod, "工时", decimal(report, "workHours"));
                if (!str(report, "remark").isBlank()) addRow(prod, "报工备注：" + str(report, "remark"));
            }
            chain.add(prod);
        }

        // ⑥ 生产领料（物料批次；按需求不展示发料员）
        Map<String, Object> mat = chainStep("生产领料",
                materials.isEmpty() ? "" : materials.size() + " 种物料",
                materials.isEmpty() ? "" : materials.stream().map(m -> str(m, "materialName")).distinct()
                        .limit(3).reduce((a, b) -> a + "、" + b).orElse(""),
                "material", materials.isEmpty());
        for (Map<String, Object> m : materials) {
            addRow(mat, str(m, "materialName") + "（" + str(m, "materialCode") + "）"
                    + " · 批次 " + fallback(str(m, "materialBatchNo"), "未记录")
                    + " · 领用 " + decimal(m, "consumeQuantity")
                    + (m.get("consumedAt") == null ? "" : " · " + timeText(m.get("consumedAt"))));
        }
        chain.add(mat);

        // ⑦ 物料质检（来料检验，责任人：物料质检员）
        Map<String, Object> mq = chainStep("物料质检",
                matInsp.isEmpty() ? "" : str(matInsp.get(0), "inspectionNo") + (matInsp.size() > 1 ? " 等 " + matInsp.size() + " 单" : ""),
                matInsp.isEmpty() ? "" : matInsp.size() + " 单来料检验 · 不合格批次 "
                        + matInsp.stream().filter(q -> bd(q.get("unqualifiedQuantity")).signum() > 0).count(),
                "material_quality", matInsp.isEmpty());
        matInsp.stream().map(q -> str(q, "inspectorName")).filter(s -> !s.isBlank()).distinct()
                .forEach(name -> addPerson(mq, "物料质检员", name));
        for (Map<String, Object> q : matInsp) {
            addRow(mq, str(q, "inspectionNo") + " · " + str(q, "materialName")
                    + " 批次 " + fallback(str(q, "batchNo"), "未记录")
                    + " · " + resultCn(str(q, "inspectionResult"))
                    + " · 检验员 " + fallback(str(q, "inspectorName"), "未记录"));
        }
        chain.add(mq);

        // ⑧ 供应商采购
        List<Map<String, Object>> withPo = materials.stream().filter(m -> m.get("purchaseOrderId") != null).toList();
        Map<String, Object> sup = chainStep("供应商采购",
                withPo.isEmpty() ? "" : str(withPo.get(0), "purchaseOrderNo") + (withPo.size() > 1 ? " 等 " + withPo.size() + " 单" : ""),
                withPo.isEmpty() ? "" : withPo.stream().map(m -> str(m, "supplierName")).filter(s -> !s.isBlank())
                        .distinct().limit(3).reduce((a, b) -> a + "、" + b).orElse(""),
                "supplier", withPo.isEmpty());
        withPo.stream().map(m -> str(m, "purchaserName")).filter(s -> !s.isBlank()).distinct()
                .forEach(name -> addPerson(sup, "采购员", name));
        for (Map<String, Object> m : withPo) {
            addRow(sup, str(m, "supplierName") + " · " + str(m, "purchaseOrderNo")
                    + " · " + str(m, "materialName")
                    + " · 单价 ¥" + decimal(m, "unitPrice")
                    + (m.get("purchaseDate") == null ? "" : " · " + timeText(m.get("purchaseDate")))
                    + (str(m, "supplierContact").isBlank() ? "" : " · 联系人 " + str(m, "supplierContact") + " " + str(m, "supplierPhone")));
        }
        chain.add(sup);

        return chain;
    }

    private Map<String, Object> chainStep(String title, String no, String desc, String type, boolean missing) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("no", missing || no == null || no.isBlank() ? "—" : no);
        m.put("desc", missing ? "未记录" : desc);
        m.put("type", type);
        m.put("missing", missing);
        m.put("people", new ArrayList<Map<String, String>>());
        m.put("details", new ArrayList<Map<String, String>>());
        m.put("rows", new ArrayList<String>());
        return m;
    }

    @SuppressWarnings("unchecked")
    private void addPerson(Map<String, Object> step, String role, String name) {
        if (name == null || name.isBlank()) return;
        ((List<Map<String, String>>) step.get("people")).add(Map.of("role", role, "name", name));
    }

    @SuppressWarnings("unchecked")
    private void addDetail(Map<String, Object> step, String label, String value) {
        if (value == null || value.isBlank() || "null".equals(value)) return;
        ((List<Map<String, String>>) step.get("details")).add(Map.of("label", label, "value", value));
    }

    @SuppressWarnings("unchecked")
    private void addRow(Map<String, Object> step, String line) {
        ((List<String>) step.get("rows")).add(line);
    }

    private String timeText(Object v) {
        if (v == null) return "";
        if (v instanceof LocalDateTime t) return t.format(FMT);
        return String.valueOf(v).replace('T', ' ');
    }

    private String resultCn(String r) {
        return switch (r) {
            case "QUALIFIED", "PASSED", "PASS" -> "合格";
            case "UNQUALIFIED", "FAILED", "FAIL" -> "不合格";
            case "PENDING" -> "待检";
            case "CONCESSION" -> "让步接收";
            case "" -> "-";
            default -> r;
        };
    }

    private String deliveryStatusCn(String s) {
        return switch (s) {
            case "PREPARED" -> "备货中";
            case "SHIPPED" -> "已发货";
            case "DELIVERED", "RECEIVED", "SIGNED" -> "已签收";
            default -> s;
        };
    }

    private Map<String, Object> node(String id, String name, String code, String type, String department,
                                     int score, String summary, List<String> evidences) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id); node.put("name", name); node.put("code", code); node.put("nodeType", type);
        node.put("department", department); node.put("score", score); node.put("riskLevel", riskLevel(score));
        node.put("summary", summary); node.put("evidences", evidences);
        return node;
    }

    private Map<String, Object> link(String source, String target, String label, double strength) {
        return Map.of("source", source, "target", target, "label", label, "strength", strength);
    }

    private Map<String, Object> cause(String name, String department, int score, String evidence, String nodeId) {
        return Map.of("name", name, "department", department, "score", score,
                "riskLevel", riskLevel(score), "evidence", evidence, "nodeId", nodeId);
    }

    private Map<String,Object> cause(String name,String department,Map<String,Object> card,String evidence,String nodeId){Map<String,Object>m=new LinkedHashMap<>();m.put("name",name);m.put("department",department);m.put("score",card.get("score"));m.put("riskLevel",riskLevel((Integer)card.get("score")));m.put("confidence",card.get("confidence"));m.put("dimensions",card.get("dimensions"));m.put("evidence",evidence);m.put("nodeId",nodeId);return m;}
    private void attachScoreCard(List<Map<String,Object>>nodes,String id,Map<String,Object>card){nodes.stream().filter(n->id.equals(n.get("id"))).findFirst().ifPresent(n->{n.put("confidence",card.get("confidence"));n.put("dimensions",card.get("dimensions"));});}

    private Map<String, Object> loss(String name, int base, BigDecimal factor, String formula) {
        BigDecimal amount = BigDecimal.valueOf(base).multiply(factor).setScale(2, RoundingMode.HALF_UP);
        return Map.of("name", name, "amount", amount, "formula", formula, "status", "ESTIMATED");
    }

    private Map<String, Object> action(String department, String title, String priority, String reason) {
        return Map.of("department", department, "title", title, "priority", priority, "reason", reason);
    }

    private Map<String,Object> supplierScoreCard(List<Map<String,Object>> materials,Map<String,Object> stats,Map<String,Object> c){
        List<Map<String,Object>>d=new ArrayList<>();boolean linked=!materials.isEmpty()&&materials.stream().allMatch(m->m.get("purchaseOrderId")!=null);int direct=materials.isEmpty()?0:(linked?30:20);d.add(dimension("direct","直接关联",direct,30,materials.isEmpty()?"未找到工单领料记录":linked?"领料批次均可追溯至采购单和供应商":"存在领料记录，但部分采购来源缺失"));
        int time=materials.isEmpty()?0:14;d.add(dimension("time","时间相关",time,20,materials.isEmpty()?"无采购时间证据":"采购入库批次早于工单领料，时间链成立"));
        String p=str(c,"problemDescription")+str(c,"problemType");boolean match=materials.stream().anyMatch(m->{String n=str(m,"materialName");return(p.contains("显示")||p.contains("亮点")||p.contains("闪")||p.contains("色"))&&(n.contains("LCD")||n.contains("背光")||n.contains("驱动"));});int defect=match?18:6;d.add(dimension("defect","缺陷匹配",defect,20,match?"客户缺陷与 LCD/背光/驱动类物料具有技术关联":"缺陷与领用物料的技术匹配较弱"));
        long batchCases=longNumber(stats==null?null:stats.get("sameBatchCases"));int history=batchCases>=3?18:batchCases==2?12:5;d.add(dimension("history","历史异常",history,20,"同成品批次售后事件 "+batchCases+" 条；当前缺少原料批次总投产量，未换算真实缺陷率"));
        int complete=materials.isEmpty()?2:linked?9:6;d.add(dimension("complete","数据完整度",complete,10,linked?"领料、批次、采购、供应商链路完整":"供应商链路存在缺失"));return scoreCard(d);
    }
    private Map<String,Object> equipmentScoreCard(List<Map<String,Object>> reports,List<Map<String,Object>> alarms,Map<String,Object> c){
        List<Map<String,Object>>d=new ArrayList<>();boolean linked=reports.stream().anyMatch(r->r.get("equipmentId")!=null);int direct=linked?28:0;d.add(dimension("direct","直接关联",direct,30,linked?"生产报工记录直接关联设备":"报工未记录设备"));
        long workOrderAlarms=alarms.stream().filter(a->a.get("workOrderId")!=null).count();int time=alarms.isEmpty()?3:Math.min(20,10+(int)workOrderAlarms*5);d.add(dimension("time","时间相关",time,20,alarms.isEmpty()?"生产时间窗口内未匹配报警":"生产窗口匹配 "+alarms.size()+" 条报警，其中工单直接报警 "+workOrderAlarms+" 条"));
        String p=str(c,"problemDescription")+str(c,"problemType");long matched=alarms.stream().filter(a->alarmMatchesProblem(str(a,"alarmDescription"),p)).count();int defect=matched>0?Math.min(20,10+(int)matched*5):4;d.add(dimension("defect","缺陷匹配",defect,20,matched>0?matched+" 条报警内容与客户缺陷语义匹配":"报警内容与客户问题匹配较弱"));
        long badReports=reports.stream().filter(r->bd(r.get("unqualifiedQuantity")).signum()>0).count();int history=Math.min(20,(int)badReports*5+(alarms.size()>1?5:0));d.add(dimension("history","历史异常",history,20,"存在不合格的设备报工 "+badReports+" 条，关联报警 "+alarms.size()+" 条"));
        int complete=linked?(reports.stream().allMatch(r->r.get("startTime")!=null&&r.get("endTime")!=null)?9:7):2;d.add(dimension("complete","数据完整度",complete,10,"设备报工覆盖 "+reports.size()+" 条，时间字段"+(complete>=9?"完整":"存在缺失")));return scoreCard(d);
    }
    private Map<String,Object> qualityScoreCard(Map<String,Object> c,List<Map<String,Object>> items){
        List<Map<String,Object>>d=new ArrayList<>();boolean linked=c.get("inspectionId")!=null;d.add(dimension("direct","直接关联",linked?30:0,30,linked?"售后工单直接关联质检单 "+str(c,"inspectionNo"):"售后未关联质检单"));
        int time=linked?16:0;d.add(dimension("time","时间相关",time,20,linked?"质检记录属于关联工单和成品批次":"无质检时间链"));
        String status=str(c,"inspectionStatus");BigDecimal sample=bd(c.get("sampleQuantity")),bad=bd(c.get("unqualifiedQuantity"));int defect=("FAILED".equals(status)||"RECHECK_REQUIRED".equals(status))?16:bad.signum()>0?10:3;long failed=items.stream().filter(x->"FAILED".equals(str(x,"result"))).count();defect=Math.min(20,defect+(int)failed*2);d.add(dimension("defect","缺陷匹配",defect,20,"状态 "+status+"，抽检 "+sample.stripTrailingZeros().toPlainString()+"，不合格 "+bad.stripTrailingZeros().toPlainString()+"，失败检测项 "+failed));
        int history=sample.signum()>0?bad.multiply(BigDecimal.valueOf(20)).divide(sample,2,RoundingMode.HALF_UP).min(BigDecimal.valueOf(20)).intValue():0;d.add(dimension("history","历史异常",history,20,"当前质检不合格率用于批内异常强度，尚未接入历史检验基线"));
        long measured=items.stream().filter(x->!str(x,"measuredValue").isBlank()).count();int complete=!linked?0:items.isEmpty()?4:Math.min(10,6+(int)(measured*4/Math.max(1,items.size())));d.add(dimension("complete","数据完整度",complete,10,"检测项 "+items.size()+" 项，已录实测值 "+measured+" 项"));return scoreCard(d);
    }
    private Map<String,Object> processScoreCard(Map<String,Object> c,List<Map<String,Object>> reports){
        List<Map<String,Object>>d=new ArrayList<>();boolean linked=c.get("workOrderId")!=null;d.add(dimension("direct","直接关联",linked?30:0,30,linked?"质检单直接关联生产工单":"未找到生产工单"));
        int time=reports.isEmpty()?0:reports.stream().allMatch(r->r.get("startTime")!=null&&r.get("endTime")!=null)?18:12;d.add(dimension("time","时间相关",time,20,reports.isEmpty()?"无报工时间记录":"报工时间链"+(time==18?"完整":"部分缺失")));
        BigDecimal q=bd(c.get("completedQuantity")),bad=bd(c.get("workUnqualifiedQuantity"));int defect=q.signum()>0?bad.multiply(BigDecimal.valueOf(20)).divide(q,2,RoundingMode.HALF_UP).min(BigDecimal.valueOf(20)).intValue():0;d.add(dimension("defect","缺陷匹配",defect,20,"工单完成 "+q.stripTrailingZeros().toPlainString()+"，不合格 "+bad.stripTrailingZeros().toPlainString()));
        long badReports=reports.stream().filter(r->bd(r.get("unqualifiedQuantity")).signum()>0).count();int history=Math.min(20,(int)badReports*5);d.add(dimension("history","历史异常",history,20,"存在不合格的报工记录 "+badReports+" 条"));
        int complete=!linked?0:reports.isEmpty()?4:reports.stream().allMatch(r->r.get("operatorId")!=null&&r.get("stepId")!=null)?10:7;d.add(dimension("complete","数据完整度",complete,10,"工单、报工、工序和人员字段"+(complete==10?"完整":"存在缺失")));return scoreCard(d);
    }
    private Map<String,Object> dimension(String code,String name,int score,int max,String evidence){return Map.of("code",code,"name",name,"score",Math.min(score,max),"max",max,"evidence",evidence);}
    private Map<String,Object> scoreCard(List<Map<String,Object>> dimensions){int score=dimensions.stream().mapToInt(x->(Integer)x.get("score")).sum();int complete=dimensions.stream().filter(x->"complete".equals(x.get("code"))).mapToInt(x->(Integer)x.get("score")).findFirst().orElse(0);String confidence=complete>=8?"HIGH":complete>=5?"MEDIUM":"LOW";return Map.of("score",clamp(score),"confidence",confidence,"dimensions",dimensions);}
    private boolean alarmMatchesProblem(String alarm,String problem){String a=alarm==null?"":alarm,p=problem==null?"":problem;if(p.contains("闪")||p.contains("刷新"))return a.contains("接触")||a.contains("电")||a.contains("刷新")||a.contains("亮度");if(p.contains("亮点")||p.contains("色")||p.contains("显示"))return a.contains("LCD")||a.contains("色")||a.contains("亮度")||a.contains("贴附");return false;}

    private int qualityScore(Map<String,Object> c,List<Map<String,Object>> items){int s=0;String status=str(c,"inspectionStatus");if("FAILED".equals(status)||"RECHECK_REQUIRED".equals(status))s+=35;BigDecimal sample=bd(c.get("sampleQuantity")),bad=bd(c.get("unqualifiedQuantity"));if(sample.signum()>0)s+=bad.multiply(BigDecimal.valueOf(100)).divide(sample,2,RoundingMode.HALF_UP).min(BigDecimal.valueOf(30)).intValue();long failed=items.stream().filter(x->"FAILED".equals(str(x,"result"))).count();s+=Math.min(25,(int)failed*12);if(items.isEmpty())s+=8;return clamp(s);}
    private int equipmentScore(List<Map<String,Object>> reports,List<Map<String,Object>> alarms){int s=0;for(Map<String,Object>a:alarms)s+="URGENT".equals(str(a,"alarmLevel"))?25:10;for(Map<String,Object>r:reports){BigDecimal q=bd(r.get("completedQuantity")),b=bd(r.get("unqualifiedQuantity"));if(q.signum()>0&&b.divide(q,4,RoundingMode.HALF_UP).compareTo(new BigDecimal("0.02"))>0)s+=10;}return clamp(s);}
    private int processScore(Map<String,Object> c,List<Map<String,Object>> reports){int s=0;BigDecimal q=bd(c.get("completedQuantity")),b=bd(c.get("workUnqualifiedQuantity"));if(q.signum()>0){BigDecimal rate=b.divide(q,4,RoundingMode.HALF_UP);s+=rate.multiply(BigDecimal.valueOf(800)).min(BigDecimal.valueOf(45)).intValue();}if(reports.isEmpty())s+=10;for(Map<String,Object>r:reports)if(r.get("startTime")==null||r.get("endTime")==null)s+=5;return clamp(s);}
    private int supplierScore(List<Map<String,Object>> materials,Map<String,Object> stats,Map<String,Object> c){if(materials.isEmpty())return 18;int s=15;long batchCases=longNumber(stats==null?null:stats.get("sameBatchCases"));if(batchCases>1)s+=Math.min(30,(int)batchCases*10);String problem=str(c,"problemDescription")+str(c,"problemType");for(Map<String,Object>m:materials){String name=str(m,"materialName");if((problem.contains("显示")||problem.contains("亮点")||problem.contains("色"))&&(name.contains("LCD")||name.contains("背光")))s+=25;if(m.get("purchaseOrderId")==null)s+=8;}return clamp(s);}
    private int equipmentNodeScore(Map<String,Object> report,List<Map<String,Object>> alarms){int s=alarms.stream().mapToInt(a->"URGENT".equals(str(a,"alarmLevel"))?25:10).sum();BigDecimal q=bd(report.get("completedQuantity")),b=bd(report.get("unqualifiedQuantity"));if(q.signum()>0)s+=b.divide(q,4,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(600)).intValue();return clamp(s);}
    private int materialNodeScore(Map<String,Object> material,Map<String,Object> c){int s=20;String p=str(c,"problemDescription")+str(c,"problemType"),n=str(material,"materialName");if((p.contains("显示")||p.contains("亮点")||p.contains("色"))&&(n.contains("LCD")||n.contains("背光")))s+=35;if(material.get("purchaseOrderId")==null)s+=8;return clamp(s);}
    private List<String> qualityEvidence(Map<String,Object> c,List<Map<String,Object>> items){List<String>e=new ArrayList<>();e.add("质检状态："+str(c,"inspectionStatus"));e.add("抽检 "+decimal(c,"sampleQuantity")+"，不合格 "+decimal(c,"unqualifiedQuantity"));long failed=items.stream().filter(x->"FAILED".equals(str(x,"result"))).count();e.add(items.isEmpty()?"检测项实测数据缺失":"失败检测项 "+failed+" 项，共 "+items.size()+" 项");return e;}
    private List<String> processEvidence(Map<String,Object> c,List<Map<String,Object>> reports){return List.of("工单完成 "+decimal(c,"completedQuantity"),"工单不合格 "+decimal(c,"workUnqualifiedQuantity"),"真实报工记录 "+reports.size()+" 条");}
    private List<String> equipmentEvidence(Map<String,Object> r,List<Map<String,Object>> alarms){List<String>e=new ArrayList<>();e.add("设备："+str(r,"equipmentName"));e.add("报工不合格 "+decimal(r,"unqualifiedQuantity"));e.add("生产窗口关联报警 "+alarms.size()+" 条");alarms.stream().limit(2).forEach(a->e.add(str(a,"alarmLevel")+"："+str(a,"alarmDescription")));return e;}
    private List<String> aggregateEquipmentEvidence(List<Map<String,Object>> reports,List<Map<String,Object>> alarms){List<String>e=new ArrayList<>();for(Map<String,Object>r:reports){long count=alarms.stream().filter(a->String.valueOf(a.get("equipmentId")).equals(String.valueOf(r.get("equipmentId")))).count();e.add("工序 "+fallback(str(r,"stepName"),str(r,"stepId"))+" · 操作员 "+fallback(str(r,"operatorName"),str(r,"operatorId"))+" · 设备 "+fallback(str(r,"equipmentCode"),"未记录")+" "+str(r,"equipmentName")+" · 不合格 "+decimal(r,"unqualifiedQuantity")+" · 报警 "+count+" 条");}alarms.stream().limit(3).forEach(a->e.add(str(a,"alarmLevel")+"："+str(a,"alarmDescription")));return e;}
    private List<String> materialEvidence(Map<String,Object> m){List<String>e=new ArrayList<>();e.add("实际领用数量："+decimal(m,"consumeQuantity"));e.add("库存流水批次："+fallback(str(m,"materialBatchNo"),"缺失"));e.add(m.get("purchaseOrderId")==null?"未匹配采购入库来源":"采购单："+str(m,"purchaseOrderNo"));return e;}
    private String materialSummary(List<Map<String,Object>> m){if(m.isEmpty())return "未找到工单领料批次，供应商维度置信度较低";long linked=m.stream().filter(x->x.get("purchaseOrderId")!=null).count();return "真实领料 "+m.size()+" 项，其中 "+linked+" 项可追溯采购来源";}
    private String alarmSummary(List<Map<String,Object>> a){return a.isEmpty()?"生产窗口未匹配到安灯报警":"生产窗口匹配到 "+a.size()+" 条真实安灯报警";}
    private String qualitySummary(Map<String,Object> c,List<Map<String,Object>> i){return "质检状态 "+str(c,"inspectionStatus")+"，不合格 "+decimal(c,"unqualifiedQuantity")+"，检测明细 "+i.size()+" 项";}
    private String processSummary(Map<String,Object> c,List<Map<String,Object>> r){return "工单不合格 "+decimal(c,"workUnqualifiedQuantity")+"，报工记录 "+r.size()+" 条";}
    private List<Map<String,Object>> realLosses(Map<String,Object> c,Map<String,Object> settlement,List<Map<String,Object>> alarms){BigDecimal unit=bd(c.get("standardCost")),bad=bd(c.get("unqualifiedQuantity"));if(bad.signum()==0)bad=bd(c.get("workUnqualifiedQuantity"));List<Map<String,Object>>x=new ArrayList<>();x.add(lossAmount("不合格品材料损失",unit.multiply(bad),bad+" 台 × 标准成本 "+unit+" 元"));BigDecimal labor=settlement==null?BigDecimal.ZERO:bd(settlement.get("laborCost")).multiply(bad).divide(bd(settlement.get("totalCost")).max(BigDecimal.ONE),2,RoundingMode.HALF_UP).multiply(unit);x.add(lossAmount("返工人工估算",labor,"根据工单结算人工成本占比估算"));BigDecimal quality=settlement==null?BigDecimal.ZERO:bd(settlement.get("qualityCost")).multiply(bad).divide(bd(c.get("completedQuantity")).max(BigDecimal.ONE),2,RoundingMode.HALF_UP);x.add(lossAmount("复检质量成本",quality,"工单质量成本 ÷ 完成数量 × 不合格数量"));BigDecimal stop=BigDecimal.valueOf(alarms.stream().filter(a->"URGENT".equals(str(a,"alarmLevel"))).count()*300L);x.add(lossAmount("设备异常处置",stop,"紧急报警数量 × 300 元估算"));return x;}
    private Map<String,Object> lossAmount(String name,BigDecimal amount,String formula){return Map.of("name",name,"amount",amount.max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP),"formula",formula,"status","ESTIMATED");}
    private List<Map<String,Object>> ruleActions(List<Map<String,Object>> causes,List<Map<String,Object>> materials,List<Map<String,Object>> reports){List<Map<String,Object>>a=new ArrayList<>();for(Map<String,Object>c:causes){int s=(Integer)c.get("score");if(s<40)continue;String d=String.valueOf(c.get("department"));if("PURCHASE".equals(d))a.add(action(d,"核查并冻结高风险物料批次",s>=70?"URGENT":"HIGH","依据真实领料与采购入库关系开展供应商协查"));if("DEVICE".equals(d))a.add(action(d,"复核关联设备报警和维护状态",s>=70?"URGENT":"HIGH","检查工单生产窗口内的真实报警记录"));if("QUALITY".equals(d))a.add(action(d,"对关联成品批次执行扩大复检","HIGH","补录检测项实测值并复核不合格项"));if("PRODUCTION".equals(d))a.add(action(d,"复核工单报工与工艺执行","HIGH","核查不合格报工、工序和操作记录"));}a.add(action("COST","确认售后质量损失","NORMAL","将规则估算转换为财务确认金额"));return a;}
    private String ruleConclusion(List<Map<String,Object>> causes,Map<String,Object> c){Map<String,Object>top=causes.get(0);return "基于当前 MES 真实数据，"+top.get("name")+"的排查优先级最高（嫌疑分 "+top.get("score")+"/100）。主要证据："+top.get("evidence")+"。该结果用于辅助缩小排查范围，最终根因需由相关部门人工确认。";}
    private int clamp(int s){return Math.max(0,Math.min(100,s));}
    private Long longValue(Object v){if(v==null)return null;return ((Number)v).longValue();}
    private long longNumber(Object v){return v instanceof Number n?n.longValue():0;}
    private BigDecimal bd(Object v){if(v==null)return BigDecimal.ZERO;if(v instanceof BigDecimal b)return b;if(v instanceof Number n)return new BigDecimal(n.toString());try{return new BigDecimal(v.toString());}catch(Exception e){return BigDecimal.ZERO;}}
    private String decimal(Map<String,Object>m,String k){return bd(m.get(k)).stripTrailingZeros().toPlainString();}
    private String fallback(String value,String fallback){return value==null||value.isBlank()?fallback:value;}

    private String riskLevel(int score) {
        if (score >= 85) return "CRITICAL";
        if (score >= 70) return "HIGH";
        if (score >= 50) return "MEDIUM";
        if (score >= 30) return "WATCH";
        return "LOW";
    }

    private String value(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void fmtTime(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof LocalDateTime) row.put(key, ((LocalDateTime) v).format(FMT));
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : "";
    }

    private String statusCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "OPEN"       -> "待受理";
            case "TRACING"    -> "追溯中";
            case "PROCESSING" -> "处理中";
            case "RESOLVED"   -> "已解决";
            case "CLOSED"     -> "已关闭";
            case "CANCELLED"  -> "已取消";
            default -> s;
        };
    }

    private String levelCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "GENERAL"   -> "一般";
            case "IMPORTANT" -> "重要";
            case "URGENT"    -> "紧急";
            case "LOW"       -> "低";
            case "MEDIUM"    -> "中";
            case "HIGH"      -> "高";
            case "CRITICAL"  -> "紧急";
            default -> s;
        };
    }

    private String problemTypeCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "DISPLAY_DEFECT"  -> "显示缺陷";
            case "COLOR_ISSUE"     -> "色彩问题";
            case "DEAD_PIXEL"      -> "坏点/亮点";
            case "INTERFACE_FAULT" -> "接口故障";
            case "APPEARANCE"      -> "外观损伤";
            case "POWER_ISSUE"     -> "电源问题";
            case "OTHER"           -> "其他";
            default -> s;
        };
    }

    private String inspectionStatusCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "PENDING"          -> "待检";
            case "PASSED"           -> "质检通过";
            case "FAILED"           -> "质检不通过";
            case "RECHECK_REQUIRED" -> "需复检";
            case "CLOSED"           -> "已关闭";
            default -> s;
        };
    }

    private String severityCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "MINOR" -> "轻微"; case "GENERAL" -> "一般";
            case "MAJOR" -> "严重"; case "CRITICAL" -> "致命";
            default -> s;
        };
    }

    private String ncHandleStatusCn(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "PENDING" -> "待处置"; case "PROCESSING" -> "处理中"; case "DONE" -> "已处置";
            default -> s;
        };
    }
}
