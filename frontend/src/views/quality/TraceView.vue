<template>
  <div class="qc-page">
    <div class="trace-header">
      <div class="trace-header__title">质量追溯</div>
      <div class="trace-header__search">
        <el-input v-model="keyword" placeholder="质检单号 / 工单号 / 批次号 / 物料名" clearable style="width:300px" @keyup.enter="doSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" :loading="searching" @click="doSearch">追溯</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
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

    <div v-else class="trace-body">
      <div class="result-list">
        <div v-for="item in results" :key="item.inspectionId"
          class="result-card" :class="{active:selected?.inspectionId===item.inspectionId}"
          @click="onSelect(item)">
          <div class="rc-header">
            <span class="rc-no">{{ item.inspectionNo }}</span>
            <el-tag :type="item.inspectionCategory==='SEMI_FINISHED'?'warning':'success'" size="small" effect="plain">{{ item.inspectionCategoryCn }}</el-tag>
            <StatusBadge :status="item.inspectionStatusCn" />
          </div>
          <div class="rc-meta"><span>工单：{{ item.workOrderNo||'-' }}</span><span>批次：{{ item.batchNo||'-' }}</span></div>
          <div class="rc-meta"><span>送检 {{ item.sampleQuantity }}</span><span :style="item.unqualifiedQuantity>0?'color:#f56c6c;font-weight:700':''">不良 {{ item.unqualifiedQuantity||0 }}</span><span>{{ item.inspectedAt||'-' }}</span></div>
        </div>
      </div>

      <div class="trace-detail" v-if="selected">
        <div class="tl-title">追溯链路</div>
        <el-timeline>
          <el-timeline-item :type="tlType(selected.inspectionStatus)" :timestamp="selected.createdAt" placement="top">
            <div class="tl-card">
              <div class="tl-card__hd">质检单创建
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
            <div class="tl-card">
              <div class="tl-card__hd">检测项录入（共 {{ traceItems.length }} 项）</div>
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
            <div class="tl-card">
              <div class="tl-card__hd">质检判定 <StatusBadge :status="selected.inspectionStatusCn" style="margin-left:6px" /></div>
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
            <div class="tl-card">
              <div class="tl-card__hd">不良品登记
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
            <div class="tl-card"><div class="tl-card__hd">质检单已关闭</div></div>
          </el-timeline-item>
        </el-timeline>
      </div>
      <div class="trace-detail empty" v-else><el-empty description="点击左侧记录查看追溯链路" /></div>
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
.qc-page { display:flex; flex-direction:column; height:100%; padding:10px; gap:10px; background:#f5f6fa; }
.trace-header { background:#fff; border-radius:6px; padding:14px 16px; display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:10px; }
.trace-header__title { font-size:15px; font-weight:700; color:#303133; }
.trace-header__search { display:flex; gap:8px; align-items:center; }
.trace-empty { flex:1; display:flex; align-items:center; justify-content:center; background:#fff; border-radius:6px; }
.trace-tip-list { display:flex; gap:8px; flex-wrap:wrap; margin-top:8px; justify-content:center; }
.trace-tip-item { display:flex; align-items:center; gap:6px; background:#f5f7fa; border:1px solid #e4e7ed; border-radius:20px; padding:4px 12px; cursor:pointer; transition:border-color .15s; }
.trace-tip-item:hover { border-color:#409eff; }
.tip-tag { font-size:11px; color:#909399; }
.tip-val { font-size:12px; color:#303133; font-weight:600; }
.trace-body { display:flex; gap:10px; flex:1; min-height:0; }
.result-list { width:290px; flex-shrink:0; display:flex; flex-direction:column; gap:8px; overflow-y:auto; }
.result-card { background:#fff; border:2px solid transparent; border-radius:6px; padding:10px 12px; cursor:pointer; transition:border-color .15s; }
.result-card:hover { border-color:#409eff; }
.result-card.active { border-color:#409eff; background:#ecf5ff; }
.rc-header { display:flex; align-items:center; gap:6px; margin-bottom:5px; }
.rc-no { font-weight:600; font-size:13px; color:#303133; flex:1; }
.rc-meta { font-size:12px; color:#909399; display:flex; gap:10px; flex-wrap:wrap; margin-top:3px; }
.trace-detail { flex:1; background:#fff; border-radius:6px; padding:16px; overflow-y:auto; }
.trace-detail.empty { display:flex; align-items:center; justify-content:center; }
.tl-title { font-size:14px; font-weight:600; color:#303133; margin-bottom:16px; padding-bottom:10px; border-bottom:1px solid #f0f0f0; }
.tl-card { background:#fafafa; border:1px solid #e4e7ed; border-radius:6px; padding:10px 12px; }
.tl-card__hd { font-size:13px; font-weight:600; color:#303133; margin-bottom:8px; display:flex; align-items:center; }
.item-badges { display:flex; gap:8px; flex-wrap:wrap; }
.ib { font-size:11px; padding:2px 8px; border-radius:10px; }
.ib.passed  { background:#f0fdf4; color:#67c23a; }
.ib.failed  { background:#fff1f0; color:#f56c6c; }
.ib.warning { background:#fffbe6; color:#e6a23c; }
.ib.pending { background:#f5f5f5; color:#909399; }
</style>