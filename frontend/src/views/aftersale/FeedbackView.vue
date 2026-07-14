<template>
  <div class="aftersale-sub-page">
    <div class="page-header">
      <span class="page-title">客户反馈</span>
      <div style="display:flex;gap:8px">
        <el-input v-model="keyword" placeholder="案例号/客户/问题" clearable size="small" style="width:200px" />
        <el-button size="small" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <!-- 统计条 -->
    <div class="stat-bar">
      <div v-for="s in stats" :key="s.key" class="stat-item" :class="s.cls">
        <span class="stat-num">{{ s.val }}</span>
        <span class="stat-label">{{ s.label }}</span>
      </div>
    </div>

    <el-table :data="filtered" border stripe size="small" style="width:100%" v-loading="loading">
      <el-table-column prop="caseNo" label="案例号" width="150" />
      <el-table-column prop="customerName" label="客户" width="140" show-overflow-tooltip />
      <el-table-column prop="contactName" label="联系人" width="90" />
      <el-table-column prop="contactPhone" label="联系电话" width="120" />
      <el-table-column prop="problemTypeCn" label="问题类型" width="110" />
      <el-table-column prop="problemDescription" label="反馈内容" min-width="180" show-overflow-tooltip />
      <el-table-column label="级别" width="60" align="center">
        <template #default="{ row }">
          <el-tag :type="levelType(row.caseLevel)" size="small">{{ row.caseLevelCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="85">
        <template #default="{ row }">
          <el-tag :type="statusType(row.caseStatus)" size="small">{{ row.caseStatusCn }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="openedAt" label="反馈时间" width="148" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchCaseViews } from '@/api/aftersale'

const list    = ref([])
const loading = ref(false)
const keyword = ref('')

const stats = computed(() => {
  const d = list.value
  return [
    { key: 'total',      label: '全部反馈', val: d.length,                                         cls: '' },
    { key: 'open',       label: '待受理',   val: d.filter(r => r.caseStatus === 'OPEN').length,      cls: 'open' },
    { key: 'processing', label: '处理中',   val: d.filter(r => r.caseStatus === 'PROCESSING').length, cls: 'proc' },
    { key: 'resolved',   label: '已解决',   val: d.filter(r => r.caseStatus === 'RESOLVED').length,  cls: 'done' },
    { key: 'closed',     label: '已关闭',   val: d.filter(r => r.caseStatus === 'CLOSED').length,    cls: 'closed' },
  ]
})

const filtered = computed(() => {
  if (!keyword.value) return list.value
  const kw = keyword.value.toLowerCase()
  return list.value.filter(r =>
    (r.caseNo || '').toLowerCase().includes(kw) ||
    (r.customerName || '').toLowerCase().includes(kw) ||
    (r.problemDescription || '').toLowerCase().includes(kw)
  )
})

function statusType(s) {
  return { OPEN: 'danger', TRACING: 'warning', PROCESSING: 'warning', RESOLVED: 'primary', CLOSED: 'info', CANCELLED: 'info' }[s] || 'info'
}
function levelType(s) {
  return { GENERAL: '', IMPORTANT: 'warning', URGENT: 'danger', LOW: '', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }[s] || ''
}

async function load() {
  loading.value = true
  try {
    const res = await fetchCaseViews()
    list.value = res.data ?? res
  } catch { /* 静默 */ }
  finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped>
.aftersale-sub-page { padding: 16px; background: #f5f7fa; min-height: 100%; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.page-title  { font-size: 15px; font-weight: 600; color: #2c3e50; }

.stat-bar  { display: flex; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.stat-item {
  background: #fff; border: 1px solid #e4e7ed; border-radius: 6px;
  padding: 8px 18px; display: flex; flex-direction: column; align-items: center; min-width: 72px;
}
.stat-item.open   { border-top: 3px solid #f56c6c; }
.stat-item.proc   { border-top: 3px solid #e6a23c; }
.stat-item.done   { border-top: 3px solid #409eff; }
.stat-item.closed { border-top: 3px solid #909399; }
.stat-num   { font-size: 20px; font-weight: 700; color: #2c3e50; }
.stat-label { font-size: 11px; color: #8492a6; margin-top: 2px; }
</style>
