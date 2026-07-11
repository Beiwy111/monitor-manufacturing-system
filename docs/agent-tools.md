# Agent 工具清单（五大模块）

> 面向"管道 + 人工闸门"式全流程智能体。每个工具 = 后端一个真实 REST 接口。
> 约定：所有返回都是 `Result{code,message,data}`，前端 axios 已自动解包为 `data`。
> **护栏**：`read` 类可由 Agent 自主调用；`write` 类**必须经人工闸门确认后**才执行（见 `agent_flow.sql`）。
> `operator` 一律传当前登录用户名（后端据此定位操作人）。

---

## 一图看懂：管道 + 闸门

```
目标(自然语言)
  → [采购] 缺料补料 ─┐
  → [质检] 检验判定  │  每一环：
  → [设备] 产线保障  ├─ 1.read 感知现状
  → [售后] 问题回流  │   2.propose 提议动作+理由
  → [财务] 成本结算 ─┘   3.【暂停·等人】批准/改参/跳过/终止
                        4.execute 调 write 接口
                        5.结果入 context，进入下一环
```

---

## 1. 采购 PURCHASE  （base: `/purchase`）

### read（感知）
| 工具 | 方法 · 路径 | 参数 | 说明 |
|---|---|---|---|
| purchase.shortageList | GET `/purchase/workbench/list` | materialName?, status?, priority? | 待采购缺料清单（物料维度） |
| purchase.shortageByOrder | GET `/purchase/workbench/byOrder` | — | 缺料按订单/工单展开 |
| purchase.requirementDetail | GET `/purchase/workbench/detail` | requirementId | 某需求的来源追溯 |
| purchase.orderList | GET/POST `/purchase/purchaseOrder/list` | — | 采购单列表 |

### compute / write（执行 → 需闸门）
| 工具 | 方法 · 路径 | 参数（JSON body 除非注明） | 说明 |
|---|---|---|---|
| purchase.calculate | POST `/purchase/workbench/calculate` | — | 重算缺料（会写工作台，幂等） |
| **purchase.generate** | POST `/purchase/workbench/generate` | `{requirementIds:[Long], supplierOverrides?:{}, purchaserId?, remark?}` | 一键按供应商拆单生成采购单 |
| purchase.confirmArrival | POST `/purchase/order/confirmArrival?purchaseOrderId=` | query | 到货确认 + 自动入库 |
| purchase.revoke | POST `/purchase/order/revoke?purchaseOrderId=` | query | 撤销采购单，需求恢复待采购 |
| purchase.aiParse | POST `/purchase/ai/document/parse` | multipart `file` | AI 识别送货/采购单据（演示爆点） |

---

## 2. 质检 QUALITY  （base: `/quality`，write body = QualityActionRequest）

### read
| 工具 | 方法 · 路径 | 参数 | 说明 |
|---|---|---|---|
| quality.kpi | GET `/quality/kpi` | — | 待检/通过/不通过/复检/今日关闭 |
| quality.inspectionViews | GET `/quality/inspection/views` | — | 质检单列表（含物料名、中文状态） |
| quality.nonconformingViews | GET `/quality/nonconforming/views` | — | 不良品台账 |
| quality.recheckViews | GET `/quality/recheck/views` | — | 需复检清单 |
| quality.inspectionDetail | GET `/quality/inspection/detail` | inspectionId | 单据 + 检测项 + 关联不良品 |

### write（需闸门）
| 工具 | 方法 · 路径 | 参数（body） | 说明 |
|---|---|---|---|
| **quality.pass** | POST `/quality/inspection/pass` | `{inspectionId, remark?, operator}` | 判定通过 → 工单质检通过 |
| **quality.fail** | POST `/quality/inspection/fail` | `{inspectionId, defectType?, defectReason, defectQuantity, severity?, remark?, operator}` | 判定不通过 → 生成不良品 |
| quality.recheck | POST `/quality/inspection/recheck` | `{inspectionId, reason?, operator}` | 转复检 |
| quality.recheckPass / recheckFail | POST `/quality/inspection/recheckPass`(或 `/recheckFail`) | 同 pass / fail | 复检结论 |
| quality.close | POST `/quality/inspection/close` | `{inspectionId, remark?, operator}` | 关闭质检单 |
| quality.handleNc | POST `/quality/nonconforming/handle` | `{nonconformingId, handleMethod, remark?, operator}` | 不良品处置：REWORK/SCRAP/CONCESSION_ACCEPT/RETURNED |

---

## 3. 设备 EQUIPMENT  （base: `/equipment`，write body = EquipmentActionRequest）

### read
| 工具 | 方法 · 路径 | 参数 | 说明 |
|---|---|---|---|
| equipment.kpi | GET `/equipment/kpi` | — | 总数/正常/故障/维保中/未闭环报警 |
| equipment.views | GET `/equipment/equipment/views` | — | 设备台账（状态、未闭环报警数） |
| equipment.alarmViews | GET `/equipment/alarm/views` | — | 安灯报警列表 |
| equipment.maintenanceViews | GET `/equipment/maintenance/views` | — | 维护记录 |

### write（需闸门）
| 工具 | 方法 · 路径 | 参数（body） | 说明 |
|---|---|---|---|
| **equipment.triggerAlarm** | POST `/equipment/triggerAlarm` | `{equipmentId, alarmType?, alarmLevel?, description?, operator}` | 触发报警 → 设备置故障 |
| equipment.receiveAlarm | POST `/equipment/alarm/receive` | `{alarmId, operator}` | 接收报警 |
| equipment.resolveAlarm | POST `/equipment/alarm/resolve` | `{alarmId, remark?, operator}` | 解除报警 |
| **equipment.startMaintenance** | POST `/equipment/startMaintenance` | `{equipmentId, alarmId?, maintenanceType?, faultDescription?, maintenanceContent?, operator}` | 开始维保 → 维保中 |
| **equipment.finishMaintenance** | POST `/equipment/maintenance/finish` | `{maintenanceId, result?, costAmount?, maintenanceContent?, operator}` | 完成维保 → 恢复 + 关闭报警 |

---

## 4. 售后 AFTERSALES  （base: `/afterSales`，write body = JSON）

### read
| 工具 | 方法 · 路径 | 参数 | 说明 |
|---|---|---|---|
| aftersale.kpi | GET `/afterSales/kpi` | — | 售后案例 KPI |
| aftersale.caseViews | GET `/afterSales/case/views` | — | 售后案例列表 |
| aftersale.trace | GET `/afterSales/case/trace` | caseNo | 质量追溯链（回溯到工单/质检） |

### write（需闸门）
| 工具 | 方法 · 路径 | 参数（body） | 说明 |
|---|---|---|---|
| aftersale.accept | POST `/afterSales/case/accept` | `{caseNo, operator}` | 受理案例 |
| aftersale.resolve | POST `/afterSales/case/resolve` | `{caseNo, solution?, traceResult?, operator}` | 标记解决 + 追溯结论 |
| aftersale.close | POST `/afterSales/case/close` | `{caseNo, remark?, operator}` | 关闭案例 |

---

## 5. 财务 COST  （base: `/cost`，write body = JSON）

### read
| 工具 | 方法 · 路径 | 参数 | 说明 |
|---|---|---|---|
| cost.kpi | GET `/cost/kpi` | — | 成本结算 KPI |
| cost.settlementViews | GET `/cost/settlement/views` | — | 结算单列表 |
| cost.group | GET `/cost/group` | — | 按来源类型成本归集汇总 |

### write（需闸门）
| 工具 | 方法 · 路径 | 参数（body） | 说明 |
|---|---|---|---|
| **cost.confirm** | POST `/cost/settlement/confirm` | `{settlementId, operator}` | 确认结算 |
| cost.export | POST `/cost/settlement/export` | `{settlementId, operator}` | 导出结算单 |

---

## 附：function-calling 工具描述示例（供 Agent 端直接改造）

```json
{
  "type": "function",
  "function": {
    "name": "quality_fail",
    "description": "质检判定不通过，并生成一条不良品记录。属于写操作，必须经人工闸门确认后调用。",
    "parameters": {
      "type": "object",
      "properties": {
        "inspectionId": { "type": "integer", "description": "质检单ID" },
        "defectReason": { "type": "string", "description": "不良原因，必填" },
        "defectQuantity": { "type": "number", "description": "不良数量，>0" },
        "severity": { "type": "string", "enum": ["MINOR","GENERAL","MAJOR","CRITICAL"] },
        "defectType": { "type": "string" },
        "remark": { "type": "string" },
        "operator": { "type": "string", "description": "当前登录用户名" }
      },
      "required": ["inspectionId", "defectReason", "defectQuantity", "operator"]
    }
  }
}
```

> 落地时：把上表每个 **write 工具**都写成这种 JSON 塞给 deepseek 的 `tools`；
> read 工具可让 Agent 自由调用来"感知"，write 工具的调用先落成"提议"（proposal），
> 经 `agent_flow_step` 的人工决策后再真正打后端接口。
