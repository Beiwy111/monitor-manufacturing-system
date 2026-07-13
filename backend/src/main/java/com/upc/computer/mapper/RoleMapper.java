package com.upc.computer.mapper;

import com.upc.computer.entity.Role;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface RoleMapper {

    // 查询所有角色
    @Select("SELECT role_id, role_code, role_name, role_description, status, created_at, updated_at FROM role")
    public ArrayList<Role> roleList();

    // 根据主键查询角色
    @Select("SELECT role_id, role_code, role_name, role_description, status, created_at, updated_at FROM role WHERE role_id = #{roleId}")
    public Role getRoleById(Long roleId);

    // 新增角色
    @Insert("INSERT INTO role (role_id, role_code, role_name, role_description, status, created_at, updated_at) VALUES (#{roleId}, #{roleCode}, #{roleName}, #{roleDescription}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "roleId")
    public void insertRole(Role role);

    // 修改角色
    @Update("UPDATE role SET role_code=#{roleCode}, role_name=#{roleName}, role_description=#{roleDescription}, status=#{status}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE role_id = #{roleId}")
    public void updateRole(Role role);

    // 删除角色
    @Delete("DELETE FROM role WHERE role_id = #{roleId}")
    public void deleteRole(Long roleId);

}
