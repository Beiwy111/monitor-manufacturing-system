package com.upc.computer.ai.tool;

import com.upc.computer.service.EquipmentService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/** 设备、安灯和维保只读工具。 */
@Component
public class EquipmentAgentTools {

    private final EquipmentService equipmentService;

    public EquipmentAgentTools(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @Tool(name = "equipment_list_status", description = "查询设备台账、运行状态、报警数和维保状态")
    public Object listEquipmentStatus() {
        return AgentToolSupport.limit(equipmentService.equipmentViews(), 150);
    }

    @Tool(name = "equipment_kpi", description = "查询设备数量、故障、报警和维保 KPI")
    public Object equipmentKpi() {
        return equipmentService.equipmentKpi();
    }

    @Tool(name = "equipment_list_alarms", description = "查询安灯报警及报警处理状态")
    public Object listAlarms() {
        return AgentToolSupport.limit(equipmentService.alarmViews(), 150);
    }

    @Tool(name = "equipment_list_maintenance", description = "查询设备维保记录、维护人和处理结果")
    public Object listMaintenance() {
        return AgentToolSupport.limit(equipmentService.maintenanceViews(), 150);
    }

    @Tool(name = "equipment_health_analysis", description = "查询逐台设备健康分、扣分原因和维护建议")
    public Object healthAnalysis() {
        return AgentToolSupport.limit(equipmentService.calcHealthList(), 150);
    }

    @Tool(name = "equipment_workshop_overview", description = "查询显示器八道生产工序和各车间设备状态总览")
    public Object workshopOverview() {
        return equipmentService.workshopOverview();
    }
}
