package com.upc.computer.service;

import com.upc.computer.entity.Role;
import com.upc.computer.entity.User;
import com.upc.computer.entity.Permission;
import com.upc.computer.entity.OperationLog;
import com.upc.computer.entity.SysMenu;
import com.upc.computer.vo.SysMenuTree;
import java.util.ArrayList;

public interface SystemService {

    public ArrayList<Role> roleList();

    public Role getRoleById(Long roleId);

    public void insertRole(Role role);

    public void updateRole(Role role);

    public void deleteRole(Long roleId);

    public ArrayList<User> userList();

    public User getUserById(Long userId);

    public User getUserByUsername(String username);

    public User getUserByUsernameAndPassword(String username, String passwordHash);

    public void insertUser(User user);

    public void updateUser(User user);

    public void deleteUser(Long userId);

    public ArrayList<Permission> permissionList();

    public Permission getPermissionById(Long permissionId);

    public void insertPermission(Permission permission);

    public void updatePermission(Permission permission);

    public void deletePermission(Long permissionId);

    public ArrayList<OperationLog> operationLogList();

    public OperationLog getOperationLogById(Long logId);

    public void insertOperationLog(OperationLog operationLog);

    public void updateOperationLog(OperationLog operationLog);

    public void deleteOperationLog(Long logId);

    public ArrayList<SysMenu> menuList();

    public ArrayList<SysMenuTree> menuTree();

    public SysMenu getMenuById(Long menuId);

    public void insertMenu(SysMenu menu);

    public void updateMenu(SysMenu menu);

    public void deleteMenu(Long menuId);

}
