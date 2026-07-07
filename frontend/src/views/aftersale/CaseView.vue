<template>
  <MesPageShell
    toolbar-title="售后登记"
    :status-options="AFTERSALE_STATUS"
    :toolbar-actions="[{ label: '登记反馈', key: 'add', type: 'primary' }]"
    :detail-rows="rows"
    :logs="mes.operationLogs.slice(0,8)"
    @toolbar-action="dialogVisible=true"
  >
    <template #table>
      <el-table :data="mes.aftersaleCases" highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="售后单" width="130" />
        <el-table-column prop="orderId" label="订单" width="130" />
        <el-table-column prop="customerName" label="客户" min-width="120" />
        <el-table-column prop="feedback" label="反馈" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-actions>
      <el-button size="small" @click="trace">质量追溯</el-button>
      <el-button v-if="selected?.status!=='已关闭'" type="primary" size="small" @click="process">处理完成</el-button>
    </template>
    <template #detail-extra>
      <ProcessTimeline v-if="chain" :items="traceItems" />
    </template>
  </MesPageShell>
  <el-dialog v-model="dialogVisible" title="售后登记" width="440px">
    <el-form label-width="90px">
      <el-form-item label="订单号"><el-input v-model="form.orderId" /></el-form-item>
      <el-form-item label="批次号"><el-input v-model="form.batchNo" /></el-form-item>
      <el-form-item label="客户"><el-input v-model="form.customerName" /></el-form-item>
      <el-form-item label="反馈"><el-input v-model="form.feedback" type="textarea" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">登记</el-button></template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { AFTERSALE_STATUS } from '@/mock/constants'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import ProcessTimeline from '@/components/mes/ProcessTimeline.vue'

const mes = useMesStore()
const userStore = useUserStore()
const dialogVisible = ref(false)
const form = reactive({ orderId: '', batchNo: '', customerName: '', productModel: 'DM-24-LCD-FHD', feedback: '' })
const { selected, onRowClick } = useMesFilter(computed(() => mes.aftersaleCases), ['id'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'orderId', label: '订单' }, { key: 'batchNo', label: '批次' }, { key: 'feedback', label: '反馈' }, { key: 'status', label: '状态' }
]))
const chain = computed(() => selected.value ? mes.traceChain(selected.value.orderId) : null)
const traceItems = computed(() => {
  if (!chain.value?.order) return []
  return [
    { title: '订单', desc: chain.value.order.status },
    { title: '工单', desc: chain.value.wo?.id || '-' },
    { title: '报工', desc: `${chain.value.reports?.length || 0} 条` },
    { title: '质检', desc: `${chain.value.inspections?.length || 0} 条` },
    { title: '发货', desc: chain.value.deliveries?.[0]?.status || '-' }
  ]
})
function save() {
  mes.createAftersale(form, userStore.displayName, userStore.roleKey)
  ElMessage.success('已登记')
  dialogVisible.value = false
}
function trace() {
  if (selected.value) mes.processAftersale(selected.value.id, { status: '追溯中' }, userStore.displayName, userStore.roleKey)
  ElMessage.success('已进入追溯')
}
function process() {
  mes.processAftersale(selected.value.id, { status: '已关闭', result: '已处理完毕' }, userStore.displayName, userStore.roleKey)
  ElMessage.success('售后已关闭')
}
</script>
