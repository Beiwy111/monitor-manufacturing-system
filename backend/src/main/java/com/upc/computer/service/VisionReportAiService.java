package com.upc.computer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YOLO 外观检测报告：根据多图检测结果调用千问生成分析，失败时模板兜底。
 */
@Service
public class VisionReportAiService {

    @Autowired
    private QwenAiService qwenAiService;

    @SuppressWarnings("unchecked")
    public Map<String, Object> generate(Map<String, Object> body) {
        Map<String, Object> context = asMap(body.get("context"));
        List<Map<String, Object>> images = asListOfMaps(body.get("images"));

        int total = images.size();
        int defectImages = 0;
        int defectCount = 0;
        double maxConfidence = 0;
        List<String> defectNames = new ArrayList<>();

        for (Map<String, Object> img : images) {
            boolean defect = Boolean.TRUE.equals(img.get("defect"));
            int count = toInt(img.get("count"));
            double conf = toDouble(img.get("maxConfidence"));
            if (defect) {
                defectImages++;
                defectCount += Math.max(count, 1);
                String name = str(img.get("name"));
                if (!name.isBlank()) defectNames.add(name);
            }
            maxConfidence = Math.max(maxConfidence, conf);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalImages", total);
        stats.put("defectImages", defectImages);
        stats.put("normalImages", Math.max(0, total - defectImages));
        stats.put("defectCount", defectCount);
        stats.put("maxConfidence", round2(maxConfidence));
        stats.put("passRate", total > 0 ? round1((total - defectImages) * 100.0 / total) : 100.0);
        stats.put("verdict", defectImages > 0 ? "发现外观缺陷" : "外观检测正常");

        String template = buildTemplate(context, stats, images, defectNames);
        String systemPrompt = """
                你是显示器制造企业的外观质量工程师。请根据 YOLO 外观检测结果，撰写一份简洁专业的外观检测报告。
                要求：
                1. 使用中文，180-320字
                2. 分三段，每段以标题开头：【检测概况】【主要问题】【处置建议】
                3. 只依据提供的检测数据，不要编造未提供的信息
                4. 强调这是 AI 辅助结论，最终判定需质检员确认
                """;
        String userPrompt = buildUserPrompt(context, stats, images);
        String aiText = qwenAiService.chat(systemPrompt, userPrompt);

        boolean aiGenerated = aiText != null && !aiText.isBlank();
        String fullText = aiGenerated ? aiText.trim() : template;
        Map<String, String> sections = parseSections(fullText);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("aiGenerated", aiGenerated);
        report.put("reportSource", aiGenerated ? "AI" : "TEMPLATE");
        report.put("fullText", fullText);
        report.put("sections", sections);
        report.put("summary", firstNonBlank(sections.get("检测概况"), stats.get("verdict").toString()));
        report.put("conclusion", stats.get("verdict"));
        report.put("riskPoints", firstNonBlank(sections.get("主要问题"), defectImages > 0 ? "存在外观缺陷，需人工复核" : "未见明显外观风险"));
        report.put("suggestions", firstNonBlank(sections.get("处置建议"), "结合人工目视确认后写入质检结论"));
        report.put("stats", stats);
        report.put("context", context);
        return report;
    }

    private String buildUserPrompt(Map<String, Object> context, Map<String, Object> stats,
                                   List<Map<String, Object>> images) {
        StringBuilder sb = new StringBuilder();
        sb.append("【检测对象】\n");
        sb.append("- 质检单号：").append(str(context.get("inspectionNo"))).append('\n');
        sb.append("- 产品：").append(str(context.get("materialName"))).append('\n');
        sb.append("- 批次：").append(str(context.get("batchNo"))).append('\n');
        sb.append("- 序列号：").append(str(context.get("serialNo"))).append('\n');

        sb.append("\n【汇总】\n");
        sb.append("- 图片总数：").append(stats.get("totalImages")).append('\n');
        sb.append("- 缺陷图片数：").append(stats.get("defectImages")).append('\n');
        sb.append("- 正常图片数：").append(stats.get("normalImages")).append('\n');
        sb.append("- 缺陷区域总数：").append(stats.get("defectCount")).append('\n');
        sb.append("- 最高置信度：").append(stats.get("maxConfidence")).append('\n');
        sb.append("- 外观通过率：").append(stats.get("passRate")).append("%\n");

        sb.append("\n【分图结果】\n");
        int i = 1;
        for (Map<String, Object> img : images) {
            sb.append(i++).append(". ")
                    .append(str(img.get("name")))
                    .append(" | ")
                    .append(Boolean.TRUE.equals(img.get("defect")) ? "缺陷" : "正常")
                    .append(" | 区域数=").append(toInt(img.get("count")))
                    .append(" | 置信度=").append(round2(toDouble(img.get("maxConfidence"))))
                    .append(" | ").append(str(img.get("summary")))
                    .append('\n');
        }
        return sb.toString();
    }

    private String buildTemplate(Map<String, Object> context, Map<String, Object> stats,
                                 List<Map<String, Object>> images, List<String> defectNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("【检测概况】");
        sb.append(String.format("质检单 %s，产品 %s，批次 %s。本次 YOLO 外观共检测 %s 张图片，其中缺陷图 %s 张、正常图 %s 张，缺陷区域合计 %s 处，最高置信度 %.2f，外观通过率 %.1f%%。",
                str(context.get("inspectionNo")),
                str(context.get("materialName")),
                str(context.get("batchNo")),
                stats.get("totalImages"),
                stats.get("defectImages"),
                stats.get("normalImages"),
                stats.get("defectCount"),
                toDouble(stats.get("maxConfidence")),
                toDouble(stats.get("passRate"))));

        sb.append("\n\n【主要问题】");
        int defectImages = toInt(stats.get("defectImages"));
        if (defectImages <= 0) {
            sb.append("未检出明显屏幕划痕或表面缺陷，建议结合人工目视做最终确认。");
        } else {
            sb.append("检测到外观缺陷，主要集中在：");
            sb.append(String.join("、", defectNames.stream().limit(5).toList()));
            sb.append("。请重点复核对应照片的标注区域。");
        }

        sb.append("\n\n【处置建议】");
        if (defectImages <= 0) {
            sb.append("可在人工确认后写入「外观正常」备注；若为成品抽检，维持现有抽检频次即可。");
        } else {
            sb.append("1）对缺陷图片做人工复核并记录缺陷位置；");
            sb.append("2）必要时触发返工/复检或登记不合格品；");
            sb.append("3）同批次其余产品可提高外观抽检比例。");
        }
        return sb.toString();
    }

    private Map<String, String> parseSections(String text) {
        Map<String, String> sections = new LinkedHashMap<>();
        String[] keys = {"检测概况", "主要问题", "处置建议", "质量概况", "改进建议"};
        for (int i = 0; i < keys.length; i++) {
            String marker = "【" + keys[i] + "】";
            int start = text.indexOf(marker);
            if (start < 0) continue;
            int contentStart = start + marker.length();
            int end = text.length();
            for (int j = i + 1; j < keys.length; j++) {
                int next = text.indexOf("【" + keys[j] + "】", contentStart);
                if (next > start) {
                    end = next;
                    break;
                }
            }
            String key = switch (keys[i]) {
                case "质量概况" -> "检测概况";
                case "改进建议" -> "处置建议";
                default -> keys[i];
            };
            sections.putIfAbsent(key, text.substring(contentStart, end).trim());
        }
        if (sections.isEmpty()) {
            sections.put("分析报告", text.trim());
        }
        return sections;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object v) {
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, val) -> out.put(String.valueOf(k), val));
            return out;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMaps(Object v) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!(v instanceof List<?> list)) return out;
        for (Object item : list) {
            if (item instanceof Map<?, ?>) out.add(asMap(item));
        }
        return out;
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : (b == null ? "" : b);
    }

    private int toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
