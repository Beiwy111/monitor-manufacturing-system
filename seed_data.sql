-- 显示屏制造企业演示数据（按外键依赖顺序插入）
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清空已有数据
TRUNCATE TABLE cost_settlement;
TRUNCATE TABLE after_sales_case;
TRUNCATE TABLE equipment_maintenance_record;
TRUNCATE TABLE andon_alarm;
TRUNCATE TABLE nonconforming_product;
TRUNCATE TABLE quality_inspection;
TRUNCATE TABLE delivery_order;
TRUNCATE TABLE inventory_transaction;
TRUNCATE TABLE purchase_order_item;
TRUNCATE TABLE purchase_order;
TRUNCATE TABLE work_progress;
TRUNCATE TABLE work_report;
TRUNCATE TABLE dispatch_task;
TRUNCATE TABLE work_order;
TRUNCATE TABLE process_step;
TRUNCATE TABLE process_route;
TRUNCATE TABLE production_plan_item;
TRUNCATE TABLE production_plan;
TRUNCATE TABLE customer_order_item;
TRUNCATE TABLE customer_order;
TRUNCATE TABLE inventory;
TRUNCATE TABLE bom;
TRUNCATE TABLE material;
TRUNCATE TABLE equipment;
TRUNCATE TABLE operation_log;
TRUNCATE TABLE permission;
TRUNCATE TABLE `user`;
TRUNCATE TABLE role;

-- ==================== 1. 系统管理 ====================
INSERT INTO role (role_code, role_name, role_description, status) VALUES
('ADMIN', '系统管理员', '拥有全部系统权限', 1),
('ORDER', '订单管理员', '客户订单审核、跟踪与提交计划', 1),
('PLANNER', '计划员', '负责生产计划编制与下达', 1),
('MANAGER', '生产主管', '生成工单、派工与生产监控', 1),
('OPERATOR', '生产操作员', '负责现场生产操作与报工', 1),
('QC', '质检员', '负责质量检验与不合格品处理', 1),
('PURCHASER', '采购员', '负责采购订单管理', 1),
('WAREHOUSE', '仓管员', '负责物料与库存管理', 1),
('DEVICE', '设备维护人员', '设备台账、安灯与维修', 1),
('SERVICE', '售后专员', '负责售后案例处理', 1),
('COST', '财务成本人员', '工单成本与结算报表', 1);

INSERT INTO `user` (role_id, username, password_hash, real_name, employee_no, phone, email, department, status, last_login_at) VALUES
(1, 'admin', '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG', '张管理', 'EMP001', '13800001001', 'admin@display.com', '信息部', 1, '2026-07-06 08:30:00'),
(2, 'zhang_order', '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG', '张订单', 'EMP101', '13800001101', 'order@display.com', '销售部', 1, NULL),
(3, 'li_planner', '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG', '李计划', 'EMP102', '13800001102', 'planner@display.com', '计划部', 1, NULL),
(4, 'li_manager', '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG', '李主管', 'EMP103', '13800001103', 'manager@display.com', '生产部', 1, NULL),
(5, 'wang_operator', '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG', '王操作', 'EMP104', '13800001104', 'operator@display.com', '生产一部', 1, NULL),
(6, 'chen_qc', '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG', '陈质检', 'EMP105', '13800001105', 'qc@display.com', '质量部', 1, NULL),
(7, 'liu_purchase', '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG', '刘采购', 'EMP106', '13800001106', 'purchase@display.com', '采购部', 1, NULL),
(8, 'zhou_warehouse', '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG', '周仓管', 'EMP107', '13800001107', 'warehouse@display.com', '仓储部', 1, NULL),
(9, 'zhou_device', '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG', '周设备', 'EMP108', '13800001108', 'device@display.com', '设备部', 1, NULL),
(10, 'wu_service', '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG', '吴售后', 'EMP109', '13800001109', 'service@display.com', '售后部', 1, NULL),
(11, 'zheng_cost', '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG', '郑财务', 'EMP110', '13800001110', 'cost@display.com', '财务部', 1, NULL);

INSERT INTO permission (role_id, permission_code, permission_name, resource_type, resource_path, parent_id, sort_no, status) VALUES
(1, 'system', '系统管理', 'MENU', '/system', NULL, 1, 1),
(1, 'system:user', '用户管理', 'MENU', '/system/user', 1, 1, 1),
(1, 'system:role', '角色管理', 'MENU', '/system/role', 1, 2, 1),
(2, 'production', '生产管理', 'MENU', '/production', NULL, 2, 1),
(2, 'production:plan', '生产计划', 'MENU', '/production/plan', 4, 1, 1),
(3, 'production:report', '生产报工', 'MENU', '/production/report', 4, 2, 1),
(4, 'quality', '质量管理', 'MENU', '/quality', NULL, 3, 1),
(5, 'purchase', '采购管理', 'MENU', '/purchase', NULL, 4, 1),
(6, 'material', '物料库存', 'MENU', '/material', NULL, 5, 1);

INSERT INTO operation_log (user_id, module_name, operation_type, business_table, business_id, operation_content, ip_address, result_status, operated_at) VALUES
(1, '系统管理', '登录', 'user', 1, '管理员登录系统', '192.168.1.100', 'SUCCESS', '2026-07-06 08:30:00'),
(2, '生产管理', '新增', 'production_plan', 1, '创建生产计划 PP202607001', '192.168.1.101', 'SUCCESS', '2026-07-01 09:00:00'),
(3, '生产管理', '报工', 'work_report', 1, '提交报工记录 WR202607001', '192.168.1.102', 'SUCCESS', '2026-07-03 15:30:00'),
(5, '质量管理', '检验', 'quality_inspection', 1, '完成首件检验 QI202607001', '192.168.1.103', 'SUCCESS', '2026-07-03 16:00:00'),
(7, '物料库存', '入库', 'inventory_transaction', 1, '采购入库 500片LCD面板', '192.168.1.104', 'SUCCESS', '2026-06-28 10:00:00'),
(7, '物料库存', '出库', 'inventory_transaction', 3, '生产领料 200片LCD面板', '192.168.1.104', 'SUCCESS', '2026-07-02 08:00:00'),
(8, '售后管理', '登记', 'after_sales_case', 1, '登记客户投诉案例', '192.168.1.105', 'SUCCESS', '2026-07-04 11:00:00');

-- ==================== 2. 物料库存 ====================
INSERT INTO material (material_code, material_name, material_type, specification, unit, safety_stock, standard_cost, status) VALUES
('MAT-001', '15.6寸LCD面板', 'RAW', '1920x1080 IPS', '片', 500.0000, 280.0000, 1),
('MAT-002', '背光模组', 'RAW', 'LED背光 15.6寸', '套', 300.0000, 85.0000, 1),
('MAT-003', '驱动IC', 'RAW', 'TCON驱动芯片', '颗', 2000.0000, 12.5000, 1),
('MAT-004', '铝合金边框', 'RAW', 'CNC精加工边框', '套', 200.0000, 45.0000, 1),
('MAT-005', 'PCB主板', 'RAW', '主控电路板', '块', 400.0000, 65.0000, 1),
('MAT-006', '电源适配器', 'RAW', '19V 3.42A', '个', 300.0000, 35.0000, 1),
('PRD-001', '15.6寸商用显示器', 'FINISHED', '1920x1080 商用款', '台', 50.0000, 680.0000, 1),
('PRD-002', '23.8寸电竞显示器', 'FINISHED', '2560x1440 144Hz', '台', 30.0000, 1280.0000, 1),
('PRD-003', '27寸4K显示器', 'FINISHED', '3840x2160 HDR', '台', 20.0000, 2100.0000, 1);

INSERT INTO bom (parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, expire_date, status, remark) VALUES
(7, 1, 1.0000, 0.0200, 'V1.0', '2026-01-01', NULL, 1, '15.6寸显示器BOM'),
(7, 2, 1.0000, 0.0100, 'V1.0', '2026-01-01', NULL, 1, NULL),
(7, 3, 2.0000, 0.0050, 'V1.0', '2026-01-01', NULL, 1, '每块面板需2颗驱动IC'),
(7, 4, 1.0000, 0.0100, 'V1.0', '2026-01-01', NULL, 1, NULL),
(7, 5, 1.0000, 0.0200, 'V1.0', '2026-01-01', NULL, 1, NULL),
(7, 6, 1.0000, 0.0000, 'V1.0', '2026-01-01', NULL, 1, NULL),
(8, 1, 1.0000, 0.0200, 'V1.0', '2026-01-01', NULL, 1, '23.8寸电竞显示器BOM'),
(8, 2, 1.0000, 0.0100, 'V1.0', '2026-01-01', NULL, 1, NULL),
(8, 3, 3.0000, 0.0050, 'V1.0', '2026-01-01', NULL, 1, NULL);

INSERT INTO inventory (material_id, warehouse_code, warehouse_name, location_code, batch_no, quantity_on_hand, quantity_reserved, quantity_available, inventory_status, last_transaction_at) VALUES
(1, 'WH-01', '原材料仓', 'A-01-01', 'BATCH-LCD-202606', 1200.0000, 200.0000, 1000.0000, 'NORMAL', '2026-07-02 08:00:00'),
(2, 'WH-01', '原材料仓', 'A-02-01', 'BATCH-BL-202606', 800.0000, 100.0000, 700.0000, 'NORMAL', '2026-07-02 08:00:00'),
(3, 'WH-01', '原材料仓', 'A-03-01', 'BATCH-IC-202605', 5000.0000, 400.0000, 4600.0000, 'NORMAL', '2026-07-02 08:00:00'),
(4, 'WH-01', '原材料仓', 'A-04-01', 'BATCH-FR-202606', 600.0000, 50.0000, 550.0000, 'NORMAL', '2026-06-28 10:00:00'),
(5, 'WH-01', '原材料仓', 'A-05-01', 'BATCH-PCB-202606', 900.0000, 100.0000, 800.0000, 'NORMAL', '2026-07-02 08:00:00'),
(6, 'WH-01', '原材料仓', 'A-06-01', 'BATCH-PS-202605', 700.0000, 0.0000, 700.0000, 'NORMAL', '2026-06-28 10:00:00'),
(7, 'WH-02', '成品仓', 'B-01-01', 'BATCH-PRD-202607', 85.0000, 30.0000, 55.0000, 'NORMAL', '2026-07-05 16:00:00'),
(8, 'WH-02', '成品仓', 'B-02-01', 'BATCH-PRD-202607', 42.0000, 10.0000, 32.0000, 'NORMAL', '2026-07-04 14:00:00'),
(9, 'WH-02', '成品仓', 'B-03-01', 'BATCH-PRD-202606', 18.0000, 5.0000, 13.0000, 'NORMAL', '2026-06-30 11:00:00');

-- ==================== 3. 订单发货 ====================
INSERT INTO customer_order (order_no, customer_name, customer_contact, customer_phone, order_date, required_delivery_date, order_amount, audit_status, audit_user_id, audit_at, audit_opinion, remark, created_by) VALUES
('CO202607001', '深圳华创科技有限公司', '林经理', '13900001001', '2026-07-01', '2026-07-20', 136000.00, 'APPROVED', 1, '2026-07-01 14:00:00', '订单审核通过', '商用显示器批量订单', 2),
('CO202607002', '北京星辰电竞俱乐部', '王总', '13900001002', '2026-07-02', '2026-07-25', 192000.00, 'APPROVED', 1, '2026-07-02 10:30:00', '同意排产', '电竞显示器订单', 2),
('CO202607003', '上海视觉设计工作室', '陈设计师', '13900001003', '2026-07-03', '2026-08-05', 84000.00, 'APPROVED', 1, '2026-07-03 09:00:00', '4K显示器小批量', NULL, 2),
('CO202607004', '广州教育设备有限公司', '黄主任', '13900001004', '2026-07-04', '2026-08-10', 68000.00, 'PENDING', NULL, NULL, NULL, '教育行业采购', 2),
('CO202607005', '杭州电商运营中心', '张运营', '13900001005', '2026-07-05', '2026-07-30', 54400.00, 'APPROVED', 1, '2026-07-05 11:00:00', '加急订单', '电商渠道', 2);

INSERT INTO customer_order_item (order_id, material_id, product_name, specification, quantity, unit, unit_price, line_amount, delivery_date, item_status) VALUES
(1, 7, '15.6寸商用显示器', '1920x1080 商用款', 200.0000, '台', 680.0000, 136000.00, '2026-07-20', 'PRODUCING'),
(2, 8, '23.8寸电竞显示器', '2560x1440 144Hz', 150.0000, '台', 1280.0000, 192000.00, '2026-07-25', 'PRODUCING'),
(3, 9, '27寸4K显示器', '3840x2160 HDR', 40.0000, '台', 2100.0000, 84000.00, '2026-08-05', 'PENDING'),
(4, 7, '15.6寸商用显示器', '1920x1080 教育版', 100.0000, '台', 680.0000, 68000.00, '2026-08-10', 'PENDING'),
(5, 7, '15.6寸商用显示器', '1920x1080 商用款', 80.0000, '台', 680.0000, 54400.00, '2026-07-30', 'PRODUCING');

-- ==================== 4. 生产管理 ====================
INSERT INTO production_plan (plan_no, plan_name, source_order_id, planned_start_date, planned_end_date, priority, plan_status, planner_id, approved_by, approved_at, remark) VALUES
('PP202607001', '华创科技200台商用显示器生产计划', 1, '2026-07-05', '2026-07-18', 'HIGH', 'RUNNING', 2, 1, '2026-07-02 15:00:00', '关联订单CO202607001'),
('PP202607002', '星辰电竞150台电竞显示器生产计划', 2, '2026-07-06', '2026-07-22', 'HIGH', 'RUNNING', 2, 1, '2026-07-03 10:00:00', '关联订单CO202607002'),
('PP202607003', '电商80台商用显示器补货计划', 5, '2026-07-08', '2026-07-25', 'NORMAL', 'RELEASED', 2, 1, '2026-07-06 09:00:00', NULL),
('PP202607004', '4K显示器小批量试产计划', 3, '2026-07-15', '2026-08-01', 'NORMAL', 'DRAFT', 2, NULL, NULL, '待物料齐套');

INSERT INTO production_plan_item (plan_id, order_item_id, material_id, planned_quantity, completed_quantity, planned_start_date, planned_end_date, item_status) VALUES
(1, 1, 7, 200.0000, 85.0000, '2026-07-05', '2026-07-18', 'RUNNING'),
(2, 2, 8, 150.0000, 42.0000, '2026-07-06', '2026-07-22', 'RUNNING'),
(3, 5, 7, 80.0000, 0.0000, '2026-07-08', '2026-07-25', 'PENDING'),
(4, 3, 9, 40.0000, 0.0000, '2026-07-15', '2026-08-01', 'PENDING');

INSERT INTO process_route (material_id, route_code, route_name, version_no, status, created_by) VALUES
(7, 'RT-PRD001-V1', '15.6寸商用显示器工艺路线', 'V1.0', 1, 2),
(8, 'RT-PRD002-V1', '23.8寸电竞显示器工艺路线', 'V1.0', 1, 2),
(9, 'RT-PRD003-V1', '27寸4K显示器工艺路线', 'V1.0', 1, 2);

INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status) VALUES
(1, 10, 'STEP-01', '显示屏加工', 0.35, '显示屏线', 0, 1),
(1, 20, 'STEP-02', '主板装配', 0.45, '主板线', 0, 1),
(1, 30, 'STEP-03', '面板贴附', 0.50, '贴附机', 1, 1),
(1, 40, 'STEP-04', '整机组装', 0.80, '组装线', 0, 1),
(1, 50, 'STEP-05', '整机老化测试', 4.00, '老化架', 1, 1),
(1, 60, 'STEP-06', '外观检验包装', 0.30, '包装线', 1, 1),
(2, 10, 'STEP-01', '显示屏加工', 0.40, '显示屏线', 0, 1),
(2, 20, 'STEP-02', '主板装配', 0.50, '主板线', 0, 1),
(2, 30, 'STEP-03', '面板贴附', 0.60, '贴附机', 1, 1),
(2, 40, 'STEP-04', '整机组装', 1.00, '组装线', 0, 1),
(2, 50, 'STEP-05', '整机老化测试', 6.00, '老化架', 1, 1),
(2, 60, 'STEP-06', '电竞调校', 1.50, '调校台', 1, 1),
(2, 70, 'STEP-07', '外观检验包装', 0.40, '包装线', 1, 1);

INSERT INTO equipment (equipment_code, equipment_name, equipment_type, workshop, workstation, manufacturer, model, purchase_date, status, last_maintenance_at) VALUES
('EQ-DISP-01', '1号显示屏加工线', '显示屏线', '显示屏加工一车间', 'F线-01', '显示科技', 'XS-DP100', '2024-05-10', 'RUNNING', '2026-06-18 10:00:00'),
('EQ-DISP-02', '2号显示屏加工线', '显示屏线', '显示屏加工二车间', 'F线-02', '显示科技', 'XS-DP100', '2024-06-01', 'RUNNING', '2026-06-18 10:00:00'),
('EQ-DISP-03', '3号显示屏加工线', '显示屏线', '显示屏加工三车间', 'F线-03', '显示科技', 'XS-DP100', '2024-06-01', 'IDLE', '2026-06-18 10:00:00'),
('EQ-MB-01', '1号主板装配线', '主板线', '主板装配一车间', 'G线-01', '华南电子', 'HN-MB80', '2024-05-10', 'RUNNING', '2026-06-18 10:00:00'),
('EQ-MB-02', '2号主板装配线', '主板线', '主板装配二车间', 'G线-02', '华南电子', 'HN-MB80', '2024-06-01', 'RUNNING', '2026-06-18 10:00:00'),
('EQ-MB-03', '3号主板装配线', '主板线', '主板装配三车间', 'G线-03', '华南电子', 'HN-MB80', '2024-06-01', 'IDLE', '2026-06-18 10:00:00'),
('EQ-001', '1号自动贴附机', '贴附机', '贴附一车间', 'A线-01', '精工自动化', 'JF-TF200', '2024-03-15', 'RUNNING', '2026-06-15 10:00:00'),
('EQ-002', '2号自动贴附机', '贴附机', '贴附二车间', 'A线-02', '精工自动化', 'JF-TF200', '2024-03-15', 'IDLE', '2026-06-15 10:00:00'),
('EQ-003', '1号组装流水线', '组装线', '组装一车间', 'B线-01', '华南机械', 'HN-ZX100', '2023-08-20', 'RUNNING', '2026-05-20 14:00:00'),
('EQ-007', '2号组装流水线', '组装线', '组装二车间', 'B线-02', '华南机械', 'HN-ZX100', '2024-08-01', 'RUNNING', '2026-05-20 14:00:00'),
('EQ-012', '3号组装流水线', '组装线', '组装三车间', 'B线-03', '华南机械', 'HN-ZX100', '2025-01-01', 'IDLE', '2026-05-20 14:00:00'),
('EQ-004', '老化测试架A区', '老化架', '生产二部', 'C区-01', '可靠性设备', 'KK-LH50', '2023-11-10', 'RUNNING', '2026-06-01 09:00:00'),
('EQ-005', '电竞调校台', '调校台', '生产二部', 'D区-01', '显示科技', 'XS-TJ300', '2025-01-08', 'IDLE', '2026-06-20 16:00:00'),
('EQ-006', '自动包装线', '包装线', '生产二部', 'E线-01', '华南机械', 'HN-BZ80', '2024-06-01', 'RUNNING', '2026-06-10 11:00:00');

INSERT INTO work_order (work_order_no, plan_id, plan_item_id, material_id, route_id, planned_quantity, completed_quantity, qualified_quantity, unqualified_quantity, planned_start_time, planned_end_time, actual_start_time, actual_end_time, status, created_by, released_by, released_at, remark) VALUES
('WO202607001', 1, 1, 7, 1, 200.0000, 85.0000, 82.0000, 3.0000, '2026-07-05 08:00:00', '2026-07-18 18:00:00', '2026-07-05 08:30:00', NULL, 'RUNNING', 2, 1, '2026-07-04 17:00:00', '华创订单工单'),
('WO202607002', 2, 2, 8, 2, 150.0000, 42.0000, 40.0000, 2.0000, '2026-07-06 08:00:00', '2026-07-22 18:00:00', '2026-07-06 09:00:00', NULL, 'RUNNING', 2, 1, '2026-07-05 16:00:00', '电竞显示器工单'),
('WO202607003', 3, 3, 7, 1, 80.0000, 0.0000, 0.0000, 0.0000, '2026-07-08 08:00:00', '2026-07-25 18:00:00', NULL, NULL, 'RELEASED', 2, 1, '2026-07-06 10:00:00', '电商补货工单');

INSERT INTO dispatch_task (dispatch_no, work_order_id, step_id, operator_id, equipment_id, assigned_quantity, accepted_quantity, completed_quantity, assigned_by, assigned_at, accepted_at, status, remark) VALUES
('DT202607001', 1, 1, 3, 1, 50.0000, 50.0000, 50.0000, 2, '2026-07-05 08:00:00', '2026-07-05 08:15:00', 'COMPLETED', '面板贴附批次1'),
('DT202607002', 1, 2, 3, 3, 50.0000, 50.0000, 48.0000, 2, '2026-07-05 14:00:00', '2026-07-05 14:10:00', 'RUNNING', '背光组装批次1'),
('DT202607003', 1, 3, 4, 4, 48.0000, 48.0000, 45.0000, 2, '2026-07-06 08:00:00', '2026-07-06 08:20:00', 'RUNNING', '老化测试批次1'),
('DT202607004', 2, 5, 3, 1, 40.0000, 40.0000, 40.0000, 2, '2026-07-06 09:00:00', '2026-07-06 09:10:00', 'COMPLETED', '电竞面板贴附'),
('DT202607005', 2, 6, 4, 3, 40.0000, 40.0000, 38.0000, 2, '2026-07-06 14:00:00', '2026-07-06 14:05:00', 'RUNNING', '电竞背光组装'),
('DT202607006', 1, 4, 4, 6, 30.0000, 30.0000, 28.0000, 2, '2026-07-06 16:00:00', '2026-07-06 16:10:00', 'RUNNING', '包装批次1');

INSERT INTO work_report (report_no, work_order_id, dispatch_id, step_id, operator_id, equipment_id, report_date, start_time, end_time, completed_quantity, qualified_quantity, unqualified_quantity, work_hours, report_status, confirmed_by, confirmed_at, remark) VALUES
('WR202607001', 1, 1, 1, 3, 1, '2026-07-05', '2026-07-05 08:15:00', '2026-07-05 12:00:00', 50.0000, 49.0000, 1.0000, 3.75, 'CONFIRMED', 2, '2026-07-05 17:00:00', '贴附完成'),
('WR202607002', 1, 2, 2, 3, 3, '2026-07-05', '2026-07-05 14:10:00', '2026-07-05 18:00:00', 48.0000, 47.0000, 1.0000, 3.83, 'CONFIRMED', 2, '2026-07-05 18:30:00', NULL),
('WR202607003', 1, 3, 3, 4, 4, '2026-07-06', '2026-07-06 08:20:00', '2026-07-06 16:00:00', 45.0000, 44.0000, 1.0000, 7.67, 'SUBMITTED', NULL, NULL, '老化进行中'),
('WR202607004', 2, 4, 5, 3, 1, '2026-07-06', '2026-07-06 09:10:00', '2026-07-06 13:00:00', 40.0000, 39.0000, 1.0000, 3.83, 'CONFIRMED', 2, '2026-07-06 14:00:00', NULL),
('WR202607005', 1, 6, 4, 4, 6, '2026-07-06', '2026-07-06 16:10:00', '2026-07-06 18:00:00', 28.0000, 27.0000, 1.0000, 1.83, 'SUBMITTED', NULL, NULL, '包装报工');

INSERT INTO work_progress (work_order_id, dispatch_id, progress_date, progress_percent, completed_quantity, current_status, progress_description, recorded_by, recorded_at) VALUES
(1, 2, '2026-07-05', 25.00, 50.0000, 'RUNNING', '完成面板贴附50台', 2, '2026-07-05 18:00:00'),
(1, 3, '2026-07-06', 42.50, 85.0000, 'RUNNING', '老化测试进行中，累计完成85台', 2, '2026-07-06 17:00:00'),
(2, 5, '2026-07-06', 28.00, 42.0000, 'RUNNING', '电竞显示器背光组装中', 2, '2026-07-06 17:30:00'),
(1, 6, '2026-07-06', 42.50, 85.0000, 'RUNNING', '部分产品已进入包装环节', 2, '2026-07-06 18:00:00');

-- ==================== 5. 采购管理 ====================
INSERT INTO purchase_order (purchase_order_no, supplier_name, supplier_contact, supplier_phone, purchase_date, expected_arrival_date, total_amount, status, purchaser_id, approved_by, approved_at, remark) VALUES
('PO202606001', '深圳光电材料有限公司', '孙经理', '13700001001', '2026-06-20', '2026-06-28', 168000.00, 'RECEIVED', 6, 1, '2026-06-21 10:00:00', 'LCD面板采购'),
('PO202606002', '东莞背光科技股份', '钱工', '13700001002', '2026-06-22', '2026-06-30', 51000.00, 'RECEIVED', 6, 1, '2026-06-23 09:00:00', '背光模组采购'),
('PO202607001', '苏州芯片代理商', '郑销售', '13700001003', '2026-07-01', '2026-07-10', 37500.00, 'APPROVED', 6, 1, '2026-07-02 11:00:00', '驱动IC补货'),
('PO202607002', '惠州电路板厂', '冯厂长', '13700001004', '2026-07-03', '2026-07-12', 32500.00, 'DRAFT', NULL, NULL, NULL, 'PCB主板采购');

INSERT INTO purchase_order_item (purchase_order_id, material_id, quantity, received_quantity, unit, unit_price, line_amount, item_status) VALUES
(1, 1, 600.0000, 600.0000, '片', 280.0000, 168000.00, 'RECEIVED'),
(2, 2, 600.0000, 600.0000, '套', 85.0000, 51000.00, 'RECEIVED'),
(3, 3, 3000.0000, 0.0000, '颗', 12.5000, 37500.00, 'PENDING'),
(4, 5, 500.0000, 0.0000, '块', 65.0000, 32500.00, 'PENDING');

INSERT INTO inventory_transaction (transaction_no, inventory_id, material_id, transaction_type, quantity, warehouse_code, location_code, batch_no, related_purchase_order_id, related_work_order_id, handled_by, handled_at, remark) VALUES
('IT202606001', 1, 1, 'PURCHASE_IN', 600.0000, 'WH-01', 'A-01-01', 'BATCH-LCD-202606', 1, NULL, 7, '2026-06-28 10:00:00', 'LCD面板采购入库'),
('IT202606002', 2, 2, 'PURCHASE_IN', 600.0000, 'WH-01', 'A-02-01', 'BATCH-BL-202606', 2, NULL, 7, '2026-06-30 14:00:00', '背光模组采购入库'),
('IT202607001', 1, 1, 'MATERIAL_OUT', 200.0000, 'WH-01', 'A-01-01', 'BATCH-LCD-202606', NULL, 1, 7, '2026-07-02 08:00:00', '工单WO202607001领料'),
('IT202607002', 2, 2, 'MATERIAL_OUT', 100.0000, 'WH-01', 'A-02-01', 'BATCH-BL-202606', NULL, 1, 7, '2026-07-02 08:30:00', '工单WO202607001领料'),
('IT202607003', 7, 7, 'PRODUCT_IN', 55.0000, 'WH-02', 'B-01-01', 'BATCH-PRD-202607', NULL, 1, 7, '2026-07-05 16:00:00', '成品入库'),
('IT202607004', 8, 8, 'PRODUCT_IN', 32.0000, 'WH-02', 'B-02-01', 'BATCH-PRD-202607', NULL, 2, 7, '2026-07-04 14:00:00', '电竞成品入库');

-- ==================== 6. 发货 ====================
INSERT INTO delivery_order (delivery_no, order_id, work_order_id, customer_name, material_id, batch_no, delivery_quantity, delivery_date, logistics_company, logistics_no, receiver_name, receiver_phone, receiver_address, delivery_status, shipped_by, remark) VALUES
('DO202607001', 1, 1, '深圳华创科技有限公司', 7, 'BATCH-PRD-202607', 50.0000, '2026-07-10', '顺丰速运', 'SF1234567890', '林经理', '13900001001', '深圳市南山区科技园南路88号', 'SHIPPED', 7, '首批发货50台'),
('DO202607002', 2, 2, '北京星辰电竞俱乐部', 8, 'BATCH-PRD-202607', 30.0000, '2026-07-12', '德邦物流', 'DB9876543210', '王总', '13900001002', '北京市朝阳区电竞产业园A座', 'SHIPPED', 7, '首批发货30台'),
('DO202607003', 1, 1, '深圳华创科技有限公司', 7, 'BATCH-PRD-202607', 32.0000, '2026-07-15', '顺丰速运', 'SF1234567891', '林经理', '13900001001', '深圳市南山区科技园南路88号', 'PREPARED', 7, '第二批备货中'),
('DO202607004', 5, 1, '杭州电商运营中心', 7, 'BATCH-PRD-202607', 20.0000, '2026-07-20', NULL, NULL, '张运营', '13900001005', '杭州市余杭区电商园B区', 'PREPARED', NULL, '待发货');

-- ==================== 7. 质量管理 ====================
INSERT INTO quality_inspection (inspection_no, work_order_id, work_report_id, material_id, batch_no, inspection_type, sample_quantity, qualified_quantity, unqualified_quantity, inspection_result, inspector_id, inspected_at, remark) VALUES
('QI202607001', 1, 1, 7, 'BATCH-PRD-202607', 'PROCESS', 5.0000, 5.0000, 0.0000, 'QUALIFIED', 5, '2026-07-05 12:30:00', '贴附工序首件合格'),
('QI202607002', 1, 2, 7, 'BATCH-PRD-202607', 'PROCESS', 10.0000, 9.0000, 1.0000, 'QUALIFIED', 5, '2026-07-05 18:30:00', '背光组装抽检'),
('QI202607003', 1, 3, 7, 'BATCH-PRD-202607', 'PROCESS', 10.0000, 9.0000, 1.0000, 'QUALIFIED', 5, '2026-07-06 16:30:00', '老化后抽检'),
('QI202607004', 2, 4, 8, 'BATCH-PRD-202607', 'PROCESS', 3.0000, 3.0000, 0.0000, 'QUALIFIED', 5, '2026-07-06 13:30:00', '电竞显示器首件'),
('QI202607005', 1, 5, 7, 'BATCH-PRD-202607', 'FINAL', 8.0000, 7.0000, 1.0000, 'PENDING', 5, '2026-07-06 18:30:00', '包装后待确认');

INSERT INTO nonconforming_product (nonconforming_no, inspection_id, work_order_id, work_report_id, material_id, batch_no, defect_type, defect_description, quantity, severity, handle_method, handle_status, registered_by, registered_at, handled_by, handled_at, remark) VALUES
('NC202607001', 2, 1, 2, 7, 'BATCH-PRD-202607', '亮点不良', '屏幕右上角有1个亮点', 1.0000, 'MINOR', 'REWORK', 'COMPLETED', 5, '2026-07-05 19:00:00', 3, '2026-07-06 10:00:00', '返工后复检合格'),
('NC202607002', 3, 1, 3, 7, 'BATCH-PRD-202607', '色差', '色温偏差超出标准范围', 1.0000, 'GENERAL', 'REWORK', 'PROCESSING', 5, '2026-07-06 17:00:00', NULL, NULL, '调校中'),
('NC202607003', 5, 1, 5, 7, 'BATCH-PRD-202607', '外观划伤', '边框有轻微划伤', 1.0000, 'MINOR', 'PENDING', 'PENDING', 5, '2026-07-06 19:00:00', NULL, NULL, '待处理');

-- ==================== 8. 设备管理 ====================
INSERT INTO andon_alarm (alarm_no, work_order_id, dispatch_id, equipment_id, alarm_type, alarm_level, alarm_description, alarm_status, reported_by, reported_at, received_by, received_at, closed_by, closed_at, close_result) VALUES
('AL202607001', 1, 2, 3, 'EQUIPMENT', 'URGENT', '组装流水线传送带卡顿', 'CLOSED', 3, '2026-07-05 15:30:00', 1, '2026-07-05 15:35:00', 1, '2026-07-05 16:00:00', '更换传送带轴承后恢复正常'),
('AL202607002', 2, 5, 3, 'QUALITY', 'GENERAL', '背光亮度不均匀', 'OPEN', 4, '2026-07-06 15:00:00', 5, '2026-07-06 15:10:00', NULL, NULL, NULL),
('AL202607003', 1, 3, 4, 'MATERIAL', 'GENERAL', '老化架测试治具不足', 'RECEIVED', 4, '2026-07-06 09:00:00', 2, '2026-07-06 09:15:00', NULL, NULL, NULL);

INSERT INTO equipment_maintenance_record (maintenance_no, equipment_id, alarm_id, maintenance_type, fault_description, maintenance_content, start_time, end_time, downtime_minutes, maintenance_result, maintainer_id, cost_amount, remark) VALUES
('MR202607001', 3, 1, 'REPAIR', '传送带卡顿', '更换传送带轴承，润滑传动部件', '2026-07-05 15:40:00', '2026-07-05 16:00:00', 30, 'COMPLETED', 3, 350.00, '关联安灯报警AL202607001'),
('MR202606001', 1, NULL, 'MAINTENANCE', NULL, '贴附机月度保养：清洁、校准、润滑', '2026-06-15 08:00:00', '2026-06-15 10:00:00', 120, 'COMPLETED', 3, 0.00, '计划保养'),
('MR202606002', 4, NULL, 'MAINTENANCE', NULL, '老化架季度保养：检查线路和温控', '2026-06-01 09:00:00', '2026-06-01 11:00:00', 120, 'COMPLETED', 4, 0.00, '计划保养');

-- ==================== 9. 售后成本 ====================
INSERT INTO after_sales_case (case_no, order_id, delivery_id, material_id, batch_no, customer_name, contact_name, contact_phone, problem_description, problem_type, case_level, case_status, trace_result, handle_result, service_user_id, opened_at, closed_at) VALUES
('AS202607001', 1, 1, 7, 'BATCH-PRD-202607', '深圳华创科技有限公司', '林经理', '13900001001', '收到显示器后发现有2台屏幕闪烁', '显示异常', 'GENERAL', 'PROCESSING', '追溯至老化测试环节温控波动', '安排技术人员上门检测，更换主板', 8, '2026-07-04 11:00:00', NULL),
('AS202607002', 2, 2, 8, 'BATCH-PRD-202607', '北京星辰电竞俱乐部', '王总', '13900001002', '1台显示器刷新率无法达到144Hz', '性能问题', 'URGENT', 'OPEN', NULL, NULL, 8, '2026-07-05 16:00:00', NULL),
('AS202607003', 1, 1, 7, 'BATCH-PRD-202607', '深圳华创科技有限公司', '林经理', '13900001001', '咨询批量采购延保服务', '咨询服务', 'GENERAL', 'CLOSED', '确认为正常商务咨询', '已提供延保方案报价', 8, '2026-06-28 10:00:00', '2026-06-28 15:00:00');

INSERT INTO cost_settlement (settlement_no, work_order_id, order_id, material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost, settlement_period, settlement_status, confirmed_by, confirmed_at, exported_at, remark) VALUES
('CS202607001', 1, 1, 95200.00, 18500.00, 8200.00, 3500.00, 1200.00, 126600.00, '2026-07', 'CONFIRMED', 1, '2026-07-06 10:00:00', NULL, '华创订单工单成本结算（进行中）'),
('CS202607002', 2, 2, 86400.00, 15200.00, 6800.00, 2800.00, 800.00, 112000.00, '2026-07', 'DRAFT', NULL, NULL, NULL, '电竞显示器工单成本预估');

SET FOREIGN_KEY_CHECKS = 1;
