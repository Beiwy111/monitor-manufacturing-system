package com.upc.computer.controller;

import com.upc.computer.common.BusinessException;
import com.upc.computer.common.JwtUtil;
import com.upc.computer.common.Result;
import com.upc.computer.service.RoleWorkbenchDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/workbench/dashboard")
public class RoleWorkbenchDashboardController {

    @Autowired
    private RoleWorkbenchDashboardService dashboardService;
    @Autowired
    private JwtUtil jwtUtil;

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
}
