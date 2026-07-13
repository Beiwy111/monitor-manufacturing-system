-- 将计划员工序排程从旧四道工序升级为八道生产工序（与 process_step / ProductionWorkshopCatalog 对齐）
USE display_manufacturing;
SET NAMES utf8mb4;

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
