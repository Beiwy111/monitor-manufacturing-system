<template>
  <MesPageShell toolbar-title="生产报工" :detail-rows="rows" :logs="mes.operationLogs.slice(0,8)">
    <template #table>
      <el-alert
        type="info"
        :closable="false"
        title="流程说明：先完成计划数量的生产报工，再点击「提交质检」；质检合格后由仓储管理员入库。"
        style="margin-bottom: 12px"
      />
      <el-table :data="activeDispatches" highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="派工单" width="130" />
        <el-table-column prop="processStep" label="工序" width="100" />
        <el-table-column prop="planQty" label="计划" width="70" />
        <el-table-column prop="completedQty" label="已报" width="70" />
        <el-table-column label="进度" width="90">
          <template #default="{ row }">
            {{ row.completedQty >= row.planQty ? '可提交质检' : '生产中' }}
          </template>
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
import { computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const username = computed(() => userStore.userInfo?.username)
const activeDispatches = computed(() =>
  mes.myDispatches(username.value).filter((d) => ['已接收', '生产中', '待质检'].includes(d.status))
)
const { selected, onRowClick } = useMesFilter(activeDispatches, ['id'])
const form = reactive({ reportQty: 10, qualifiedQty: 10, unqualifiedQty: 0, workHours: 2, remark: '' })
const rows = computed(() => detailRows(selected.value, [
  { key: 'workOrderNo', label: '工单' },
  { key: 'processStep', label: '工序' },
  { key: 'planQty', label: '计划量' },
  { key: 'completedQty', label: '已报工' },
  { key: 'status', label: '状态' }
]))

const canReport = computed(() =>
  selected.value && ['已接收', '生产中'].includes(selected.value.status) && selected.value.completedQty < selected.value.planQty
)

function canSubmitQc(row) {
  return row.status === '生产中' && row.completedQty >= row.planQty
}

function submit() {
  if (!selected.value) return
  const rpt = mes.submitReport(
    { ...form, dispatchId: selected.value.id, operatorName: userStore.displayName },
    username.value,
    'operator'
  )
  if (rpt) {
    ElMessage.success('报工已提交')
    if (selected.value.completedQty >= selected.value.planQty) {
      ElMessage.info('计划数量已完成，请点击「提交质检」送交质检员')
    }
  } else {
    ElMessage.warning('报工失败，请确认派工状态')
  }
}

function submitQc() {
  if (!selected.value) return
  const qcId = mes.submitToInspection(selected.value.id, username.value, 'operator')
  if (qcId) {
    ElMessage.success('已提交质检，质检员可在「待检任务」中处理')
  } else {
    ElMessage.warning('提交失败：请确认计划数量已全部报工且尚未重复提交')
  }
}

function selectAndSubmitQc(row) {
  selected.value = row
  submitQc()
}
</script>
