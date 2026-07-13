-- 物料来料检示例数据（采购原材料质检）
INSERT IGNORE INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status, remark, created_at, updated_at)
SELECT 'QI202607301', NULL, 1, 'BATCH-LCD-202607', 'INCOMING', 'RAW_MATERIAL',
       10, 0, 0, 'PENDING', 'PENDING', '来料批次 200 件，抽检 10 件', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607301');

INSERT IGNORE INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status, remark, created_at, updated_at)
SELECT 'QI202607302', NULL, 2, 'BATCH-BL-202607', 'INCOMING', 'RAW_MATERIAL',
       8, 0, 0, 'PENDING', 'PENDING', '来料批次 100 件，抽检 8 件', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607302');

INSERT IGNORE INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_type, inspection_category,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status, inspected_at, remark, created_at, updated_at)
SELECT 'QI202607303', NULL, 5, 'BATCH-PCB-202606', 'INCOMING', 'RAW_MATERIAL',
       5, 5, 0, 'QUALIFIED', 'PASSED', NOW(), '来料批次 50 件，抽检 5 件，全部合格', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM quality_inspection WHERE inspection_no = 'QI202607303');
