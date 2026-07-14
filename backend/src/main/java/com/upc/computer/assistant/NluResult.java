package com.upc.computer.assistant;

import java.util.List;

/**
 * NLU 解析结果。action 归一到全局动作码（aftersale.* / device.* / production.* / purchase.* /
 * warehouse.* / *.overview / notify.send / unknown）；写操作缺参时用 missingSlots 标记需追问的槽位。
 * 一句话串联多个动作时 steps 按顺序放子指令（每项也是 NluResult，不再嵌套 steps）。
 */
public record NluResult(
        String action,          // 全局动作码，见 MesActionCatalog.nluCatalogText()
        String caseNo,          // 用户直接报的案例号/单号（AS202607002、ALM-2026-001…），否则空
        String customerName,    // 定位线索：客户名片段（售后）
        String problemTypeCn,   // 定位线索：中文问题类型（售后）
        String keyword,         // 定位线索：其它关键词（设备名/物料/工序/供应商/描述片段）
        String solution,        // aftersale.resolve 的解决方案
        String traceResult,     // aftersale.resolve 的追溯结论（可选）
        String remark,          // 关闭/解除类动作的备注；notify.send 的通知内容
        int qty,                // purchase.receive 的到货数量（0=未说）
        String departments,     // aftersale.rca_dispatch 提到的部门（中文逗号分隔，空=默认）
        String targetModule,    // notify.send 的目标模块码（production/purchase/...，空=需追问）
        List<NluResult> steps,  // 多步指令（≥2 时生效），单动作为空列表
        List<String> missingSlots,
        double confidence,
        String reply            // 给用户的一句中文回应
) {
    public boolean isAftersaleWrite() {
        return "aftersale.accept".equals(action) || "aftersale.resolve".equals(action)
                || "aftersale.close".equals(action);
    }
}
