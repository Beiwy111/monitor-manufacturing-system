-- 修复售后菜单中文名称（Windows 下避免 SQL 文件编码导致 ????）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/migrations/aftersale_menu_fix_names.sql

USE display_manufacturing;

UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E8B083E69FA5E5B7A5E4BD9CE58FB0') USING utf8mb4), updated_at = NOW() WHERE menu_id = 100;
UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E594AEE5908EE5B7A5E58D95') USING utf8mb4), updated_at = NOW() WHERE menu_id = 101;
UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E696B9E6A188E5AEA1E689B9') USING utf8mb4), updated_at = NOW() WHERE menu_id = 102;
UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E689A7E8A18CE58D8FE5908C') USING utf8mb4), updated_at = NOW() WHERE menu_id = 103;
UPDATE sys_menu SET menu_name = CONVERT(UNHEX('E9AA8CE8AF81E997ADE78EAF') USING utf8mb4), updated_at = NOW() WHERE menu_id = 104;
