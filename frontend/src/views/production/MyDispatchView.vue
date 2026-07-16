<template>
  <MesPageShell toolbar-title="我的派工" :status-options="DISPATCH_STATUS" :detail-rows="rows">
    <template #table>
      <div v-if="binding" class="dispatch-context">
        <span class="dispatch-context__tag">固定车间</span>
        <strong>{{ binding.workshopName }}</strong>
        <el-divider direction="vertical" />
        <span class="dispatch-context__tag">负责工序</span>
        <strong>{{ binding.stageName }}</strong>
        <span class="dispatch-context__hint">（同一时间只能承担一道工序）</span>
      </div>

      <el-alert
        v-if="!myList.length"
        type="info"
        :closable="false"
        title="暂无派工任务。请生产主管在「工单派工」页将任务派给您。"
        style="margin: 12px 0"
      />
      <el-alert
        v-if="isAdminDemo"
        type="info"
        :closable="false"
        show-icon
        title="系统管理员以第 8 道工序操作员身份查看派工"
        style="margin-bottom: 12px"
      />

      <el-table v-if="myList.length" :data="myList" highlight-current-row class="mes-table-light" @current-change="onRowClick">
        <el-table-column prop="id" label="派工单" width="148" show-overflow-tooltip />
        <el-table-column prop="workOrderNo" label="工单" width="148" show-overflow-tooltip />
        <el-table-column prop="workshopName" label="车间" min-width="120" show-overflow-tooltip />
        <el-table-column prop="processStep" label="工序" width="100" show-overflow-tooltip />
        <el-table-column label="工序进度" min-width="168" show-overflow-tooltip>
          <template #default="{ row }">{{ stageProgressLabel(row) }}</template>
        </el-table-column>
        <el-table-column prop="equipment" label="设备" min-width="120" show-overflow-tooltip />
        <el-table-column prop="planQty" label="计划" width="72" align="center" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '已分配'" link type="primary" @click="selectAndAccept(row)">接收</el-button>
            <el-button v-if="row.status === '已接收'" link type="warning" @click="openPick(row)">仓库领料</el-button>
            <el-button v-if="row.status === '已接收'" link type="success" @click="selectAndStart(row)">开始生产</el-button>
            <el-button v-if="DISPATCH_REPORTABLE.includes(row.status)" link type="primary" @click="$router.push(reportPath)">去报工</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected?.status === '已分配'" type="primary" size="small" @click="accept">接收派工</el-button>
      <el-button v-if="selected?.status === '已接收'" type="warning" size="small" plain @click="openPick(selected)">仓库领料</el-button>
      <el-button v-if="selected?.status === '已接收'" type="success" size="small" @click="start">开始生产</el-button>
      <el-button v-if="selected && DISPATCH_REPORTABLE.includes(selected.status)" type="primary" size="small" @click="$router.push(reportPath)">提交报工</el-button>
    </template>
  </MesPageShell>

  <el-dialog
    v-model="pickVisible"
    class="pick-material-dialog"
    title="生产领料（开工前需领齐物料）"
    width="920px"
  >
    <el-alert
      type="info"
      :closable="false"
      show-icon
      :title="pickAlertTitle"
      style="margin-bottom: 12px"
    />
    <div class="pick-toolbar">
      <el-button
        v-if="!allPicked && pendingPickTasks.length"
        type="warning"
        size="default"
        :loading="pickAllLoading"
        :disabled="!canPickAll"
        @click="pickAll"
      >一键领料（{{ pendingPickTasks.length }} 项）</el-button>
      <el-tag v-else-if="allPicked" type="success" effect="plain">全部物料已领齐</el-tag>
      <span v-if="!allPicked && pendingPickTasks.length && !canPickAll" class="pick-toolbar__hint">
        部分物料库存不足，请联系仓库补货后再领料
      </span>
    </div>
    <el-table v-loading="pickLoading" :data="pickTasks" size="small" border class="pick-material-table">
      <el-table-column prop="materialCode" label="物料编码" width="120" show-overflow-tooltip />
      <el-table-column prop="materialName" label="物料" min-width="150" show-overflow-tooltip />
      <el-table-column prop="requiredQty" label="需求" width="76" align="center" />
      <el-table-column prop="issuedQty" label="已领" width="76" align="center" />
      <el-table-column label="库存" width="88" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="Number(row.stockQty) >= pickRemain(row) ? 'success' : 'danger'">
            {{ Number(row.stockQty || 0) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="领取态" width="108" align="center" class-name="pick-material-table__status">
        <template #default="{ row }">
          <StatusBadge :status="row.status" />
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <span v-if="allPicked" class="pick-done">✓ 物料已领齐，可以开始生产</span>
      <el-button @click="pickVisible = false">关闭</el-button>
      <el-button
        v-if="!allPicked && pendingPickTasks.length"
        type="warning"
        :loading="pickAllLoading"
        :disabled="!canPickAll"
        @click="pickAll"
      >一键领料</el-button>
      <el-button v-if="allPicked" type="success" @click="startAfterPick">开始生产</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { DISPATCH_STATUS, DISPATCH_REPORTABLE } from '@/mock/constants'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import { useOperatorIdentity } from '@/composables/useOperatorIdentity'
import { stageProgressLabel } from '@/utils/operatorWorkshop'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const { operatorUsername, binding, reportPath, isAdminDemo } = useOperatorIdentity()
const myList = computed(() => mes.myDispatches(operatorUsername.value))
const { selected, onRowClick } = useMesFilter(myList, ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'workOrderNo', label: '工单' },
  { key: 'workshopName', label: '车间' },
  { key: 'processStep', label: '工序' },
  { key: 'equipment', label: '设备' },
  { key: 'planQty', label: '计划量' },
  { key: 'status', label: '状态' }
]))

async function accept() {
  if (!selected.value) return
  try {
    const ok = await mes.acceptDispatch(selected.value.id, operatorUsername.value, userStore.roleKey)
    if (ok !== false) {
      ElMessage.success('已接收派工，请先完成仓库领料')
    } else {
      ElMessage.error('接收失败，请确认该任务分配给当前账号')
    }
  } catch (e) {
    ElMessage.error(e?.message || '接收派工失败')
  }
}

async function start() {
  if (!selected.value) return
  const ready = await ensureMaterialsReady(selected.value)
  if (!ready) return
  try {
    const ok = await mes.startDispatch(selected.value.id, operatorUsername.value, userStore.roleKey)
    if (ok !== false) {
      ElMessage.success('已开始生产，可前往报工')
    } else {
      ElMessage.warning('请先接收派工')
    }
  } catch (e) {
    ElMessage.error(e?.message || '开始生产失败')
  }
}

async function ensureMaterialsReady(dispatch) {
  try {
    const list = await mes.listPickTasks(dispatch.id, operatorUsername.value, userStore.roleKey)
    const pending = (list || []).filter((t) => t.status !== '已完成')
    if (pending.length) {
      ElMessage.warning('请先点击「一键领料」领齐全部物料')
      openPick(dispatch)
      return false
    }
    return true
  } catch (e) {
    ElMessage.error(e?.message || '校验领料状态失败')
    return false
  }
}

function selectAndAccept(row) {
  selected.value = row
  accept()
}

function selectAndStart(row) {
  selected.value = row
  start()
}

// —— 生产前仓库领料 ——
const pickVisible = ref(false)
const pickLoading = ref(false)
const pickTasks = ref([])
const pickAllLoading = ref(false)
const pickDispatch = ref(null)

const allPicked = computed(() => pickTasks.value.length > 0 && pickTasks.value.every((t) => t.status === '已完成'))
const pendingPickTasks = computed(() => pickTasks.value.filter((t) => t.status !== '已完成'))
const pickAlertTitle = computed(() =>
  allPicked.value
    ? '全部物料已领齐，可以开始生产。'
    : '根据工单 BOM 生成领料需求。请点击「一键领料」一次性领齐全部物料，领齐后方可开始生产。'
)
const canPickAll = computed(() =>
  pendingPickTasks.value.length > 0
  && pendingPickTasks.value.every((row) => Number(row.stockQty) >= pickRemain(row))
)

function pickRemain(row) {
  return Math.max(1, (row.requiredQty || 0) - (row.issuedQty || 0))
}

async function loadPickTasks() {
  if (!pickDispatch.value) return
  pickLoading.value = true
  try {
    const list = await mes.listPickTasks(pickDispatch.value.id, operatorUsername.value, userStore.roleKey)
    pickTasks.value = list || []
  } catch (e) {
    ElMessage.error(e?.message || '加载领料任务失败')
  } finally {
    pickLoading.value = false
  }
}

function openPick(row) {
  if (!row) return
  selected.value = row
  pickDispatch.value = row
  pickVisible.value = true
  pickTasks.value = []
  loadPickTasks()
}

async function pickAll() {
  if (!pickDispatch.value || !pendingPickTasks.value.length || pickAllLoading.value) return
  if (!canPickAll.value) {
    ElMessage.warning('部分物料库存不足，请联系仓库补货')
    return
  }
  pickAllLoading.value = true
  try {
    const res = await mes.pickAllMaterials(pickDispatch.value.id, operatorUsername.value, userStore.roleKey)
    await loadPickTasks()
    const failures = res?.failures || []
    if (failures.length) {
      const names = failures.map((f) => (typeof f === 'string' ? f : f.materialName || f)).filter(Boolean)
      ElMessage.warning(res?.message || `部分领料失败：${names.join('、')}`)
    } else {
      ElMessage.success(res?.message || '一键领料完成，全部物料已领齐')
    }
  } catch (e) {
    ElMessage.error(e?.message || '一键领料失败')
  } finally {
    pickAllLoading.value = false
  }
}

async function startAfterPick() {
  if (!pickDispatch.value) return
  selected.value = pickDispatch.value
  pickVisible.value = false
  await start()
}
</script>

<style scoped>
.pick-done {
  margin-right: auto;
  font-size: 12px;
  color: #67c23a;
}

.pick-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.pick-toolbar__hint {
  font-size: 12px;
  color: #e6a23c;
}
.dispatch-context {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.dispatch-context__tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
}

.dispatch-context__hint {
  font-size: 12px;
  color: #909399;
}
</style>

<style>
.pick-material-dialog .pick-material-table .cell {
  white-space: nowrap;
}

.pick-material-dialog .pick-material-table__status .cell {
  overflow: visible;
}

.pick-material-dialog .pick-material-table .status-badge {
  white-space: nowrap;
}
</style>
