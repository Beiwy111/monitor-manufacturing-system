SET NAMES utf8mb4;
-- 每角色一名可登录用户；初始密码 123456（请由管理员在用户管理中修改）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/init/seed_business_roles_users.sql
SET @pwd = '$2b$10$KoXI6kkD6AOp0WtyKXENlek9gKs2sAMFIHjUtme1CiPaSYnEhTzRm';

UPDATE role SET role_name = '系统管理员', status = 1 WHERE role_code = 'ADMIN';
UPDATE role SET role_name = '订单管理员', status = 1 WHERE role_code = 'ORDER';
UPDATE role SET role_name = '计划员', status = 1 WHERE role_code = 'PLANNER';
UPDATE role SET role_name = '生产主管', status = 1 WHERE role_code = 'MANAGER';
UPDATE role SET role_name = '生产操作员', status = 1 WHERE role_code = 'OPERATOR';
UPDATE role SET role_name = '质检员', status = 1 WHERE role_code = 'QC';
UPDATE role SET role_name = '采购员', status = 1 WHERE role_code = 'PURCHASER';
UPDATE role SET role_name = '仓管员', status = 1 WHERE role_code = 'WAREHOUSE';
UPDATE role SET role_name = '设备维护人员', status = 1 WHERE role_code = 'DEVICE';
UPDATE role SET role_name = '售后专员', status = 1 WHERE role_code = 'SERVICE';
UPDATE role SET role_name = '财务成本人员', status = 1 WHERE role_code = 'COST';

UPDATE `user` SET status = 0 WHERE username IN (
  'planner01','operator01','operator02','qc01','purchase01','warehouse01','service01'
);

UPDATE `user` u JOIN role r ON r.role_code = 'ADMIN'
SET u.password_hash = @pwd, u.status = 1, u.role_id = r.role_id WHERE u.username = 'admin';

UPDATE `user` u JOIN role r ON r.role_code = 'ORDER'
SET u.username = 'zhang_order', u.password_hash = @pwd, u.real_name = '张订单', u.status = 1, u.role_id = r.role_id
WHERE u.user_id = 31 OR u.username = 'zhang_order';

UPDATE `user` u JOIN role r ON r.role_code = 'PLANNER'
SET u.username = 'li_planner', u.password_hash = @pwd, u.real_name = '李计划', u.status = 1, u.role_id = r.role_id
WHERE u.user_id = 32 OR u.username = 'li_planner';

UPDATE `user` u JOIN role r ON r.role_code = 'MANAGER'
SET u.username = 'li_manager', u.password_hash = @pwd, u.real_name = '李主管', u.status = 1, u.role_id = r.role_id
WHERE u.user_id = 33 OR u.username = 'li_manager';

UPDATE `user` u JOIN role r ON r.role_code = 'OPERATOR'
SET u.username = 'wang_operator', u.password_hash = @pwd, u.real_name = '王操作', u.status = 1, u.role_id = r.role_id
WHERE u.user_id = 34 OR u.username = 'wang_operator';

UPDATE `user` u JOIN role r ON r.role_code = 'QC'
SET u.username = 'chen_qc', u.password_hash = @pwd, u.real_name = '陈质检', u.status = 1, u.role_id = r.role_id
WHERE u.user_id = 35 OR u.username = 'chen_qc';

UPDATE `user` u JOIN role r ON r.role_code = 'PURCHASER'
SET u.username = 'liu_purchase', u.password_hash = @pwd, u.real_name = '刘采购', u.status = 1, u.role_id = r.role_id
WHERE u.user_id = 36 OR u.username = 'liu_purchase';

UPDATE `user` u JOIN role r ON r.role_code = 'WAREHOUSE'
SET u.username = 'zhou_warehouse', u.password_hash = @pwd, u.real_name = '周仓管', u.status = 1, u.role_id = r.role_id
WHERE u.user_id = 37 OR u.username = 'zhou_warehouse';

UPDATE `user` u JOIN role r ON r.role_code = 'DEVICE'
SET u.username = 'zhou_device', u.password_hash = @pwd, u.real_name = '周设备', u.status = 1, u.role_id = r.role_id
WHERE u.user_id = 41 OR (u.username = 'zhou_device' AND u.status = 1);

UPDATE `user` u JOIN role r ON r.role_code = 'SERVICE'
SET u.username = 'wu_service', u.password_hash = @pwd, u.real_name = '吴售后', u.status = 1, u.role_id = r.role_id
WHERE u.user_id = 42 OR (u.username = 'wu_service' AND u.status = 1);

UPDATE `user` u JOIN role r ON r.role_code = 'COST'
SET u.username = 'zheng_cost', u.password_hash = @pwd, u.real_name = '郑财务', u.status = 1, u.role_id = r.role_id
WHERE u.user_id = 43 OR (u.username = 'zheng_cost' AND u.status = 1);

UPDATE `user` SET status = 0 WHERE username IN ('sales','planner','manager','operator','qc','buyer','warehouse','device','aftersale','cost');
