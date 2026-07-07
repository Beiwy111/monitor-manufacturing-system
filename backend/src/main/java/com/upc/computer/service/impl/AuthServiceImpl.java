package com.upc.computer.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.upc.computer.common.BusinessException;
import com.upc.computer.common.JwtUtil;
import com.upc.computer.dto.LoginRequest;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.entity.Role;
import com.upc.computer.entity.User;
import com.upc.computer.mapper.RoleMapper;
import com.upc.computer.mapper.UserMapper;
import com.upc.computer.service.AuthService;
import com.upc.computer.service.SystemService;
import com.upc.computer.vo.SysMenuTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private SystemService systemService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        User user = userMapper.getUserByUsername(request.getUsername().trim());
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(401, "用户已停用");
        }
        if (!matchPassword(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        Role role = user.getRoleId() != null ? roleMapper.getRoleById(user.getRoleId()) : null;
        LoginResponse response = new LoginResponse();
        response.setToken(jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRoleId(),
                role != null ? role.getRoleCode() : null));
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRoleId(user.getRoleId());
        if (role != null) {
            response.setRoleCode(role.getRoleCode());
            response.setRoleName(role.getRoleName());
        }
        return response;
    }

    @Override
    public User getCurrentUser(Long userId) {
        User user = userMapper.getUserById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        user.setPasswordHash(null);
        return user;
    }

    @Override
    public ArrayList<SysMenuTree> getMenusByRole(Long roleId, String roleCode) {
        ArrayList<SysMenuTree> fullTree = systemService.menuTree();
        if (roleId == null || "ADMIN".equalsIgnoreCase(roleCode)) {
            return fullTree;
        }
        Set<String> allowedModules = getAllowedModules(roleCode);
        ArrayList<SysMenuTree> filtered = new ArrayList<SysMenuTree>();
        for (SysMenuTree node : fullTree) {
            if (allowedModules.contains(node.getMenuCode())) {
                filtered.add(node);
            }
        }
        return filtered;
    }

    private boolean matchPassword(String rawPassword, String passwordHash) {
        if (passwordHash == null) {
            return false;
        }
        if (passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2b$")) {
            return BCrypt.checkpw(rawPassword, passwordHash);
        }
        return rawPassword.equals(passwordHash);
    }

    private Set<String> getAllowedModules(String roleCode) {
        if (roleCode == null) {
            return new HashSet<String>(Arrays.asList("system"));
        }
        switch (roleCode.toUpperCase()) {
            case "PLANNER":
                return new HashSet<String>(Arrays.asList("production", "order", "material"));
            case "OPERATOR":
                return new HashSet<String>(Arrays.asList("production", "equipment"));
            case "QC":
                return new HashSet<String>(Arrays.asList("quality", "production"));
            case "PURCHASER":
                return new HashSet<String>(Arrays.asList("purchase", "material"));
            case "WAREHOUSE":
                return new HashSet<String>(Arrays.asList("material", "order"));
            case "SERVICE":
                return new HashSet<String>(Arrays.asList("afterSales", "order", "quality"));
            default:
                return new HashSet<String>(Arrays.asList("system", "material", "order", "production",
                        "purchase", "quality", "equipment", "afterSales"));
        }
    }
}
