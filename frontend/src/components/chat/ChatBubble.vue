<script setup>
/**
 * 通用消息展示（全角色助手对话复用）。
 * 用户：右对齐浅橙气泡；助手：左对齐自然段落排版，无模型 Logo。
 */
import { computed } from 'vue'

const props = defineProps({
  role: { type: String, required: true },
  text: { type: String, default: '' }
})

/** 将纯文本拆成段落 / 列表 / 简易表格块（兼容单换行编号列表） */
const blocks = computed(() => {
  const raw = props.text || ''
  if (!raw.trim()) return []

  const lines = raw.split('\n')
  const result = []
  let i = 0

  const isNumbered = (l) => /^\d+[.、)\]]\s*/.test(l.trim())
  const isBullet = (l) => /^[-•*]\s+/.test(l.trim())
  const isTableRow = (l) => (l.match(/[丨|]/g) || []).length >= 2

  while (i < lines.length) {
    while (i < lines.length && !lines[i].trim()) i++
    if (i >= lines.length) break

    const line = lines[i].trim()

    if (isNumbered(line)) {
      const items = []
      while (i < lines.length && isNumbered(lines[i])) {
        items.push(lines[i].trim().replace(/^\d+[.、)\]]\s*/, ''))
        i++
      }
      result.push({ type: 'ol', items })
      continue
    }

    if (isBullet(line)) {
      const items = []
      while (i < lines.length && isBullet(lines[i])) {
        items.push(lines[i].trim().replace(/^[-•*]\s+/, ''))
        i++
      }
      result.push({ type: 'ul', items })
      continue
    }

    if (isTableRow(line)) {
      const rows = []
      while (i < lines.length && lines[i].trim() && isTableRow(lines[i])) {
        rows.push(lines[i].split(/[丨|]/).map((c) => c.trim()).filter(Boolean))
        i++
      }
      result.push({ type: 'table', rows })
      continue
    }

    const paraLines = []
    while (
      i < lines.length
      && lines[i].trim()
      && !isNumbered(lines[i])
      && !isBullet(lines[i])
      && !isTableRow(lines[i])
    ) {
      paraLines.push(lines[i].trim())
      i++
    }
    if (paraLines.length) result.push({ type: 'p', lines: paraLines })
  }

  return result
})
</script>

<template>
  <div :class="['chat-msg', role]">
    <div v-if="role === 'user'" class="chat-msg__user-bubble">{{ text }}</div>

    <div v-else class="chat-msg__assistant-body">
      <template v-if="blocks.length">
        <template v-for="(block, bi) in blocks" :key="bi">
          <p v-if="block.type === 'p'" class="chat-msg__para">
            <template v-for="(line, li) in block.lines" :key="li">
              <span v-if="li > 0"><br /></span>{{ line }}
            </template>
          </p>
          <ol v-else-if="block.type === 'ol'" class="chat-msg__list chat-msg__list--ol">
            <li v-for="(item, ii) in block.items" :key="ii">{{ item }}</li>
          </ol>
          <ul v-else-if="block.type === 'ul'" class="chat-msg__list chat-msg__list--ul">
            <li v-for="(item, ii) in block.items" :key="ii">{{ item }}</li>
          </ul>
          <div v-else-if="block.type === 'table'" class="chat-msg__table-wrap">
            <table class="chat-msg__table">
              <tbody>
                <tr v-for="(row, ri) in block.rows" :key="ri">
                  <td v-for="(cell, ci) in row" :key="ci">{{ cell }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </template>
      <p v-else-if="text" class="chat-msg__para">{{ text }}</p>
    </div>
  </div>
</template>

<style scoped>
.chat-msg {
  font-family: Inter, "PingFang SC", "Noto Sans SC", "Microsoft YaHei", system-ui, sans-serif;
  width: 100%;
  display: flex;
}
.chat-msg.user {
  justify-content: flex-end;
}
.chat-msg.assistant {
  justify-content: flex-start;
}

.chat-msg__user-bubble {
  max-width: min(85%, 640px);
  padding: 10px 14px;
  border-radius: 14px;
  background: #fff1df;
  border: 1px solid #f5e4cc;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.7;
  color: #1f2937;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-msg__assistant-body {
  width: 100%;
  max-width: 100%;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.7;
  color: #2f2f2f;
  word-break: break-word;
}

.chat-msg__para {
  margin: 0 0 12px;
}
.chat-msg__para:last-child {
  margin-bottom: 0;
}

.chat-msg__list {
  margin: 0 0 12px;
  padding-left: 1.35em;
}
.chat-msg__list:last-child {
  margin-bottom: 0;
}
.chat-msg__list li {
  margin-bottom: 6px;
  padding-left: 4px;
}
.chat-msg__list li:last-child {
  margin-bottom: 0;
}
.chat-msg__list--ol {
  list-style-type: decimal;
}
.chat-msg__list--ul {
  list-style-type: disc;
}

.chat-msg__table-wrap {
  margin: 0 0 12px;
  overflow-x: auto;
  width: 100%;
}
.chat-msg__table-wrap:last-child {
  margin-bottom: 0;
}
.chat-msg__table {
  width: 100%;
  min-width: 280px;
  border-collapse: collapse;
  font-size: 14px;
  line-height: 1.6;
}
.chat-msg__table td {
  padding: 8px 12px;
  border: 1px solid #dce3e8;
  vertical-align: top;
  color: #2f2f2f;
  white-space: normal;
  word-break: break-word;
}
.chat-msg__table tr:first-child td {
  background: #f8fafb;
  font-weight: 500;
  color: #1f2937;
}
</style>
