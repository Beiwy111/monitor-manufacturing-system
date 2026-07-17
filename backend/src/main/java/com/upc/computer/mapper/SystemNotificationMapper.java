package com.upc.computer.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** sys_notification 通用消息读写。 */
@Mapper
public interface SystemNotificationMapper {

    @Insert("""
        INSERT INTO sys_notification
        (receiver_role,title,content,level,business_type,business_id,target_path,read_status,created_at)
        VALUES
        (#{receiverRole},#{title},#{content},#{level},#{businessType},#{businessId},#{targetPath},0,NOW())
        """)
    @Options(useGeneratedKeys = true, keyProperty = "notificationId")
    int insert(Map<String, Object> row);

    @Select("""
        SELECT notification_id notificationId,receiver_role receiverRole,title,content,level,
               business_type businessType,business_id businessId,target_path targetPath,
               read_status readStatus,created_at createdAt
        FROM sys_notification
        WHERE receiver_role=#{roleCode} OR receiver_role='ALL'
        ORDER BY created_at DESC,notification_id DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> listForRole(@Param("roleCode") String roleCode, @Param("limit") int limit);
}
