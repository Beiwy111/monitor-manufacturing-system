<template>
  <ModulePageShell>
    <div class="page-head">
      <span class="page-title">方案审批</span>
      <el-select v-model="caseNo" filterable placeholder="选择案例" size="small" style="width:200px" @change="loadPlan">
        <el-option v-for="c in caseOptions" :key="c.caseNo" :label="`${c.caseNo} · ${c.customerName}`" :value="c.caseNo" />
      </el-select>
    </div>

    <div v-loading="loading" class="plan-layout">
      <ModulePanelSection>
        <div class="panel-title">处置方案</div>
        <el-form :model="form" label-width="96px" size="small">
          <el-form-item label="案例号"><span>{{ caseNo || '-' }}</span></el-form-item>
          <el-form-item label="方案类型" required>
            <el-select v-model="form.planType" style="width:100%">
              <el-option v-for="p in PLAN_TYPE_OPTIONS" :key="p.value" :label="p.label" :value="p.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="追溯摘要">
            <el-input v-model="form.traceSummary" type="textarea" rows="3" placeholder="引用调查工作台追溯结论" />
            <el-button link type="primary" size="small" style="margin-top:4px" :disabled="!caseNo" :loading="importingTrace" @click="importTraceSummary">
              引用追溯结论
            </el-button>
          </el-form-item>
          <el-form-item label="方案说明">
            <el-input v-model="form.planDetail" type="textarea" rows="3" />
          </el-form-item>
          <el-form-item label="负责人" required><el-input v-model="form.ownerName" /></el-form-item>
          <el-form-item label="预计完成">
            <el-date-picker v-model="form.expectedFinishAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
          </el-form-item>
          <el-form-item label="预计费用"><el-input-number v-model="form.estimatedCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
          <el-form-item label="配件清单"><el-input v-model="form.partsJson" placeholder="如：背光模组×1，驱动板×1" /></el-form-item>
          <el-form-item label="客户意见"><el-input v-model="form.customerOpinion" type="textarea" rows="2" /></el-form-item>
          <el-form-item label="审批状态">
            <el-tag>{{ plan.approvalStatusCn || '草稿' }}</el-tag>
          </el-form-item>
        </el-form>
        <div class="action-row">
          <el-button type="primary" :disabled="!caseNo" :loading="saving" @click="save">保存方案</el-button>
          <el-button :disabled="!plan.planId" :loading="saving" @click="submit">提交审批</el-button>
          <el-button type="success" :disabled="plan.approvalStatus!=='PENDING'" :loading="saving" @click="approve">审批通过</el-button>
          <el-button type="danger" :disabled="plan.approvalStatus!=='PENDING'" :loading="saving" @click="reject">驳回</el-button>
        </div>
      </ModulePanelSection>

      <ModulePanelSection>
        <div class="panel-title">待审批方案</div>
        <el-table :data="pendingPlans" border size="small" @row-click="pickPlan">
          <el-table-column prop="caseNo" label="案例" width="130" />
          <el-table-column prop="planTypeCn" label="类型" width="100" />
          <el-table-column prop="ownerName" label="负责人" width="80" />
          <el-table-column prop="estimatedCost" label="费用" width="80" align="right" />
          <el-table-column prop="approvalStatusCn" label="状态" width="80" />
        </el-table>
      </ModulePanelSection>
    </div>
  </ModulePageShell>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { fetchCaseViews, fetchPlans, fetchPlanByCase, savePlan, submitPlan, approvePlan, rejectPlan, fetchRcaAnalysis, fetchTraceDetail } from '@/api/aftersale'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import ModulePanelSection from '@/components/module/ModulePanelSection.vue'
import { PLAN_TYPE_OPTIONS, buildRcaTraceSummary, popTraceSummary } from '@/constants/aftersaleWorkflow'

const route = useRoute()
const userStore = useUserStore()
const caseNo = ref(route.query.caseNo || '')
const caseOptions = ref([])
const plan = ref({})
const plans = ref([])
const loading = ref(false)
const saving = ref(false)
const importingTrace = ref(false)
const form = reactive({
  planType: 'REPAIR', traceSummary: '', planDetail: '', ownerName: '',
  expectedFinishAt: null, estimatedCost: null, partsJson: '', customerOpinion: ''
})

const pendingPlans = computed(() => plans.value.filter(p => p.approvalStatus !== 'APPROVED'))

async function loadCases() {
  const res = await fetchCaseViews()
  caseOptions.value = (res?.data ?? res ?? []).filter(c => c.caseStatus !== 'CLOSED')
}

async function loadPlan() {
  if (!caseNo.value) return
  loading.value = true
  try {
    const res = await fetchPlanByCase(caseNo.value)
    plan.value = res?.data ?? res ?? {}
    Object.assign(form, {
      planType: plan.value.planType || 'REPAIR',
      traceSummary: plan.value.traceSummary || '',
      planDetail: plan.value.planDetail || '',
      ownerName: plan.value.ownerName || userStore.userInfo?.realName || '',
      expectedFinishAt: plan.value.expectedFinishAt || null,
      estimatedCost: plan.value.estimatedCost ?? null,
      partsJson: plan.value.partsJson || '',
      customerOpinion: plan.value.customerOpinion || ''
    })
    const cached = popTraceSummary(caseNo.value)
    if (cached) {
      form.traceSummary = cached
      ElMessage.success('已填入调查工作台的追溯结论')
    }
  } finally { loading.value = false }
}

async function importTraceSummary() {
  if (!caseNo.value) return
  importingTrace.value = true
  try {
    const [analysisRes, traceRes] = await Promise.all([
      fetchRcaAnalysis(caseNo.value).catch(() => null),
      fetchTraceDetail(caseNo.value).catch(() => null)
    ])
    const analysis = analysisRes?.data ?? analysisRes
    const traceChain = traceRes?.traceChain || traceRes?.data?.traceChain || []
    const summary = buildRcaTraceSummary(analysis, traceChain)
    if (!summary) {
      ElMessage.warning('当前案例暂无追溯结论，请先在调查工作台完成质量追溯')
      return
    }
    form.traceSummary = summary
    ElMessage.success('已引用最新追溯结论')
  } finally { importingTrace.value = false }
}

function planPayload() {
  const payload = { caseNo: caseNo.value, ...form }
  if (!payload.expectedFinishAt) payload.expectedFinishAt = null
  return payload
}

async function loadPlans() {
  const res = await fetchPlans()
  plans.value = res?.data ?? res ?? []
}

function pickPlan(row) { caseNo.value = row.caseNo; loadPlan() }

async function save() {
  if (!caseNo.value) return
  saving.value = true
  try {
    const res = await savePlan(planPayload())
    plan.value = res?.data ?? res ?? {}
    ElMessage.success('方案已保存')
    await loadPlans()
  } catch (e) { ElMessage.error(e?.message || '保存失败') }
  finally { saving.value = false }
}

async function submit() {
  saving.value = true
  try {
    await save()
    const res = await submitPlan({ planId: plan.value.planId })
    plan.value = res?.data ?? res ?? {}
    ElMessage.success('已提交审批')
    await loadPlans()
  } catch (e) { ElMessage.error(e?.message || '提交失败') }
  finally { saving.value = false }
}

async function approve() {
  saving.value = true
  try {
    await approvePlan({ planId: plan.value.planId, operator: userStore.userInfo?.username })
    ElMessage.success('方案已通过，执行任务已生成')
    await loadPlan(); await loadPlans()
  } catch (e) { ElMessage.error(e?.message || '审批失败') }
  finally { saving.value = false }
}

async function reject() {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回方案')
  saving.value = true
  try {
    await rejectPlan({ planId: plan.value.planId, remark: value })
    ElMessage.success('已驳回')
    await loadPlan(); await loadPlans()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e?.message || '操作失败') }
  finally { saving.value = false }
}

onMounted(async () => {
  await loadCases(); await loadPlans()
  if (caseNo.value) await loadPlan()
})
</script>

<style scoped>
.page-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
.page-title { font-size:18px; font-weight:700; color:#001b3f; }
.plan-layout { display:grid; grid-template-columns:1fr 1fr; gap:14px; }
.panel-title { font-size:15px; font-weight:600; margin-bottom:10px; color:#001b3f; }
.action-row { display:flex; flex-wrap:wrap; gap:8px; margin-top:8px; }
@media (max-width: 1100px) { .plan-layout { grid-template-columns:1fr; } }
</style>
