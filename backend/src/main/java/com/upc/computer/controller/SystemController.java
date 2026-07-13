package com.upc.computer.controller;

import com.upc.computer.service.SystemService;
import com.upc.computer.entity.Role;
import com.upc.computer.entity.User;
import com.upc.computer.entity.Permission;
import com.upc.computer.entity.OperationLog;
import com.upc.computer.entity.SysMenu;
import com.upc.computer.vo.SysMenuTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;

@RestController
@RequestMapping("/system")
public class SystemController {

    @Autowired
    private SystemService systemService;

    // 查询角色列表
    @RequestMapping("/role/list")
    public ArrayList<Role> roleList() {
        return systemService.roleList();
    }

    // 根据主键查询角色
    @RequestMapping("/role/get")
    public Role getRoleById(Long roleId) {
        return systemService.getRoleById(roleId);
    }

    // 新增角色
    @RequestMapping("/role/insert")
    public void insertRole(Role role) {
        systemService.insertRole(role);
    }

    // 修改角色
    @RequestMapping("/role/update")
    public void updateRole(Role role) {
        systemService.updateRole(role);
    }

    // 删除角色
    @RequestMapping("/role/delete")
    public void deleteRole(Long roleId) {
        systemService.deleteRole(roleId);
    }

    // 查询用户列表
    @RequestMapping("/user/list")
    public ArrayList<User> userList() {
        return systemService.userList();
    }

    // 根据主键查询用户
    @RequestMapping("/user/get")
    public User getUserById(Long userId) {
        return systemService.getUserById(userId);
    }

    // 根据用户名查询用户
    @RequestMapping("/user/getByUsername")
    public User getUserByUsername(String username) {
        return systemService.getUserByUsername(username);
    }

    // 根据用户名和密码查询用户（登录）
    @RequestMapping("/user/login")
    public User getUserByUsernameAndPassword(String username, String passwordHash) {
        return systemService.getUserByUsernameAndPassword(username, passwordHash);
    }

    // 新增用户
    @RequestMapping("/user/insert")
    public void insertUser(User user) {
        systemService.insertUser(user);
    }

    // 修改用户
    @RequestMapping("/user/update")
    public void updateUser(User user) {
        systemService.updateUser(user);
    }

    // 删除用户
    @RequestMapping("/user/delete")
    public void deleteUser(Long userId) {
        systemService.deleteUser(userId);
    }

    // 查询权限列表
    @RequestMapping("/permission/list")
    public ArrayList<Permission> permissionList() {
        return systemService.permissionList();
    }

    // 根据主键查询权限
    @RequestMapping("/permission/get")
    public Permission getPermissionById(Long permissionId) {
        return systemService.getPermissionById(permissionId);
    }

    // 新增权限
    @RequestMapping("/permission/insert")
    public void insertPermission(Permission permission) {
        systemService.insertPermission(permission);
    }

    // 修改权限
    @RequestMapping("/permission/update")
    public void updatePermission(Permission permission) {
        systemService.updatePermission(permission);
    }

    // 删除权限
    @RequestMapping("/permission/delete")
    public void deletePermission(Long permissionId) {
        systemService.deletePermission(permissionId);
    }

    // 查询操作日志列表
    @RequestMapping("/operationLog/list")
    public ArrayList<OperationLog> operationLogList() {
        return systemService.operationLogList();
    }

    // 根据主键查询操作日志
    @RequestMapping("/operationLog/get")
    public OperationLog getOperationLogById(Long logId) {
        return systemService.getOperationLogById(logId);
    }

    // 新增操作日志
    @RequestMapping("/operationLog/insert")
    public void insertOperationLog(OperationLog operationLog) {
        systemService.insertOperationLog(operationLog);
    }

    // 修改操作日志
    @RequestMapping("/operationLog/update")
    public void updateOperationLog(OperationLog operationLog) {
        systemService.updateOperationLog(operationLog);
    }

    // 删除操作日志
    @RequestMapping("/operationLog/delete")
    public void deleteOperationLog(Long logId) {
        systemService.deleteOperationLog(logId);
    }

    // 查询菜单列表（平铺）
    @RequestMapping("/menu/list")
    public ArrayList<SysMenu> menuList() {
        return systemService.menuList();
    }

    // 查询菜单树（前端导航）
    @RequestMapping("/menu/tree")
    public ArrayList<SysMenuTree> menuTree() {
        return systemService.menuTree();
    }

    // 根据主键查询菜单
    @RequestMapping("/menu/get")
    public SysMenu getMenuById(Long menuId) {
        return systemService.getMenuById(menuId);
    }

    // 新增菜单
    @RequestMapping("/menu/insert")
    public void insertMenu(SysMenu menu) {
        systemService.insertMenu(menu);
    }

    // 修改菜单
    @RequestMapping("/menu/update")
    public void updateMenu(SysMenu menu) {
        systemService.updateMenu(menu);
    }

    // 删除菜单
    @RequestMapping("/menu/delete")
    public void deleteMenu(Long menuId) {
        systemService.deleteMenu(menuId);
    }

}
