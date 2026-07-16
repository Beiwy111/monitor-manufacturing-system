<template>
  <ModulePageShell>
    <div class="page-head">
      <span class="page-title">验证闭环</span>
      <el-select v-model="caseNo" filterable placeholder="选择案例" size="small" style="width:200px" @change="loadClosure">
        <el-option v-for="c in caseOptions" :key="c.caseNo" :label="`${c.caseNo} · ${c.caseStatusCn}`" :value="c.caseNo" />
      </el-select>
    </div>

    <div v-loading="loading" class="closure-layout">
      <ModulePanelSection>
        <div class="panel-title">复检与客户确认</div>
        <el-form :model="form" label-width="110px" size="small">
          <el-form-item label="复检结果"><el-input v-model="form.recheckResult" type="textarea" rows="2" /></el-form-item>
          <el-form-item label="复检通过">
            <el-switch v-model="form.recheckPassed" />
          </el-form-item>
          <el-form-item label="客户已确认">
            <el-switch v-model="form.customerConfirmed" />
          </el-form-item>
          <el-form-item label="满意度">
            <el-rate v-model="form.satisfactionScore" :max="5" />
          </el-form-item>
          <el-form-item label="实际成本"><el-input-number v-model="form.actualCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
        </el-form>
      </ModulePanelSection>

      <ModulePanelSection>
        <div class="panel-title">根因与改进</div>
        <el-form :model="form" label-width="110px" size="small">
          <el-form-item label="根本原因" required><el-input v-model="form.rootCause" type="textarea" rows="2" /></el-form-item>
          <el-form-item label="责任归属" required><el-input v-model="form.responsibility" /></el-form-item>
          <el-form-item label="改进措施" required><el-input v-model="form.improvementMeasures" type="textarea" rows="3" /></el-form-item>
          <el-form-item label="关闭备注"><el-input v-model="form.closedRemark" type="textarea" rows="2" /></el-form-item>
        </el-form>
        <div class="action-row">
          <el-button type="primary" :disabled="!caseNo" :loading="saving" @click="save">保存</el-button>
          <el-button :disabled="!caseNo" :loading="saving" @click="confirmCustomer">客户确认</el-button>
          <el-button type="success" :disabled="!caseNo" :loading="saving" @click="closeCase">关闭案例</el-button>
        </div>
      </ModulePanelSection>
    </div>
  </ModulePageShell>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { fetchCaseViews, fetchClosure, saveClosure, confirmCustomer as confirmCustomerApi, closeWithClosure } from '@/api/aftersale'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import ModulePanelSection from '@/components/module/ModulePanelSection.vue'

const route = useRoute()
const userStore = useUserStore()
const caseNo = ref(route.query.caseNo || '')
const caseOptions = ref([])
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  recheckResult: '', recheckPassed: false, customerConfirmed: false,
  satisfactionScore: 0, actualCost: null, rootCause: '', responsibility: '',
  improvementMeasures: '', closedRemark: ''
})

async function loadCases() {
  const res = await fetchCaseViews()
  caseOptions.value = (res?.data ?? res ?? []).filter(c => !['OPEN', 'CLOSED'].includes(c.caseStatus))
}

async function loadClosure() {
  if (!caseNo.value) return
  loading.value = true
  try {
    const res = await fetchClosure(caseNo.value)
    const d = res?.data ?? res ?? {}
    Object.assign(form, {
      recheckResult: d.recheckResult || '',
      recheckPassed: !!d.recheckPassed,
      customerConfirmed: !!d.customerConfirmed,
      satisfactionScore: d.satisfactionScore || 0,
      actualCost: d.actualCost ?? null,
      rootCause: d.rootCause || '',
      responsibility: d.responsibility || '',
      improvementMeasures: d.improvementMeasures || '',
      closedRemark: d.closedRemark || ''
    })
  } catch (e) {
    ElMessage.error(e?.message || '加载闭环信息失败')
  } finally { loading.value = false }
}

async function save() {
  saving.value = true
  try {
    await saveClosure({ caseNo: caseNo.value, ...form })
    ElMessage.success('已保存')
  } catch (e) { ElMessage.error(e?.message || '保存失败') }
  finally { saving.value = false }
}

async function confirmCustomer() {
  await save()
  saving.value = true
  try {
    await confirmCustomerApi({ caseNo: caseNo.value })
    ElMessage.success('客户已确认，案例进入已解决')
    await loadCases()
  } catch (e) { ElMessage.error(e?.message || '操作失败') }
  finally { saving.value = false }
}

async function closeCase() {
  await ElMessageBox.confirm('确认所有闭环项已完成并关闭案例？', '关闭确认', { type: 'warning' })
  await save()
  saving.value = true
  try {
    await closeWithClosure({ caseNo: caseNo.value, operator: userStore.userInfo?.username })
    ElMessage.success('案例已关闭')
    await loadCases()
  } catch (e) { ElMessage.error(e?.message || '关闭失败') }
  finally { saving.value = false }
}

onMounted(async () => {
  await loadCases()
  if (caseNo.value) await loadClosure()
})
</script>

<style scoped>
.page-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
.page-title { font-size:18px; font-weight:700; color:#001b3f; }
.closure-layout { display:grid; grid-template-columns:1fr 1fr; gap:14px; }
.panel-title { font-size:15px; font-weight:600; margin-bottom:10px; color:#001b3f; }
.action-row { display:flex; flex-wrap:wrap; gap:8px; margin-top:8px; }
@media (max-width: 1100px) { .closure-layout { grid-template-columns:1fr; } }
</style>
