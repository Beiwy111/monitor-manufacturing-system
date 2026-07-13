-- ================================================================
-- 考勤管理 & 排班日历模块
-- 目标库：display_manufacturing
-- ================================================================

USE `display_manufacturing`;

-- 1. 考勤记录表
CREATE TABLE IF NOT EXISTS `attendance_record` (
    `record_id`        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `employee_no`      VARCHAR(32)  DEFAULT NULL COMMENT '工号',
    `real_name`        VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    `department`       VARCHAR(64)  DEFAULT NULL COMMENT '部门',
    `attendance_date`  DATE         NOT NULL COMMENT '考勤日期',
    `check_in_time`    DATETIME     DEFAULT NULL COMMENT '上班打卡时间',
    `check_out_time`   DATETIME     DEFAULT NULL COMMENT '下班打卡时间',
    `status`           VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/LATE/EARLY_LEAVE/ABSENT',
    `remark`           VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`record_id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `attendance_date`),
    KEY `idx_attendance_date` (`attendance_date`),
    KEY `idx_attendance_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录表';

-- 2. 排班表
CREATE TABLE IF NOT EXISTS `shift_schedule` (
    `schedule_id`    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '排班ID',
    `schedule_date`  DATE         NOT NULL COMMENT '排班日期',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `employee_no`    VARCHAR(32)  DEFAULT NULL COMMENT '工号',
    `real_name`      VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    `shift_type`     VARCHAR(20)  NOT NULL DEFAULT 'DAY' COMMENT '班次：DAY/NIGHT/REST',
    `workshop`       VARCHAR(64)  DEFAULT NULL COMMENT '负责车间',
    `remark`         VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `created_by`     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`schedule_id`),
    UNIQUE KEY `uk_schedule_user_date` (`user_id`, `schedule_date`),
    KEY `idx_schedule_date` (`schedule_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工排班表';

-- 3. 菜单：考勤管理（系统管理员）
INSERT INTO `sys_menu` (`menu_code`, `menu_name`, `parent_id`, `menu_level`, `api_path`, `business_table`, `icon`, `sort_no`, `status`)
SELECT 'attendance', '考勤管理', NULL, 1, NULL, NULL, 'calendar', 12, 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_code` = 'attendance');

SET @attendance_parent := (SELECT menu_id FROM `sys_menu` WHERE `menu_code` = 'attendance' LIMIT 1);

INSERT INTO `sys_menu` (`menu_code`, `menu_name`, `parent_id`, `menu_level`, `api_path`, `business_table`, `sort_no`, `status`)
SELECT 'attendance:record', '考勤记录', @attendance_parent, 2, '/attendance/record/list', 'attendance_record', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_code` = 'attendance:record');

INSERT INTO `sys_menu` (`menu_code`, `menu_name`, `parent_id`, `menu_level`, `api_path`, `business_table`, `sort_no`, `status`)
SELECT 'attendance:statistics', '考勤统计', @attendance_parent, 2, '/attendance/statistics', 'attendance_record', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_code` = 'attendance:statistics');

-- 4. 菜单：排班日历（生产主管，挂在生产管理下）
INSERT INTO `sys_menu` (`menu_code`, `menu_name`, `parent_id`, `menu_level`, `api_path`, `business_table`, `sort_no`, `status`)
SELECT 'production:shiftCalendar', '排班日历', 4, 2, '/attendance/schedule/list', 'shift_schedule', 9, 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_code` = 'production:shiftCalendar');

-- 5. 角色菜单授权
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `role` r
CROSS JOIN `sys_menu` m
WHERE r.role_code = 'ADMIN'
  AND m.menu_code IN ('attendance', 'attendance:record', 'attendance:statistics')
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` srm
    WHERE srm.role_id = r.role_id AND srm.menu_id = m.menu_id
  );

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `role` r
JOIN `sys_menu` m ON m.menu_code = 'production:shiftCalendar'
WHERE r.role_code = 'MANAGER'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` srm
    WHERE srm.role_id = r.role_id AND srm.menu_id = m.menu_id
  );

-- 6. 演示考勤数据（近30天操作员打卡记录）
INSERT INTO `attendance_record`
  (`user_id`, `employee_no`, `real_name`, `department`, `attendance_date`, `check_in_time`, `check_out_time`, `status`)
SELECT
  u.user_id,
  u.employee_no,
  u.real_name,
  u.department,
  d.att_date,
  CONCAT(d.att_date, ' ', d.in_time),
  CONCAT(d.att_date, ' ', d.out_time),
  d.att_status
FROM `user` u
JOIN `role` r ON r.role_id = u.role_id AND r.role_code = 'OPERATOR'
CROSS JOIN (
  SELECT CURDATE() - INTERVAL n DAY AS att_date,
         CASE WHEN n % 7 = 0 THEN '09:15:00' WHEN n % 5 = 0 THEN '08:45:00' ELSE '08:05:00' END AS in_time,
         CASE WHEN n % 11 = 0 THEN '16:30:00' ELSE '17:10:00' END AS out_time,
         CASE WHEN n % 7 = 0 THEN 'LATE' WHEN n % 11 = 0 THEN 'EARLY_LEAVE' ELSE 'NORMAL' END AS att_status
  FROM (
    SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
    UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
    UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
    UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
  ) nums
) d
WHERE u.status = 1
  AND d.att_date < CURDATE()
  AND NOT EXISTS (
    SELECT 1 FROM `attendance_record` ar
    WHERE ar.user_id = u.user_id AND ar.attendance_date = d.att_date
  )
LIMIT 500;
