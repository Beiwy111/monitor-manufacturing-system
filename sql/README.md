# 数据库脚本

MySQL 库名：`display_manufacturing`

## 目录说明

| 目录 | 说明 |
|------|------|
| `init/` | 全量初始化、演示数据、菜单、角色用户 |
| `migrations/` | 模块升级与表结构变更 |
| `fixes/` | 一次性数据/配置修复 |

## 全新环境（推荐顺序）

```bash
# 1. 导入完整库（含表结构与基础数据）
mysql -u root -p display_manufacturing < sql/init/init.sql

# 或分步导入：
mysql -u root -p display_manufacturing < sql/init/seed_data.sql
mysql -u root -p display_manufacturing < sql/init/sys_menu.sql
mysql -u root -p display_manufacturing < sql/init/seed_business_roles_users.sql
```

按需执行 `migrations/` 下各模块脚本（若 `init.sql` 已包含最新结构可跳过）。

## 常用修复

```bash
# 统一所有启用用户密码为 123456（需能连上 MySQL）
mysql -u root -p display_manufacturing < sql/fixes/fix_login_password.sql
```

若本机 `mysql` 命令连不上库，可在**后端已启动**时执行：

```powershell
powershell -ExecutionPolicy Bypass -File sql/fixes/reset_password_via_api.ps1
```
