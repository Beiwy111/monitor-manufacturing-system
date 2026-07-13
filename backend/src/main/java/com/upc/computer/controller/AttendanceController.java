package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.dto.ShiftScheduleSaveRequest;
import com.upc.computer.entity.AttendanceRecord;
import com.upc.computer.entity.ShiftSchedule;
import com.upc.computer.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping("/record/list")
    public Result<ArrayList<AttendanceRecord>> recordList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {
        return Result.success(attendanceService.recordList(startDate, endDate, realName, department, status));
    }

    @GetMapping("/record/today")
    public Result<AttendanceRecord> todayRecord(@RequestParam Long userId) {
        return Result.success(attendanceService.todayRecord(userId));
    }

    @PostMapping("/check-in")
    public Result<AttendanceRecord> checkIn(@RequestParam Long userId) {
        return Result.success(attendanceService.checkIn(userId));
    }

    @PostMapping("/check-out")
    public Result<AttendanceRecord> checkOut(@RequestParam Long userId) {
        return Result.success(attendanceService.checkOut(userId));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String realName) {
        return Result.success(attendanceService.statistics(month, department, realName));
    }

    @GetMapping("/schedule/list")
    public Result<ArrayList<ShiftSchedule>> scheduleList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(attendanceService.scheduleList(startDate, endDate));
    }

    @GetMapping("/schedule/by-date")
    public Result<ArrayList<ShiftSchedule>> scheduleByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(attendanceService.scheduleByDate(date));
    }

    @PostMapping("/schedule/save")
    public Result<Void> saveSchedule(@RequestBody ShiftScheduleSaveRequest request) {
        attendanceService.saveSchedule(request);
        return Result.success();
    }

    @PostMapping("/schedule/delete")
    public Result<Void> deleteSchedule(@RequestParam Long scheduleId) {
        attendanceService.deleteSchedule(scheduleId);
        return Result.success();
    }

    @GetMapping("/operators")
    public Result<List<Map<String, Object>>> operatorList() {
        return Result.success(attendanceService.operatorList());
    }
}
