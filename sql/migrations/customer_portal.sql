-- 客户用户端：角色、用户、菜单、扩展字段
USE display_manufacturing;

-- 用户表扩展：关联客户企业
ALTER TABLE `user` ADD COLUMN `customer_name` varchar(150) DEFAULT NULL COMMENT '关联客户企业名称' AFTER `department`;
ALTER TABLE `user` ADD COLUMN `shipping_address` varchar(255) DEFAULT NULL COMMENT '默认收货地址' AFTER `customer_name`;

-- 售后案例扩展：客户上传图片
ALTER TABLE after_sales_case ADD COLUMN `attachment_urls` varchar(2000) DEFAULT NULL COMMENT '附件URL，逗号分隔' AFTER `problem_description`;

-- 客户角色
INSERT INTO role (role_code, role_name, role_description, status)
SELECT 'CUSTOMER', '客户用户', 'B2B客户门户：下单、查单、反馈', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'CUSTOMER');

SET @customer_role_id = (SELECT role_id FROM role WHERE role_code = 'CUSTOMER' LIMIT 1);

-- 客户门户菜单（父级 + 6 子菜单，首页由布局「首页」入口进入）
INSERT INTO sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status)
SELECT 201, 'customer', '客户门户', NULL, 1, NULL, NULL, NULL, NULL, 20, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 201);

INSERT INTO sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status)
SELECT 202, 'customer:newOrder', '新建订单', 201, 2, '/customer/portal/orders', '/customer/order/new', 'customer_order', NULL, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 202);

INSERT INTO sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status)
SELECT 203, 'customer:orders', '我的订单', 201, 2, '/customer/portal/orders', '/customer/orders', 'customer_order', NULL, 2, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 203);

INSERT INTO sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status)
SELECT 204, 'customer:products', '产品与规格', 201, 2, '/customer/portal/products', '/customer/products', 'material', NULL, 3, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 204);

INSERT INTO sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status)
SELECT 205, 'customer:feedbackSubmit', '提交反馈', 201, 2, '/customer/portal/feedbacks', '/customer/feedback/submit', 'after_sales_case', NULL, 4, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 205);

INSERT INTO sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status)
SELECT 206, 'customer:feedbackList', '我的反馈', 201, 2, '/customer/portal/feedbacks', '/customer/feedback/list', 'after_sales_case', NULL, 5, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 206);

INSERT INTO sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, api_path, route_path, business_table, icon, sort_no, status)
SELECT 207, 'customer:profile', '个人中心', 201, 2, '/customer/portal/profile', '/customer/profile', 'user', NULL, 6, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 207);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @customer_role_id, m.menu_id
FROM sys_menu m
WHERE m.menu_id BETWEEN 201 AND 207
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = @customer_role_id AND rm.menu_id = m.menu_id
  );

-- 客户测试账号（密码 123456，与系统其他账号一致）
INSERT INTO `user` (role_id, username, password_hash, real_name, phone, email, department, customer_name, shipping_address, status, created_at, updated_at)
SELECT @customer_role_id, 'huachuang', '$2b$10$KoXI6kkD6AOp0WtyKXENlek9gKs2sAMFIHjUtme1CiPaSYnEhTzRm',
       '林经理', '13900001001', 'huachuang@example.com', '深圳华创科技', '深圳华创科技有限公司',
       '深圳市南山区科技园南路88号', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'huachuang');

INSERT INTO `user` (role_id, username, password_hash, real_name, phone, email, department, customer_name, shipping_address, status, created_at, updated_at)
SELECT @customer_role_id, 'xingchen', '$2b$10$KoXI6kkD6AOp0WtyKXENlek9gKs2sAMFIHjUtme1CiPaSYnEhTzRm',
       '王总', '13900001002', 'xingchen@example.com', '北京星辰电竞', '北京星辰电竞俱乐部',
       '北京市朝阳区电竞产业园A座', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'xingchen');

-- 华创客户待确认订单（门户「待确认事项」展示）
INSERT INTO customer_order (order_no, customer_name, customer_contact, customer_phone, order_date, required_delivery_date, order_amount, audit_status, remark, created_by, created_at, updated_at)
SELECT 'CO202607010', '深圳华创科技有限公司', '林经理', '13900001001', '2026-07-08', '2026-07-28', 34000.00, 'PENDING',
       '客户门户提交：追加50台商用显示器', NULL, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM customer_order WHERE order_no = 'CO202607010');

INSERT INTO customer_order_item (order_id, material_id, product_name, specification, quantity, unit, unit_price, line_amount, delivery_date, item_status, created_at, updated_at)
SELECT o.order_id, 7, '15.6寸商用显示器', '1920x1080 商用款', 50.0000, '台', 680.0000, 34000.00, '2026-07-28', 'PENDING', NOW(), NOW()
FROM customer_order o
WHERE o.order_no = 'CO202607010'
  AND NOT EXISTS (SELECT 1 FROM customer_order_item i WHERE i.order_id = o.order_id);
