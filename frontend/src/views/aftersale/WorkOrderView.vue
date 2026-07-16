<template>
  <ModulePageShell>
    <div class="page-head">
      <span class="page-title">售后工单</span>
      <el-button type="primary" size="small" @click="openNewDialog">+ 新增售后</el-button>
    </div>

    <ModuleListDetailLayout :has-detail="!!selected" detail-width="420px" empty-description="选择工单查看详情">
      <template #list>
        <div class="list-toolbar">
          <el-input v-model="keyword" placeholder="案例号 / 客户 / 订单 / 批次" clearable size="small" style="width:220px" />
          <el-select v-model="statusFilter" clearable placeholder="状态" size="small" style="width:120px;margin-left:8px">
            <el-option v-for="s in CASE_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-button size="small" style="margin-left:8px" :loading="loading" @click="loadData">刷新</el-button>
        </div>
        <el-table :data="filtered" border stripe highlight-current-row size="small" @current-change="onSelect">
          <el-table-column prop="caseNo" label="案例号" width="138" />
          <el-table-column prop="customerName" label="客户" width="110" show-overflow-tooltip />
          <el-table-column prop="orderNo" label="订单" width="118" show-overflow-tooltip />
          <el-table-column prop="batchNo" label="批次" width="120" show-overflow-tooltip />
          <el-table-column label="AI分诊" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.aiTriageCategory" size="small" type="info">{{ row.aiTriageCategory }}</el-tag>
              <span v-else class="muted">未分诊</span>
            </template>
          </el-table-column>
          <el-table-column prop="assigneeName" label="受理人" width="80" />
          <el-table-column label="SLA" width="148">
            <template #default="{ row }">
              <el-tag v-if="row.slaDeadline" :type="slaTagType(row.slaDeadline)" size="small">{{ row.slaDeadline }}</el-tag>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.caseStatus)" size="small">{{ row.caseStatusCn }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template #detail>
        <ModulePanelSection v-if="selected">
          <div class="panel-title">
            工单详情
            <el-tag :type="statusTagType(selected.caseStatus)" size="small" style="margin-left:8px">{{ selected.caseStatusCn }}</el-tag>
          </div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="案例号">{{ selected.caseNo }}</el-descriptions-item>
            <el-descriptions-item label="客户">{{ selected.customerName }}</el-descriptions-item>
            <el-descriptions-item label="订单">{{ selected.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="产品批次">{{ selected.batchNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="产品">{{ selected.materialName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="受理人">{{ selected.assigneeName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="AI分诊">{{ selected.aiTriageCategory || '未分诊' }}</el-descriptions-item>
            <el-descriptions-item label="SLA截止">{{ selected.slaDeadline || '-' }}</el-descriptions-item>
            <el-descriptions-item label="问题类型">{{ selected.problemTypeCn }}</el-descriptions-item>
            <el-descriptions-item label="级别">{{ selected.caseLevelCn }}</el-descriptions-item>
            <el-descriptions-item label="登记时间">{{ selected.openedAt }}</el-descriptions-item>
            <el-descriptions-item label="受理时间">{{ selected.processingAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="问题描述" :span="2">{{ selected.problemDescription }}</el-descriptions-item>
          </el-descriptions>
          <div class="flow-hint">{{ nextStepHint(selected.caseStatus) }}</div>
          <div class="action-row">
            <el-button v-if="selected.caseStatus==='OPEN'" type="primary" size="small" :loading="acting" @click="doAccept">受理</el-button>
            <el-button size="small" @click="goWorkbench">调查工作台</el-button>
            <el-button v-if="canPlan" size="small" @click="goPlan">制定方案</el-button>
            <el-button v-if="canExecute" size="small" @click="goExecution">执行协同</el-button>
            <el-button v-if="canCloseLoop" size="small" @click="goClosure">验证闭环</el-button>
          </div>
        </ModulePanelSection>
      </template>
    </ModuleListDetailLayout>

    <el-dialog v-model="newDialog" title="新增售后案例" width="500px" :close-on-click-modal="false">
      <el-form :model="newForm" label-width="90px" size="small">
        <el-form-item label="客户名称" required><el-input v-model="newForm.customerName" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="newForm.contactName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="newForm.contactPhone" /></el-form-item>
        <el-form-item label="问题类型">
          <el-select v-model="newForm.problemType" style="width:100%">
            <el-option v-for="t in PROBLEM_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重级别">
          <el-radio-group v-model="newForm.caseLevel">
            <el-radio value="LOW">低</el-radio><el-radio value="MEDIUM">中</el-radio>
            <el-radio value="HIGH">高</el-radio><el-radio value="CRITICAL">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="批次号"><el-input v-model="newForm.batchNo" /></el-form-item>
        <el-form-item label="问题描述" required><el-input v-model="newForm.problemDescription" type="textarea" rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newDialog=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSaveNew">登记</el-button>
      </template>
    </el-dialog>
  </ModulePageShell>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { fetchCaseViews, acceptCase, insertCase } from '@/api/aftersale'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import ModuleListDetailLayout from '@/components/module/ModuleListDetailLayout.vue'
import ModulePanelSection from '@/components/module/ModulePanelSection.vue'
import { moduleStatusType } from '@/constants/moduleStatus'
import { CASE_STATUS_OPTIONS, PROBLEM_TYPES, nextStepHint, slaTagType } from '@/constants/aftersaleWorkflow'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const list = ref([])
const selected = ref(null)
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)
const acting = ref(false)
const saving = ref(false)
const newDialog = ref(false)
const newForm = reactive({
  customerName: '', contactName: '', contactPhone: '',
  problemType: 'DISPLAY_DEFECT', caseLevel: 'MEDIUM', batchNo: '', problemDescription: ''
})

const filtered = computed(() => {
  let data = list.value
  if (statusFilter.value) data = data.filter(r => r.caseStatus === statusFilter.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    data = data.filter(r =>
      (r.caseNo || '').toLowerCase().includes(kw) ||
      (r.customerName || '').toLowerCase().includes(kw) ||
      (r.orderNo || '').toLowerCase().includes(kw) ||
      (r.batchNo || '').toLowerCase().includes(kw)
    )
  }
  return data
})

const canPlan = computed(() => selected.value && ['ACCEPTED', 'PENDING_PLAN', 'PENDING_APPROVAL', 'PROCESSING'].includes(selected.value.caseStatus))
const canExecute = computed(() => selected.value && ['EXECUTING', 'PENDING_RECHECK'].includes(selected.value.caseStatus))
const canCloseLoop = computed(() => selected.value && ['PENDING_RECHECK', 'PENDING_CONFIRM', 'RESOLVED'].includes(selected.value.caseStatus))

function statusTagType(s) { return moduleStatusType('aftersaleCase', s) }
function onSelect(row) { selected.value = row || null }

async function loadData() {
  loading.value = true
  try {
    const res = await fetchCaseViews()
    list.value = res?.data ?? res ?? []
    const q = route.query.caseNo || route.query.caseId
    if (q) {
      const hit = list.value.find(r => r.caseNo === q)
      if (hit) selected.value = hit
    }
  } finally { loading.value = false }
}

function openNewDialog() { newDialog.value = true }

async function doSaveNew() {
  if (!newForm.customerName.trim() || !newForm.problemDescription.trim()) {
    ElMessage.warning('请填写客户名称和问题描述'); return
  }
  saving.value = true
  try {
    const caseNo = 'AS' + Date.now()
    await insertCase({
      caseNo, customerName: newForm.customerName, contactName: newForm.contactName,
      contactPhone: newForm.contactPhone, problemType: newForm.problemType,
      caseLevel: newForm.caseLevel, batchNo: newForm.batchNo || null,
      problemDescription: newForm.problemDescription, caseStatus: 'OPEN',
      openedAt: new Date().toISOString().slice(0, 19).replace('T', ' ')
    })
    ElMessage.success('售后案例已登记')
    newDialog.value = false
    await loadData()
  } catch (e) { ElMessage.error(e?.message || '登记失败') }
  finally { saving.value = false }
}

async function doAccept() {
  const caseNo = selected.value?.caseNo
  if (!caseNo) return
  try {
    await ElMessageBox.confirm('确认受理该工单？', '受理确认')
  } catch {
    return
  }
  acting.value = true
  try {
    await acceptCase({ caseNo, operator: userStore.userInfo?.username })
    ElMessage.success('已受理')
    await loadData()
    selected.value = list.value.find(r => r.caseNo === caseNo) || null
  } catch (e) {
    ElMessage.error(e?.message || '受理失败')
  } finally {
    acting.value = false
  }
}

function goWorkbench() { router.push({ path: '/dashboard/aftersale', query: { caseNo: selected.value?.caseNo } }) }
function goPlan() { router.push({ path: '/aftersale/plan', query: { caseNo: selected.value?.caseNo } }) }
function goExecution() { router.push({ path: '/aftersale/execution', query: { caseNo: selected.value?.caseNo } }) }
function goClosure() { router.push({ path: '/aftersale/closure', query: { caseNo: selected.value?.caseNo } }) }

onMounted(loadData)
</script>

<style scoped>
.page-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
.page-title { font-size:18px; font-weight:700; color:#001b3f; }
.list-toolbar { display:flex; align-items:center; margin-bottom:10px; }
.panel-title { font-size:15px; font-weight:600; margin-bottom:10px; color:#001b3f; }
.flow-hint { margin:12px 0; padding:8px 10px; background:#f4f7fb; border-left:3px solid #3b7fc4; font-size:13px; color:#50657a; }
.action-row { display:flex; flex-wrap:wrap; gap:8px; }
.muted { color:#bbb; font-size:12px; }
</style>
