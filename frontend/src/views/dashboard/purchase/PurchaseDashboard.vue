<template>
  <div class="pdb">
    <!-- ① 顶部工作概览 -->
    <div class="pdb-hero">
      <div class="pdb-hero__left">
        <div class="pdb-hero__title">采购工作台</div>
        <div class="pdb-hero__sub">
          <span v-if="stats.pendingRequirements > 0" class="hero-badge hero-badge--warn">待采购 {{ stats.pendingRequirements }}</span>
          <span v-if="stats.overdueOrders > 0" class="hero-badge hero-badge--danger">逾期未到 {{ stats.overdueOrders }}</span>
          <span v-if="aiPending > 0" class="hero-badge hero-badge--info">AI单据待确认 {{ aiPending }}</span>
          <span v-if="stats.pendingRequirements === 0 && stats.overdueOrders === 0" class="hero-badge hero-badge--ok">今日无紧急事项</span>
        </div>
      </div>
      <div class="pdb-hero__actions">
        <router-link to="/purchase/ai-document" class="hero-btn hero-btn--primary">AI 单据录入</router-link>
        <router-link to="/purchase/demand" class="hero-btn">查看缺料需求</router-link>
        <router-link to="/purchase/order" class="hero-btn">采购订单</router-link>
      </div>
    </div>

    <!-- ② KPI 卡片 -->
    <div class="pdb-kpi-row">
      <div v-for="kpi in kpiCards" :key="kpi.label" class="kpi-card" :class="'kpi-card--' + kpi.color">
        <div class="kpi-card__icon">{{ kpi.icon }}</div>
        <div class="kpi-card__body">
          <div class="kpi-card__value">{{ kpi.value }}</div>
          <div class="kpi-card__label">{{ kpi.label }}</div>
          <div class="kpi-card__hint" :class="'hint--' + kpi.hintColor">{{ kpi.hint }}</div>
        </div>
      </div>
    </div>

    <!-- ③ 核心任务区 + Top物料 + 订单摘要 -->
    <div class="pdb-main-row">
      <!-- 左：待办优先级列表 -->
      <div class="pane pane--task">
        <div class="pane-header">
          <span class="pane-title">待办事项</span>
          <el-tag size="small" type="warning">{{ todos.length }}</el-tag>
        </div>
        <div class="task-list">
          <div v-if="todos.length === 0" class="task-empty">暂无待办事项</div>
          <div
            v-for="t in todos"
            :key="t.id"
            class="task-item"
            :class="'task-item--' + t.level"
            @click="$router.push(t.path)"
          >
            <span class="task-item__dot"></span>
            <div class="task-item__body">
              <div class="task-item__title">{{ t.title }}</div>
              <div class="task-item__meta">{{ t.meta }}</div>
            </div>
            <el-tag :type="t.level === 'danger' ? 'danger' : t.level === 'warn' ? 'warning' : 'info'" size="small">{{ t.tag }}</el-tag>
          </div>
        </div>
      </div>

      <!-- 中：缺料 Top 物料排行 -->
      <div class="pane pane--chart">
        <div class="pane-header">
          <span class="pane-title">缺料 Top 物料</span>
          <router-link to="/purchase/demand" class="pane-link">查看全部</router-link>
        </div>
        <div v-if="topMaterials.length === 0" class="chart-empty">暂无缺料数据，请先执行缺料计算</div>
        <div v-else class="rank-list">
          <div v-for="(item, idx) in topMaterials" :key="item.materialId || idx" class="rank-item">
            <span class="rank-item__no" :class="idx < 3 ? 'rank-item__no--top' : ''">{{ idx + 1 }}</span>
            <div class="rank-item__info">
              <div class="rank-item__name">{{ item.materialName || item.materialCode || '未知物料' }}</div>
              <div class="rank-item__bar-wrap">
                <div class="rank-item__bar" :style="{ width: barWidth(item.shortageQuantity) + '%' }" :class="idx === 0 ? 'bar--red' : idx === 1 ? 'bar--orange' : 'bar--blue'"></div>
              </div>
            </div>
            <span class="rank-item__val">{{ item.shortageQuantity }}</span>
            <el-tag :type="item.priority === 1 ? 'danger' : item.priority === 2 ? 'warning' : 'info'" size="small">{{ item.priority === 1 ? '紧急' : item.priority === 2 ? '高' : '普通' }}</el-tag>
          </div>
        </div>
      </div>

      <!-- 右：订单状态摘要 + 到货风险 -->
      <div class="pane pane--summary">
        <div class="pane-header"><span class="pane-title">订单状态摘要</span></div>
        <div class="summary-list">
          <div v-for="s in orderSummary" :key="s.label" class="summary-item">
            <span class="summary-item__label">{{ s.label }}</span>
            <div class="summary-item__bar-wrap">
              <div class="summary-item__bar" :style="{ width: s.pct + '%', background: s.color }"></div>
            </div>
            <span class="summary-item__val" :style="{ color: s.color }">{{ s.value }}</span>
          </div>
        </div>
        <div class="pane-header" style="margin-top:16px"><span class="pane-title">到货风险</span></div>
        <div v-if="overdueOrders.length === 0" class="chart-empty">无逾期订单</div>
        <div v-else class="risk-list">
          <div v-for="o in overdueOrders.slice(0, 5)" :key="o.purchaseOrderId" class="risk-item" @click="$router.push('/purchase/order')">
            <span class="risk-item__no">{{ o.purchaseOrderNo }}</span>
            <span class="risk-item__supplier">{{ o.supplierName || '—' }}</span>
            <el-tag type="danger" size="small">逾期</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- ④ 今日待办表格 -->
    <div class="pdb-table-section">
      <div class="pane-header">
        <span class="pane-title">今日重点任务</span>
        <span class="pane-link" style="cursor:pointer" @click="loadData">刷新</span>
      </div>
      <el-table :data="taskTable" border stripe size="small" style="width:100%">
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.typeColor" size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="单号 / 物料" min-width="160" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.priorityColor" size="small">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="110" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.statusColor" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(row.path)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="taskTable.length === 0" class="chart-empty" style="padding:24px 0">暂无重点任务</div>
    </div>

    <!-- ⑤ 快捷入口 -->
    <div class="pdb-shortcuts">
      <router-link v-for="s in shortcuts" :key="s.path" :to="s.path" class="sc-card">
        <span class="sc-card__icon">{{ s.icon }}</span>
        <span class="sc-card__label">{{ s.label }}</span>
        <span class="sc-card__arrow">›</span>
      </router-link>
    </div>
  </div>
</template>
<!-- SCRIPT_BLOCK -->

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { fetchWorkbenchList } from '@/api/business'

const orders = ref([])
const requirements = ref([])
const loading = ref(false)

const today = new Date().toISOString().slice(0, 10)

// ---- 接口数据暂时没有 AI 单据待确认字段，用 mock 占位 ----
// TODO: 对接 /purchase/ai/document/pending 接口后替换
const aiPending = ref(0)

const stats = computed(() => {
  const pending = requirements.value.filter(r => r.status === 'PENDING').length
  const totalO = orders.value.length
  const arrived = orders.value.filter(o => o.status === 'RECEIVED').length
  const overdue = orders.value.filter(o => {
    if (o.status === 'RECEIVED' || o.status === 'CANCELLED') return false
    return o.expectedArrivalDate && o.expectedArrivalDate < today
  }).length
  return {
    totalRequirements: requirements.value.length,
    pendingRequirements: pending,
    totalOrders: totalO,
    arrivedOrders: arrived,
    overdueOrders: overdue
  }
})

const kpiCards = computed(() => [
  {
    icon: '📋', label: '缺料需求', value: stats.value.totalRequirements, color: 'default',
    hint: stats.value.totalRequirements > 0 ? '需跟进处理' : '无缺料', hintColor: stats.value.totalRequirements > 0 ? 'warn' : 'ok'
  },
  {
    icon: '⏳', label: '待采购', value: stats.value.pendingRequirements, color: 'warn',
    hint: stats.value.pendingRequirements > 0 ? '待生成采购单' : '全部已处理', hintColor: stats.value.pendingRequirements > 0 ? 'warn' : 'ok'
  },
  {
    icon: '📦', label: '采购订单', value: stats.value.totalOrders, color: 'primary',
    hint: '本期订单总数', hintColor: 'neutral'
  },
  {
    icon: '✅', label: '已到货', value: stats.value.arrivedOrders, color: 'success',
    hint: stats.value.totalOrders > 0 ? Math.round(stats.value.arrivedOrders / stats.value.totalOrders * 100) + '% 完成率' : '—', hintColor: 'ok'
  },
  {
    icon: '🚨', label: '逾期未到', value: stats.value.overdueOrders, color: 'danger',
    hint: stats.value.overdueOrders > 0 ? '需立即跟催' : '无逾期', hintColor: stats.value.overdueOrders > 0 ? 'danger' : 'ok'
  }
])

const topMaterials = computed(() =>
  [...requirements.value]
    .sort((a, b) => Number(b.shortageQuantity) - Number(a.shortageQuantity))
    .slice(0, 8)
)

const maxShortage = computed(() =>
  topMaterials.value.reduce((m, r) => Math.max(m, Number(r.shortageQuantity) || 0), 1)
)

function barWidth(val) {
  return Math.max(4, Math.round((Number(val) || 0) / maxShortage.value * 100))
}

const overdueOrders = computed(() =>
  orders.value.filter(o => {
    if (o.status === 'RECEIVED' || o.status === 'CANCELLED') return false
    return o.expectedArrivalDate && o.expectedArrivalDate < today
  })
)

const orderSummary = computed(() => {
  const total = orders.value.length || 1
  const groups = [
    { label: '草稿', key: 'DRAFT', color: '#8c8c8c' },
    { label: '已审核', key: 'APPROVED', color: '#4096ff' },
    { label: '已到货', key: 'RECEIVED', color: '#52c41a' },
    { label: '逾期未到', key: '_overdue', color: '#f5222d' }
  ]
  return groups.map(g => {
    const count = g.key === '_overdue'
      ? overdueOrders.value.length
      : orders.value.filter(o => o.status === g.key).length
    return { ...g, value: count, pct: Math.round(count / total * 100) }
  })
})

// 待办列表 — 结合真实接口数据动态生成
const todos = computed(() => {
  const list = []
  requirements.value
    .filter(r => r.status === 'PENDING' && r.priority === 1)
    .slice(0, 3)
    .forEach(r => list.push({
      id: 'req_' + r.requirementId,
      level: 'danger',
      title: '紧急缺料：' + (r.materialName || r.materialCode),
      meta: '缺口 ' + r.shortageQuantity + ' 件',
      tag: '紧急',
      path: '/purchase/demand'
    }))
  overdueOrders.value.slice(0, 3).forEach(o => list.push({
    id: 'od_' + o.purchaseOrderId,
    level: 'warn',
    title: '逾期未到：' + o.purchaseOrderNo,
    meta: '期望到货 ' + (o.expectedArrivalDate || '—'),
    tag: '逾期',
    path: '/purchase/order'
  }))
  requirements.value
    .filter(r => r.status === 'PENDING' && r.priority === 2)
    .slice(0, 2)
    .forEach(r => list.push({
      id: 'req2_' + r.requirementId,
      level: 'info',
      title: '待采购：' + (r.materialName || r.materialCode),
      meta: '建议采购 ' + r.suggestedPurchaseQuantity + ' 件',
      tag: '高',
      path: '/purchase/demand'
    }))
  // mock: AI 单据待确认（TODO: 对接真实接口后替换）
  if (aiPending.value > 0) {
    list.push({ id: 'ai_pending', level: 'info', title: 'AI 单据待人工确认', meta: aiPending.value + ' 张待录入', tag: '待确认', path: '/purchase/ai-document' })
  }
  return list
})

// 今日重点任务表格 — 综合 todos 生成，方便后续直接替换成接口
const taskTable = computed(() => {
  const rows = []
  requirements.value.filter(r => r.status === 'PENDING').slice(0, 5).forEach(r => {
    rows.push({
      type: '缺料', typeColor: r.priority === 1 ? 'danger' : 'warning',
      title: (r.materialName || r.materialCode || '—') + (r.specification ? ' ' + r.specification : ''),
      priority: r.priority === 1 ? '紧急' : r.priority === 2 ? '高' : '普通',
      priorityColor: r.priority === 1 ? 'danger' : r.priority === 2 ? 'warning' : 'info',
      deadline: r.expectedArrivalDate || '—',
      status: '待采购', statusColor: 'warning',
      path: '/purchase/demand'
    })
  })
  overdueOrders.value.slice(0, 3).forEach(o => {
    rows.push({
      type: '到货', typeColor: 'danger',
      title: o.purchaseOrderNo + (o.supplierName ? ' / ' + o.supplierName : ''),
      priority: '紧急', priorityColor: 'danger',
      deadline: o.expectedArrivalDate || '—',
      status: '逾期', statusColor: 'danger',
      path: '/purchase/order'
    })
  })
  return rows
})

const shortcuts = [
  { icon: '🔍', label: '采购需求工作台', path: '/purchase/demand' },
  { icon: '📦', label: '采购订单', path: '/purchase/order' },
  { icon: '🏢', label: '供应商管理', path: '/purchase/supplier' },
  { icon: '🤖', label: 'AI 单据录入', path: '/purchase/ai-document' }
]

async function loadData() {
  loading.value = true
  try {
    const [orderRes, reqRes] = await Promise.allSettled([
      request.get('/purchase/purchaseOrder/list'),
      fetchWorkbenchList({})
    ])
    orders.value = orderRes.status === 'fulfilled' ? (orderRes.value || []) : []
    requirements.value = reqRes.status === 'fulfilled' ? (reqRes.value || []) : []
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.pdb { padding: 16px 20px; background: #f0f2f5; min-height: 100%; box-sizing: border-box; }

/* ---- 顶部 Hero ---- */
.pdb-hero { display: flex; align-items: center; justify-content: space-between; background: #fff; border-radius: 8px; padding: 16px 20px; margin-bottom: 14px; box-shadow: 0 1px 4px rgba(0,0,0,.06); flex-wrap: wrap; gap: 10px; }
.pdb-hero__title { font-size: 20px; font-weight: 700; color: #001b3f; line-height: 1.3; }
.pdb-hero__sub { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 6px; }
.hero-badge { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.hero-badge--warn { background: #fff7e6; color: #d46b08; border: 1px solid #ffd591; }
.hero-badge--danger { background: #fff1f0; color: #cf1322; border: 1px solid #ffa39e; }
.hero-badge--info { background: #e6f4ff; color: #0958d9; border: 1px solid #91caff; }
.hero-badge--ok { background: #f6ffed; color: #389e0d; border: 1px solid #b7eb8f; }
.pdb-hero__actions { display: flex; gap: 8px; flex-wrap: wrap; }
.hero-btn { display: inline-flex; align-items: center; padding: 6px 16px; border-radius: 6px; font-size: 13px; text-decoration: none; border: 1px solid #d9d9d9; background: #fafafa; color: #001b3f; transition: all .15s; white-space: nowrap; }
.hero-btn:hover { border-color: #4096ff; color: #4096ff; background: #e6f4ff; }
.hero-btn--primary { background: #4096ff; color: #fff; border-color: #4096ff; }
.hero-btn--primary:hover { background: #1677ff; border-color: #1677ff; color: #fff; }

/* ---- KPI 卡片 ---- */
.pdb-kpi-row { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.kpi-card { flex: 1; min-width: 140px; background: #fff; border-radius: 8px; padding: 14px 16px; display: flex; align-items: flex-start; gap: 12px; box-shadow: 0 1px 4px rgba(0,0,0,.06); border-left: 3px solid #e8ecf0; }
.kpi-card--warn { border-left-color: #faad14; }
.kpi-card--primary { border-left-color: #4096ff; }
.kpi-card--success { border-left-color: #52c41a; }
.kpi-card--danger { border-left-color: #f5222d; }
.kpi-card__icon { font-size: 22px; line-height: 1; margin-top: 2px; }
.kpi-card__value { font-size: 26px; font-weight: 700; color: #001b3f; line-height: 1.2; }
.kpi-card__label { font-size: 12px; color: #8c8c8c; margin-top: 2px; }
.kpi-card__hint { font-size: 11px; margin-top: 4px; }
.hint--warn { color: #d46b08; }
.hint--danger { color: #cf1322; }
.hint--ok { color: #389e0d; }
.hint--neutral { color: #8c8c8c; }

/* ---- 核心任务三栏 ---- */
.pdb-main-row { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.pane { background: #fff; border-radius: 8px; padding: 14px 16px; box-shadow: 0 1px 4px rgba(0,0,0,.06); overflow: hidden; }
.pane--task { flex: 1; min-width: 220px; }
.pane--chart { flex: 1.6; min-width: 260px; }
.pane--summary { flex: 1; min-width: 200px; }
.pane-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.pane-title { font-size: 13px; font-weight: 600; color: #001b3f; }
.pane-link { font-size: 12px; color: #4096ff; text-decoration: none; cursor: pointer; }
.pane-link:hover { text-decoration: underline; }

/* 待办列表 */
.task-list { display: flex; flex-direction: column; gap: 6px; }
.task-empty { font-size: 13px; color: #bfbfbf; text-align: center; padding: 20px 0; }
.task-item { display: flex; align-items: flex-start; gap: 8px; padding: 8px 10px; border-radius: 6px; background: #fafafa; border: 1px solid #f0f0f0; cursor: pointer; transition: background .15s; }
.task-item:hover { background: #f0f7ff; border-color: #91caff; }
.task-item--danger .task-item__dot { background: #f5222d; }
.task-item--warn .task-item__dot { background: #faad14; }
.task-item--info .task-item__dot { background: #4096ff; }
.task-item__dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 4px; flex-shrink: 0; }
.task-item__body { flex: 1; min-width: 0; }
.task-item__title { font-size: 13px; color: #001b3f; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.task-item__meta { font-size: 11px; color: #8c8c8c; margin-top: 2px; }

/* 排行榜 */
.rank-list { display: flex; flex-direction: column; gap: 8px; }
.chart-empty { font-size: 13px; color: #bfbfbf; text-align: center; padding: 20px 0; }
.rank-item { display: flex; align-items: center; gap: 8px; }
.rank-item__no { width: 20px; font-size: 12px; font-weight: 700; color: #8c8c8c; text-align: center; flex-shrink: 0; }
.rank-item__no--top { color: #f5222d; }
.rank-item__info { flex: 1; min-width: 0; }
.rank-item__name { font-size: 12px; color: #001b3f; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 3px; }
.rank-item__bar-wrap { height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; }
.rank-item__bar { height: 100%; border-radius: 3px; transition: width .4s; }
.bar--red { background: #f5222d; }
.bar--orange { background: #faad14; }
.bar--blue { background: #4096ff; }
.rank-item__val { font-size: 12px; font-weight: 600; color: #001b3f; width: 40px; text-align: right; flex-shrink: 0; }

/* 摘要条 */
.summary-list { display: flex; flex-direction: column; gap: 8px; }
.summary-item { display: flex; align-items: center; gap: 8px; }
.summary-item__label { font-size: 12px; color: #595959; width: 56px; flex-shrink: 0; }
.summary-item__bar-wrap { flex: 1; height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; }
.summary-item__bar { height: 100%; border-radius: 3px; transition: width .4s; min-width: 2px; }
.summary-item__val { font-size: 12px; font-weight: 600; width: 24px; text-align: right; flex-shrink: 0; }

/* 风险列表 */
.risk-list { display: flex; flex-direction: column; gap: 5px; }
.risk-item { display: flex; align-items: center; gap: 8px; padding: 6px 8px; background: #fff1f0; border-radius: 4px; cursor: pointer; }
.risk-item:hover { background: #ffd6d6; }
.risk-item__no { font-size: 12px; font-weight: 600; color: #001b3f; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.risk-item__supplier { font-size: 11px; color: #8c8c8c; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* ---- 今日任务表格 ---- */
.pdb-table-section { background: #fff; border-radius: 8px; padding: 14px 16px; margin-bottom: 14px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }

/* ---- 快捷入口 ---- */
.pdb-shortcuts { display: flex; gap: 10px; flex-wrap: wrap; }
.sc-card { flex: 1; min-width: 140px; display: flex; align-items: center; gap: 10px; background: #fff; border-radius: 8px; padding: 12px 16px; text-decoration: none; color: #001b3f; box-shadow: 0 1px 4px rgba(0,0,0,.06); border: 1px solid transparent; transition: all .15s; }
.sc-card:hover { border-color: #4096ff; background: #e6f4ff; color: #0958d9; }
.sc-card__icon { font-size: 20px; flex-shrink: 0; }
.sc-card__label { font-size: 13px; font-weight: 500; flex: 1; }
.sc-card__arrow { font-size: 16px; color: #bfbfbf; }
.sc-card:hover .sc-card__arrow { color: #4096ff; }
</style>
