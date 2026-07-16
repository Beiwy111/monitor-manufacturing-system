-- 财务模块重构：精简菜单 + 回款/催款表
USE display_manufacturing;
SET NAMES utf8mb4;

-- 1) 父菜单改名：财务管理
UPDATE sys_menu SET
  menu_name = CONVERT(UNHEX('E8B4A2E58AA1E7AEA1E79086') USING utf8mb4),
  menu_code = 'cost',
  route_path = NULL
WHERE menu_id = 110;

-- 2) 子菜单精简为 5 项
UPDATE sys_menu SET
  menu_name = CONVERT(UNHEX('E9A696E9A1B5') USING utf8mb4),
  menu_code = 'cost:home',
  route_path = '/dashboard/cost',
  sort_no = 1
WHERE menu_id = 111;

UPDATE sys_menu SET
  menu_name = CONVERT(UNHEX('E68890E69CACE6A0B8E7AE97') USING utf8mb4),
  menu_code = 'cost:accounting',
  route_path = '/finance/cost-accounting',
  sort_no = 2
WHERE menu_id = 112;

UPDATE sys_menu SET
  menu_name = CONVERT(UNHEX('E694B6E79B8AE6A0B8E7AE97') USING utf8mb4),
  menu_code = 'cost:revenue',
  route_path = '/finance/revenue',
  sort_no = 3
WHERE menu_id = 113;

UPDATE sys_menu SET
  menu_name = CONVERT(UNHEX('E8B4A2E58AA1E5A4A7E5B18F') USING utf8mb4),
  menu_code = 'cost:screen',
  route_path = '/finance/screen',
  sort_no = 4
WHERE menu_id = 114;

UPDATE sys_menu SET
  menu_name = CONVERT(UNHEX('E8B4A2E58AA1E68AA5E8A1A8') USING utf8mb4),
  menu_code = 'cost:financeReport',
  route_path = '/finance/report',
  sort_no = 5
WHERE menu_id = 115;

-- 旧「成本报表」菜单停用（角色权限保留无影响）
UPDATE sys_menu SET status = 0 WHERE menu_id = 116;

DELETE FROM sys_role_menu WHERE role_id = 11 AND menu_id = 116;

-- 3) 回款记录表
CREATE TABLE IF NOT EXISTS finance_payment (
  payment_id        bigint unsigned NOT NULL AUTO_INCREMENT,
  order_id          bigint unsigned NOT NULL,
  order_no          varchar(50)     NOT NULL,
  customer_name     varchar(150)    NOT NULL,
  contract_amount   decimal(18,2)   NOT NULL DEFAULT 0,
  discount_amount   decimal(18,2)   NOT NULL DEFAULT 0,
  refund_amount     decimal(18,2)   NOT NULL DEFAULT 0,
  tax_amount        decimal(18,2)   NOT NULL DEFAULT 0,
  receivable_amount decimal(18,2)   NOT NULL DEFAULT 0,
  received_amount   decimal(18,2)   NOT NULL DEFAULT 0,
  planned_date      date            DEFAULT NULL,
  actual_date       date            DEFAULT NULL,
  payment_status    varchar(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PARTIAL/RECEIVED/OVERDUE',
  remark            varchar(500)    DEFAULT NULL,
  created_at        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (payment_id),
  UNIQUE KEY uk_fin_payment_order (order_id),
  KEY idx_fin_payment_status (payment_status),
  KEY idx_fin_payment_customer (customer_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单回款记录';

-- 4) 催款记录表
CREATE TABLE IF NOT EXISTS finance_collection_log (
  log_id         bigint unsigned NOT NULL AUTO_INCREMENT,
  order_id       bigint unsigned DEFAULT NULL,
  order_no       varchar(50)     DEFAULT NULL,
  customer_name  varchar(150)    NOT NULL,
  note           varchar(500)    NOT NULL,
  operator       varchar(64)     DEFAULT NULL,
  created_at     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id),
  KEY idx_fin_collection_customer (customer_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收账款催款记录';

-- 5) 从订单初始化回款（仅尚无记录的订单）
INSERT INTO finance_payment
(order_id, order_no, customer_name, contract_amount, discount_amount, refund_amount, tax_amount,
 receivable_amount, received_amount, planned_date, actual_date, payment_status, remark)
SELECT
  co.order_id,
  co.order_no,
  co.customer_name,
  co.order_amount,
  GREATEST(co.order_amount - IFNULL((
    SELECT SUM(oi.line_amount) FROM customer_order_item oi WHERE oi.order_id = co.order_id
  ), co.order_amount), 0),
  IFNULL((
    SELECT SUM(c.actual_cost) FROM after_sales_case ac
    JOIN after_sales_closure c ON c.case_no = ac.case_no
    WHERE ac.order_id = co.order_id AND c.actual_cost > 0
  ), 0),
  ROUND(co.order_amount * 0.13, 2),
  co.order_amount,
  CASE
    WHEN co.audit_status = 'SHIPPED' THEN ROUND(co.order_amount * 0.85, 2)
    WHEN co.audit_status IN ('APPROVED','PLANNED','PRODUCING') THEN ROUND(co.order_amount * 0.30, 2)
    ELSE 0
  END,
  co.required_delivery_date,
  CASE WHEN co.audit_status = 'SHIPPED' THEN DATE_ADD(co.required_delivery_date, INTERVAL 15 DAY) ELSE NULL END,
  CASE
    WHEN co.audit_status = 'SHIPPED' AND co.required_delivery_date < CURDATE() THEN 'PARTIAL'
    WHEN co.audit_status = 'SHIPPED' THEN 'PARTIAL'
    WHEN co.audit_status IN ('APPROVED','PLANNED','PRODUCING') THEN 'PENDING'
    ELSE 'PENDING'
  END,
  CONCAT('订单 ', co.order_no, ' 回款初始化')
FROM customer_order co
WHERE co.audit_status NOT IN ('PENDING','REJECTED','CANCELLED')
  AND NOT EXISTS (SELECT 1 FROM finance_payment fp WHERE fp.order_id = co.order_id);

UPDATE finance_payment SET
  receivable_amount = GREATEST(contract_amount - discount_amount - refund_amount - received_amount, 0);

-- 6) 催款样例
INSERT INTO finance_collection_log (order_id, order_no, customer_name, note, operator)
SELECT fp.order_id, fp.order_no, fp.customer_name,
  CONCAT('电话催款，待收 ¥', FORMAT(fp.receivable_amount, 2)),
  'zheng_cost'
FROM finance_payment fp
WHERE fp.payment_status IN ('PENDING','PARTIAL','OVERDUE')
  AND fp.receivable_amount > 0
  AND NOT EXISTS (
    SELECT 1 FROM finance_collection_log l WHERE l.order_id = fp.order_id LIMIT 1
  )
LIMIT 4;
