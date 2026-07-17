package com.upc.computer.controller;

import com.upc.computer.common.BusinessException;
import com.upc.computer.common.JwtUtil;
import com.upc.computer.common.Result;
import com.upc.computer.service.RoleWorkbenchDashboardService;
import com.upc.computer.service.FinanceAiAnalysisService;
import com.upc.computer.service.GlobalAiAnalysisService;
import com.upc.computer.service.SystemNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workbench/dashboard")
public class RoleWorkbenchDashboardController {

    @Autowired
    private RoleWorkbenchDashboardService dashboardService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private FinanceAiAnalysisService financeAiAnalysisService;
    @Autowired
    private GlobalAiAnalysisService globalAiAnalysisService;
    @Autowired
    private SystemNotificationService systemNotificationService;

    @GetMapping
    public Result<Map<String, Object>> dashboard(
            @RequestParam String role,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!StringUtils.hasText(role)) {
            throw new BusinessException("role 不能为空");
        }
        Long userId = resolveUserId(authorization);
        return Result.success(dashboardService.buildDashboard(role.trim().toLowerCase(), userId, days, status));
    }

    @PostMapping("/cost/ai-analysis")
    public Result<Map<String, Object>> financeAiAnalysis(
            @RequestParam(defaultValue = "7") int days,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = requireToken(authorization);
        String roleCode = jwtUtil.parseToken(token).get("roleCode", String.class);
        if (!"COST".equalsIgnoreCase(roleCode)) {
            throw new BusinessException(403, "仅财务角色可以生成 AI 财务分析");
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        return Result.success(financeAiAnalysisService.generate(userId, days));
    }

    @PostMapping("/admin/ai-analysis")
    public Result<Map<String, Object>> globalAiAnalysis(
            @RequestParam(defaultValue = "7") int days,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = requireToken(authorization);
        String roleCode = jwtUtil.parseToken(token).get("roleCode", String.class);
        if (!"ADMIN".equalsIgnoreCase(roleCode)) {
            throw new BusinessException(403, "仅管理员可以生成 AI 全局分析");
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        return Result.success(globalAiAnalysisService.generate(userId, days));
    }

    /** 管理员将全局分析中的一条行动建议发送给对应部门。 */
    @PostMapping("/admin/ai-analysis/notify")
    public Result<Map<String, Object>> notifyGlobalAiAction(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = requireToken(authorization);
        String roleCode = jwtUtil.parseToken(token).get("roleCode", String.class);
        if (!"ADMIN".equalsIgnoreCase(roleCode)) {
            throw new BusinessException(403, "仅管理员可以发送 AI 全局分析行动提醒");
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.parseToken(token).get("username", String.class);
        return Result.success(systemNotificationService.sendGlobalAnalysisAction(body, userId, username));
    }

    /** 当前登录角色的持久化业务消息，由网页消息中心定时同步。 */
    @GetMapping("/notifications")
    public Result<List<Map<String, Object>>> notifications(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = requireToken(authorization);
        String roleCode = jwtUtil.parseToken(token).get("roleCode", String.class);
        return Result.success(systemNotificationService.listForRole(roleCode));
    }

    private Long resolveUserId(String authorization) {
        String token = jwtUtil.extractTokenFromHeader(authorization);
        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            return null;
        }
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private String requireToken(String authorization) {
        String token = jwtUtil.extractTokenFromHeader(authorization);
        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "未登录或令牌无效");
        }
        return token;
    }
}
