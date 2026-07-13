package com.upc.computer.service;

import com.upc.computer.dto.LoginRequest;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.dto.RegisterRequest;
import com.upc.computer.entity.User;
import com.upc.computer.vo.SysMenuTree;

import java.util.ArrayList;
import java.util.Map;

/**
 * 认证服务接口
 */
public interface AuthService {

    /** 校验账号密码，返回用户信息（不含 token） */
    LoginResponse authenticate(LoginRequest request);

    /** 用户自助注册，待管理员分配角色后启用 */
    Map<String, Object> register(RegisterRequest request);

    /** 将登录用户信息写入 Redis，key 为 token */
    void cacheLoginSession(String token, LoginResponse session);

    /** 从 Redis 读取登录用户信息 */
    LoginResponse getLoginSession(String token);

    /** 登出时删除 Redis 会话 */
    void removeLoginSession(String token);

    User getCurrentUser(Long userId);

    ArrayList<SysMenuTree> getMenusByRole(Long roleId, String roleCode);
}
