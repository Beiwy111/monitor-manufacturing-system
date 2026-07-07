package com.upc.computer.mapper;

import com.upc.computer.entity.SysMenu;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SysMenuMapper {

    // 查询所有菜单
    @Select("SELECT menu_id, menu_code, menu_name, parent_id, menu_level, api_path, business_table, icon, sort_no, status, created_at, updated_at FROM sys_menu ORDER BY sort_no")
    public ArrayList<SysMenu> menuList();

    // 查询启用的菜单
    @Select("SELECT menu_id, menu_code, menu_name, parent_id, menu_level, api_path, business_table, icon, sort_no, status, created_at, updated_at FROM sys_menu WHERE status = 1 ORDER BY sort_no")
    public ArrayList<SysMenu> enabledMenuList();

    // 根据主键查询菜单
    @Select("SELECT menu_id, menu_code, menu_name, parent_id, menu_level, api_path, business_table, icon, sort_no, status, created_at, updated_at FROM sys_menu WHERE menu_id = #{menuId}")
    public SysMenu getMenuById(Long menuId);

    // 新增菜单
    @Insert("INSERT INTO sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, api_path, business_table, icon, sort_no, status, created_at, updated_at) VALUES (#{menuId}, #{menuCode}, #{menuName}, #{parentId}, #{menuLevel}, #{apiPath}, #{businessTable}, #{icon}, #{sortNo}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "menuId")
    public void insertMenu(SysMenu menu);

    // 修改菜单
    @Update("UPDATE sys_menu SET menu_code=#{menuCode}, menu_name=#{menuName}, parent_id=#{parentId}, menu_level=#{menuLevel}, api_path=#{apiPath}, business_table=#{businessTable}, icon=#{icon}, sort_no=#{sortNo}, status=#{status}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE menu_id = #{menuId}")
    public void updateMenu(SysMenu menu);

    // 删除菜单
    @Delete("DELETE FROM sys_menu WHERE menu_id = #{menuId}")
    public void deleteMenu(Long menuId);
}
