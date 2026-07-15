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

  <el-dialog v-model="pickVisible" title="生产领料（开工前需领齐物料）" width="640px">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="根据工单 BOM 生成领料需求，领取后仓库库存将扣减对应数量；全部领齐后方可开始生产。"
      style="margin-bottom: 12px"
    />
    <el-table v-loading="pickLoading" :data="pickTasks" size="small" border>
      <el-table-column prop="materialCode" label="物料编码" width="110" show-overflow-tooltip />
      <el-table-column prop="materialName" label="物料" min-width="110" show-overflow-tooltip />
      <el-table-column prop="requiredQty" label="需求" width="70" align="center" />
      <el-table-column prop="issuedQty" label="已领" width="70" align="center" />
      <el-table-column label="库存" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="Number(row.stockQty) >= pickRemain(row) ? 'success' : 'danger'">
            {{ Number(row.stockQty || 0) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="86">
        <template #default="{ row }"><StatusBadge :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="领取" width="180" fixed="right">
        <template #default="{ row }">
          <div v-if="row.status !== '已完成'" class="pick-actions">
            <el-input-number
              v-model="pickQty[row.id]"
              :min="1"
              :max="pickRemain(row)"
              size="small"
              :controls="false"
              style="width: 64px"
            />
            <el-button
              type="primary"
              size="small"
              :loading="pickingId === row.id"
              :disabled="Number(row.stockQty) < (pickQty[row.id] || 1)"
              @click="doPick(row)"
            >领取</el-button>
          </div>
          <el-tag v-else size="small" type="success">已领齐</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <span v-if="allPicked" class="pick-done">✓ 物料已领齐，可以开始生产</span>
      <el-button @click="pickVisible = false">关闭</el-button>
      <el-button v-if="allPicked" type="success" @click="startAfterPick">开始生产</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
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
      ElMessage.success('已接收派工，请点击「开始生产」')
    } else {
      ElMessage.error('接收失败，请确认该任务分配给当前账号')
    }
  } catch (e) {
    ElMessage.error(e?.message || '接收派工失败')
  }
}

async function start() {
  if (!selected.value) return
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
const pickQty = reactive({})
const pickingId = ref('')
const pickDispatch = ref(null)

const allPicked = computed(() => pickTasks.value.length > 0 && pickTasks.value.every((t) => t.status === '已完成'))

function pickRemain(row) {
  return Math.max(1, (row.requiredQty || 0) - (row.issuedQty || 0))
}

async function loadPickTasks() {
  if (!pickDispatch.value) return
  pickLoading.value = true
  try {
    const list = await mes.listPickTasks(pickDispatch.value.id, operatorUsername.value, userStore.roleKey)
    pickTasks.value = list || []
    for (const t of pickTasks.value) {
      if (t.status !== '已完成') {
        pickQty[t.id] = Math.min(pickRemain(t), Math.max(1, Number(t.stockQty || 0)))
      }
    }
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

async function doPick(row) {
  const qty = pickQty[row.id] || 1
  if (pickingId.value) return
  pickingId.value = row.id
  try {
    const ok = await mes.pickMaterial(pickDispatch.value.id, row.id, qty, operatorUsername.value, userStore.roleKey)
    if (ok !== false) {
      ElMessage.success(`已领取 ${row.materialName} × ${qty}，库存已扣减`)
      await loadPickTasks()
    } else {
      ElMessage.error('库存不足，无法领取')
    }
  } catch (e) {
    ElMessage.error(e?.message || '领料失败')
  } finally {
    pickingId.value = ''
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
.pick-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pick-done {
  margin-right: auto;
  font-size: 12px;
  color: #67c23a;
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
