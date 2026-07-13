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

-- ================================================================
-- 差异化演示工单：用于展示 AI 分诊与追溯的多种路由差异
-- ================================================================

-- 案例5【分诊路由：技术支持/兼容诊断 → 低风险 → 不触发RCA】
-- 特征：DP接口+驱动关键词 → AI分诊识别为兼容性问题，评分低，建议远程诊断而非质量追溯
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, created_at, updated_at)
SELECT 'AS202607005', 2, 8, 'BATCH-FP-202607-4K',
  NULL,
  '杭州创智办公设备有限公司', '周工', '13500005555',
  '新采购的4K显示器接DP线后分辨率只能到1080P，换HDMI可以到4K，怀疑DP接口或驱动有问题，显卡是RTX 3060',
  'INTERFACE_FAULT', 'LOW', 'OPEN',
  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607005');

-- 案例6【分诊路由：物流损坏 → 中风险 → 不触发RCA，走换货流程】
-- 特征：运输+包装破损关键词 → AI分诊识别为物流责任，建议留存物流证据
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, created_at, updated_at)
SELECT 'AS202607006', 1, 7, 'BATCH-FP-202607-27G',
  NULL,
  '成都西部电子采购中心', '刘经理', '13400006666',
  '收到货物后发现外包装箱有明显挤压变形，开箱后显示器屏幕左上角有一道横向裂痕，疑为运输途中碰撞所致',
  'APPEARANCE', 'MEDIUM', 'OPEN',
  DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607006');

-- 案例7【分诊路由：批次质量风险 → 高风险 → 触发RCA + 跨部门协同】
-- 特征：同批次已有多条亮点投诉 + 质检不通过 → 评分突破60，AI升级为批次事件
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no, quality_inspection_id,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, created_at, updated_at)
SELECT 'AS202607007', 2, 8, 'BATCH-FP-202607-4K',
  (SELECT inspection_id FROM quality_inspection WHERE inspection_no='QI202607204' LIMIT 1),
  '武汉光谷科技园采购部', '孙总', '13300007777',
  '本次采购的20台4K显示器已发现3台存在屏幕亮点，其中2台右下角有固定亮点，1台中央有闪烁点，影响正常使用',
  'DEAD_PIXEL', 'HIGH', 'OPEN',
  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607007');

-- 案例8【分诊路由：业务/采购咨询 → 极低风险 → 转客服，不进质量流程】
-- 特征：询问交期、报价关键词 → AI识别为非质量类，直接分流至采购客服
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, created_at, updated_at)
SELECT 'AS202607008', NULL, 8, NULL,
  NULL,
  '南京云峰教育集团', '采购专员赵', '13200008888',
  '我们想采购100台27寸显示器用于高校机房，请问现在有现货吗？最快什么时候能到货？能否给一下批量采购报价',
  'OTHER', 'LOW', 'OPEN',
  DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607008');

-- 案例9【追溯差异：有完整追溯链路 - 工单→设备报警→质检→根因确认】
-- 特征：已完成RCA，根因确认为设备异常，追溯链路完整，展示与案例3（质检漏判根因）的差异
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no, quality_inspection_id,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   handle_result, trace_result,
   opened_at, processing_at, resolved_at, created_at, updated_at)
SELECT 'AS202607009', 1, 7, 'BATCH-FP-202607-27G',
  (SELECT inspection_id FROM quality_inspection WHERE inspection_no='QI202607201' LIMIT 1),
  '西安智慧显示技术公司', '技术主管林', '13100009999',
  '整批27寸显示器在使用2周后陆续出现屏幕闪烁，重启后可短暂恢复，怀疑背光驱动板存在问题，目前已影响5台',
  'DISPLAY_DEFECT', 'CRITICAL', 'RESOLVED',
  '根因确认为背光贴附工序设备EQ-BL-03在生产该批次期间发生5次紧急报警（背光压力异常），导致部分背光模组贴附不均匀。已对同批次产品全检，退换受影响12台，设备完成维护。',
  '追溯路径：工单WO-2026-0315→设备EQ-BL-03（5次URGENT报警）→质检QI202607201（贴附项目实测值偏差）→批次BATCH-FP-202607-27G。根因分类：设备异常，责任部门：生产/设备。',
  DATE_SUB(NOW(), INTERVAL 8 DAY),
  DATE_SUB(NOW(), INTERVAL 7 DAY),
  DATE_SUB(NOW(), INTERVAL 2 DAY),
  DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607009');

-- 案例10【追溯差异：追溯到供应商批次异常，与案例3/9形成三种根因对比】
-- 特征：处理中，关联质检，追溯分析指向LCD供应商批次异常（物料维度根因）
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no, quality_inspection_id,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, processing_at, created_at, updated_at)
SELECT 'AS202607010', 2, 8, 'BATCH-FP-202607-4K',
  (SELECT inspection_id FROM quality_inspection WHERE inspection_no='QI202607204' LIMIT 1),
  '北京视界科技园', '采购部张总', '13900010101',
  '本批次4K显示器已收到5台，全部出现色彩偏绿的问题，尤其白色背景下绿色偏移明显，怀疑LCD面板本身存在色彩一致性问题',
  'COLOR_ISSUE', 'HIGH', 'PROCESSING',
  DATE_SUB(NOW(), INTERVAL 6 DAY),
  DATE_SUB(NOW(), INTERVAL 5 DAY),
  DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607010');
