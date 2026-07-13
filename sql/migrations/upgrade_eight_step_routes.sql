-- 升级工艺路线为八道生产工序，补齐设备/操作员，并为未完成工单补派缺失工序
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/migrations/upgrade_eight_step_routes.sql

USE display_manufacturing;
SET NAMES utf8mb4;

-- ========== 1. 补齐生产设备（电源板 / 接口板 / 外壳 / 支架） ==========
INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-PB-01', '1号电源板装配线', '电源板线', '电源板装配一车间', 'H线-01', '华南电子', 'HN-PB60', '2024-05-12', 'RUNNING', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-PB-01');

INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-PB-02', '2号电源板装配线', '电源板线', '电源板装配二车间', 'H线-02', '华南电子', 'HN-PB60', '2024-06-05', 'RUNNING', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-PB-02');

INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-IF-01', '1号接口板装配线', '接口板线', '接口板装配一车间', 'I线-01', '华南电子', 'HN-IF50', '2024-05-15', 'RUNNING', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-IF-01');

INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-IF-02', '2号接口板装配线', '接口板线', '接口板装配二车间', 'I线-02', '华南电子', 'HN-IF50', '2024-06-08', 'IDLE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-IF-02');

INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-SH-01', '1号外壳装配线', '外壳线', '外壳装配一车间', 'J线-01', '精工自动化', 'JF-SH80', '2024-07-01', 'RUNNING', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-SH-01');

INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-SH-02', '2号外壳装配线', '外壳线', '外壳装配二车间', 'J线-02', '精工自动化', 'JF-SH80', '2024-07-15', 'RUNNING', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-SH-02');

INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-BR-01', '1号支架底座装配线', '支架线', '支架底座装配一车间', 'K线-01', '华南机械', 'HN-BR40', '2024-08-01', 'RUNNING', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-BR-01');

INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-BR-02', '2号支架底座装配线', '支架线', '支架底座装配二车间', 'K线-02', '华南机械', 'HN-BR40', '2024-08-10', 'IDLE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-BR-02');

-- 确保既有四道工序设备归属正确
UPDATE equipment SET equipment_name = '1号显示屏加工线', equipment_type = '显示屏线', workshop = '显示屏加工一车间' WHERE equipment_code = 'EQ-DISP-01';
UPDATE equipment SET equipment_name = '2号显示屏加工线', equipment_type = '显示屏线', workshop = '显示屏加工二车间' WHERE equipment_code = 'EQ-DISP-02';
UPDATE equipment SET equipment_name = '3号显示屏加工线', equipment_type = '显示屏线', workshop = '显示屏加工三车间' WHERE equipment_code = 'EQ-DISP-03';
UPDATE equipment SET equipment_name = '1号主板装配线', equipment_type = '主板线', workshop = '主板装配一车间' WHERE equipment_code = 'EQ-MB-01';
UPDATE equipment SET equipment_name = '2号主板装配线', equipment_type = '主板线', workshop = '主板装配二车间' WHERE equipment_code = 'EQ-MB-02';
UPDATE equipment SET equipment_name = '3号主板装配线', equipment_type = '主板线', workshop = '主板装配三车间' WHERE equipment_code = 'EQ-MB-03';
UPDATE equipment SET workshop = '贴附一车间' WHERE equipment_code = 'EQ-001';
UPDATE equipment SET workshop = '贴附二车间' WHERE equipment_code IN ('EQ-002', 'EQ-010');
UPDATE equipment SET workshop = '组装一车间' WHERE equipment_code = 'EQ-003';
UPDATE equipment SET workshop = '组装二车间', equipment_type = '组装线' WHERE equipment_code = 'EQ-007';
UPDATE equipment SET workshop = '组装三车间', equipment_type = '组装线' WHERE equipment_code = 'EQ-012';

-- ========== 2. 补齐生产操作员（固定车间绑定） ==========
SET @operator_role_id = (SELECT role_id FROM role WHERE role_code = 'OPERATOR' LIMIT 1);
SET @pwd = '$2b$10$KoXI6kkD6AOp0WtyKXENlek9gKs2sAMFIHjUtme1CiPaSYnEhTzRm';

INSERT INTO `user` (role_id, username, password_hash, real_name, employee_no, phone, email, department, status, created_at, updated_at)
SELECT @operator_role_id, u.username, @pwd, u.real_name, u.emp_no, u.phone, u.email, '生产一部', 1, NOW(), NOW()
FROM (
    SELECT 'li_operator' AS username, '李操作' AS real_name, 'EMP202' AS emp_no, '13800001202' AS phone, 'li.op@display.com' AS email UNION ALL
    SELECT 'wu_operator', '吴操作', 'EMP205', '13800001205', 'wu.op@display.com' UNION ALL
    SELECT 'bai_operator', '白操作', 'EMP211', '13800001211', 'bai.op@display.com' UNION ALL
    SELECT 'huang_operator', '黄操作', 'EMP212', '13800001212', 'huang.op@display.com' UNION ALL
    SELECT 'xu_operator', '徐操作', 'EMP213', '13800001213', 'xu.op@display.com' UNION ALL
    SELECT 'yang_operator', '杨操作', 'EMP214', '13800001214', 'yang.op@display.com' UNION ALL
    SELECT 'he_operator', '何操作', 'EMP215', '13800001215', 'he.op@display.com' UNION ALL
    SELECT 'zhao_operator', '赵操作', 'EMP201', '13800001201', 'zhao.op@display.com' UNION ALL
    SELECT 'ma_operator', '马操作', 'EMP204', '13800001204', 'ma.op@display.com' UNION ALL
    SELECT 'feng_operator', '冯操作', 'EMP216', '13800001216', 'feng.op@display.com' UNION ALL
    SELECT 'wang_operator', '王操作', 'EMP104', '13800001104', 'wang.op@display.com' UNION ALL
    SELECT 'zhou_operator', '周操作', 'EMP206', '13800001206', 'zhou.op@display.com' UNION ALL
    SELECT 'gu_operator', '顾操作', 'EMP217', '13800001217', 'gu.op@display.com' UNION ALL
    SELECT 'xie_operator', '谢操作', 'EMP218', '13800001218', 'xie.op@display.com' UNION ALL
    SELECT 'sun_operator', '孙操作', 'EMP203', '13800001203', 'sun.op@display.com' UNION ALL
    SELECT 'chen_operator', '陈操作', 'EMP219', '13800001219', 'chen.op@display.com' UNION ALL
    SELECT 'lin_operator', '林操作', 'EMP220', '13800001220', 'lin.op@display.com' UNION ALL
    SELECT 'han_operator', '韩操作', 'EMP221', '13800001221', 'han.op@display.com' UNION ALL
    SELECT 'tang_operator', '唐操作', 'EMP222', '13800001222', 'tang.op@display.com'
) u
WHERE NOT EXISTS (SELECT 1 FROM `user` x WHERE x.username = u.username);

UPDATE `user` SET role_id = @operator_role_id, department = '生产一部', status = 1
WHERE username IN (
    'li_operator','wu_operator','bai_operator','huang_operator','xu_operator','yang_operator','he_operator',
    'zhao_operator','ma_operator','feng_operator','wang_operator','zhou_operator','gu_operator','xie_operator',
    'sun_operator','chen_operator','lin_operator','han_operator','tang_operator'
);

-- ========== 3. 工艺路线工序升级为八道生产工序 ==========
-- 先将 step_code 改为全局唯一临时码，避免 uk_process_step_route_step_code 冲突
UPDATE process_step SET step_code = CONCAT('TMP-', step_id) WHERE route_id IN (1, 2, 3);
UPDATE process_step SET step_no = step_no + 900 WHERE route_id IN (1, 2, 3);

-- 路线 1：已有工序改名重排
UPDATE process_step SET step_no = 10, step_code = 'STEP-01', step_name = '主板装配', standard_work_hours = 0.45, standard_equipment_type = '主板线', quality_required = 0
WHERE route_id = 1 AND (step_name LIKE '%主板装配%' OR step_name LIKE '%主板%');

UPDATE process_step SET step_no = 40, step_code = 'STEP-04', step_name = '显示屏加工', standard_work_hours = 0.35, standard_equipment_type = '显示屏线', quality_required = 0
WHERE route_id = 1 AND (step_name LIKE '%显示屏加工%' OR step_name LIKE '%显示屏%');

UPDATE process_step SET step_no = 50, step_code = 'STEP-05', step_name = '面板贴附', standard_work_hours = 0.50, standard_equipment_type = '贴附机', quality_required = 1
WHERE route_id = 1 AND (step_name LIKE '%面板贴附%' OR step_name LIKE '%贴附%');

UPDATE process_step SET step_no = 70, step_code = 'STEP-07', step_name = '整机组装', standard_work_hours = 0.80, standard_equipment_type = '组装线', quality_required = 0
WHERE route_id = 1 AND (step_name LIKE '%整机组装%' OR (step_name LIKE '%组装%' AND step_name NOT LIKE '%背光%'));

UPDATE process_step SET step_no = 90, step_code = 'STEP-09', step_name = '整机老化测试', standard_equipment_type = '老化架', quality_required = 1
WHERE route_id = 1 AND step_name LIKE '%老化%';

UPDATE process_step SET step_no = 100, step_code = 'STEP-10', step_name = '外观检验包装', standard_equipment_type = '包装线', quality_required = 1
WHERE route_id = 1 AND (step_name LIKE '%包装%' OR step_name LIKE '%外观%');

UPDATE process_step SET step_no = 20, step_code = 'STEP-02', step_name = '电源板装配', standard_work_hours = 0.40, standard_equipment_type = '电源板线', quality_required = 0
WHERE route_id = 1 AND step_name = '电源板装配';

UPDATE process_step SET step_no = 30, step_code = 'STEP-03', step_name = '接口板装配', standard_work_hours = 0.38, standard_equipment_type = '接口板线', quality_required = 0
WHERE route_id = 1 AND step_name = '接口板装配';

UPDATE process_step SET step_no = 60, step_code = 'STEP-06', step_name = '外壳装配', standard_work_hours = 0.55, standard_equipment_type = '外壳线', quality_required = 0
WHERE route_id = 1 AND step_name = '外壳装配';

UPDATE process_step SET step_no = 80, step_code = 'STEP-08', step_name = '支架底座装配', standard_work_hours = 0.42, standard_equipment_type = '支架线', quality_required = 0
WHERE route_id = 1 AND step_name = '支架底座装配';

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 1, 20, 'STEP-02', '电源板装配', 0.40, '电源板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 1 AND step_name = '电源板装配');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 1, 30, 'STEP-03', '接口板装配', 0.38, '接口板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 1 AND step_name = '接口板装配');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 1, 60, 'STEP-06', '外壳装配', 0.55, '外壳线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 1 AND step_name = '外壳装配');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 1, 80, 'STEP-08', '支架底座装配', 0.42, '支架线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 1 AND step_name = '支架底座装配');

-- 路线 2
UPDATE process_step SET step_no = 10, step_code = 'STEP-01', step_name = '主板装配', standard_work_hours = 0.50, standard_equipment_type = '主板线', quality_required = 0
WHERE route_id = 2 AND (step_name LIKE '%主板装配%' OR step_name LIKE '%主板%');

UPDATE process_step SET step_no = 40, step_code = 'STEP-04', step_name = '显示屏加工', standard_work_hours = 0.40, standard_equipment_type = '显示屏线', quality_required = 0
WHERE route_id = 2 AND (step_name LIKE '%显示屏加工%' OR step_name LIKE '%显示屏%');

UPDATE process_step SET step_no = 50, step_code = 'STEP-05', step_name = '面板贴附', standard_work_hours = 0.60, standard_equipment_type = '贴附机', quality_required = 1
WHERE route_id = 2 AND (step_name LIKE '%面板贴附%' OR step_name LIKE '%贴附%');

UPDATE process_step SET step_no = 70, step_code = 'STEP-07', step_name = '整机组装', standard_work_hours = 1.00, standard_equipment_type = '组装线', quality_required = 0
WHERE route_id = 2 AND (step_name LIKE '%整机组装%' OR (step_name LIKE '%组装%' AND step_name NOT LIKE '%背光%'));

UPDATE process_step SET step_no = 90, step_code = 'STEP-09', step_name = '整机老化测试', standard_equipment_type = '老化架', quality_required = 1
WHERE route_id = 2 AND step_name LIKE '%老化%';

UPDATE process_step SET step_no = 100, step_code = 'STEP-10', step_name = '电竞调校', standard_equipment_type = '调校台', quality_required = 1
WHERE route_id = 2 AND step_name LIKE '%调校%';

UPDATE process_step SET step_no = 110, step_code = 'STEP-11', step_name = '外观检验包装', standard_equipment_type = '包装线', quality_required = 1
WHERE route_id = 2 AND (step_name LIKE '%包装%' OR step_name LIKE '%外观%');

UPDATE process_step SET step_no = 20, step_code = 'STEP-02', step_name = '电源板装配', standard_work_hours = 0.42, standard_equipment_type = '电源板线', quality_required = 0
WHERE route_id = 2 AND step_name = '电源板装配';

UPDATE process_step SET step_no = 30, step_code = 'STEP-03', step_name = '接口板装配', standard_work_hours = 0.40, standard_equipment_type = '接口板线', quality_required = 0
WHERE route_id = 2 AND step_name = '接口板装配';

UPDATE process_step SET step_no = 60, step_code = 'STEP-06', step_name = '外壳装配', standard_work_hours = 0.58, standard_equipment_type = '外壳线', quality_required = 0
WHERE route_id = 2 AND step_name = '外壳装配';

UPDATE process_step SET step_no = 80, step_code = 'STEP-08', step_name = '支架底座装配', standard_work_hours = 0.45, standard_equipment_type = '支架线', quality_required = 0
WHERE route_id = 2 AND step_name = '支架底座装配';

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 2, 20, 'STEP-02', '电源板装配', 0.42, '电源板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 2 AND step_name = '电源板装配');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 2, 30, 'STEP-03', '接口板装配', 0.40, '接口板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 2 AND step_name = '接口板装配');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 2, 60, 'STEP-06', '外壳装配', 0.58, '外壳线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 2 AND step_name = '外壳装配');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 2, 80, 'STEP-08', '支架底座装配', 0.45, '支架线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 2 AND step_name = '支架底座装配');

-- 路线 3（若存在）
INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 10, 'STEP-01', '主板装配', 0.55, '主板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_name = '主板装配');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 20, 'STEP-02', '电源板装配', 0.48, '电源板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_name = '电源板装配');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 30, 'STEP-03', '接口板装配', 0.45, '接口板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_name = '接口板装配');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 40, 'STEP-04', '显示屏加工', 0.45, '显示屏线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_name = '显示屏加工');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 50, 'STEP-05', '面板贴附', 0.65, '贴附机', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_name = '面板贴附');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 60, 'STEP-06', '外壳装配', 0.60, '外壳线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_name = '外壳装配');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 70, 'STEP-07', '整机组装', 1.10, '组装线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_name = '整机组装');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 80, 'STEP-08', '支架底座装配', 0.48, '支架线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_name = '支架底座装配');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 90, 'STEP-09', '整机老化测试', 6.00, '老化架', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_name LIKE '%老化%');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 100, 'STEP-10', '外观检验包装', 0.40, '包装线', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_name LIKE '%包装%');

UPDATE process_step SET step_no = 20, step_code = 'STEP-02', step_name = '电源板装配', standard_work_hours = 0.48, standard_equipment_type = '电源板线', quality_required = 0
WHERE route_id = 3 AND step_name = '电源板装配';

UPDATE process_step SET step_no = 30, step_code = 'STEP-03', step_name = '接口板装配', standard_work_hours = 0.45, standard_equipment_type = '接口板线', quality_required = 0
WHERE route_id = 3 AND step_name = '接口板装配';

UPDATE process_step SET step_no = 60, step_code = 'STEP-06', step_name = '外壳装配', standard_work_hours = 0.60, standard_equipment_type = '外壳线', quality_required = 0
WHERE route_id = 3 AND step_name = '外壳装配';

UPDATE process_step SET step_no = 80, step_code = 'STEP-08', step_name = '支架底座装配', standard_work_hours = 0.48, standard_equipment_type = '支架线', quality_required = 0
WHERE route_id = 3 AND step_name = '支架底座装配';

-- 清理仅含临时编码且不属于目标工序的残留行
DELETE FROM process_step
WHERE route_id IN (1, 2, 3)
  AND step_code LIKE 'TMP-%'
  AND step_name NOT IN (
      '主板装配', '电源板装配', '接口板装配', '显示屏加工', '面板贴附', '外壳装配', '整机组装', '支架底座装配',
      '整机老化测试', '电竞调校', '外观检验包装'
  );

-- ========== 4. 未完成工单补派八道生产工序 ==========
SET @next_dispatch_seq := COALESCE(
    (SELECT MAX(CAST(SUBSTRING(dispatch_no, 9) AS UNSIGNED))
     FROM dispatch_task
     WHERE dispatch_no LIKE CONCAT('DT', DATE_FORMAT(NOW(), '%Y%m'), '%')),
    0
);

SET @manager_id := (SELECT user_id FROM `user` WHERE username = 'li_manager' LIMIT 1);

-- 补派辅助：按工序名 + 主责操作员 + 设备
INSERT INTO dispatch_task (
    dispatch_no, work_order_id, step_id, operator_id, equipment_id,
    assigned_quantity, accepted_quantity, completed_quantity,
    assigned_by, assigned_at, status, remark, created_at, updated_at
)
SELECT
    CONCAT('DT', DATE_FORMAT(NOW(), '%Y%m'), LPAD((@next_dispatch_seq := @next_dispatch_seq + 1), 3, '0')),
    wo.work_order_id,
    ps.step_id,
    (SELECT user_id FROM `user` WHERE username = cfg.operator LIMIT 1),
    (SELECT equipment_id FROM equipment WHERE equipment_code = cfg.equip LIMIT 1),
    wo.planned_quantity,
    0,
    0,
    @manager_id,
    NOW(),
    'ASSIGNED',
    CONCAT('补派-', ps.step_name, '（', wo.work_order_no, '）'),
    NOW(),
    NOW()
FROM work_order wo
JOIN (
    SELECT '主板装配' AS step_name, 'li_operator' AS operator, 'EQ-MB-01' AS equip UNION ALL
    SELECT '电源板装配', 'huang_operator', 'EQ-PB-01' UNION ALL
    SELECT '接口板装配', 'yang_operator', 'EQ-IF-01' UNION ALL
    SELECT '显示屏加工', 'zhao_operator', 'EQ-DISP-01' UNION ALL
    SELECT '面板贴附', 'wang_operator', 'EQ-001' UNION ALL
    SELECT '外壳装配', 'gu_operator', 'EQ-SH-01' UNION ALL
    SELECT '整机组装', 'sun_operator', 'EQ-003' UNION ALL
    SELECT '支架底座装配', 'han_operator', 'EQ-BR-01'
) cfg
JOIN process_step ps ON ps.route_id = wo.route_id AND ps.step_name = cfg.step_name
WHERE wo.status IN ('RELEASED', 'DISPATCHED', 'PRODUCING', 'QC_PENDING', 'RUNNING')
  AND NOT EXISTS (
      SELECT 1 FROM dispatch_task dt
      JOIN process_step s ON s.step_id = dt.step_id
      WHERE dt.work_order_id = wo.work_order_id AND s.step_name = ps.step_name
  );

-- ========== 5. 为在制工单生成随机种子报工进度（仅补全缺失工序的派工完成量） ==========
-- 使用工单 ID 作为种子，为已有派工但完成量为 0 的生产工序写入合理进度
UPDATE dispatch_task dt
JOIN process_step ps ON ps.step_id = dt.step_id
JOIN work_order wo ON wo.work_order_id = dt.work_order_id
SET dt.completed_quantity = LEAST(
        wo.planned_quantity,
        GREATEST(
            0,
            FLOOR(wo.planned_quantity * (
                0.15 + 0.65 * (
                    (wo.work_order_id * 17 + ps.step_no * 13 + ASCII(SUBSTRING(ps.step_name, 1, 1))) % 100
                ) / 100.0
            ))
        )
    ),
    dt.accepted_quantity = LEAST(wo.planned_quantity, COALESCE(dt.accepted_quantity, dt.assigned_quantity)),
    dt.status = CASE
        WHEN dt.completed_quantity >= wo.planned_quantity THEN 'COMPLETED'
        WHEN dt.completed_quantity > 0 THEN 'RUNNING'
        ELSE dt.status
    END,
    dt.updated_at = NOW()
WHERE wo.status IN ('DISPATCHED', 'PRODUCING', 'QC_PENDING', 'RUNNING')
  AND ps.step_name IN ('主板装配','电源板装配','接口板装配','显示屏加工','面板贴附','外壳装配','整机组装','支架底座装配')
  AND COALESCE(dt.completed_quantity, 0) = 0
  AND dt.status IN ('ASSIGNED', 'ACCEPTED', 'PRODUCING', 'RUNNING');

-- ========== 6. 重算工单成品瓶颈完成量（八道生产工序） ==========
UPDATE work_order wo
SET completed_quantity = COALESCE((
    SELECT MIN(stage_total)
    FROM (
        SELECT SUM(dt.completed_quantity) AS stage_total
        FROM dispatch_task dt
        JOIN process_step ps ON ps.step_id = dt.step_id
        WHERE dt.work_order_id = wo.work_order_id
          AND ps.step_name NOT LIKE '%老化%'
          AND ps.step_name NOT LIKE '%调校%'
          AND ps.step_name NOT LIKE '%包装%'
          AND ps.step_name NOT LIKE '%质检%'
          AND ps.step_name NOT LIKE '%检验%'
          AND ps.step_name NOT LIKE '%终检%'
          AND ps.step_name NOT LIKE '%发货%'
          AND ps.step_name NOT LIKE '%售后%'
        GROUP BY CASE
            WHEN ps.step_name LIKE '%主板装配%' OR (ps.step_name LIKE '%主板%' AND ps.step_name NOT LIKE '%电源%' AND ps.step_name NOT LIKE '%接口%') THEN 1
            WHEN ps.step_name LIKE '%电源板%' THEN 2
            WHEN ps.step_name LIKE '%接口板%' THEN 3
            WHEN ps.step_name LIKE '%显示屏%' THEN 4
            WHEN ps.step_name LIKE '%贴附%' THEN 5
            WHEN ps.step_name LIKE '%外壳%' THEN 6
            WHEN ps.step_name LIKE '%整机组装%' OR (ps.step_name LIKE '%组装%' AND ps.step_name NOT LIKE '%背光%') THEN 7
            WHEN ps.step_name LIKE '%支架%' OR ps.step_name LIKE '%底座%' THEN 8
            ELSE 99 END
    ) t
), 0),
updated_at = NOW();

-- ========== 7. 计划员排程明细升级为八道生产工序 ==========
DELETE FROM production_plan_schedule;

INSERT INTO production_plan_schedule (
    plan_id, step_id, step_no, step_name, workshop, equipment_id, equipment_code,
    planned_quantity, planned_start, planned_end, standard_hours, sort_no, created_at, updated_at
)
SELECT
    p.plan_id,
    ps.step_id,
    ps.step_no,
    ps.step_name,
    COALESCE(e.workshop, CONCAT(ps.step_name, '车间')),
    e.equipment_id,
    e.equipment_code,
    COALESCE(pi.planned_quantity, 1),
    COALESCE(p.planned_start_date, CURDATE()),
    COALESCE(p.planned_end_date, DATE_ADD(CURDATE(), INTERVAL 7 DAY)),
    COALESCE(ps.standard_work_hours, 1),
    ROW_NUMBER() OVER (PARTITION BY p.plan_id ORDER BY ps.step_no),
    NOW(),
    NOW()
FROM production_plan p
JOIN production_plan_item pi ON pi.plan_id = p.plan_id
JOIN process_route r ON r.material_id = pi.material_id AND (r.status IS NULL OR r.status = 1)
JOIN process_step ps ON ps.route_id = r.route_id
    AND (ps.status IS NULL OR ps.status = 1)
    AND ps.step_name IN (
        '主板装配', '电源板装配', '接口板装配', '显示屏加工',
        '面板贴附', '外壳装配', '整机组装', '支架底座装配'
    )
LEFT JOIN equipment e ON e.equipment_type = ps.standard_equipment_type
    AND e.status NOT IN ('FAULT', 'MAINTENANCE')
    AND e.equipment_id = (
        SELECT MIN(e2.equipment_id)
        FROM equipment e2
        WHERE e2.equipment_type = ps.standard_equipment_type
          AND e2.status NOT IN ('FAULT', 'MAINTENANCE')
    );
