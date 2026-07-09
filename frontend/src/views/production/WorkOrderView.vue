<template>

  <MesPageShell

    :status-items="statusItems"

    toolbar-title="生产工单"

    :status-options="WORK_ORDER_STATUS"

    :detail-rows="rows"

    :logs="mes.operationLogs.slice(0, 10)"

  >

    <template #table>

      <div v-if="isManager && pendingPlans.length" class="pending-block">
        <div class="pending-block__title">主管待生成工单（{{ pendingPlans.length }}）</div>

        <el-table :data="pendingPlans" border stripe size="small" style="margin-bottom: 12px">

          <el-table-column prop="id" label="计划号" width="140" />

          <el-table-column prop="orderNo" label="订单号" width="140" />

          <el-table-column prop="productModel" label="型号" width="130" />

          <el-table-column prop="quantity" label="数量" width="80" />

          <el-table-column label="操作" width="120">

            <template #default="{ row }">

              <el-button link type="primary" @click="createWoFromPlan(row.id)">生成工单</el-button>

            </template>

          </el-table-column>

        </el-table>

      </div>



      <el-table :data="filtered" border stripe highlight-current-row @current-change="onRowClick">

        <el-table-column prop="id" label="工单号" width="130" />

        <el-table-column prop="orderNo" label="订单号" width="130" />

        <el-table-column prop="productModel" label="型号" width="130" />

        <el-table-column prop="quantity" label="计划量" width="80" />

        <el-table-column prop="completedQty" label="完成量" width="80" />

        <el-table-column prop="line" label="产线" width="100" />

        <el-table-column prop="status" label="状态" width="90">

          <template #default="{ row }"><StatusBadge :status="row.status" /></template>

        </el-table-column>

        <el-table-column label="操作" width="160" fixed="right">

          <template #default="{ row }">

            <el-button v-if="isManager && row.status === '草稿'" link type="primary" @click="selectAndRelease(row)">下达工单</el-button>

            <el-button v-if="isManager && row.status === '已下达'" link type="primary" @click="$router.push(`/production/dispatch?workOrderId=${row.id}`)">去派工</el-button>
          </template>

        </el-table-column>

      </el-table>

    </template>

    <template #detail-actions>

      <el-button v-if="isManager && selected?.status === '草稿'" type="primary" size="small" @click="release">下达工单</el-button>

      <el-button v-if="isManager && selected?.status === '已下达'" type="primary" size="small" @click="$router.push(`/production/dispatch?workOrderId=${selected.id}`)">前往派工</el-button>
    </template>

  </MesPageShell>

</template>



<script setup>

import { computed, onMounted } from 'vue'

import { useRoute } from 'vue-router'

import { ElMessage } from 'element-plus'

import { useMesStore } from '@/stores/mes'

import { useUserStore } from '@/stores/user'

import { WORK_ORDER_STATUS } from '@/mock/constants'

import { useMesFilter, detailRows } from '@/composables/useMesPage'

import MesPageShell from '@/components/mes/MesPageShell.vue'

import StatusBadge from '@/components/mes/StatusBadge.vue'



const route = useRoute()

const mes = useMesStore()

const userStore = useUserStore()

const isManager = computed(() => userStore.roleKey === 'manager')
const isOperator = computed(() => userStore.roleKey === 'operator')

const pendingPlans = computed(() => (isManager.value ? mes.pendingManagerPlans : []))

const workOrderSource = computed(() => {
  if (!isOperator.value) return mes.workOrders
  const woNos = new Set(
    mes.myDispatches(userStore.userInfo?.username).map((d) => d.workOrderId || d.workOrderNo)
  )
  return mes.workOrders.filter((w) => woNos.has(w.id) || woNos.has(w.workOrderNo))
})

const { selected, filtered, onRowClick } = useMesFilter(workOrderSource, ['id', 'orderNo'])



const statusItems = computed(() => {
  if (isOperator.value) {
    return [
      { label: '我的相关工单', value: filtered.value.length },
      { label: '生产中', value: filtered.value.filter((w) => w.status === '生产中').length }
    ]
  }
  return [
    { label: '待生成工单', value: pendingPlans.value.length, warn: pendingPlans.value.length > 0 },
    { label: '待下达', value: mes.pendingReleaseWorkOrders.length, warn: mes.pendingReleaseWorkOrders.length > 0 },
    { label: '待派工', value: mes.pendingDispatchWorkOrders.length, warn: mes.pendingDispatchWorkOrders.length > 0 },
    { label: '工单总数', value: mes.workOrders.length }
  ]
})



const rows = computed(() => detailRows(selected.value, [

  { key: 'id', label: '工单号' }, { key: 'productModel', label: '型号' },

  { key: 'quantity', label: '计划' }, { key: 'completedQty', label: '完成' }, { key: 'status', label: '状态' }

]))



onMounted(async () => {
  try {
    await mes.hydrateFromApi()
  } catch {
    /* ignore */
  }
  if (isManager.value && route.query.planId) {
    createWoFromPlan(String(route.query.planId))
  }
})



async function createWoFromPlan(planId) {
  try {
    const wo = await mes.createWorkOrder(planId, userStore.username, userStore.roleKey)
    if (wo) {
      ElMessage.success(`工单 ${wo.id} 已创建并下达，可直接前往派工`)
    } else {
      ElMessage.warning('请确认计划已由计划员提交至主管')
    }
  } catch (e) {
    ElMessage.error(e?.message || '生成工单失败')
  }
}

async function release() {
  if (!selected.value) return
  try {
    await mes.releaseWorkOrder(selected.value.id, userStore.username, userStore.roleKey)
    ElMessage.success('工单已下达，请到「工单派工」分配操作员')
  } catch (e) {
    ElMessage.error(e?.message || '下达工单失败')
  }
}



function selectAndRelease(row) {

  selected.value = row

  release()

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

</style>

