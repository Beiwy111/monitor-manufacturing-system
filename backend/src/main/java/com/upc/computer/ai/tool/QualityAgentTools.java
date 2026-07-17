package com.upc.computer.ai.tool;

import com.upc.computer.service.QualityService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 质量检验和不合格品只读工具。 */
@Component
public class QualityAgentTools {

    private final QualityService qualityService;

    public QualityAgentTools(QualityService qualityService) {
        this.qualityService = qualityService;
    }

    @Tool(name = "quality_kpi", description = "查询质检任务、合格率、不合格和复检 KPI")
    public Object qualityKpi() {
        return qualityService.inspectionKpi();
    }

    @Tool(name = "quality_list_inspections", description = "查询来料、半成品和成品质检任务及状态")
    public Object listInspections() {
        return AgentToolSupport.limit(qualityService.listInspectionViews(), 200);
    }

    @Tool(name = "quality_inspection_detail", description = "根据质检任务数据库主键查询质检详情和检测项")
    public Object inspectionDetail(@ToolParam(description = "质检任务数据库主键") Long inspectionId) {
        return qualityService.getInspectionDetail(
                AgentToolSupport.requiredId(inspectionId, "质检任务ID"));
    }

    @Tool(name = "quality_list_nonconforming", description = "查询不合格品和处置状态")
    public Object listNonconforming() {
        return AgentToolSupport.limit(qualityService.listNonconformingViews(), 200);
    }

    @Tool(name = "quality_list_rechecks", description = "查询需要复检或正在复检的质量任务")
    public Object listRechecks() {
        return AgentToolSupport.limit(qualityService.listRecheckViews(), 200);
    }

    @Tool(name = "quality_evaluate_inspection", description = "根据已经录入的检测项计算并返回质检判定建议，不修改质检状态")
    public Object evaluateInspection(@ToolParam(description = "质检任务数据库主键") Long inspectionId) {
        return qualityService.evaluate(
                AgentToolSupport.requiredId(inspectionId, "质检任务ID"));
    }
}
