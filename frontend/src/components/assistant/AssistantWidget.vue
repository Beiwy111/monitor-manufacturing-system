<script setup>
import { computed, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Microphone, ChatDotRound, Promotion, Close } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useMesStore } from '@/stores/mes'
import { asr, interpret, execute } from '@/api/assistant'

const emit = defineEmits(['executed'])

const user = useUserStore()
const mes = useMesStore()
const route = useRoute()
const sessionId = `${user.userInfo?.username || 'anon'}-${Date.now()}`

const open = ref(false)
const messages = ref([{ role: 'assistant', text: '你好，我是 MES 全局助手。查询研判全局可用：概况、售后追溯/研判（"你觉得怎么处理"）、设备诊断（"EQ-xxx 健康怎么样"）。写操作只在所属模块页面可执行，会先请你确认；跨模块请用协办通知（"通知生产…"）。还支持一句话串多步，如"受理星辰这单并启动根因协查"。' }])
const confirmCard = ref(null)   // { proposalId, action, humanReadable, editable? }
const editParam = ref('')       // 确认卡里可选的参数修改（editable.key 指定字段）
const inputText = ref('')
const recording = ref(false)
const busy = ref(false)
const listEl = ref(null)

const currentModule = computed(() => {
  const path = route.path
  if (path.includes('aftersale')) return 'aftersale'
  if (path.includes('device')) return 'device'
  if (path.includes('quality')) return 'quality'
  if (path.includes('purchase')) return 'purchase'
  if (path.includes('warehouse')) return 'warehouse'
  if (path.includes('production')) return 'production'
  if (path.includes('order') || path.includes('delivery')) return 'order'
  if (path.includes('cost')) return 'cost'
  if (path.includes('system')) return 'system'
  return user.roleKey
})

function push(role, text) {
  messages.value.push({ role, text })
  nextTick(() => { if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight })
}

async function send(text) {
  const t = (text ?? inputText.value).trim()
  if (!t || busy.value) return
  inputText.value = ''
  push('user', t)
  busy.value = true
  try {
    const r = await interpret({ sessionId, text: t, module: currentModule.value })
    handle(r)
  } catch (e) {
    push('assistant', '出错了：' + (e.message || '请求失败'))
  } finally {
    busy.value = false
  }
}

function handle(r) {
  if (r.reply) push('assistant', r.reply)
  if (r.type === 'confirm') {
    confirmCard.value = r
    editParam.value = ''
  }
}

async function onApprove(decision) {
  if (!confirmCard.value || busy.value) return
  busy.value = true
  const card = confirmCard.value
  const body = {
    proposalId: card.proposalId,
    decision,
    operator: user.userInfo?.username || '',
    roleKey: user.roleKey || ''
  }
  if (decision === 'MODIFY' && editParam.value.trim() && card.editable?.key) {
    body.finalParams = { [card.editable.key]: editParam.value.trim() }
  }
  try {
    const res = await execute(body)
    push('assistant', res.reply)
    confirmCard.value = null
    if (res.ok) {
      ElMessage.success((res.reply || '已执行').split('\n')[0])
      mes.hydrateFromApi().catch(() => {})   // 刷新各页面共享的 MES 快照数据
      emit('executed', res.data)
    }
    // 多步流：本步执行完，后端直接推进下一步（读步的应答或下一张确认卡）
    if (res.next) handle(res.next)
  } catch (e) {
    push('assistant', '执行失败：' + (e.message || ''))
  } finally {
    busy.value = false
  }
}

function cancelConfirm() {
  if (confirmCard.value) onApprove('SKIP')
}

// 麦克风：点击开始 / 再点停止（比"按住说话"稳，避开授权异步与 mouseleave 的时序问题）
async function toggleMic() {
  if (busy.value) return
  if (!recording.value) {
    if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
      ElMessage.warning('当前为普通 HTTP 访问，浏览器禁用了麦克风；文字助手仍可正常使用。请改用 HTTPS 或 localhost 后录音。')
      return
    }
    try {
      const { startRecording } = await import('@/utils/recorder')
      await startRecording()
      recording.value = true
    } catch (e) {
      ElMessage.error(e.message || '无法打开麦克风，请检查浏览器授权')
    }
    return
  }
  // 停止并识别
  recording.value = false
  busy.value = true
  let text = ''
  try {
    const { stopRecording } = await import('@/utils/recorder')
    const blob = await stopRecording()
    console.log('[assistant] 录音大小', blob?.size, 'bytes')
    if (!blob || blob.size < 2000) {
      busy.value = false
      push('assistant', '没录到声音。点一下麦克风开始，说完再点一次停止（别一点就松）。')
      return
    }
    const res = await asr(blob)
    text = res.text
  } catch (e) {
    busy.value = false
    push('assistant', '识别失败：' + (e.message || ''))
    return
  }
  busy.value = false
  if (text) await send(text)
  else push('assistant', '没识别到内容，请靠近麦克风重试。')
}
</script>

<template>
  <div class="assistant-fab">
    <!-- 悬浮开关 -->
    <el-button v-if="!open" type="primary" circle size="large" :icon="ChatDotRound" @click="open = true" />

    <div v-else class="assistant-panel">
      <div class="ap-head">
        <span><el-icon><ChatDotRound /></el-icon> MES 全局助手 · {{ currentModule }}</span>
        <el-icon class="ap-close" @click="open = false"><Close /></el-icon>
      </div>

      <div ref="listEl" class="ap-body">
        <div v-for="(m, i) in messages" :key="i" :class="['ap-bubble', m.role]">{{ m.text }}</div>

        <!-- 人工闸门确认卡 -->
        <div v-if="confirmCard" class="ap-gate">
          <pre>{{ confirmCard.humanReadable }}</pre>
          <el-input
            v-if="confirmCard.editable"
            v-model="editParam" size="small" type="textarea" :rows="2"
            :placeholder="confirmCard.editable.placeholder" />
          <div class="ap-gate-btns">
            <el-button type="primary" size="small" :loading="busy"
              @click="onApprove(editParam.trim() && confirmCard.editable ? 'MODIFY' : 'APPROVE')">确认执行</el-button>
            <el-button size="small" @click="cancelConfirm">取消</el-button>
          </div>
        </div>
      </div>

      <div class="ap-input">
        <el-input v-model="inputText" size="default" placeholder="说人话或直接输入指令…"
          :disabled="busy" @keyup.enter="send()" />
        <el-button :icon="Promotion" :disabled="busy" @click="send()" />
        <el-button :type="recording ? 'danger' : 'default'" :icon="Microphone"
          :loading="busy && !recording" @click="toggleMic">
          {{ recording ? '停止' : '' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.assistant-fab { position: fixed; right: 28px; bottom: 32px; z-index: 3000; }
.assistant-panel {
  width: 360px; height: 520px; display: flex; flex-direction: column;
  background: #fff; border-radius: 14px; box-shadow: 0 12px 40px rgba(0,0,0,.18); overflow: hidden;
}
.ap-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; background: linear-gradient(135deg,#2b6cff,#5b8cff); color: #fff; font-weight: 600;
}
.ap-head span { display: flex; align-items: center; gap: 6px; }
.ap-close { cursor: pointer; }
.ap-body { flex: 1; overflow-y: auto; padding: 14px; background: #f5f7fb; }
.ap-bubble { max-width: 80%; padding: 8px 12px; border-radius: 10px; margin-bottom: 10px; font-size: 13px; line-height: 1.5; white-space: pre-wrap; word-break: break-word; }
.ap-bubble.assistant { background: #fff; color: #1f2937; border: 1px solid #e5e7eb; }
.ap-bubble.user { background: #2b6cff; color: #fff; margin-left: auto; }
.ap-gate { background: #fff; border: 1px solid #ffd591; border-radius: 10px; padding: 10px; margin-bottom: 10px; }
.ap-gate pre { margin: 0 0 8px; font-size: 13px; line-height: 1.5; white-space: pre-wrap; font-family: inherit; color: #7c3a00; }
.ap-gate-btns { display: flex; gap: 8px; margin-top: 8px; }
.ap-input { display: flex; gap: 6px; padding: 10px; border-top: 1px solid #eee; background: #fff; }
.ap-input .el-input { flex: 1; }
</style>
