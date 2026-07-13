<template>
  <el-drawer
    :model-value="visible"
    title="报警详情"
    size="480px"
    direction="rtl"
    :destroy-on-close="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <template v-if="alarm">
      <!-- 头部：单号 + 级别 + 状态 -->
      <div class="ad-head">
        <div class="ad-head__left">
          <span class="ad-no">{{ alarm.alarmNo }}</span>
          <el-tag :type="levelTagType(alarm.alarmLevel)" size="small" effect="dark" style="margin-left:8px">
            {{ alarm.alarmLevelCn }}
          </el-tag>
          <el-tag :type="statusTagType(alarm.alarmStatus)" size="small" style="margin-left:6px">
            {{ alarm.alarmStatusCn }}
          </el-tag>
        </div>
      </div>

      <div class="ad-eq-row">
        <span class="ad-eq-code">{{ alarm.equipmentCode }}</span>
        <span class="ad-eq-name">{{ alarm.equipmentName }}</span>
      </div>

      <!-- 基本信息 -->
      <div class="ad-section">
        <div class="ad-section__title">基本信息</div>
        <div class="ad-grid">
          <div class="ad-cell"><span class="ad-cell__k">类型</span><span class="ad-cell__v">{{ alarm.alarmTypeCn }}</span></div>
          <div class="ad-cell"><span class="ad-cell__k">级别</span>
            <span class="ad-cell__v" :style="{ color: levelColor(alarm.alarmLevel) }">{{ alarm.alarmLevelCn }}</span>
          </div>
          <div class="ad-cell ad-cell--full">
            <span class="ad-cell__k">描述</span>
            <span class="ad-cell__v ad-cell__v--wrap">{{ alarm.alarmDescription || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 处理时间线 -->
      <div class="ad-section">
        <div class="ad-section__title">处理时间线</div>
        <el-timeline>
          <el-timeline-item
            v-for="node in timeline"
            :key="node.action"
            :type="node.type"
            :hollow="node.hollow"
            :timestamp="node.time"
            placement="top"
          >
            <div class="tl-item">
              <span class="tl-item__action">{{ node.action }}</span>
              <span v-if="node.operator" class="tl-item__op">{{ node.operator }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- 操作区 -->
      <div v-if="!isClosed" class="ad-section ad-actions">
        <div class="ad-section__title">执行操作</div>
        <el-input
          v-model="remark"
          type="textarea"
          :rows="2"
          placeholder="解除/关闭备注（可选）"
          style="margin-bottom:10px"
        />
        <div class="ad-action-btns">
          <el-button
            v-if="alarm.alarmStatus === 'OPEN'"
            type="primary"
            :loading="acting"
            @click="doReceive"
          >接收</el-button>
          <el-button
            v-if="alarm.alarmStatus === 'FAULT' || canStartMaint"
            type="warning"
            :loading="acting"
            @click="$emit('start-maint', alarm)"
          >开始维保</el-button>
          <el-button
            v-if="!isClosed"
            type="info"
            :loading="acting"
            @click="doResolve"
          >解除报警</el-button>
        </div>
      </div>
    </template>

    <el-empty v-else description="请选择一条报警记录" :image-size="80" />
  </el-drawer>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { receiveAlarm, resolveAlarm } from '@/api/business'

const props = defineProps({
  visible: { type: Boolean, default: false },
  alarm:   { type: Object, default: null },
})
const emit = defineEmits(['update:visible', 'refresh', 'start-maint'])

const userStore = useUserStore()
const acting    = ref(false)
const remark    = ref('')

const isClosed = computed(() =>
  props.alarm?.alarmStatus === 'CLOSED' || props.alarm?.alarmStatus === 'CANCELLED'
)
const canStartMaint = computed(() =>
  props.alarm && ['OPEN', 'RECEIVED', 'PROCESSING'].includes(props.alarm.alarmStatus)
)

// 时间线节点
const timeline = computed(() => {
  const a = props.alarm
  if (!a) return []
  const nodes = []
  nodes.push({
    action: '触发报警', operator: a.reporterName,
    time: a.reportedAt ?? '', type: 'danger', hollow: false,
  })
  if (a.receivedAt) nodes.push({
    action: '已接收', operator: a.receiverName,
    time: a.receivedAt, type: 'warning', hollow: false,
  })
  if (a.alarmStatus === 'PROCESSING') nodes.push({
    action: '维保处理中', operator: '—',
    time: '', type: 'primary', hollow: true,
  })
  if (a.closedAt) nodes.push({
    action: `已${a.alarmStatusCn}`, operator: a.closeResult ?? '',
    time: a.closedAt, type: 'success', hollow: false,
  })
  if (!a.closedAt && a.alarmStatus === 'OPEN') nodes.push({
    action: '等待接收处理…', operator: '',
    time: '', type: 'info', hollow: true,
  })
  return nodes
})

function levelColor(l) {
  return { URGENT: '#ef4444', IMPORTANT: '#f59e0b', GENERAL: '#3b82f6' }[l] ?? '#6b7a90'
}
function levelTagType(l) {
  return { URGENT: 'danger', IMPORTANT: 'warning', GENERAL: 'primary' }[l] ?? 'info'
}
function statusTagType(s) {
  return { OPEN: 'danger', RECEIVED: 'warning', PROCESSING: 'primary', CLOSED: 'success', CANCELLED: 'info' }[s] ?? 'info'
}

async function doReceive() {
  acting.value = true
  try {
    await receiveAlarm({ alarmId: props.alarm.alarmId, operator: userStore.userInfo?.username })
    ElMessage.success('已接收报警')
    emit('refresh')
    emit('update:visible', false)
  } catch (e) {
    ElMessage.error(e?.message || '接收失败')
  } finally {
    acting.value = false
  }
}

async function doResolve() {
  acting.value = true
  try {
    await resolveAlarm({
      alarmId:  props.alarm.alarmId,
      remark:   remark.value || '手动解除',
      operator: userStore.userInfo?.username,
    })
    ElMessage.success('报警已解除')
    remark.value = ''
    emit('refresh')
    emit('update:visible', false)
  } catch (e) {
    ElMessage.error(e?.message || '解除失败')
  } finally {
    acting.value = false
  }
}
</script>

<style scoped>
.ad-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.ad-head__left { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.ad-no { font-size: 13px; font-weight: 700; color: #001b3f; font-family: monospace; }
.ad-eq-row { display: flex; align-items: center; gap: 8px; margin: 6px 0 16px; }
.ad-eq-code { font-size: 11px; color: #8090a8; font-family: monospace;
  background: #f0f4f8; padding: 1px 6px; border-radius: 4px; }
.ad-eq-name { font-size: 15px; font-weight: 600; color: #1a2a40; }

.ad-section { margin-bottom: 20px; }
.ad-section__title {
  font-size: 12px; font-weight: 600; color: #3b5a80;
  border-left: 3px solid #3b82f6; padding-left: 7px;
  margin-bottom: 10px;
}

.ad-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.ad-cell { display: flex; flex-direction: column; gap: 2px; }
.ad-cell--full { grid-column: 1 / -1; }
.ad-cell__k { font-size: 11px; color: #9aa5b4; }
.ad-cell__v { font-size: 13px; color: #1a2a40; font-weight: 500; }
.ad-cell__v--wrap { white-space: pre-wrap; line-height: 1.55; }

.tl-item { display: flex; align-items: center; gap: 8px; }
.tl-item__action { font-size: 13px; font-weight: 600; color: #1a2a40; }
.tl-item__op     { font-size: 12px; color: #8090a8; }

.ad-actions { background: #f7faff; border-radius: 8px; padding: 12px 14px; }
.ad-action-btns { display: flex; gap: 8px; flex-wrap: wrap; }
</style>
