-- 扩展成品 catalog 至 8 款（可重复执行）
-- mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/migrations/seed_eight_finished_products.sql

USE display_manufacturing;
SET NAMES utf8mb4;

-- 1. 扩展 material 展示字段
SET @col_img := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'image_url');
SET @sql_img := IF(@col_img = 0,
  'ALTER TABLE material ADD COLUMN image_url varchar(500) DEFAULT NULL COMMENT ''展示图路径'' AFTER specification',
  'SELECT 1');
PREPARE s1 FROM @sql_img; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @col_sum := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'product_summary');
SET @sql_sum := IF(@col_sum = 0,
  'ALTER TABLE material ADD COLUMN product_summary varchar(500) DEFAULT NULL COMMENT ''产品简介'' AFTER image_url',
  'SELECT 1');
PREPARE s2 FROM @sql_sum; EXECUTE s2; DEALLOCATE PREPARE s2;

SET @col_ports := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'ports');
SET @sql_ports := IF(@col_ports = 0,
  'ALTER TABLE material ADD COLUMN ports varchar(200) DEFAULT NULL COMMENT ''接口配置'' AFTER product_summary',
  'SELECT 1');
PREPARE s3 FROM @sql_ports; EXECUTE s3; DEALLOCATE PREPARE s3;

SET @col_sort := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'sort_order');
SET @sql_sort := IF(@col_sort = 0,
  'ALTER TABLE material ADD COLUMN sort_order int NOT NULL DEFAULT 0 COMMENT ''展示排序'' AFTER ports',
  'SELECT 1');
PREPARE s4 FROM @sql_sort; EXECUTE s4; DEALLOCATE PREPARE s4;

-- 2. 更新既有 3 款
UPDATE material SET
  image_url = '/materials/products/prd-001.jpg',
  product_summary = '15.6 英寸 IPS 商用显示器，1080P 全高清，适合办公与教育场景。',
  ports = 'VGA ×1 · HDMI ×1',
  sort_order = 1
WHERE material_code = 'PRD-001';

UPDATE material SET
  image_url = '/materials/products/prd-002.jpg',
  product_summary = '23.8 英寸 2K 电竞显示器，144Hz 高刷，低延迟游戏体验。',
  ports = 'HDMI ×2 · DP ×1 · USB-C ×1',
  sort_order = 2
WHERE material_code = 'PRD-002';

UPDATE material SET
  image_url = '/materials/products/prd-003.jpg',
  product_summary = '27 英寸 4K HDR 显示器，高色域专业视觉体验。',
  ports = 'HDMI ×2 · DP ×1 · USB-C ×1',
  sort_order = 3
WHERE material_code = 'PRD-003';

-- 3. 新增 5 款成品
INSERT INTO material (material_code, material_name, material_type, specification, image_url, product_summary, ports, unit, safety_stock, standard_cost, sort_order, status, created_at, updated_at)
SELECT 'PRD-004', '21.5寸办公显示器', 'FINISHED', '1920x1080 IPS 办公款', '/materials/products/prd-004.jpg',
  '21.5 英寸 IPS 办公显示器，窄边框设计，日常办公优选。', 'HDMI ×1 · DP ×1', '台', 40, 720.0000, 4, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'PRD-004');

INSERT INTO material (material_code, material_name, material_type, specification, image_url, product_summary, ports, unit, safety_stock, standard_cost, sort_order, status, created_at, updated_at)
SELECT 'PRD-005', '24寸曲面显示器', 'FINISHED', '1920x1080 165Hz 曲面', '/materials/products/prd-005.jpg',
  '24 英寸曲面显示器，165Hz 刷新率，沉浸式娱乐与办公。', 'HDMI ×2 · DP ×1', '台', 35, 980.0000, 5, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'PRD-005');

INSERT INTO material (material_code, material_name, material_type, specification, image_url, product_summary, ports, unit, safety_stock, standard_cost, sort_order, status, created_at, updated_at)
SELECT 'PRD-006', '32寸电竞显示器', 'FINISHED', '2560x1440 165Hz 电竞', '/materials/products/prd-006.jpg',
  '32 英寸 2K 大屏电竞显示器，165Hz 高刷，竞技大屏首选。', 'HDMI ×2 · DP ×1 · USB-C ×1', '台', 25, 1680.0000, 6, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'PRD-006');

INSERT INTO material (material_code, material_name, material_type, specification, image_url, product_summary, ports, unit, safety_stock, standard_cost, sort_order, status, created_at, updated_at)
SELECT 'PRD-007', '27寸OLED显示器', 'FINISHED', '3840x2160 OLED HDR', '/materials/products/prd-007.jpg',
  '27 英寸 OLED 4K 显示器，纯黑对比度，专业创作与影音。', 'HDMI ×2 · DP ×1 · USB-C ×1', '台', 20, 2580.0000, 7, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'PRD-007');

INSERT INTO material (material_code, material_name, material_type, specification, image_url, product_summary, ports, unit, safety_stock, standard_cost, sort_order, status, created_at, updated_at)
SELECT 'PRD-008', '34寸超宽显示器', 'FINISHED', '3440x1440 100Hz 超宽', '/materials/products/prd-008.jpg',
  '34 英寸 21:9 超宽显示器，100Hz 刷新，多任务与沉浸办公。', 'HDMI ×2 · DP ×1 · USB-C ×1', '台', 15, 2280.0000, 8, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'PRD-008');

-- 4. 复制 BOM（办公系←PRD-001，电竞系←PRD-002，高端系←PRD-003）
INSERT INTO bom (parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, status, remark, created_at, updated_at)
SELECT tgt.material_id, b.child_material_id, b.quantity, b.loss_rate, b.version_no, b.effective_date, b.status, b.remark, NOW(), NOW()
FROM bom b
JOIN material src ON src.material_code = 'PRD-001'
JOIN material tgt ON tgt.material_code = 'PRD-004'
WHERE b.parent_material_id = src.material_id
  AND NOT EXISTS (SELECT 1 FROM bom x WHERE x.parent_material_id = tgt.material_id AND x.child_material_id = b.child_material_id);

INSERT INTO bom (parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, status, remark, created_at, updated_at)
SELECT tgt.material_id, b.child_material_id, b.quantity, b.loss_rate, b.version_no, b.effective_date, b.status, b.remark, NOW(), NOW()
FROM bom b
JOIN material src ON src.material_code = 'PRD-002'
JOIN material tgt ON tgt.material_code IN ('PRD-005', 'PRD-006')
WHERE b.parent_material_id = src.material_id
  AND NOT EXISTS (SELECT 1 FROM bom x WHERE x.parent_material_id = tgt.material_id AND x.child_material_id = b.child_material_id);

INSERT INTO bom (parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, status, remark, created_at, updated_at)
SELECT tgt.material_id, b.child_material_id, b.quantity, b.loss_rate, b.version_no, b.effective_date, b.status, b.remark, NOW(), NOW()
FROM bom b
JOIN material src ON src.material_code = 'PRD-003'
JOIN material tgt ON tgt.material_code IN ('PRD-007', 'PRD-008')
WHERE b.parent_material_id = src.material_id
  AND NOT EXISTS (SELECT 1 FROM bom x WHERE x.parent_material_id = tgt.material_id AND x.child_material_id = b.child_material_id);

-- 5. 工艺路线
INSERT INTO process_route (material_id, route_code, route_name, version_no, status, created_by, created_at, updated_at)
SELECT m.material_id, CONCAT('RT-', m.material_code, '-V1'), CONCAT(m.material_name, '工艺路线'), 'V1.0', 1, 2, NOW(), NOW()
FROM material m
WHERE m.material_code IN ('PRD-004','PRD-005','PRD-006','PRD-007','PRD-008')
  AND NOT EXISTS (SELECT 1 FROM process_route pr WHERE pr.material_id = m.material_id);

-- 6. 复制工序（按模板产品）
INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status, created_at, updated_at)
SELECT tr.route_id, ps.step_no, CONCAT('S-', tm.material_code, '-', LPAD(ps.step_no, 2, '0')), ps.step_name,
       ps.standard_work_hours, ps.standard_equipment_type, ps.quality_required, ps.status, NOW(), NOW()
FROM process_step ps
JOIN process_route sr ON sr.route_id = ps.route_id
JOIN material sm ON sm.material_id = sr.material_id
JOIN material tm ON tm.material_code IN ('PRD-004','PRD-005','PRD-006','PRD-007','PRD-008')
JOIN process_route tr ON tr.material_id = tm.material_id
WHERE ((tm.material_code = 'PRD-004' AND sm.material_code = 'PRD-001')
   OR (tm.material_code IN ('PRD-005','PRD-006') AND sm.material_code = 'PRD-002')
   OR (tm.material_code IN ('PRD-007','PRD-008') AND sm.material_code = 'PRD-003'))
  AND NOT EXISTS (SELECT 1 FROM process_step xs WHERE xs.route_id = tr.route_id AND xs.step_no = ps.step_no);

-- 7. 成品库存占位
INSERT INTO inventory (material_id, warehouse_code, warehouse_name, location_code, batch_no, quantity_on_hand, quantity_available, quantity_locked, inventory_status, last_transaction_at, created_at, updated_at)
SELECT m.material_id, 'WH-02', '成品仓', CONCAT('B-', LPAD(m.sort_order, 2, '0'), '-01'), CONCAT('BATCH-', m.material_code), 0, 0, 0, 'NORMAL', NOW(), NOW(), NOW()
FROM material m
WHERE m.material_code IN ('PRD-004','PRD-005','PRD-006','PRD-007','PRD-008')
  AND NOT EXISTS (SELECT 1 FROM inventory i WHERE i.material_id = m.material_id AND i.warehouse_code = 'WH-02');
