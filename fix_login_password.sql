-- 统一登录密码为 Mes@2026（BCrypt）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < fix_login_password.sql
UPDATE `user` SET password_hash = '$2b$10$uMXvLPZc/8QyK1T13GovwuxodZJlnVcnS0N.NeYkVWvsFs3D0rXnG' WHERE status = 1;
