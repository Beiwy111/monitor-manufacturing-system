package com.upc.computer.dto;

import java.util.ArrayList;

/**
 * 角色菜单分配请求
 */
public class RoleMenuAssignRequest {

    private Long roleId;
    private ArrayList<Long> menuIds;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public ArrayList<Long> menuIds() {
        return menuIds;
    }

    public ArrayList<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(ArrayList<Long> menuIds) {
        this.menuIds = menuIds;
    }
}
