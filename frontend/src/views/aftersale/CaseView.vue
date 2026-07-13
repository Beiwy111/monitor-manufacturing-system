<template>
  <ModulePageShell>
    <!-- KPI 栏 -->
    <KpiStrip v-model="tabActive" :cards="kpiCards" :metrics="kpi">
      <template #actions>
        <el-button type="primary" size="small" @click="openNewDialog">+ 登记售后</el-button>
      </template>
    </KpiStrip>

    <ModuleListDetailLayout
      :has-detail="!!selected"
      detail-width="400px"
      empty-description="点击左侧案例查看详情和反向追溯"
    >
      <!-- 左侧列表 -->
      <template #list>
        <div class="list-toolbar">
          <el-input v-model="keyword" placeholder="案例号/订单/客户/批次" clearable size="small" style="width:210px" />
          <el-select v-model="statusFilter" clearable placeholder="状态" size="small" style="width:100px;margin-left:6px">
            <el-option label="待受理" value="OPEN" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
          <el-button size="small" style="margin-left:6px" :loading="loading" @click="loadData">刷新</el-button>
        </div>

        <el-table :data="filtered" border stripe highlight-current-row size="small"
          style="width:100%" @current-change="onSelect">
          <el-table-column prop="caseNo" label="案例号" width="140" />
          <el-table-column prop="orderNo" label="关联订单" width="120" show-overflow-tooltip />
          <el-table-column prop="customerName" label="客户" width="100" show-overflow-tooltip />
          <el-table-column prop="problemTypeCn" label="问题类型" width="100" />
          <el-table-column prop="materialName" label="产品/物料" min-width="110" show-overflow-tooltip />
          <el-table-column prop="caseLevelCn" label="级别" width="60" align="center">
            <template #default="{ row }">
              <el-tag :type="levelTagType(row.caseLevel)" size="small">{{ row.caseLevelCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="85">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.caseStatus)" size="small">{{ row.caseStatusCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="质检关联" width="75" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.inspectionNo" type="info" size="small">已关联</el-tag>
              <span v-else style="color:#bbb">无</span>
            </template>
          </el-table-column>
          <el-table-column prop="openedAt" label="登记时间" width="148" />
        </el-table>
      </template>

      <!-- 右侧：详情 + 追溯链路 -->
      <template #detail>
        <!-- 基础信息 -->
        <ModulePanelSection>
          <div class="panel-title">
            案例详情
            <el-tag :type="statusTagType(selected.caseStatus)" size="small" style="margin-left:8px">
              {{ selected.caseStatusCn }}
            </el-tag>
            <el-tag :type="levelTagType(selected.caseLevel)" size="small" style="margin-left:4px">
              {{ selected.caseLevelCn }}
            </el-tag>
          </div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="案例编号">{{ selected.caseNo }}</el-descriptions-item>
            <el-descriptions-item label="关联订单">{{ selected.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户">{{ selected.customerName }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ selected.contactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ selected.contactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="产品/物料">{{ selected.materialName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="批次">{{ selected.batchNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="问题类型">{{ selected.problemTypeCn }}</el-descriptions-item>
            <el-descriptions-item label="关联质检单">
              <el-tag v-if="selected.inspectionNo" type="primary" size="small">{{ selected.inspectionNo }}</el-tag>
              <span v-else style="color:#bbb">未关联</span>
            </el-descriptions-item>
            <el-descriptions-item label="登记时间">{{ selected.openedAt }}</el-descriptions-item>
            <el-descriptions-item label="问题描述" :span="2">{{ selected.problemDescription || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="selected.handleResult" label="解决方案" :span="2">
              {{ selected.handleResult }}
            </el-descriptions-item>
            <el-descriptions-item v-if="selected.traceResult" label="追溯结论" :span="2">
              {{ selected.traceResult }}
            </el-descriptions-item>
          </el-descriptions>
        </ModulePanelSection>

        <!-- 追溯链路 -->
        <ModulePanelSection>
          <div class="panel-title">
            反向追溯链路
            <el-button size="small" style="margin-left:8px" :loading="traceLoading" @click="loadTrace">
              查看追溯
            </el-button>
          </div>
          <div v-if="!traceDetail" class="empty-hint">点击「查看追溯」加载固定八环反向链路（客户反馈→发货→入库→成品质检→生产→领料→物料质检→供应商）</div>
          <div v-else class="trace-chain">
            <div v-for="(step, idx) in traceDetail.traceChain" :key="idx" class="trace-step" :class="{ missing: step.missing }">
              <div class="step-icon" :class="'step-' + step.type">
                {{ stepIcon(step.type) }}
              </div>
              <div class="step-body">
                <div class="step-title">{{ step.title }}</div>
                <div class="step-no">{{ step.no }}</div>
                <div class="step-desc" v-if="step.desc">{{ step.desc }}</div>
                <div class="step-people" v-if="step.people?.length">
                  {{ step.people.map(p => p.role + '：' + p.name).join('，') }}
                </div>
              </div>
              <div v-if="idx < traceDetail.traceChain.length-1" class="step-arrow">↓</div>
            </div>
          </div>
        </ModulePanelSection>

        <!-- 操作区 -->
        <ModulePanelSection v-if="selected.caseStatus !== 'CLOSED'">
          <div class="panel-title">案例操作</div>
          <template v-if="selected.caseStatus === 'OPEN'">
            <div class="action-row">
              <el-button type="primary" :loading="acting" @click="doAccept">受理案例</el-button>
            </div>
          </template>
          <template v-else-if="selected.caseStatus === 'PROCESSING'">
            <el-form label-width="80px" size="small">
              <el-form-item label="解决方案" required>
                <el-input v-model="resolveForm.solution" type="textarea" rows="2" placeholder="必填，描述处理方案" />
              </el-form-item>
              <el-form-item label="追溯结论">
                <el-input v-model="resolveForm.traceResult" type="textarea" rows="2"
                  placeholder="可选，如：质检记录显示该批次坏点超标，已联动质量改进" />
              </el-form-item>
            </el-form>
            <div class="action-row">
              <el-button type="success" :loading="acting" @click="doResolve">标记已解决</el-button>
              <el-button :loading="acting" @click="doClose">直接关闭</el-button>
            </div>
          </template>
          <template v-else-if="selected.caseStatus === 'RESOLVED'">
            <div class="action-row">
              <el-button :loading="acting" @click="doClose">关闭案例</el-button>
            </div>
          </template>
        </ModulePanelSection>
        <ModulePanelSection v-else><el-tag type="info">案例已关闭</el-tag></ModulePanelSection>
      </template>
    </ModuleListDetailLayout>

    <!-- 登记新案例对话框 -->
    <el-dialog v-model="newDialog" title="登记售后案例" width="500px" :close-on-click-modal="false">
      <el-form :model="newForm" label-width="90px" size="small">
        <el-form-item label="客户名称" required>
          <el-input v-model="newForm.customerName" placeholder="必填" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="newForm.contactName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="newForm.contactPhone" />
        </el-form-item>
        <el-form-item label="问题类型">
          <el-select v-model="newForm.problemType" style="width:100%">
            <el-option v-for="t in PROBLEM_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重级别">
          <el-radio-group v-model="newForm.caseLevel">
            <el-radio value="LOW">低</el-radio>
            <el-radio value="MEDIUM">中</el-radio>
            <el-radio value="HIGH">高</el-radio>
            <el-radio value="CRITICAL">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="newForm.batchNo" placeholder="可选，关联批次" />
        </el-form-item>
        <el-form-item label="关联质检单">
          <el-input v-model="newForm.qualityInspectionId" placeholder="可选，填写质检单ID" />
        </el-form-item>
        <el-form-item label="问题描述" required>
          <el-input v-model="newForm.problemDescription" type="textarea" rows="3" placeholder="必填，详述客户反映的问题" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newDialog=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSaveNew">登记</el-button>
      </template>
    </el-dialog>
  </ModulePageShell>
</template>
<!-- SCRIPT_PLACEHOLDER -->

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { fetchCaseViews, fetchAfterSalesKpi, fetchTraceDetail, acceptCase, resolveCase, closeCase, insertCase } from '@/api/aftersale'
import KpiStrip from '@/components/module/KpiStrip.vue'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import ModuleListDetailLayout from '@/components/module/ModuleListDetailLayout.vue'
import ModulePanelSection from '@/components/module/ModulePanelSection.vue'
import { moduleStatusType } from '@/constants/moduleStatus'

const userStore = useUserStore()
const route = useRoute()

const list        = ref([])
const kpi         = ref({})
const selected    = ref(null)
const traceDetail = ref(null)
const keyword     = ref('')
const statusFilter = ref('')
const tabActive   = ref('all')
const loading     = ref(false)
const traceLoading = ref(false)
const acting      = ref(false)
const saving      = ref(false)
const newDialog   = ref(false)

const resolveForm = reactive({ solution: '', traceResult: '' })
const newForm = reactive({
  customerName: '', contactName: '', contactPhone: '',
  problemType: 'DISPLAY_DEFECT', caseLevel: 'MEDIUM',
  batchNo: '', qualityInspectionId: '', problemDescription: ''
})

const PROBLEM_TYPES = [
  { value: 'DISPLAY_DEFECT',  label: '显示缺陷' },
  { value: 'COLOR_ISSUE',     label: '色彩问题' },
  { value: 'DEAD_PIXEL',      label: '坏点/亮点' },
  { value: 'INTERFACE_FAULT', label: '接口故障' },
  { value: 'APPEARANCE',      label: '外观损伤' },
  { value: 'POWER_ISSUE',     label: '电源问题' },
  { value: 'OTHER',           label: '其他' }
]

const kpiCards = [
  { key: 'all',        kpiKey: 'total',      label: '全部',   cls: '' },
  { key: 'OPEN',       kpiKey: 'open',       label: '待受理', cls: 'open' },
  { key: 'PROCESSING', kpiKey: 'processing', label: '处理中', cls: 'processing' },
  { key: 'RESOLVED',   kpiKey: 'resolved',   label: '已解决', cls: 'resolved' },
  { key: 'CLOSED',     kpiKey: 'closed',     label: '已关闭', cls: 'closed' },
]

const filtered = computed(() => {
  let data = list.value
  if (tabActive.value !== 'all') data = data.filter(r => r.caseStatus === tabActive.value)
  if (statusFilter.value) data = data.filter(r => r.caseStatus === statusFilter.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    data = data.filter(r =>
      (r.caseNo || '').toLowerCase().includes(kw) ||
      (r.orderNo || '').toLowerCase().includes(kw) ||
      (r.customerName || '').toLowerCase().includes(kw) ||
      (r.batchNo || '').toLowerCase().includes(kw)
    )
  }
  return data
})

function statusTagType(s) {
  return moduleStatusType('aftersaleCase', s)
}
function levelTagType(s) {
  return { GENERAL: '', IMPORTANT: 'warning', URGENT: 'danger', LOW: '', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }[s] || ''
}
function stepIcon(type) {
  return {
    feedback: '🛎', delivery: '🚚', inbound: '📥', quality: '🔍', production: '🏭',
    material: '📦', material_quality: '🧪', supplier: '🤝',
    order: '📋', defect: '⚠️', aftersale: '🛎'
  }[type] || '●'
}

async function loadData() {
  loading.value = true
  try {
    const [v, k] = await Promise.all([
      fetchCaseViews().catch(() => null),
      fetchAfterSalesKpi().catch(() => null)
    ])
    if (v) list.value = v.data ?? v
    if (k) kpi.value = k.data ?? k
  } finally {
    loading.value = false
  }
}

function onSelect(row) {
  selected.value = row
  traceDetail.value = null
  resolveForm.solution = ''
  resolveForm.traceResult = ''
}

async function loadTrace() {
  if (!selected.value) return
  traceLoading.value = true
  try {
    const res = await fetchTraceDetail(selected.value.caseNo)
    traceDetail.value = res.data ?? res
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '追溯加载失败')
  } finally {
    traceLoading.value = false
  }
}

async function doAccept() {
  await ElMessageBox.confirm('确认受理该售后案例？', '受理确认', { type: 'info' })
  acting.value = true
  try {
    await acceptCase({ caseNo: selected.value.caseNo, operator: userStore.userInfo?.username })
    ElMessage.success('已受理，案例进入处理中')
    await loadData(); selected.value = null
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '操作失败') }
  finally { acting.value = false }
}

async function doResolve() {
  if (!resolveForm.solution.trim()) { ElMessage.warning('请填写解决方案'); return }
  acting.value = true
  try {
    await resolveCase({
      caseNo: selected.value.caseNo,
      solution: resolveForm.solution,
      traceResult: resolveForm.traceResult,
      operator: userStore.userInfo?.username
    })
    ElMessage.success('案例已标记解决')
    await loadData(); selected.value = null
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '操作失败') }
  finally { acting.value = false }
}

async function doClose() {
  await ElMessageBox.confirm('确认关闭该售后案例？', '关闭确认', { type: 'warning' })
  acting.value = true
  try {
    await closeCase({ caseNo: selected.value.caseNo, remark: '手动关闭', operator: userStore.userInfo?.username })
    ElMessage.success('案例已关闭')
    await loadData(); selected.value = null
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '操作失败') }
  finally { acting.value = false }
}

function openNewDialog() {
  Object.assign(newForm, {
    customerName: '', contactName: '', contactPhone: '',
    problemType: 'DISPLAY_DEFECT', caseLevel: 'MEDIUM',
    batchNo: '', qualityInspectionId: '', problemDescription: ''
  })
  newDialog.value = true
}

async function doSaveNew() {
  if (!newForm.customerName.trim()) { ElMessage.warning('请填写客户名称'); return }
  if (!newForm.problemDescription.trim()) { ElMessage.warning('请填写问题描述'); return }
  saving.value = true
  try {
    const now = new Date().toISOString().slice(0, 19).replace('T', ' ')
    const caseNo = 'AS' + Date.now()
    await insertCase({
      caseNo,
      customerName: newForm.customerName,
      contactName: newForm.contactName,
      contactPhone: newForm.contactPhone,
      problemType: newForm.problemType,
      caseLevel: newForm.caseLevel,
      batchNo: newForm.batchNo || null,
      qualityInspectionId: newForm.qualityInspectionId ? Number(newForm.qualityInspectionId) : null,
      problemDescription: newForm.problemDescription,
      caseStatus: 'OPEN',
      openedAt: now,
      createdAt: now,
      updatedAt: now
    })
    ElMessage.success('售后案例已登记')
    newDialog.value = false
    await loadData()
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '登记失败') }
  finally { saving.value = false }
}

onMounted(async () => {
  await loadData()
  const caseNo = String(route.query.caseNo || '')
  if (!caseNo) return
  const target = list.value.find(item => item.caseNo === caseNo)
  if (target) {
    await nextTick()
    onSelect(target)
  }
})
</script>

<style scoped>
.list-toolbar { display: flex; align-items: center; }
.panel-title {
  font-size: 13px; font-weight: 600; color: #2c3e50;
  margin-bottom: 10px; display: flex; align-items: center;
}

/* 追溯链路 */
.trace-chain { display: flex; flex-direction: column; gap: 0; }
.trace-step  { display: flex; flex-direction: column; align-items: flex-start; position: relative; }
.step-icon   {
  width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center;
  justify-content: center; font-size: 14px; background: #f0f4ff; border: 2px solid #c6d1f0;
  flex-shrink: 0; margin-bottom: 4px;
}
.step-icon.step-order    { background: #e3f2fd; border-color: #90caf9; }
.step-icon.step-material { background: #f3e5f5; border-color: #ce93d8; }
.step-icon.step-quality  { background: #e8f5e9; border-color: #a5d6a7; }
.step-icon.step-defect   { background: #fff3e0; border-color: #ffcc80; }
.step-icon.step-aftersale{ background: #fce4ec; border-color: #f48fb1; }
.step-icon.step-feedback { background: #fce4ec; border-color: #f48fb1; }
.step-icon.step-delivery { background: #e3f2fd; border-color: #90caf9; }
.step-icon.step-inbound  { background: #e0f7fa; border-color: #80deea; }
.step-icon.step-production { background: #ede7f6; border-color: #b39ddb; }
.step-icon.step-material_quality { background: #e8f5e9; border-color: #a5d6a7; }
.step-icon.step-supplier { background: #fff8e1; border-color: #ffe082; }
.trace-step.missing { opacity: .45; }
.step-people { font-size: 11px; color: #2f6fce; font-weight: 600; margin-top: 2px; }
.step-body   { padding-left: 8px; margin-bottom: 4px; }
.step-title  { font-size: 11px; color: #8492a6; }
.step-no     { font-size: 13px; font-weight: 600; color: #2c3e50; }
.step-desc   { font-size: 12px; color: #606266; margin-top: 2px; }
.step-arrow  { font-size: 16px; color: #c0c4cc; padding-left: 10px; margin-bottom: 4px; }

.action-row { display: flex; gap: 8px; flex-wrap: wrap; }
.empty-hint { color: #c0c4cc; font-size: 12px; padding: 8px 0; }
</style>
