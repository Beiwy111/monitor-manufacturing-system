# 显示屏制造管理系统（前后端分离）

本项目采用 **前后端分离** 架构，根目录保留数据库设计与代码生成脚本，业务代码分别位于 `backend/` 与 `frontend/`。

## 目录结构

```
computer/
├── backend/                 # Spring Boot 后端（包名 com.upc.computer 不变）
│   ├── src/main/java/...
│   ├── src/main/resources/
│   ├── pom.xml
│   └── mvnw / mvnw.cmd
├── frontend/                # Vue 3 + Vite 前端
│   └── src/
│       ├── api/             # 接口封装
│       ├── router/          # 路由与守卫
│       ├── stores/          # Pinia 状态
│       ├── layouts/         # 主布局
│       ├── views/           # 业务页面（按模块分目录）
│       ├── components/      # 公共组件
│       ├── utils/           # 工具（Axios 等）
│       └── styles/
├── schema.tsv               # 表结构设计
├── seed_data.sql            # 演示数据
├── sys_menu.sql             # 菜单数据
└── generate_code.py         # 代码生成脚本
```

## 启动方式

### 后端（端口 8088）

```bash
cd backend
./mvnw spring-boot:run
# Windows 也可：mvnw.cmd spring-boot:run
# 或：mvn spring-boot:run
```

接口根地址：`http://localhost:8088`

### 前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

访问地址：`http://localhost:5173`（开发环境通过 Vite 代理 `/api` → `http://localhost:8088`）

## 登录与角色

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | password | 系统管理员 |
| planner01 | password | 计划员 / 生产主管 |
| operator01 | password | 操作员 |
| qc01 | password | 质检员 |
| purchase01 | password | 采购员 |
| warehouse01 | password | 仓管员 |
| service01 | password | 售后专员 |

认证接口（返回 `Result` 包装）：

- `POST /auth/login` — 登录
- `GET /auth/userInfo` — 当前用户
- `GET /auth/menus` — 角色菜单树

业务 CRUD 接口保持原有风格（直接返回 `ArrayList`），例如：

- `GET /order/customerOrder/list`
- `GET /production/workOrder/list`

## 前端模块说明

| 目录 | 说明 |
|------|------|
| views/dashboard | 工作台 / 统计看板 |
| views/system | 用户、角色、操作日志 |
| views/order | 客户订单 |
| views/production | 计划、工单、派工、报工 |
| views/purchase | 采购订单 |
| views/warehouse | 库存、入库 |
| views/quality | 质量检验 |
| views/device | 设备、安灯报警 |
| views/delivery | 发货 |
| views/aftersale | 售后 |
| views/cost | 成本结算 |

左侧菜单优先从 `/auth/menus` 加载，按角色过滤；`MainLayout` 中 `apiPathMap` 将后端 `apiPath` 映射到前端路由。

## 数据库

MySQL 库名：`display_manufacturing`

```bash
mysql -u root -p display_manufacturing < seed_data.sql
mysql -u root -p display_manufacturing < sys_menu.sql
```

## 已实现阶段

1. **项目结构整理** — backend / frontend 分离，后端可独立运行
2. **后端基础能力** — Result、全局异常、分页对象、JWT 登录、CORS、菜单接口
3. **前端基础框架** — 登录、Layout、菜单、路由守卫、Axios、Pinia
4. **核心业务骨架** — 订单、生产、质检、入库等列表页（对接现有 list 接口）
5. **辅助模块骨架** — 采购、设备、发货、售后、成本等列表页

后续可在各模块页面中逐步补充新增、编辑、审核、派工、报工等业务操作。
