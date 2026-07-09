<template>
  <MesPageShell
    :status-items="statusItems"
    toolbar-title="工单派工"
    :status-options="DISPATCH_STATUS"
    :toolbar-actions="[
      { label: '智能派工推荐', key: 'smart', type: 'warning' },
      { label: '新建派工', key: 'create', type: 'primary' }
    ]"
    :detail-rows="rows"
    :logs="mes.operationLogs.slice(0, 10)"
    @toolbar-action="onToolbarAction"
  >
    <template #table>
      <div v-if="agentPlans.length" class="pending-block">
        <div class="pending-block__title">Agent 计划 · 一键派工（{{ agentPlans.length }}）</div>
        <el-table :data="agentPlans" border stripe size="small" style="margin-bottom: 12px">
          <el-table-column prop="id" label="计划号" width="140" />
          <el-table-column prop="orderNo" label="订单号" width="130" />
          <el-table-column prop="productModel" label="型号" width="120" />
          <el-table-column label="Agent建议" min-width="160">
            <template #default="{ row }">
              {{ row.agentRecommendation?.totalOperators || '-' }} 人 ·
              {{ row.agentRecommendation?.totalMachines || '-' }} 台设备 ·
              {{ (row.agentRecommendation?.workshops || []).length }} 车间
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button link type="primary" @click="agentDispatch(row)">Agent 一键派工</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="pendingWo.length" class="pending-block">
        <div class="pending-block__title">待派工生产工单（{{ pendingWo.length }}）</div>
        <el-table :data="pendingWo" border stripe size="small" style="margin-bottom: 12px">
          <el-table-column prop="id" label="工单号" width="130" />
          <el-table-column prop="orderNo" label="订单号" width="130" />
          <el-table-column prop="productModel" label="型号" width="130" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="status" label="状态" width="90" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDialog(row.id)">派工</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-table :data="filtered" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="派工单号" width="130" />
        <el-table-column prop="workOrderNo" label="工单号" width="130" />
        <el-table-column prop="processStep" label="工序" width="100" />
        <el-table-column prop="operatorName" label="操作员" width="100" />
        <el-table-column prop="planQty" label="计划量" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="72" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeDispatch(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <p v-if="selected" class="dispatch-hint">已派给 {{ selected.operatorName }}（账号 {{ selected.operator }}），操作员登录后可接收</p>
      <el-button v-if="selected" type="danger" size="small" plain @click="removeDispatch(selected)">删除派工</el-button>
    </template>
  </MesPageShell>

  <el-dialog v-model="dialogVisible" title="新建派工" width="480px">
    <el-form label-width="100px">
      <el-form-item label="生产工单">
        <el-select v-model="form.workOrderId" style="width:100%" placeholder="请选择已下达工单">
          <el-option v-for="w in dispatchableWo" :key="w.id" :label="`${w.id} · ${w.productModel}`" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!dispatchableWo.length">
        <el-alert type="warning" :closable="false" title="没有可派工工单。请先在「生产工单」页下达工单（状态须为「已下达」）。" />
      </el-form-item>
      <el-form-item label="工序">
        <el-select v-model="form.processStep" style="width:100%">
          <el-option v-for="s in processStepOptions" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item label="设备"><el-input v-model="form.equipment" /></el-form-item>
      <el-form-item label="操作员">
        <el-select v-model="form.operator" style="width:100%" @change="onOperatorChange">
          <el-option
            v-for="u in mes.operatorUsers"
            :key="u.username"
            :label="`${u.realName}（${u.username}）`"
            :value="u.username"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="派工数量">
        <el-input-number v-model="form.planQty" :min="1" style="width:100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!form.workOrderId || !form.operator" @click="save">确认派工</el-button>
    </template>
  </el-dialog>

  <SmartDispatchDialog v-model="smartVisible" :default-plan-id="smartPlanId" @success="onSmartSuccess" />
</template>

<script setup>
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { DISPATCH_STATUS } from '@/mock/constants'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import { useMesDelete } from '@/composables/useMesDelete'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import SmartDispatchDialog from '@/components/mes/SmartDispatchDialog.vue'

const route = useRoute()
const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)
const dialogVisible = ref(false)
const smartVisible = ref(false)
const smartPlanId = ref('')
const processStepOptions = computed(() => mes.processSteps?.length ? mes.processSteps : [])
const todaySlot = () => {
  const d = new Date().toISOString().slice(0, 10)
  return { start: `${d} 08:00`, end: `${d} 18:00` }
}
const form = reactive({
  workOrderId: '',
  processStep: '',
  equipment: '',
  operator: '',
  operatorName: '',
  planQty: 100,
  planStart: todaySlot().start,
  planEnd: todaySlot().end
})

const pendingWo = computed(() => mes.pendingDispatchWorkOrders)
const agentPlans = computed(() =>
  mes.plans.filter((p) => p.status === '已提交' && p.agentGenerated && p.dispatchSuggestions?.length)
)
const dispatchableWo = computed(() => mes.workOrders.filter((w) => ['已下达', '已派工', '生产中'].includes(w.status)))
const { selected, filtered, onRowClick } = useMesFilter(computed(() => mes.dispatches), ['id', 'workOrderNo'])

const statusItems = computed(() => [
  { label: '待派工工单', value: pendingWo.value.length, warn: pendingWo.value.length > 0 },
  { label: '派工记录', value: mes.dispatches.length },
  { label: '待接收', value: mes.dispatches.filter((d) => d.status === '已分配').length }
])

const rows = computed(() => detailRows(selected.value, [
  { key: 'id', label: '派工单' }, { key: 'processStep', label: '工序' },
  { key: 'operatorName', label: '操作员' }, { key: 'operator', label: '账号' }, { key: 'status', label: '状态' }
]))

onMounted(() => {
  if (route.query.workOrderId) openDialog(String(route.query.workOrderId))
})

watch(() => route.query.workOrderId, (id) => {
  if (id) openDialog(String(id))
})

function onToolbarAction(key) {
  if (key === 'smart') {
    smartPlanId.value = mes.pendingManagerPlans[0]?.id || ''
    smartVisible.value = true
    return
  }
  if (key === 'create') openDialog()
}

function onSmartSuccess() {
  mes.hydrateFromApi?.()
}

function onOperatorChange(username) {
  const user = mes.operatorUsers.find((u) => u.username === username)
  form.operatorName = user?.realName || username
}

function openDialog(workOrderId) {
  form.workOrderId = workOrderId || pendingWo.value[0]?.id || dispatchableWo.value[0]?.id || ''
  form.processStep = processStepOptions.value[0] || ''
  form.operator = mes.operatorUsers[0]?.username || ''
  onOperatorChange(form.operator)
  if (form.workOrderId) {
    const wo = mes.workOrders.find((w) => w.id === form.workOrderId)
    if (wo) form.planQty = Math.min(100, wo.quantity)
  }
  const slot = todaySlot()
  form.planStart = slot.start
  form.planEnd = slot.end
  dialogVisible.value = true
}

async function save() {
  try {
    const d = await mes.createDispatch(form, userStore.username, userStore.roleKey)
    if (d) {
      ElMessage.success(`派工成功，已分配给 ${form.operatorName}（${form.operator}），请操作员登录接收`)
      dialogVisible.value = false
    } else {
      ElMessage.warning('派工失败：请确认工单已下达（状态为「已下达」）')
    }
  } catch (e) {
    ElMessage.error(e?.message || '派工失败')
  }
}

async function agentDispatch(plan) {
  const op = mes.operatorUsers[0]
  if (!op) {
    ElMessage.warning('系统中暂无可用操作员，请先在用户管理中添加操作员')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将按 Agent 建议为计划 ${plan.id} 自动生成工单并批量派工，是否继续？`,
      'Agent 一键派工',
      { type: 'info' }
    )
    const res = await mes.agentBatchDispatch(plan.id, userStore.username, userStore.roleKey, {
      operator: op.username,
      operatorName: op.realName
    })
    ElMessage.success(`已创建 ${res?.count || 0} 条派工，请操作员接收`)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || 'Agent 派工失败')
  }
}

function removeDispatch(row) {
  if (!row) return
  runDelete({
    action: 'deleteDispatch',
    payload: { dispatchId: row.id },
    message: `确定删除派工 ${row.id}？关联报工、质检记录将一并删除。`,
    onSuccess: () => {
      if (selected.value?.id === row.id) selected.value = null
    }
  }).catch(() => {})
}
</script>

<style scoped>
.pending-block {
  padding: 12px 0 4px;
  border-bottom: 1px solid #e8ecf0;
  margin-bottom: 8px;
}
.pending-block__title {
  font-size: 14px;
  font-weight: 600;
  color: #001b3f;
  margin-bottom: 8px;
}
.dispatch-hint {
  font-size: 12px;
  color: #4f5f73;
  margin: 0;
  width: 100%;
}
</style>
