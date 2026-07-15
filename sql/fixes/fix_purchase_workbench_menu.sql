-- 采购员侧栏：采购工作台（物料需求总览 / 一键生成采购单）
SET NAMES utf8mb4;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_level, route_path, sort_no, status)
SELECT 'purchase:workbench', '采购工作台', p.menu_id, 2, '/purchase/workbench', 0, 1
FROM sys_menu p
WHERE p.menu_code = 'purchase'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'purchase:workbench');

SET @purchaser_role_id = (SELECT role_id FROM role WHERE role_code = 'PURCHASER' LIMIT 1);
SET @workbench_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_code = 'purchase:workbench' LIMIT 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @purchaser_role_id, @workbench_menu_id
WHERE @purchaser_role_id IS NOT NULL
  AND @workbench_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu
    WHERE role_id = @purchaser_role_id AND menu_id = @workbench_menu_id
  );
