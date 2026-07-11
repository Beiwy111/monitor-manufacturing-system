<template>
  <div class="qc-page">
    <div class="kpi-bar">
      <div class="kpi-card">
        <div class="kpi-num">{{ list.length }}</div>
        <div class="kpi-label">需复检总数</div>
      </div>
      <div class="kpi-card semi">
        <div class="kpi-num">{{ semiCount }}</div>
        <div class="kpi-label">半成品复检</div>
      </div>
      <div class="kpi-card fp">
        <div class="kpi-num">{{ fpCount }}</div>
        <div class="kpi-label">成品复检</div>
      </div>
    </div>

    <div class="page-body">
      <div class="list-panel">
        <div class="list-toolbar">
          <el-input v-model="keyword" placeholder="质检单/工单/批次" clearable size="small" style="width:200px" />
          <el-select v-model="categoryFilter" clearable placeholder="分类" size="small" style="width:100px;margin-left:6px">
            <el-option label="半成品质检" value="SEMI_FINISHED" />
            <el-option label="成品质检" value="FINISHED_PRODUCT" />
          </el-select>
          <el-button size="small" style="margin-left:6px" :loading="loading" @click="loadData">刷新</el-button>
        </div>
        <el-table v-loading="loading" :data="filtered" border stripe highlight-current-row size="small"
          empty-text="暂无需复检任务" @current-change="onSelect">
          <el-table-column prop="inspectionNo" label="质检单号" width="145" />
          <el-table-column label="分类" width="90">
            <template #default="{ row }">
              <el-tag :type="row.inspectionCategory==='SEMI_FINISHED'?'warning':'success'" size="small" effect="plain">
                {{ row.inspectionCategoryCn }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="inspectionTypeCn" label="类型" width="80" />
          <el-table-column prop="workOrderNo" label="工单" width="120" show-overflow-tooltip />
          <el-table-column prop="materialName" label="物料" min-width="95" show-overflow-tooltip />
          <el-table-column prop="batchNo" label="批次" width="120" show-overflow-tooltip />
          <el-table-column prop="sampleQuantity" label="抽样" width="55" align="center" />
          <el-table-column prop="unqualifiedQuantity" label="不良" width="55" align="center">
            <template #default="{ row }">
              <span style="color:#f56c6c;font-weight:700">{{ row.unqualifiedQuantity }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="转复检时间" width="148" show-overflow-tooltip />
        </el-table>
      </div>

      <div class="detail-panel" v-if="selected">
        <div class="detail-section">
          <div class="section-title">
            <el-tag :type="selected.inspectionCategory==='SEMI_FINISHED'?'warning':'success'" size="small" style="margin-right:6px">
              {{ selected.inspectionCategoryCn }}
            </el-tag>
            {{ selected.inspectionNo }}
            <el-tag type="warning" size="small" style="margin-left:8px">需复检</el-tag>
          </div>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="工单">{{ selected.workOrderNo||'-' }}</el-descriptions-item>
            <el-descriptions-item label="物料">{{ selected.materialName||'-' }}</el-descriptions-item>
            <el-descriptions-item label="批次">{{ selected.batchNo }}</el-descriptions-item>
            <el-descriptions-item label="检验类型">{{ selected.inspectionTypeCn }}</el-descriptions-item>
            <el-descriptions-item label="送检数">{{ selected.sampleQuantity }}</el-descriptions-item>
            <el-descriptions-item label="上次不良数">
              <span style="color:#f56c6c;font-weight:700">{{ selected.unqualifiedQuantity }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="检验员">{{ selected.inspectorName||'-' }}</el-descriptions-item>
            <el-descriptions-item label="转复检时间">{{ selected.updatedAt||'-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ selected.remark||'-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section" v-if="items.length">
          <div class="section-title">上次检测项记录</div>
          <el-table :data="items" border size="small">
            <el-table-column prop="itemName" label="检测项目" min-width="90" />
            <el-table-column prop="standardValue" label="标准值" width="95" show-overflow-tooltip />
            <el-table-column prop="measuredValue" label="实测值" width="85">
              <template #default="{ row }">{{ row.measuredValue||'-' }}</template>
            </el-table-column>
            <el-table-column label="结果" width="72">
              <template #default="{ row }">
                <el-tag :type="itemTagType(row.result)" size="small">{{ row.resultCn }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="缺陷级别" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.defectLevel" :type="defectTagType(row.defectLevel)" size="small">
                  {{ row.defectLevelCn }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="detail-section">
          <div class="section-title">复检判定</div>
          <el-form label-width="80px" size="small">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" rows="2" placeholder="可选，填写复检说明" />
            </el-form-item>
          </el-form>
          <div class="action-row">
            <el-button type="success" :loading="acting" @click="doRecheckPass">复检通过</el-button>
            <el-button type="danger" @click="openFailDialog">复检不通过</el-button>
          </div>
        </div>
      </div>

      <div class="detail-panel empty" v-else>
        <el-empty description="从左侧选择需复检的质检单进行处理" />
      </div>
    </div>

    <el-dialog v-model="failDialog" title="复检不通过 - 填写不良信息" width="460px" :close-on-click-modal="false">
      <el-form :model="failForm" label-width="90px" size="small">
        <el-form-item label="不良原因" required>
          <el-input v-model="failForm.defectReason" type="textarea" rows="2" placeholder="必填，描述复检缺陷" />
        </el-form-item>
        <el-form-item label="缺陷类型">
          <el-select v-model="failForm.defectType" style="width:100%">
            <el-option v-for="t in DEFECT_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="不良数量" required>
          <el-input-number v-model="failForm.defectQuantity" :min="1" style="width:130px" />
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="failForm.severity" style="width:100%">
            <el-option label="轻微 MINOR" value="MINOR" />
            <el-option label="一般 GENERAL" value="GENERAL" />
            <el-option label="严重 MAJOR" value="MAJOR" />
            <el-option label="致命 CRITICAL" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="failForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="failDialog=false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doRecheckFail">确认复检不通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { fetchRecheckViews, fetchInspectionItems, recheckPass, recheckFail } from '@/api/quality'

const route = useRoute()
const userStore = useUserStore()
const list = ref([])
const selected = ref(null)
const items = ref([])
const loading = ref(false)
const acting = ref(false)
const failDialog = ref(false)
const keyword = ref('')
const categoryFilter = ref('')
const form = reactive({ remark: '' })

const routeCategory = computed(() => route.meta?.category || '')
categoryFilter.value = routeCategory.value
const failForm = reactive({
  defectReason: '', defectType: '外观缺陷', defectQuantity: 1, severity: 'GENERAL', remark: ''
})

const DEFECT_TYPES = ['外观缺陷', '色差', '亮点不良', '暗点不良', '坏点', '漏光', '线路不良', '结构问题', '其他']

const filtered = computed(() => {
  let data = list.value
  if (categoryFilter.value) data = data.filter(r => r.inspectionCategory === categoryFilter.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    data = data.filter(r =>
      (r.inspectionNo||'').toLowerCase().includes(kw) ||
      (r.workOrderNo||'').toLowerCase().includes(kw) ||
      (r.batchNo||'').toLowerCase().includes(kw)
    )
  }
  return data
})

const semiCount = computed(() => list.value.filter(r => r.inspectionCategory === 'SEMI_FINISHED').length)
const fpCount   = computed(() => list.value.filter(r => r.inspectionCategory === 'FINISHED_PRODUCT').length)

function itemTagType(r)  { return { PASSED:'success', FAILED:'danger', WARNING:'warning', PENDING:'info' }[r] || 'info' }
function defectTagType(l){ return { MINOR:'', MAJOR:'warning', CRITICAL:'danger' }[l] || '' }

async function loadData() {
  loading.value = true
  const res = await fetchRecheckViews().catch(() => null)
  if (res) list.value = res.data ?? res
  loading.value = false
}

async function onSelect(row) {
  selected.value = row
  form.remark = ''
  items.value = []
  if (!row) return
  const it = await fetchInspectionItems(row.inspectionId).catch(() => null)
  if (it) items.value = it.data ?? it
}

async function doRecheckPass() {
  await ElMessageBox.confirm('确认复检通过？', '复检通过', { type: 'success' })
  acting.value = true
  try {
    await recheckPass({ inspectionId: selected.value.inspectionId, remark: form.remark, operator: userStore.userInfo?.username })
    ElMessage.success('复检通过，工单已更新')
    await loadData()
    selected.value = null
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}

function openFailDialog() {
  Object.assign(failForm, { defectReason: '', defectType: '外观缺陷', defectQuantity: 1, severity: 'GENERAL', remark: '' })
  failDialog.value = true
}

async function doRecheckFail() {
  if (!failForm.defectReason.trim()) { ElMessage.warning('请填写不良原因'); return }
  acting.value = true
  try {
    await recheckFail({ inspectionId: selected.value.inspectionId, ...failForm, operator: userStore.userInfo?.username })
    ElMessage.warning('复检不通过，已更新不良品记录')
    failDialog.value = false
    await loadData()
    selected.value = null
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}

onMounted(loadData)

watch(routeCategory, (val) => {
  categoryFilter.value = val
  selected.value = null
  loadData()
})
</script>

<style scoped>
.qc-page { display:flex; flex-direction:column; height:100%; padding:10px; gap:10px; background:#f5f6fa; }
.kpi-bar { display:flex; gap:8px; flex-wrap:wrap; }
.kpi-card { flex:1; min-width:80px; background:#fff; border:2px solid transparent; border-radius:8px; padding:8px 12px; text-align:center; }
.kpi-card.semi { border-left:3px solid #e6a23c; }
.kpi-card.fp   { border-left:3px solid #67c23a; }
.kpi-num   { font-size:20px; font-weight:700; color:#303133; }
.kpi-label { font-size:11px; color:#909399; margin-top:2px; }
.page-body { display:flex; gap:10px; flex:1; min-height:0; }
.list-panel { flex:1; min-width:0; display:flex; flex-direction:column; gap:8px; background:#fff; border-radius:6px; padding:10px; }
.list-toolbar { display:flex; align-items:center; flex-wrap:wrap; gap:4px; }
.detail-panel { width:420px; flex-shrink:0; display:flex; flex-direction:column; gap:10px; overflow-y:auto; }
.detail-panel.empty { background:#fff; border-radius:6px; align-items:center; justify-content:center; }
.detail-section { background:#fff; border-radius:6px; padding:12px 14px; }
.section-title { display:flex; align-items:center; font-size:13px; font-weight:600; color:#303133; margin-bottom:10px; }
.action-row { display:flex; gap:8px; flex-wrap:wrap; }
</style>
