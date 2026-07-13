-- 修正表注释：补全缺失注释、修复中文乱码（??????）
-- 库：display_manufacturing
-- 执行前请确保客户端编码为 utf8mb4

USE display_manufacturing;
SET NAMES utf8mb4;

-- 无注释的表
ALTER TABLE `sys_role_menu` COMMENT = '角色与菜单关联表';
ALTER TABLE `production_plan_schedule` COMMENT = '生产计划工序排程明细';
ALTER TABLE `production_plan_history` COMMENT = '生产计划变更历史';
ALTER TABLE `prod_line_station` COMMENT = '产线工位实时状态';
ALTER TABLE `prod_hourly_metric` COMMENT = '按小时产量统计';
ALTER TABLE `prod_shift_capacity` COMMENT = '班组产能统计';
ALTER TABLE `prod_downtime_reason` COMMENT = '停机原因统计';

-- 注释乱码（??????）的仓储条码相关表
ALTER TABLE `material_batch` COMMENT = '物料批次主档';
ALTER TABLE `barcode_rule` COMMENT = '条码生成规则';
ALTER TABLE `inventory_barcode` COMMENT = '库存条码实例';
ALTER TABLE `inventory_scan_log` COMMENT = '库存扫码流水';

-- 其他迁移表（表存在时才更新）
SET @db = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'purchase_requirement') > 0,
  'ALTER TABLE `purchase_requirement` COMMENT = ''采购需求工作台''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'purchase_requirement_source') > 0,
  'ALTER TABLE `purchase_requirement_source` COMMENT = ''采购需求来源追溯''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'quality_inspection_item') > 0,
  'ALTER TABLE `quality_inspection_item` COMMENT = ''质检项目明细''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'supplier') > 0,
  'ALTER TABLE `supplier` COMMENT = ''供应商管理''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'agent_flow_run') > 0,
  'ALTER TABLE `agent_flow_run` COMMENT = ''Agent流程实例''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'agent_flow_step') > 0,
  'ALTER TABLE `agent_flow_step` COMMENT = ''Agent流程步骤''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
