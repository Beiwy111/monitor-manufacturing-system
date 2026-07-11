<template>
  <div class="defect-page">
    <!-- KPI 统计条 -->
    <div class="kpi-bar">
      <div class="kpi-card" v-for="k in kpiCards" :key="k.key"
        :class="k.cls"
        :style="filterStatus===k.filterVal?'outline:2px solid #409eff':''"
        @click="filterStatus = filterStatus===k.filterVal ? '' : k.filterVal">
        <span class="kpi-num">{{ k.val }}</span>
        <span class="kpi-lbl">{{ k.label }}</span>
      </div>
      <el-button size="small" :loading="loading" style="margin-left:auto" @click="load">刷新</el-button>
    </div>

    <div class="page-body">
      <!-- 列表 -->
      <div class="list-panel">
        <div class="list-toolbar">
          <el-input v-model="keyword" placeholder="不良品单号/缺陷类型/工单" clearable size="small" style="width:230px" />
          <el-select v-model="filterStatus" clearable placeholder="状态" size="small" style="width:100px;margin-left:6px">
            <el-option label="待处置" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已处置" value="DONE" />
          </el-select>
        </div>
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

      <!-- 右侧详情 + 处置操作 -->
      <div class="detail-panel" v-if="selected">
        <div class="panel-section">
          <div class="panel-title">
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

        <!-- 处置操作区 -->
        <div class="panel-section" v-if="selected.handleStatus !== 'DONE'">
          <div class="panel-title">处置操作</div>
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
        <div class="panel-section" v-else>
          <el-tag type="success">已处置完毕</el-tag>
          <div v-if="selected.handleMethod" style="margin-top:8px;font-size:13px;color:#606266">
            处置方式：{{ methodCn(selected.handleMethod) }}
          </div>
        </div>
      </div>

      <div class="detail-panel empty" v-else>
        <el-empty description="点击左侧不良品记录查看详情和处置操作" />
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

const pending    = computed(() => list.value.filter(r => r.handleStatus === 'PENDING').length)
const processing = computed(() => list.value.filter(r => r.handleStatus === 'PROCESSING').length)
const done       = computed(() => list.value.filter(r => r.handleStatus === 'DONE').length)

const kpiCards = computed(() => [
  { key: 'all',        label: '全部',   val: list.value.length, cls: '',          filterVal: '' },
  { key: 'pending',    label: '待处置', val: pending.value,     cls: 'pending',   filterVal: 'PENDING' },
  { key: 'processing', label: '处理中', val: processing.value,  cls: 'proc',      filterVal: 'PROCESSING' },
  { key: 'done',       label: '已处置', val: done.value,        cls: 'done',      filterVal: 'DONE' },
])

const filtered = computed(() => {
  let data = list.value
  if (categoryFilter.value) data = data.filter(r => r.inspectionCategory === categoryFilter.value)
  if (filterStatus.value) data = data.filter(r => r.handleStatus === filterStatus.value)
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
  return { PENDING: 'danger', PROCESSING: 'warning', DONE: 'success' }[s] || 'info'
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
.defect-page { display: flex; flex-direction: column; height: 100%; padding: 12px; gap: 10px; background: #f5f7fa; }

.kpi-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.kpi-card {
  background: #fff; border: 1px solid #e4e7ed; border-radius: 6px;
  padding: 10px 18px; cursor: pointer; user-select: none;
  display: flex; flex-direction: column; align-items: center; min-width: 76px;
  transition: box-shadow .15s;
}
.kpi-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.1); }
.kpi-card.pending { border-top: 3px solid #f56c6c; }
.kpi-card.proc    { border-top: 3px solid #e6a23c; }
.kpi-card.done    { border-top: 3px solid #67c23a; }
.kpi-num { font-size: 22px; font-weight: 700; color: #2c3e50; line-height: 1.2; }
.kpi-lbl { font-size: 11px; color: #8492a6; margin-top: 2px; }

.page-body  { display: flex; gap: 12px; flex: 1; min-height: 0; }
.list-panel { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px; }
.list-toolbar { display: flex; align-items: center; }

.detail-panel {
  width: 380px; flex-shrink: 0; background: #fff;
  border: 1px solid #e4e7ed; border-radius: 6px;
  overflow-y: auto; display: flex; flex-direction: column;
}
.detail-panel.empty { align-items: center; justify-content: center; }

.panel-section { padding: 14px 16px; border-bottom: 1px solid #f0f2f5; }
.panel-section:last-child { border-bottom: none; }
.panel-title {
  font-size: 13px; font-weight: 600; color: #2c3e50;
  margin-bottom: 10px; display: flex; align-items: center;
}
.action-row { display: flex; gap: 8px; }
</style>
