-- 质检员工作台演示数据（关联真实工单/物料，可重复执行）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/fixes/seed_quality_dashboard.sql

USE display_manufacturing;
SET NAMES utf8mb4;

-- ── 1. 补充质检单（半成品 / 成品 / 来料） ─────────────────────────────

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607011', wo.work_order_id, wo.material_id, 'BATCH-WO202607001-PANEL', 'PROCESS', 'SEMI_FINISHED',
  20, 19, 1, 'UNQUALIFIED', 'FAILED', 5, '2026-07-10 14:20:00', '面板贴附后抽检，1台坏点超标', NOW(), NOW()
FROM work_order wo WHERE wo.work_order_no = 'WO202607001'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607011');

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607012', wo.work_order_id, wo.material_id, 'BATCH-WO202607001-BL', 'PROCESS', 'SEMI_FINISHED',
  15, 15, 0, 'QUALIFIED', 'PASSED', 5, '2026-07-10 16:45:00', '背光模组亮度均匀性合格', NOW(), NOW()
FROM work_order wo WHERE wo.work_order_no = 'WO202607001'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607012');

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607013', wo.work_order_id, wo.material_id, 'BATCH-WO202607001-6536', 'FINAL', 'FINISHED_PRODUCT',
  10, 8, 2, 'UNQUALIFIED', 'RECHECK_REQUIRED', 5, '2026-07-11 10:30:00', '终检发现色准与漏光问题，安排复检', NOW(), NOW()
FROM work_order wo WHERE wo.work_order_no = 'WO202607001'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607013');

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607021', wo.work_order_id, wo.material_id, 'BATCH-WO202607002-3201', 'FINAL', 'FINISHED_PRODUCT',
  10, 7, 3, 'UNQUALIFIED', 'FAILED', 5, '2026-07-11 15:08:00', '电竞批次终检：接口与外观不良', NOW(), NOW()
FROM work_order wo WHERE wo.work_order_no = 'WO202607002'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607021');

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607022', wo.work_order_id, wo.material_id, 'BATCH-WO202607002-3201', 'RECHECK', 'FINISHED_PRODUCT',
  3, 2, 1, 'UNQUALIFIED', 'FAILED', 5, '2026-07-12 08:40:00', '复检后仍有1台亮点未消除', NOW(), NOW()
FROM work_order wo WHERE wo.work_order_no = 'WO202607002'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607022');

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607301', NULL, 1, 'BATCH-LCD-202607', 'INCOMING', 'RAW_MATERIAL',
  10, 0, 0, 'PENDING', 'PENDING', 5, NOW(), 'LCD面板来料 200件，待抽检', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607301');

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607302', NULL, 2, 'BATCH-BL-202607', 'INCOMING', 'RAW_MATERIAL',
  8, 7, 1, 'UNQUALIFIED', 'FAILED', 5, '2026-07-09 11:20:00', '背光模组来料抽检1台漏光', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607302');

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607303', NULL, 5, 'BATCH-PCB-202606', 'INCOMING', 'RAW_MATERIAL',
  5, 5, 0, 'QUALIFIED', 'PASSED', 5, '2026-07-08 09:15:00', 'PCB主板来料全检合格', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607303');

-- ── 2. 不良品台账（覆盖帕累托 / 处置闭环各状态） ───────────────────────

INSERT INTO nonconforming_product
  (nonconforming_no, inspection_id, work_order_id, material_id, batch_no,
   defect_type, defect_description, quantity, severity, handle_method, handle_status,
   registered_by, registered_at, handled_by, handled_at, remark, created_at, updated_at)
SELECT 'NC202607101', qi.inspection_id, qi.work_order_id, qi.material_id, qi.batch_no,
  '亮点不良', '屏幕右上角检测到1个持续亮点', 1, 'MINOR', 'REWORK', 'COMPLETED',
  5, '2026-07-10 14:35:00', 5, '2026-07-10 18:00:00', '返工擦拭后复检合格', NOW(), NOW()
FROM quality_inspection qi WHERE qi.inspection_no = 'QI202607011'
  AND NOT EXISTS (SELECT 1 FROM nonconforming_product WHERE nonconforming_no = 'NC202607101');

INSERT INTO nonconforming_product
  (nonconforming_no, inspection_id, work_order_id, material_id, batch_no,
   defect_type, defect_description, quantity, severity, handle_method, handle_status,
   registered_by, registered_at, remark, created_at, updated_at)
SELECT 'NC202607102', qi.inspection_id, qi.work_order_id, qi.material_id, qi.batch_no,
  '色准超标', '色准ΔE=3.2超出≤2标准，需色彩校准', 1, 'MAJOR', 'REWORK', 'PROCESSING',
  5, '2026-07-11 10:45:00', '调校中，预计今日完成', NOW(), NOW()
FROM quality_inspection qi WHERE qi.inspection_no = 'QI202607013'
  AND NOT EXISTS (SELECT 1 FROM nonconforming_product WHERE nonconforming_no = 'NC202607102');

INSERT INTO nonconforming_product
  (nonconforming_no, inspection_id, work_order_id, material_id, batch_no,
   defect_type, defect_description, quantity, severity, handle_method, handle_status,
   registered_by, registered_at, handled_by, handled_at, remark, created_at, updated_at)
SELECT 'NC202607103', qi.inspection_id, qi.work_order_id, qi.material_id, qi.batch_no,
  '漏光', '四角轻微漏光，在可接受范围内', 1, 'GENERAL', 'CONCESSION', 'COMPLETED',
  5, '2026-07-11 11:00:00', 5, '2026-07-11 16:30:00', '客户确认让步接收', NOW(), NOW()
FROM quality_inspection qi WHERE qi.inspection_no = 'QI202607013'
  AND NOT EXISTS (SELECT 1 FROM nonconforming_product WHERE nonconforming_no = 'NC202607103');

INSERT INTO nonconforming_product
  (nonconforming_no, inspection_id, work_order_id, material_id, batch_no,
   defect_type, defect_description, quantity, severity, handle_method, handle_status,
   registered_by, registered_at, remark, created_at, updated_at)
SELECT 'NC202607104', qi.inspection_id, qi.work_order_id, qi.material_id, qi.batch_no,
  '外观划伤', '铝合金边框左侧轻微划伤', 1, 'MINOR', 'PENDING', 'PENDING',
  5, '2026-07-11 15:20:00', '待工艺确认处置方式', NOW(), NOW()
FROM quality_inspection qi WHERE qi.inspection_no = 'QI202607021'
  AND NOT EXISTS (SELECT 1 FROM nonconforming_product WHERE nonconforming_no = 'NC202607104');

INSERT INTO nonconforming_product
  (nonconforming_no, inspection_id, work_order_id, material_id, batch_no,
   defect_type, defect_description, quantity, severity, handle_method, handle_status,
   registered_by, registered_at, handled_by, handled_at, remark, created_at, updated_at)
SELECT 'NC202607105', qi.inspection_id, qi.work_order_id, qi.material_id, qi.batch_no,
  '接口异常', 'HDMI接口无信号输出', 1, 'GENERAL', 'SCRAP', 'COMPLETED',
  5, '2026-07-11 15:30:00', 5, '2026-07-12 09:00:00', '主板故障，整台报废拆解', NOW(), NOW()
FROM quality_inspection qi WHERE qi.inspection_no = 'QI202607021'
  AND NOT EXISTS (SELECT 1 FROM nonconforming_product WHERE nonconforming_no = 'NC202607105');

INSERT INTO nonconforming_product
  (nonconforming_no, inspection_id, work_order_id, material_id, batch_no,
   defect_type, defect_description, quantity, severity, handle_method, handle_status,
   registered_by, registered_at, remark, created_at, updated_at)
SELECT 'NC202607106', qi.inspection_id, qi.work_order_id, qi.material_id, qi.batch_no,
  '亮点不良', '屏幕中央区域2个暗点', 1, 'MINOR', 'REWORK', 'PENDING',
  5, '2026-07-12 08:50:00', '返工排期中', NOW(), NOW()
FROM quality_inspection qi WHERE qi.inspection_no = 'QI202607022'
  AND NOT EXISTS (SELECT 1 FROM nonconforming_product WHERE nonconforming_no = 'NC202607106');

INSERT INTO nonconforming_product
  (nonconforming_no, inspection_id, work_order_id, material_id, batch_no,
   defect_type, defect_description, quantity, severity, handle_method, handle_status,
   registered_by, registered_at, remark, created_at, updated_at)
SELECT 'NC202607107', qi.inspection_id, NULL, qi.material_id, qi.batch_no,
  '漏光', '背光模组抽检发现左下角漏光', 1, 'GENERAL', 'REWORK', 'PROCESSING',
  5, '2026-07-09 11:35:00', '退回供应商返修', NOW(), NOW()
FROM quality_inspection qi WHERE qi.inspection_no = 'QI202607302'
  AND NOT EXISTS (SELECT 1 FROM nonconforming_product WHERE nonconforming_no = 'NC202607107');

-- ── 3. 关键检测项（新质检单） ───────────────────────────────────────────

INSERT INTO quality_inspection_item (inspection_id, item_code, item_name, standard_value, measured_value, unit, result, defect_level, sort_order, created_at, updated_at)
SELECT qi.inspection_id, 'PANEL-02', '坏点检测', '亮点≤3，暗点≤2', '4', '个', 'FAILED', 'MINOR', 2, NOW(), NOW()
FROM quality_inspection qi WHERE qi.inspection_no = 'QI202607011'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection_item ii WHERE ii.inspection_id = qi.inspection_id AND ii.item_code = 'PANEL-02');

INSERT INTO quality_inspection_item (inspection_id, item_code, item_name, standard_value, measured_value, unit, result, defect_level, sort_order, created_at, updated_at)
SELECT qi.inspection_id, 'FP-08', '色准ΔE', '≤2', '3.2', 'ΔE', 'FAILED', 'MAJOR', 8, NOW(), NOW()
FROM quality_inspection qi WHERE qi.inspection_no = 'QI202607013'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection_item ii WHERE ii.inspection_id = qi.inspection_id AND ii.item_code = 'FP-08');

INSERT INTO quality_inspection_item (inspection_id, item_code, item_name, standard_value, measured_value, unit, result, defect_level, sort_order, created_at, updated_at)
SELECT qi.inspection_id, 'FP-10', 'HDMI接口', '正常输出', '无信号', '', 'FAILED', 'GENERAL', 10, NOW(), NOW()
FROM quality_inspection qi WHERE qi.inspection_no = 'QI202607021'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection_item ii WHERE ii.inspection_id = qi.inspection_id AND ii.item_code = 'FP-10');
