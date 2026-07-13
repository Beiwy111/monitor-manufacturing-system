-- 生产工序与车间结构调整：4 个有序生产车间 + 质检/包装工序保留但归属质检/售后
-- 执行顺序：先备份，再运行本脚本，最后重启后端（DispatchProgressRepairTask 会重算工单完成量）

-- 1. 新增生产设备
INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-DISP-01', '1号显示屏加工线', '显示屏线', '生产一部', 'F线-01', '显示科技', 'XS-DP100', '2024-05-10', 'RUNNING', '2026-06-18 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-DISP-01');

INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-MB-01', '1号主板装配线', '主板线', '生产一部', 'G线-01', '华南电子', 'HN-MB80', '2024-05-10', 'RUNNING', '2026-06-18 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-MB-01');

-- 2. 工艺路线 1：插入显示屏加工、主板装配（若不存在）
INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 1, 5, 'STEP-00A', '显示屏加工', 0.35, '显示屏线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 1 AND step_code = 'STEP-00A');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 1, 8, 'STEP-00B', '主板装配', 0.45, '主板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 1 AND step_code = 'STEP-00B');

-- 将背光组装统一为整机组装（组装车间）
UPDATE process_step SET step_name = '整机组装', standard_equipment_type = '组装线'
WHERE route_id = 1 AND step_code = 'STEP-02' AND step_name = '背光组装';

-- 老化/包装标记为质检环节（仍保留在工艺路线供质检模块使用）
UPDATE process_step SET quality_required = 1
WHERE route_id = 1 AND step_name IN ('整机老化测试', '外观检验包装');

-- 3. 工艺路线 2：同样补齐前两道生产工序
INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 2, 5, 'STEP-00A', '显示屏加工', 0.40, '显示屏线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 2 AND step_code = 'STEP-00A');

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status)
SELECT 2, 8, 'STEP-00B', '主板装配', 0.50, '主板线', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM process_step WHERE route_id = 2 AND step_code = 'STEP-00B');

UPDATE process_step SET step_name = '整机组装', standard_equipment_type = '组装线'
WHERE route_id = 2 AND step_code = 'STEP-02' AND step_name = '背光组装';

-- 4. 工单完成量按瓶颈重算（各生产工序完成量取最小值）
UPDATE work_order wo
SET completed_quantity = COALESCE((
    SELECT MIN(dt.completed_quantity)
    FROM dispatch_task dt
    JOIN process_step ps ON ps.step_id = dt.step_id
    WHERE dt.work_order_id = wo.work_order_id
      AND ps.step_name NOT REGEXP '老化|调校|包装|质检|检验|终检|发货|售后'
      AND (
        ps.standard_equipment_type IN ('显示屏线', '主板线', '贴附机', '组装线')
        OR ps.step_name REGEXP '显示屏|主板|贴附|组装'
      )
), 0);
