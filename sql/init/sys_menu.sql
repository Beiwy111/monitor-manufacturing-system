-- 系统菜单表：整合8个业务模块、28张业务表
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    menu_code       VARCHAR(50)  NOT NULL COMMENT '菜单编码',
    menu_name       VARCHAR(100) NOT NULL COMMENT '菜单名称',
    parent_id       BIGINT UNSIGNED DEFAULT NULL COMMENT '父菜单ID',
    menu_level      TINYINT NOT NULL DEFAULT 1 COMMENT '层级：1模块 2功能页',
    api_path        VARCHAR(255) DEFAULT NULL COMMENT '列表接口路径',
    business_table  VARCHAR(100) DEFAULT NULL COMMENT '对应业务表名',
    icon            VARCHAR(50)  DEFAULT NULL COMMENT '图标',
    sort_no         INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (menu_id),
    UNIQUE KEY uk_menu_code (menu_code),
    KEY idx_menu_parent_id (parent_id),
    KEY idx_menu_status (status),
    CONSTRAINT fk_menu_parent FOREIGN KEY (parent_id) REFERENCES sys_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- 清空后重新插入（若表已存在且有数据）
DELETE FROM sys_menu;

-- 一级菜单：8个业务模块
INSERT INTO sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, api_path, business_table, icon, sort_no, status) VALUES
(1,  'system',     '系统管理', NULL, 1, NULL, NULL, 'setting', 1, 1),
(2,  'material',   '物料库存', NULL, 1, NULL, NULL, 'box', 2, 1),
(3,  'order',      '订单发货', NULL, 1, NULL, NULL, 'order', 3, 1),
(4,  'production', '生产管理', NULL, 1, NULL, NULL, 'factory', 4, 1),
(5,  'purchase',   '采购管理', NULL, 1, NULL, NULL, 'shopping', 5, 1),
(6,  'quality',    '质量管理', NULL, 1, NULL, NULL, 'check', 6, 1),
(7,  'equipment',  '设备管理', NULL, 1, NULL, NULL, 'tool', 7, 1),
(8,  'afterSales', '售后成本', NULL, 1, NULL, NULL, 'service', 8, 1);

-- 二级菜单：系统管理（4张表）
INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_level, api_path, business_table, sort_no, status) VALUES
('system:role',         '角色管理', 1, 2, '/system/role/list',         'role', 1, 1),
('system:user',         '用户管理', 1, 2, '/system/user/list',         'user', 2, 1),
('system:permission',   '权限管理', 1, 2, '/system/permission/list',   'permission', 3, 1),
('system:operationLog', '操作日志', 1, 2, '/system/operationLog/list', 'operation_log', 4, 1);

-- 二级菜单：物料库存（4张表）
INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_level, api_path, business_table, sort_no, status) VALUES
('material:material',    '物料管理', 2, 2, '/material/material/list',    'material', 1, 1),
('material:bom',         'BOM管理',  2, 2, '/material/bom/list',         'bom', 2, 1),
('material:inventory',   '库存管理', 2, 2, '/material/inventory/list',   'inventory', 3, 1),
('material:transaction', '库存流水', 2, 2, '/material/transaction/list', 'inventory_transaction', 4, 1);

-- 二级菜单：订单发货（3张表）
INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_level, api_path, business_table, sort_no, status) VALUES
('order:customerOrder', '客户订单', 3, 2, '/order/customerOrder/list', 'customer_order', 1, 1),
('order:orderItem',     '订单明细', 3, 2, '/order/orderItem/list',     'customer_order_item', 2, 1),
('order:delivery',      '发货管理', 3, 2, '/order/delivery/list',      'delivery_order', 3, 1);

-- 二级菜单：生产管理（8张表）
INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_level, api_path, business_table, sort_no, status) VALUES
('production:plan',      '生产计划', 4, 2, '/production/plan/list',      'production_plan', 1, 1),
('production:planItem',  '计划明细', 4, 2, '/production/planItem/list',  'production_plan_item', 2, 1),
('production:route',     '工艺路线', 4, 2, '/production/route/list',     'process_route', 3, 1),
('production:step',      '工序管理', 4, 2, '/production/step/list',      'process_step', 4, 1),
('production:workOrder', '工单管理', 4, 2, '/production/workOrder/list', 'work_order', 5, 1),
('production:dispatch',  '派工任务', 4, 2, '/production/dispatch/list',  'dispatch_task', 6, 1),
('production:report',    '报工管理', 4, 2, '/production/report/list',    'work_report', 7, 1),
('production:progress',  '生产进度', 4, 2, '/production/progress/list',  'work_progress', 8, 1);

-- 二级菜单：采购管理（2张表）
INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_level, api_path, business_table, sort_no, status) VALUES
('purchase:purchaseOrder',     '采购订单', 5, 2, '/purchase/purchaseOrder/list',     'purchase_order', 1, 1),
('purchase:purchaseOrderItem', '采购明细', 5, 2, '/purchase/purchaseOrderItem/list', 'purchase_order_item', 2, 1);

-- 二级菜单：质量管理（2张表）
INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_level, api_path, business_table, sort_no, status) VALUES
('quality:inspection',    '质量检验', 6, 2, '/quality/inspection/list',    'quality_inspection', 1, 1),
('quality:nonconforming', '不合格品', 6, 2, '/quality/nonconforming/list', 'nonconforming_product', 2, 1);

-- 二级菜单：设备管理（3张表）
INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_level, api_path, business_table, sort_no, status) VALUES
('equipment:equipment',   '设备台账', 7, 2, '/equipment/equipment/list',   'equipment', 1, 1),
('equipment:alarm',       '安灯报警', 7, 2, '/equipment/alarm/list',       'andon_alarm', 2, 1),
('equipment:maintenance', '设备维护', 7, 2, '/equipment/maintenance/list', 'equipment_maintenance_record', 3, 1);

-- 二级菜单：售后成本（2张表）
INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_level, api_path, business_table, sort_no, status) VALUES
('afterSales:afterSalesCase', '售后案例', 8, 2, '/afterSales/afterSalesCase/list', 'after_sales_case', 1, 1),
('afterSales:settlement',     '成本结算', 8, 2, '/afterSales/settlement/list',     'cost_settlement', 2, 1);

SET FOREIGN_KEY_CHECKS = 1;
