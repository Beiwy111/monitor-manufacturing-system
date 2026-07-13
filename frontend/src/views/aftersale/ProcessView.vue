<template>
  <div class="aftersale-sub-page">
    <div class="page-header">
      <span class="page-title">售后处理</span>
      <el-button size="small" :loading="loading" @click="load">刷新</el-button>
    </div>
    <el-table :data="list" border stripe size="small" style="width:100%" v-loading="loading">
      <el-table-column prop="caseNo" label="案例号" width="150" />
      <el-table-column prop="customerName" label="客户" width="130" show-overflow-tooltip />
      <el-table-column prop="problemTypeCn" label="问题类型" width="110" />
      <el-table-column prop="materialName" label="产品/物料" min-width="120" show-overflow-tooltip />
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
      <el-table-column label="关联质检" width="75" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.inspectionNo" type="primary" size="small">{{ row.inspectionNo }}</el-tag>
          <span v-else style="color:#bbb;font-size:12px">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="openedAt" label="登记时间" width="148" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.caseStatus==='OPEN'" type="primary" size="small"
            :loading="acting===row.caseNo" @click="accept(row)">受理</el-button>
          <el-button v-else-if="row.caseStatus==='PROCESSING'" type="success" size="small"
            @click="goDetail(row)">处理</el-button>
          <el-button v-else-if="row.caseStatus==='RESOLVED'" size="small"
            :loading="acting===row.caseNo" @click="close(row)">关闭</el-button>
          <el-tag v-else type="info" size="small">已关闭</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { fetchCaseViews, acceptCase, closeCase } from '@/api/aftersale'

const router = useRouter()
const userStore = useUserStore()
const list    = ref([])
const loading = ref(false)
const acting  = ref('')

async function load() {
  loading.value = true
  try {
    const res = await fetchCaseViews()
    const all = res.data ?? res
    // 只展示未关闭的案例
    list.value = all.filter(r => r.caseStatus !== 'CLOSED')
  } catch { /* 静默 */ }
  finally { loading.value = false }
}

function statusType(s) {
  return { OPEN: 'danger', TRACING: 'warning', PROCESSING: 'warning', RESOLVED: 'primary', CLOSED: 'info', CANCELLED: 'info' }[s] || 'info'
}
function levelType(s) {
  return { GENERAL: '', IMPORTANT: 'warning', URGENT: 'danger', LOW: '', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }[s] || ''
}

function goDetail(row) {
  router.push({ path: '/aftersale/case', query: { caseNo: row.caseNo } })
}

async function accept(row) {
  await ElMessageBox.confirm(`受理案例 ${row.caseNo}？`, '确认', { type: 'info' })
  acting.value = row.caseNo
  try {
    await acceptCase({ caseNo: row.caseNo, operator: userStore.userInfo?.username })
    ElMessage.success('已受理')
    await load()
  } catch (e) { ElMessage.error(e?.response?.data?.message || '操作失败') }
  finally { acting.value = '' }
}

async function close(row) {
  await ElMessageBox.confirm(`关闭案例 ${row.caseNo}？`, '确认', { type: 'warning' })
  acting.value = row.caseNo
  try {
    await closeCase({ caseNo: row.caseNo, remark: '处理完毕关闭', operator: userStore.userInfo?.username })
    ElMessage.success('案例已关闭')
    await load()
  } catch (e) { ElMessage.error(e?.response?.data?.message || '操作失败') }
  finally { acting.value = '' }
}

onMounted(load)
</script>

<style scoped>
.aftersale-sub-page { padding: 16px; background: #f5f7fa; min-height: 100%; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.page-title { font-size: 15px; font-weight: 600; color: #2c3e50; }
</style>
