package com.upc.computer.controller;

import com.upc.computer.common.BusinessException;
import com.upc.computer.common.JwtUtil;
import com.upc.computer.common.Result;
import com.upc.computer.dto.LoginRequest;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.dto.RegisterRequest;
import com.upc.computer.service.AuthService;
import com.upc.computer.vo.SysMenuTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器：Controller 生成 token，Service 将会话写入 Redis，Result.data 返回 token
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody LoginRequest request) {
        LoginResponse session = authService.authenticate(request);
        String token = jwtUtil.generateToken(
                session.getUserId(),
                session.getUsername(),
                session.getRoleId(),
                session.getRoleCode()
        );
        authService.cacheLoginSession(token, session);
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = jwtUtil.extractTokenFromHeader(authorization);
        if (StringUtils.hasText(token)) {
            authService.removeLoginSession(token);
        }
        return Result.success(null);
    }

    @GetMapping("/userInfo")
    public Result<LoginResponse> userInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        LoginResponse session = requireLoginSession(authorization);
        return Result.success(session);
    }

    @GetMapping("/menus")
    public Result<ArrayList<SysMenuTree>> menus(@RequestHeader(value = "Authorization", required = false) String authorization) {
        LoginResponse session = requireLoginSession(authorization);
        return Result.success(authService.getMenusByRole(session.getRoleId(), session.getRoleCode()));
    }

    private LoginResponse requireLoginSession(String authorization) {
        String token = jwtUtil.extractTokenFromHeader(authorization);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "未登录或令牌无效");
        }
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "token无效或者过期");
        }
        LoginResponse session = authService.getLoginSession(token);
        if (session == null) {
            throw new BusinessException(401, "登录已失效，请重新登录");
        }
        return session;
    }
}
