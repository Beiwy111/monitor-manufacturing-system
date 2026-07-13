USE `display_manufacturing`;

-- 案例1：待受理，27寸竞技显示器，关联质检单17
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no, quality_inspection_id,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, created_at, updated_at)
SELECT 'AS202607001', 36, 8, 'BATCH-FP-202607-23G', 17,
  '北京星辰科技有限公司', '王总监', '13800001111',
  '收到的23.8寸竞技显示器色彩偏暖，与产品规格标注的sRGB 99%色域不符，颜色明显偏红',
  'COLOR_ISSUE', 'HIGH', 'OPEN',
  NOW(), NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607001');

-- 案例2：处理中，外观划痕，无质检关联
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, processing_at, created_at, updated_at)
SELECT 'AS202607002', 37, 8, 'BATCH-FP-202607-23G',
  '上海联创电子科技', '李工', '13900002222',
  '23.8寸显示器边框左下角有约3cm划痕，包装箱完好，怀疑是出厂前外观检测漏检',
  'APPEARANCE', 'MEDIUM', 'PROCESSING',
  DATE_SUB(NOW(), INTERVAL 2 DAY),
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607002');

-- 案例3：已解决，追溯结论为质检漏判
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no, quality_inspection_id,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   handle_result, trace_result,
   opened_at, processing_at, resolved_at, created_at, updated_at)
SELECT 'AS202607003', 38, 9, 'BATCH-FP-202607-27K', 18,
  '深圳明图显示器采购部', '陈主管', '13700003333',
  '27寸4K显示器局部区域出现亮线，怀疑LCD面板本身存在缺陷',
  'DISPLAY_DEFECT', 'HIGH', 'RESOLVED',
  '已对该批次面板重新质检，发现存在2条亮线缺陷未完全检出，已安排退换货并向供应商申请质量赔偿',
  '追溯至质检单QI202607002，面板检测项（亮线/暗线）判定待检，实测值未录入，系质检漏判。已联动质量改进措施。',
  DATE_SUB(NOW(), INTERVAL 5 DAY),
  DATE_SUB(NOW(), INTERVAL 4 DAY),
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607003');

-- 案例4：已关闭，接口故障，低优先级，确认为线缆问题
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   handle_result,
   opened_at, processing_at, resolved_at, closed_at, created_at, updated_at)
SELECT 'AS202607004', 36, 8, 'BATCH-FP-202607-23G',
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

-- 案例5：【分诊→技术支持】DP接口兼容性问题，AI评分低，不触发RCA
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, created_at, updated_at)
SELECT 'AS202607005', 37, 9, 'BATCH-FP-202607-27K',
  '杭州创智办公设备有限公司', '周工', '13500005555',
  '新采购的27寸4K显示器接DP线后分辨率只能到1080P，换HDMI可以到4K，怀疑DP接口或驱动有问题，显卡是RTX 3060',
  'INTERFACE_FAULT', 'LOW', 'OPEN',
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607005');

-- 案例6：【分诊→物流损坏】包装箱破损，AI识别为物流责任，不进质量流程
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, created_at, updated_at)
SELECT 'AS202607006', 36, 8, 'BATCH-FP-202607-23G',
  '成都西部电子采购中心', '刘经理', '13400006666',
  '收到货物后发现外包装箱有明显挤压变形，开箱后显示器屏幕左上角有一道横向裂痕，疑为运输途中碰撞所致',
  'APPEARANCE', 'MEDIUM', 'OPEN',
  DATE_SUB(NOW(), INTERVAL 3 DAY),
  DATE_SUB(NOW(), INTERVAL 3 DAY),
  DATE_SUB(NOW(), INTERVAL 3 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607006');

-- 案例7：【分诊→批次质量风险】20台中3台亮点+关联质检不通过，AI评分突破60触发RCA
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no, quality_inspection_id,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, created_at, updated_at)
SELECT 'AS202607007', 38, 9, 'BATCH-FP-202607-27K', 18,
  '武汉光谷科技园采购部', '孙总', '13300007777',
  '本次采购的20台4K显示器已发现3台存在屏幕亮点，其中2台右下角有固定亮点，1台中央有闪烁点，影响正常使用',
  'DEAD_PIXEL', 'HIGH', 'OPEN',
  DATE_SUB(NOW(), INTERVAL 2 DAY),
  DATE_SUB(NOW(), INTERVAL 2 DAY),
  DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607007');

-- 案例8：【分诊→业务咨询】询问报价交期，AI直接分流至采购客服，不进质量流程
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, created_at, updated_at)
SELECT 'AS202607008', NULL, 9, NULL,
  '南京云峰教育集团', '采购专员赵', '13200008888',
  '我们想采购100台27寸显示器用于高校机房，请问现在有现货吗？最快什么时候能到货？能否给一下批量采购报价',
  'OTHER', 'LOW', 'OPEN',
  DATE_SUB(NOW(), INTERVAL 4 HOUR),
  DATE_SUB(NOW(), INTERVAL 4 HOUR),
  DATE_SUB(NOW(), INTERVAL 4 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607008');

-- 案例9：【追溯根因→设备异常】已解决，根因为背光贴附设备报警，与案例3（质检漏判）形成对比
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no, quality_inspection_id,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   handle_result, trace_result,
   opened_at, processing_at, resolved_at, created_at, updated_at)
SELECT 'AS202607009', 37, 8, 'BATCH-FP-202607-23G', 17,
  '西安智慧显示技术公司', '技术主管林', '13100009999',
  '整批23.8寸竞技显示器在使用2周后陆续出现屏幕闪烁，重启后可短暂恢复，目前已影响5台，怀疑背光驱动板存在问题',
  'DISPLAY_DEFECT', 'CRITICAL', 'RESOLVED',
  '根因确认为背光贴附工序设备在生产该批次期间发生多次紧急报警（背光压力异常），导致部分背光模组贴附不均匀。已对同批次产品全检，退换受影响12台，设备完成维护校准。',
  '追溯路径：工单→设备多次URGENT报警（背光压力异常）→质检单QI202607001→批次BATCH-FP-202607-23G。根因分类：设备异常，责任部门：生产/设备部门。',
  DATE_SUB(NOW(), INTERVAL 8 DAY),
  DATE_SUB(NOW(), INTERVAL 7 DAY),
  DATE_SUB(NOW(), INTERVAL 2 DAY),
  DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607009');

-- 案例10：【追溯根因→供应商批次】处理中，5台全部色彩偏绿，指向LCD供应商批次色彩一致性问题
INSERT INTO `after_sales_case`
  (case_no, order_id, material_id, batch_no, quality_inspection_id,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   opened_at, processing_at, created_at, updated_at)
SELECT 'AS202607010', 39, 9, 'BATCH-FP-202607-27K', 18,
  '北京视界科技园', '采购部张总', '13900010101',
  '本批次4K显示器已收到5台，全部出现色彩偏绿的问题，尤其白色背景下绿色偏移明显，怀疑LCD面板本身存在色彩一致性问题',
  'COLOR_ISSUE', 'HIGH', 'PROCESSING',
  DATE_SUB(NOW(), INTERVAL 6 DAY),
  DATE_SUB(NOW(), INTERVAL 5 DAY),
  DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `after_sales_case` WHERE case_no='AS202607010');
