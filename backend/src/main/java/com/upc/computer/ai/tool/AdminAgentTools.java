package com.upc.computer.ai.tool;

import com.upc.computer.entity.User;
import com.upc.computer.service.AttendanceService;
import com.upc.computer.service.RoleWorkbenchDashboardService;
import com.upc.computer.service.SystemService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理员后台只读工具。 */
@Component
public class AdminAgentTools {

    private final SystemService systemService;
    private final AttendanceService attendanceService;
    private final RoleWorkbenchDashboardService dashboardService;

    public AdminAgentTools(SystemService systemService, AttendanceService attendanceService,
                           RoleWorkbenchDashboardService dashboardService) {
        this.systemService = systemService;
        this.attendanceService = attendanceService;
        this.dashboardService = dashboardService;
    }

    @Tool(name = "admin_dashboard", description = "查询管理员工作台实时汇总、业务指标和异常信息")
    public Map<String, Object> dashboard() {
        return dashboardService.buildDashboard("admin", null, 30, null);
    }

    @Tool(name = "admin_list_users", description = "查询系统用户列表，不返回密码或密码哈希")
    public List<Map<String, Object>> listUsers() {
        return AgentToolSupport.limit(systemService.userList(), 100).stream().map(this::safeUser).toList();
    }

    @Tool(name = "admin_list_roles", description = "查询系统角色列表")
    public Object listRoles() {
        return AgentToolSupport.limit(systemService.roleList(), 100);
    }

    @Tool(name = "admin_list_permissions", description = "查询系统权限配置列表")
    public Object listPermissions() {
        return AgentToolSupport.limit(systemService.permissionList(), 200);
    }

    @Tool(name = "admin_list_menus", description = "查询当前启用的系统菜单树")
    public Object listMenus() {
        return systemService.menuTree();
    }

    @Tool(name = "admin_list_operation_logs", description = "查询最近的系统操作日志")
    public Object listOperationLogs() {
        return AgentToolSupport.limit(systemService.operationLogList(), 100);
    }

    @Tool(name = "admin_attendance_statistics", description = "查询当前月份的员工考勤统计")
    public Object attendanceStatistics() {
        return attendanceService.statistics(YearMonth.now().toString(), null, null);
    }

    @Tool(name = "admin_recent_attendance", description = "查询最近三十天的员工考勤记录")
    public Object recentAttendance() {
        return AgentToolSupport.limit(attendanceService.recordList(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null, null), 200);
    }

    @Tool(name = "admin_upcoming_schedules", description = "查询从今天开始十四天内的员工排班")
    public Object upcomingSchedules() {
        return AgentToolSupport.limit(attendanceService.scheduleList(
                LocalDate.now(), LocalDate.now().plusDays(14)), 200);
    }

    private Map<String, Object> safeUser(User user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userId", user.getUserId());
        row.put("username", user.getUsername());
        row.put("realName", user.getRealName());
        row.put("employeeNo", user.getEmployeeNo());
        row.put("roleId", user.getRoleId());
        row.put("department", user.getDepartment());
        row.put("phone", user.getPhone());
        row.put("email", user.getEmail());
        row.put("status", user.getStatus());
        row.put("lastLoginAt", user.getLastLoginAt());
        return row;
    }
}
