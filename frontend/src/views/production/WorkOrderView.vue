<template>
  <div class="ruoyi-page manager-wo-page">
    <div class="ruoyi-stats">
      <span class="ruoyi-stats__item" :class="{ 'ruoyi-stats__item--warn': tabCounts.pendingPlan > 0 }">
        待生成工单：<em>{{ tabCounts.pendingPlan }}</em>
      </span>
      <span class="ruoyi-stats__item" :class="{ 'ruoyi-stats__item--warn': tabCounts.pendingDispatch > 0 }">
        待派工：<em>{{ tabCounts.pendingDispatch }}</em>
      </span>
      <span class="ruoyi-stats__item">执行中：<em>{{ tabCounts.executing }}</em></span>
      <span class="ruoyi-stats__item" :class="{ 'ruoyi-stats__item--danger': tabCounts.abnormal > 0 }">
        异常：<em>{{ tabCounts.abnormal }}</em>
      </span>
    </div>

    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">生产工单</span>
      <el-input v-model="keyword" clearable placeholder="计划号 / 工单号 / 订单号" style="width: 220px" />
      <el-button @click="refresh">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="wo-tabs">
      <el-tab-pane label="待生成工单" name="pendingPlan">
        <el-table :data="filteredPlans" border stripe size="small" highlight-current-row>
          <el-table-column prop="id" label="计划号" width="130" />
          <el-table-column prop="orderNo" label="订单号" width="130" />
          <el-table-column prop="productModel" label="型号" min-width="120" show-overflow-tooltip />
          <el-table-column prop="quantity" label="数量" width="72" align="right" />
          <el-table-column prop="planEnd" label="计划完工" width="110" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPlanDetail(row)">详情</el-button>
              <el-button link type="warning" @click="openSmart(row.id)">智能派工</el-button>
              <el-button link type="success" @click="createWo(row.id)">生成工单</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="待派工" name="pendingDispatch">
        <el-table :data="filteredPendingDispatch" border stripe size="small">
          <el-table-column prop="id" label="工单号" width="130" />
          <el-table-column prop="orderNo" label="订单号" width="130" />
          <el-table-column prop="productModel" label="型号" min-width="120" />
          <el-table-column prop="quantity" label="数量" width="72" align="right" />
          <el-table-column prop="status" label="状态" width="88">
            <template #default="{ row }"><StatusBadge :status="row.status" /></template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="warning" @click="openSmartByWo(row)">智能派工</el-button>
              <el-button link type="primary" @click="goDispatch(row.id)">手动派工</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="执行中" name="executing">
        <el-table :data="filteredExecuting" border stripe size="small">
          <el-table-column prop="id" label="工单号" width="130" />
          <el-table-column prop="orderNo" label="订单号" width="130" />
          <el-table-column prop="productModel" label="型号" min-width="120" />
          <el-table-column prop="quantity" label="计划" width="64" align="right" />
          <el-table-column prop="completedQty" label="完成" width="64" align="right" />
          <el-table-column prop="line" label="产线/车间" width="110" />
          <el-table-column prop="status" label="状态" width="88">
            <template #default="{ row }"><StatusBadge :status="row.status" /></template>
          </el-table-column>
          <el-table-column label="进度" min-width="120">
            <template #default="{ row }">
              <el-progress :percentage="woProgress(row)" :stroke-width="8" />
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="异常" name="abnormal">
        <el-table :data="filteredAbnormal" border stripe size="small">
          <el-table-column prop="id" label="工单号" width="130" />
          <el-table-column prop="orderNo" label="订单号" width="130" />
          <el-table-column prop="productModel" label="型号" min-width="120" />
          <el-table-column prop="status" label="状态" width="88">
            <template #default="{ row }"><StatusBadge :status="row.status" /></template>
          </el-table-column>
          <el-table-column label="关联报警" min-width="160">
            <template #default="{ row }">{{ relatedAlarms(row.id) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push('/device/alarm')">查看报警</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="planDrawer" title="计划详情" size="560px" destroy-on-close>
      <template v-if="planContext">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="计划号">{{ planContext.planId }}</el-descriptions-item>
          <el-descriptions-item label="订单">{{ planContext.orderId }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ planContext.customerName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ planContext.productModel }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ planContext.quantity }} 台</el-descriptions-item>
          <el-descriptions-item label="交期">{{ planContext.deliveryDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="周期" :span="2">{{ planContext.planStart }} ~ {{ planContext.planEnd }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">工艺路线（来自数据库）</el-divider>
        <el-table :data="planContext.processRoute || []" border stripe size="small">
          <el-table-column prop="stepNo" label="序号" width="56" align="center" />
          <el-table-column prop="stepName" label="工序" min-width="120" />
          <el-table-column prop="standardEquipmentType" label="设备类型" width="100" />
          <el-table-column prop="standardWorkHours" label="工时" width="72" align="right" />
        </el-table>
        <div class="drawer-actions">
          <el-button type="success" :loading="creating" @click="createWo(planContext.planId)">生成总工单</el-button>
          <el-button type="warning" @click="openSmart(planContext.planId)">智能派工（按工序）</el-button>
        </div>
      </template>
      <el-skeleton v-else :rows="6" animated />
    </el-drawer>

    <SmartDispatchDialog v-model="smartVisible" :default-plan-id="smartPlanId" @success="onSmartSuccess" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { fetchManagerPlanContext } from '@/api/mes'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import SmartDispatchDialog from '@/components/mes/SmartDispatchDialog.vue'

const route = useRoute()
const router = useRouter()
const mes = useMesStore()
const userStore = useUserStore()

const activeTab = ref('pendingPlan')
const keyword = ref('')
const planDrawer = ref(false)
const planContext = ref(null)
const creating = ref(false)
const smartVisible = ref(false)
const smartPlanId = ref('')

const tabCounts = computed(() => ({
  pendingPlan: mes.pendingManagerPlans.length,
  pendingDispatch: mes.pendingDispatchWorkOrders.length,
  executing: mes.executingWorkOrders.length,
  abnormal: mes.abnormalWorkOrders.length
}))

function matchKeyword(item) {
  if (!keyword.value) return true
  const k = keyword.value.toLowerCase()
  return [item.id, item.orderNo, item.orderId, item.planId].some((f) => String(f || '').toLowerCase().includes(k))
}

const filteredPlans = computed(() => mes.pendingManagerPlans.filter(matchKeyword))
const filteredPendingDispatch = computed(() => mes.pendingDispatchWorkOrders.filter(matchKeyword))
const filteredExecuting = computed(() => mes.executingWorkOrders.filter(matchKeyword))
const filteredAbnormal = computed(() => mes.abnormalWorkOrders.filter(matchKeyword))

onMounted(async () => {
  await refresh()
  if (route.query.planId) {
    activeTab.value = 'pendingPlan'
    openPlanDetail({ id: String(route.query.planId) })
  }
})

async function refresh() {
  try { await mes.hydrateFromApi() } catch { /* ignore */ }
}

function woProgress(row) {
  if (!row?.quantity) return 0
  return Math.min(100, Math.round((row.completedQty || 0) / row.quantity * 100))
}

function relatedAlarms(woId) {
  return mes.alarms
    .filter((a) => a.workOrderId === woId && a.status !== '已关闭')
    .map((a) => a.type)
    .join('、') || '—'
}

async function openPlanDetail(row) {
  if (!row?.id) return
  planDrawer.value = true
  planContext.value = null
  try {
    planContext.value = await fetchManagerPlanContext(row.id)
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
    planDrawer.value = false
  }
}

async function createWo(planId) {
  creating.value = true
  try {
    const wo = await mes.createWorkOrder(planId, userStore.username, userStore.roleKey)
    if (wo) {
      ElMessage.success(`工单 ${wo.id} 已创建`)
      activeTab.value = 'pendingDispatch'
      planDrawer.value = false
      await refresh()
    } else {
      ElMessage.warning('请确认计划已提交至主管')
    }
  } catch (e) {
    ElMessage.error(e?.message || '生成失败')
  } finally {
    creating.value = false
  }
}

function openSmart(planId) {
  smartPlanId.value = planId
  smartVisible.value = true
}

function openSmartByWo(wo) {
  smartPlanId.value = wo.planId || mes.plans.find((p) => p.orderNo === wo.orderNo)?.id || ''
  smartVisible.value = true
}

function goDispatch(woId) {
  router.push(`/production/dispatch?workOrderId=${woId}`)
}

function onSmartSuccess() {
  refresh()
  activeTab.value = 'executing'
}
</script>

<style scoped>
.manager-wo-page { padding: 0 4px; }
.wo-tabs { margin-top: 8px; }
.drawer-actions { margin-top: 16px; display: flex; gap: 8px; }
</style>
