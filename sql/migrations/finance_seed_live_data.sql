-- 财务模块活数据：基于现有订单/工单/交付/售后写入，可重复执行（幂等）
USE display_manufacturing;
SET NAMES utf8mb4;

-- 允许无工单的成本结算（售后/采购退货等）
SET @c := (SELECT IS_NULLABLE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'cost_settlement' AND COLUMN_NAME = 'work_order_id');
SET @s := IF(@c = 'NO',
  'ALTER TABLE cost_settlement MODIFY work_order_id bigint unsigned NULL COMMENT ''工单ID''',
  'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ── 1) 工单成本结算：按工单产量 × 单价 × 成本率自动生成 ─────────────
INSERT INTO cost_settlement
(settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
 material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
 settlement_period, settlement_status, confirmed_by, confirmed_at, exported_at, remark, created_at, updated_at)
SELECT
  CONCAT('CS-', wo.work_order_no),
  wo.work_order_id,
  pp.source_order_id,
  'WORK_ORDER',
  wo.work_order_no,
  CONCAT('工单 ', wo.work_order_no, ' 生产成本核算'),
  ROUND(b.base * 0.56, 2),
  ROUND(b.base * 0.17, 2),
  ROUND(b.base * 0.11, 2),
  ROUND(b.base * 0.09, 2),
  ROUND(b.base * 0.07, 2),
  ROUND(b.base, 2),
  '2026-07',
  CASE wo.work_order_id % 3
    WHEN 0 THEN 'EXPORTED'
    WHEN 1 THEN 'CONFIRMED'
    ELSE 'DRAFT'
  END,
  43,
  CASE WHEN wo.work_order_id % 3 != 2 THEN DATE_SUB(NOW(), INTERVAL (wo.work_order_id % 5) DAY) ELSE NULL END,
  CASE WHEN wo.work_order_id % 3 = 0 THEN DATE_SUB(NOW(), INTERVAL 1 DAY) ELSE NULL END,
  CONCAT('关联订单 ', co.order_no, ' · 客户 ', co.customer_name),
  DATE_SUB(NOW(), INTERVAL (40 - wo.work_order_id % 10) DAY),
  NOW()
FROM work_order wo
JOIN production_plan pp ON pp.plan_id = wo.plan_id
JOIN customer_order co ON co.order_id = pp.source_order_id
JOIN (
  SELECT wo2.work_order_id,
         GREATEST(wo2.qualified_quantity, wo2.completed_quantity, 1) * MAX(oi.unit_price) * 0.48 AS base
  FROM work_order wo2
  JOIN production_plan pp2 ON pp2.plan_id = wo2.plan_id
  JOIN customer_order_item oi ON oi.order_id = pp2.source_order_id
  WHERE wo2.work_order_id >= 31
  GROUP BY wo2.work_order_id, wo2.qualified_quantity, wo2.completed_quantity
) b ON b.work_order_id = wo.work_order_id
WHERE wo.work_order_id >= 31
  AND NOT EXISTS (SELECT 1 FROM cost_settlement cs WHERE cs.work_order_id = wo.work_order_id);

-- ── 2) 售后 / 不良品 / 设备 / 采购退货 专项成本（无工单绑定）────────
INSERT INTO cost_settlement
(settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
 material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
 settlement_period, settlement_status, confirmed_by, confirmed_at, remark, created_at, updated_at)
SELECT 'CS-AS-202607901', NULL, 42, 'AFTER_SALES', 'AS202607901',
  '深圳华创27寸4K色彩偏差售后处理：上门校准+补发线材',
  2800.00, 3600.00, 0, 1200.00, 850.00, 8450.00,
  '2026-07', 'CONFIRMED', 43, DATE_SUB(NOW(), INTERVAL 6 DAY),
  '售后案例 AS202607901 实际成本', DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cost_settlement WHERE settlement_no = 'CS-AS-202607901');

INSERT INTO cost_settlement
(settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
 material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
 settlement_period, settlement_status, remark, created_at, updated_at)
SELECT 'CS-AS-202607904', NULL, 45, 'AFTER_SALES', 'AS202607904',
  '上海优视接口异常返厂检测与换货物流',
  1200.00, 1800.00, 0, 0, 2200.00, 5200.00,
  '2026-07', 'DRAFT', '待复检确认后结算', DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cost_settlement WHERE settlement_no = 'CS-AS-202607904');

INSERT INTO cost_settlement
(settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
 material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
 settlement_period, settlement_status, confirmed_by, confirmed_at, remark, created_at, updated_at)
SELECT 'CS-NC-202607102', NULL, 44, 'NONCONFORMING_PRODUCT', 'NC202607102',
  '色准超标不良品返工：校准工时+耗材',
  0, 4200.00, 0, 6800.00, 0, 11000.00,
  '2026-07', 'CONFIRMED', 43, DATE_SUB(NOW(), INTERVAL 4 DAY),
  '质检记录 NC202607102', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cost_settlement WHERE settlement_no = 'CS-NC-202607102');

INSERT INTO cost_settlement
(settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
 material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
 settlement_period, settlement_status, confirmed_by, confirmed_at, remark, created_at, updated_at)
SELECT 'CS-EQ-202607036', NULL, 47, 'EQUIPMENT_MAINTENANCE', 'EQ-001',
  '15.6寸产线贴附机气压阀更换停机损失',
  1850.00, 920.00, 5600.00, 0, 0, 8370.00,
  '2026-07', 'CONFIRMED', 43, DATE_SUB(NOW(), INTERVAL 2 DAY),
  '设备维护记录', DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cost_settlement WHERE settlement_no = 'CS-EQ-202607036');

INSERT INTO cost_settlement
(settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
 material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
 settlement_period, settlement_status, remark, created_at, updated_at)
SELECT 'CS-PO-RETURN-078', NULL, NULL, 'PURCHASE_RETURN', 'PO-202607078',
  '背光模组来料批次不合格退货：检测费+运费',
  0, 0, 0, 3200.00, 1580.00, 4780.00,
  '2026-07', 'DRAFT', '采购退货待供应商确认', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cost_settlement WHERE settlement_no = 'CS-PO-RETURN-078');

-- ── 3) 补全交付记录
INSERT INTO delivery_order
(delivery_no, order_id, work_order_id, customer_name, material_id, batch_no,
 delivery_quantity, delivery_date, logistics_company, logistics_no,
 receiver_name, receiver_phone, receiver_address, delivery_status, shipped_by, remark, created_at, updated_at)
SELECT 'DO20260712001-B2', 42, 31, co.customer_name, wo.material_id, 'BATCH-WO202607001-810',
  80.0000, '2026-07-08', '顺丰速运', 'SF20260712001B',
  co.customer_contact, co.customer_phone, '深圳市南山区科技园', 'SHIPPED', 7,
  '华创订单第二批发货80台', NOW(), NOW()
FROM customer_order co, work_order wo
WHERE co.order_id = 42 AND wo.work_order_id = 31
  AND NOT EXISTS (SELECT 1 FROM delivery_order WHERE delivery_no = 'DO20260712001-B2');

INSERT INTO delivery_order
(delivery_no, order_id, work_order_id, customer_name, material_id, batch_no,
 delivery_quantity, delivery_date, logistics_company, logistics_no,
 receiver_name, receiver_phone, receiver_address, delivery_status, shipped_by, remark, created_at, updated_at)
SELECT 'DO2026071212002-A1', 43, 33, co.customer_name, wo.material_id, 'BATCH-WO202607003',
  120.0000, '2026-07-10', '德邦物流', 'DB2026071001',
  co.customer_contact, co.customer_phone, '北京市朝阳区', 'SHIPPED', 7,
  '华创电竞屏首批120台', NOW(), NOW()
FROM customer_order co, work_order wo
WHERE co.order_id = 43 AND wo.work_order_id = 33
  AND NOT EXISTS (SELECT 1 FROM delivery_order WHERE delivery_no = 'DO2026071212002-A1');

INSERT INTO delivery_order
(delivery_no, order_id, work_order_id, customer_name, material_id, batch_no,
 delivery_quantity, delivery_date, logistics_company, logistics_no,
 receiver_name, receiver_phone, receiver_address, delivery_status, shipped_by, remark, created_at, updated_at)
SELECT 'DO202607121212003-A1', 44, 32, co.customer_name, wo.material_id, 'BATCH-WO202607002',
  90.0000, '2026-07-11', '顺丰速运', 'SF2026071103',
  co.customer_contact, co.customer_phone, '深圳市福田区', 'SHIPPED', 7,
  '华创4K屏首批90台', NOW(), NOW()
FROM customer_order co, work_order wo
WHERE co.order_id = 44 AND wo.work_order_id = 32
  AND NOT EXISTS (SELECT 1 FROM delivery_order WHERE delivery_no = 'DO202607121212003-A1');

INSERT INTO delivery_order
(delivery_no, order_id, work_order_id, customer_name, material_id, batch_no,
 delivery_quantity, delivery_date, logistics_company, logistics_no,
 receiver_name, receiver_phone, receiver_address, delivery_status, shipped_by, remark, created_at, updated_at)
SELECT 'DO202607121212005-A1', 46, 35, co.customer_name, wo.material_id, 'BATCH-WO202607005',
  200.0000, '2026-07-09', '京东物流', 'JD2026070901',
  co.customer_contact, co.customer_phone, '广州市天河区', 'SHIPPED', 7,
  '星辰商用屏首批200台', NOW(), NOW()
FROM customer_order co, work_order wo
WHERE co.order_id = 46 AND wo.work_order_id = 35
  AND NOT EXISTS (SELECT 1 FROM delivery_order WHERE delivery_no = 'DO202607121212005-A1');

UPDATE delivery_order SET delivery_status = 'SHIPPED', delivery_date = '2026-07-12'
WHERE delivery_no = 'DO202607002' AND delivery_status = 'PREPARED';

-- ── 4) 售后闭环实际成本（支撑退款金额）────────────────────────────
UPDATE after_sales_closure SET actual_cost = 8450.00, recheck_passed = 1, customer_confirmed = 1,
  satisfaction_score = 4, root_cause = '面板贴附工艺色准漂移', responsibility = '生产工艺部',
  improvement_measures = '校准贴附机压力参数并增加成品质检抽检比例'
WHERE case_no = 'AS202607901';

INSERT INTO after_sales_closure (case_no, recheck_result, recheck_passed, customer_confirmed,
  satisfaction_score, actual_cost, root_cause, responsibility, improvement_measures, closed_remark, created_at, updated_at)
SELECT 'AS202607904', '返厂检测确认接口批次问题', 1, 0, 3, 5200.00,
  '物料来料接口批次不良', '采购部', '切换备用供应商并加严来料检验', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM after_sales_closure WHERE case_no = 'AS202607904');

-- ── 5) 回款记录：按交付进度与成本结算重算 ─────────────────────────
UPDATE finance_payment fp
JOIN customer_order co ON co.order_id = fp.order_id
LEFT JOIN (
  SELECT d.order_id, SUM(d.delivery_quantity) AS shipped_qty
  FROM delivery_order d WHERE d.delivery_status = 'SHIPPED' GROUP BY d.order_id
) dv ON dv.order_id = fp.order_id
LEFT JOIN (
  SELECT order_id, SUM(total_cost) AS order_cost
  FROM cost_settlement WHERE order_id IS NOT NULL GROUP BY order_id
) cs ON cs.order_id = fp.order_id
LEFT JOIN customer_order_item oi ON oi.order_id = fp.order_id
SET
  fp.contract_amount = co.order_amount,
  fp.discount_amount = ROUND(co.order_amount * CASE WHEN co.order_amount >= 300000 THEN 0.03 WHEN co.order_amount >= 100000 THEN 0.02 ELSE 0 END, 2),
  fp.refund_amount = COALESCE((
    SELECT SUM(c.actual_cost) FROM after_sales_case ac
    JOIN after_sales_closure c ON c.case_no = ac.case_no
    WHERE ac.order_id = fp.order_id AND c.actual_cost > 0
  ), 0),
  fp.tax_amount = ROUND(co.order_amount * 0.13, 2),
  fp.received_amount = GREATEST(0, ROUND(
    COALESCE(dv.shipped_qty, 0) * COALESCE(oi.unit_price, 0) * 0.85
    - ROUND(co.order_amount * CASE WHEN co.order_amount >= 300000 THEN 0.03 WHEN co.order_amount >= 100000 THEN 0.02 ELSE 0 END, 2) * 0.5,
    2)),
  fp.planned_date = co.required_delivery_date,
  fp.actual_date = CASE
    WHEN COALESCE(dv.shipped_qty, 0) > 0 THEN DATE_ADD(co.required_delivery_date, INTERVAL 15 DAY)
    ELSE NULL
  END,
  fp.payment_status = CASE
    WHEN co.audit_status = 'SHIPPED' AND COALESCE(dv.shipped_qty, 0) * COALESCE(oi.unit_price, 0) >= co.order_amount * 0.9 THEN 'RECEIVED'
    WHEN co.required_delivery_date < CURDATE() AND COALESCE(dv.shipped_qty, 0) * COALESCE(oi.unit_price, 0) < co.order_amount * 0.5 THEN 'OVERDUE'
    WHEN COALESCE(dv.shipped_qty, 0) > 0 THEN 'PARTIAL'
    ELSE 'PENDING'
  END,
  fp.remark = CONCAT('合同¥', FORMAT(co.order_amount, 2), ' · 已发货', COALESCE(dv.shipped_qty, 0), '台 · 订单成本¥', FORMAT(COALESCE(cs.order_cost, 0), 2)),
  fp.updated_at = NOW();

UPDATE finance_payment SET receivable_amount = GREATEST(
  contract_amount - discount_amount - refund_amount - received_amount, 0);

UPDATE finance_payment SET received_amount = 0 WHERE received_amount < 0;
UPDATE finance_payment SET receivable_amount = GREATEST(
  contract_amount - discount_amount - refund_amount - received_amount, 0)
WHERE received_amount = 0 AND receivable_amount < 0;

UPDATE finance_payment SET payment_status = 'OVERDUE'
WHERE planned_date < CURDATE() AND receivable_amount > 80000 AND payment_status = 'PENDING';

-- 补全尚未初始化的有效订单回款
INSERT INTO finance_payment
(order_id, order_no, customer_name, contract_amount, discount_amount, refund_amount, tax_amount,
 receivable_amount, received_amount, planned_date, payment_status, remark)
SELECT co.order_id, co.order_no, co.customer_name, co.order_amount, 0, 0,
  ROUND(co.order_amount * 0.13, 2), co.order_amount, 0, co.required_delivery_date, 'PENDING',
  CONCAT('待排产回款 · 订单 ', co.order_no)
FROM customer_order co
WHERE co.audit_status IN ('PLANNED', 'PLAN_PENDING')
  AND NOT EXISTS (SELECT 1 FROM finance_payment fp WHERE fp.order_id = co.order_id);

-- ── 6) 催款记录（关联真实欠款客户）────────────────────────────────
INSERT INTO finance_collection_log (order_id, order_no, customer_name, note, operator, created_at)
SELECT fp.order_id, fp.order_no, fp.customer_name,
  CONCAT('电话催款：待收 ¥', FORMAT(fp.receivable_amount, 2), '，计划回款日 ', fp.planned_date),
  'zheng_cost', DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM finance_payment fp
WHERE fp.payment_status IN ('OVERDUE', 'PARTIAL') AND fp.receivable_amount > 50000
  AND NOT EXISTS (
    SELECT 1 FROM finance_collection_log l
    WHERE l.order_id = fp.order_id AND l.note LIKE '电话催款%'
  );

INSERT INTO finance_collection_log (order_id, order_no, customer_name, note, operator, created_at)
SELECT fp.order_id, fp.order_no, fp.customer_name,
  CONCAT('邮件发送对账单，应收 ¥', FORMAT(fp.receivable_amount, 2)),
  'zheng_cost', DATE_SUB(NOW(), INTERVAL 5 DAY)
FROM finance_payment fp
WHERE fp.payment_status = 'OVERDUE' AND fp.receivable_amount > 0
  AND NOT EXISTS (
    SELECT 1 FROM finance_collection_log l
    WHERE l.order_id = fp.order_id AND l.note LIKE '邮件发送%'
  );
