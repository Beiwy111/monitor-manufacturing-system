-- 对齐 after_sales_closure 表结构与 AfterSalesWorkflowMapper（旧表字段名不同）
USE display_manufacturing;
SET NAMES utf8mb4;

-- reinspection_result -> recheck_result
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'after_sales_closure' AND COLUMN_NAME = 'recheck_result');
SET @old := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'after_sales_closure' AND COLUMN_NAME = 'reinspection_result');
SET @s := IF(@c = 0 AND @old > 0,
  'ALTER TABLE after_sales_closure CHANGE COLUMN reinspection_result recheck_result varchar(500) DEFAULT NULL COMMENT ''复检结果''',
  'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- recheck_passed
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'after_sales_closure' AND COLUMN_NAME = 'recheck_passed');
SET @s := IF(@c = 0,
  'ALTER TABLE after_sales_closure ADD COLUMN recheck_passed tinyint NOT NULL DEFAULT 0 COMMENT ''复检通过'' AFTER recheck_result',
  'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- satisfaction -> satisfaction_score
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'after_sales_closure' AND COLUMN_NAME = 'satisfaction_score');
SET @old := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'after_sales_closure' AND COLUMN_NAME = 'satisfaction');
SET @s := IF(@c = 0 AND @old > 0,
  'ALTER TABLE after_sales_closure CHANGE COLUMN satisfaction satisfaction_score int DEFAULT NULL COMMENT ''满意度''',
  'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- responsibility_dept -> responsibility
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'after_sales_closure' AND COLUMN_NAME = 'responsibility');
SET @old := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'after_sales_closure' AND COLUMN_NAME = 'responsibility_dept');
SET @s := IF(@c = 0 AND @old > 0,
  'ALTER TABLE after_sales_closure CHANGE COLUMN responsibility_dept responsibility varchar(200) DEFAULT NULL COMMENT ''责任归属''',
  'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- closed_remark
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'after_sales_closure' AND COLUMN_NAME = 'closed_remark');
SET @s := IF(@c = 0,
  'ALTER TABLE after_sales_closure ADD COLUMN closed_remark varchar(500) DEFAULT NULL COMMENT ''关闭备注'' AFTER improvement_measures',
  'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- created_at
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'display_manufacturing' AND TABLE_NAME = 'after_sales_closure' AND COLUMN_NAME = 'created_at');
SET @s := IF(@c = 0,
  'ALTER TABLE after_sales_closure ADD COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER closed_remark',
  'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
