-- 售后模块菜单 v2：4 阶段工作流 + 调查工作台，移除售后角色的发货管理权限
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/migrations/aftersale_menu_v2.sql

USE display_manufacturing;

-- 调查工作台（挂在售后管理下）
INSERT INTO sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status, created_at, updated_at)
SELECT 100, 'aftersale:workbench', '调查工作台', 10, 2, NULL, '/dashboard/aftersale', NULL, NULL, 0, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 100);

UPDATE sys_menu SET
  menu_code = 'aftersale:work-order',
  menu_name = '售后工单',
  parent_id = 10,
  menu_level = 2,
  api_path = '/afterSales/afterSalesCase/list',
  route_path = '/aftersale/work-order',
  sort_no = 1,
  updated_at = NOW()
WHERE menu_id = 101;

UPDATE sys_menu SET
  menu_code = 'aftersale:plan',
  menu_name = '方案审批',
  parent_id = 10,
  menu_level = 2,
  api_path = NULL,
  route_path = '/aftersale/plan',
  sort_no = 2,
  updated_at = NOW()
WHERE menu_id = 102;

UPDATE sys_menu SET
  menu_code = 'aftersale:execution',
  menu_name = '执行协同',
  parent_id = 10,
  menu_level = 2,
  api_path = NULL,
  route_path = '/aftersale/execution',
  sort_no = 3,
  updated_at = NOW()
WHERE menu_id = 103;

UPDATE sys_menu SET
  menu_code = 'aftersale:closure',
  menu_name = '验证闭环',
  parent_id = 10,
  menu_level = 2,
  api_path = NULL,
  route_path = '/aftersale/closure',
  sort_no = 4,
  updated_at = NOW()
WHERE menu_id = 104;

-- 售后专员(role_id=7)：去掉发货管理，补上调查工作台
DELETE FROM sys_role_menu WHERE role_id = 7 AND menu_id IN (7, 71);

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 149, 7, 100
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 7 AND menu_id = 100);
