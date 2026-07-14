import request from '@/utils/request'

/** 语音 → 文本（multipart 上传 16k WAV 录音；mock 模式返回样例文本） */
export function asr(blob) {
  const fd = new FormData()
  fd.append('file', blob, 'cmd.wav')
  return request.post('/assistant/asr', fd)
}

/** 文本 → 意图（读=直接应答；写=返回待确认提议 {type:'confirm',proposalId,...}；module=当前页面模块，供意图偏置） */
export function interpret(data) {
  return request.post('/assistant/interpret', data)
}

/** 人工闸门决策 → 执行（decision: APPROVE / MODIFY / SKIP；多步流时返回 next 自动推进） */
export function execute(data) {
  return request.post('/assistant/execute', data)
}

/** 跨模块协办通知列表（GlobalBusinessMonitor 轮询并入通知中心，按角色过滤） */
export function fetchVoiceNotices() {
  return request.get('/assistant/notices')
}
