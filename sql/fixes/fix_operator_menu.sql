-- 操作员不应拥有「生产工单」「工单派工」菜单（智能派工为生产主管职能）
SET NAMES utf8mb4;

SET @operator_role_id = (SELECT role_id FROM role WHERE role_code = 'OPERATOR' LIMIT 1);

DELETE rm FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE rm.role_id = @operator_role_id
  AND m.menu_code IN ('production:workOrder', 'production:dispatch');
