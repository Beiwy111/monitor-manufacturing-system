SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS after_sales_rca_analysis (
  analysis_id bigint unsigned NOT NULL AUTO_INCREMENT,
  analysis_no varchar(80) NOT NULL,
  case_no varchar(50) NOT NULL,
  algorithm_version varchar(30) NOT NULL,
  model_type varchar(20) NOT NULL,
  top_cause varchar(100) DEFAULT NULL,
  top_department varchar(30) DEFAULT NULL,
  top_score decimal(5,2) DEFAULT NULL,
  data_completeness decimal(5,4) DEFAULT NULL,
  estimated_loss decimal(18,2) NOT NULL DEFAULT 0,
  conclusion text,
  confirmed_cause varchar(100) DEFAULT NULL,
  confirmed_department varchar(30) DEFAULT NULL,
  confirm_remark varchar(1000) DEFAULT NULL,
  confirmed_at datetime DEFAULT NULL,
  snapshot_json longtext NOT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (analysis_id),
  UNIQUE KEY uk_rca_analysis_no (analysis_no),
  KEY idx_rca_case (case_no,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后根因分析快照';

CREATE TABLE IF NOT EXISTS after_sales_collaboration_task (
  task_id bigint unsigned NOT NULL AUTO_INCREMENT,
  task_no varchar(100) NOT NULL,
  case_no varchar(50) NOT NULL,
  analysis_no varchar(80) DEFAULT NULL,
  department_code varchar(30) NOT NULL,
  title varchar(200) NOT NULL,
  content varchar(1000) DEFAULT NULL,
  priority varchar(20) NOT NULL DEFAULT 'NORMAL',
  status varchar(20) NOT NULL DEFAULT 'PENDING',
  result varchar(1000) DEFAULT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at datetime DEFAULT NULL,
  PRIMARY KEY (task_id),
  UNIQUE KEY uk_rca_task_no (task_no),
  KEY idx_rca_task_department (department_code,status),
  KEY idx_rca_task_case (case_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后跨部门协同任务';

CREATE TABLE IF NOT EXISTS sys_notification (
  notification_id bigint unsigned NOT NULL AUTO_INCREMENT,
  receiver_role varchar(30) NOT NULL,
  title varchar(200) NOT NULL,
  content varchar(1000) DEFAULT NULL,
  level varchar(20) NOT NULL DEFAULT 'INFO',
  business_type varchar(40) DEFAULT NULL,
  business_id varchar(100) DEFAULT NULL,
  target_path varchar(255) DEFAULT NULL,
  read_status tinyint NOT NULL DEFAULT 0,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (notification_id),
  KEY idx_notification_role (receiver_role,read_status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统业务通知';

CREATE TABLE IF NOT EXISTS after_sales_triage (
  triage_id bigint unsigned NOT NULL AUTO_INCREMENT,
  triage_no varchar(80) NOT NULL,
  case_no varchar(50) NOT NULL,
  category varchar(40) NOT NULL,
  category_name varchar(100) NOT NULL,
  escalation_score int NOT NULL DEFAULT 0,
  risk_level varchar(20) NOT NULL,
  need_rca tinyint NOT NULL DEFAULT 0,
  model_type varchar(20) NOT NULL,
  reason_json longtext NOT NULL,
  action_json longtext NOT NULL,
  summary varchar(1000) DEFAULT NULL,
  snapshot_json longtext NOT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (triage_id),
  UNIQUE KEY uk_triage_no (triage_no),
  KEY idx_triage_case (case_no,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI售后智能分诊快照';

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='after_sales_rca_analysis' AND COLUMN_NAME='confirmed_cause')=0,'ALTER TABLE after_sales_rca_analysis ADD COLUMN confirmed_cause varchar(100) DEFAULT NULL','SELECT 1'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='after_sales_rca_analysis' AND COLUMN_NAME='confirmed_department')=0,'ALTER TABLE after_sales_rca_analysis ADD COLUMN confirmed_department varchar(30) DEFAULT NULL','SELECT 1'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='after_sales_rca_analysis' AND COLUMN_NAME='confirm_remark')=0,'ALTER TABLE after_sales_rca_analysis ADD COLUMN confirm_remark varchar(1000) DEFAULT NULL','SELECT 1'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='after_sales_rca_analysis' AND COLUMN_NAME='confirmed_at')=0,'ALTER TABLE after_sales_rca_analysis ADD COLUMN confirmed_at datetime DEFAULT NULL','SELECT 1'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
