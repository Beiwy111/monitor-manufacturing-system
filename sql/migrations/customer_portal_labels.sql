USE display_manufacturing;
UPDATE role SET role_name='客户用户', role_description='B2B客户门户' WHERE role_code='CUSTOMER';
UPDATE sys_menu SET menu_name='客户门户' WHERE menu_id=201;
UPDATE sys_menu SET menu_name='新建订单' WHERE menu_id=202;
UPDATE sys_menu SET menu_name='我的订单' WHERE menu_id=203;
UPDATE sys_menu SET menu_name='产品与规格' WHERE menu_id=204;
UPDATE sys_menu SET menu_name='提交反馈' WHERE menu_id=205;
UPDATE sys_menu SET menu_name='我的反馈' WHERE menu_id=206;
UPDATE sys_menu SET menu_name='个人中心' WHERE menu_id=207;
