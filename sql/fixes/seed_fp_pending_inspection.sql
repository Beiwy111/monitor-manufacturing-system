-- 成品待检任务演示数据（关联真实工单，可重复执行）
-- 仅插入尚无成品 PASSED 记录的工单，避免被待检列表 suppress 逻辑隐藏
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/fixes/seed_fp_pending_inspection.sql

USE display_manufacturing;
SET NAMES utf8mb4;

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607407', wo.work_order_id, wo.material_id, 'BATCH-WO202607004-1506', 'FINAL', 'FINISHED_PRODUCT',
  8, 0, 0, 'PENDING', 'PENDING', 5, NOW(), '15.6 commercial batch final inspection pending', NOW(), NOW()
FROM work_order wo WHERE wo.work_order_no = 'WO202607004'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607407')
  AND NOT EXISTS (
    SELECT 1 FROM quality_inspection qi
    WHERE qi.work_order_id = wo.work_order_id
      AND qi.inspection_category = 'FINISHED_PRODUCT'
      AND qi.inspection_status = 'PASSED'
  );

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607408', wo.work_order_id, wo.material_id, 'BATCH-WO202607005-1506', 'FINAL', 'FINISHED_PRODUCT',
  6, 0, 0, 'PENDING', 'PENDING', 5, NOW(), '15.6 education batch pending final inspection', NOW(), NOW()
FROM work_order wo WHERE wo.work_order_no = 'WO202607005'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607408')
  AND NOT EXISTS (
    SELECT 1 FROM quality_inspection qi
    WHERE qi.work_order_id = wo.work_order_id
      AND qi.inspection_category = 'FINISHED_PRODUCT'
      AND qi.inspection_status = 'PASSED'
  );

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607409', wo.work_order_id, wo.material_id, 'BATCH-WO202607006-1506', 'FINAL', 'FINISHED_PRODUCT',
  10, 0, 0, 'PENDING', 'PENDING', 5, NOW(), '15.6 replenishment batch aging done pending QC', NOW(), NOW()
FROM work_order wo WHERE wo.work_order_no = 'WO202607006'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607409')
  AND NOT EXISTS (
    SELECT 1 FROM quality_inspection qi
    WHERE qi.work_order_id = wo.work_order_id
      AND qi.inspection_category = 'FINISHED_PRODUCT'
      AND qi.inspection_status = 'PASSED'
  );

INSERT INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607410', wo.work_order_id, wo.material_id, 'BATCH-WO202607003-3201-R1', 'RECHECK', 'FINISHED_PRODUCT',
  3, 0, 0, 'PENDING', 'RECHECK_REQUIRED', 5, NOW(), '23.8 esports batch color shift recheck', NOW(), NOW()
FROM work_order wo WHERE wo.work_order_no = 'WO202607003'
  AND NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607410')
  AND NOT EXISTS (
    SELECT 1 FROM quality_inspection qi
    WHERE qi.work_order_id = wo.work_order_id
      AND qi.inspection_category = 'FINISHED_PRODUCT'
      AND qi.inspection_status = 'PASSED'
  );
