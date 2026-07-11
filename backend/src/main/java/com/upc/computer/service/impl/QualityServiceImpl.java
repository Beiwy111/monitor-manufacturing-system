package com.upc.computer.service.impl;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.NonconformingProduct;
import com.upc.computer.entity.QualityInspection;
import com.upc.computer.entity.QualityInspectionItem;
import com.upc.computer.entity.Material;
import com.upc.computer.mapper.MaterialMapper;
import com.upc.computer.mapper.NonconformingProductMapper;
import com.upc.computer.mapper.QualityInspectionItemMapper;
import com.upc.computer.mapper.QualityInspectionMapper;
import com.upc.computer.service.MesWorkflowService;
import com.upc.computer.service.QualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class QualityServiceImpl implements QualityService {

    @Autowired private QualityInspectionMapper inspectionMapper;
    @Autowired private NonconformingProductMapper nonconformingMapper;
    @Autowired private QualityInspectionItemMapper itemMapper;
    @Autowired private MaterialMapper materialMapper;
    @Autowired private MesWorkflowService mesWorkflowService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── CRUD ──────────────────────────────────────────────────
    @Override public ArrayList<QualityInspection> inspectionList() { return inspectionMapper.inspectionList(); }
    @Override public QualityInspection getInspectionById(Long id)  { return inspectionMapper.getInspectionById(id); }
    @Override public void insertInspection(QualityInspection i)    { i.setCreatedAt(LocalDateTime.now()); i.setUpdatedAt(LocalDateTime.now()); inspectionMapper.insertInspection(i); }
    @Override public void updateInspection(QualityInspection i)    { i.setUpdatedAt(LocalDateTime.now()); inspectionMapper.updateInspection(i); }
    @Override public void deleteInspection(Long id)                { inspectionMapper.deleteInspection(id); }
    @Override public ArrayList<NonconformingProduct> nonconformingList()  { return nonconformingMapper.nonconformingList(); }
    @Override public NonconformingProduct getNonconformingById(Long id)   { return nonconformingMapper.getNonconformingById(id); }
    @Override public void insertNonconforming(NonconformingProduct n)     { n.setCreatedAt(LocalDateTime.now()); n.setUpdatedAt(LocalDateTime.now()); nonconformingMapper.insertNonconforming(n); }
    @Override public void updateNonconforming(NonconformingProduct n)     { n.setUpdatedAt(LocalDateTime.now()); nonconformingMapper.updateNonconforming(n); }
    @Override public void deleteNonconforming(Long id)                    { nonconformingMapper.deleteNonconforming(id); }

    // ── 视图查询 ──────────────────────────────────────────────
    @Override
    public List<Map<String, Object>> listInspectionViews() {
        List<Map<String, Object>> list = inspectionMapper.listInspectionViews();
        list.forEach(this::enrichInspection);
        return list;
    }

    @Override
    public Map<String, Object> getInspectionDetail(Long inspectionId) {
        Map<String, Object> view = inspectionMapper.getInspectionDetailView(inspectionId);
        if (view == null) return Collections.emptyMap();
        enrichInspection(view);
        List<NonconformingProduct> ncList = nonconformingMapper.listByInspectionId(inspectionId);
        List<Map<String, Object>> ncViews = new ArrayList<>();
        for (NonconformingProduct nc : ncList) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nonconformingId",  nc.getNonconformingId());
            m.put("nonconformingNo",  nc.getNonconformingNo());
            m.put("defectType",       nc.getDefectType());
            m.put("quantity",         nc.getQuantity());
            m.put("severity",         nc.getSeverity());
            m.put("severityCn",       severityCn(nc.getSeverity()));
            m.put("handleStatus",     nc.getHandleStatus());
            m.put("handleStatusCn",   handleStatusCn(nc.getHandleStatus()));
            m.put("handleMethod",     nc.getHandleMethod());
            m.put("handleMethodCn",   handleMethodCn(nc.getHandleMethod()));
            ncViews.add(m);
        }
        view.put("nonconformingList", ncViews);
        view.put("items", listItems(inspectionId));
        return view;
    }

    @Override
    public List<Map<String, Object>> listNonconformingViews() {
        List<Map<String, Object>> list = nonconformingMapper.listNonconformingViews();
        list.forEach(row -> {
            row.put("severityCn",     severityCn(str(row,"severity")));
            row.put("handleStatusCn", handleStatusCn(str(row,"handleStatus")));
            row.put("handleMethodCn", handleMethodCn(str(row,"handleMethod")));
        });
        return list;
    }

    @Override
    public List<Map<String, Object>> listRecheckViews() {
        List<Map<String, Object>> list = inspectionMapper.listRecheckViews();
        list.forEach(this::enrichInspection);
        return list;
    }

    @Override
    public Map<String, Object> inspectionKpi() {
        Map<String, Object> kpi = inspectionMapper.inspectionKpi();
        if (kpi == null) kpi = new HashMap<>();
        long pendingNc = nonconformingMapper.nonconformingList().stream()
                .filter(n -> !"DONE".equals(n.getHandleStatus())).count();
        kpi.put("pendingNonconforming", pendingNc);
        return kpi;
    }
    // ── 检测项 ────────────────────────────────────────────────
    @Override
    public List<Map<String, Object>> listItems(Long inspectionId) {
        List<QualityInspectionItem> items = itemMapper.listByInspectionId(inspectionId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (QualityInspectionItem i : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("inspectionItemId", i.getInspectionItemId());
            m.put("itemCode",         i.getItemCode());
            m.put("itemName",         i.getItemName());
            m.put("standardValue",    i.getStandardValue());
            m.put("measuredValue",    i.getMeasuredValue());
            m.put("unit",             i.getUnit());
            m.put("result",           i.getResult());
            m.put("resultCn",         itemResultCn(i.getResult()));
            m.put("defectLevel",      i.getDefectLevel());
            m.put("defectLevelCn",    defectLevelCn(i.getDefectLevel()));
            m.put("sortOrder",        i.getSortOrder());
            m.put("remark",           i.getRemark());
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional
    public void saveItems(Long inspectionId, List<QualityInspectionItem> items) {
        itemMapper.deleteByInspectionId(inspectionId);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < items.size(); i++) {
            QualityInspectionItem item = items.get(i);
            item.setInspectionId(inspectionId);
            item.setSortOrder(i + 1);
            if (item.getCreatedAt() == null) item.setCreatedAt(now);
            item.setUpdatedAt(now);
            itemMapper.insert(item);
        }
    }

    @Override
    @Transactional
    public List<QualityInspectionItem> generateDefaultItems(Long inspectionId) {
        QualityInspection insp = inspectionMapper.getInspectionById(inspectionId);
        if (insp == null) throw new BusinessException("质检单不存在");
        itemMapper.deleteByInspectionId(inspectionId);

        // 获取物料规格信息，用于差异化标准值
        Material material = insp.getMaterialId() != null
                ? materialMapper.getMaterialById(insp.getMaterialId()) : null;
        String spec = material != null && material.getSpecification() != null
                ? material.getSpecification().toLowerCase() : "";
        String matName = material != null && material.getMaterialName() != null
                ? material.getMaterialName() : "";

        // 根据物料名称/规格识别产品尺寸等级
        DisplaySpec ds = resolveDisplaySpec(matName, spec);

        String type = "FINISHED_PRODUCT".equals(insp.getInspectionCategory())
                ? "FINISHED_PRODUCT"
                : "RAW_MATERIAL".equals(insp.getInspectionCategory())
                    ? "INCOMING"
                    : insp.getInspectionType();
        List<QualityInspectionItem> defaults = buildDefaultItems(type, inspectionId, ds, matName);
        LocalDateTime now = LocalDateTime.now();
        for (QualityInspectionItem item : defaults) {
            item.setCreatedAt(now); item.setUpdatedAt(now);
            itemMapper.insert(item);
        }
        return defaults;
    }

    /** 从物料名称/规格字符串推断显示器规格等级 */
    private DisplaySpec resolveDisplaySpec(String matName, String spec) {
        String combined = (matName + " " + spec).toLowerCase();
        // 尺寸识别
        double inch = 0;
        if (combined.contains("27")) inch = 27;
        else if (combined.contains("23.8") || combined.contains("24")) inch = 23.8;
        else if (combined.contains("15.6") || combined.contains("16")) inch = 15.6;

        // 分辨率识别
        boolean is4K     = combined.contains("4k") || combined.contains("3840") || combined.contains("2160");
        boolean is2K     = combined.contains("2k") || combined.contains("2560") || combined.contains("1440") || combined.contains("qhd");
        boolean is1080p  = combined.contains("1920") || combined.contains("1080") || combined.contains("fhd");

        // 刷新率
        boolean is144hz  = combined.contains("144hz") || combined.contains("144 hz");
        boolean is165hz  = combined.contains("165hz") || combined.contains("165 hz");
        boolean isHighHz = is144hz || is165hz || combined.contains("240hz");

        // HDR
        boolean isHdr    = combined.contains("hdr");

        return new DisplaySpec(inch, is4K, is2K, is1080p, isHighHz, isHdr);
    }

    private record DisplaySpec(double inch, boolean is4K, boolean is2K,
                                boolean is1080p, boolean isHighHz, boolean isHdr) {
        String brightnessStd() {
            if (is4K && isHdr)  return "≥600";
            if (is4K)           return "≥450";
            if (is2K && isHighHz) return "≥350";
            if (isHighHz)       return "≥350";
            return "≥400";
        }
        String srgbStd()  { return is4K ? "≥99%" : is2K ? "≥99%" : "≥99%"; }
        String dcip3Std() { return is4K ? "≥95%" : "≥90%"; }
        String deltaEStd(){ return is4K ? "≤1.5" : is2K ? "≤2"  : "≤2"; }
        String refreshStd(){
            if (isHighHz) return is165hz() ? "165Hz" : "144Hz";
            return is4K ? "60Hz" : "75Hz";
        }
        private boolean is165hz() { return false; } // 精确值在 resolveDisplaySpec 里已判断
        String deadPixelStd(){
            if (is4K) return "亮点≤1，暗点≤1";
            if (is2K) return "亮点≤2，暗点≤1";
            return "亮点≤3，暗点≤2";
        }
        String agingStd(){ return is4K || isHighHz ? "12h无异常" : "8h无异常"; }
        String resolutionLabel(){
            if (is4K)  return "3840×2160";
            if (is2K)  return "2560×1440";
            return "1920×1080";
        }
        String sizeLabel(){
            if (inch == 27)   return "27寸";
            if (inch == 23.8) return "23.8寸";
            if (inch == 15.6) return "15.6寸";
            return "标准";
        }
    }

    @Override
    public Map<String, Object> evaluate(Long inspectionId) {
        List<QualityInspectionItem> items = itemMapper.listByInspectionId(inspectionId);
        if (items.isEmpty()) return Map.of("total",0,"passed",0,"failed",0,"pending",0,
                "suggestedResult","PENDING","suggestedResultCn","待检");
        long failed  = items.stream().filter(i -> "FAILED".equals(i.getResult())).count();
        long passed  = items.stream().filter(i -> "PASSED".equals(i.getResult())).count();
        long warning = items.stream().filter(i -> "WARNING".equals(i.getResult())).count();
        long pending = items.stream().filter(i -> i.getResult()==null||"PENDING".equals(i.getResult())).count();
        boolean hasCritical = items.stream().anyMatch(i->"FAILED".equals(i.getResult())&&"CRITICAL".equals(i.getDefectLevel()));
        boolean hasMajor    = items.stream().anyMatch(i->"FAILED".equals(i.getResult())&&"MAJOR".equals(i.getDefectLevel()));
        String suggested = pending>0 ? "PENDING" : failed==0&&warning==0 ? "PASSED"
                : (hasCritical||hasMajor) ? "FAILED" : failed>0 ? "RECHECK_REQUIRED" : "PASSED";
        return Map.of("total",items.size(),"passed",passed,"failed",failed,"warning",warning,
                "pending",pending,"suggestedResult",suggested,"suggestedResultCn",inspectionStatusCn(suggested));
    }
    // ── 质检状态流转 ──────────────────────────────────────────
    @Override @Transactional
    public QualityInspection passInspection(Long id, String remark, String operator) {
        QualityInspection i = requireInspection(id);
        if ("CLOSED".equals(i.getInspectionStatus())) throw new BusinessException("质检单已关闭");
        int qualQty = resolveQualifiedQty(i);
        i.setQualifiedQuantity(BigDecimal.valueOf(qualQty));
        i.setUnqualifiedQuantity(BigDecimal.ZERO);
        i.setInspectionStatus("PASSED"); i.setInspectionResult("QUALIFIED");
        i.setInspectedAt(LocalDateTime.now()); i.setRemark(remark); i.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateInspection(i);
        mesWorkflowService.afterInspectionPassed(i, operator, "quality");
        return i;
    }

    @Override @Transactional
    public QualityInspection failInspection(Long id, String defectType, String defectReason,
                                            BigDecimal qty, String severity, String remark, String operator) {
        QualityInspection i = requireInspection(id);
        if ("CLOSED".equals(i.getInspectionStatus())) throw new BusinessException("质检单已关闭");
        i.setInspectionStatus("FAILED"); i.setInspectionResult("UNQUALIFIED");
        i.setInspectedAt(LocalDateTime.now()); i.setRemark(remark); i.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateInspection(i);
        createNonconforming(i, defectType, defectReason, qty, severity, operator); return i;
    }

    @Override @Transactional
    public QualityInspection requireRecheck(Long id, String reason, String operator) {
        QualityInspection i = requireInspection(id);
        if ("CLOSED".equals(i.getInspectionStatus())) throw new BusinessException("质检单已关闭");
        i.setInspectionStatus("RECHECK_REQUIRED"); i.setInspectionType("RECHECK");
        i.setRemark(reason); i.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateInspection(i); return i;
    }

    @Override @Transactional
    public QualityInspection recheckPass(Long id, String remark, String operator) {
        QualityInspection i = requireInspection(id);
        if (!"RECHECK_REQUIRED".equals(i.getInspectionStatus()))
            throw new BusinessException("仅需复检状态可执行复检通过");
        int qualQty = resolveQualifiedQty(i);
        i.setQualifiedQuantity(BigDecimal.valueOf(qualQty));
        i.setUnqualifiedQuantity(BigDecimal.ZERO);
        i.setInspectionStatus("PASSED"); i.setInspectionResult("QUALIFIED");
        i.setInspectedAt(LocalDateTime.now()); i.setRemark(remark); i.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateInspection(i);
        mesWorkflowService.afterInspectionPassed(i, operator, "quality");
        return i;
    }

    @Override @Transactional
    public QualityInspection recheckFail(Long id, String defectType, String defectReason,
                                         BigDecimal qty, String severity, String remark, String operator) {
        QualityInspection i = requireInspection(id);
        if (!"RECHECK_REQUIRED".equals(i.getInspectionStatus()))
            throw new BusinessException("仅需复检状态可执行复检不通过");
        i.setInspectionStatus("FAILED"); i.setInspectionResult("UNQUALIFIED");
        i.setInspectedAt(LocalDateTime.now()); i.setRemark(remark); i.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateInspection(i);
        createNonconforming(i, defectType, defectReason, qty, severity, operator); return i;
    }

    @Override @Transactional
    public QualityInspection closeInspection(Long id, String remark, String operator) {
        QualityInspection i = requireInspection(id);
        if ("CLOSED".equals(i.getInspectionStatus())) throw new BusinessException("质检单已关闭");
        i.setInspectionStatus("CLOSED"); i.setRemark(remark); i.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateInspection(i); return i;
    }

    @Override @Transactional
    public NonconformingProduct handleNonconforming(Long ncId, String handleMethod, String remark, String operator) {
        NonconformingProduct nc = nonconformingMapper.getNonconformingById(ncId);
        if (nc == null) throw new BusinessException("不合格品记录不存在");
        if ("DONE".equals(nc.getHandleStatus())) throw new BusinessException("该不良品已处置完成");
        nc.setHandleMethod(handleMethod); nc.setHandleStatus("DONE");
        nc.setHandledAt(LocalDateTime.now()); nc.setRemark(remark); nc.setUpdatedAt(LocalDateTime.now());
        nonconformingMapper.updateNonconforming(nc); return nc;
    }
    @Override @Transactional
    public QualityInspection createIncomingInspection(Long materialId, String batchNo,
                                                      int lotQuantity, int sampleQuantity, String operator) {
        Material mat = materialMapper.getMaterialById(materialId);
        if (mat == null) throw new BusinessException("物料不存在");
        if (!"RAW".equals(mat.getMaterialType())) throw new BusinessException("仅支持原材料来料检验");
        if (batchNo == null || batchNo.isBlank()) throw new BusinessException("批次号不能为空");
        int lot = lotQuantity > 0 ? lotQuantity : 1;
        int sample = sampleQuantity > 0 ? sampleQuantity : Math.max(1, (int) Math.ceil(lot * 0.1));
        if (sample > lot) sample = lot;

        QualityInspection qi = new QualityInspection();
        qi.setInspectionNo("QI" + System.currentTimeMillis());
        qi.setMaterialId(materialId);
        qi.setBatchNo(batchNo.trim());
        qi.setInspectionType("INCOMING");
        qi.setInspectionCategory("RAW_MATERIAL");
        qi.setSampleQuantity(BigDecimal.valueOf(sample));
        qi.setQualifiedQuantity(BigDecimal.ZERO);
        qi.setUnqualifiedQuantity(BigDecimal.ZERO);
        qi.setInspectionStatus("PENDING");
        qi.setInspectionResult("PENDING");
        qi.setRemark("来料批次 " + lot + " 件，抽检 " + sample + " 件");
        qi.setCreatedAt(LocalDateTime.now());
        qi.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.insertInspection(qi);
        return qi;
    }

    @Override @Transactional
    public QualityInspection updateSampling(Long inspectionId, int sampleQty, int qualifiedQty, int unqualifiedQty) {
        QualityInspection i = requireInspection(inspectionId);
        if (!List.of("PENDING", "RECHECK_REQUIRED").contains(i.getInspectionStatus())) {
            throw new BusinessException("当前状态不可修改抽样数据");
        }
        if (sampleQty <= 0) throw new BusinessException("抽样数量须大于 0");
        if (qualifiedQty + unqualifiedQty > sampleQty) {
            throw new BusinessException("合格数与不良数之和不能超过抽样数");
        }
        i.setSampleQuantity(BigDecimal.valueOf(sampleQty));
        i.setQualifiedQuantity(BigDecimal.valueOf(qualifiedQty));
        i.setUnqualifiedQuantity(BigDecimal.valueOf(unqualifiedQty));
        i.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateInspection(i);
        return i;
    }

    // ── 内部工具 ──────────────────────────────────────────────
    private QualityInspection requireInspection(Long id) {
        if (id == null) throw new BusinessException("inspectionId 不能为空");
        QualityInspection i = inspectionMapper.getInspectionById(id);
        if (i == null) throw new BusinessException("质检单不存在: " + id);
        return i;
    }

    private int resolveQualifiedQty(QualityInspection inspection) {
        int qual = inspection.getQualifiedQuantity() != null
                ? inspection.getQualifiedQuantity().intValue() : 0;
        int unqual = inspection.getUnqualifiedQuantity() != null
                ? inspection.getUnqualifiedQuantity().intValue() : 0;
        int sample = inspection.getSampleQuantity() != null
                ? inspection.getSampleQuantity().intValue() : 0;
        if (qual > 0) return qual;
        if (sample > 0 && unqual >= 0) return Math.max(0, sample - unqual);
        int submitQty = mesWorkflowService.inspectionSubmitQty(inspection.getInspectionNo());
        if (submitQty > 0) return submitQty;
        return sample > 0 ? sample : 1;
    }

    private void createNonconforming(QualityInspection insp, String defectType, String defectReason,
                                     BigDecimal qty, String severity, String operator) {
        NonconformingProduct nc = new NonconformingProduct();
        nc.setNonconformingNo("NC" + System.currentTimeMillis());
        nc.setInspectionId(insp.getInspectionId());
        nc.setWorkOrderId(insp.getWorkOrderId());
        nc.setWorkReportId(insp.getWorkReportId());
        nc.setMaterialId(insp.getMaterialId());
        nc.setBatchNo(insp.getBatchNo());
        nc.setDefectType(defectType != null ? defectType : "其他");
        nc.setDefectDescription(defectReason);
        nc.setQuantity(qty != null ? qty : BigDecimal.ONE);
        nc.setSeverity(severity != null ? severity : "GENERAL");
        nc.setHandleMethod("PENDING"); nc.setHandleStatus("PENDING");
        nc.setRegisteredAt(LocalDateTime.now());
        nc.setCreatedAt(LocalDateTime.now()); nc.setUpdatedAt(LocalDateTime.now());
        nonconformingMapper.insertNonconforming(nc);
    }

    private void enrichInspection(Map<String, Object> row) {
        row.put("inspectionStatusCn",   inspectionStatusCn(str(row, "inspectionStatus")));
        row.put("inspectionCategoryCn", categoryCn(str(row, "inspectionCategory")));
        row.put("inspectionTypeCn",     inspectionTypeCn(str(row, "inspectionType")));
        fmtTime(row, "inspectedAt"); fmtTime(row, "updatedAt"); fmtTime(row, "createdAt");
    }

    private void fmtTime(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof LocalDateTime) row.put(key, ((LocalDateTime) v).format(FMT));
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : "";
    }
    private List<QualityInspectionItem> buildDefaultItems(String type, Long inspectionId, DisplaySpec ds, String matName) {
        if (type == null) type = "";
        List<String[]> defs = switch (type.toUpperCase()) {
            case "PANEL_INSPECTION" -> List.of(
                new String[]{"PANEL-01", "外观检查",    "无划痕/污点",           ""},
                new String[]{"PANEL-02", "坏点检测",    ds.deadPixelStd(),       "个"},
                new String[]{"PANEL-03", "亮线/暗线",   "无连续亮线/暗线",        ""},
                new String[]{"PANEL-04", "偏色检测",    "ΔE" + ds.deltaEStd(),  "ΔE"},
                new String[]{"PANEL-05", "分辨率确认",  ds.resolutionLabel(),    ""});
            case "BACKLIGHT_INSPECTION" -> List.of(
                new String[]{"BL-01", "亮度均匀性", "≥80%",                                "%"},
                new String[]{"BL-02", "亮度测试",   ds.brightnessStd(),                    "cd/m²"},
                new String[]{"BL-03", "背光闪烁",   "≥1000Hz或直流",                       "Hz"},
                new String[]{"BL-04", "漏光检测",   "四角无明显漏光",                       ""});
            case "PCB_INSPECTION" -> List.of(
                new String[]{"PCB-01", "焊点检查",  "无虚焊/连焊",   ""},
                new String[]{"PCB-02", "短路测试",  "无短路",        ""},
                new String[]{"PCB-03", "通电测试",  "上电正常无异常",""},
                new String[]{"PCB-04", "电压检测",  "12V±5%",       "V"});
            case "ASSEMBLY_INSPECTION" -> List.of(
                new String[]{"ASM-01", "线缆连接",  "无松动/断线",           ""},
                new String[]{"ASM-02", "结构装配",  "螺丝到位无缺失",         ""},
                new String[]{"ASM-03", "通电显示",  "点亮正常无花屏",         ""},
                new String[]{"ASM-04", "外观检查",  "无刮痕/变形",            ""},
                new String[]{"ASM-05", "尺寸确认",  ds.sizeLabel() + " ±2mm","mm"});
            case "FINISHED_PRODUCT", "FINAL_INSPECTION", "FINAL" -> buildFinishedProductItems(ds, inspectionId);
            case "RECHECK" -> buildFinishedProductItems(ds, inspectionId);
            case "INCOMING" -> buildIncomingItems(matName, inspectionId);
            case "PROCESS", "PROCESS_INSPECTION" -> buildProcessItems(ds, inspectionId);
            default -> List.of(
                new String[]{"QC-01", "外观检查",  "无明显缺陷",      ""},
                new String[]{"QC-02", "功能测试",  "全部功能正常",    ""},
                new String[]{"QC-03", "尺寸检查",  "符合规格要求",    "mm"});
        };

        if (defs instanceof ArrayList) {
            // already mutable
        } else {
            defs = new ArrayList<>(defs);
        }

        List<QualityInspectionItem> items = new ArrayList<>();
        for (int i = 0; i < defs.size(); i++) {
            String[] d = defs.get(i);
            QualityInspectionItem item = new QualityInspectionItem();
            item.setInspectionId(inspectionId);
            item.setItemCode(d[0]); item.setItemName(d[1]);
            item.setStandardValue(d[2]); item.setUnit(d[3]);
            item.setResult("PENDING"); item.setSortOrder(i + 1);
            items.add(item);
        }
        return items;
    }

    /** 半成品过程检 —— 按产品规格生成检测项 */
    private List<String[]> buildProcessItems(DisplaySpec ds, Long inspectionId) {
        List<String[]> defs = new ArrayList<>();
        defs.add(new String[]{"PC-01", "外观检查",    "无划痕/污点/变形",        ""});
        defs.add(new String[]{"PC-02", "坏点检测",    ds.deadPixelStd(),         "个"});
        defs.add(new String[]{"PC-03", "亮度测试",    ds.brightnessStd(),        "cd/m²"});
        defs.add(new String[]{"PC-04", "亮度均匀性",  "≥80%",                    "%"});
        defs.add(new String[]{"PC-05", "色准ΔE",     ds.deltaEStd(),            "ΔE"});
        defs.add(new String[]{"PC-06", "分辨率确认",  ds.resolutionLabel(),      ""});
        defs.add(new String[]{"PC-07", "刷新率",      ds.refreshStd(),           "Hz"});
        defs.add(new String[]{"PC-08", "背光均匀性",  "≥80%",                    "%"});
        defs.add(new String[]{"PC-09", "漏光检测",    "四角无明显漏光",           ""});
        defs.add(new String[]{"PC-10", "线缆连接",    "无松动/断线",              ""});
        return defs;
    }

    /** 来料检 —— 按原材料类型生成检测项 */
    private List<String[]> buildIncomingItems(String matName, Long inspectionId) {
        String name = matName != null ? matName.toLowerCase() : "";
        List<String[]> defs = new ArrayList<>();
        defs.add(new String[]{"IN-01", "外观包装", "无破损/受潮/变形", ""});
        defs.add(new String[]{"IN-02", "规格核对", "与采购订单/图纸一致", ""});
        defs.add(new String[]{"IN-03", "数量清点", "账实相符", "件"});
        defs.add(new String[]{"IN-04", "合格证", "齐全且在有效期内", ""});
        if (name.contains("面板") || name.contains("lcd") || name.contains("ips")) {
            defs.add(new String[]{"IN-05", "坏点抽检", "≤3个亮点/暗点", "个"});
            defs.add(new String[]{"IN-06", "偏色检测", "无明显色偏", ""});
            defs.add(new String[]{"IN-07", "尺寸确认", "符合规格", "mm"});
        } else if (name.contains("背光")) {
            defs.add(new String[]{"IN-05", "亮度抽检", "≥350cd/m²", "cd/m²"});
            defs.add(new String[]{"IN-06", "均匀性", "≥80%", "%"});
            defs.add(new String[]{"IN-07", "驱动电压", "12V±5%", "V"});
        } else if (name.contains("pcb") || name.contains("主控") || name.contains("芯片")) {
            defs.add(new String[]{"IN-05", "焊点检查", "无虚焊/连焊", ""});
            defs.add(new String[]{"IN-06", "通电测试", "上电正常", ""});
            defs.add(new String[]{"IN-07", "版本核对", "与BOM一致", ""});
        } else if (name.contains("边框") || name.contains("电源") || name.contains("适配器")) {
            defs.add(new String[]{"IN-05", "尺寸测量", "符合图纸", "mm"});
            defs.add(new String[]{"IN-06", "材质确认", "与规格一致", ""});
            defs.add(new String[]{"IN-07", "功能抽检", "正常使用", ""});
        } else {
            defs.add(new String[]{"IN-05", "尺寸检测", "符合图纸要求", "mm"});
            defs.add(new String[]{"IN-06", "性能抽检", "满足技术标准", ""});
        }
        return defs;
    }

    /** 成品终检项 —— 按显示器规格生成差异化标准值 */
    private List<String[]> buildFinishedProductItems(DisplaySpec ds, Long inspectionId) {
        List<String[]> defs = new ArrayList<>();
        defs.add(new String[]{"FP-01", "老化测试",    ds.agingStd(),             "h"});
        defs.add(new String[]{"FP-02", "坏点检测",    ds.deadPixelStd(),         "个"});
        defs.add(new String[]{"FP-03", "亮度测试",    ds.brightnessStd(),        "cd/m²"});
        defs.add(new String[]{"FP-04", "色域-sRGB",   ds.srgbStd(),              "%"});
        defs.add(new String[]{"FP-05", "色域-DCI-P3", ds.dcip3Std(),             "%"});
        defs.add(new String[]{"FP-06", "色准ΔE",     ds.deltaEStd(),            "ΔE"});
        defs.add(new String[]{"FP-07", "刷新率",      ds.refreshStd(),           "Hz"});
        defs.add(new String[]{"FP-08", "分辨率",      ds.resolutionLabel(),      ""});
        defs.add(new String[]{"FP-09", "亮度均匀性",  "≥80%",                    "%"});
        defs.add(new String[]{"FP-10", "响应时间",    ds.isHighHz() ? "≤5ms" : "≤8ms", "ms"});
        defs.add(new String[]{"FP-11", "HDMI接口",    "正常输出",                ""});
        defs.add(new String[]{"FP-12", "DP接口",      "正常输出",                ""});
        // 4K产品额外检查 Type-C
        if (ds.is4K() || ds.isHighHz()) {
            defs.add(new String[]{"FP-13", "Type-C接口", "正常输出",              ""});
        }
        if (ds.isHdr()) {
            defs.add(new String[]{"FP-14", "HDR效果",   "峰值亮度≥600cd/m²",    "cd/m²"});
        }
        defs.add(new String[]{"FP-" + (defs.size() + 1), "外观检测", "无划痕/边框平整", ""});
        return defs;
    }

    private String inspectionStatusCn(String s) {
        if (s == null || s.isEmpty()) return "待检";
        return switch (s) {
            case "PENDING" -> "待检"; case "PASSED" -> "质检通过";
            case "FAILED"  -> "质检不通过"; case "RECHECK_REQUIRED" -> "需复检";
            case "CLOSED"  -> "已关闭"; default -> s;
        };
    }

    private String categoryCn(String s) {
        if (s == null || s.isEmpty()) return "-";
        return switch (s) {
            case "SEMI_FINISHED" -> "半成品质检";
            case "RAW_MATERIAL" -> "物料来料检";
            case "FINISHED_PRODUCT" -> "成品质检";
            default -> s;
        };
    }

    private String inspectionTypeCn(String s) {
        if (s == null || s.isEmpty()) return "-";
        return switch (s.toUpperCase()) {
            case "PANEL_INSPECTION"     -> "面板检验";
            case "BACKLIGHT_INSPECTION" -> "背光检验";
            case "PCB_INSPECTION"       -> "PCB检验";
            case "ASSEMBLY_INSPECTION"  -> "装配检验";
            case "PROCESS"              -> "过程检";
            case "FINAL_INSPECTION","FINAL" -> "终检";
            case "INCOMING"             -> "来料检";
            case "RECHECK"              -> "复检";
            case "AGING_TEST"           -> "老化测试";
            case "DEAD_PIXEL_TEST"      -> "坏点测试";
            case "BRIGHTNESS_TEST"      -> "亮度测试";
            case "COLOR_GAMUT_TEST"     -> "色域测试";
            case "COLOR_ACCURACY_TEST"  -> "色准测试";
            case "REFRESH_RATE_TEST"    -> "刷新率测试";
            case "INTERFACE_TEST"       -> "接口检测";
            case "APPEARANCE_TEST"      -> "外观检测";
            default -> s;
        };
    }

    private String severityCn(String s) {
        if (s == null) return "";
        return switch (s) {
            case "MINOR"->"轻微"; case "GENERAL"->"一般"; case "MAJOR"->"严重"; case "CRITICAL"->"致命"; default->s;
        };
    }

    private String handleStatusCn(String s) {
        if (s == null) return "待处置";
        return switch (s) {
            case "PENDING"->"待处置"; case "PROCESSING"->"处理中"; case "DONE"->"已处置"; default->s;
        };
    }

    private String handleMethodCn(String s) {
        if (s == null) return "待确认";
        return switch (s) {
            case "PENDING"->"待确认"; case "REWORK"->"返修"; case "SCRAP"->"报废";
            case "CONCESSION_ACCEPT"->"让步接收"; case "RETURNED"->"退货"; default->s;
        };
    }

    private String itemResultCn(String s) {
        if (s == null || s.isEmpty()) return "待检";
        return switch (s) {
            case "PASSED"->"合格"; case "FAILED"->"不合格"; case "WARNING"->"警告"; case "PENDING"->"待检"; default->s;
        };
    }

    private String defectLevelCn(String s) {
        if (s == null || s.isEmpty()) return "";
        return switch (s) {
            case "MINOR"->"轻微"; case "MAJOR"->"严重"; case "CRITICAL"->"致命"; default->s;
        };
    }
}
