package com.upc.computer.mapper;

import org.apache.ibatis.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper
public interface RoleMenuMapper {

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    ArrayList<Long> menuIdsByRoleId(Long roleId);

    @Select("SELECT role_id AS roleId, menu_id AS menuId FROM sys_role_menu ORDER BY role_id, menu_id")
    List<Map<String, Object>> listAll();

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteByRoleId(Long roleId);

    @Insert({
            "<script>",
            "INSERT INTO sys_role_menu (role_id, menu_id) VALUES ",
            "<foreach collection='menuIds' item='menuId' separator=','>",
            "(#{roleId}, #{menuId})",
            "</foreach>",
            "</script>"
    })
    void insertBatch(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}
