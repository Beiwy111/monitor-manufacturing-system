# 采购模块迭代说明

## 1. 本次采购模块重组目标

将 `PurchaseService` 从普通采购订单 CRUD 升级为业务闭环模块：

- 缺料需求汇总：基于销售订单 / 生产工单 / BOM / 库存 / 在途采购，形成待采购物料清单
- 采购工作台：物料维度 + 订单维度两种视图
- 一键生成采购单：勾选多条需求合并生成采购订单及明细
- 到货确认 + 入库预留联动：更新采购单、明细、需求状态，并预留库存入库对接点

原有采购订单、采购明细的 CRUD 功能完整保留。

## 2. 业务流程

```
销售订单(已审核) / 生产工单(在制)
        |
        v  展开产品 BOM（含损耗率，多级递归）
   计算物料总需求
        |
        v  扣减可用库存 quantity_available
        v  扣减在途采购数量（未到货采购明细的 quantity - received_quantity）
        |
        v  净缺料 = 总需求 - 库存 - 在途
        |
        v  写入采购需求工作台（物料维度汇总 + 来源追溯）
        |
   采购员筛选 / 勾选
        |
        v  一键生成采购单（PurchaseOrder + PurchaseOrderItem）
        v  需求状态 -> PURCHASED
        |
        v  到货确认 confirmArrival
        v  采购单 -> ARRIVED，明细收货，需求 -> ARRIVED
        v  预留 MaterialService.inboundByPurchase 入库联动点
```

## 3. 新增 / 修改的实体

新增实体：

- `PurchaseRequirement`（表 `purchase_requirement`）：物料维度汇总需求
- `PurchaseRequirementSource`（表 `purchase_requirement_source`）：来源订单/工单追溯

新增 VO / DTO：

- `vo/PurchaseRequirementDetailVO`：物料需求 + 来源明细
- `vo/PurchaseByOrderVO`：订单维度分组视图
- `dto/GeneratePurchaseRequest`：一键生成采购单入参

未修改订单、生产、物料实体，仅只读引用其已有 Mapper。

## 4. 新增 / 修改的接口

Controller 全部工作台接口统一返回 `Result<T>`，遵循查询 `@GetMapping`、写操作 `@PostMapping`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/purchase/workbench/calculate` | 重新计算缺料需求并写入工作台 |
| GET | `/purchase/workbench/list` | 物料维度待采购清单，支持 materialName/status/priority 筛选 |
| GET | `/purchase/workbench/detail` | 某物料需求的来源订单明细 |
| GET | `/purchase/workbench/byOrder` | 订单维度视图，按订单/工单分组展开缺料 |
| POST | `/purchase/workbench/generate` | 勾选需求一键生成采购单 |
| POST | `/purchase/workbench/select` | 标记需求已选中 |
| POST | `/purchase/workbench/cancel` | 取消待采购需求 |
| POST | `/purchase/order/confirmArrival` | 采购到货确认 + 入库预留联动 |

原采购 CRUD 接口（`/purchase/purchaseOrder/*`、`/purchase/purchaseOrderItem/*`）保持不变。

## 5. 缺料计算规则

代码位置：`PurchaseServiceImpl.calculateRequirements()` 及私有方法 `expandDemand`、`loadAvailableStock`、`loadOnPurchaseQuantity`。

规则：

1. **需求来源**
   - 销售订单：仅统计 `audit_status` 为 APPROVED/PASS/PASSED 的订单
   - 生产工单：仅统计在制状态（CREATED/ISSUED/RELEASED/DISPATCHED/IN_PROGRESS/RUNNING）

2. **BOM 展开**（`expandDemand` 递归）
   - 若物料存在子件（在 BOM 中作为 parent），继续向下递归展开
   - 若物料为叶子件（无子件），累加为可采购需求
   - 子件需求 = 父件需求 × 单位用量 `quantity` × (1 + 损耗率 `loss_rate`)
   - 使用 visiting 集合防止 BOM 循环引用

3. **净缺料**
   - 可用库存 = `inventory.quantity_available` 汇总到物料维度（为空时回退 `quantity_on_hand`）
   - 在途采购 = 未到货采购明细的 `(quantity - received_quantity)` 汇总（已到货/已取消采购单不计入）
   - `净缺料 = 总需求 - 可用库存 - 在途采购`
   - 净缺料 ≤ 0 的物料不生成采购需求

4. **合并同物料**
   - 以 `materialId` 为 key 汇总到一条 `PurchaseRequirement`
   - 每个来源单独写入一条 `PurchaseRequirementSource`，保留可反查明细

5. **优先级 / 期望到货**
   - 交期 7 天内的销售订单需求优先级=1，其余订单=5，工单=2
   - 期望到货日期取来源中最早的交期

## 6. 采购工作台两种视图

- **物料维度**（`/workbench/list` + `/workbench/detail`）
  - 列表按物料汇总，一种物料一行
  - detail 展开该物料来自哪些销售订单/工单，含需求数量、缺料数量

- **订单维度**（`/workbench/byOrder`）
  - 按来源订单/工单分组，一个订单下展开其全部缺料零部件行

## 7. 到货确认与库存入库联动

代码位置：`PurchaseServiceImpl.confirmArrival(Long purchaseOrderId)`，`@Transactional` 控制。

流程：

1. 校验采购单存在、非 ARRIVED、非 CANCELLED
2. 采购单状态 -> ARRIVED，更新 `updated_at`
3. 采购明细 `received_quantity = quantity`，`item_status = ARRIVED`
4. 关联采购需求状态 -> ARRIVED
5. **入库联动点（预留）**：当人员 A 的 `MaterialService.inboundByPurchase(Long purchaseOrderId)` 就绪后，在 TODO 处调用即可；当前不直接修改 `inventory` 表，避免与库存主流程冲突

## 8. 当前已完成内容

- 缺料需求计算（BOM 多级展开 + 库存/在途扣减 + 同物料合并 + 来源追溯）
- 物料维度、订单维度两种工作台视图
- 一键生成采购单（事务控制，主表明细同时成功/失败，回写需求状态与总金额）
- select / cancel 需求状态流转
- 到货确认（采购单、明细、需求状态联动 + 入库预留点）
- 建表脚本 `sql/migrations/purchase_requirement.sql`
- 原采购 CRUD 完整保留

## 9. 暂未完成但预留的扩展点

- **按供应商拆单**：当前无供应商模型，`generate` 合并生成一张采购单。后续引入供应商表后，可按 material→supplier 映射拆分多张采购单
- **入库联动**：`confirmArrival` 中已预留 `inboundByPurchase` 调用点（TODO），待库存模块提供接口
- **部分到货**：当前到货确认为整单全到货（received = quantity）。PART_ARRIVED 状态已在枚举中预留，后续可支持按明细分批收货
- **安全库存**：`material.safety_stock` 暂未纳入缺料计算，后续可将其加入总需求

## 10. 与人员 A 模块的对接点

| 对接内容 | 说明 |
| --- | --- |
| `MaterialService.inboundByPurchase(Long purchaseOrderId)` | 到货确认后的库存入库，A 提供接口后 B 在 `confirmArrival` TODO 处调用 |
| 库存字段 `inventory.quantity_available` | 缺料计算读取，A 需保证该字段有效（为空回退 quantity_on_hand） |
| BOM 数据 `bom` 表 | 缺料展开依赖 parent/child/quantity/loss_rate/status 字段 |
| 工单状态取值 | 缺料计算按 CREATED/ISSUED/RELEASED/DISPATCHED/IN_PROGRESS/RUNNING 判定在制，需与 A 的工单状态枚举保持一致 |

## 11. 自测清单

准备：先执行 `sql/migrations/purchase_requirement.sql` 建表；确保库中有已审核销售订单、BOM、库存、（可选）在途采购单数据。

1. `POST /purchase/workbench/calculate` — 返回缺料需求列表，同一物料合并为一条
2. `GET /purchase/workbench/list?materialName=xx&status=PENDING&priority=1` — 筛选生效
3. `GET /purchase/workbench/detail?requirementId=1` — 返回该物料来源订单明细
4. `GET /purchase/workbench/byOrder` — 按订单/工单分组展开缺料
5. `POST /purchase/workbench/select?requirementId=1` — 状态变 SELECTED
6. `POST /purchase/workbench/generate` body `{"requirementIds":[1,2],"supplierName":"供应商A"}` — 生成采购单，需求状态变 PURCHASED
7. 重复 generate 同一需求 — 返回业务异常（不可重复生成）
8. `POST /purchase/order/confirmArrival?purchaseOrderId=xx` — 采购单变 ARRIVED，明细 received=quantity，需求变 ARRIVED
9. 重复 confirmArrival — 返回业务异常（不可重复确认）
10. 空 requirementIds 调用 generate — 返回业务异常（至少勾选一条）
11. 项目正常启动，无 Mapper 解析、Bean 注入、编译错误

验证工具：可用 Swagger UI（`http://localhost:8088/swagger-ui.html`）或前端联调。
