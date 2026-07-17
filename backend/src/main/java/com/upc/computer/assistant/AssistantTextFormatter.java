package com.upc.computer.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** 将模型可能返回的 Markdown 规整为聊天气泡适合显示的纯文本。 */
final class AssistantTextFormatter {

    private static final Pattern TABLE_SEPARATOR = Pattern.compile(
            "^\\s*\\|?\\s*:?-{3,}:?\\s*(?:\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$");
    private static final Pattern HORIZONTAL_RULE = Pattern.compile("^\\s*([-*_])(?:\\s*\\1){2,}\\s*$");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)");

    private AssistantTextFormatter() {
    }

    static String toPlainText(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder result = new StringBuilder();
        boolean previousBlank = false;
        for (String sourceLine : normalized.split("\n", -1)) {
            String line = cleanLine(sourceLine);
            if (line == null) continue;
            boolean blank = line.isBlank();
            if (blank && (previousBlank || result.length() == 0)) continue;
            if (result.length() > 0) result.append('\n');
            if (!blank) result.append(line);
            previousBlank = blank;
        }
        return result.toString().strip();
    }

    private static String cleanLine(String sourceLine) {
        String line = sourceLine == null ? "" : sourceLine.stripTrailing();
        String trimmed = line.trim();
        if (trimmed.startsWith("```")) return null;
        if (TABLE_SEPARATOR.matcher(trimmed).matches() || HORIZONTAL_RULE.matcher(trimmed).matches()) return null;

        line = line.replaceFirst("^\\s{0,3}#{1,6}\\s*", "");
        line = line.replaceFirst("^\\s*>\\s?", "");
        line = line.replaceFirst("^\\s*[-*+]\\s+", "• ");

        String tableCandidate = line.trim();
        long pipeCount = tableCandidate.chars().filter(ch -> ch == '|').count();
        if (pipeCount >= 2) {
            String body = tableCandidate;
            if (body.startsWith("|")) body = body.substring(1);
            if (body.endsWith("|")) body = body.substring(0, body.length() - 1);
            List<String> cells = new ArrayList<>();
            for (String cell : body.split("\\|")) {
                String value = cleanInline(cell.trim());
                if (!value.isBlank()) cells.add(value);
            }
            return String.join("；", cells);
        }
        return cleanInline(line).stripTrailing();
    }

    private static String cleanInline(String value) {
        String line = MARKDOWN_LINK.matcher(value).replaceAll("$1（$2）");
        line = line.replace("**", "").replace("__", "").replace("~~", "").replace("`", "");
        line = line.replaceAll("(?<!\\w)\\*([^*]+)\\*(?!\\w)", "$1");
        return line;
    }

    /**
     * 流式纯文本清洗器。
     *
     * 每次收到模型分片后都重新渲染当前完整文本，并仅保留末尾少量字符作为稳定缓冲。
     * 这样既能处理被拆到两个网络分片中的 Markdown 标记，也不会为了等待换行而阻塞整段回答。
     */
    static final class Stream {
        private static final int STABILITY_MARGIN = 24;

        private final Consumer<String> downstream;
        private final StringBuilder rawText = new StringBuilder();
        private final StringBuilder emittedText = new StringBuilder();
        private boolean finished;

        Stream(Consumer<String> downstream) {
            this.downstream = downstream == null ? ignored -> { } : downstream;
        }

        void accept(String chunk) {
            if (finished || chunk == null || chunk.isEmpty()) return;
            rawText.append(chunk.replace("\r\n", "\n").replace('\r', '\n'));
            flush(false);
        }

        String finish() {
            if (!finished) {
                flush(true);
                finished = true;
            }
            return emittedText.toString();
        }

        private void flush(boolean finalFlush) {
            String cleaned = toPlainText(rawText.toString());
            if (!cleaned.startsWith(emittedText.toString())) {
                // 极少数尚未闭合的复杂 Markdown（例如超长链接）可能改写已渲染前缀。
                // 此时先等待更多内容，避免向客户端重复输出或破坏已经显示的正文。
                return;
            }

            int targetLength = finalFlush ? cleaned.length() : stableLength(cleaned);
            if (targetLength <= emittedText.length()) return;

            String delta = cleaned.substring(emittedText.length(), targetLength);
            downstream.accept(delta);
            emittedText.append(delta);
        }

        private int stableLength(String cleaned) {
            int emittedLength = emittedText.length();

            // 已经完整生成的行可以立即发送，不必继续保留稳定缓冲。
            int lastNewline = cleaned.lastIndexOf('\n');
            int stable = lastNewline >= emittedLength
                    ? lastNewline + 1
                    : Math.max(emittedLength, cleaned.length() - STABILITY_MARGIN);

            // 尚未闭合的内联 Markdown 从标记处开始保持在缓冲区，等待后续分片完成清洗。
            int currentLineStart = cleaned.lastIndexOf('\n') + 1;
            for (char marker : new char[]{'[', '*', '_', '`', '~'}) {
                int markerIndex = cleaned.lastIndexOf(marker);
                if (markerIndex >= currentLineStart) stable = Math.min(stable, markerIndex);
            }
            return Math.max(emittedLength, stable);
        }
    }
}
