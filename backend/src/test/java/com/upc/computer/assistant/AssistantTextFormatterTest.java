package com.upc.computer.assistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantTextFormatterTest {

    @Test
    void convertsMarkdownHeadingsTablesAndEmphasisToPlainText() {
        String markdown = """
                ### 订单结论
                **订单最多的客户**是星辰俱乐部。

                | 客户 | 订单数 |
                |---|---:|
                | 星辰俱乐部 | 6 |
                - 建议优先跟进交期
                """;

        String text = AssistantTextFormatter.toPlainText(markdown);

        assertTrue(text.contains("订单结论"));
        assertTrue(text.contains("客户；订单数"));
        assertTrue(text.contains("星辰俱乐部；6"));
        assertTrue(text.contains("• 建议优先跟进交期"));
        assertFalse(text.contains("###"));
        assertFalse(text.contains("**"));
        assertFalse(text.contains("|---"));
    }

    @Test
    void streamingFormatterHandlesMarkdownMarkersSplitAcrossChunks() {
        StringBuilder emitted = new StringBuilder();
        AssistantTextFormatter.Stream stream = new AssistantTextFormatter.Stream(emitted::append);
        stream.accept("### 订单");
        stream.accept("结论\n**星辰");
        stream.accept("俱乐部**最多");

        String answer = stream.finish();

        assertEquals("订单结论\n星辰俱乐部最多", answer);
        assertEquals(answer, emitted.toString());
    }

    @Test
    void streamingFormatterEmitsLongParagraphBeforeFinishWithoutWaitingForNewline() {
        StringBuilder emitted = new StringBuilder();
        AssistantTextFormatter.Stream stream = new AssistantTextFormatter.Stream(emitted::append);

        stream.accept("这是一段没有换行但长度足够的回答，应该在模型仍然生成后续内容时就先显示前面的纯文本内容。随后还会继续输出更多分析。 ");

        assertFalse(emitted.isEmpty(), "长段落必须在 finish 前产生增量，不能退化为一次性输出");
        String answer = stream.finish();
        assertEquals("这是一段没有换行但长度足够的回答，应该在模型仍然生成后续内容时就先显示前面的纯文本内容。随后还会继续输出更多分析。", answer);
        assertEquals(answer, emitted.toString());
    }
}
