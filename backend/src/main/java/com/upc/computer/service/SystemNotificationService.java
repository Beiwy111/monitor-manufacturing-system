package com.upc.computer.service;

import com.upc.computer.common.BusinessException;
import com.upc.computer.mapper.SystemNotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 持久化业务消息，并按登录角色提供消息中心收件箱。 */
@Service
public class SystemNotificationService {

    private final SystemNotificationMapper mapper;

    public SystemNotificationService(SystemNotificationMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public Map<String, Object> sendGlobalAnalysisAction(Map<String, Object> request,
                                                        Long senderUserId,
                                                        String senderUsername) {
        String department = limited(request.get("department"), 100);
        String action = limited(request.get("action"), 500);
        String basis = limited(request.get("basis"), 800);
        String priority = normalizePriority(request.get("priority"));
        if (department.isBlank()) throw new BusinessException("行动建议缺少负责部门");
        if (action.isBlank()) throw new BusinessException("行动建议内容不能为空");

        Target target = resolveTarget(department);
        String businessId = "GLOBAL-AI-" + System.currentTimeMillis();
        String title = "AI全局分析行动提醒（" + priority + "）";
        StringBuilder content = new StringBuilder("管理员通过AI全局分析发起提醒：").append(action);
        if (!basis.isBlank()) content.append("。分析依据：").append(basis);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("receiverRole", target.roleCode());
        row.put("title", title);
        row.put("content", limited(content, 1000));
        row.put("level", "P1".equals(priority) ? "HIGH" : "P2".equals(priority) ? "MEDIUM" : "INFO");
        row.put("businessType", "AI_GLOBAL_ACTION");
        row.put("businessId", businessId);
        row.put("targetPath", target.path());
        row.put("senderUserId", senderUserId);
        row.put("senderUsername", limited(senderUsername, 80));
        mapper.insert(row);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("notificationId", row.get("notificationId"));
        result.put("targetRole", target.roleCode());
        result.put("targetDepartment", target.departmentName());
        result.put("targetPath", target.path());
        result.put("title", title);
        return result;
    }

    public List<Map<String, Object>> listForRole(String roleCode) {
        String normalized = limited(roleCode, 30).toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) throw new BusinessException(401, "登录角色无效");
        return mapper.listForRole(normalized, 200);
    }

    private Target resolveTarget(String department) {
        String value = department.replaceAll("\\s+", "");
        if (contains(value, "订单", "审核")) return new Target("ORDER", "订单管理部", "/order/audit");
        if (contains(value, "计划", "排产")) return new Target("PLANNER", "计划部", "/production/plan");
        if (contains(value, "质检", "质量", "品控")) return new Target("QC", "质量部", "/quality/inspection");
        if (contains(value, "采购", "供应")) return new Target("PURCHASER", "采购部", "/purchase/demand");
        if (contains(value, "仓储", "仓库", "库存")) return new Target("WAREHOUSE", "仓储部", "/warehouse/alert");
        if (contains(value, "设备", "维保", "维修")) return new Target("DEVICE", "设备维护部", "/device/alarm");
        if (contains(value, "售后", "客服")) return new Target("SERVICE", "售后部", "/dashboard/aftersale");
        if (contains(value, "财务", "成本", "结算")) return new Target("COST", "财务部", "/cost/report");
        if (contains(value, "生产", "车间", "运营")) return new Target("MANAGER", "生产部", "/production/work-order");
        if (contains(value, "系统", "安全", "权限")) return new Target("ADMIN", "系统管理部", "/system/log");
        throw new BusinessException("无法识别建议行动对应的部门：" + department);
    }

    private boolean contains(String value, String... keywords) {
        for (String keyword : keywords) if (value.contains(keyword)) return true;
        return false;
    }

    private String normalizePriority(Object value) {
        String priority = limited(value, 10).toUpperCase(Locale.ROOT);
        return List.of("P1", "P2", "P3").contains(priority) ? priority : "P2";
    }

    private String limited(Object value, int maxLength) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private record Target(String roleCode, String departmentName, String path) {
    }
}
