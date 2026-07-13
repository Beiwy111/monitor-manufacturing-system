package com.upc.computer.mapper;

import com.upc.computer.entity.ShiftSchedule;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.ArrayList;

@Mapper
public interface ShiftScheduleMapper {

    @Select("SELECT schedule_id, schedule_date, user_id, employee_no, real_name, shift_type, " +
            "workshop, remark, created_by, created_at, updated_at FROM shift_schedule " +
            "WHERE schedule_date BETWEEN #{startDate} AND #{endDate} ORDER BY schedule_date, real_name")
    ArrayList<ShiftSchedule> listByDateRange(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Select("SELECT schedule_id, schedule_date, user_id, employee_no, real_name, shift_type, " +
            "workshop, remark, created_by, created_at, updated_at FROM shift_schedule " +
            "WHERE schedule_date = #{date} ORDER BY real_name")
    ArrayList<ShiftSchedule> listByDate(LocalDate date);

    @Select("SELECT schedule_id, schedule_date, user_id, employee_no, real_name, shift_type, " +
            "workshop, remark, created_by, created_at, updated_at FROM shift_schedule " +
            "WHERE user_id = #{userId} AND schedule_date = #{date} LIMIT 1")
    ShiftSchedule getByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Insert("INSERT INTO shift_schedule (schedule_date, user_id, employee_no, real_name, shift_type, " +
            "workshop, remark, created_by) VALUES (#{scheduleDate}, #{userId}, #{employeeNo}, #{realName}, " +
            "#{shiftType}, #{workshop}, #{remark}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "scheduleId")
    void insert(ShiftSchedule schedule);

    @Update("UPDATE shift_schedule SET shift_type=#{shiftType}, workshop=#{workshop}, remark=#{remark}, " +
            "updated_at=NOW() WHERE schedule_id=#{scheduleId}")
    void update(ShiftSchedule schedule);

    @Delete("DELETE FROM shift_schedule WHERE schedule_id = #{scheduleId}")
    void delete(Long scheduleId);

    @Delete("DELETE FROM shift_schedule WHERE user_id = #{userId} AND schedule_date = #{date}")
    void deleteByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
