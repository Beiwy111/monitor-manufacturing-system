package com.upc.computer.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.upc.computer.common.BusinessException;
import com.upc.computer.common.JwtUtil;
import com.upc.computer.common.RedisUtil;
import com.upc.computer.dto.LoginRequest;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.dto.RegisterRequest;
import com.upc.computer.entity.Role;
import com.upc.computer.entity.User;
import com.upc.computer.mapper.RoleMapper;
import com.upc.computer.mapper.RoleMenuMapper;
import com.upc.computer.mapper.UserMapper;
import com.upc.computer.service.AuthService;
import com.upc.computer.service.SystemService;
import com.upc.computer.vo.SysMenuTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private SystemService systemService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginResponse authenticate(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        User user = userMapper.getUserByUsername(request.getUsername().trim());
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getRoleId() == null) {
            throw new BusinessException(401, "账号待管理员分配角色，暂无法登录");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(401, "用户已停用");
        }
        if (!matchPassword(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        Role role = user.getRoleId() != null ? roleMapper.getRoleById(user.getRoleId()) : null;
        LoginResponse session = new LoginResponse();
        session.setUserId(user.getUserId());
        session.setUsername(user.getUsername());
        session.setRealName(user.getRealName());
        session.setRoleId(user.getRoleId());
        if (role != null) {
            session.setRoleCode(role.getRoleCode());
            session.setRoleName(role.getRoleName());
        }
        return session;
    }

    @Override
    public Map<String, Object> register(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException("注册信息不能为空");
        }
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String password = request.getPassword() != null ? request.getPassword() : "";
        String realName = request.getRealName() != null ? request.getRealName().trim() : "";

        if (username.isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException("用户名需为 4-20 位字母、数字或下划线");
        }
        if (password.length() < 6) {
            throw new BusinessException("密码至少 6 位");
        }
        if (realName.isEmpty()) {
            throw new BusinessException("姓名不能为空");
        }
        if (userMapper.getUserByUsername(username) != null) {
            throw new BusinessException("用户名已被占用");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(BCrypt.hashpw(password));
        user.setRealName(realName);
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setDepartment(trimToNull(request.getDepartment()));
        user.setRoleId(null);
        user.setStatus(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insertUser(user);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("message", "注册成功，请等待管理员分配角色并启用账号后再登录");
        return result;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public void cacheLoginSession(String token, LoginResponse session) {
        redisUtil.setJson(RedisUtil.loginTokenKey(token), session, jwtUtil.getExpireSeconds());
    }

    @Override
    public LoginResponse getLoginSession(String token) {
        return redisUtil.getJson(RedisUtil.loginTokenKey(token), LoginResponse.class);
    }

    @Override
    public void removeLoginSession(String token) {
        redisUtil.delete(RedisUtil.loginTokenKey(token));
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
        if (roleId != null) {
            ArrayList<Long> menuIds = roleMenuMapper.menuIdsByRoleId(roleId);
            if (menuIds != null && !menuIds.isEmpty()) {
                return filterTreeByMenuIds(fullTree, new HashSet<>(menuIds));
            }
        }
        Set<String> allowedModules = getAllowedModules(roleCode);
        ArrayList<SysMenuTree> filtered = new ArrayList<>();
        for (SysMenuTree node : fullTree) {
            if (allowedModules.contains(node.getMenuCode())) {
                filtered.add(node);
            }
        }
        return filtered;
    }

    private ArrayList<SysMenuTree> filterTreeByMenuIds(ArrayList<SysMenuTree> fullTree, Set<Long> allowedIds) {
        ArrayList<SysMenuTree> result = new ArrayList<>();
        for (SysMenuTree node : fullTree) {
            SysMenuTree copy = copyMenuNode(node);
            if (copy.getChildren() != null && !copy.getChildren().isEmpty()) {
                ArrayList<SysMenuTree> children = new ArrayList<>();
                for (SysMenuTree child : copy.getChildren()) {
                    if (allowedIds.contains(child.getMenuId())) {
                        children.add(child);
                    }
                }
                copy.setChildren(children);
            }
            if (allowedIds.contains(copy.getMenuId()) || (copy.getChildren() != null && !copy.getChildren().isEmpty())) {
                result.add(copy);
            }
        }
        return result;
    }

    private SysMenuTree copyMenuNode(SysMenuTree node) {
        SysMenuTree copy = new SysMenuTree();
        copy.setMenuId(node.getMenuId());
        copy.setMenuCode(node.getMenuCode());
        copy.setMenuName(node.getMenuName());
        copy.setParentId(node.getParentId());
        copy.setMenuLevel(node.getMenuLevel());
        copy.setApiPath(node.getApiPath());
        copy.setBusinessTable(node.getBusinessTable());
        copy.setIcon(node.getIcon());
        copy.setSortNo(node.getSortNo());
        copy.setStatus(node.getStatus());
        if (node.getChildren() != null) {
            copy.setChildren(new ArrayList<>(node.getChildren()));
        }
        return copy;
    }

    private boolean matchPassword(String rawPassword, String passwordHash) {
        if (passwordHash == null) {
            return false;
        }
        if (passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2b$") || passwordHash.startsWith("$2y$")) {
            return BCrypt.checkpw(rawPassword, passwordHash);
        }
        return rawPassword.equals(passwordHash);
    }

    private Set<String> getAllowedModules(String roleCode) {
        if (roleCode == null) {
            return new HashSet<>(Arrays.asList("system"));
        }
        switch (roleCode.toUpperCase()) {
            case "ORDER":
                return new HashSet<>(Arrays.asList("order", "material"));
            case "PLANNER":
                return new HashSet<>(Arrays.asList("production", "order", "material"));
            case "MANAGER":
                return new HashSet<>(Arrays.asList("production", "equipment", "order"));
            case "OPERATOR":
                return new HashSet<>(Arrays.asList("production", "equipment"));
            case "QC":
                return new HashSet<>(Arrays.asList("quality", "production"));
            case "PURCHASER":
                return new HashSet<>(Arrays.asList("purchase", "material"));
            case "WAREHOUSE":
                return new HashSet<>(Arrays.asList("material", "order"));
            case "DEVICE":
                return new HashSet<>(Arrays.asList("equipment", "production"));
            case "SERVICE":
                return new HashSet<>(Arrays.asList("afterSales", "order", "quality"));
            case "COST":
                return new HashSet<>(Arrays.asList("afterSales", "material", "production"));
            default:
                return new HashSet<>(Arrays.asList("system", "material", "order", "production",
                        "purchase", "quality", "equipment", "afterSales"));
        }
    }
}
