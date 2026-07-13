<template>
  <div class="ruoyi-page dispatch-page">
    <div class="ruoyi-stats">
      <span class="ruoyi-stats__item ruoyi-stats__item--warn">待派工：<em>{{ pendingWo.length }}</em></span>
      <span class="ruoyi-stats__item">派工记录：<em>{{ mes.dispatches.length }}</em></span>
      <span class="ruoyi-stats__item">待接收：<em>{{ assignedCount }}</em></span>
    </div>

    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">工单派工</span>
      <el-input v-model="keyword" clearable placeholder="派工单号 / 工单号" style="width: 200px" />
      <el-select v-model="statusFilter" clearable placeholder="状态" style="width: 110px">
        <el-option v-for="s in DISPATCH_STATUS" :key="s" :label="s" :value="s" />
      </el-select>
      <el-button type="warning" @click="openSmart">智能派工</el-button>
      <el-button type="primary" @click="openManual">手动派工</el-button>
      <el-button @click="refresh">刷新</el-button>
    </div>

    <el-table :data="filteredDispatches" border stripe size="small">
      <el-table-column prop="id" label="派工单号" width="130" />
      <el-table-column prop="workOrderNo" label="工单号" width="130" />
      <el-table-column prop="processStep" label="工序" width="100" />
      <el-table-column prop="workshopName" label="车间" min-width="110" show-overflow-tooltip />
      <el-table-column prop="equipment" label="设备" width="100" />
      <el-table-column prop="operatorName" label="操作员" width="90" />
      <el-table-column prop="planQty" label="数量" width="72" align="right" />
      <el-table-column prop="status" label="状态" width="88">
        <template #default="{ row }"><StatusBadge :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="72" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="removeDispatch(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="manualVisible" title="手动派工" width="480px">
      <el-form label-width="88px" size="small">
        <el-form-item label="生产工单">
          <el-select v-model="form.workOrderId" style="width:100%" @change="onWoChange">
            <el-option v-for="w in dispatchableWo" :key="w.id" :label="`${w.id} · ${w.productModel}`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="工序">
          <el-select v-model="form.processStep" style="width:100%">
            <el-option v-for="s in stepOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备">
          <el-select v-model="form.equipment" style="width:100%" filterable allow-create>
            <el-option v-for="eq in mes.equipment" :key="eq.id" :label="eq.name" :value="eq.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作员">
          <el-select v-model="form.operator" style="width:100%" @change="onOperatorChange">
            <el-option v-for="u in mes.boundOperatorUsers" :key="u.username" :label="operatorLabel(u)" :value="u.username" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="form.planQty" :min="1" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!form.workOrderId || !form.operator" @click="saveManual">确认</el-button>
      </template>
    </el-dialog>

    <SmartDispatchDialog v-model="smartVisible" :default-plan-id="smartPlanId" @success="refresh" />
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { DISPATCH_STATUS } from '@/mock/constants'
import { fetchManagerPlanContext } from '@/api/mes'
import { operatorLabel } from '@/utils/operatorWorkshop'
import { useMesDelete } from '@/composables/useMesDelete'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import SmartDispatchDialog from '@/components/mes/SmartDispatchDialog.vue'

const route = useRoute()
const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)

const keyword = ref('')
const statusFilter = ref('')
const manualVisible = ref(false)
const smartVisible = ref(false)
const smartPlanId = ref('')
const stepOptions = ref([])

const form = reactive({
  workOrderId: '',
  processStep: '',
  equipment: '',
  operator: '',
  operatorName: '',
  planQty: 100
})

const pendingWo = computed(() => mes.pendingDispatchWorkOrders)
const assignedCount = computed(() => mes.dispatches.filter((d) => d.status === '已分配').length)
const dispatchableWo = computed(() => mes.workOrders.filter((w) => ['已下达', '已派工', '生产中'].includes(w.status)))

const filteredDispatches = computed(() => {
  let list = mes.dispatches
  if (keyword.value) {
    const k = keyword.value.toLowerCase()
    list = list.filter((d) => [d.id, d.workOrderNo].some((f) => String(f).toLowerCase().includes(k)))
  }
  if (statusFilter.value) list = list.filter((d) => d.status === statusFilter.value)
  return list
})

onMounted(async () => {
  await refresh()
  if (route.query.workOrderId) openManual(String(route.query.workOrderId))
})

watch(() => route.query.workOrderId, (id) => { if (id) openManual(String(id)) })

async function refresh() {
  try { await mes.hydrateFromApi() } catch { /* ignore */ }
}

async function loadStepsForWo(woId) {
  const wo = mes.workOrders.find((w) => w.id === woId)
  if (!wo?.planId) {
    stepOptions.value = mes.processSteps || []
    return
  }
  try {
    const ctx = await fetchManagerPlanContext(wo.planId)
    stepOptions.value = (ctx.processRoute || []).map((s) => s.stepName)
  } catch {
    stepOptions.value = mes.processSteps || []
  }
}

async function onWoChange(woId) {
  const wo = mes.workOrders.find((w) => w.id === woId)
  if (wo) form.planQty = wo.quantity
  await loadStepsForWo(woId)
  form.processStep = stepOptions.value[0] || ''
}

function openSmart() {
  smartPlanId.value = mes.pendingManagerPlans[0]?.id || ''
  smartVisible.value = true
}

function onOperatorChange(username) {
  const user = mes.boundOperatorUsers.find((u) => u.username === username)
  form.operatorName = user?.realName || username
}

async function openManual(workOrderId) {
  form.workOrderId = workOrderId || pendingWo.value[0]?.id || dispatchableWo.value[0]?.id || ''
  form.operator = mes.boundOperatorUsers[0]?.username || ''
  onOperatorChange(form.operator)
  if (form.workOrderId) await onWoChange(form.workOrderId)
  manualVisible.value = true
}

async function saveManual() {
  try {
    const d = await mes.createDispatch(form, userStore.username, userStore.roleKey)
    if (d) {
      ElMessage.success(`已派给 ${form.operatorName}`)
      manualVisible.value = false
      await refresh()
    }
  } catch (e) {
    ElMessage.error(e?.message || '派工失败')
  }
}

function removeDispatch(row) {
  runDelete({
    action: 'deleteDispatch',
    payload: { dispatchId: row.id },
    message: `确定删除派工 ${row.id}？`,
  }).catch(() => {})
}
</script>

<style scoped>
.dispatch-page { padding: 0 4px; }
</style>
