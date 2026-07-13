<template>
  <el-drawer
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="data?.equipmentName || '设备详情'"
    size="520px"
    direction="rtl"
    destroy-on-close
  >
    <template v-if="data">
      <!-- 顶部状态栏 -->
      <div class="hd-header">
        <div class="hd-eq">
          <span class="hd-code">{{ data.equipmentCode }}</span>
          <el-tag :type="statusTagType" size="small" effect="plain" style="margin-left:8px">
            {{ data.statusCn }}
          </el-tag>
          <el-tag :type="levelTagType" size="small" style="margin-left:6px">
            健康度 {{ data.healthScore }}  {{ levelLabel }}
          </el-tag>
        </div>
        <div class="hd-meta">
          <span>{{ data.equipmentType }}</span>
          <span v-if="data.workshop" style="margin-left:10px">{{ data.workshop }}</span>
          <span style="margin-left:10px;color:#a0a4ab">上次维保：{{ data.lastMaintenanceAt || '—' }}</span>
        </div>
      </div>

      <!-- 健康分析 -->
      <div class="hd-section">
        <div class="hd-section__title">健康评分分析</div>

        <!-- 健康分大环 -->
        <div class="hd-score-row">
          <svg viewBox="0 0 100 100" width="100" height="100">
            <circle cx="50" cy="50" r="42" fill="none" stroke="#eef1f5" stroke-width="9" />
            <circle
              cx="50" cy="50" r="42" fill="none"
              :stroke="levelColor" stroke-width="9" stroke-linecap="round"
              :stroke-dasharray="`${(263.9 * data.healthScore / 100).toFixed(1)} 263.9`"
              stroke-dashoffset="65.97"
              style="transition: stroke-dasharray 1s ease"
            />
            <text x="50" y="44" text-anchor="middle" font-size="22" font-weight="800"
              :fill="levelColor">{{ data.healthScore }}</text>
            <text x="50" y="60" text-anchor="middle" font-size="11" fill="#909399">/ 100</text>
          </svg>

          <div class="hd-deduct-list">
            <div class="hd-deduct-row hd-deduct-row--base">
              <span class="hd-deduct-label">基础分</span>
              <span class="hd-deduct-note">满分</span>
              <span class="hd-deduct-val">100</span>
            </div>
            <div v-for="row in deductRows" :key="row.label" class="hd-deduct-row"
              :class="row.value > 0 ? 'is-deduct' : ''">
              <span class="hd-deduct-label">− {{ row.label }}</span>
              <span class="hd-deduct-note">{{ row.note }}</span>
              <span class="hd-deduct-val" :style="{ color: row.value > 0 ? '#f56c6c' : '#67c23a' }">
                {{ row.value > 0 ? '-' + row.value : '0' }}
              </span>
            </div>
            <div class="hd-deduct-row hd-deduct-row--total">
              <span class="hd-deduct-label">最终健康分</span>
              <span />
              <span class="hd-deduct-val" :style="{ color: levelColor }">{{ data.healthScore }}</span>
            </div>
          </div>
        </div>

        <!-- 智能建议 -->
        <div class="hd-advice" :class="`advice-${data.healthLevel?.toLowerCase()}`">
          <span class="hd-advice__icon">{{ adviceIcon }}</span>
          <span class="hd-advice__text">{{ data.advice }}</span>
        </div>
      </div>

      <!-- 近7天报警 -->
      <div class="hd-section">
        <div class="hd-section__title">
          近7天报警
          <el-tag v-if="data.alarm7d > 0" type="danger" size="small" round style="margin-left:8px">
            {{ data.alarm7d }} 次
          </el-tag>
        </div>
        <el-empty v-if="!alarmList.length" description="近7天无报警记录" :image-size="60" />
        <el-table v-else :data="alarmList" size="small" :show-header="true" max-height="200">
          <el-table-column prop="alarmNo" label="报警号" width="118" />
          <el-table-column label="级别" width="62" align="center">
            <template #default="{ row }">
              <el-tag :type="levelTagMap[row.alarmLevel]" size="small">{{ row.alarmLevelCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="alarmDescription" label="描述" min-width="100" show-overflow-tooltip />
          <el-table-column label="状态" width="72" align="center">
            <template #default="{ row }">
              <el-tag :type="alarmStatusTagMap[row.alarmStatus]" size="small">{{ row.alarmStatusCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reportedAt" label="上报时间" width="140" />
        </el-table>
      </div>

      <!-- 最近维保记录 -->
      <div class="hd-section">
        <div class="hd-section__title">
          最近维保记录
          <span style="font-size:12px;font-weight:400;color:#a0a4ab;margin-left:8px">近30天</span>
        </div>
        <el-empty v-if="!maintList.length" description="暂无维保记录" :image-size="60" />
        <el-table v-else :data="maintList" size="small" max-height="200">
          <el-table-column prop="maintenanceNo" label="单号" width="118" />
          <el-table-column prop="maintenanceTypeCn" label="类型" width="60" align="center" />
          <el-table-column prop="maintenanceContent" label="内容" min-width="100" show-overflow-tooltip />
          <el-table-column label="结果" width="78" align="center">
            <template #default="{ row }">
              <el-tag :type="row.inProgress ? 'warning' : (row.maintenanceResult === 'COMPLETED' ? 'success' : 'info')"
                size="small">{{ row.maintenanceResultCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" width="140" />
        </el-table>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  data: { type: Object, default: null }
})
defineEmits(['update:modelValue'])

const LEVEL_COLOR = { GOOD: '#67c23a', WARN: '#e6a23c', ALERT: '#f56c6c', DANGER: '#c0392b' }
const LEVEL_LABEL = { GOOD: '优良 ✓', WARN: '注意 ⚠', ALERT: '警告 ⚠', DANGER: '危险 ✕' }
const LEVEL_TAG   = { GOOD: 'success',  WARN: 'warning', ALERT: 'danger', DANGER: 'danger' }
const STATUS_TAG  = { RUNNING: 'success', IDLE: 'info', FAULT: 'danger', MAINTAINING: 'warning', SCRAPPED: 'info' }

const levelColor   = computed(() => LEVEL_COLOR[props.data?.healthLevel] ?? '#909399')
const levelLabel   = computed(() => LEVEL_LABEL[props.data?.healthLevel] ?? '')
const levelTagType = computed(() => LEVEL_TAG[props.data?.healthLevel]   ?? 'info')
const statusTagType = computed(() => STATUS_TAG[props.data?.status]      ?? 'info')
const adviceIcon   = computed(() => {
  const l = props.data?.healthLevel
  return l === 'GOOD' ? '✅' : l === 'WARN' ? '⚠️' : l === 'ALERT' ? '🔴' : '🚨'
})

const deductRows = computed(() => {
  const d = props.data
  if (!d) return []
  return [
    { label: '超时运行扣分', value: d.deductRun,   note: `运行 ${d.runHours}h` },
    { label: '高频报警扣分', value: d.deductAlarm, note: `近7天 ${d.alarm7d} 次` },
    { label: '逾期保养扣分', value: d.deductMaint, note: `已 ${d.daysSinceMaint} 天未维保` },
    { label: '关联缺陷扣分', value: d.deductNc,    note: `近30天故障维修 ${d.faultCount30d} 次` },
  ]
})

const alarmList = computed(() => props.data?.alarmList7d ?? [])
const maintList = computed(() => props.data?.maintList   ?? [])

const levelTagMap = { GENERAL: 'info', IMPORTANT: 'warning', URGENT: 'danger' }
const alarmStatusTagMap = {
  OPEN: 'danger', RECEIVED: 'warning', PROCESSING: 'warning', CLOSED: 'success', CANCELLED: 'info'
}
</script>

<style scoped>
.hd-header {
  background: #f7f9fc;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 16px;
}
.hd-eq {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}
.hd-code {
  font-size: 16px;
  font-weight: 700;
  color: #001b3f;
}
.hd-meta {
  font-size: 12px;
  color: #606266;
}

.hd-section {
  margin-bottom: 20px;
}
.hd-section__title {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #001b3f;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f2f5;
}

/* 健康分区域 */
.hd-score-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 12px;
}
.hd-deduct-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding-top: 6px;
}
.hd-deduct-row {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #606266;
  gap: 4px;
}
.hd-deduct-row.is-deduct { color: #f56c6c; }
.hd-deduct-row--base { color: #909399; }
.hd-deduct-row--total {
  font-weight: 700;
  font-size: 13px;
  color: #001b3f;
  border-top: 1px solid #eef1f5;
  padding-top: 5px;
  margin-top: 3px;
}
.hd-deduct-label { flex: 0 0 90px; }
.hd-deduct-note  { flex: 1; color: #a0a4ab; font-size: 11px; }
.hd-deduct-val   { flex: 0 0 32px; text-align: right; font-weight: 600; }

/* 建议 */
.hd-advice {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
  background: #f0f9eb;
  color: #529b2e;
}
.hd-advice.advice-warn   { background: #fdf6ec; color: #b26e00; }
.hd-advice.advice-alert  { background: #fef0f0; color: #c45656; }
.hd-advice.advice-danger { background: #fde8e8; color: #9b2222; }
.hd-advice__icon { flex-shrink: 0; font-size: 16px; margin-top: 1px; }
.hd-advice__text { flex: 1; }
</style>
