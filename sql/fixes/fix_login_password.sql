-- 统一登录密码为 123456（BCrypt）
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/fixes/fix_login_password.sql
UPDATE `user` SET password_hash = '$2b$10$KoXI6kkD6AOp0WtyKXENlek9gKs2sAMFIHjUtme1CiPaSYnEhTzRm' WHERE status = 1;
