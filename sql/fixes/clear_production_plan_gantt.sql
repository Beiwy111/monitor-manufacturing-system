-- 清空生产计划甘特排程（production_plan_schedule 为甘特图活数据来源）
-- mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/fixes/clear_production_plan_gantt.sql
SET NAMES utf8mb4;

DELETE FROM production_plan_schedule;

SELECT COUNT(*) AS remaining_schedules FROM production_plan_schedule;
