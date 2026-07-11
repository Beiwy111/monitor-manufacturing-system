-- 修复设备/用户中文乱码（????），请在 display_manufacturing 库执行
-- cmd /c "mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < fix_encoding_data.sql"
USE display_manufacturing;
SET NAMES utf8mb4;

-- 基础设备
UPDATE equipment SET equipment_name='1号自动贴附机', equipment_type='贴附机', workshop='贴附一车间', workstation='A线-01', manufacturer='精工自动化' WHERE equipment_code='EQ-001';
UPDATE equipment SET equipment_name='2号自动贴附机', equipment_type='贴附机', workshop='贴附二车间', workstation='A线-02', manufacturer='精工自动化' WHERE equipment_code='EQ-002';
UPDATE equipment SET equipment_name='1号组装流水线', equipment_type='组装线', workshop='组装一车间', workstation='B线-01', manufacturer='华南机械' WHERE equipment_code='EQ-003';
UPDATE equipment SET equipment_name='老化测试架A区', equipment_type='老化架', workshop='生产二部', workstation='C区-01', manufacturer='可靠性设备' WHERE equipment_code='EQ-004';
UPDATE equipment SET equipment_name='电竞调校台', equipment_type='调校台', workshop='生产二部', workstation='D区-01', manufacturer='显示科技' WHERE equipment_code='EQ-005';
UPDATE equipment SET equipment_name='自动包装线', equipment_type='包装线', workshop='生产二部', workstation='E线-01', manufacturer='华南机械' WHERE equipment_code='EQ-006';
UPDATE equipment SET equipment_name='2号组装流水线', equipment_type='组装线', workshop='组装二车间', workstation='B线-02', manufacturer='华南机械' WHERE equipment_code='EQ-007';
UPDATE equipment SET equipment_name='2号自动包装线', equipment_type='包装线', workshop='生产二部', workstation='E线-02', manufacturer='华南机械' WHERE equipment_code='EQ-008';
UPDATE equipment SET equipment_name='老化测试架B区', equipment_type='老化架', workshop='生产二部', workstation='C区-02', manufacturer='可靠性设备' WHERE equipment_code='EQ-009';
UPDATE equipment SET equipment_name='3号自动贴附机', equipment_type='贴附机', workshop='贴附二车间', workstation='A线-03', manufacturer='精工自动化' WHERE equipment_code='EQ-010';
UPDATE equipment SET equipment_name='2号电竞调校台', equipment_type='调校台', workshop='生产二部', workstation='D区-02', manufacturer='显示科技' WHERE equipment_code='EQ-011';
UPDATE equipment SET equipment_name='3号组装流水线', equipment_type='组装线', workshop='组装三车间', workstation='B线-03', manufacturer='华南机械' WHERE equipment_code='EQ-012';

-- 四道工序专用设备
UPDATE equipment SET equipment_name='1号显示屏加工线', equipment_type='显示屏线', workshop='显示屏加工一车间', workstation='F线-01', manufacturer='显示科技' WHERE equipment_code='EQ-DISP-01';
UPDATE equipment SET equipment_name='2号显示屏加工线', equipment_type='显示屏线', workshop='显示屏加工二车间', workstation='F线-02', manufacturer='显示科技' WHERE equipment_code='EQ-DISP-02';
UPDATE equipment SET equipment_name='3号显示屏加工线', equipment_type='显示屏线', workshop='显示屏加工三车间', workstation='F线-03', manufacturer='显示科技' WHERE equipment_code='EQ-DISP-03';
UPDATE equipment SET equipment_name='1号主板装配线', equipment_type='主板线', workshop='主板装配一车间', workstation='G线-01', manufacturer='华南电子' WHERE equipment_code='EQ-MB-01';
UPDATE equipment SET equipment_name='2号主板装配线', equipment_type='主板线', workshop='主板装配二车间', workstation='G线-02', manufacturer='华南电子' WHERE equipment_code='EQ-MB-02';
UPDATE equipment SET equipment_name='3号主板装配线', equipment_type='主板线', workshop='主板装配三车间', workstation='G线-03', manufacturer='华南电子' WHERE equipment_code='EQ-MB-03';

-- 操作员部门
UPDATE `user` SET department='生产一部' WHERE username IN ('zhao_operator','li_operator','zhou_operator','wang_operator');
UPDATE `user` SET department='生产二部' WHERE username IN ('sun_operator','ma_operator','wu_operator');

-- 工艺步骤设备类型乱码修复
UPDATE process_step SET standard_equipment_type='显示屏线' WHERE step_name='显示屏加工' AND (standard_equipment_type IS NULL OR standard_equipment_type LIKE '%?%');
UPDATE process_step SET standard_equipment_type='主板线' WHERE step_name='主板装配' AND (standard_equipment_type IS NULL OR standard_equipment_type LIKE '%?%');
UPDATE process_step SET standard_equipment_type='贴附机' WHERE step_name IN ('面板贴附','贴附') AND (standard_equipment_type IS NULL OR standard_equipment_type LIKE '%?%');
UPDATE process_step SET standard_equipment_type='组装线' WHERE step_name IN ('整机组装','背光组装','组装') AND (standard_equipment_type IS NULL OR standard_equipment_type LIKE '%?%');
