package com.upc.computer.mapper;

import com.upc.computer.entity.Permission;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface PermissionMapper {

    // 查询所有权限
    @Select("SELECT permission_id, role_id, permission_code, permission_name, resource_type, resource_path, parent_id, sort_no, status, created_at, updated_at FROM permission")
    public ArrayList<Permission> permissionList();

    // 根据主键查询权限
    @Select("SELECT permission_id, role_id, permission_code, permission_name, resource_type, resource_path, parent_id, sort_no, status, created_at, updated_at FROM permission WHERE permission_id = #{permissionId}")
    public Permission getPermissionById(Long permissionId);

    // 根据角色ID查询权限
    @Select("SELECT permission_id, role_id, permission_code, permission_name, resource_type, resource_path, parent_id, sort_no, status, created_at, updated_at FROM permission WHERE role_id = #{roleId} AND status = 1 ORDER BY sort_no")
    public ArrayList<Permission> permissionListByRoleId(Long roleId);

    // 新增权限
    @Insert("INSERT INTO permission (permission_id, role_id, permission_code, permission_name, resource_type, resource_path, parent_id, sort_no, status, created_at, updated_at) VALUES (#{permissionId}, #{roleId}, #{permissionCode}, #{permissionName}, #{resourceType}, #{resourcePath}, #{parentId}, #{sortNo}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "permissionId")
    public void insertPermission(Permission permission);

    // 修改权限
    @Update("UPDATE permission SET role_id=#{roleId}, permission_code=#{permissionCode}, permission_name=#{permissionName}, resource_type=#{resourceType}, resource_path=#{resourcePath}, parent_id=#{parentId}, sort_no=#{sortNo}, status=#{status}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE permission_id = #{permissionId}")
    public void updatePermission(Permission permission);

    // 删除权限
    @Delete("DELETE FROM permission WHERE permission_id = #{permissionId}")
    public void deletePermission(Long permissionId);

}
