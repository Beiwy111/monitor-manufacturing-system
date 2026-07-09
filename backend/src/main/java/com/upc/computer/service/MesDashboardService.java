package com.upc.computer.service;

import java.util.Map;

/**
 * 生产主管大屏数据服务
 */
public interface MesDashboardService {

    Map<String, Object> getSnapshot();

    Map<String, Object> getKpi();

    Map<String, Object> getStations();

    Map<String, Object> getWorkOrderProgress();

    Map<String, Object> getEquipmentStatus();

    Map<String, Object> getAlarms();

    Map<String, Object> getQualityIssues();

    Map<String, Object> getHourlyOutputTrend();

    Map<String, Object> getYieldTrend();

    Map<String, Object> getDowntimeReasons();

    Map<String, Object> getWorkshops3d();

    void ensureInitialized();
}
