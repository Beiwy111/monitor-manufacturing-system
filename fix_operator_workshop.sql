-- 修复「两个王操作」及组装工序无可用操作员
USE display_manufacturing;
SET NAMES utf8mb4;

-- 1. qc01 实为质检员，纠正角色与姓名，避免与 wang_operator 重名
UPDATE `user` SET role_id = (SELECT role_id FROM role WHERE role_code = 'QC' LIMIT 1),
  real_name = '王质检', department = '质量部'
WHERE username = 'qc01';

-- 2. 补齐组装二/三车间专职操作员（原 operator01/operator02 为计划员/主管账号，不能派工）
SET @operator_role_id = (SELECT role_id FROM role WHERE role_code = 'OPERATOR' LIMIT 1);
SET @pwd = '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG';

INSERT INTO `user` (role_id, username, password_hash, real_name, employee_no, phone, email, department, status, created_at, updated_at)
SELECT @operator_role_id, 'chen_operator', @pwd, '陈操作', 'EMP207', '13800001207', 'chen.op@display.com', '生产一部', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'chen_operator');

INSERT INTO `user` (role_id, username, password_hash, real_name, employee_no, phone, email, department, status, created_at, updated_at)
SELECT @operator_role_id, 'lin_operator', @pwd, '林操作', 'EMP208', '13800001208', 'lin.op@display.com', '生产二部', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'lin_operator');

UPDATE `user` SET real_name='陈操作', department='生产一部', role_id=@operator_role_id WHERE username='chen_operator';
UPDATE `user` SET real_name='林操作', department='生产二部', role_id=@operator_role_id WHERE username='lin_operator';
