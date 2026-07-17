package com.upc.computer.assistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantAnswerDetailTest {

    @Test
    void usesDetailedAnswersByDefaultUnlessUserExplicitlyRequestsSummary() {
        assertTrue(AssistantService.wantsDetailedAnswer("详细说一下，列出来"));
        assertTrue(AssistantService.wantsDetailedAnswer("请逐项展开库存明细"));
        assertTrue(AssistantService.wantsDetailedAnswer("给出完整清单，不要省略"));
        assertTrue(AssistantService.wantsDetailedAnswer("现在库存怎么样？"));
        assertFalse(AssistantService.wantsDetailedAnswer("简单概括一下，只说结论"));
        assertFalse(AssistantService.isSubstantiveAnswer("收到。"));
        assertFalse(AssistantService.isSubstantiveAnswer("我能处理这些功能，请换个说法。"));
        assertTrue(AssistantService.isSubstantiveAnswer("当前库存共有16条，其中正常11条、预警5条，以下按物料逐项列出。"));

        assertFalse(AssistantService.isExplicitWriteRequest("订单有哪些？订单最多的客户是谁？"));
        assertFalse(AssistantService.isExplicitWriteRequest("列出当前库存"));
        assertFalse(AssistantService.isExplicitWriteRequest("已关闭订单有哪些"));
        assertTrue(AssistantService.isExplicitWriteRequest("确认采购单 PO-001 到货"));
        assertTrue(AssistantService.isExplicitWriteRequest("帮我关闭报警 ALM-001"));
        assertTrue(AssistantService.isExplicitWriteRequest("通知生产部门暂停派工"));
    }
}
