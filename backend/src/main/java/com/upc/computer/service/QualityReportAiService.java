package com.upc.computer.service;

import com.upc.computer.entity.QualityInspection;
import com.upc.computer.entity.WorkOrder;
import com.upc.computer.mapper.QualityInspectionMapper;
import com.upc.computer.mapper.WorkOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 质检报告 AI 生成：汇总批次/本日统计，调用千问生成质量分析报告。
 */
@Service
public class QualityReportAiService {

    @Autowired
    private QwenAiService qwenAiService;
    @Autowired
    private QualityInspectionMapper qualityInspectionMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;

    /**
     * 构建本日质量汇总统计。
     */
    public Map<String, Object> buildDailyStats(LocalDate date) {
        List<QualityInspection> todayList = qualityInspectionMapper.inspectionList().stream()
                .filter(i -> i.getInspectionResult() != null && !"PENDING".equals(i.getInspectionResult()))
                .filter(i -> {
                    if (i.getInspectedAt() != null) {
                        return i.getInspectedAt().toLocalDate().equals(date);
                    }
                    return i.getUpdatedAt() != null && i.getUpdatedAt().toLocalDate().equals(date);
                })
                .toList();

        int totalSample = 0;
        int totalQualified = 0;
        int totalUnqualified = 0;
        Map<String, Integer> defectDist = new LinkedHashMap<>();
        Set<String> workOrders = new LinkedHashSet<>();

        for (QualityInspection insp : todayList) {
            int sample = intVal(insp.getSampleQuantity());
            int qualified = intVal(insp.getQualifiedQuantity());
            int unqualified = intVal(insp.getUnqualifiedQuantity());
            totalSample += sample;
            totalQualified += qualified;
            totalUnqualified += unqualified;

            if (unqualified > 0) {
                String type = insp.getInspectionType() != null ? insp.getInspectionType() : "未分类";
                defectDist.merge(type + "不良", unqualified, Integer::sum);
            }

            WorkOrder wo = workOrderMapper.getWorkOrderById(insp.getWorkOrderId());
            if (wo != null) {
                workOrders.add(wo.getWorkOrderNo());
            }
        }

        double yieldRate = totalSample > 0 ? totalQualified * 100.0 / totalSample : 100.0;
        String topDefect = defectDist.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("无明显不良");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("date", date.toString());
        stats.put("inspectionCount", todayList.size());
        stats.put("totalSample", totalSample);
        stats.put("totalQualified", totalQualified);
        stats.put("totalUnqualified", totalUnqualified);
        stats.put("yieldRate", round1(yieldRate));
        stats.put("topDefect", topDefect);
        stats.put("defectDistribution", defectDist);
        stats.put("relatedWorkOrders", new ArrayList<>(workOrders));
        return stats;
    }

    /**
     * 生成 AI 质量分析报告，失败时返回模板报告。
     */
    public AiReportResult generateAnalysis(Map<String, Object> batchStats, Map<String, Object> dailyStats) {
        String template = buildTemplateAnalysis(batchStats, dailyStats);
        String systemPrompt = """
                你是显示器制造企业的质量工程师。请根据提供的质检统计数据，撰写一份简洁、专业的质量分析报告。
                要求：
                1. 使用中文，200-350字
                2. 分三段，每段以标题开头：【质量概况】【主要问题】【改进建议】
                3. 结合本批次与本日数据，给出可执行的改进措施
                4. 不要编造未提供的数据
                """;

        String userPrompt = buildUserPrompt(batchStats, dailyStats);
        String aiText = qwenAiService.chat(systemPrompt, userPrompt);

        if (aiText != null && !aiText.isBlank()) {
            return new AiReportResult(aiText, "AI", parseSections(aiText), true);
        }
        return new AiReportResult(template, "TEMPLATE", parseSections(template), false);
    }

    private String buildUserPrompt(Map<String, Object> batch, Map<String, Object> daily) {
        StringBuilder sb = new StringBuilder();
        sb.append("【本批次质检】\n");
        sb.append("- 质检单：").append(batch.get("qcId")).append("\n");
        sb.append("- 工单：").append(batch.get("workOrderId")).append("\n");
        sb.append("- 产品型号：").append(batch.get("productModel")).append("\n");
        sb.append("- 批次号：").append(batch.get("batchNo")).append("\n");
        sb.append("- 判定结果：").append(batch.get("result")).append("\n");
        sb.append("- 检测总数：").append(batch.get("sampleQty")).append("\n");
        sb.append("- 合格数：").append(batch.get("qualifiedQty")).append("\n");
        sb.append("- 不合格数：").append(batch.get("unqualifiedQty")).append("\n");
        sb.append("- 合格率：").append(batch.get("yieldRate")).append("%\n");
        sb.append("- 主要缺陷类型：").append(batch.get("topDefect")).append("\n");
        sb.append("- 不良分布：").append(batch.get("defectDistribution")).append("\n");

        sb.append("\n【本日质量汇总】\n");
        sb.append("- 统计日期：").append(daily.get("date")).append("\n");
        sb.append("- 本日质检批次数：").append(daily.get("inspectionCount")).append("\n");
        sb.append("- 检测总数：").append(daily.get("totalSample")).append("\n");
        sb.append("- 合格数：").append(daily.get("totalQualified")).append("\n");
        sb.append("- 不合格数：").append(daily.get("totalUnqualified")).append("\n");
        sb.append("- 合格率：").append(daily.get("yieldRate")).append("%\n");
        sb.append("- 主要缺陷类型：").append(daily.get("topDefect")).append("\n");
        sb.append("- 涉及工单：").append(daily.get("relatedWorkOrders")).append("\n");
        return sb.toString();
    }

    private String buildTemplateAnalysis(Map<String, Object> batch, Map<String, Object> daily) {
        double batchYield = toDouble(batch.get("yieldRate"));
        double dailyYield = toDouble(daily.get("yieldRate"));
        int unqualified = intValObj(batch.get("unqualifiedQty"));
        String topDefect = String.valueOf(batch.get("topDefect"));
        String result = String.valueOf(batch.get("result"));

        StringBuilder sb = new StringBuilder();
        sb.append("【质量概况】");
        sb.append(String.format("本批次（%s）质检判定为%s，抽检 %s 件，合格 %s 件，不合格 %s 件，合格率 %.1f%%。",
                batch.get("batchNo"), result, batch.get("sampleQty"),
                batch.get("qualifiedQty"), batch.get("unqualifiedQty"), batchYield));
        sb.append(String.format("本日共完成 %s 批次质检，累计检测 %s 件，整体合格率 %.1f%%，涉及工单 %s。",
                daily.get("inspectionCount"), daily.get("totalSample"), dailyYield,
                daily.get("relatedWorkOrders")));

        sb.append("\n\n【主要问题】");
        if (unqualified <= 0) {
            sb.append("本批次未发现明显质量缺陷，本日整体质量表现稳定。");
        } else {
            sb.append(String.format("本批次不良主要集中在「%s」，需关注对应工序的工艺稳定性与设备状态。", topDefect));
            if (dailyYield < 95) {
                sb.append("本日整体合格率低于 95%，存在批量质量波动风险。");
            }
        }

        sb.append("\n\n【改进建议】");
        if (unqualified <= 0 && dailyYield >= 98) {
            sb.append("维持现有工艺参数与抽检标准，做好批次追溯记录。");
        } else {
            sb.append(String.format("1）针对「%s」开展工序复盘与加严抽检；", topDefect));
            sb.append("2）核对设备点检与操作记录，排查共性原因；");
            if (dailyYield < 95) {
                sb.append("3）建议生产、工艺、质量部门联合召开质量分析会。");
            } else {
                sb.append("3）对同型号在制工单提高巡检频次。");
            }
        }
        return sb.toString();
    }

    private Map<String, String> parseSections(String text) {
        Map<String, String> sections = new LinkedHashMap<>();
        String[] keys = {"质量概况", "主要问题", "改进建议"};
        for (int i = 0; i < keys.length; i++) {
            String marker = "【" + keys[i] + "】";
            int start = text.indexOf(marker);
            if (start < 0) {
                continue;
            }
            int contentStart = start + marker.length();
            int end = text.length();
            for (int j = i + 1; j < keys.length; j++) {
                int next = text.indexOf("【" + keys[j] + "】", contentStart);
                if (next > start) {
                    end = next;
                    break;
                }
            }
            sections.put(keys[i], text.substring(contentStart, end).trim());
        }
        if (sections.isEmpty()) {
            sections.put("分析报告", text.trim());
        }
        return sections;
    }

    private int intVal(java.math.BigDecimal v) {
        return v != null ? v.intValue() : 0;
    }

    private int intValObj(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double toDouble(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public record AiReportResult(String fullText, String source, Map<String, String> sections, boolean aiGenerated) {
    }
}
