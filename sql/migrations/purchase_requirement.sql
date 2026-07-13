-- 采购模块升级：缺料需求工作台建表脚本
-- 库：display_manufacturing

-- 采购需求（物料维度汇总）
DROP TABLE IF EXISTS `purchase_requirement`;
CREATE TABLE `purchase_requirement` (
  `requirement_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '采购需求ID',
  `material_id` BIGINT NOT NULL COMMENT '物料ID',
  `material_code` VARCHAR(64) DEFAULT NULL COMMENT '物料编码',
  `material_name` VARCHAR(128) DEFAULT NULL COMMENT '物料名称',
  `required_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '总需求数量',
  `stock_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '现有可用库存',
  `on_purchase_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '在途采购数量',
  `shortage_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '净缺料数量',
  `suggested_purchase_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '建议采购数量',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SELECTED/PURCHASED/PART_ARRIVED/ARRIVED/CANCELLED',
  `priority` INT DEFAULT 5 COMMENT '优先级，数值越小越紧急',
  `expected_arrival_date` DATE DEFAULT NULL COMMENT '期望到货日期',
  `purchase_order_id` BIGINT DEFAULT NULL COMMENT '生成的采购单ID',
  `supplier_id` BIGINT DEFAULT NULL COMMENT '对应供应商ID',
  `supplier_name` VARCHAR(150) DEFAULT NULL COMMENT '对应供应商名称',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`requirement_id`),
  KEY `idx_material` (`material_id`),
  KEY `idx_status` (`status`),
  KEY `idx_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购需求工作台';

-- 采购需求来源追溯
DROP TABLE IF EXISTS `purchase_requirement_source`;
CREATE TABLE `purchase_requirement_source` (
  `source_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '来源ID',
  `requirement_id` BIGINT NOT NULL COMMENT '关联采购需求ID',
  `customer_order_id` BIGINT DEFAULT NULL COMMENT '来源销售订单ID',
  `customer_order_no` VARCHAR(64) DEFAULT NULL COMMENT '来源销售订单编号',
  `work_order_id` BIGINT DEFAULT NULL COMMENT '来源生产工单ID',
  `work_order_no` VARCHAR(64) DEFAULT NULL COMMENT '来源生产工单编号',
  `source_type` VARCHAR(32) DEFAULT NULL COMMENT 'ORDER/WORK_ORDER',
  `material_id` BIGINT NOT NULL COMMENT '缺料物料ID',
  `required_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '该来源需求数量',
  `shortage_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '该来源缺料数量',
  `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`source_id`),
  KEY `idx_requirement` (`requirement_id`),
  KEY `idx_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购需求来源追溯';
