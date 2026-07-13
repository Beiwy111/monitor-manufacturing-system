package com.upc.computer.mapper;

import com.upc.computer.entity.AttendanceRecord;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Mapper
public interface AttendanceRecordMapper {

    @Select("SELECT record_id, user_id, employee_no, real_name, department, attendance_date, " +
            "check_in_time, check_out_time, status, remark, created_at, updated_at " +
            "FROM attendance_record ORDER BY attendance_date DESC, record_id DESC")
    ArrayList<AttendanceRecord> listAll();

    @Select("<script>" +
            "SELECT record_id, user_id, employee_no, real_name, department, attendance_date, " +
            "check_in_time, check_out_time, status, remark, created_at, updated_at " +
            "FROM attendance_record WHERE 1=1 " +
            "<if test='startDate != null'> AND attendance_date &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> AND attendance_date &lt;= #{endDate} </if>" +
            "<if test='realName != null and realName != \"\"'> AND real_name LIKE CONCAT('%',#{realName},'%') </if>" +
            "<if test='department != null and department != \"\"'> AND department = #{department} </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "ORDER BY attendance_date DESC, record_id DESC" +
            "</script>")
    ArrayList<AttendanceRecord> listByFilter(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("realName") String realName,
                                             @Param("department") String department,
                                             @Param("status") String status);

    @Select("SELECT COUNT(*) FROM attendance_record")
    int countAll();

    @Select("SELECT record_id, user_id, employee_no, real_name, department, attendance_date, " +
            "check_in_time, check_out_time, status, remark, created_at, updated_at " +
            "FROM attendance_record WHERE user_id = #{userId} AND attendance_date = #{date} LIMIT 1")
    AttendanceRecord getByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT record_id, user_id, employee_no, real_name, department, attendance_date, " +
            "check_in_time, check_out_time, status, remark, created_at, updated_at " +
            "FROM attendance_record WHERE record_id = #{recordId}")
    AttendanceRecord getById(Long recordId);

    @Insert("INSERT INTO attendance_record (user_id, employee_no, real_name, department, attendance_date, " +
            "check_in_time, check_out_time, status, remark) " +
            "VALUES (#{userId}, #{employeeNo}, #{realName}, #{department}, #{attendanceDate}, " +
            "#{checkInTime}, #{checkOutTime}, #{status}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "recordId")
    void insert(AttendanceRecord record);

    @Update("UPDATE attendance_record SET check_in_time=#{checkInTime}, check_out_time=#{checkOutTime}, " +
            "status=#{status}, remark=#{remark}, updated_at=NOW() WHERE record_id=#{recordId}")
    void update(AttendanceRecord record);

    @Select("SELECT status, COUNT(*) AS cnt FROM attendance_record " +
            "WHERE attendance_date BETWEEN #{startDate} AND #{endDate} GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    @Select("SELECT department, COUNT(*) AS total, " +
            "SUM(CASE WHEN status = 'NORMAL' THEN 1 ELSE 0 END) AS normal_cnt " +
            "FROM attendance_record WHERE attendance_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY department ORDER BY total DESC")
    List<java.util.Map<String, Object>> countByDepartment(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Select("SELECT attendance_date AS att_date, COUNT(*) AS cnt " +
            "FROM attendance_record WHERE attendance_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY attendance_date ORDER BY attendance_date")
    List<java.util.Map<String, Object>> countByDate(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);
}
