package com.upc.computer.mapper;

import com.upc.computer.entity.AndonAlarm;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface AndonAlarmMapper {

    // 查询所有安灯报警（仅管理/快照兜底；优先用 listRecentAlarms / listOpenAlarms）
    @Select("SELECT alarm_id, alarm_no, work_order_id, dispatch_id, equipment_id, alarm_type, alarm_level, alarm_description, alarm_status, reported_by, reported_at, received_by, received_at, closed_by, closed_at, close_result, created_at, updated_at FROM andon_alarm ORDER BY reported_at DESC LIMIT #{limit}")
    public ArrayList<AndonAlarm> listRecentAlarms(@Param("limit") int limit);

    @Select("SELECT alarm_id, alarm_no, work_order_id, dispatch_id, equipment_id, alarm_type, alarm_level, alarm_description, alarm_status, reported_by, reported_at, received_by, received_at, closed_by, closed_at, close_result, created_at, updated_at FROM andon_alarm WHERE alarm_status <> 'CLOSED' ORDER BY reported_at DESC LIMIT #{limit}")
    public ArrayList<AndonAlarm> listOpenAlarms(@Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM andon_alarm WHERE alarm_status <> 'CLOSED'")
    public int countOpenAlarms();

    @Delete("DELETE FROM andon_alarm WHERE alarm_status = 'CLOSED' AND closed_at IS NOT NULL AND closed_at < #{before}")
    public int deleteClosedBefore(@Param("before") LocalDateTime before);

    /** @deprecated 全表扫描，请改用 listRecentAlarms */
    @Select("SELECT alarm_id, alarm_no, work_order_id, dispatch_id, equipment_id, alarm_type, alarm_level, alarm_description, alarm_status, reported_by, reported_at, received_by, received_at, closed_by, closed_at, close_result, created_at, updated_at FROM andon_alarm ORDER BY reported_at DESC LIMIT 500")
    public ArrayList<AndonAlarm> alarmList();

    // 根据主键查询安灯报警
    @Select("SELECT alarm_id, alarm_no, work_order_id, dispatch_id, equipment_id, alarm_type, alarm_level, alarm_description, alarm_status, reported_by, reported_at, received_by, received_at, closed_by, closed_at, close_result, created_at, updated_at FROM andon_alarm WHERE alarm_id = #{alarmId}")
    public AndonAlarm getAlarmById(Long alarmId);

    // 新增安灯报警
    @Insert("INSERT INTO andon_alarm (alarm_id, alarm_no, work_order_id, dispatch_id, equipment_id, alarm_type, alarm_level, alarm_description, alarm_status, reported_by, reported_at, received_by, received_at, closed_by, closed_at, close_result, created_at, updated_at) VALUES (#{alarmId}, #{alarmNo}, #{workOrderId}, #{dispatchId}, #{equipmentId}, #{alarmType}, #{alarmLevel}, #{alarmDescription}, #{alarmStatus}, #{reportedBy}, #{reportedAt}, #{receivedBy}, #{receivedAt}, #{closedBy}, #{closedAt}, #{closeResult}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "alarmId")
    public void insertAlarm(AndonAlarm alarm);

    // 修改安灯报警
    @Update("UPDATE andon_alarm SET alarm_no=#{alarmNo}, work_order_id=#{workOrderId}, dispatch_id=#{dispatchId}, equipment_id=#{equipmentId}, alarm_type=#{alarmType}, alarm_level=#{alarmLevel}, alarm_description=#{alarmDescription}, alarm_status=#{alarmStatus}, reported_by=#{reportedBy}, reported_at=#{reportedAt}, received_by=#{receivedBy}, received_at=#{receivedAt}, closed_by=#{closedBy}, closed_at=#{closedAt}, close_result=#{closeResult}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE alarm_id = #{alarmId}")
    public void updateAlarm(AndonAlarm alarm);

    // 删除安灯报警
    @Delete("DELETE FROM andon_alarm WHERE alarm_id = #{alarmId}")
    public void deleteAlarm(Long alarmId);

}
