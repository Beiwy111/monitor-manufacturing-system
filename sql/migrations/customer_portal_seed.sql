USE display_manufacturing;

INSERT IGNORE INTO `user` (role_id, username, password_hash, real_name, phone, email, department, customer_name, shipping_address, status, created_at, updated_at)
VALUES (17, 'huachuang', '$2b$10$KoXI6kkD6AOp0WtyKXENlek9gKs2sAMFIHjUtme1CiPaSYnEhTzRm',
        '林经理', '13900001001', 'huachuang@example.com', '深圳华创科技', '深圳华创科技有限公司',
        '深圳市南山区科技园南路88号', 1, NOW(), NOW());

INSERT IGNORE INTO `user` (role_id, username, password_hash, real_name, phone, email, department, customer_name, shipping_address, status, created_at, updated_at)
VALUES (17, 'xingchen', '$2b$10$KoXI6kkD6AOp0WtyKXENlek9gKs2sAMFIHjUtme1CiPaSYnEhTzRm',
        '王总', '13900001002', 'xingchen@example.com', '北京星辰电竞', '北京星辰电竞俱乐部',
        '北京市朝阳区电竞产业园A座', 1, NOW(), NOW());

INSERT INTO customer_order (order_no, customer_name, customer_contact, customer_phone, order_date, required_delivery_date, order_amount, audit_status, remark, created_at, updated_at)
SELECT 'CO202607010', '深圳华创科技有限公司', '林经理', '13900001001', '2026-07-08', '2026-07-28', 34000.00, 'PENDING',
       '客户门户提交：追加50台商用显示器', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM customer_order WHERE order_no = 'CO202607010');

INSERT INTO customer_order_item (order_id, material_id, product_name, specification, quantity, unit, unit_price, line_amount, delivery_date, item_status, created_at, updated_at)
SELECT o.order_id, 7, '15.6寸商用显示器', '1920x1080 商用款', 50.0000, '台', 680.0000, 34000.00, '2026-07-28', 'PENDING', NOW(), NOW()
FROM customer_order o
WHERE o.order_no = 'CO202607010'
  AND NOT EXISTS (SELECT 1 FROM customer_order_item i WHERE i.order_id = o.order_id);
