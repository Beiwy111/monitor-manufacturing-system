-- 清空甘特排程明细（production_plan_schedule），保留生产计划主表
-- mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/fixes/clear_gantt_schedules.sql
SET NAMES utf8mb4;

DELETE FROM production_plan_schedule;
