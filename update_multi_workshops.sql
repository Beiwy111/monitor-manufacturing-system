-- 四道工序 × 每道 2~3 车间：设备归属与工艺对齐
USE display_manufacturing;
SET NAMES utf8mb4;

-- 显示屏加工（3 车间）
UPDATE equipment SET workshop = '显示屏加工一车间', equipment_type = '显示屏线' WHERE equipment_code = 'EQ-DISP-01';
INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-DISP-02', '2号显示屏加工线', '显示屏线', '显示屏加工二车间', 'F线-02', '显示科技', 'XS-DP100', '2024-06-01', 'RUNNING', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-DISP-02');
INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-DISP-03', '3号显示屏加工线', '显示屏线', '显示屏加工三车间', 'F线-03', '显示科技', 'XS-DP100', '2024-06-01', 'IDLE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-DISP-03');

-- 主板装配（3 车间）
UPDATE equipment SET workshop = '主板装配一车间', equipment_type = '主板线' WHERE equipment_code = 'EQ-MB-01';
INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-MB-02', '2号主板装配线', '主板线', '主板装配二车间', 'G线-02', '华南电子', 'HN-MB80', '2024-06-01', 'RUNNING', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-MB-02');
INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-MB-03', '3号主板装配线', '主板线', '主板装配三车间', 'G线-03', '华南电子', 'HN-MB80', '2024-06-01', 'IDLE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-MB-03');

-- 贴附（2 车间）
UPDATE equipment SET workshop = '贴附一车间' WHERE equipment_code = 'EQ-001';
UPDATE equipment SET workshop = '贴附二车间' WHERE equipment_code = 'EQ-002';
UPDATE equipment SET workshop = '贴附二车间' WHERE equipment_code = 'EQ-010';

-- 组装（3 车间）
UPDATE equipment SET workshop = '组装一车间' WHERE equipment_code = 'EQ-003';
INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-007', '2号组装流水线', '组装线', '组装二车间', 'B线-02', '华南机械', 'HN-ZX100', '2024-08-01', 'RUNNING', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-007');
INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at)
SELECT 'EQ-012', '3号组装流水线', '组装线', '组装三车间', 'B线-03', '华南机械', 'HN-ZX100', '2025-01-01', 'IDLE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE equipment_code = 'EQ-012');

-- 质检/售后设备保留在生产二部（不参与生产排产）
UPDATE equipment SET workshop = '生产二部' WHERE equipment_type IN ('老化架', '调校台', '包装线');

-- 工单完成量：按四道工序汇总后取瓶颈
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
), 0);
