<template>
  <div class="ruoyi-page trace-page">
    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">质量追溯</span>
      <el-input v-model="keyword" placeholder="质检单号 / 工单号 / 批次号 / 物料名" clearable style="width:300px" @keyup.enter="doSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" :loading="searching" @click="doSearch">追溯</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div v-if="!searched" class="trace-empty">
      <el-empty description="输入质检单号、工单号或批次号开始追溯">
        <div class="trace-tip-list">
          <div class="trace-tip-item" v-for="tip in TIPS" :key="tip.val" @click="keyword=tip.val;doSearch()">
            <span class="tip-tag">{{ tip.label }}</span>
            <span class="tip-val">{{ tip.val }}</span>
          </div>
        </div>
      </el-empty>
    </div>

    <div v-else-if="!results.length" class="trace-empty">
      <el-empty :description="`未找到与「${searchedKw}」相关的质检记录`" />
    </div>

    <div v-else class="qc-split-layout trace-split">
      <div class="qc-split-layout__main ruoyi-table-wrap">
        <el-table :data="results" border stripe highlight-current-row size="small"
          @current-change="onSelect">
          <el-table-column prop="inspectionNo" label="质检单号" width="140" />
          <el-table-column label="分类" width="88">
            <template #default="{ row }">
              <el-tag :type="row.inspectionCategory==='SEMI_FINISHED'?'warning':'success'" size="small" effect="plain">
                {{ row.inspectionCategoryCn }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="workOrderNo" label="工单" width="120" show-overflow-tooltip />
          <el-table-column prop="batchNo" label="批次" width="130" show-overflow-tooltip />
          <el-table-column prop="sampleQuantity" label="送检" width="60" align="center" />
          <el-table-column label="不良" width="60" align="center">
            <template #default="{ row }">
              <span :style="row.unqualifiedQuantity>0?'color:#f56c6c;font-weight:700':''">{{ row.unqualifiedQuantity||0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="88">
            <template #default="{ row }"><StatusBadge :status="row.inspectionStatusCn" /></template>
          </el-table-column>
          <el-table-column prop="inspectedAt" label="检验时间" width="148" />
        </el-table>
      </div>

      <div class="qc-split-layout__side">
        <template v-if="selected">
        <div class="qc-section">
          <div class="qc-section__title">追溯链路</div>
        <el-timeline>
          <el-timeline-item :type="tlType(selected.inspectionStatus)" :timestamp="selected.createdAt" placement="top">
            <div class="tl-block">
              <div class="tl-block__hd">质检单创建
                <el-tag size="small" :type="selected.inspectionCategory==='SEMI_FINISHED'?'warning':'success'" effect="plain" style="margin-left:6px">{{ selected.inspectionCategoryCn }}</el-tag>
              </div>
              <el-descriptions :column="2" size="small">
                <el-descriptions-item label="质检单号">{{ selected.inspectionNo }}</el-descriptions-item>
                <el-descriptions-item label="检验类型">{{ selected.inspectionTypeCn }}</el-descriptions-item>
                <el-descriptions-item label="工单">{{ selected.workOrderNo||'-' }}</el-descriptions-item>
                <el-descriptions-item label="物料">{{ selected.materialName||'-' }}</el-descriptions-item>
                <el-descriptions-item label="批次">{{ selected.batchNo }}</el-descriptions-item>
                <el-descriptions-item label="送检数">{{ selected.sampleQuantity }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-timeline-item>

          <el-timeline-item v-if="traceItems.length" type="primary" :timestamp="selected.inspectedAt||selected.updatedAt" placement="top">
            <div class="tl-block">
              <div class="tl-block__hd">检测项录入（共 {{ traceItems.length }} 项）</div>
              <div class="item-badges">
                <span class="ib passed">通过 {{ passedN }}</span>
                <span class="ib failed">不通过 {{ failedN }}</span>
                <span class="ib warning" v-if="warnN">警告 {{ warnN }}</span>
                <span class="ib pending" v-if="pendingN">待录入 {{ pendingN }}</span>
              </div>
              <el-table :data="traceItems" border size="small" style="margin-top:8px">
                <el-table-column prop="itemName" label="检测项" min-width="90" />
                <el-table-column prop="standardValue" label="标准值" width="90" show-overflow-tooltip />
                <el-table-column prop="measuredValue" label="实测值" width="80"><template #default="{row}">{{ row.measuredValue||'-' }}</template></el-table-column>
                <el-table-column label="结果" width="72"><template #default="{row}"><el-tag :type="itType(row.result)" size="small">{{ row.resultCn }}</el-tag></template></el-table-column>
              </el-table>
            </div>
          </el-timeline-item>

          <el-timeline-item v-if="selected.inspectionStatus!=='PENDING'" :type="tlType(selected.inspectionStatus)" :timestamp="selected.inspectedAt||selected.updatedAt" placement="top">
            <div class="tl-block">
              <div class="tl-block__hd">质检判定 <StatusBadge :status="selected.inspectionStatusCn" style="margin-left:6px" /></div>
              <el-descriptions :column="2" size="small">
                <el-descriptions-item label="合格数"><span style="color:#67c23a;font-weight:600">{{ selected.qualifiedQuantity }}</span></el-descriptions-item>
                <el-descriptions-item label="不良数"><span :style="selected.unqualifiedQuantity>0?'color:#f56c6c;font-weight:700':''">{{ selected.unqualifiedQuantity }}</span></el-descriptions-item>
                <el-descriptions-item label="检验员">{{ selected.inspectorName||'-' }}</el-descriptions-item>
                <el-descriptions-item label="检验时间">{{ selected.inspectedAt||'-' }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-timeline-item>

          <el-timeline-item v-for="nc in ncList" :key="nc.nonconformingId"
            :type="nc.handleStatus==='DONE'?'success':'warning'" :timestamp="nc.registeredAt" placement="top">
            <div class="tl-block">
              <div class="tl-block__hd">不良品登记
                <el-tag :type="sevType(nc.severity)" size="small" style="margin-left:6px">{{ nc.severityCn }}</el-tag>
              </div>
              <el-descriptions :column="2" size="small">
                <el-descriptions-item label="编号">{{ nc.nonconformingNo }}</el-descriptions-item>
                <el-descriptions-item label="缺陷类型">{{ nc.defectType }}</el-descriptions-item>
                <el-descriptions-item label="数量">{{ nc.quantity }}</el-descriptions-item>
                <el-descriptions-item label="处置方式">{{ nc.handleMethodCn }}</el-descriptions-item>
                <el-descriptions-item label="处置状态"><StatusBadge :status="nc.handleStatusCn" /></el-descriptions-item>
                <el-descriptions-item v-if="nc.handlerName" label="处理人">{{ nc.handlerName }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-timeline-item>

          <el-timeline-item v-if="selected.inspectionStatus==='CLOSED'" type="info" :timestamp="selected.updatedAt" placement="top">
            <div class="tl-block"><div class="tl-block__hd">质检单已关闭</div></div>
          </el-timeline-item>
        </el-timeline>
        </div>
        </template>
        <div v-else class="ruoyi-detail__empty" style="padding: 48px 16px; text-align: center;">
          点击左侧记录查看追溯链路
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import { fetchInspectionViews, fetchInspectionDetail, fetchInspectionItems } from '@/api/quality'

const route = useRoute()
const routeCategory = computed(() => route.meta?.category || '')

const allList = ref([])
const results = ref([])
const selected = ref(null)
const traceItems = ref([])
const ncList = ref([])
const keyword = ref('')
const searchedKw = ref('')
const searching = ref(false)
const searched = ref(false)

const TIPS = [
  { label: '质检单', val: 'QI-FP-001' },
  { label: '质检单', val: 'QI-SEMI-001' },
  { label: '批次',   val: 'BATCH-PANEL-202607' }
]

const passedN  = computed(() => traceItems.value.filter(i=>i.result==='PASSED').length)
const failedN  = computed(() => traceItems.value.filter(i=>i.result==='FAILED').length)
const warnN    = computed(() => traceItems.value.filter(i=>i.result==='WARNING').length)
const pendingN = computed(() => traceItems.value.filter(i=>i.result==='PENDING').length)

function tlType(s) { return {PASSED:'success',FAILED:'danger',CLOSED:'info',RECHECK_REQUIRED:'warning'}[s]||'primary' }
function itType(r) { return {PASSED:'success',FAILED:'danger',WARNING:'warning',PENDING:'info'}[r]||'info' }
function sevType(s){ return {MINOR:'',GENERAL:'warning',MAJOR:'danger',CRITICAL:'danger'}[s]||'' }

async function loadAll() {
  const res = await fetchInspectionViews().catch(()=>null)
  if (res) {
    const all = res.data ?? res
    // 按路由品类约束预过滤，追溯范围跟随侧边栏
    allList.value = routeCategory.value
      ? all.filter(r => r.inspectionCategory === routeCategory.value)
      : all
  }
}

async function doSearch() {
  if (!keyword.value.trim()) return
  searching.value = true
  searchedKw.value = keyword.value.trim()
  searched.value = true
  selected.value = null; traceItems.value = []; ncList.value = []
  const kw = searchedKw.value.toLowerCase()
  results.value = allList.value.filter(r=>
    (r.inspectionNo||'').toLowerCase().includes(kw)||
    (r.workOrderNo||'').toLowerCase().includes(kw)||
    (r.batchNo||'').toLowerCase().includes(kw)||
    (r.materialName||'').toLowerCase().includes(kw)
  )
  searching.value = false
}

async function onSelect(row) {
  selected.value = row; traceItems.value = []; ncList.value = []
  const [d,it] = await Promise.all([
    fetchInspectionDetail(row.inspectionId).catch(()=>null),
    fetchInspectionItems(row.inspectionId).catch(()=>null)
  ])
  if (d) ncList.value = (d.data??d).nonconformingList || []
  if (it) traceItems.value = it.data ?? it
}

function reset() {
  keyword.value=''; searched.value=false; results.value=[]
  selected.value=null; traceItems.value=[]; ncList.value=[]
}

onMounted(loadAll)

watch(routeCategory, () => {
  reset()
  loadAll()
})
</script>

<style scoped>
.trace-empty {
  padding: 48px 16px;
  border-top: 1px solid #ebeef5;
}

.trace-tip-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
  justify-content: center;
}

.trace-tip-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 20px;
  padding: 4px 12px;
  cursor: pointer;
}

.trace-tip-item:hover {
  border-color: var(--layout-accent);
}

.tip-tag { font-size: 11px; color: #909399; }
.tip-val { font-size: 12px; color: #303133; font-weight: 500; }

.trace-split {
  border-top: 1px solid #ebeef5;
}

.tl-block {
  padding: 8px 0;
}

.tl-block__hd {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.item-badges { display: flex; gap: 8px; flex-wrap: wrap; }
.ib { font-size: 11px; padding: 2px 8px; border-radius: 10px; }
.ib.passed  { background: #f0fdf4; color: #67c23a; }
.ib.failed  { background: #fff1f0; color: #f56c6c; }
.ib.warning { background: #fffbe6; color: #e6a23c; }
.ib.pending { background: #f5f5f5; color: #909399; }
</style>