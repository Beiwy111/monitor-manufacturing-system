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
├── sql/                     # 数据库脚本（init / migrations / fixes）
│   ├── init/                # 初始化与种子数据
│   ├── migrations/          # 模块升级
│   └── fixes/               # 一次性修复
├── schema.tsv               # 表结构设计
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

### MES Agent / Spring AI 本地配置

后端使用 Spring AI 1.1.8 的 OpenAI 兼容模型适配器连接 DeepSeek V4 Pro。配置位于
`backend/src/main/resources/application-ai.yml`，该配置仅在启用 `ai` Profile 时生效，
并复用 `application.yaml` 中现有的 `deepseek.api-key`。

启动 Agent：

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=ai"
```

启动后访问 `GET http://localhost:8088/ai/health`。该接口只验证 `ChatClient` Bean
已成功初始化，不会向模型发送消息，也不会产生模型调用费用。

Agent 聊天接口为 `POST /agent/chat`，必须携带登录返回的 Bearer Token：

```json
{
  "message": "新建一个订单，客户是答辩演示客户，型号是23.8寸电竞显示器",
  "sessionId": "demo-001",
  "conversation": []
}
```

信息不完整时 Agent 会提示补充数量、交期等字段。后续请求继续使用同一个 `sessionId`，
并携带前端保存的 `conversation`，已填写参数会在 Redis 草稿中保留 15 分钟。信息完整后
返回 `type=confirm` 和 `proposalId=AIP-...`，此时还没有修改业务数据。

确认方案可继续使用现有前端接口：

```http
POST /assistant/execute
Authorization: Bearer <登录 token>
Content-Type: application/json

{
  "proposalId": "AIP-示例编号",
  "decision": "APPROVE"
}
```

也可使用 Agent 专用确认接口：

```http
POST /agent/plans/AIP-示例编号/confirm
Authorization: Bearer <登录 token>
Content-Type: application/json

{
  "decision": "APPROVE",
  "finalParams": null
}
```

`decision` 支持 `APPROVE`、`MODIFY` 和 `SKIP`。确认时后端会重新读取 JWT/Redis 登录会话，
再次校验角色、方案所有人、方案有效期和业务快照；重复确认同一方案只返回首次结果。

管理员可使用全部后台查询、分析和非客户自助业务动作；其他角色只会获得其前后端职责范围内的
Tool。查询 Tool 可直接执行；写 Tool 只生成待确认方案，确认后由事务服务调用现有业务 Service。
Agent 不直接访问 Mapper，也不暴露 SQL、Shell、代码执行或通用数据库工具。

常用答辩测试语句：

- 订单人员：`新建一个订单，客户是答辩演示客户，型号是23.8寸电竞显示器`
- 计划员：`为订单 CO202607001 智能排产，7月20日开始，7月25日结束`
- 生产主管：`为计划 PP202607001 生成智能派工方案`
- 操作员：`接收派工 DT202607001`
- 质检员：`把质检单 QC202607001 判定为合格，抽检100台，合格100台，不合格0台`
- 设备人员：`给设备ID 1触发报警，描述为贴片机温度异常`

标准测试不会调用付费模型，可执行：

```powershell
cd backend
.\mvnw.cmd "-Dspring.main.lazy-initialization=true" "-Dspring.task.scheduling.enabled=false" test
```

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
| admin | 123456 | 系统管理员 |
| li_manager | 123456 | 生产主管 |
| li_planner | 123456 | 计划员 |
| liu_purchase | 123456 | 采购员 |
| chen_qc | 123456 | 质检员 |
| zhou_warehouse | 123456 | 仓管员 |

（首次改密前旧密码可能仍为 `Mes@2026`；执行 `sql/fixes/fix_login_password.sql` 或 `reset_password_via_api.ps1` 后统一为 `123456`。）

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
mysql -u root -p display_manufacturing < sql/init/seed_data.sql
mysql -u root -p display_manufacturing < sql/init/sys_menu.sql
```

## 已实现阶段

1. **项目结构整理** — backend / frontend 分离，后端可独立运行
2. **后端基础能力** — Result、全局异常、分页对象、JWT 登录、CORS、菜单接口
3. **前端基础框架** — 登录、Layout、菜单、路由守卫、Axios、Pinia
4. **核心业务骨架** — 订单、生产、质检、入库等列表页（对接现有 list 接口）
5. **辅助模块骨架** — 采购、设备、发货、售后、成本等列表页

后续可在各模块页面中逐步补充新增、编辑、审核、派工、报工等业务操作。
