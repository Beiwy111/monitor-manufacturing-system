-- 释放当前被占用的设备与操作员（基于数据库现有记录，不写死 ID）
-- 占用来源：dispatch_task 进行中状态 + work_order 在制状态 + equipment RUNNING

USE display_manufacturing;

START TRANSACTION;

-- 1) 结束所有进行中的派工（人员/设备占用判定依据）
UPDATE dispatch_task
SET `status` = 'COMPLETED',
    accepted_at = COALESCE(accepted_at, NOW()),
    completed_quantity = GREATEST(completed_quantity, assigned_quantity),
    updated_at = NOW()
WHERE `status` IN ('ASSIGNED', 'ACCEPTED', 'PRODUCING', 'RUNNING', 'QC_PENDING');

-- 2) 结束在制工单，避免大屏定时任务再次把设备标为 RUNNING
UPDATE work_order
SET `status` = 'COMPLETED',
    actual_end_time = COALESCE(actual_end_time, NOW()),
    completed_quantity = GREATEST(completed_quantity, planned_quantity),
    qualified_quantity = GREATEST(qualified_quantity, planned_quantity),
    updated_at = NOW()
WHERE `status` IN ('RUNNING', 'RELEASED', 'DISPATCHED', 'PRODUCING', 'QC_PENDING');

-- 3) 设备恢复空闲（保留维保/故障/报废状态）
UPDATE equipment
SET `status` = 'IDLE',
    updated_at = NOW()
WHERE `status` = 'RUNNING';

-- 4) 产线工位解除当前工单绑定
UPDATE prod_line_station
SET work_order_id = NULL,
    work_order_no = NULL,
    station_status = 'IDLE',
    alarm_flag = 0,
    updated_at = NOW()
WHERE work_order_id IS NOT NULL
   OR station_status <> 'IDLE';

COMMIT;

-- 验证
SELECT 'dispatch_active' AS metric, COUNT(*) AS cnt
FROM dispatch_task
WHERE status IN ('ASSIGNED', 'ACCEPTED', 'PRODUCING', 'RUNNING', 'QC_PENDING')
UNION ALL
SELECT 'work_order_active', COUNT(*)
FROM work_order
WHERE status IN ('RUNNING', 'RELEASED', 'DISPATCHED', 'PRODUCING', 'QC_PENDING')
UNION ALL
SELECT 'equipment_running', COUNT(*)
FROM equipment
WHERE status = 'RUNNING'
UNION ALL
SELECT 'station_bound', COUNT(*)
FROM prod_line_station
WHERE work_order_id IS NOT NULL;
