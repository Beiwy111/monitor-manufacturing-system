<template>
  <MesPageShell toolbar-title="不合格品处理" :detail-rows="rows" :logs="mes.operationLogs.slice(0,8)">
    <template #table>
      <el-table :data="defectList" highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="编号" width="120" />
        <el-table-column prop="workOrderId" label="工单" width="130" />
        <el-table-column prop="defectLocation" label="问题部位" width="110" />
        <el-table-column prop="severity" label="严重程度" width="90">
          <template #default="{ row }">
            <StatusBadge :status="row.severity === '严重' ? '不合格' : '让步接收'" />
            <span style="margin-left:4px">{{ row.severity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column prop="disposition" label="处置建议" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-extra>
      <div v-if="selected" class="defect-detail">
        <p><strong>问题描述：</strong>{{ selected.description }}</p>
        <p v-if="selected.failedItems?.length"><strong>不合格项：</strong>{{ selected.failedItems.join('、') }}</p>
        <p><strong>生产操作员：</strong>{{ selected.operatorName || selected.operator || '-' }}</p>
      </div>
    </template>
    <template #detail-actions>
      <template v-if="selected?.status === '待处理'">
        <el-button type="danger" size="small" @click="scrap">直接报废</el-button>
        <el-button type="primary" size="small" @click="rework">派返修（操作员）</el-button>
      </template>
      <el-tag v-else-if="selected?.status === '返修中'" type="warning">返修中，操作员完成后需再次提交质检</el-tag>
      <el-tag v-else-if="selected?.status === '已返修'" type="success">返修完成</el-tag>
      <el-tag v-else-if="selected?.status === '已报废'" type="info">已报废</el-tag>
    </template>
  </MesPageShell>

  <el-dialog v-model="scrapDialog" title="确认报废" width="420px">
    <p>确定将 {{ selected?.quantity }} 台不合格品直接报废？此操作不可撤销。</p>
    <el-input v-model="scrapRemark" type="textarea" rows="2" placeholder="报废原因说明" />
    <template #footer>
      <el-button @click="scrapDialog = false">取消</el-button>
      <el-button type="danger" @click="confirmScrap">确认报废</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const defectList = computed(() => mes.defects)
const { selected, onRowClick } = useMesFilter(defectList, ['id'])
const scrapDialog = ref(false)
const scrapRemark = ref('')

const rows = computed(() => detailRows(selected.value, [
  { key: 'id', label: '编号' },
  { key: 'workOrderId', label: '工单' },
  { key: 'batchNo', label: '批次' },
  { key: 'defectLocation', label: '问题部位' },
  { key: 'severity', label: '严重程度' },
  { key: 'quantity', label: '数量' },
  { key: 'status', label: '状态' }
]))

function scrap() {
  scrapRemark.value = selected.value?.description || ''
  scrapDialog.value = true
}

function confirmScrap() {
  if (!selected.value) return
  if (mes.scrapDefect(selected.value.id, userStore.userInfo.username, userStore.roleKey, scrapRemark.value)) {
    ElMessage.success('已标记报废')
    scrapDialog.value = false
  } else {
    ElMessage.error('操作失败')
  }
}

function rework() {
  if (!selected.value) return
  const dispatchId = mes.reworkDefect(selected.value.id, userStore.userInfo.username, userStore.roleKey)
  if (dispatchId) {
    ElMessage.success(`已派返修给 ${selected.value.operatorName || '操作员'}，请通知其接收返修任务`)
  } else {
    ElMessage.error('派返修失败')
  }
}
</script>

<style scoped>
.defect-detail {
  margin-top: 12px;
  font-size: 13px;
  color: #4f5f73;
  line-height: 1.6;
}
</style>
