<template>
  <div class="ruoyi-page progress-report-page">
    <div class="page-breadcrumb">
      <span>报表中心</span>
      <el-icon><ArrowRight /></el-icon>
      <span>生产报表</span>
      <el-icon><ArrowRight /></el-icon>
      <strong>生产制令单进度表</strong>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="filters" inline label-width="88px" class="filter-form">
        <el-form-item label="单据编号">
          <el-input v-model="filters.docNo" placeholder="制令单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="物料编号">
          <el-input v-model="filters.materialCode" placeholder="物料/订单编号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input v-model="filters.materialName" placeholder="产品型号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="生产状态">
          <el-select v-model="filters.status" placeholder="请选择" clearable style="width: 140px">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="生产交期">
          <el-date-picker
            v-model="filters.deliveryDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" :icon="Download" :disabled="!filteredRows.length" @click="exportExcel">
            导出 Excel
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="ruoyi-toolbar">
        <span class="ruoyi-toolbar__title">制令单进度汇总</span>
        <span class="ruoyi-toolbar__meta">共 {{ filteredRows.length }} 条 · 操作员 {{ operatorName }}（{{ username || '未识别账号' }}）</span>
      </div>

      <el-alert
        v-if="!sourceRows.length"
        type="info"
        :closable="false"
        show-icon
        class="empty-tip"
        title="暂无制令单进度数据"
      >
        <template #default>
          <p>本报表只显示<strong>已派工给当前账号</strong>的制令单。请按以下步骤产生数据：</p>
          <ol class="empty-tip__steps">
            <li>计划员创建并发布生产计划，提交生产主管</li>
            <li>生产主管在「工单派工」或「智能派工」中生成工单并派给您（{{ username || '当前账号' }}）</li>
            <li>您在「我的派工」中能看到任务后，本报表会自动出现对应制令单</li>
          </ol>
          <p v-if="mes.dispatches.length">系统共有 {{ mes.dispatches.length }} 条派工，其中分配给您的 {{ myDispatchCount }} 条。</p>
        </template>
      </el-alert>

      <el-table
        :data="pagedRows"
        border
        stripe
        highlight-current-row
        row-key="workOrderNo"
        @current-change="onSelectRow"
      >
        <el-table-column type="index" label="序号" width="60" align="center" :index="indexMethod" />
        <el-table-column prop="deliveryDate" label="生产交期" width="110" />
        <el-table-column prop="workOrderNo" label="制令单号" width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="生产状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialCode" label="物料编号" width="130" show-overflow-tooltip />
        <el-table-column prop="materialName" label="物料名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="startTime" label="开工时间" width="155" />
        <el-table-column prop="planQty" label="计划量" width="80" align="right" />
        <el-table-column prop="wipQty" label="在制量" width="80" align="right" />
        <el-table-column prop="completedQty" label="完成量" width="80" align="right" />
        <el-table-column prop="finishTime" label="完成时间" width="155" />
        <el-table-column prop="creator" label="制单员" width="90" />
        <el-table-column prop="createdAt" label="制单时间" width="155" />
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :total="filteredRows.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <el-card shadow="never" class="table-card detail-card">
      <div class="ruoyi-toolbar">
        <span class="ruoyi-toolbar__title">工序进度明细</span>
        <span class="ruoyi-toolbar__meta">
          {{ selectedRow ? `制令单 ${selectedRow.workOrderNo}` : '请在上方选择制令单' }}
          · {{ detailRows.length }} 条工序
        </span>
      </div>

      <el-table :data="detailRows" border stripe empty-text="请选择上方制令单查看工序明细">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="batchNo" label="批次" width="130" />
        <el-table-column prop="status" label="生产状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="processStep" label="工序名称" width="120" />
        <el-table-column prop="processGroup" label="工序组别" width="110" />
        <el-table-column prop="startTime" label="开工时间" width="155" />
        <el-table-column prop="planQty" label="计划量" width="80" align="right" />
        <el-table-column prop="completedQty" label="完成量" width="80" align="right" />
        <el-table-column prop="progress" label="进度" width="120" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :stroke-width="8" :status="row.progress >= 100 ? 'success' : ''" />
          </template>
        </el-table-column>
        <el-table-column prop="executor" label="执行者" width="100" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="DISPATCH_REPORTABLE.includes(row.status) && row.isMine"
              link
              type="primary"
              @click="$router.push('/production/report')"
            >
              报工
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ArrowRight, Download, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { DISPATCH_REPORTABLE } from '@/mock/constants'
import { useOperatorIdentity } from '@/composables/useOperatorIdentity'
import { exportExcelSheets } from '@/utils/excelExport'
import { finishedGoodsQty, isProductionStep, workshopForStep } from '@/utils/productionProgress'

const mes = useMesStore()
const { operatorUsername, operatorDisplayName } = useOperatorIdentity()

const operatorName = operatorDisplayName
const username = operatorUsername

const filters = reactive({
  docNo: '',
  materialCode: '',
  materialName: '',
  status: '',
  deliveryDate: ''
})

const page = reactive({ current: 1, size: 20 })
const selectedRow = ref(null)

const planById = computed(() => {
  const map = new Map()
  mes.plans.forEach((p) => map.set(p.id, p))
  return map
})

const orderById = computed(() => {
  const map = new Map()
  mes.orders.forEach((o) => map.set(o.id, o))
  return map
})

/** 操作员相关工单（有本人派工任务的制令单） */
const sourceRows = computed(() => {
  const myDispatches = mes.myDispatches(username.value)
  const woIds = new Set(myDispatches.map((d) => d.workOrderId || d.workOrderNo))
  const rows = []

  for (const wo of mes.workOrders) {
    if (!woIds.has(wo.id)) continue
    const plan = planById.value.get(wo.planId)
    const order = orderById.value.get(wo.orderId)
    const myTasks = myDispatches.filter((d) => (d.workOrderId || d.workOrderNo) === wo.id)
    const startTime = myTasks.map((d) => d.planStart || d.createdAt).filter(Boolean).sort()[0] || wo.updatedAt || wo.createdAt
    const completedQty = finishedGoodsQty(mes.dispatches, wo.id)
    const planQty = wo.quantity ?? 0
    const wipQty = ['生产中', '已派工', '已下达'].includes(wo.status)
      ? Math.max(0, planQty - completedQty)
      : 0

    rows.push({
      workOrderNo: wo.id,
      deliveryDate: plan?.planEnd || order?.deliveryDate || '-',
      status: wo.status,
      materialCode: wo.orderNo || wo.orderId || plan?.orderNo || '-',
      materialName: wo.productModel || '-',
      startTime: startTime || '-',
      planQty,
      wipQty,
      completedQty,
      finishTime: wo.status === '已完成' ? (wo.updatedAt || '-') : '-',
      creator: wo.manager || plan?.planner || '-',
      createdAt: wo.createdAt || '-',
      orderId: wo.orderId,
      planId: wo.planId
    })
  }

  return rows.sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
})

const statusOptions = computed(() => [...new Set(sourceRows.value.map((r) => r.status).filter(Boolean))])

const myDispatchCount = computed(() => mes.myDispatches(username.value).length)

const filteredRows = computed(() => {
  let list = sourceRows.value
  if (filters.docNo) {
    const q = filters.docNo.trim().toLowerCase()
    list = list.filter((r) => r.workOrderNo.toLowerCase().includes(q))
  }
  if (filters.materialCode) {
    const q = filters.materialCode.trim().toLowerCase()
    list = list.filter((r) => String(r.materialCode).toLowerCase().includes(q))
  }
  if (filters.materialName) {
    const q = filters.materialName.trim().toLowerCase()
    list = list.filter((r) => String(r.materialName).toLowerCase().includes(q))
  }
  if (filters.status) {
    list = list.filter((r) => r.status === filters.status)
  }
  if (filters.deliveryDate) {
    list = list.filter((r) => r.deliveryDate === filters.deliveryDate)
  }
  return list
})

const pagedRows = computed(() => {
  const start = (page.current - 1) * page.size
  return filteredRows.value.slice(start, start + page.size)
})

const detailRows = computed(() => {
  if (!selectedRow.value) return []
  const woNo = selectedRow.value.workOrderNo
  return mes.myDispatches(username.value)
    .filter((d) => (d.workOrderId || d.workOrderNo) === woNo)
    .filter((d) => isProductionStep(d.processStep))
    .map((d, idx) => {
      const planQty = d.planQty || 0
      const completedQty = d.completedQty || 0
      const progress = planQty > 0 ? Math.min(100, Math.round((completedQty / planQty) * 100)) : 0
      return {
        batchNo: d.id || `BATCH-${idx + 1}`,
        status: d.status,
        processStep: d.processStep,
        processGroup: workshopForStep(d.processStep),
        startTime: d.planStart || d.createdAt || '-',
        planQty,
        completedQty,
        progress,
        executor: d.operatorName || d.operator || '-',
        isMine: d.operator === username.value
      }
    })
})

function indexMethod(index) {
  return (page.current - 1) * page.size + index + 1
}

function statusTagType(status) {
  const map = {
    '生产中': 'primary',
    '已派工': 'success',
    '已下达': 'info',
    '已完成': 'success',
    '草稿': 'info',
    '待派工': 'warning',
    '工艺确认': 'warning'
  }
  return map[status] || 'info'
}

function onSelectRow(row) {
  selectedRow.value = row || null
}

function handleSearch() {
  page.current = 1
  if (filteredRows.value.length && !filteredRows.value.some((r) => r.workOrderNo === selectedRow.value?.workOrderNo)) {
    selectedRow.value = filteredRows.value[0]
  }
}

function handleReset() {
  filters.docNo = ''
  filters.materialCode = ''
  filters.materialName = ''
  filters.status = ''
  filters.deliveryDate = ''
  page.current = 1
  selectedRow.value = filteredRows.value[0] || null
}

function exportExcel() {
  const mainHeaders = [
    '序号', '生产交期', '制令单号', '生产状态', '物料编号', '物料名称',
    '开工时间', '计划量', '在制量', '完成量', '完成时间', '制单员', '制单时间'
  ]
  const mainRows = filteredRows.value.map((r, i) => [
    i + 1, r.deliveryDate, r.workOrderNo, r.status, r.materialCode, r.materialName,
    r.startTime, r.planQty, r.wipQty, r.completedQty, r.finishTime, r.creator, r.createdAt
  ])

  const detailHeaders = [
    '制令单号', '批次', '生产状态', '工序名称', '工序组别', '开工时间',
    '计划量', '完成量', '进度(%)', '执行者'
  ]
  const detailExportRows = []
  filteredRows.value.forEach((main) => {
    const tasks = mes.myDispatches(username.value)
      .filter((d) => (d.workOrderId || d.workOrderNo) === main.workOrderNo)
      .filter((d) => isProductionStep(d.processStep))
    tasks.forEach((d, idx) => {
      const planQty = d.planQty || 0
      const completedQty = d.completedQty || 0
      const progress = planQty > 0 ? Math.min(100, Math.round((completedQty / planQty) * 100)) : 0
      detailExportRows.push([
        main.workOrderNo,
        d.id || `BATCH-${idx + 1}`,
        d.status,
        d.processStep,
        workshopForStep(d.processStep),
        d.planStart || d.createdAt || '-',
        planQty,
        completedQty,
        progress,
        d.operatorName || d.operator || '-'
      ])
    })
  })

  const date = new Date().toISOString().slice(0, 10)
  exportExcelSheets([
    { name: '制令单进度', headers: mainHeaders, rows: mainRows },
    { name: '工序明细', headers: detailHeaders, rows: detailExportRows }
  ], `生产制令单进度表_${operatorName.value}_${date}.xls`)

  ElMessage.success(`已导出 ${mainRows.length} 条制令单、${detailExportRows.length} 条工序明细`)
}

watch(filteredRows, (list) => {
  if (!list.length) {
    selectedRow.value = null
    return
  }
  if (!selectedRow.value || !list.some((r) => r.workOrderNo === selectedRow.value.workOrderNo)) {
    selectedRow.value = list[0]
  }
}, { immediate: true })

onMounted(async () => {
  try {
    await mes.hydrateFromApi()
  } catch {
    /* ignore */
  }
})
</script>

<style scoped>
.progress-report-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  padding: 4px 2px;
}
.page-breadcrumb strong {
  color: #303133;
}
.filter-card :deep(.el-card__body) {
  padding-bottom: 4px;
}
.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 0;
}
.table-card :deep(.el-card__body) {
  padding-top: 12px;
}
.ruoyi-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.ruoyi-toolbar__title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.ruoyi-toolbar__meta {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
.detail-card {
  margin-bottom: 8px;
}
.empty-tip {
  margin-bottom: 12px;
}
.empty-tip__steps {
  margin: 8px 0 8px 18px;
  padding: 0;
  line-height: 1.8;
  font-size: 13px;
}
.empty-tip p {
  margin: 4px 0;
  font-size: 13px;
}
</style>
