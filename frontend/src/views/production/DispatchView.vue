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
      <el-button type="warning" @click="openAutoDispatch">一键派工</el-button>
      <el-button type="primary" @click="openManualDispatch">手动派工</el-button>
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

    <SmartDispatchDialog
      v-model="smartVisible"
      :default-plan-id="smartPlanId"
      :manual="smartManual"
      :intent="smartIntent"
      @success="refresh"
    />
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { DISPATCH_STATUS } from '@/mock/constants'
import { useMesDelete } from '@/composables/useMesDelete'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import SmartDispatchDialog from '@/components/mes/SmartDispatchDialog.vue'

const route = useRoute()
const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)

const keyword = ref('')
const statusFilter = ref('')
const smartVisible = ref(false)
const smartPlanId = ref('')
const smartManual = ref(false)
const smartIntent = ref('dispatch')

const pendingWo = computed(() => mes.pendingDispatchWorkOrders)
const assignedCount = computed(() => mes.dispatches.filter((d) => d.status === '已分配').length)

function planIdFromWorkOrder(woId) {
  const wo = mes.workOrders.find((w) => w.id === woId)
  return wo?.planId || mes.plans.find((p) => p.orderNo === wo?.orderNo)?.id || ''
}

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
  if (route.query.workOrderId) openManualDispatch(String(route.query.workOrderId))
})

watch(() => route.query.workOrderId, (id) => { if (id) openManualDispatch(String(id)) })

async function refresh() {
  try { await mes.hydrateForPage() } catch { /* ignore */ }
}

function openAutoDispatch() {
  const wo = pendingWo.value[0]
  smartPlanId.value = mes.pendingManagerPlans[0]?.id || planIdFromWorkOrder(wo?.id) || ''
  smartManual.value = false
  smartIntent.value = 'dispatch'
  smartVisible.value = true
}

function openManualDispatch(workOrderId) {
  const woId = workOrderId || pendingWo.value[0]?.id || ''
  smartPlanId.value = planIdFromWorkOrder(woId)
  if (!smartPlanId.value) {
    ElMessage.warning('请先选择有待派工的生产工单')
    return
  }
  smartManual.value = true
  smartIntent.value = 'dispatch'
  smartVisible.value = true
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
