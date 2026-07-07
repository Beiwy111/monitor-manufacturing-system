<template>
  <MesPageShell
    :status-items="statusItems"
    toolbar-title="生产计划"
    :status-options="PLAN_STATUS"
    :toolbar-actions="[{ label: '创建计划', key: 'create', type: 'primary' }]"
    :detail-rows="rows"
    :logs="mes.operationLogs.slice(0, 10)"
    @toolbar-action="onAction"
  >
    <template #table>
      <!-- 已审核、尚未建计划的订单 -->
      <div v-if="pendingOrders.length" class="pending-block">
        <div class="pending-block__title">已审核待计划订单（{{ pendingOrders.length }}）</div>
        <el-table :data="pendingOrders" size="small" style="margin-bottom: 16px">
          <el-table-column prop="id" label="订单号" width="140" />
          <el-table-column prop="customerName" label="客户" min-width="120" />
          <el-table-column prop="productModel" label="型号" width="140" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="openCreateForOrder(row.id)">创建计划</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-table :data="filtered" highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="计划号" width="140" />
        <el-table-column prop="orderNo" label="订单号" width="140" />
        <el-table-column prop="productModel" label="型号" width="130" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="planStart" label="开始" width="110" />
        <el-table-column prop="planEnd" label="结束" width="110" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '草稿'" link type="primary" @click="selectAndPublish(row)">发布</el-button>
            <el-button v-if="['已发布', '执行中'].includes(row.status)" link type="primary" @click="selectAndCreateWo(row)">生成工单</el-button>
            <el-button v-if="row.status === '草稿'" link @click="selectAndCreateWo(row)">发布并生成工单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button v-if="selected?.status === '草稿'" type="primary" size="small" @click="publish">发布计划</el-button>
      <el-button v-if="['已发布', '执行中'].includes(selected?.status)" type="primary" size="small" @click="createWo">生成工单</el-button>
    </template>
  </MesPageShell>

  <el-dialog v-model="dialogVisible" title="创建生产计划" width="440px">
    <el-form label-width="100px">
      <el-form-item label="已审核订单">
        <el-select v-model="form.orderId" style="width:100%" placeholder="请选择已审核订单">
          <el-option v-for="o in pendingOrders" :key="o.id" :label="`${o.id} · ${o.productModel}`" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!pendingOrders.length">
        <el-alert type="info" :closable="false" title="当前没有已审核待计划的订单，请先在 sales 端完成订单审核。" />
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

const route = useRoute()
const mes = useMesStore()
const userStore = useUserStore()
const dialogVisible = ref(false)
const form = reactive({ orderId: '', planStart: '2026-03-01', planEnd: '2026-03-28' })

const pendingOrders = computed(() => mes.pendingPlanOrders)
const { selected, filtered, onRowClick } = useMesFilter(computed(() => mes.plans), ['id', 'orderNo'])

const statusItems = computed(() => [
  { label: '待计划订单', value: pendingOrders.value.length, warn: pendingOrders.value.length > 0 },
  { label: '生产计划', value: mes.plans.length },
  { label: '草稿计划', value: mes.plans.filter(p => p.status === '草稿').length },
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
  if (k === 'create') openCreateForOrder(pendingOrders.value[0]?.id || '')
}

function openCreateForOrder(orderId) {
  form.orderId = orderId || pendingOrders.value[0]?.id || ''
  dialogVisible.value = true
}

function save() {
  const p = mes.createPlan(form, userStore.displayName, userStore.roleKey)
  if (p) {
    ElMessage.success(`计划 ${p.id} 已创建，请发布后可生成工单`)
    dialogVisible.value = false
  } else {
    ElMessage.warning('请选择有效的已审核订单')
  }
}

function publish() {
  if (!selected.value) return
  if (mes.publishPlan(selected.value.id, userStore.displayName, userStore.roleKey)) {
    ElMessage.success('计划已发布')
  }
}

function createWo() {
  if (!selected.value) return
  const wo = mes.createWorkOrder(selected.value.id, userStore.displayName, userStore.roleKey)
  if (wo) ElMessage.success(`工单 ${wo.id} 已创建，请到「生产工单」下达`)
  else ElMessage.warning('请先发布计划')
}

function selectAndPublish(row) {
  selected.value = row
  publish()
}

function selectAndCreateWo(row) {
  selected.value = row
  if (row.status === '草稿') {
    mes.publishPlan(row.id, userStore.displayName, userStore.roleKey)
  }
  createWo()
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
