import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

/** 语音 → 文本（multipart 上传 16k WAV 录音；mock 模式返回样例文本） */
export function asr(blob) {
  const fd = new FormData()
  fd.append('file', blob, 'cmd.wav')
  return request.post('/assistant/asr', fd)
}

/** 文本 → 意图（读=直接应答；写=返回待确认提议 {type:'confirm',proposalId,...}；module=当前页面模块，供意图偏置） */
export function interpret(data) {
  // 全局助手每轮需分析全厂快照和完整对话；0 表示不由浏览器端中断模型思考。
  return request.post('/assistant/interpret', data, { timeout: 0 })
}

/**
 * 文本 → 真流式问答（NDJSON）。
 * onDelta 每收到一段纯文本立即触发；最终返回与 interpret 相同的 answer/confirm 结构。
 */
export async function interpretStream(data, { onDelta } = {}) {
  const userStore = useUserStore()
  const response = await fetch('/api/assistant/interpret/stream', {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/x-ndjson',
      ...(userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {})
    },
    body: JSON.stringify(data)
  })

  if (!response.ok) {
    if (response.status === 401) userStore.logout()
    let message = `请求失败（HTTP ${response.status}）`
    try {
      const errorBody = await response.json()
      message = errorBody?.message || message
    } catch { /* 非 JSON 错误响应 */ }
    throw new Error(message)
  }
  if (!response.body) throw new Error('当前浏览器不支持流式响应')

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let result = null

  const consumeLine = (line) => {
    if (!line.trim()) return
    const event = JSON.parse(line)
    if (event.type === 'delta') onDelta?.(event.text || '')
    else if (event.type === 'result') result = event.data
    else if (event.type === 'error') throw new Error(event.message || '流式请求失败')
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    let newline
    while ((newline = buffer.indexOf('\n')) >= 0) {
      const line = buffer.slice(0, newline)
      buffer = buffer.slice(newline + 1)
      consumeLine(line)
    }
    if (done) break
  }
  if (buffer.trim()) consumeLine(buffer)
  if (!result) throw new Error('流式响应未返回最终结果')
  return result
}

/** 人工闸门决策 → 执行（decision: APPROVE / MODIFY / SKIP；多步流时返回 next 自动推进） */
export function execute(data) {
  return request.post('/assistant/execute', data)
}

/** 跨模块协办通知列表（GlobalBusinessMonitor 轮询并入通知中心，按角色过滤） */
export function fetchVoiceNotices() {
  return request.get('/assistant/notices')
}
