-- 升级工艺路线为四道生产工序，并为未完成工单补派「显示屏加工」「主板装配」
-- 执行：mysql -uroot -p display_manufacturing < upgrade_four_step_routes.sql

USE display_manufacturing;
SET NAMES utf8mb4;

-- ========== 1. 补齐生产设备 ==========
INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-DISP-01', '1号显示屏加工线', '显示屏线', '显示屏加工一车间', 'F线-01', '显示科技', 'XS-DP100', '2024-05-10', 'RUNNING', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-DISP-01');

INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-MB-01', '1号主板装配线', '主板线', '主板装配一车间', 'G线-01', '华南电子', 'HN-MB80', '2024-05-10', 'RUNNING', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-MB-01');

UPDATE equipment SET equipment_name = '2号显示屏加工线', equipment_type = '显示屏线', workshop = '显示屏加工二车间' WHERE equipment_code = 'EQ-DISP-02';
UPDATE equipment SET equipment_name = '3号显示屏加工线', equipment_type = '显示屏线', workshop = '显示屏加工三车间' WHERE equipment_code = 'EQ-DISP-03';
UPDATE equipment SET equipment_name = '2号主板装配线', equipment_type = '主板线', workshop = '主板装配二车间' WHERE equipment_code = 'EQ-MB-02';
UPDATE equipment SET equipment_name = '3号主板装配线', equipment_type = '主板线', workshop = '主板装配三车间' WHERE equipment_code = 'EQ-MB-03';
UPDATE equipment SET workshop = '贴附一车间' WHERE equipment_code = 'EQ-001';
UPDATE equipment SET workshop = '贴附二车间' WHERE equipment_code IN ('EQ-002', 'EQ-010');
UPDATE equipment SET workshop = '组装一车间' WHERE equipment_code = 'EQ-003';
UPDATE equipment SET workshop = '组装二车间', equipment_type = '组装线' WHERE equipment_code = 'EQ-007';
UPDATE equipment SET workshop = '组装三车间', equipment_type = '组装线' WHERE equipment_code = 'EQ-012';

-- ========== 2. 工艺路线 1：补齐四道生产工序 ==========
UPDATE process_step SET step_no = 901, step_code = 'STEP-TMP-01'
WHERE route_id = 1 AND step_code = 'STEP-01';

UPDATE process_step SET step_no = 902, step_code = 'STEP-TMP-02'
WHERE route_id = 1 AND step_code = 'STEP-02';

UPDATE process_step SET step_no = 903, step_code = 'STEP-TMP-03'
WHERE route_id = 1 AND step_code = 'STEP-03';

UPDATE process_step SET step_no = 904, step_code = 'STEP-TMP-04'
WHERE route_id = 1 AND step_code = 'STEP-04';

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 1, 10, 'STEP-00A', '显示屏加工', 0.35, '显示屏线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 1 AND step_code = 'STEP-00A');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 1, 20, 'STEP-00B', '主板装配', 0.45, '主板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 1 AND step_code = 'STEP-00B');

UPDATE process_step SET step_no = 30, step_code = 'STEP-03', step_name = '面板贴附', standard_equipment_type = '贴附机'
WHERE route_id = 1 AND step_code = 'STEP-TMP-01';

UPDATE process_step SET step_no = 40, step_code = 'STEP-04', step_name = '整机组装', standard_equipment_type = '组装线'
WHERE route_id = 1 AND step_code = 'STEP-TMP-02';

UPDATE process_step SET step_no = 50, step_code = 'STEP-05', quality_required = 1
WHERE route_id = 1 AND step_code = 'STEP-TMP-03';

UPDATE process_step SET step_no = 60, step_code = 'STEP-06', quality_required = 1
WHERE route_id = 1 AND step_code = 'STEP-TMP-04';

-- ========== 3. 工艺路线 2：补齐四道生产工序 ==========
UPDATE process_step SET step_no = 901, step_code = 'STEP-TMP-01'
WHERE route_id = 2 AND step_code = 'STEP-01';

UPDATE process_step SET step_no = 902, step_code = 'STEP-TMP-02'
WHERE route_id = 2 AND step_code = 'STEP-02';

UPDATE process_step SET step_no = 903, step_code = 'STEP-TMP-03'
WHERE route_id = 2 AND step_code = 'STEP-03';

UPDATE process_step SET step_no = 904, step_code = 'STEP-TMP-04'
WHERE route_id = 2 AND step_code = 'STEP-04';

UPDATE process_step SET step_no = 905, step_code = 'STEP-TMP-05'
WHERE route_id = 2 AND step_code = 'STEP-05';

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 2, 10, 'STEP-00A', '显示屏加工', 0.40, '显示屏线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 2 AND step_code = 'STEP-00A');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 2, 20, 'STEP-00B', '主板装配', 0.50, '主板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 2 AND step_code = 'STEP-00B');

UPDATE process_step SET step_no = 30, step_code = 'STEP-03', step_name = '面板贴附', standard_equipment_type = '贴附机'
WHERE route_id = 2 AND step_code = 'STEP-TMP-01';

UPDATE process_step SET step_no = 40, step_code = 'STEP-04', step_name = '整机组装', standard_equipment_type = '组装线'
WHERE route_id = 2 AND step_code = 'STEP-TMP-02';

UPDATE process_step SET step_no = 50, step_code = 'STEP-05', quality_required = 1
WHERE route_id = 2 AND step_code = 'STEP-TMP-03';

UPDATE process_step SET step_no = 60, step_code = 'STEP-06', quality_required = 1
WHERE route_id = 2 AND step_code = 'STEP-TMP-04';

UPDATE process_step SET step_no = 70, step_code = 'STEP-07', quality_required = 1
WHERE route_id = 2 AND step_code = 'STEP-TMP-05';

-- ========== 4. 工艺路线 3：新建完整工序 ==========
INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 10, 'STEP-01', '显示屏加工', 0.45, '显示屏线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_code = 'STEP-01');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 20, 'STEP-02', '主板装配', 0.55, '主板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_code = 'STEP-02');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 30, 'STEP-03', '面板贴附', 0.65, '贴附机', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_code = 'STEP-03');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 40, 'STEP-04', '整机组装', 1.10, '组装线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_code = 'STEP-04');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 50, 'STEP-05', '整机老化测试', 6.00, '老化架', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_code = 'STEP-05');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 3, 60, 'STEP-06', '外观检验包装', 0.40, '包装线', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 3 AND step_code = 'STEP-06');

-- ========== 5. 未完成工单补派前两道生产工序 ==========
-- 显示屏加工 -> 赵操作员；主板装配 -> 李操作员；派工人 -> 李主管

SET @next_dispatch_seq := COALESCE(
    (SELECT MAX(CAST(SUBSTRING(dispatch_no, 9) AS UNSIGNED))
     FROM dispatch_task
     WHERE dispatch_no LIKE CONCAT('DT', DATE_FORMAT(NOW(), '%Y%m'), '%')),
    0
);

INSERT INTO dispatch_task (
    dispatch_no, work_order_id, step_id, operator_id, equipment_id,
    assigned_quantity, accepted_quantity, completed_quantity,
    assigned_by, assigned_at, status, remark, created_at, updated_at
)
SELECT
    CONCAT('DT', DATE_FORMAT(NOW(), '%Y%m'), LPAD((@next_dispatch_seq := @next_dispatch_seq + 1), 3, '0')),
    wo.work_order_id,
    ps.step_id,
    (SELECT user_id FROM user WHERE username = 'zhao_operator' LIMIT 1),
    (SELECT equipment_id FROM equipment WHERE equipment_code = 'EQ-DISP-01' LIMIT 1),
    wo.planned_quantity,
    0,
    0,
    (SELECT user_id FROM user WHERE username = 'li_manager' LIMIT 1),
    NOW(),
    'ASSIGNED',
    CONCAT('补派-', ps.step_name, '（', wo.work_order_no, '）'),
    NOW(),
    NOW()
FROM work_order wo
JOIN process_step ps ON ps.route_id = wo.route_id AND ps.step_code = 'STEP-00A'
WHERE wo.status IN ('RELEASED', 'DISPATCHED', 'PRODUCING', 'QC_PENDING', 'RUNNING')
  AND NOT EXISTS (
      SELECT 1 FROM dispatch_task dt
      JOIN process_step s ON s.step_id = dt.step_id
      WHERE dt.work_order_id = wo.work_order_id AND s.step_name = '显示屏加工'
  );

INSERT INTO dispatch_task (
    dispatch_no, work_order_id, step_id, operator_id, equipment_id,
    assigned_quantity, accepted_quantity, completed_quantity,
    assigned_by, assigned_at, status, remark, created_at, updated_at
)
SELECT
    CONCAT('DT', DATE_FORMAT(NOW(), '%Y%m'), LPAD((@next_dispatch_seq := @next_dispatch_seq + 1), 3, '0')),
    wo.work_order_id,
    ps.step_id,
    (SELECT user_id FROM user WHERE username = 'li_operator' LIMIT 1),
    (SELECT equipment_id FROM equipment WHERE equipment_code = 'EQ-MB-01' LIMIT 1),
    wo.planned_quantity,
    0,
    0,
    (SELECT user_id FROM user WHERE username = 'li_manager' LIMIT 1),
    NOW(),
    'ASSIGNED',
    CONCAT('补派-', ps.step_name, '（', wo.work_order_no, '）'),
    NOW(),
    NOW()
FROM work_order wo
JOIN process_step ps ON ps.route_id = wo.route_id AND ps.step_code = 'STEP-00B'
WHERE wo.status IN ('RELEASED', 'DISPATCHED', 'PRODUCING', 'QC_PENDING', 'RUNNING')
  AND NOT EXISTS (
      SELECT 1 FROM dispatch_task dt
      JOIN process_step s ON s.step_id = dt.step_id
      WHERE dt.work_order_id = wo.work_order_id AND s.step_name = '主板装配'
  );

-- ========== 6. 重算工单成品瓶颈完成量 ==========
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
            WHEN ps.step_name LIKE '%显示屏%' THEN 1
            WHEN ps.step_name LIKE '%主板%' THEN 2
            WHEN ps.step_name LIKE '%贴附%' THEN 3
            WHEN ps.step_name LIKE '%组装%' THEN 4
            ELSE 99 END
    ) t
), 0),
updated_at = NOW();
