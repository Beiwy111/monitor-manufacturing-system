package com.upc.computer.service;

import com.upc.computer.dto.ShiftScheduleSaveRequest;
import com.upc.computer.entity.AttendanceRecord;
import com.upc.computer.entity.ShiftSchedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface AttendanceService {

    ArrayList<AttendanceRecord> recordList(LocalDate startDate, LocalDate endDate,
                                           String realName, String department, String status);

    AttendanceRecord todayRecord(Long userId);

    AttendanceRecord checkIn(Long userId);

    AttendanceRecord checkOut(Long userId);

    Map<String, Object> statistics(String month, String department, String realName);

    ArrayList<ShiftSchedule> scheduleList(LocalDate startDate, LocalDate endDate);

    ArrayList<ShiftSchedule> scheduleByDate(LocalDate date);

    void saveSchedule(ShiftScheduleSaveRequest request);

    void deleteSchedule(Long scheduleId);

    List<Map<String, Object>> operatorList();
}
