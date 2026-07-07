package com.upc.computer.service;

import com.upc.computer.dto.LoginRequest;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.entity.User;
import com.upc.computer.vo.SysMenuTree;

import java.util.ArrayList;

/**
 * 认证服务接口
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    User getCurrentUser(Long userId);

    ArrayList<SysMenuTree> getMenusByRole(Long roleId, String roleCode);
}
