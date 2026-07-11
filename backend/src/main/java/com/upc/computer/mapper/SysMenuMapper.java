package com.upc.computer.mapper;

import com.upc.computer.entity.SysMenu;
import org.apache.ibatis.annotations.*;

import java.util.ArrayList;

@Mapper
public interface SysMenuMapper {

    @Select("SELECT menu_id, menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status, created_at, updated_at FROM sys_menu ORDER BY sort_no")
    ArrayList<SysMenu> menuList();

    @Select("SELECT menu_id, menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status, created_at, updated_at FROM sys_menu WHERE status = 1 ORDER BY sort_no")
    ArrayList<SysMenu> enabledMenuList();

    @Select("SELECT menu_id, menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status, created_at, updated_at FROM sys_menu WHERE menu_id = #{menuId}")
    SysMenu getMenuById(Long menuId);

    @Insert("INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status, created_at, updated_at) VALUES (#{menuCode}, #{menuName}, #{parentId}, #{menuLevel}, #{apiPath}, #{routePath}, #{businessTable}, #{icon}, #{sortNo}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "menuId")
    void insertMenu(SysMenu menu);

    @Update("UPDATE sys_menu SET menu_code=#{menuCode}, menu_name=#{menuName}, parent_id=#{parentId}, menu_level=#{menuLevel}, api_path=#{apiPath}, route_path=#{routePath}, business_table=#{businessTable}, icon=#{icon}, sort_no=#{sortNo}, status=#{status}, updated_at=#{updatedAt} WHERE menu_id=#{menuId}")
    void updateMenu(SysMenu menu);

    @Delete("DELETE FROM sys_menu WHERE menu_id = #{menuId}")
    void deleteMenu(Long menuId);
}
