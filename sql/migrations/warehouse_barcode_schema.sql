-- 仓储条码/批次/扫码流转（第一阶段）
-- 基于现有 inventory / inventory_transaction 表扩展，不破坏原库存结构。

USE display_manufacturing;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `material_batch` (
  `batch_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `batch_no` varchar(80) NOT NULL COMMENT '批次号',
  `material_id` bigint unsigned NOT NULL COMMENT '物料ID',
  `source_type` varchar(30) NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型',
  `source_no` varchar(80) DEFAULT NULL COMMENT '来源单号',
  `quantity` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '批次数量',
  `batch_status` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '批次状态',
  `produced_at` datetime DEFAULT NULL COMMENT '生产时间',
  `received_at` datetime DEFAULT NULL COMMENT '入库时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`batch_id`),
  UNIQUE KEY `uk_material_batch_no_material` (`batch_no`, `material_id`),
  KEY `idx_material_batch_material` (`material_id`),
  CONSTRAINT `fk_material_batch_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料批次主档';

CREATE TABLE IF NOT EXISTS `barcode_rule` (
  `rule_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码',
  `business_type` varchar(30) NOT NULL COMMENT '业务类型',
  `prefix` varchar(20) NOT NULL COMMENT '条码前缀',
  `date_pattern` varchar(20) NOT NULL DEFAULT 'yyyyMMdd' COMMENT '日期格式',
  `serial_length` int NOT NULL DEFAULT 5 COMMENT '流水位数',
  `current_serial` bigint unsigned NOT NULL DEFAULT 0 COMMENT '当前流水',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_barcode_rule_code` (`rule_code`),
  UNIQUE KEY `uk_barcode_business_type` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='条码生成规则';

CREATE TABLE IF NOT EXISTS `inventory_barcode` (
  `barcode_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '条码ID',
  `barcode_no` varchar(100) NOT NULL COMMENT '条码号',
  `material_id` bigint unsigned NOT NULL COMMENT '物料ID',
  `batch_no` varchar(80) DEFAULT NULL COMMENT '批次号',
  `inventory_id` bigint unsigned DEFAULT NULL COMMENT '库存ID',
  `quantity` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '条码数量',
  `remaining_quantity` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '剩余数量',
  `barcode_status` varchar(20) NOT NULL DEFAULT 'IN_STOCK' COMMENT '条码状态',
  `source_type` varchar(30) NOT NULL COMMENT '来源类型',
  `source_no` varchar(80) DEFAULT NULL COMMENT '来源单号',
  `related_work_order_id` bigint unsigned DEFAULT NULL COMMENT '关联工单ID',
  `related_purchase_order_id` bigint unsigned DEFAULT NULL COMMENT '关联采购订单ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`barcode_id`),
  UNIQUE KEY `uk_inventory_barcode_no` (`barcode_no`),
  KEY `idx_inventory_barcode_material` (`material_id`),
  KEY `idx_inventory_barcode_batch` (`batch_no`),
  KEY `idx_inventory_barcode_inventory` (`inventory_id`),
  CONSTRAINT `fk_inventory_barcode_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`material_id`),
  CONSTRAINT `fk_inventory_barcode_inventory` FOREIGN KEY (`inventory_id`) REFERENCES `inventory` (`inventory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存条码实例';

CREATE TABLE IF NOT EXISTS `inventory_scan_log` (
  `scan_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '扫码ID',
  `scan_no` varchar(50) NOT NULL COMMENT '扫码流水号',
  `barcode_no` varchar(100) NOT NULL COMMENT '条码号',
  `scan_type` varchar(30) NOT NULL COMMENT '扫码类型',
  `quantity` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '扫码数量',
  `warehouse_code` varchar(50) DEFAULT NULL COMMENT '仓库编码',
  `location_code` varchar(50) DEFAULT NULL COMMENT '库位编码',
  `business_no` varchar(80) DEFAULT NULL COMMENT '业务单号',
  `result_status` varchar(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '结果状态',
  `message` varchar(500) DEFAULT NULL COMMENT '结果说明',
  `handled_by` bigint unsigned DEFAULT NULL COMMENT '操作人ID',
  `handled_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`scan_id`),
  UNIQUE KEY `uk_inventory_scan_no` (`scan_no`),
  KEY `idx_inventory_scan_barcode` (`barcode_no`),
  KEY `idx_inventory_scan_type` (`scan_type`),
  KEY `idx_inventory_scan_time` (`handled_at`),
  CONSTRAINT `fk_inventory_scan_user` FOREIGN KEY (`handled_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存扫码流水';

INSERT INTO `barcode_rule` (`rule_code`, `business_type`, `prefix`, `date_pattern`, `serial_length`, `status`)
SELECT 'RULE-PRODUCT-IN', 'PRODUCT_IN', 'PDI', 'yyyyMMdd', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM `barcode_rule` WHERE `business_type` = 'PRODUCT_IN');

INSERT INTO `barcode_rule` (`rule_code`, `business_type`, `prefix`, `date_pattern`, `serial_length`, `status`)
SELECT 'RULE-MATERIAL-OUT', 'MATERIAL_OUT', 'MTO', 'yyyyMMdd', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM `barcode_rule` WHERE `business_type` = 'MATERIAL_OUT');

INSERT INTO `barcode_rule` (`rule_code`, `business_type`, `prefix`, `date_pattern`, `serial_length`, `status`)
SELECT 'RULE-PURCHASE-IN', 'PURCHASE_IN', 'PCI', 'yyyyMMdd', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM `barcode_rule` WHERE `business_type` = 'PURCHASE_IN');

-- 从现有库存补齐批次主档。
INSERT INTO `material_batch` (`batch_no`, `material_id`, `source_type`, `source_no`, `quantity`, `batch_status`, `received_at`)
SELECT i.`batch_no`, i.`material_id`, 'INIT', i.`inventory_id`, i.`quantity_on_hand`, i.`inventory_status`, i.`last_transaction_at`
FROM `inventory` i
WHERE i.`batch_no` IS NOT NULL AND i.`batch_no` <> ''
  AND NOT EXISTS (
    SELECT 1 FROM `material_batch` b
    WHERE b.`batch_no` = i.`batch_no` AND b.`material_id` = i.`material_id`
  );
