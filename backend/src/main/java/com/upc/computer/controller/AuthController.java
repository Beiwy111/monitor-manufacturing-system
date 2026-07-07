package com.upc.computer.controller;

import com.upc.computer.common.JwtUtil;
import com.upc.computer.common.Result;
import com.upc.computer.dto.LoginRequest;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.entity.User;
import com.upc.computer.service.AuthService;
import com.upc.computer.vo.SysMenuTree;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

/**
 * 认证控制器（前后端分离专用，返回 Result 包装）
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    // 用户登录
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    // 获取当前用户信息
    @GetMapping("/userInfo")
    public Result<User> userInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = parseUserId(authorization);
        return Result.success(authService.getCurrentUser(userId));
    }

    // 获取当前角色菜单树
    @GetMapping("/menus")
    public Result<ArrayList<SysMenuTree>> menus(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Claims claims = parseClaims(authorization);
        Long roleId = claims.get("roleId", Long.class);
        String roleCode = claims.get("roleCode", String.class);
        return Result.success(authService.getMenusByRole(roleId, roleCode));
    }

    private Long parseUserId(String authorization) {
        Claims claims = parseClaims(authorization);
        return claims.get("userId", Long.class);
    }

    private Claims parseClaims(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new com.upc.computer.common.BusinessException(401, "未登录或令牌无效");
        }
        String token = authorization.substring(7);
        return jwtUtil.parseToken(token);
    }
}
