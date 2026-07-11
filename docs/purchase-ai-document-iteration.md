# AI 采购单据自动解析录入 - 迭代文档

## 功能目标

用户上传采购单据图片（JPG/PNG），后端调用视觉 AI 模型自动提取结构化字段（供应商、付款方式、物料行），
前端展示可编辑表格，用户人工校正后一键确认，生成采购单草稿写入数据库。

## 前后端交互流程

```
[前端] 选择图片 → 点击「开始 AI 解析」
   ↓ POST /purchase/ai/document/parse  (multipart/form-data, key=file)
[后端] 校验文件类型 → base64 编码 → 调用视觉 AI → 提取 JSON → 返回解析结果
   ↓ Result<AiDocumentParseResult>
[前端] 渲染可编辑表格，用户修改字段/新增行/删除行
   ↓ POST /purchase/ai/document/confirm  (JSON body)
[后端] 校验必填 → @Transactional 写入 PurchaseOrder + PurchaseOrderItem → 返回草稿 ID
   ↓ Result<AiDocumentConfirmResponse>
[前端] 显示采购单号、状态 DRAFT，提供「查看采购订单」跳转按钮
```

## 页面入口

- 路由：`/purchase/ai-document`
- 菜单：登录采购员账号（`liu_purchase / 123456`）后，左侧菜单「采购管理」→「AI 单据录入」
- 也可直接访问：`http://localhost:5173/purchase/ai-document`

## 后端接口

### POST /purchase/ai/document/parse

- 请求：`multipart/form-data`，参数名 `file`，支持 jpg/jpeg/png
- 响应：`Result<AiDocumentParseResult>`

```json
{
  "code": 200,
  "data": {
    "supplierName": "华东机电供应商",
    "paymentMethod": "月结30天",
    "items": [
      {
        "materialCode": "M001",
        "materialName": "深沟球轴承",
        "specification": "6205-2RS",
        "unitPrice": 12.50,
        "quantity": 100,
        "deliveryDate": "2026-08-01",
        "remark": "",
        "confidence": 0.95
      }
    ],
    "rawText": "...",
    "rawJson": "..."
  }
}
```

### POST /purchase/ai/document/confirm

- 请求：JSON body `AiDocumentConfirmRequest`
- 响应：`Result<AiDocumentConfirmResponse>`

```json
{
  "code": 200,
  "data": {
    "purchaseOrderId": 1001,
    "purchaseOrderNo": "AI20260710143022",
    "status": "DRAFT",
    "message": "AI解析结果已确认录入为采购单草稿"
  }
}
```

## DTO 字段说明

### AiDocumentParseResult

| 字段 | 类型 | 说明 |
|------|------|------|
| supplierName | String | 供应商名称 |
| paymentMethod | String | 付款方式 |
| items | List | 物料行列表 |
| rawText | String | AI 原始回复文本 |
| rawJson | String | 从回复中提取的 JSON 字符串 |

### AiDocumentItemDTO

| 字段 | 类型 | 说明 |
|------|------|------|
| materialCode | String | 物料编码（可为空） |
| materialName | String | 物料名称（必填） |
| specification | String | 规格型号 |
| unitPrice | BigDecimal | 单价（可为 null） |
| quantity | BigDecimal | 数量（必填，>0） |
| deliveryDate | String | 交期，格式 yyyy-MM-dd |
| remark | String | 备注 |
| confidence | Double | AI 置信度 0~1 |

## AI Key 配置方式

在 `application.yaml` 中配置：

```yaml
ai:
  api-key: ${AI_API_KEY:your-key-here}
  base-url: ${AI_BASE_URL:https://api.siliconflow.cn/v1}
  model: ${AI_MODEL:Qwen/Qwen2.5-VL-72B-Instruct}
  timeout-seconds: 60
  mock: false
```

- 生产环境建议通过环境变量 `AI_API_KEY` 注入，不要明文写入配置文件
- 当前已预填 SiliconFlow 的 API Key，使用 Qwen2.5-VL 视觉模型
- 切换模型只需修改 `ai.model`，接口兼容 OpenAI Chat Completions Vision 格式

### Mock 模式

将 `ai.mock: true` 后，parse 接口不调用真实 AI，直接返回两条演示数据，方便前端联调。
**默认关闭（mock: false）**。

## Prompt 设计

固定 System Prompt 维护在 `AiDocumentServiceImpl.PROMPT` 常量中。
核心要求：

1. 角色定位：采购单据结构化解析助手
2. 严格返回 JSON，不返回 Markdown，不解释
3. 指定所有字段名和类型，无法识别的数字字段返回 null
4. 置信度 confidence 0~1 之间

## 确认录入落库策略

项目无采购申请单表，直接写入 `purchase_order` + `purchase_order_item`：

- `purchase_order.status` = `DRAFT`
- `purchase_order.purchase_order_no` = `AI` + 时间戳（如 `AI20260710143022`）
- `purchase_order.remark` 存储付款方式
- `purchase_order_item` 每条物料一行，`item_status` = `DRAFT`
- 整个操作用 `@Transactional` 保证主单和明细原子性

## 前端测试步骤

1. 启动后端：`cd backend && .\mvnw.cmd spring-boot:run`
2. 启动前端：`cd frontend && npm run dev`
3. 浏览器访问 `http://localhost:5173`，用 `liu_purchase / 123456` 登录
4. 左侧菜单「采购管理」→「AI 单据录入」
5. 点击上传区域，选择一张采购单据图片（JPG/PNG）
6. 点击「开始 AI 解析」，等待约 10~30 秒（视觉模型处理时间）
7. 解析成功后表格自动填入，可修改字段、新增行、删除行
8. 点击「确认录入为采购单草稿」
9. 成功后显示生成的采购单号，点「查看采购订单」跳转核查

## 后端接口测试步骤（Swagger）

访问 `http://localhost:8088/swagger-ui.html`：

1. `POST /purchase/ai/document/parse`：上传图片文件，观察返回的 JSON
2. `POST /purchase/ai/document/confirm`：粘贴上一步返回的 items，观察是否生成采购单
3. 到 `GET /purchase/purchaseOrder/list` 确认新增了一条 DRAFT 状态的采购单

## 异常兜底

| 场景 | 后端处理 | 前端显示 |
|------|----------|----------|
| 上传非图片格式 | 返回 400 + 友好提示 | 前端 input accept 过滤，同时显示 el-alert 错误 |
| AI_API_KEY 未配置 | 返回 500 + "AI_API_KEY 未配置" | el-alert 红色提示 |
| AI 接口超时 | 捕获异常，返回 500 + "AI 调用失败：..." | el-alert 红色提示 |
| AI 返回非 JSON | extractJson 尝试提取，失败则返回友好错误 | el-alert 红色提示 |
| 物料名称为空 | confirm 接口校验返回 400 | el-message 错误提示，行内红字标注 |
| 数量 <= 0 | confirm 接口校验返回 400 | el-message 错误提示，行内红字标注 |
| 数据库写入失败 | @Transactional 回滚，返回 500 | el-message 错误提示 |

## 后续扩展

- **PDF 首页解析**：使用 Apache PDFBox 提取首页为图片，再传入视觉模型
- **供应商自动匹配**：解析出 supplierName 后，模糊搜索 supplier 表，候选项下拉供选择
- **物料编码自动匹配**：解析出 materialName/specification 后，模糊搜索 material 表，辅助填充 materialId
- **单据历史记录**：新增 ai_parse_log 表，记录每次解析的原始图片路径、rawJson、解析时间、操作人
- **全局 Agent 自然语言查询**：计划作为独立 Agent 模块实现，不在本迭代范围内
