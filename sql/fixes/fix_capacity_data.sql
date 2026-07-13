-- 产能基础数据修复：设备 + 操作员（在 display_manufacturing 库执行，可重复执行）
-- Windows 推荐执行：
--   cmd /c "mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/fixes/fix_capacity_data.sql"
-- 若通过 PowerShell 管道导入中文乱码，请用上面 cmd 方式，或执行文末「编码修复」段
USE display_manufacturing;

SET NAMES utf8mb4;

-- 1. 修复现有故障/待机设备，恢复可用产能
UPDATE equipment SET status = 'RUNNING', updated_at = NOW()
WHERE equipment_code IN ('EQ-003', 'EQ-006');

UPDATE equipment SET status = 'IDLE', updated_at = NOW()
WHERE equipment_code = 'EQ-002' AND status NOT IN ('RUNNING', 'IDLE');

-- 2. 按工艺工序 standard_equipment_type 补齐设备（每种至少 2 台可用）
INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at, created_at, updated_at)
VALUES
('EQ-007', '2号组装流水线', '组装线', '生产一部', 'B线-02', '华南机械', 'HN-ZX100', '2024-08-01', 'RUNNING', NOW(), NOW(), NOW()),
('EQ-008', '2号自动包装线', '包装线', '生产二部', 'E线-02', '华南机械', 'HN-BZ80', '2024-09-01', 'RUNNING', NOW(), NOW(), NOW()),
('EQ-009', '老化测试架B区', '老化架', '生产二部', 'C区-02', '可靠性设备', 'KK-LH50', '2024-10-01', 'RUNNING', NOW(), NOW(), NOW()),
('EQ-010', '3号自动贴附机', '贴附机', '生产一部', 'A线-03', '精工自动化', 'JF-TF200', '2025-02-01', 'IDLE', NOW(), NOW(), NOW()),
('EQ-011', '2号电竞调校台', '调校台', '生产二部', 'D区-02', '显示科技', 'XS-TJ300', '2025-03-01', 'IDLE', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
  equipment_name = VALUES(equipment_name),
  equipment_type = VALUES(equipment_type),
  workshop = VALUES(workshop),
  workstation = VALUES(workstation),
  status = VALUES(status),
  updated_at = NOW();

-- 3. 补齐生产操作员（角色 OPERATOR，默认密码 123456，与 fix_login_password.sql 一致）
SET @operator_role_id = (SELECT role_id FROM role WHERE role_code = 'OPERATOR' LIMIT 1);
SET @pwd = '$2b$10$KoXI6kkD6AOp0WtyKXENlek9gKs2sAMFIHjUtme1CiPaSYnEhTzRm';

INSERT INTO `user` (role_id, username, password_hash, real_name, employee_no, phone, email, department, status, created_at, updated_at)
SELECT @operator_role_id, 'zhao_operator', @pwd, '赵操作', 'EMP201', '13800001201', 'zhao.op@display.com', '生产一部', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'zhao_operator');

INSERT INTO `user` (role_id, username, password_hash, real_name, employee_no, phone, email, department, status, created_at, updated_at)
SELECT @operator_role_id, 'li_operator', @pwd, '李操作', 'EMP202', '13800001202', 'li.op@display.com', '生产一部', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'li_operator');

INSERT INTO `user` (role_id, username, password_hash, real_name, employee_no, phone, email, department, status, created_at, updated_at)
SELECT @operator_role_id, 'sun_operator', @pwd, '孙操作', 'EMP203', '13800001203', 'sun.op@display.com', '生产二部', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'sun_operator');

INSERT INTO `user` (role_id, username, password_hash, real_name, employee_no, phone, email, department, status, created_at, updated_at)
SELECT @operator_role_id, 'ma_operator', @pwd, '马操作', 'EMP204', '13800001204', 'ma.op@display.com', '生产二部', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'ma_operator');

INSERT INTO `user` (role_id, username, password_hash, real_name, employee_no, phone, email, department, status, created_at, updated_at)
SELECT @operator_role_id, 'wu_operator', @pwd, '吴操作', 'EMP205', '13800001205', 'wu.op@display.com', '生产二部', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'wu_operator');

INSERT INTO `user` (role_id, username, password_hash, real_name, employee_no, phone, email, department, status, created_at, updated_at)
SELECT @operator_role_id, 'zhou_operator', @pwd, '周操作', 'EMP206', '13800001206', 'zhou.op@display.com', '生产一部', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'zhou_operator');

-- 4. 校验：各工序设备类型是否都有可用设备
SELECT ps.standard_equipment_type AS required_type,
       COUNT(e.equipment_id) AS available_count
FROM (SELECT DISTINCT standard_equipment_type FROM process_step WHERE status = 1) ps
LEFT JOIN equipment e ON e.equipment_type = ps.standard_equipment_type
    AND e.status NOT IN ('FAULT', 'MAINTENANCE')
GROUP BY ps.standard_equipment_type
ORDER BY ps.standard_equipment_type;

SELECT COUNT(*) AS active_operators
FROM `user` u
JOIN role r ON u.role_id = r.role_id
WHERE r.role_code = 'OPERATOR' AND u.status = 1;

-- 5. 编码修复（仅当导入后 equipment_type / real_name 出现乱码时执行）
UPDATE equipment SET equipment_type='组装线', equipment_name='2号组装流水线', workshop='生产一部' WHERE equipment_code='EQ-007';
UPDATE equipment SET equipment_type='包装线', equipment_name='2号自动包装线', workshop='生产二部' WHERE equipment_code='EQ-008';
UPDATE equipment SET equipment_type='老化架', equipment_name='老化测试架B区', workshop='生产二部' WHERE equipment_code='EQ-009';
UPDATE equipment SET equipment_type='贴附机', equipment_name='3号自动贴附机', workshop='生产一部' WHERE equipment_code='EQ-010';
UPDATE equipment SET equipment_type='调校台', equipment_name='2号电竞调校台', workshop='生产二部' WHERE equipment_code='EQ-011';
UPDATE `user` SET real_name='赵操作' WHERE username='zhao_operator';
UPDATE `user` SET real_name='李操作' WHERE username='li_operator';
UPDATE `user` SET real_name='孙操作' WHERE username='sun_operator';
UPDATE `user` SET real_name='马操作' WHERE username='ma_operator';
UPDATE `user` SET real_name='吴操作' WHERE username='wu_operator';
UPDATE `user` SET real_name='周操作' WHERE username='zhou_operator';
