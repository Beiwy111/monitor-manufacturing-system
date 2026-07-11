-- 供应商管理表
-- 执行后需重启后端

CREATE TABLE IF NOT EXISTS `supplier` (
  `supplier_id`    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
  `supplier_no`    VARCHAR(32)   NOT NULL COMMENT '供应商编号',
  `supplier_name`  VARCHAR(150)  NOT NULL COMMENT '供应商名称',
  `contact_person` VARCHAR(100)  DEFAULT NULL COMMENT '联系人',
  `contact_phone`  VARCHAR(50)   DEFAULT NULL COMMENT '联系电话',
  `contact_email`  VARCHAR(150)  DEFAULT NULL COMMENT '联系邮箱',
  `address`        VARCHAR(300)  DEFAULT NULL COMMENT '地址',
  `supply_materials` VARCHAR(500) DEFAULT NULL COMMENT '主要供应物料',
  `status`         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  `remark`         VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  `created_at`     DATETIME      DEFAULT NULL COMMENT '创建时间',
  `updated_at`     DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`supplier_id`),
  UNIQUE KEY `uk_supplier_no` (`supplier_no`),
  KEY `idx_name` (`supplier_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商管理';
