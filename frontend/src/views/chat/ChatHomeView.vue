<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  MagicStick, Box, DataAnalysis, Bell, ShoppingCart,
  Monitor, Checked, Van, Tools, Service, Coin, User
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useMesStore } from '@/stores/mes'
import { useChatStore } from '@/stores/chat'
import { asr, interpret, execute } from '@/api/assistant'
import ChatBubble from '@/components/chat/ChatBubble.vue'
import ChatComposer from '@/components/chat/ChatComposer.vue'

const user = useUserStore()
const mes = useMesStore()
const chat = useChatStore()
chat.ensureLoaded()

const confirmCard = ref(null)
const editParam = ref('')
const inputText = ref('')
const busy = ref(false)
const recording = ref(false)
const listEl = ref(null)

const messages = computed(() => chat.activeSession?.messages || [])
const started = computed(() => messages.value.length > 0)

// 切换会话 / 返回主页时，丢弃未处理的确认卡（提议属于旧会话上下文）
watch(() => chat.activeId, () => {
  confirmCard.value = null
  editParam.value = ''
  scrollBottom()
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

/** interpret 的 module 直接传角色码，后端 normalizeModule 会归一（admin→system 全放行） */
const moduleKey = computed(() => user.roleKey || '')

const ROLE_SUGGESTIONS = {
  order: [
    { icon: ShoppingCart, title: '对话下单', text: '给深圳华创下 200 台 27寸4K显示器' },
    { icon: DataAnalysis, title: '订单概况', text: '订单什么情况' },
    { icon: Box, title: '查库存', text: '查库存' },
    { icon: Bell, title: '协办通知', text: '通知计划 新订单已审核，请尽快排产' }
  ],
  admin: [
    { icon: ShoppingCart, title: '对话下单', text: '给深圳华创下 200 台 27寸4K显示器' },
    { icon: Box, title: '查库存', text: '查库存' },
    { icon: DataAnalysis, title: '生产概况', text: '生产什么情况' },
    { icon: User, title: '用户概况', text: '系统里有多少用户' }
  ],
  manager: [
    { icon: DataAnalysis, title: '生产概况', text: '生产什么情况' },
    { icon: Monitor, title: '设备概况', text: '设备什么情况' },
    { icon: Checked, title: '质检概况', text: '质检什么情况' },
    { icon: Box, title: '查库存', text: '查库存' }
  ],
  planner: [
    { icon: DataAnalysis, title: '生产概况', text: '生产什么情况' },
    { icon: ShoppingCart, title: '订单概况', text: '订单什么情况' },
    { icon: Box, title: '查库存', text: '查库存' },
    { icon: Bell, title: '协办通知', text: '通知采购 请关注缺料风险' }
  ],
  operator: [
    { icon: MagicStick, title: '接收派工', text: '接收派工' },
    { icon: DataAnalysis, title: '生产概况', text: '生产什么情况' },
    { icon: Bell, title: '协办通知', text: '通知设备 请检查产线设备状态' },
    { icon: Box, title: '查库存', text: '查库存' }
  ],
  quality: [
    { icon: Checked, title: '质检概况', text: '质检什么情况' },
    { icon: DataAnalysis, title: '生产概况', text: '生产什么情况' },
    { icon: Bell, title: '协办通知', text: '通知生产 不合格品正在复检中' },
    { icon: Box, title: '查库存', text: '查库存' }
  ],
  warehouse: [
    { icon: Box, title: '查库存', text: '查库存' },
    { icon: Box, title: '物料查询', text: 'MAT-001 还有多少' },
    { icon: Van, title: '仓库概况', text: '仓库什么情况' },
    { icon: Bell, title: '协办通知', text: '通知采购 部分物料低于安全库存' }
  ],
  purchase: [
    { icon: ShoppingCart, title: '采购概况', text: '采购什么情况' },
    { icon: Box, title: '查库存', text: '查库存' },
    { icon: Van, title: '到货确认', text: '采购单到货了' },
    { icon: Bell, title: '协办通知', text: '通知仓库 明天有一批物料到货' }
  ],
  device: [
    { icon: Monitor, title: '设备概况', text: '设备什么情况' },
    { icon: Bell, title: '报警查询', text: '现在有多少报警' },
    { icon: Tools, title: '设备诊断', text: 'EQ-001 健康怎么样' },
    { icon: Bell, title: '协办通知', text: '通知生产 设备维护中请暂缓排产' }
  ],
  aftersale: [
    { icon: Service, title: '售后概况', text: '售后什么情况' },
    { icon: MagicStick, title: '受理工单', text: '受理最新的售后单' },
    { icon: DataAnalysis, title: '客户追溯', text: '查一下星辰的售后' },
    { icon: Bell, title: '协办通知', text: '通知质检 请协查售后质量问题' }
  ],
  cost: [
    { icon: Coin, title: '成本概况', text: '成本什么情况' },
    { icon: ShoppingCart, title: '订单概况', text: '订单什么情况' },
    { icon: DataAnalysis, title: '生产概况', text: '生产什么情况' },
    { icon: Bell, title: '协办通知', text: '通知订单 请核对本月订单金额' }
  ]
}
const suggestions = computed(() => {
  if (user.roleKey === 'customer') {
    const name = user.userInfo?.customerName || user.displayName
    return [
      { icon: ShoppingCart, title: '对话下单', text: `给${name}下 100 台 27寸4K显示器` },
      { icon: DataAnalysis, title: '订单进展', text: '订单什么情况' },
      { icon: Monitor, title: '看看能订什么', text: '我要下一个订单' },
      { icon: Service, title: '售后进展', text: '售后什么情况' }
    ]
  }
  return ROLE_SUGGESTIONS[user.roleKey] || ROLE_SUGGESTIONS.admin
})

function scrollBottom() {
  nextTick(() => { if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight })
}

function push(role, text) {
  chat.pushMessage(role, text)
  scrollBottom()
}

async function send(text) {
  const t = (text ?? inputText.value).trim()
  if (!t || busy.value) return
  inputText.value = ''
  push('user', t)
  busy.value = true
  try {
    const r = await interpret({
      sessionId: chat.activeSession?.apiSessionId,
      text: t,
      module: moduleKey.value
    })
    handle(r)
  } catch (e) {
    push('assistant', '出错了：' + (e.message || '请求失败'))
  } finally {
    busy.value = false
    scrollBottom()
  }
}

function handle(r) {
  if (r.reply) push('assistant', r.reply)
  if (r.type === 'confirm') {
    confirmCard.value = r
    editParam.value = ''
    scrollBottom()
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
      mes.hydrateFromApi({ force: true }).catch(() => {})
    }
    if (res.next) handle(res.next)
  } catch (e) {
    push('assistant', '执行失败：' + (e.message || ''))
  } finally {
    busy.value = false
    scrollBottom()
  }
}

function cancelConfirm() {
  if (confirmCard.value) onApprove('SKIP')
}

async function toggleMic() {
  if (busy.value) return
  if (!recording.value) {
    if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
      ElMessage.warning('当前为普通 HTTP 访问，浏览器禁用了麦克风；请改用 HTTPS 或 localhost 后录音，文字对话不受影响。')
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
  recording.value = false
  busy.value = true
  let text = ''
  try {
    const { stopRecording } = await import('@/utils/recorder')
    const blob = await stopRecording()
    if (!blob || blob.size < 2000) {
      busy.value = false
      push('assistant', '没录到声音。点一下麦克风开始，说完再点一次停止。')
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
  <div class="chat-home" :class="{ 'is-started': started }">
    <!-- ═══ 欢迎态：居中问候 + 大输入框 + 建议卡 ═══ -->
    <div v-if="!started" class="ch-hero">
      <div class="ch-badge">MES 智能工作台</div>
      <h1 class="ch-greet">
        <span class="ch-star">✳</span>
        {{ greeting }}，{{ user.displayName }}
      </h1>

      <ChatComposer
        v-model="inputText"
        variant="hero"
        :busy="busy"
        :recording="recording"
        placeholder="今天想完成什么？直接用一句话描述，例如「给深圳华创下 200 台 27寸4K显示器」"
        @send="send()"
        @mic="toggleMic"
      >
        <template #meta>{{ user.userInfo?.roleName || '成员' }} · 按角色执行，写操作先确认</template>
      </ChatComposer>

      <div class="ch-suggest-head">试试这些</div>
      <div class="ch-suggest-grid">
        <button v-for="(s, i) in suggestions" :key="i" class="ch-suggest" @click="send(s.text)">
          <el-icon class="ch-suggest-icon"><component :is="s.icon" /></el-icon>
          <span class="ch-suggest-title">{{ s.title }}</span>
          <span class="ch-suggest-text">{{ s.text }}</span>
        </button>
      </div>
    </div>

    <!-- ═══ 对话态：消息流 + 底部输入 ═══ -->
    <template v-else>
      <div ref="listEl" class="ch-thread">
        <div class="ch-thread-inner">
          <ChatBubble v-for="(m, i) in messages" :key="i" :role="m.role" :text="m.text" />

          <!-- 人工闸门确认卡 -->
          <div v-if="confirmCard" class="ch-gate">
            <div class="ch-gate-title">待你确认</div>
            <pre class="ch-gate-body">{{ confirmCard.humanReadable }}</pre>
            <el-input
              v-if="confirmCard.editable"
              v-model="editParam" size="small" type="textarea" :rows="2"
              :placeholder="confirmCard.editable.placeholder" />
            <div class="ch-gate-btns">
              <el-button type="primary" :loading="busy"
                @click="onApprove(editParam.trim() && confirmCard.editable ? 'MODIFY' : 'APPROVE')">确认执行</el-button>
              <el-button @click="cancelConfirm">取消</el-button>
            </div>
          </div>

          <div v-if="busy && !confirmCard" class="ch-typing">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <div class="ch-footer">
        <ChatComposer
          v-model="inputText"
          variant="slim"
          class="ch-footer-composer"
          :busy="busy"
          :recording="recording"
          placeholder="继续对话，回车发送…"
          @send="send()"
          @mic="toggleMic"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.chat-home {
  min-height: calc(100vh - 96px);
  display: flex;
  flex-direction: column;
  font-family: inherit;
}

/* ─── 欢迎态 ─── */
.ch-hero {
  flex: 1;
  max-width: 780px;
  width: 100%;
  margin: 0 auto;
  padding: 48px 24px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.ch-badge {
  padding: 5px 14px;
  border-radius: 999px;
  background: rgba(122, 110, 228, 0.12);
  color: #6c5fc7;
  font-size: 12px;
  letter-spacing: 1px;
  margin-bottom: 26px;
}
.ch-greet {
  margin: 0 0 30px;
  font-size: 34px;
  font-weight: 500;
  color: #3d3929;
  display: flex;
  align-items: center;
  gap: 12px;
}
.ch-star { color: #d97757; font-size: 30px; line-height: 1; }

.ch-suggest-head {
  align-self: flex-start;
  margin: 34px 0 12px;
  font-size: 13px;
  color: #8f8975;
}
.ch-suggest-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px;
}
.ch-suggest {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(61, 57, 41, 0.1);
  border-radius: 12px;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s, transform 0.15s, box-shadow 0.15s;
}
.ch-suggest:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(61, 57, 41, 0.08);
}
.ch-suggest-icon { color: #d97757; font-size: 18px; }
.ch-suggest-title { font-size: 13px; font-weight: 600; color: #3d3929; }
.ch-suggest-text {
  font-size: 12px;
  color: #8f8975;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ─── 对话态 ─── */
.ch-thread {
  flex: 1;
  overflow-y: auto;
  padding: 24px 16px 8px;
}
.ch-thread-inner {
  max-width: 780px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ch-gate {
  margin-left: 40px;
  max-width: 78%;
  background: rgba(255, 250, 240, 0.95);
  border: 1px solid #f0ce9a;
  border-radius: 12px;
  padding: 14px 16px;
}
.ch-gate-title { font-size: 12px; font-weight: 700; color: #b07a2c; margin-bottom: 6px; letter-spacing: 1px; }
.ch-gate-body {
  margin: 0 0 10px;
  font-size: 13.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  font-family: inherit;
  color: #6e4a12;
}
.ch-gate-btns { display: flex; gap: 10px; margin-top: 10px; }

.ch-typing { display: flex; gap: 5px; margin-left: 44px; padding: 6px 0; }
.ch-typing span {
  width: 7px; height: 7px; border-radius: 50%;
  background: #c9c2ae;
  animation: ch-blink 1.2s infinite ease-in-out;
}
.ch-typing span:nth-child(2) { animation-delay: 0.2s; }
.ch-typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes ch-blink { 0%, 80%, 100% { opacity: 0.25; } 40% { opacity: 1; } }

.ch-footer {
  position: sticky;
  bottom: 0;
  padding: 10px 16px 18px;
}
.ch-footer-composer {
  max-width: 780px;
  margin: 0 auto;
}
</style>
