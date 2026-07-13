import mysql.connector

conn = mysql.connector.connect(
    host="localhost",
    user="root",
    password="Zyd051104",
    database="display_manufacturing",
)
cur = conn.cursor()
pwd = "$2b$10$KoXI6kkD6AOp0WtyKXENlek9gKs2sAMFIHjUtme1CiPaSYnEhTzRm"
users = [
    (17, "huachuang", "林经理", "13900001001", "huachuang@example.com", "深圳华创科技", "深圳华创科技有限公司", "深圳市南山区科技园南路88号"),
    (17, "xingchen", "王总", "13900001002", "xingchen@example.com", "北京星辰电竞", "北京星辰电竞俱乐部", "北京市朝阳区电竞产业园A座"),
]
for u in users:
    cur.execute("SELECT user_id FROM user WHERE username=%s", (u[1],))
    if cur.fetchone():
        continue
    cur.execute(
        """INSERT INTO user (role_id,username,password_hash,real_name,phone,email,department,customer_name,shipping_address,status,created_at,updated_at)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,1,NOW(),NOW())""",
        (u[0], u[1], pwd, u[2], u[3], u[4], u[5], u[6], u[7]),
    )

cur.execute("SELECT order_id FROM customer_order WHERE order_no=%s", ("CO202607010",))
if not cur.fetchone():
    cur.execute(
        """INSERT INTO customer_order (order_no,customer_name,customer_contact,customer_phone,order_date,required_delivery_date,order_amount,audit_status,remark,created_at,updated_at)
        VALUES ('CO202607010','深圳华创科技有限公司','林经理','13900001001','2026-07-08','2026-07-28',34000.00,'PENDING','客户门户提交：追加50台商用显示器',NOW(),NOW())"""
    )
    conn.commit()
    cur.execute("SELECT order_id FROM customer_order WHERE order_no=%s", ("CO202607010",))
    oid = cur.fetchone()[0]
    cur.execute("SELECT 1 FROM customer_order_item WHERE order_id=%s", (oid,))
    if not cur.fetchone():
        cur.execute(
            """INSERT INTO customer_order_item (order_id,material_id,product_name,specification,quantity,unit,unit_price,line_amount,delivery_date,item_status,created_at,updated_at)
            VALUES (%s,7,'15.6寸商用显示器','1920x1080 商用款',50,'台',680,34000,'2026-07-28','PENDING',NOW(),NOW())""",
            (oid,),
        )

conn.commit()
cur.close()
conn.close()
print("seed ok")
