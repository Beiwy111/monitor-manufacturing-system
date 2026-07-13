<template>
  <MesPageShell toolbar-title="生产报工" :detail-rows="rows" :logs="mes.operationLogs.slice(0,8)">
    <template #table>
      <div v-if="binding" class="report-context">
        <div class="report-context__main">
          <span class="report-context__tag">固定车间</span>
          <strong>{{ binding.workshopName }}</strong>
          <el-divider direction="vertical" />
          <span class="report-context__tag">负责工序</span>
          <strong>{{ binding.stageName }}</strong>
        </div>
        <div v-if="currentTask" class="report-context__current">
          <span class="report-context__tag report-context__tag--active">当前生产</span>
          <strong>{{ currentTask.workOrderNo || currentTask.workOrderId }}</strong>
          <span class="report-context__sep">·</span>
          <span>{{ stageProgressLabel(currentTask) }}</span>
          <span class="report-context__sep">·</span>
          <span>进度 {{ currentTask.completedQty }}/{{ currentTask.planQty }}</span>
          <StatusBadge :status="currentTask.status" />
        </div>
        <el-alert
          v-else
          type="info"
          :closable="false"
          title="当前没有可报工的派工任务，请先在「我的派工」接收并开始生产。"
          style="margin-top: 8px"
        />
      </div>

      <el-alert
        type="info"
        :closable="false"
        title="每名操作员固定在一个车间，同一时间只能承担一道工序。请先完成计划数量报工，最后一道工序再提交质检。"
        style="margin: 12px 0"
      />

      <el-table
        :data="activeDispatches"
        border
        stripe
        highlight-current-row
        :row-class-name="rowClassName"
        @current-change="onRowClick"
      >
        <el-table-column prop="id" label="派工单" width="130" />
        <el-table-column prop="workshopName" label="车间" min-width="130" />
        <el-table-column prop="processStep" label="工序" width="110" />
        <el-table-column label="工序进度" width="150">
          <template #default="{ row }">{{ stageProgressLabel(row) }}</template>
        </el-table-column>
        <el-table-column prop="planQty" label="计划" width="70" />
        <el-table-column prop="completedQty" label="已报" width="70" />
        <el-table-column label="进度" width="100">
          <template #default="{ row }">{{ progressText(row) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
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
    </template>
    <template #detail-extra>
      <el-form v-if="selected && canReport" label-width="90px" style="margin-top:12px">
        <el-alert
          type="success"
          :closable="false"
          :title="`正在报工：${selected.workshopName || binding?.workshopName} · ${stageProgressLabel(selected)}`"
          style="margin-bottom: 12px"
        />
        <el-form-item label="报工数量"><el-input-number v-model="form.reportQty" :min="1" /></el-form-item>
        <el-form-item label="合格数量"><el-input-number v-model="form.qualifiedQty" :min="0" /></el-form-item>
        <el-form-item label="不合格"><el-input-number v-model="form.unqualifiedQty" :min="0" /></el-form-item>
        <el-form-item label="工时(h)"><el-input-number v-model="form.workHours" :min="0.5" :step="0.5" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
        <el-button type="primary" @click="submit">提交报工</el-button>
      </el-form>
      <el-alert
        v-else-if="selected?.status === '待质检'"
        type="success"
        :closable="false"
        title="已提交质检，请等待质检员检验。合格品将流转至仓储入库。"
      />
    </template>
    <template #detail-actions>
      <el-button
        v-if="selected && canSubmitQc(selected)"
        type="warning"
        size="small"
        @click="submitQc"
      >
        提交质检
      </el-button>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import { DISPATCH_REPORTABLE, DISPATCH_ACTIVE } from '@/mock/constants'
import { operatorBinding, pickCurrentDispatch, stageProgressLabel } from '@/utils/operatorWorkshop'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const username = computed(() => userStore.userInfo?.username)
const binding = computed(() => operatorBinding(username.value))

const activeDispatches = computed(() =>
  mes.myDispatches(username.value).filter((d) => DISPATCH_ACTIVE.includes(d.status))
)
const currentTask = computed(() => pickCurrentDispatch(activeDispatches.value, true))
const { selected, onRowClick } = useMesFilter(activeDispatches, ['id'])
const form = reactive({ reportQty: 10, qualifiedQty: 10, unqualifiedQty: 0, workHours: 2, remark: '' })

watch(
  currentTask,
  (task) => {
    if (task && DISPATCH_REPORTABLE.includes(task.status)) {
      selected.value = task
    }
  },
  { immediate: true }
)

const rows = computed(() => detailRows(selected.value, [
  { key: 'workOrderNo', label: '工单' },
  { key: 'workshopName', label: '车间' },
  { key: 'processStep', label: '工序' },
  { key: 'planQty', label: '计划量' },
  { key: 'completedQty', label: '已报工' },
  { key: 'status', label: '状态' }
]))

const canReport = computed(() =>
  selected.value && DISPATCH_REPORTABLE.includes(selected.value.status) && selected.value.completedQty < selected.value.planQty
)

function rowClassName({ row }) {
  return row.id === currentTask.value?.id ? 'is-current-task' : ''
}

function canSubmitQc(row) {
  return DISPATCH_REPORTABLE.includes(row.status) &&
    row.completedQty >= row.planQty &&
    (row.finalProductionStep || row.processStep === '返修')
}

function progressText(row) {
  if (row.completedQty < row.planQty) return '生产中'
  if (row.finalProductionStep || row.processStep === '返修') return '可提交质检'
  return '工序完成'
}

async function submit() {
  if (!selected.value) return
  try {
    const rpt = await mes.submitReport(
      { ...form, dispatchId: selected.value.id, operatorName: userStore.displayName },
      username.value,
      'operator'
    )
    if (rpt) {
      ElMessage.success('报工已提交')
      if (selected.value.completedQty >= selected.value.planQty) {
        ElMessage.info(canSubmitQc(selected.value)
          ? '最后一道工序已完成，请点击「提交质检」送交质检员'
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
      ElMessage.warning('提交失败：请确认计划数量已全部报工且尚未重复提交')
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
.report-context {
  margin-bottom: 12px;
  padding: 14px 16px;
  border: 1px solid #d9ecff;
  border-radius: 8px;
  background: linear-gradient(180deg, #f5f9ff 0%, #fff 100%);
}

.report-context__main,
.report-context__current {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.report-context__current {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e4e7ed;
}

.report-context__tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
}

.report-context__tag--active {
  color: #409eff;
  background: #ecf5ff;
}

.report-context__sep {
  color: #c0c4cc;
}

:deep(.is-current-task > td) {
  background: #f0f9eb !important;
}
</style>
