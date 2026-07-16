-- 售后管理模块重构：统一流程状态 + 方案/执行/闭环表
USE display_manufacturing;
SET NAMES utf8mb4;

-- 1) 案例表补 SLA
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'after_sales_case' AND COLUMN_NAME = 'sla_deadline');
SET @s := IF(@c = 0,
  'ALTER TABLE after_sales_case ADD COLUMN sla_deadline datetime DEFAULT NULL COMMENT ''SLA截止时间'' AFTER processing_at',
  'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- 2) 旧状态迁移
UPDATE after_sales_case SET case_status = 'OPEN' WHERE case_status IN ('PENDING_ACCEPT');
UPDATE after_sales_case SET case_status = 'ACCEPTED' WHERE case_status IN ('PROCESSING', 'TRACING', 'INVESTIGATING', 'QUALITY_TRACE');
UPDATE after_sales_case SET case_status = 'PENDING_PLAN' WHERE case_status IN ('PLAN_PENDING');

-- 3) 状态约束
ALTER TABLE after_sales_case DROP CHECK chk_after_sales_status;
ALTER TABLE after_sales_case
  ADD CONSTRAINT chk_after_sales_status
  CHECK (case_status IN (
    'OPEN', 'ACCEPTED', 'PENDING_PLAN', 'PENDING_APPROVAL', 'EXECUTING',
    'PENDING_RECHECK', 'PENDING_CONFIRM', 'RESOLVED', 'CLOSED',
    'PROCESSING', 'TRACING', 'CANCELLED'
  ));

-- 4) 方案审批
CREATE TABLE IF NOT EXISTS after_sales_plan (
  plan_id           bigint unsigned NOT NULL AUTO_INCREMENT,
  plan_no           varchar(32)     NOT NULL,
  case_no           varchar(50)     NOT NULL,
  plan_type         varchar(32)     NOT NULL COMMENT 'REMOTE_GUIDE/RETURN_INSPECTION/REPAIR/EXCHANGE/RETURN/PARTS_RESUPPLY/SUPPLIER_CLAIM/BATCH_RECALL',
  trace_summary     varchar(2000)   DEFAULT NULL,
  plan_detail       text,
  owner_name        varchar(64)     DEFAULT NULL,
  expected_finish_at datetime       DEFAULT NULL,
  estimated_cost    decimal(12,2)   DEFAULT NULL,
  parts_json        varchar(2000)   DEFAULT NULL,
  customer_opinion  varchar(1000)   DEFAULT NULL,
  approval_status   varchar(20)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/APPROVED/REJECTED',
  approved_at       datetime        DEFAULT NULL,
  created_at        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (plan_id),
  UNIQUE KEY uk_plan_no (plan_no),
  KEY idx_plan_case (case_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后处置方案';

-- 5) 执行协同任务
CREATE TABLE IF NOT EXISTS after_sales_task (
  task_id        bigint unsigned NOT NULL AUTO_INCREMENT,
  task_no        varchar(32)     NOT NULL,
  case_no        varchar(50)     NOT NULL,
  plan_id        bigint unsigned DEFAULT NULL,
  task_type      varchar(32)     NOT NULL,
  title          varchar(128)    NOT NULL,
  assignee       varchar(64)     DEFAULT NULL,
  status         varchar(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/DONE/CANCELLED',
  due_at         datetime        DEFAULT NULL,
  started_at     datetime        DEFAULT NULL,
  completed_at   datetime        DEFAULT NULL,
  remark         varchar(500)    DEFAULT NULL,
  created_at     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (task_id),
  UNIQUE KEY uk_task_no (task_no),
  KEY idx_task_case (case_no, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后执行任务';

-- 6) 验证闭环
CREATE TABLE IF NOT EXISTS after_sales_closure (
  closure_id            bigint unsigned NOT NULL AUTO_INCREMENT,
  case_no               varchar(50)     NOT NULL,
  recheck_result        varchar(500)    DEFAULT NULL,
  recheck_passed        tinyint         NOT NULL DEFAULT 0,
  customer_confirmed    tinyint         NOT NULL DEFAULT 0,
  satisfaction_score    int             DEFAULT NULL,
  actual_cost           decimal(12,2)   DEFAULT NULL,
  root_cause            varchar(500)    DEFAULT NULL,
  responsibility        varchar(200)    DEFAULT NULL,
  improvement_measures  text,
  closed_remark         varchar(500)    DEFAULT NULL,
  created_at            datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (closure_id),
  UNIQUE KEY uk_closure_case (case_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后验证闭环';
