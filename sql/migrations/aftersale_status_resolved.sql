-- 售后工单状态约束补全 RESOLVED（标记已解决）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/migrations/aftersale_status_resolved.sql

USE display_manufacturing;
SET NAMES utf8mb4;

ALTER TABLE after_sales_case DROP CHECK chk_after_sales_status;

ALTER TABLE after_sales_case
  ADD CONSTRAINT chk_after_sales_status
  CHECK (case_status IN ('OPEN', 'TRACING', 'PROCESSING', 'RESOLVED', 'CLOSED', 'CANCELLED'));
