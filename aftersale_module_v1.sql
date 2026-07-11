-- ================================================================
-- 售后模块升级：补字段 + quality_inspection_id 关联 + 演示数据
-- 目标库：display_manufacturing
-- ================================================================

USE `display_manufacturing`;

-- 1. after_sales_case 补新字段
SET @c1 := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='display_manufacturing' AND TABLE_NAME='after_sales_case'
    AND COLUMN_NAME='quality_inspection_id');
SET @s1 := IF(@c1=0,
  'ALTER TABLE `after_sales_case` ADD COLUMN `quality_inspection_id` bigint DEFAULT NULL COMMENT ''关联质检单ID，用于追溯'' AFTER `batch_no`',
  'SELECT 1');
PREPARE p FROM @s1; EXECUTE p; DEALLOCATE PREPARE p;

SET @c2 := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='display_manufacturing' AND TABLE_NAME='after_sales_case'
    AND COLUMN_NAME='processing_at');
SET @s2 := IF(@c2=0,
  'ALTER TABLE `after_sales_case` ADD COLUMN `processing_at` datetime DEFAULT NULL COMMENT ''受理时间'' AFTER `opened_at`',
  'SELECT 1');
PREPARE p FROM @s2; EXECUTE p; DEALLOCATE PREPARE p;

SET @c3 := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='display_manufacturing' AND TABLE_NAME='after_sales_case'
    AND COLUMN_NAME='resolved_at');
SET @s3 := IF(@c3=0,
  'ALTER TABLE `after_sales_case` ADD COLUMN `resolved_at` datetime DEFAULT NULL COMMENT ''解决时间'' AFTER `processing_at`',
  'SELECT 1');
PREPARE p FROM @s3; EXECUTE p; DEALLOCATE PREPARE p;

-- 2. 演示数据
-- 案例1：待受理，关联4K显示器质检单（QI202607204，色准超标已不通过）
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no, quality_inspection_id,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, created_at, updated_at)
SELECT 'AS202607001', 2, 8, 'BATCH-FP-202607-4K',
  (SELECT inspection_id FROM quality_inspection WHERE inspection_no='QI202607204' LIMIT 1),
  '北京星辰科技有限公司', '王总监', '13800001111',
  '收到的4K显示器色彩偏暖，与产品规格页标注的DCI-P3 90%色域不符，颜色明显偏红', 
  'COLOR_ISSUE', 'HIGH', 'OPEN',
  NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607001');

-- 案例2：处理中，无质检关联（出货后外观损伤）
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, processing_at, created_at, updated_at)
SELECT 'AS202607002', 1, 7, 'BATCH-FP-202607-27G',
  NULL,
  '上海联创电子科技', '李工', '13900002222',
  '27寸显示器边框左下角有约3cm划痕，包装箱完好，怀疑是出厂前外观检测漏检',
  'APPEARANCE', 'MEDIUM', 'PROCESSING',
  DATE_SUB(NOW(), INTERVAL 2 DAY),
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607002');

-- 案例3：已解决，已有追溯结论
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no, quality_inspection_id,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   handle_result, trace_result,
   opened_at, processing_at, resolved_at, created_at, updated_at)
SELECT 'AS202607003', 1, 7, 'BATCH-PANEL-202607',
  (SELECT inspection_id FROM quality_inspection WHERE inspection_no='QI202607201' LIMIT 1),
  '深圳明图显示器采购部', '陈主管', '13700003333',
  '显示器局部区域出现亮线，怀疑LCD面板本身存在缺陷',
  'DISPLAY_DEFECT', 'HIGH', 'RESOLVED',
  '已对该批次面板重新质检，发现面板批次QI202607201存在2条亮线缺陷未完全检出，已安排退换货并向供应商申请质量赔偿',
  '追溯至质检单QI202607201，面板检测项PANEL-03（亮线/暗线）判定待检，实测值未录入，系质检漏判。已联动质量改进措施。',
  DATE_SUB(NOW(), INTERVAL 5 DAY),
  DATE_SUB(NOW(), INTERVAL 4 DAY),
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607003');

-- 案例4：已关闭，接口故障，低优先级
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   handle_result,
   opened_at, processing_at, resolved_at, closed_at, created_at, updated_at)
SELECT 'AS202607004', 2, 8, 'BATCH-FP-202607-27G',
  NULL,
  '广州绿洲办公采购中心', '赵助理', '13600004444',
  'HDMI接口偶发无法识别，重新插拔后恢复正常，怀疑接口接触不良',
  'INTERFACE_FAULT', 'LOW', 'CLOSED',
  '经测试为线缆问题，非显示器本体故障，已指导客户更换HDMI线解决',
  DATE_SUB(NOW(), INTERVAL 10 DAY),
  DATE_SUB(NOW(), INTERVAL 9 DAY),
  DATE_SUB(NOW(), INTERVAL 8 DAY),
  DATE_SUB(NOW(), INTERVAL 7 DAY),
  DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607004');
