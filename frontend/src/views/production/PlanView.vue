<template>

  <MesPageShell

    :status-items="statusItems"

    toolbar-title="生产计划"

    :status-options="PLAN_STATUS"

    :toolbar-actions="[
      { label: '智能排产（库存分析）', key: 'agent', type: 'success' },
      { label: '创建计划', key: 'create', type: 'primary' }
    ]"

    :detail-rows="rows"

    :logs="mes.operationLogs.slice(0, 10)"

    @toolbar-action="onAction"

  >

    <template #table>

      <div v-if="pendingOrders.length" class="pending-block">

        <div class="pending-block__title">待计划订单（{{ pendingOrders.length }}）</div>

        <el-table :data="pendingOrders" border stripe size="small" style="margin-bottom: 12px">

          <el-table-column prop="id" label="订单号" width="140" />

          <el-table-column prop="customerName" label="客户" min-width="120" />

          <el-table-column prop="productModel" label="型号" width="140" />

          <el-table-column prop="quantity" label="数量" width="80" />

          <el-table-column label="操作" width="120">

            <template #default="{ row }">

              <el-button link type="success" @click="openAgentForOrder(row.id)">智能排产</el-button>

              <el-button link type="primary" @click="openCreateForOrder(row.id)">手动创建</el-button>

            </template>

          </el-table-column>

        </el-table>

      </div>



      <div v-if="pendingSubmitPlans.length" class="pending-block">

        <div class="pending-block__title">已发布待提交主管（{{ pendingSubmitPlans.length }}）</div>

        <el-table :data="pendingSubmitPlans" border stripe size="small" style="margin-bottom: 12px">

          <el-table-column prop="id" label="计划号" width="140" />

          <el-table-column prop="orderNo" label="订单号" width="140" />

          <el-table-column prop="productModel" label="型号" width="130" />

          <el-table-column label="操作" width="140">

            <template #default="{ row }">

              <el-button link type="primary" @click="submitToManager(row.id)">提交生产主管</el-button>

            </template>

          </el-table-column>

        </el-table>

      </div>



      <el-table :data="filtered" border stripe highlight-current-row @current-change="onRowClick">

        <el-table-column prop="id" label="计划号" width="140" />

        <el-table-column prop="orderNo" label="订单号" width="140" />

        <el-table-column prop="productModel" label="型号" width="130" />

        <el-table-column prop="quantity" label="数量" width="80" />

        <el-table-column prop="planStart" label="开始" width="110" />

        <el-table-column prop="planEnd" label="结束" width="110" />

        <el-table-column prop="status" label="状态" width="90">

          <template #default="{ row }"><StatusBadge :status="row.status" /></template>

        </el-table-column>

        <el-table-column label="Agent" width="90" align="center">

          <template #default="{ row }">

            <el-tag v-if="row.agentGenerated" type="success" size="small">智能</el-tag>

          </template>

        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">

          <template #default="{ row }">

            <el-button v-if="row.status === '草稿'" link type="primary" @click="selectAndPublish(row)">发布</el-button>

            <el-button v-if="row.status === '已发布'" link type="primary" @click="selectAndSubmit(row)">提交主管</el-button>

          </template>

        </el-table-column>

      </el-table>

    </template>

    <template #detail-actions>

      <el-button v-if="selected?.status === '草稿'" type="primary" size="small" @click="publish">发布计划</el-button>

      <el-button v-if="selected?.status === '已发布'" type="primary" size="small" @click="submitToManager(selected.id)">提交生产主管</el-button>

    </template>

  </MesPageShell>



  <el-dialog v-model="dialogVisible" title="创建生产计划" width="440px">

    <el-form label-width="100px">

      <el-form-item label="待计划订单">

        <el-select v-model="form.orderId" style="width:100%" placeholder="请选择待计划订单">

          <el-option v-for="o in pendingOrders" :key="o.id" :label="`${o.id} · ${o.productModel}`" :value="o.id" />

        </el-select>

      </el-form-item>

      <el-form-item v-if="!pendingOrders.length">

        <el-alert type="info" :closable="false" title="当前没有待计划订单，请先完成订单审核。" />

      </el-form-item>

      <el-form-item label="计划开始">

        <el-date-picker v-model="form.planStart" type="date" value-format="YYYY-MM-DD" style="width:100%" />

      </el-form-item>

      <el-form-item label="计划结束">

        <el-date-picker v-model="form.planEnd" type="date" value-format="YYYY-MM-DD" style="width:100%" />

      </el-form-item>

    </el-form>

    <template #footer>

      <el-button @click="dialogVisible = false">取消</el-button>

      <el-button type="primary" :disabled="!form.orderId" @click="save">保存</el-button>

    </template>

  </el-dialog>

  <PlannerAgentDialog v-model="agentVisible" :default-order-id="agentOrderId" @success="onAgentSuccess" />

</template>



<script setup>

import { computed, ref, reactive, onMounted } from 'vue'

import { useRoute } from 'vue-router'

import { ElMessage } from 'element-plus'

import { useMesStore } from '@/stores/mes'

import { useUserStore } from '@/stores/user'

import { PLAN_STATUS } from '@/mock/constants'

import { useMesFilter, detailRows } from '@/composables/useMesPage'

import MesPageShell from '@/components/mes/MesPageShell.vue'

import StatusBadge from '@/components/mes/StatusBadge.vue'

import PlannerAgentDialog from '@/components/mes/PlannerAgentDialog.vue'



const route = useRoute()

const mes = useMesStore()

const userStore = useUserStore()

const dialogVisible = ref(false)

const agentVisible = ref(false)

const agentOrderId = ref('')

const form = reactive({ orderId: '', planStart: '2026-03-01', planEnd: '2026-03-28' })



const pendingOrders = computed(() => mes.pendingPlanOrders)

const pendingSubmitPlans = computed(() => mes.pendingSubmitPlans)

const { selected, filtered, onRowClick } = useMesFilter(computed(() => mes.plans), ['id', 'orderNo'])



const statusItems = computed(() => [

  { label: '待计划订单', value: pendingOrders.value.length, warn: pendingOrders.value.length > 0 },

  { label: '待提交主管', value: pendingSubmitPlans.value.length, warn: pendingSubmitPlans.value.length > 0 },

  { label: '生产计划', value: mes.plans.length },

  { label: '执行中', value: mes.plans.filter(p => p.status === '执行中').length }

])



const rows = computed(() => detailRows(selected.value, [

  { key: 'id', label: '计划号' }, { key: 'orderNo', label: '订单' },

  { key: 'quantity', label: '数量' }, { key: 'status', label: '状态' }

]))



onMounted(() => {

  if (route.query.orderId) openCreateForOrder(String(route.query.orderId))

})



function onAction(k) {

  if (k === 'agent') {

    agentOrderId.value = pendingOrders.value[0]?.id || ''

    agentVisible.value = true

    return

  }

  if (k === 'create') openCreateForOrder(pendingOrders.value[0]?.id || '')

}

function onAgentSuccess() {

  mes.hydrateFromApi?.()

}



function openCreateForOrder(orderId) {

  form.orderId = orderId || pendingOrders.value[0]?.id || ''

  dialogVisible.value = true

}

function openAgentForOrder(orderId) {

  agentOrderId.value = orderId

  agentVisible.value = true

}



async function save() {
  try {
    const p = await mes.createPlan(form, userStore.username, userStore.roleKey)
    if (p) {
      ElMessage.success(`计划 ${p.id} 已创建，请发布后提交生产主管`)
      dialogVisible.value = false
    } else {
      ElMessage.warning('请选择有效的待计划订单')
    }
  } catch (e) {
    ElMessage.error(e?.message || '创建计划失败')
  }
}



async function publish() {

  if (!selected.value) return

  if (await mes.publishPlan(selected.value.id, userStore.username, userStore.roleKey)) {

    ElMessage.success('计划已发布，请提交生产主管')

  }

}



async function submitToManager(planId) {

  if (!planId) return

  const ok = await mes.submitPlanToManager(planId, userStore.username, userStore.roleKey)

  if (ok !== false) {

    ElMessage.success('计划已提交生产主管，请主管生成工单')

  } else {

    ElMessage.warning('计划状态不允许提交主管')

  }

}



function selectAndPublish(row) {

  selected.value = row

  publish()

}



function selectAndSubmit(row) {

  selected.value = row

  submitToManager(row.id)

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

