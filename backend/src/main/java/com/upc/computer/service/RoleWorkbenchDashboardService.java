package com.upc.computer.service;

import java.util.Map;

/**
 * 各角色工作台大屏数据（来自数据库实时聚合）
 */
public interface RoleWorkbenchDashboardService {

    Map<String, Object> buildDashboard(String roleKey, Long userId, int days, String statusFilter);
}
