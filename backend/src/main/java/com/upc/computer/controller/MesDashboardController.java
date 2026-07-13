package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.service.MesDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 生产主管大屏接口
 */
@RestController
@RequestMapping("/mes/dashboard")
public class MesDashboardController {

    @Autowired
    private MesDashboardService mesDashboardService;

    @GetMapping("/snapshot")
    public Result<Map<String, Object>> snapshot() {
        return Result.success(mesDashboardService.getSnapshot());
    }

    @GetMapping("/kpi")
    public Result<Map<String, Object>> kpi() {
        return Result.success(mesDashboardService.getKpi());
    }

    @GetMapping("/stations")
    public Result<Map<String, Object>> stations() {
        return Result.success(mesDashboardService.getStations());
    }

    @GetMapping("/work-orders")
    public Result<Map<String, Object>> workOrders() {
        return Result.success(mesDashboardService.getWorkOrderProgress());
    }

    @GetMapping("/equipment")
    public Result<Map<String, Object>> equipment() {
        return Result.success(mesDashboardService.getEquipmentStatus());
    }

    @GetMapping("/alarms")
    public Result<Map<String, Object>> alarms() {
        return Result.success(mesDashboardService.getAlarms());
    }

    @GetMapping("/quality-issues")
    public Result<Map<String, Object>> qualityIssues() {
        return Result.success(mesDashboardService.getQualityIssues());
    }

    @GetMapping("/hourly-output")
    public Result<Map<String, Object>> hourlyOutput() {
        return Result.success(mesDashboardService.getHourlyOutputTrend());
    }

    @GetMapping("/yield-trend")
    public Result<Map<String, Object>> yieldTrend() {
        return Result.success(mesDashboardService.getYieldTrend());
    }

    @GetMapping("/downtime-reasons")
    public Result<Map<String, Object>> downtimeReasons() {
        return Result.success(mesDashboardService.getDowntimeReasons());
    }
}
