package com.upc.computer.service.impl;

import com.upc.computer.common.BusinessException;
import com.upc.computer.dto.ShiftScheduleSaveRequest;
import com.upc.computer.entity.AttendanceRecord;
import com.upc.computer.entity.Role;
import com.upc.computer.entity.ShiftSchedule;
import com.upc.computer.entity.User;
import com.upc.computer.mapper.AttendanceRecordMapper;
import com.upc.computer.mapper.RoleMapper;
import com.upc.computer.mapper.ShiftScheduleMapper;
import com.upc.computer.mapper.UserMapper;
import com.upc.computer.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private static final LocalTime WORK_START = LocalTime.of(8, 30);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private ShiftScheduleMapper shiftScheduleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public ArrayList<AttendanceRecord> recordList(LocalDate startDate, LocalDate endDate,
                                                String realName, String department, String status) {
        ensureSeedDataIfEmpty();
        if (startDate == null && endDate == null && (realName == null || realName.isBlank())
                && (department == null || department.isBlank())
                && (status == null || status.isBlank())) {
            return attendanceRecordMapper.listAll();
        }
        return attendanceRecordMapper.listByFilter(startDate, endDate, realName, department, status);
    }

    @Override
    public AttendanceRecord todayRecord(Long userId) {
        return attendanceRecordMapper.getByUserAndDate(userId, LocalDate.now());
    }

    @Override
    public AttendanceRecord checkIn(Long userId) {
        User user = requireActiveUser(userId);
        LocalDate today = LocalDate.now();
        AttendanceRecord existing = attendanceRecordMapper.getByUserAndDate(userId, today);
        if (existing != null && existing.getCheckInTime() != null) {
            throw new BusinessException("今日已打卡上班，无需重复打卡");
        }
        LocalDateTime now = LocalDateTime.now();
        String status = now.toLocalTime().isAfter(WORK_START) ? "LATE" : "NORMAL";
        if (existing == null) {
            AttendanceRecord record = buildRecord(user, today);
            record.setCheckInTime(now);
            record.setStatus(status);
            attendanceRecordMapper.insert(record);
            return record;
        }
        existing.setCheckInTime(now);
        existing.setStatus(status);
        attendanceRecordMapper.update(existing);
        return existing;
    }

    @Override
    public AttendanceRecord checkOut(Long userId) {
        User user = requireActiveUser(userId);
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRecordMapper.getByUserAndDate(userId, today);
        if (record == null || record.getCheckInTime() == null) {
            throw new BusinessException("请先完成上班打卡");
        }
        if (record.getCheckOutTime() != null) {
            throw new BusinessException("今日已打卡下班，无需重复打卡");
        }
        LocalDateTime now = LocalDateTime.now();
        record.setCheckOutTime(now);
        if (now.toLocalTime().isBefore(WORK_END)) {
            record.setStatus("EARLY_LEAVE");
        } else if ("LATE".equals(record.getStatus())) {
            record.setStatus("LATE");
        } else {
            record.setStatus("NORMAL");
        }
        attendanceRecordMapper.update(record);
        return record;
    }

    @Override
    public Map<String, Object> statistics(String month, String department, String realName) {
        ensureSeedDataIfEmpty();
        YearMonth ym = parseMonth(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        ArrayList<AttendanceRecord> records = attendanceRecordMapper.listByFilter(
                start, end, trimToNull(realName), trimToNull(department), null);

        Map<String, Integer> statusDist = new LinkedHashMap<>();
        statusDist.put("NORMAL", 0);
        statusDist.put("LATE", 0);
        statusDist.put("EARLY_LEAVE", 0);
        statusDist.put("LEAVE", 0);
        statusDist.put("ABSENT", 0);

        Map<String, int[]> deptStats = new LinkedHashMap<>();
        Map<LocalDate, Integer> dailyNormal = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dailyNormal.put(d, 0);
        }

        for (AttendanceRecord record : records) {
            String st = record.getStatus() != null ? record.getStatus() : "ABSENT";
            statusDist.merge(st, 1, Integer::sum);
            String dept = record.getDepartment() != null && !record.getDepartment().isBlank()
                    ? record.getDepartment() : "未分配";
            deptStats.computeIfAbsent(dept, k -> new int[2]);
            deptStats.get(dept)[0]++;
            if ("NORMAL".equals(st)) {
                deptStats.get(dept)[1]++;
            }
            if ("NORMAL".equals(st) && record.getAttendanceDate() != null) {
                dailyNormal.merge(record.getAttendanceDate(), 1, Integer::sum);
            }
        }

        int total = records.size();
        int normalCnt = statusDist.getOrDefault("NORMAL", 0);
        int attendanceRate = total > 0 ? Math.round(normalCnt * 100f / total) : 0;

        List<String> deptNames = new ArrayList<>();
        List<Integer> deptRates = new ArrayList<>();
        deptStats.forEach((dept, arr) -> {
            deptNames.add(dept);
            deptRates.add(arr[0] > 0 ? Math.round(arr[1] * 100f / arr[0]) : 0);
        });

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<String> dates = new ArrayList<>();
        List<Integer> dailyCounts = new ArrayList<>();
        dailyNormal.forEach((date, cnt) -> {
            dates.add(date.format(fmt));
            dailyCounts.add(cnt);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("month", ym.toString());
        result.put("totalRecords", total);
        result.put("attendanceRate", attendanceRate);
        result.put("statusDistribution", statusDist);
        result.put("departmentNames", deptNames);
        result.put("departmentRates", deptRates);
        result.put("dailyDates", dates);
        result.put("dailyCounts", dailyCounts);
        return result;
    }

    private void ensureSeedDataIfEmpty() {
        if (attendanceRecordMapper.countAll() > 0) {
            return;
        }
        Role operatorRole = findRoleByCode("OPERATOR");
        if (operatorRole == null) {
            return;
        }
        YearMonth ym = YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<User> operators = new ArrayList<>();
        for (User user : userMapper.userList()) {
            if (user.getStatus() != null && user.getStatus() == 1
                    && operatorRole.getRoleId().equals(user.getRoleId())) {
                operators.add(user);
            }
        }
        if (operators.isEmpty()) {
            return;
        }
        for (User user : operators) {
            long userSeed = user.getUserId() != null ? user.getUserId() : 0L;
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                int day = date.getDayOfMonth();
                int pattern = (int) ((day + userSeed) % 23);
                AttendanceRecord record = buildRecord(user, date);
                if (pattern == 0) {
                    record.setStatus("ABSENT");
                    record.setRemark("缺勤");
                } else if (pattern == 1 || pattern == 2) {
                    record.setStatus("LEAVE");
                    record.setRemark("请假");
                } else if (pattern == 3) {
                    record.setCheckInTime(date.atTime(9, 12));
                    record.setCheckOutTime(date.atTime(17, 5));
                    record.setStatus("LATE");
                } else if (pattern == 4) {
                    record.setCheckInTime(date.atTime(8, 5));
                    record.setCheckOutTime(date.atTime(16, 20));
                    record.setStatus("EARLY_LEAVE");
                } else {
                    record.setCheckInTime(date.atTime(8, 5));
                    record.setCheckOutTime(date.atTime(17, 10));
                    record.setStatus("NORMAL");
                }
                attendanceRecordMapper.insert(record);
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public ArrayList<ShiftSchedule> scheduleList(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            YearMonth ym = YearMonth.now();
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        }
        return shiftScheduleMapper.listByDateRange(startDate, endDate);
    }

    @Override
    public ArrayList<ShiftSchedule> scheduleByDate(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return shiftScheduleMapper.listByDate(date);
    }

    @Override
    public void saveSchedule(ShiftScheduleSaveRequest request) {
        if (request.getScheduleDate() == null || request.getUserId() == null) {
            throw new BusinessException("排班日期和员工不能为空");
        }
        User user = requireActiveUser(request.getUserId());
        String shiftType = request.getShiftType() != null ? request.getShiftType() : "DAY";

        ShiftSchedule existing = shiftScheduleMapper.getByUserAndDate(user.getUserId(), request.getScheduleDate());
        if ("REST".equals(shiftType) && (request.getWorkshop() == null || request.getWorkshop().isBlank())) {
            if (existing != null) {
                shiftScheduleMapper.delete(existing.getScheduleId());
            }
            return;
        }
        if (existing != null) {
            existing.setShiftType(shiftType);
            existing.setWorkshop(request.getWorkshop());
            existing.setRemark(request.getRemark());
            shiftScheduleMapper.update(existing);
            return;
        }
        ShiftSchedule schedule = new ShiftSchedule();
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setUserId(user.getUserId());
        schedule.setEmployeeNo(user.getEmployeeNo());
        schedule.setRealName(user.getRealName());
        schedule.setShiftType(shiftType);
        schedule.setWorkshop(request.getWorkshop());
        schedule.setRemark(request.getRemark());
        schedule.setCreatedBy(request.getCreatedBy());
        shiftScheduleMapper.insert(schedule);
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        if (scheduleId == null) {
            throw new BusinessException("排班ID不能为空");
        }
        shiftScheduleMapper.delete(scheduleId);
    }

    @Override
    public List<Map<String, Object>> operatorList() {
        Role operatorRole = findRoleByCode("OPERATOR");
        if (operatorRole == null) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : userMapper.userList()) {
            if (user.getStatus() != null && user.getStatus() == 1
                    && operatorRole.getRoleId().equals(user.getRoleId())) {
                Map<String, Object> item = new HashMap<>();
                item.put("userId", user.getUserId());
                item.put("username", user.getUsername());
                item.put("realName", user.getRealName());
                item.put("employeeNo", user.getEmployeeNo());
                item.put("department", user.getDepartment());
                result.add(item);
            }
        }
        return result;
    }

    private User requireActiveUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        User user = userMapper.getUserById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException("用户已停用");
        }
        return user;
    }

    private AttendanceRecord buildRecord(User user, LocalDate date) {
        AttendanceRecord record = new AttendanceRecord();
        record.setUserId(user.getUserId());
        record.setEmployeeNo(user.getEmployeeNo());
        record.setRealName(user.getRealName());
        record.setDepartment(user.getDepartment());
        record.setAttendanceDate(date);
        return record;
    }

    private Role findRoleByCode(String roleCode) {
        for (Role role : roleMapper.roleList()) {
            if (roleCode.equalsIgnoreCase(role.getRoleCode())) {
                return role;
            }
        }
        return null;
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }
}
