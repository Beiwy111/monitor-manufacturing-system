<template>
  <MesPageShell toolbar-title="待检任务" :status-options="['待检']" :detail-rows="rows" :logs="mes.operationLogs.slice(0,8)">
    <template #table>
      <el-table :data="pending" border stripe highlight-current-row @current-change="onRowClick">
        <el-table-column prop="id" label="质检单" width="130" />
        <el-table-column prop="workOrderId" label="工单" width="130" />
        <el-table-column prop="operatorName" label="操作员" width="100" />
        <el-table-column prop="submitQty" label="送检数" width="80" />
        <el-table-column prop="qcType" label="类型" width="90" />
        <el-table-column prop="batchNo" label="批次" min-width="140" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>
    </template>
    <template #detail-extra>
      <el-form v-if="selected" label-width="100px" style="margin-top:12px">
        <el-form-item label="检验类型">
          <el-select v-model="form.qcType" style="width:100%">
            <el-option v-for="t in QC_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="检验项目">
          <el-checkbox-group v-model="form.qcItems">
            <el-checkbox v-for="i in QC_ITEMS" :key="i" :label="i" :value="i" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="抽样数"><el-input-number v-model="form.sampleQty" :min="1" /></el-form-item>
        <el-form-item label="合格数"><el-input-number v-model="form.qualifiedQty" :min="0" /></el-form-item>
        <el-form-item label="不合格数"><el-input-number v-model="form.unqualifiedQty" :min="0" /></el-form-item>
        <el-form-item label="判定">
          <el-radio-group v-model="form.result">
            <el-radio value="合格">合格 → 全部流转仓储入库</el-radio>
            <el-radio value="不合格">不合格 → 合格品入库，不合格品标记处理</el-radio>
            <el-radio value="让步接收">让步接收</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.result === '不合格'">
          <el-divider content-position="left">不合格品信息</el-divider>
          <el-form-item label="问题部位">
            <el-select v-model="form.defectLocation" style="width:100%" placeholder="请选择问题所在部位">
              <el-option v-for="loc in DEFECT_LOCATIONS" :key="loc" :label="loc" :value="loc" />
            </el-select>
          </el-form-item>
          <el-form-item label="不合格项">
            <el-checkbox-group v-model="form.failedItems">
              <el-checkbox v-for="i in QC_ITEMS" :key="i" :label="i" :value="i" />
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="严重程度">
            <el-radio-group v-model="form.severity">
              <el-radio value="轻微">轻微（可返修）</el-radio>
              <el-radio value="严重">严重（建议报废）</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="问题描述">
            <el-input v-model="form.description" type="textarea" rows="2" placeholder="说明具体缺陷，如坏点数量、外观划痕位置等" />
          </el-form-item>
        </template>

        <el-button type="primary" @click="submit">提交质检结果</el-button>
      </el-form>
    </template>
  </MesPageShell>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { QC_ITEMS, QC_TYPES, DEFECT_LOCATIONS } from '@/mock/constants'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'

const mes = useMesStore()
const userStore = useUserStore()
const pending = computed(() => mes.pendingInspections)
const { selected, onRowClick } = useMesFilter(pending, ['id'])
const form = reactive({
  qcType: '终检',
  qcItems: ['点亮测试', '坏点检测'],
  sampleQty: 10,
  qualifiedQty: 10,
  unqualifiedQty: 0,
  result: '合格',
  inspectorName: '',
  remark: '',
  defectLocation: '',
  failedItems: [],
  severity: '轻微',
  description: ''
})

watch(selected, (row) => {
  if (!row) return
  form.qcType = row.qcType || '终检'
  form.sampleQty = row.sampleQty || Math.min(10, row.submitQty || 10)
  form.qualifiedQty = row.submitQty || row.sampleQty || 10
  form.unqualifiedQty = 0
  form.result = '合格'
})

const rows = computed(() => detailRows(selected.value, [
  { key: 'id', label: '质检单' },
  { key: 'productModel', label: '型号' },
  { key: 'submitQty', label: '送检数量' },
  { key: 'operatorName', label: '生产操作员' },
  { key: 'batchNo', label: '批次' },
  { key: 'status', label: '状态' }
]))

async function submit() {
  if (!selected.value) return
  if (form.result === '不合格') {
    if (!form.defectLocation) {
      ElMessage.warning('请标记问题部位')
      return
    }
    if (!form.failedItems.length) {
      ElMessage.warning('请选择不合格检验项')
      return
    }
    if (!form.description.trim()) {
      ElMessage.warning('请填写问题描述')
      return
    }
    if (form.unqualifiedQty <= 0) {
      ElMessage.warning('请填写不合格数量')
      return
    }
  }
  form.inspectorName = userStore.displayName
  try {
    const ok = await mes.submitInspection(selected.value.id, form, userStore.userInfo.username, 'quality')
    if (!ok) {
      ElMessage.error('提交失败')
      return
    }
    if (form.result === '合格') {
      ElMessage.success('质检合格，已生成仓储入库待办和质量报告')
    } else if (form.result === '不合格') {
      ElMessage.warning('合格品已生成入库待办；系统已生成质量报告，不合格品请在「不合格品」页处理')
    } else {
      ElMessage.success('已记录让步接收，已生成入库待办和质量报告')
    }
  } catch (e) {
    ElMessage.error(e?.message || '提交质检结果失败')
  }
}
</script>
