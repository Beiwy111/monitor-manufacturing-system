package com.upc.computer.service.impl;

import com.upc.computer.service.SystemService;
import com.upc.computer.entity.Role;
import com.upc.computer.mapper.RoleMapper;
import com.upc.computer.entity.User;
import com.upc.computer.mapper.UserMapper;
import com.upc.computer.entity.Permission;
import com.upc.computer.mapper.PermissionMapper;
import com.upc.computer.entity.OperationLog;
import com.upc.computer.mapper.OperationLogMapper;
import com.upc.computer.entity.SysMenu;
import com.upc.computer.mapper.SysMenuMapper;
import com.upc.computer.vo.SysMenuTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class SystemServiceImpl implements SystemService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    // 查询所有角色
    @Override
    public ArrayList<Role> roleList() {
        return roleMapper.roleList();
    }

    // 根据主键查询角色
    @Override
    public Role getRoleById(Long roleId) {
        return roleMapper.getRoleById(roleId);
    }

    // 新增角色
    @Override
    public void insertRole(Role role) {
        roleMapper.insertRole(role);
    }

    // 修改角色
    @Override
    public void updateRole(Role role) {
        roleMapper.updateRole(role);
    }

    // 删除角色
    @Override
    public void deleteRole(Long roleId) {
        roleMapper.deleteRole(roleId);
    }

    // 查询所有用户
    @Override
    public ArrayList<User> userList() {
        return userMapper.userList();
    }

    // 根据主键查询用户
    @Override
    public User getUserById(Long userId) {
        return userMapper.getUserById(userId);
    }

    // 根据用户名查询用户
    @Override
    public User getUserByUsername(String username) {
        return userMapper.getUserByUsername(username);
    }

    // 根据用户名和密码查询用户（登录）
    @Override
    public User getUserByUsernameAndPassword(String username, String passwordHash) {
        return userMapper.getUserByUsernameAndPassword(username, passwordHash);
    }

    // 新增用户
    @Override
    public void insertUser(User user) {
        userMapper.insertUser(user);
    }

    // 修改用户
    @Override
    public void updateUser(User user) {
        userMapper.updateUser(user);
    }

    // 删除用户
    @Override
    public void deleteUser(Long userId) {
        userMapper.deleteUser(userId);
    }

    // 查询所有权限
    @Override
    public ArrayList<Permission> permissionList() {
        return permissionMapper.permissionList();
    }

    // 根据主键查询权限
    @Override
    public Permission getPermissionById(Long permissionId) {
        return permissionMapper.getPermissionById(permissionId);
    }

    // 新增权限
    @Override
    public void insertPermission(Permission permission) {
        permissionMapper.insertPermission(permission);
    }

    // 修改权限
    @Override
    public void updatePermission(Permission permission) {
        permissionMapper.updatePermission(permission);
    }

    // 删除权限
    @Override
    public void deletePermission(Long permissionId) {
        permissionMapper.deletePermission(permissionId);
    }

    // 查询所有操作日志
    @Override
    public ArrayList<OperationLog> operationLogList() {
        return operationLogMapper.operationLogList();
    }

    // 根据主键查询操作日志
    @Override
    public OperationLog getOperationLogById(Long logId) {
        return operationLogMapper.getOperationLogById(logId);
    }

    // 新增操作日志
    @Override
    public void insertOperationLog(OperationLog operationLog) {
        operationLogMapper.insertOperationLog(operationLog);
    }

    // 修改操作日志
    @Override
    public void updateOperationLog(OperationLog operationLog) {
        operationLogMapper.updateOperationLog(operationLog);
    }

    // 删除操作日志
    @Override
    public void deleteOperationLog(Long logId) {
        operationLogMapper.deleteOperationLog(logId);
    }

    // 查询所有菜单
    @Override
    public ArrayList<SysMenu> menuList() {
        return sysMenuMapper.menuList();
    }

    // 查询菜单树（前端导航用）
    @Override
    public ArrayList<SysMenuTree> menuTree() {
        ArrayList<SysMenu> allMenus = sysMenuMapper.enabledMenuList();
        HashMap<Long, SysMenuTree> nodeMap = new HashMap<Long, SysMenuTree>();
        ArrayList<SysMenuTree> roots = new ArrayList<SysMenuTree>();

        for (SysMenu menu : allMenus) {
            SysMenuTree node = convertToTreeNode(menu);
            node.setChildren(new ArrayList<SysMenuTree>());
            nodeMap.put(menu.getMenuId(), node);
        }

        for (SysMenu menu : allMenus) {
            SysMenuTree node = nodeMap.get(menu.getMenuId());
            if (menu.getParentId() == null) {
                roots.add(node);
            } else {
                SysMenuTree parent = nodeMap.get(menu.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        return roots;
    }

    // 根据主键查询菜单
    @Override
    public SysMenu getMenuById(Long menuId) {
        return sysMenuMapper.getMenuById(menuId);
    }

    // 新增菜单
    @Override
    public void insertMenu(SysMenu menu) {
        sysMenuMapper.insertMenu(menu);
    }

    // 修改菜单
    @Override
    public void updateMenu(SysMenu menu) {
        sysMenuMapper.updateMenu(menu);
    }

    // 删除菜单
    @Override
    public void deleteMenu(Long menuId) {
        sysMenuMapper.deleteMenu(menuId);
    }

    // 将菜单实体转换为树节点
    private SysMenuTree convertToTreeNode(SysMenu menu) {
        SysMenuTree node = new SysMenuTree();
        node.setMenuId(menu.getMenuId());
        node.setMenuCode(menu.getMenuCode());
        node.setMenuName(menu.getMenuName());
        node.setParentId(menu.getParentId());
        node.setMenuLevel(menu.getMenuLevel());
        node.setApiPath(menu.getApiPath());
        node.setBusinessTable(menu.getBusinessTable());
        node.setIcon(menu.getIcon());
        node.setSortNo(menu.getSortNo());
        node.setStatus(menu.getStatus());
        return node;
    }

}
