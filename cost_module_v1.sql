-- ================================================================
-- 成本结算模块升级 v1：补质量成本溯源字段 + 演示数据
-- 目标库：display_manufacturing
-- ================================================================

USE `display_manufacturing`;

-- 1. cost_settlement 补 source_type / source_id / cost_reason 字段
SET @c1 := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='display_manufacturing' AND TABLE_NAME='cost_settlement'
    AND COLUMN_NAME='source_type');
SET @s1 := IF(@c1=0,
  'ALTER TABLE `cost_settlement` ADD COLUMN `source_type` varchar(40) DEFAULT NULL COMMENT ''成本来源类型'' AFTER `order_id`',
  'SELECT 1');
PREPARE p FROM @s1; EXECUTE p; DEALLOCATE PREPARE p;

SET @c2 := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='display_manufacturing' AND TABLE_NAME='cost_settlement'
    AND COLUMN_NAME='source_id');
SET @s2 := IF(@c2=0,
  'ALTER TABLE `cost_settlement` ADD COLUMN `source_id` varchar(100) DEFAULT NULL COMMENT ''来源业务编号'' AFTER `source_type`',
  'SELECT 1');
PREPARE p FROM @s2; EXECUTE p; DEALLOCATE PREPARE p;

SET @c3 := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='display_manufacturing' AND TABLE_NAME='cost_settlement'
    AND COLUMN_NAME='cost_reason');
SET @s3 := IF(@c3=0,
  'ALTER TABLE `cost_settlement` ADD COLUMN `cost_reason` varchar(500) DEFAULT NULL COMMENT ''质量成本产生原因'' AFTER `source_id`',
  'SELECT 1');
PREPARE p FROM @s3; EXECUTE p; DEALLOCATE PREPARE p;

-- 2. 演示数据（5条，覆盖全部成本来源类型）

-- 2a 不良品处理成本（关联 NC202607201 色准超标）
INSERT INTO `cost_settlement`
  (settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
   material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
   settlement_period, settlement_status, remark, created_at, updated_at)
SELECT 'CS202607001', 2, 2, 'NONCONFORMING_PRODUCT', 'NC202607201',
  '4K显示器色准ΔE=3.8超标，1台需色彩校准返工，含材料损耗及返工人工',
  0, 320.00, 0, 480.00, 0, 800.00,
  '2026-07', 'CONFIRMED', '已完成返工并关闭不良品记录',
  NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `cost_settlement` WHERE settlement_no='CS202607001');

-- 2b 售后维修成本（关联 AS202607001 色彩问题）
INSERT INTO `cost_settlement`
  (settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
   material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
   settlement_period, settlement_status, remark, created_at, updated_at)
SELECT 'CS202607002', NULL, 2, 'AFTER_SALES', 'AS202607001',
  '北京星辰科技4K显示器色彩投诉，售后上门校准+邮寄配件',
  150.00, 200.00, 0, 0, 80.00, 430.00,
  '2026-07', 'DRAFT', '待财务确认',
  NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `cost_settlement` WHERE settlement_no='CS202607002');

-- 2c 设备维修成本
INSERT INTO `cost_settlement`
  (settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
   material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
   settlement_period, settlement_status, remark, created_at, updated_at)
SELECT 'CS202607003', 1, NULL, 'EQUIPMENT_MAINTENANCE', 'EQ-LCD-001',
  'LCD面板贴合机气压阀故障，停机4小时，维修更换气压阀及损耗配件',
  380.00, 160.00, 1200.00, 0, 0, 1740.00,
  '2026-07', 'CONFIRMED', '维修完成，已恢复生产',
  DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `cost_settlement` WHERE settlement_no='CS202607003');

-- 2d 采购退货损失
INSERT INTO `cost_settlement`
  (settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
   material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
   settlement_period, settlement_status, remark, created_at, updated_at)
SELECT 'CS202607004', NULL, NULL, 'PURCHASE_RETURN', 'PO-2026-0078',
  '背光模组供应商发货批次BL-202606不合格率超15%，退货50pcs，含运费及检测费',
  0, 0, 0, 2500.00, 420.00, 2920.00,
  '2026-07', 'DRAFT', '退货已发出，待供应商确认赔偿',
  DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `cost_settlement` WHERE settlement_no='CS202607004');

-- 2e 工单正常成本（已导出归档）
INSERT INTO `cost_settlement`
  (settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
   material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
   settlement_period, settlement_status, confirmed_at, exported_at, remark, created_at, updated_at)
SELECT 'CS202606001', 1, 1, 'WORK_ORDER', 'WO-2026-001',
  '6月工单DM-27-LCD-FHD正常生产成本结算',
  12800.00, 3200.00, 1600.00, 320.00, 480.00, 18400.00,
  '2026-06', 'EXPORTED',
  DATE_SUB(NOW(), INTERVAL 7 DAY),
  DATE_SUB(NOW(), INTERVAL 5 DAY),
  '已导出至财务系统',
  DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `cost_settlement` WHERE settlement_no='CS202606001');
