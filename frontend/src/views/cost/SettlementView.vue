<template>
  <ModulePageShell>
    <!-- KPI 栏 -->
    <KpiStrip v-model="activeKpi" :cards="kpiCards" :metrics="kpi" :formatter="fmt" @select="onKpiSelect">
      <template #actions>
        <el-button type="primary" size="small" @click="openNewDialog">+ 登记成本</el-button>
        <el-button size="small" style="margin-left:6px" :loading="loading" @click="loadData">刷新</el-button>
      </template>
    </KpiStrip>

    <!-- 来源类型分布 -->
    <div class="source-bar">
      <div v-for="g in groups" :key="g.sourceType" class="source-card"
        :class="'src-'+g.sourceType"
        :style="filterSource===g.sourceType?'outline:2px solid #409eff':''"
        @click="filterSource = filterSource===g.sourceType ? '' : g.sourceType">
        <el-tag :type="sourceTagType(g.sourceType)" size="small">{{ g.sourceTypeCn }}</el-tag>
        <span class="src-amount">¥ {{ fmtAmt(g.amount) }}</span>
        <span class="src-count">{{ g.count }} 笔</span>
      </div>
      <div v-if="filterSource" class="source-clear" @click="filterSource=''">
        <el-tag size="small" type="info" closable @close="filterSource=''">清除筛选</el-tag>
      </div>
    </div>

    <ModuleListDetailLayout
      :has-detail="!!selected"
      detail-width="380px"
      empty-description="点击左侧结算单查看明细"
    >
      <!-- 列表 -->
      <template #list>
        <div class="list-toolbar">
          <el-input v-model="keyword" placeholder="结算单/工单/备注" clearable size="small" style="width:190px" />
          <el-select v-model="filterStatus" clearable placeholder="状态" size="small" style="width:90px;margin-left:6px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已确认" value="CONFIRMED" />
            <el-option label="已导出" value="EXPORTED" />
          </el-select>
        </div>
        <el-table :data="filtered" border stripe highlight-current-row size="small"
          style="width:100%" @current-change="onSelect">
          <el-table-column prop="settlementNo" label="结算单号" width="150" />
          <el-table-column label="来源类型" width="110">
            <template #default="{ row }">
              <el-tag :type="sourceTagType(row.sourceType)" size="small">{{ row.sourceTypeCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sourceId" label="来源编号" width="130" show-overflow-tooltip />
          <el-table-column prop="workOrderNo" label="工单" width="120" show-overflow-tooltip />
          <el-table-column label="质量成本" width="90" align="right">
            <template #default="{ row }">
              <span :style="row.qualityCost>0?'color:#f56c6c;font-weight:600':''">
                {{ row.qualityCost ? '¥'+fmtAmt(row.qualityCost) : '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="合计" width="100" align="right">
            <template #default="{ row }">
              <b>¥ {{ fmtAmt(row.totalCost) }}</b>
            </template>
          </el-table-column>
          <el-table-column prop="settlementPeriod" label="期间" width="90" />
          <el-table-column label="状态" width="82">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.settlementStatus)" size="small">{{ row.settlementStatusCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="登记时间" width="148" />
        </el-table>
      </template>

      <!-- 右侧详情 -->
      <template #detail>
        <ModulePanelSection>
          <div class="panel-title">
            结算详情
            <el-tag :type="statusTagType(selected.settlementStatus)" size="small" style="margin-left:8px">
              {{ selected.settlementStatusCn }}
            </el-tag>
          </div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="结算单号">{{ selected.settlementNo }}</el-descriptions-item>
            <el-descriptions-item label="来源类型">
              <el-tag :type="sourceTagType(selected.sourceType)" size="small">{{ selected.sourceTypeCn }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="来源编号">{{ selected.sourceId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="工单号">{{ selected.workOrderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结算期间">{{ selected.settlementPeriod || '-' }}</el-descriptions-item>
            <el-descriptions-item label="确认时间">{{ selected.confirmedAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="成本原因" :span="2">{{ selected.costReason || '-' }}</el-descriptions-item>
          </el-descriptions>
        </ModulePanelSection>

        <!-- 成本明细 -->
        <ModulePanelSection>
          <div class="panel-title">成本明细</div>
          <div class="cost-breakdown">
            <div class="cost-row" v-for="item in costBreakdown" :key="item.key">
              <span class="cost-name">{{ item.label }}</span>
              <span class="cost-bar-wrap">
                <span class="cost-bar" :style="{ width: item.pct+'%', background: item.color }"></span>
              </span>
              <span class="cost-val" :style="item.val>0?'font-weight:600':''">
                {{ item.val > 0 ? '¥ ' + fmtAmt(item.val) : '-' }}
              </span>
            </div>
            <div class="cost-total">
              <span>合计</span>
              <span>¥ {{ fmtAmt(selected.totalCost) }}</span>
            </div>
          </div>
        </ModulePanelSection>

        <!-- 备注 -->
        <ModulePanelSection v-if="selected.remark">
          <div class="panel-title">备注</div>
          <div style="font-size:13px;color:#606266">{{ selected.remark }}</div>
        </ModulePanelSection>

        <!-- 操作 -->
        <ModulePanelSection v-if="selected.settlementStatus !== 'EXPORTED'">
          <div class="panel-title">操作</div>
          <div class="action-row">
            <el-button v-if="selected.settlementStatus==='DRAFT'"
              type="primary" :loading="acting" @click="doConfirm">确认结算</el-button>
            <el-button v-if="selected.settlementStatus==='CONFIRMED'"
              type="success" :loading="acting" @click="doExport">标记已导出</el-button>
          </div>
        </ModulePanelSection>
        <ModulePanelSection v-else><el-tag type="info">已导出归档</el-tag></ModulePanelSection>
      </template>
    </ModuleListDetailLayout>

    <!-- 登记新成本弹窗 -->
    <el-dialog v-model="newDialog" title="登记质量成本" width="520px" :close-on-click-modal="false">
      <el-form :model="newForm" label-width="90px" size="small">
        <el-form-item label="来源类型" required>
          <el-select v-model="newForm.sourceType" style="width:100%">
            <el-option v-for="t in SOURCE_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源编号">
          <el-input v-model="newForm.sourceId"
            :placeholder="sourceIdPlaceholder" />
        </el-form-item>
        <el-form-item label="成本原因" required>
          <el-input v-model="newForm.costReason" type="textarea" rows="2" placeholder="必填，说明成本产生原因" />
        </el-form-item>
        <el-form-item label="结算期间">
          <el-input v-model="newForm.settlementPeriod" placeholder="如 2026-07" />
        </el-form-item>
        <el-divider content-position="left" style="margin:10px 0 8px">成本构成（元）</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="材料成本"><el-input-number v-model="newForm.materialCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人工成本"><el-input-number v-model="newForm.laborCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备成本"><el-input-number v-model="newForm.equipmentCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="质量成本"><el-input-number v-model="newForm.qualityCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="其他成本"><el-input-number v-model="newForm.otherCost" :min="0" :precision="2" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合计（自动）">
              <span style="font-size:15px;font-weight:700;color:#f56c6c">¥ {{ fmtAmt(newFormTotal) }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="newForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newDialog=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSave">登记</el-button>
      </template>
    </el-dialog>
  </ModulePageShell>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { fetchSettlementViews, fetchCostKpi, fetchCostGroup, confirmSettlement, exportSettlement, saveSettlement } from '@/api/cost'
import KpiStrip from '@/components/module/KpiStrip.vue'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import ModuleListDetailLayout from '@/components/module/ModuleListDetailLayout.vue'
import ModulePanelSection from '@/components/module/ModulePanelSection.vue'
import { moduleStatusType } from '@/constants/moduleStatus'

const userStore = useUserStore()

const list     = ref([])
const kpi      = ref({})
const groups   = ref([])
const selected = ref(null)
const loading  = ref(false)
const acting   = ref(false)
const saving   = ref(false)
const newDialog = ref(false)
const keyword     = ref('')
const filterStatus = ref('')
const filterSource = ref('')
const activeKpi = ref('total')

const SOURCE_TYPES = [
  { value: 'NONCONFORMING_PRODUCT', label: '不良品处理' },
  { value: 'AFTER_SALES',           label: '售后维修' },
  { value: 'EQUIPMENT_MAINTENANCE', label: '设备维修' },
  { value: 'PURCHASE_RETURN',       label: '采购退货损失' },
  { value: 'WORK_ORDER',            label: '工单成本' },
  { value: 'OTHER',                 label: '其他' },
]

const newForm = reactive({
  sourceType: 'NONCONFORMING_PRODUCT', sourceId: '', costReason: '',
  settlementPeriod: new Date().toISOString().slice(0, 7),
  materialCost: 0, laborCost: 0, equipmentCost: 0, qualityCost: 0, otherCost: 0, remark: ''
})

const kpiCards = [
  { key: 'total',     kpiKey: 'total',     label: '总笔数',   cls: '',          filterVal: '' },
  { key: 'draft',     kpiKey: 'draft',     label: '草稿',     cls: 'draft',     filterVal: 'DRAFT' },
  { key: 'confirmed', kpiKey: 'confirmed', label: '已确认',   cls: 'confirmed', filterVal: 'CONFIRMED' },
  { key: 'exported',  kpiKey: 'exported',  label: '已导出',   cls: 'exported',  filterVal: 'EXPORTED' },
  { key: 'total-amt', kpiKey: 'totalAmount', label: '成本总额', cls: 'amount', filterVal: null },
  { key: 'quality',   kpiKey: 'totalQualityCost', label: '质量成本', cls: 'quality', filterVal: null },
]

const newFormTotal = computed(() =>
  (newForm.materialCost||0) + (newForm.laborCost||0) +
  (newForm.equipmentCost||0) + (newForm.qualityCost||0) + (newForm.otherCost||0)
)

const sourceIdPlaceholder = computed(() => {
  const m = {
    NONCONFORMING_PRODUCT: '不良品单号，如 NC202607201',
    AFTER_SALES:           '售后案例号，如 AS202607001',
    EQUIPMENT_MAINTENANCE: '设备编号',
    PURCHASE_RETURN:       '采购单号',
    WORK_ORDER:            '工单号',
  }
  return m[newForm.sourceType] || '来源业务编号'
})

const costBreakdown = computed(() => {
  if (!selected.value) return []
  const total = Number(selected.value.totalCost) || 1
  const items = [
    { key: 'materialCost',  label: '材料成本', val: Number(selected.value.materialCost)||0,  color: '#409eff' },
    { key: 'laborCost',     label: '人工成本', val: Number(selected.value.laborCost)||0,     color: '#67c23a' },
    { key: 'equipmentCost', label: '设备成本', val: Number(selected.value.equipmentCost)||0, color: '#e6a23c' },
    { key: 'qualityCost',   label: '质量成本', val: Number(selected.value.qualityCost)||0,   color: '#f56c6c' },
    { key: 'otherCost',     label: '其他成本', val: Number(selected.value.otherCost)||0,     color: '#909399' },
  ]
  return items.map(i => ({ ...i, pct: Math.round(i.val / total * 100) }))
})

const filtered = computed(() => {
  let data = list.value
  if (filterStatus.value) data = data.filter(r => r.settlementStatus === filterStatus.value)
  if (filterSource.value) data = data.filter(r => r.sourceType === filterSource.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    data = data.filter(r =>
      (r.settlementNo || '').toLowerCase().includes(kw) ||
      (r.workOrderNo  || '').toLowerCase().includes(kw) ||
      (r.sourceId     || '').toLowerCase().includes(kw) ||
      (r.remark       || '').toLowerCase().includes(kw)
    )
  }
  return data
})

function statusTagType(s) {
  return moduleStatusType('costSettlement', s)
}
function sourceTagType(s) {
  return moduleStatusType('costSource', s)
}
function fmt(v) {
  if (v === null || v === undefined) return '0'
  const n = Number(v)
  if (isNaN(n)) return v
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}
function fmtAmt(v) {
  const n = Number(v)
  if (isNaN(n) || n === 0) return '0.00'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadData() {
  loading.value = true
  try {
    const [v, k, g] = await Promise.all([
      fetchSettlementViews().catch(() => null),
      fetchCostKpi().catch(() => null),
      fetchCostGroup().catch(() => null),
    ])
    if (v) list.value = v.data ?? v
    if (k) kpi.value  = k.data ?? k
    if (g) groups.value = g.data ?? g
  } finally {
    loading.value = false
  }
}

function onSelect(row) { selected.value = row }

function onKpiSelect(card) {
  filterStatus.value = card.filterVal ?? ''
}

async function doConfirm() {
  await ElMessageBox.confirm('确认该结算单？确认后不可修改。', '确认结算', { type: 'warning' })
  acting.value = true
  try {
    await confirmSettlement(selected.value.settlementId, userStore.userInfo?.username)
    ElMessage.success('结算已确认')
    await loadData(); selected.value = null
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '操作失败') }
  finally { acting.value = false }
}

async function doExport() {
  acting.value = true
  try {
    await exportSettlement(selected.value.settlementId, userStore.userInfo?.username)
    ElMessage.success('已标记导出')
    await loadData(); selected.value = null
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '操作失败') }
  finally { acting.value = false }
}

function openNewDialog() {
  Object.assign(newForm, {
    sourceType: 'NONCONFORMING_PRODUCT', sourceId: '', costReason: '',
    settlementPeriod: new Date().toISOString().slice(0, 7),
    materialCost: 0, laborCost: 0, equipmentCost: 0, qualityCost: 0, otherCost: 0, remark: ''
  })
  newDialog.value = true
}

async function doSave() {
  if (!newForm.costReason.trim()) { ElMessage.warning('请填写成本原因'); return }
  if (newFormTotal.value <= 0) { ElMessage.warning('至少填写一项成本金额'); return }
  saving.value = true
  try {
    const now = new Date().toISOString().slice(0, 19).replace('T', ' ')
    await saveSettlement({
      settlementNo: 'CS' + Date.now(),
      sourceType:   newForm.sourceType,
      sourceId:     newForm.sourceId || null,
      costReason:   newForm.costReason,
      settlementPeriod: newForm.settlementPeriod,
      materialCost:  newForm.materialCost || 0,
      laborCost:     newForm.laborCost    || 0,
      equipmentCost: newForm.equipmentCost|| 0,
      qualityCost:   newForm.qualityCost  || 0,
      otherCost:     newForm.otherCost    || 0,
      settlementStatus: 'DRAFT',
      remark: newForm.remark || null,
      createdAt: now, updatedAt: now
    })
    ElMessage.success('成本记录已登记')
    newDialog.value = false
    await loadData()
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '登记失败') }
  finally { saving.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.source-bar {
  display: flex; gap: 8px; flex-wrap: wrap; align-items: center;
  background: #fff; border: 1px solid #e4e7ed; border-radius: 6px;
  padding: 10px 14px;
}
.source-card {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 12px; border-radius: 4px; cursor: pointer;
  border: 1px solid #ebeef5; background: #fafafa;
  transition: box-shadow .12s;
}
.source-card:hover { box-shadow: 0 1px 6px rgba(0,0,0,.1); }
.src-amount { font-size: 13px; font-weight: 600; color: #f56c6c; }
.src-count  { font-size: 11px; color: #909399; }
.source-clear { margin-left: 4px; }

.list-toolbar { display: flex; align-items: center; }
.panel-title { font-size: 13px; font-weight: 600; color: #2c3e50; margin-bottom: 10px; display: flex; align-items: center; }

.cost-breakdown { display: flex; flex-direction: column; gap: 8px; }
.cost-row    { display: flex; align-items: center; gap: 8px; }
.cost-name   { width: 64px; font-size: 12px; color: #606266; flex-shrink: 0; }
.cost-bar-wrap { flex: 1; height: 8px; background: #f0f2f5; border-radius: 4px; overflow: hidden; }
.cost-bar    { display: block; height: 100%; border-radius: 4px; transition: width .3s; min-width: 4px; }
.cost-val    { width: 90px; text-align: right; font-size: 13px; color: #2c3e50; }
.cost-total  { display: flex; justify-content: space-between; padding-top: 8px; border-top: 1px solid #ebeef5; font-weight: 700; font-size: 14px; color: #2c3e50; }

.action-row { display: flex; gap: 8px; }
</style>
