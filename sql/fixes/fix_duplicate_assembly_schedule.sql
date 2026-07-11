-- 修复生产计划工序排程中重复的「组装」行（同一计划只保留一条组装工序）
SET NAMES utf8mb4;

DELETE pps FROM production_plan_schedule pps
INNER JOIN (
  SELECT plan_id, step_name, MIN(schedule_id) AS keep_id
  FROM production_plan_schedule
  WHERE step_name REGEXP '整机组装|背光组装|^组装$'
  GROUP BY plan_id, step_name
  HAVING COUNT(*) > 1
) keeper ON pps.plan_id = keeper.plan_id
  AND pps.step_name = keeper.step_name
  AND pps.schedule_id <> keeper.keep_id;
