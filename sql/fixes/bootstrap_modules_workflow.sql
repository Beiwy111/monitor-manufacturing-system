-- 模块表结构补齐 + 工作流数据准备（可重复执行）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/fixes/bootstrap_modules_workflow.sql

USE display_manufacturing;
SET NAMES utf8mb4;

-- ========== 1. quality_inspection 补 inspection_status ==========
SET @has_status := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'quality_inspection'
    AND COLUMN_NAME = 'inspection_status'
);
SET @sql_status := IF(@has_status = 0,
  'ALTER TABLE quality_inspection ADD COLUMN inspection_status varchar(30) NOT NULL DEFAULT ''PENDING'' COMMENT ''质检状态'' AFTER inspection_result',
  'SELECT 1');
PREPARE s FROM @sql_status; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE quality_inspection SET inspection_status = CASE inspection_result
  WHEN 'PENDING' THEN 'PENDING'
  WHEN 'QUALIFIED' THEN 'PASSED'
  WHEN 'UNQUALIFIED' THEN 'FAILED'
  ELSE COALESCE(inspection_status, 'PENDING')
END
WHERE inspection_status IS NULL OR inspection_status = '';

-- ========== 2. quality_inspection_item 表 ==========
CREATE TABLE IF NOT EXISTS quality_inspection_item (
  inspection_item_id bigint NOT NULL AUTO_INCREMENT,
  inspection_id      bigint NOT NULL,
  item_code          varchar(30)  NOT NULL DEFAULT '',
  item_name          varchar(100) NOT NULL DEFAULT '',
  standard_value     varchar(100) NOT NULL DEFAULT '',
  measured_value     varchar(100)          DEFAULT NULL,
  unit               varchar(20)           DEFAULT '',
  result             varchar(20)  NOT NULL DEFAULT 'PENDING',
  defect_level       varchar(20)           DEFAULT NULL,
  sort_order         int          NOT NULL DEFAULT 0,
  remark             varchar(500)          DEFAULT NULL,
  created_at         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (inspection_item_id),
  KEY idx_inspection_id (inspection_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检项目明细';

-- ========== 3. 工艺工序编码修正（四道工序） ==========
UPDATE process_step SET step_code = 'STEP-03', step_no = 30, step_name = '面板贴附'
WHERE step_code = 'STEP-TMP-03';
UPDATE process_step SET step_code = 'STEP-04', step_no = 40, step_name = '整机组装'
WHERE step_code = 'STEP-TMP-04';

-- ========== 4. 工作流：WO202607001 四道工序已完成，重置终检派工为可提交质检 ==========
UPDATE work_order
SET status = 'PRODUCING', updated_at = NOW()
WHERE work_order_no = 'WO202607001' AND status = 'COMPLETED';

UPDATE dispatch_task dt
JOIN work_order wo ON wo.work_order_id = dt.work_order_id
JOIN process_step ps ON ps.step_id = dt.step_id
SET dt.status = 'PRODUCING', dt.updated_at = NOW()
WHERE wo.work_order_no = 'WO202607001'
  AND ps.step_name = '整机组装'
  AND dt.completed_quantity >= dt.assigned_quantity;

-- ========== 5. 采购需求：基于真实库存与工单 BOM 缺料 ==========
INSERT INTO purchase_requirement (
  material_id, material_code, material_name,
  required_quantity, stock_quantity, on_purchase_quantity, shortage_quantity, suggested_purchase_quantity,
  status, priority, expected_arrival_date, remark, created_at, updated_at
)
SELECT
  m.material_id, m.material_code, m.material_name,
  GREATEST(wo.planned_quantity * 2, 200) AS required_quantity,
  COALESCE(SUM(i.quantity_available), 0) AS stock_quantity,
  0 AS on_purchase_quantity,
  GREATEST(GREATEST(wo.planned_quantity * 2, 200) - COALESCE(SUM(i.quantity_available), 0), 0) AS shortage_quantity,
  GREATEST(GREATEST(wo.planned_quantity * 2, 200) - COALESCE(SUM(i.quantity_available), 0), 0) AS suggested_purchase_quantity,
  'PENDING', 3, DATE_ADD(CURDATE(), INTERVAL 7 DAY),
  CONCAT('工单 ', wo.work_order_no, ' 生产备料需求'),
  NOW(), NOW()
FROM work_order wo
JOIN material m ON m.material_id IN (1, 2, 5)
LEFT JOIN inventory i ON i.material_id = m.material_id
WHERE wo.work_order_no = 'WO202607001'
GROUP BY m.material_id, m.material_code, m.material_name, wo.planned_quantity, wo.work_order_no
HAVING shortage_quantity > 0
  AND NOT EXISTS (
    SELECT 1 FROM purchase_requirement pr WHERE pr.material_id = m.material_id AND pr.status = 'PENDING'
  );

INSERT INTO purchase_requirement_source (
  requirement_id, customer_order_id, customer_order_no, work_order_id, work_order_no,
  source_type, material_id, required_quantity, shortage_quantity, created_at
)
SELECT
  pr.requirement_id,
  pp.source_order_id,
  co.order_no,
  wo.work_order_id,
  wo.work_order_no,
  'WORK_ORDER',
  pr.material_id,
  pr.required_quantity,
  pr.shortage_quantity,
  NOW()
FROM purchase_requirement pr
JOIN work_order wo ON wo.work_order_no = 'WO202607001'
JOIN production_plan pp ON pp.plan_id = wo.plan_id
JOIN customer_order co ON co.order_id = pp.source_order_id
WHERE NOT EXISTS (
  SELECT 1 FROM purchase_requirement_source s WHERE s.requirement_id = pr.requirement_id
);

-- ========== 6. 售后案例：关联真实订单与成品 ==========
INSERT INTO after_sales_case (
  case_no, order_id, material_id, batch_no,
  customer_name, contact_name, contact_phone,
  problem_description, problem_type, case_level, case_status,
  opened_at, created_at, updated_at
)
SELECT
  'AS202607101', co.order_id, 9, 'BATCH-WO202607001',
  co.customer_name, '李经理', '13800001001',
  '27寸4K显示器收到后屏幕右下角有轻微色差', '显示异常', 'GENERAL', 'OPEN',
  NOW(), NOW(), NOW()
FROM customer_order co
WHERE co.order_no = 'CO202607001'
  AND NOT EXISTS (SELECT 1 FROM after_sales_case WHERE case_no = 'AS202607101');

-- ========== 7. 成本结算：关联真实工单与订单 ==========
INSERT INTO cost_settlement (
  settlement_no, work_order_id, order_id, source_type, source_id, cost_reason,
  material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost,
  settlement_period, settlement_status, remark, created_at, updated_at
)
SELECT
  'CS202607101', wo.work_order_id, pp.source_order_id, 'PRODUCTION', wo.work_order_no,
  CONCAT('工单 ', wo.work_order_no, ' 生产成本归集'),
  12500.00, 8600.00, 1200.00, 450.00, 300.00, 23050.00,
  DATE_FORMAT(NOW(), '%Y-%m'), 'DRAFT', '基于真实工单归集，待财务确认',
  NOW(), NOW()
FROM work_order wo
JOIN production_plan pp ON pp.plan_id = wo.plan_id
WHERE wo.work_order_no = 'WO202607001'
  AND NOT EXISTS (SELECT 1 FROM cost_settlement WHERE settlement_no = 'CS202607101');

-- ========== 8. 供应商表 + material.supplier_id（采购模块依赖） ==========
CREATE TABLE IF NOT EXISTS supplier (
  supplier_id    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
  supplier_no    VARCHAR(32)   NOT NULL COMMENT '供应商编号',
  supplier_name  VARCHAR(150)  NOT NULL COMMENT '供应商名称',
  contact_person VARCHAR(100)  DEFAULT NULL COMMENT '联系人',
  contact_phone  VARCHAR(50)   DEFAULT NULL COMMENT '联系电话',
  contact_email  VARCHAR(150)  DEFAULT NULL COMMENT '联系邮箱',
  address        VARCHAR(300)  DEFAULT NULL COMMENT '地址',
  supply_materials VARCHAR(500) DEFAULT NULL COMMENT '主要供应物料',
  status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  remark         VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  created_at     DATETIME      DEFAULT NULL COMMENT '创建时间',
  updated_at     DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (supplier_id),
  UNIQUE KEY uk_supplier_no (supplier_no),
  KEY idx_name (supplier_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商管理';

SET @has_supplier_col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'material'
    AND COLUMN_NAME = 'supplier_id'
);
SET @sql_supplier_col := IF(@has_supplier_col = 0,
  'ALTER TABLE material ADD COLUMN supplier_id bigint DEFAULT NULL COMMENT ''默认供应商'' AFTER standard_cost',
  'SELECT 1');
PREPARE s FROM @sql_supplier_col; EXECUTE s; DEALLOCATE PREPARE s;

INSERT INTO supplier (supplier_no, supplier_name, contact_person, contact_phone, status, created_at, updated_at)
SELECT 'SUP001', '华南显示科技', '张供应', '13800002001', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE supplier_no = 'SUP001');

UPDATE material SET supplier_id = (SELECT supplier_id FROM supplier WHERE supplier_no = 'SUP001' LIMIT 1)
WHERE material_type = 'RAW' AND supplier_id IS NULL;
