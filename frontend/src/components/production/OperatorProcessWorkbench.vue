<template>
  <div class="opwb">
    <div class="opwb__hero">
      <img :src="station.image" :alt="station.title" class="opwb__hero-img" />
      <div class="opwb__hero-body">
        <h3>{{ station.title }} · 生产报工</h3>
        <p>{{ station.subtitle }}</p>
        <div class="opwb__hero-meta">
          <el-tag v-if="binding">{{ binding.workshopName }}</el-tag>
          <span>待报工任务 <strong>{{ activeDispatches.length }}</strong> 单</span>
        </div>
      </div>
    </div>

    <el-alert
      v-if="!activeDispatches.length"
      type="info"
      :closable="false"
      title="当前工序暂无进行中的派工，请先在「我的派工」接收并开始生产。"
      style="margin-bottom: 12px"
    />

    <div v-if="currentTask" class="opwb__current">
      <span class="opwb__current-tag">当前生产</span>
      <strong>{{ currentTask.workOrderNo || currentTask.workOrderId }}</strong>
      <span>· {{ stageProgressLabel(currentTask) }}</span>
      <span>· 进度 {{ currentTask.completedQty }}/{{ currentTask.planQty }}</span>
      <StatusBadge :status="currentTask.status" />
    </div>

    <el-table
      v-if="activeDispatches.length"
      :data="activeDispatches"
      border
      stripe
      highlight-current-row
      class="opwb__table"
      :row-class-name="rowClassName"
      style="width: 100%; margin-bottom: 16px"
      @current-change="onRowClick"
    >
      <el-table-column prop="id" label="派工单" min-width="168" class-name="opwb__nowrap" />
      <el-table-column prop="workshopName" label="车间" min-width="120" />
      <el-table-column prop="equipment" label="设备" min-width="100" show-overflow-tooltip />
      <el-table-column prop="planQty" label="计划" width="96" align="center" class-name="opwb__nowrap" />
      <el-table-column prop="completedQty" label="已报" width="88" align="center" class-name="opwb__nowrap" />
      <el-table-column label="进度" width="100">
        <template #default="{ row }">{{ progressText(row) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }"><StatusBadge :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === '已分配'"
            link
            type="primary"
            @click="selectAndAccept(row)"
          >
            接收
          </el-button>
          <el-button
            v-if="row.status === '已接收'"
            link
            type="success"
            @click="selectAndStart(row)"
          >
            开始生产
          </el-button>
          <el-button
            v-if="canSubmitQc(row)"
            link
            type="primary"
            @click="selectAndSubmitQc(row)"
          >
            提交质检
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="selected && !canReport && selected.status !== '待质检'" class="opwb__prep">
      <el-alert
        type="warning"
        :closable="false"
        :title="prepHint"
        style="margin-bottom: 12px"
      />
      <el-button v-if="selected.status === '已分配'" type="primary" @click="acceptSelected">接收派工</el-button>
      <el-button v-if="selected.status === '已接收'" type="success" @click="startSelected">开始生产</el-button>
    </div>

    <div v-if="selected && canReport" class="opwb__form">
      <div class="opwb__form-hd">提交报工 · {{ selected.workOrderNo || selected.workOrderId }}</div>
      <el-form label-width="100px" size="default">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="生产数量" required>
              <el-input-number v-model="form.reportQty" :min="1" :max="remainQty" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开始时间" required>
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="选择开始时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束时间" required>
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="选择结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" size="large" @click="submit">提交报工</el-button>
        <el-button
          v-if="canSubmitQc(selected)"
          type="warning"
          size="large"
          @click="submitQc"
        >
          提交质检
        </el-button>
      </el-form>
    </div>

    <el-alert
      v-else-if="selected?.status === '待质检'"
      type="success"
      :closable="false"
      title="已提交质检，请等待质检员检验。"
    />
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { DISPATCH_REPORTABLE, DISPATCH_ACTIVE } from '@/mock/constants'
import { operatorBinding, pickCurrentDispatch, stageProgressLabel } from '@/utils/operatorWorkshop'
import { dispatchMatchesStage } from '@/config/operatorProcessStations'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const props = defineProps({
  station: { type: Object, required: true }
})

const mes = useMesStore()
const userStore = useUserStore()
const username = computed(() => userStore.userInfo?.username)
const binding = computed(() => operatorBinding(username.value))

const myDispatches = computed(() => mes.myDispatches(username.value))
const activeDispatches = computed(() =>
  myDispatches.value.filter((d) =>
    DISPATCH_ACTIVE.includes(d.status) && dispatchMatchesStage(d, props.station)
  )
)

const currentTask = computed(() => pickCurrentDispatch(activeDispatches.value, false))
const selected = ref(null)
const form = reactive({ reportQty: 10, startTime: '', endTime: '', remark: '' })

function pad2(n) {
  return String(n).padStart(2, '0')
}

function formatDateTime(d) {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

function resetFormDefaults(task) {
  const end = new Date()
  const start = new Date(end.getTime() - 2 * 60 * 60 * 1000)
  const remain = task ? Math.max(1, Number(task.planQty || 0) - Number(task.completedQty || 0)) : 10
  form.reportQty = Math.min(10, remain)
  form.startTime = formatDateTime(start)
  form.endTime = formatDateTime(end)
  form.remark = ''
}

const remainQty = computed(() => {
  if (!selected.value) return 9999
  return Math.max(1, Number(selected.value.planQty || 0) - Number(selected.value.completedQty || 0))
})

function onRowClick(row) {
  selected.value = row
  resetFormDefaults(row)
}

watch(
  () => activeDispatches.value,
  (list) => {
    if (!list?.length) {
      selected.value = null
      return
    }
    if (!selected.value || !list.some((d) => d.id === selected.value.id)) {
      selected.value = pickCurrentDispatch(list, false)
    }
    if (selected.value) resetFormDefaults(selected.value)
  },
  { immediate: true }
)

watch(() => props.station?.id, () => {
  const list = activeDispatches.value
  selected.value = list.length ? pickCurrentDispatch(list, false) : null
  if (selected.value) resetFormDefaults(selected.value)
})

const canReport = computed(() =>
  selected.value
  && DISPATCH_REPORTABLE.includes(selected.value.status)
  && selected.value.completedQty < selected.value.planQty
)

const prepHint = computed(() => {
  if (!selected.value) return ''
  if (selected.value.status === '已分配') {
    return '该派工尚未接收，请先点击「接收派工」，再「开始生产」后即可报工。'
  }
  if (selected.value.status === '已接收') {
    return '已接收派工，请点击「开始生产」后再填写报工数量。'
  }
  if (selected.value.completedQty >= selected.value.planQty) {
    return '计划数量已全部报工，如为最后一道工序请提交质检。'
  }
  return '当前状态不可报工，请确认派工流程。'
})

async function acceptSelected() {
  if (!selected.value) return
  try {
    const ok = await mes.acceptDispatch(selected.value.id, username.value, 'operator')
    if (ok !== false) {
      ElMessage.success('已接收派工，请点击「开始生产」')
    } else {
      ElMessage.error('接收失败')
    }
  } catch (e) {
    ElMessage.error(e?.message || '接收派工失败')
  }
}

async function startSelected() {
  if (!selected.value) return
  try {
    const ok = await mes.startDispatch(selected.value.id, username.value, 'operator')
    if (ok !== false) {
      ElMessage.success('已开始生产，请填写报工信息')
    } else {
      ElMessage.warning('请先接收派工')
    }
  } catch (e) {
    ElMessage.error(e?.message || '开始生产失败')
  }
}

function selectAndAccept(row) {
  selected.value = row
  acceptSelected()
}

function selectAndStart(row) {
  selected.value = row
  startSelected()
}

function rowClassName({ row }) {
  return row.id === currentTask.value?.id ? 'is-current-task' : ''
}

function canSubmitQc(row) {
  if (!(row.finalProductionStep || row.processStep === '返修')) return false
  if (Number(row.completedQty) < Number(row.planQty)) return false
  return ['已接收', '生产中', '待质检'].includes(row.status)
}

function progressText(row) {
  if (row.completedQty < row.planQty) return '生产中'
  if (row.finalProductionStep || row.processStep === '返修') return '可提交质检'
  return '工序完成'
}

async function submit() {
  if (!selected.value) return
  if (!form.startTime || !form.endTime) {
    ElMessage.warning('请填写开始时间和结束时间')
    return
  }
  if (new Date(form.endTime) <= new Date(form.startTime)) {
    ElMessage.warning('结束时间须晚于开始时间')
    return
  }
  if (!form.reportQty || form.reportQty < 1) {
    ElMessage.warning('请填写生产数量')
    return
  }
  try {
    const rpt = await mes.submitReport(
      {
        reportQty: form.reportQty,
        startTime: form.startTime,
        endTime: form.endTime,
        remark: form.remark,
        dispatchId: selected.value.id,
        operatorName: userStore.displayName
      },
      username.value,
      'operator'
    )
    if (rpt) {
      ElMessage.success('报工已提交')
      resetFormDefaults(selected.value)
      if (selected.value.completedQty >= selected.value.planQty) {
        ElMessage.info(canSubmitQc(selected.value)
          ? '最后一道工序已完成，请点击「提交质检」'
          : '当前工序已完成，等待后续工序继续生产')
      }
    } else {
      ElMessage.warning('报工失败，请确认派工状态')
    }
  } catch (e) {
    ElMessage.error(e?.message || '报工失败')
  }
}

async function submitQc() {
  if (!selected.value) return
  try {
    const qcId = await mes.submitToInspection(selected.value.id, username.value, 'operator')
    if (qcId) {
      ElMessage.success('已提交质检，质检员可在「待检任务」中处理')
    } else {
      ElMessage.warning('提交失败：请确认计划数量已全部报工')
    }
  } catch (e) {
    ElMessage.error(e?.message || '提交质检失败')
  }
}

function selectAndSubmitQc(row) {
  selected.value = row
  submitQc()
}
</script>

<style scoped>
.opwb__hero {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  padding: 16px;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  background: linear-gradient(135deg, #f8fafc 0%, #fff 60%);
}

.opwb__hero-img {
  width: 280px;
  height: 180px;
  object-fit: cover;
  border-radius: 10px;
  flex-shrink: 0;
  border: 1px solid #dcdfe6;
}

.opwb__hero-body h3 {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.opwb__hero-body p {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
}

.opwb__hero-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #909399;
}

.opwb__current {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  font-size: 13px;
}

.opwb__current-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #67c23a;
  background: #fff;
}

.opwb__form {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafbfc;
}

.opwb__prep {
  padding: 16px;
  border: 1px dashed #e6a23c;
  border-radius: 8px;
  background: #fdf6ec;
  margin-bottom: 16px;
}

.opwb__form-hd {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 14px;
}

:deep(.is-current-task > td) {
  background: #f0f9eb !important;
}

.opwb__table :deep(.opwb__nowrap .cell) {
  white-space: nowrap;
}

@media (max-width: 900px) {
  .opwb__hero {
    flex-direction: column;
  }
  .opwb__hero-img {
    width: 100%;
    height: 200px;
  }
}
</style>
