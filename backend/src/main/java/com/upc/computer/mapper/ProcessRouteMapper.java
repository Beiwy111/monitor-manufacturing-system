package com.upc.computer.mapper;

import com.upc.computer.entity.ProcessRoute;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface ProcessRouteMapper {

    // 查询所有工艺路线
    @Select("SELECT route_id, material_id, route_code, route_name, version_no, status, created_by, created_at, updated_at FROM process_route")
    public ArrayList<ProcessRoute> routeList();

    // 根据主键查询工艺路线
    @Select("SELECT route_id, material_id, route_code, route_name, version_no, status, created_by, created_at, updated_at FROM process_route WHERE route_id = #{routeId}")
    public ProcessRoute getRouteById(Long routeId);

    // 新增工艺路线
    @Insert("INSERT INTO process_route (route_id, material_id, route_code, route_name, version_no, status, created_by, created_at, updated_at) VALUES (#{routeId}, #{materialId}, #{routeCode}, #{routeName}, #{versionNo}, #{status}, #{createdBy}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "routeId")
    public void insertRoute(ProcessRoute route);

    // 修改工艺路线
    @Update("UPDATE process_route SET material_id=#{materialId}, route_code=#{routeCode}, route_name=#{routeName}, version_no=#{versionNo}, status=#{status}, created_by=#{createdBy}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE route_id = #{routeId}")
    public void updateRoute(ProcessRoute route);

    // 删除工艺路线
    @Delete("DELETE FROM process_route WHERE route_id = #{routeId}")
    public void deleteRoute(Long routeId);

    @Update("UPDATE process_route SET status = 0, updated_at = NOW() WHERE route_id = #{routeId}")
    public void disableRoute(Long routeId);

}
