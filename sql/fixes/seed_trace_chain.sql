-- ══════════════════════════════════════════════════════════════════
-- 售后反向追溯链补种子（幂等，可重复执行）
-- 目的：让固定八环链路（客户反馈→发货→入库→成品质检→生产→领料→物料质检→供应商）
--       在当前库真实外键体系（工单27/28、订单36/37、质检17/18、成品物料9）下全环有数据。
-- 责任人选用：周仓管(37)=发货/出入库，刘采购(36)=采购员，陈质检(35)=物料质检员，
--             王质检(5)=成品质检（已有），操作员 34/63/64/65（已有报工）。
-- ══════════════════════════════════════════════════════════════════

-- 1) 案例补成品质检关联（订单36 → QI17/工单27；订单37 → QI18/工单28）
UPDATE after_sales_case SET quality_inspection_id = 17
 WHERE order_id = 36 AND quality_inspection_id IS NULL;
UPDATE after_sales_case SET quality_inspection_id = 18
 WHERE order_id = 37 AND quality_inspection_id IS NULL;

-- 2) 发货单（delivery_no 唯一键幂等）+ 回填案例 delivery_id
INSERT IGNORE INTO delivery_order
  (delivery_no, order_id, work_order_id, customer_name, material_id, batch_no,
   delivery_quantity, delivery_date, logistics_company, logistics_no,
   receiver_name, receiver_phone, receiver_address, delivery_status, shipped_by, remark) VALUES
  ('DO202607001', 36, 27, '北京星辰电竞俱乐部', 9, 'BATCH-WO202607001-5059',
   50, '2026-07-08', '顺丰速运', 'SF2026070800123',
   '王总', '13900001002', '北京市朝阳区电竞产业园A座', 'SHIPPED', 37, '首批整批发货'),
  ('DO202607002', 37, 28, '上海视觉设计工作室', 9, 'BATCH-WO202607002-610',
   30, '2026-07-10', '德邦物流', 'DB2026071000456',
   '陈设计', '13900001003', '上海市徐汇区创意园B栋', 'SHIPPED', 37, '首批整批发货');

UPDATE after_sales_case a
  JOIN delivery_order d ON d.order_id = a.order_id
   SET a.delivery_id = d.delivery_id
 WHERE a.delivery_id IS NULL;

-- 3) 库存事务（transaction_no 唯一键幂等）：
--    驱动IC补一条采购入库；两张工单各领三种原料（批次对齐 PURCHASE_IN，供应商环才能接上）；成品入库
INSERT IGNORE INTO inventory_transaction
  (transaction_no, material_id, transaction_type, quantity, warehouse_code, location_code,
   batch_no, related_purchase_order_id, related_work_order_id, handled_by, handled_at, remark) VALUES
  ('IT202606003', 3, 'PURCHASE_IN', 800, 'RM-WH', 'RM-A03', 'BATCH-IC-202606',  3, NULL, 37, '2026-06-20 10:30:00', '驱动IC采购入库'),
  ('IT202607001', 1, 'MATERIAL_OUT', 55, 'RM-WH', 'RM-A01', 'BATCH-LCD-202606', NULL, 27, 37, '2026-07-01 08:30:00', 'WO202607001 生产领料'),
  ('IT202607002', 2, 'MATERIAL_OUT', 55, 'RM-WH', 'RM-A02', 'BATCH-BL-202606',  NULL, 27, 37, '2026-07-01 08:35:00', 'WO202607001 生产领料'),
  ('IT202607003', 3, 'MATERIAL_OUT', 55, 'RM-WH', 'RM-A03', 'BATCH-IC-202606',  NULL, 27, 37, '2026-07-01 08:40:00', 'WO202607001 生产领料'),
  ('IT202607004', 1, 'MATERIAL_OUT', 32, 'RM-WH', 'RM-A01', 'BATCH-LCD-202606', NULL, 28, 37, '2026-07-02 08:30:00', 'WO202607002 生产领料'),
  ('IT202607005', 2, 'MATERIAL_OUT', 32, 'RM-WH', 'RM-A02', 'BATCH-BL-202606',  NULL, 28, 37, '2026-07-02 08:35:00', 'WO202607002 生产领料'),
  ('IT202607006', 3, 'MATERIAL_OUT', 32, 'RM-WH', 'RM-A03', 'BATCH-IC-202606',  NULL, 28, 37, '2026-07-02 08:40:00', 'WO202607002 生产领料'),
  ('IT202607007', 9, 'PRODUCT_IN',   50, 'FG-WH', 'FG-A01', 'BATCH-WO202607001-5059', NULL, 27, 37, '2026-07-06 16:00:00', 'WO202607001 成品入库'),
  ('IT202607008', 9, 'PRODUCT_IN',   30, 'FG-WH', 'FG-A01', 'BATCH-WO202607002-610',  NULL, 28, 37, '2026-07-07 16:00:00', 'WO202607002 成品入库');

-- 4) 来料检验（inspection_no 唯一键幂等；物料质检员：陈质检。IC 批次留 1 片让步接收，做演示钩子）
INSERT IGNORE INTO quality_inspection
  (inspection_no, work_order_id, material_id, batch_no, inspection_category, inspection_type,
   sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspection_status,
   inspector_id, inspected_at, remark) VALUES
  ('QI202606I01', NULL, 1, 'BATCH-LCD-202606', 'RAW_MATERIAL', 'INCOMING', 60, 60, 0, 'QUALIFIED', 'PASSED', 35, '2026-06-19 14:00:00', 'LCD面板来料抽检合格'),
  ('QI202606I02', NULL, 2, 'BATCH-BL-202606',  'RAW_MATERIAL', 'INCOMING', 60, 60, 0, 'QUALIFIED', 'PASSED', 35, '2026-06-19 15:00:00', '背光模组来料抽检合格'),
  ('QI202606I03', NULL, 3, 'BATCH-IC-202606',  'RAW_MATERIAL', 'INCOMING', 80, 79, 1, 'QUALIFIED', 'PASSED', 35, '2026-06-20 14:00:00', '驱动IC来料抽检1片时序偏移，让步接收');

-- 5) 报工补设备（仅补 NULL，幂等）：面板贴附/显示屏加工/整机组装/主板装配 各对应真实机台
UPDATE work_report SET equipment_id = CASE report_id
    WHEN 27 THEN 1    -- WO202607001 面板贴附   → EQ-001 1号自动贴附机
    WHEN 28 THEN 17   -- WO202607001 显示屏加工 → EQ-DISP-01 1号显示屏加工线
    WHEN 29 THEN 3    -- WO202607001 整机组装   → EQ-003 1号组装流水线
    WHEN 30 THEN 18   -- WO202607001 主板装配   → EQ-MB-01 1号主板装配线
    WHEN 31 THEN 2    -- WO202607002 面板贴附   → EQ-002 2号自动贴附机
    WHEN 32 THEN 12   -- WO202607002 显示屏加工 → EQ-DISP-02 2号显示屏加工线
    WHEN 33 THEN 7    -- WO202607002 整机组装   → EQ-007 2号组装流水线
    WHEN 34 THEN 14   -- WO202607002 主板装配   → EQ-MB-02 2号主板装配线
    ELSE equipment_id END
 WHERE report_id BETWEEN 27 AND 34 AND equipment_id IS NULL;

-- 6) 采购单责任人矫正为真实采购员（刘采购），补联系人便于链上展示
UPDATE purchase_order SET purchaser_id = 36 WHERE purchase_order_id IN (1, 2, 3);
UPDATE purchase_order SET supplier_contact = '孙经理', supplier_phone = '13700002001'
 WHERE purchase_order_id = 1 AND (supplier_contact IS NULL OR supplier_contact = '');
UPDATE purchase_order SET supplier_contact = '钱经理', supplier_phone = '13700002002'
 WHERE purchase_order_id = 2 AND (supplier_contact IS NULL OR supplier_contact = '');
UPDATE purchase_order SET supplier_contact = '吴经理', supplier_phone = '13700002003'
 WHERE purchase_order_id = 3 AND (supplier_contact IS NULL OR supplier_contact = '');
