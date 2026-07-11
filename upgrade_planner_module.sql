-- 计划员模块升级：排程明细、历史、计划扩展字段（display_manufacturing）
USE display_manufacturing;

ALTER TABLE production_plan
  ADD COLUMN IF NOT EXISTS version_no VARCHAR(20) DEFAULT 'V1' COMMENT '计划版本' AFTER remark,
  ADD COLUMN IF NOT EXISTS parent_plan_no VARCHAR(50) DEFAULT NULL COMMENT '复制来源计划号' AFTER version_no,
  ADD COLUMN IF NOT EXISTS adjust_reason VARCHAR(500) DEFAULT NULL COMMENT '调整原因' AFTER parent_plan_no,
  ADD COLUMN IF NOT EXISTS scheduling_mode VARCHAR(30) DEFAULT 'MANUAL' COMMENT '排产模式' AFTER adjust_reason;

CREATE TABLE IF NOT EXISTS production_plan_schedule (
  schedule_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  plan_id BIGINT NOT NULL,
  step_id BIGINT DEFAULT NULL,
  step_no INT DEFAULT NULL,
  step_name VARCHAR(100) DEFAULT NULL,
  workshop VARCHAR(100) DEFAULT NULL,
  equipment_id BIGINT DEFAULT NULL,
  equipment_code VARCHAR(50) DEFAULT NULL,
  planned_quantity DECIMAL(12,4) DEFAULT NULL,
  planned_start DATETIME DEFAULT NULL,
  planned_end DATETIME DEFAULT NULL,
  standard_hours DECIMAL(10,4) DEFAULT NULL,
  sort_no INT DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_plan_schedule_plan (plan_id),
  KEY idx_plan_schedule_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产计划工序排程明细';

CREATE TABLE IF NOT EXISTS production_plan_history (
  history_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  plan_id BIGINT NOT NULL,
  plan_no VARCHAR(50) NOT NULL,
  version_no VARCHAR(20) DEFAULT 'V1',
  action_type VARCHAR(50) NOT NULL,
  reason VARCHAR(500) DEFAULT NULL,
  snapshot_json MEDIUMTEXT,
  operator_name VARCHAR(50) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_plan_history_plan (plan_id),
  KEY idx_plan_history_no (plan_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产计划变更历史';

ALTER TABLE production_plan DROP CHECK IF EXISTS chk_production_plan_status;
ALTER TABLE production_plan ADD CONSTRAINT chk_production_plan_status CHECK (
  plan_status IN ('DRAFT','PUBLISHED','SUBMITTED','EXECUTING','COMPLETED','CANCELLED','ADJUSTED','RELEASED','RUNNING')
);

UPDATE equipment SET manufacturer = '本地设备' WHERE manufacturer IS NULL OR manufacturer LIKE '%?%';
UPDATE equipment SET workstation = REPLACE(workstation, '?', '-') WHERE workstation LIKE '%?%';
