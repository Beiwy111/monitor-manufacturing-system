<template>
  <div class="ruoyi-page defect-page">
    <div class="ruoyi-stats">
      <span
        v-for="k in kpiCards"
        :key="k.key"
        class="ruoyi-stats__item ruoyi-stats__item--filter"
        :class="{
          'is-active': filterStatus === k.filterVal,
          'ruoyi-stats__item--warn': k.key === 'processing',
          'ruoyi-stats__item--danger': k.key === 'pending'
        }"
        @click="filterStatus = filterStatus === k.filterVal ? '' : k.filterVal"
      >
        {{ k.label }}：<em>{{ k.val }}</em>
      </span>
    </div>

    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">不合格品处置</span>
      <el-input v-model="keyword" placeholder="不良品单号/缺陷类型/工单" clearable size="small" style="width:230px" />
      <el-select v-model="filterStatus" clearable placeholder="状态" size="small" style="width:100px">
        <el-option label="待处置" value="PENDING" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="已处置" value="DONE" />
      </el-select>
      <el-button size="small" :loading="loading" @click="load">刷新</el-button>
    </div>

    <div class="qc-split-layout">
      <div class="qc-split-layout__main ruoyi-table-wrap">
        <el-table :data="filtered" border stripe highlight-current-row size="small"
          style="width:100%" @current-change="onSelect">
          <el-table-column prop="nonconformingNo" label="不良品单号" width="145" />
          <el-table-column prop="inspectionNo"    label="关联质检"   width="130" />
          <el-table-column prop="workOrderNo"     label="工单"       width="130" show-overflow-tooltip />
          <el-table-column prop="defectType"      label="缺陷类型"   width="110" show-overflow-tooltip />
          <el-table-column label="严重程度" width="85" align="center">
            <template #default="{ row }">
              <el-tag :type="severityType(row.severity)" size="small">{{ row.severityCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="quantity"        label="数量"       width="70" align="center" />
          <el-table-column label="状态" width="85">
            <template #default="{ row }">
              <el-tag :type="handleStatusType(row.handleStatus)" size="small">{{ row.handleStatusCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="registeredAt"    label="登记时间"   width="148" />
        </el-table>
      </div>

      <div class="qc-split-layout__side">
        <template v-if="selected">
        <div class="qc-section">
          <div class="qc-section__title">
            不良品详情
            <el-tag :type="severityType(selected.severity)" size="small" style="margin-left:8px">
              {{ selected.severityCn }}
            </el-tag>
            <el-tag :type="handleStatusType(selected.handleStatus)" size="small" style="margin-left:4px">
              {{ selected.handleStatusCn }}
            </el-tag>
          </div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="不良品单号">{{ selected.nonconformingNo }}</el-descriptions-item>
            <el-descriptions-item label="关联质检单">
              <el-tag v-if="selected.inspectionNo" type="primary" size="small">{{ selected.inspectionNo }}</el-tag>
              <span v-else style="color:#bbb">无</span>
            </el-descriptions-item>
            <el-descriptions-item label="工单号">{{ selected.workOrderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="物料">{{ selected.materialName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="批次">{{ selected.batchNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="缺陷数量">{{ selected.quantity }}</el-descriptions-item>
            <el-descriptions-item label="缺陷类型" :span="2">{{ selected.defectType }}</el-descriptions-item>
            <el-descriptions-item label="缺陷描述" :span="2">{{ selected.defectDescription || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="selected.remark" label="备注" :span="2">{{ selected.remark }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-if="normHandleStatus(selected.handleStatus) !== 'DONE'" class="qc-section">
          <div class="qc-section__title">处置操作</div>
          <el-form label-width="80px" size="small">
            <el-form-item label="处置方式" required>
              <el-select v-model="handleForm.method" style="width:100%">
                <el-option label="报废处理" value="SCRAP" />
                <el-option label="返工修复" value="REWORK" />
                <el-option label="让步接收" value="CONCESSION" />
                <el-option label="退货供应商" value="RETURN" />
              </el-select>
            </el-form-item>
            <el-form-item label="处置说明">
              <el-input v-model="handleForm.remark" type="textarea" rows="2"
                placeholder="可选，说明处置原因" />
            </el-form-item>
          </el-form>
          <div class="action-row">
            <el-button type="danger" :loading="acting" @click="doHandle">确认处置</el-button>
          </div>
        </div>
        <div v-else class="qc-section">
          <el-tag type="success">已处置完毕</el-tag>
          <div v-if="selected.handleMethod" style="margin-top:8px;font-size:13px;color:#606266">
            处置方式：{{ methodCn(selected.handleMethod) }}
          </div>
        </div>
        </template>
        <div v-else class="ruoyi-detail__empty" style="padding: 48px 16px; text-align: center;">
          点击左侧不良品记录查看详情和处置操作
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { fetchNonconformingViews, handleNonconforming } from '@/api/quality'
import { normHandleStatus } from '@/composables/useQualityDashboard'

const route = useRoute()
const userStore   = useUserStore()
const list        = ref([])
const loading     = ref(false)
const acting      = ref(false)
const selected    = ref(null)
const keyword     = ref('')
const filterStatus = ref('')

// 路由 meta.category 决定默认品类过滤
const routeCategory = computed(() => route.meta?.category || '')
const categoryFilter = ref(routeCategory.value)

const handleForm  = reactive({ method: 'SCRAP', remark: '' })

const pending    = computed(() => list.value.filter(r => normHandleStatus(r.handleStatus) === 'PENDING').length)
const processing = computed(() => list.value.filter(r => normHandleStatus(r.handleStatus) === 'PROCESSING').length)
const done       = computed(() => list.value.filter(r => normHandleStatus(r.handleStatus) === 'DONE').length)

const kpiCards = computed(() => [
  { key: 'all',        label: '全部',   val: list.value.length, cls: '',          filterVal: '' },
  { key: 'pending',    label: '待处置', val: pending.value,     cls: 'pending',   filterVal: 'PENDING' },
  { key: 'processing', label: '处理中', val: processing.value,  cls: 'proc',      filterVal: 'PROCESSING' },
  { key: 'done',       label: '已处置', val: done.value,        cls: 'done',      filterVal: 'DONE' },
])

const filtered = computed(() => {
  let data = list.value
  if (categoryFilter.value) data = data.filter(r => r.inspectionCategory === categoryFilter.value)
  if (filterStatus.value) data = data.filter(r => normHandleStatus(r.handleStatus) === filterStatus.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    data = data.filter(r =>
      (r.nonconformingNo || '').toLowerCase().includes(kw) ||
      (r.defectType      || '').toLowerCase().includes(kw) ||
      (r.workOrderNo     || '').toLowerCase().includes(kw)
    )
  }
  return data
})

function severityType(s) {
  return { MINOR: '', GENERAL: 'warning', MAJOR: 'danger', CRITICAL: 'danger' }[s] || 'info'
}
function handleStatusType(s) {
  return { PENDING: 'danger', PROCESSING: 'warning', DONE: 'success', COMPLETED: 'success' }[normHandleStatus(s)] || 'info'
}
function methodCn(s) {
  return { SCRAP: '报废处理', REWORK: '返工修复', CONCESSION: '让步接收', RETURN: '退货供应商' }[s] || s
}

function onSelect(row) {
  selected.value = row
  handleForm.method = 'SCRAP'
  handleForm.remark = ''
}

async function load() {
  loading.value = true
  try {
    const res = await fetchNonconformingViews()
    list.value = res.data ?? res
  } catch { /* 静默 */ }
  finally { loading.value = false }
}

async function doHandle() {
  if (!selected.value) return
  await ElMessageBox.confirm(
    `确认对 ${selected.value.nonconformingNo} 执行「${methodCn(handleForm.method)}」处置？`,
    '处置确认', { type: 'warning' }
  )
  acting.value = true
  try {
    await handleNonconforming({
      nonconformingId: selected.value.nonconformingId,
      handleMethod:    handleForm.method,
      remark:          handleForm.remark,
      operator:        userStore.userInfo?.username
    })
    ElMessage.success('处置完成')
    await load()
    selected.value = null
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}

onMounted(load)

watch(routeCategory, (val) => {
  categoryFilter.value = val
  selected.value = null
  load()
})
</script>

<style scoped>
.action-row { display: flex; gap: 8px; }
</style>
