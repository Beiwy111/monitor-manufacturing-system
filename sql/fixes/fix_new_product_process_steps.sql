-- 为 PRD-004~008 补齐八道生产工序（从模板产品复制，可重复执行）
-- mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/fixes/fix_new_product_process_steps.sql
SET NAMES utf8mb4;

-- 1. 确保工艺路线存在
INSERT INTO process_route (material_id, route_code, route_name, version_no, status, created_by, created_at, updated_at)
SELECT m.material_id, CONCAT('RT-', m.material_code, '-V1'), CONCAT(m.material_name, '工艺路线'), 'V1.0', 1, 2, NOW(), NOW()
FROM material m
WHERE m.material_code IN ('PRD-004', 'PRD-005', 'PRD-006', 'PRD-007', 'PRD-008')
  AND NOT EXISTS (SELECT 1 FROM process_route pr WHERE pr.material_id = m.material_id);

-- 2. 按模板复制工序
-- PRD-004 ← PRD-001（办公系，10 道）
INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status, created_at, updated_at)
SELECT tr.route_id, ps.step_no,
       CONCAT('S-', tm.material_code, '-', LPAD(CAST(ps.step_no AS CHAR), 3, '0')),
       ps.step_name, ps.standard_work_hours, ps.standard_equipment_type, ps.quality_required, ps.status, NOW(), NOW()
FROM process_step ps
JOIN process_route sr ON sr.route_id = ps.route_id
JOIN material sm ON sm.material_id = sr.material_id AND sm.material_code = 'PRD-001'
JOIN material tm ON tm.material_code = 'PRD-004'
JOIN process_route tr ON tr.material_id = tm.material_id
WHERE NOT EXISTS (SELECT 1 FROM process_step xs WHERE xs.route_id = tr.route_id AND xs.step_no = ps.step_no);

-- PRD-005、PRD-006 ← PRD-002（电竞系，11 道）
INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status, created_at, updated_at)
SELECT tr.route_id, ps.step_no,
       CONCAT('S-', tm.material_code, '-', LPAD(CAST(ps.step_no AS CHAR), 3, '0')),
       ps.step_name, ps.standard_work_hours, ps.standard_equipment_type, ps.quality_required, ps.status, NOW(), NOW()
FROM process_step ps
JOIN process_route sr ON sr.route_id = ps.route_id
JOIN material sm ON sm.material_id = sr.material_id AND sm.material_code = 'PRD-002'
JOIN material tm ON tm.material_code IN ('PRD-005', 'PRD-006')
JOIN process_route tr ON tr.material_id = tm.material_id
WHERE NOT EXISTS (SELECT 1 FROM process_step xs WHERE xs.route_id = tr.route_id AND xs.step_no = ps.step_no);

-- PRD-007、PRD-008 ← PRD-003（高端系，10 道）
INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status, created_at, updated_at)
SELECT tr.route_id, ps.step_no,
       CONCAT('S-', tm.material_code, '-', LPAD(CAST(ps.step_no AS CHAR), 3, '0')),
       ps.step_name, ps.standard_work_hours, ps.standard_equipment_type, ps.quality_required, ps.status, NOW(), NOW()
FROM process_step ps
JOIN process_route sr ON sr.route_id = ps.route_id
JOIN material sm ON sm.material_id = sr.material_id AND sm.material_code = 'PRD-003'
JOIN material tm ON tm.material_code IN ('PRD-007', 'PRD-008')
JOIN process_route tr ON tr.material_id = tm.material_id
WHERE NOT EXISTS (SELECT 1 FROM process_step xs WHERE xs.route_id = tr.route_id AND xs.step_no = ps.step_no);
