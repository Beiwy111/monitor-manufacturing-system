<template>
  <section id="live-data" class="live-section">
    <div class="live-inner">
      <!-- 标题区 -->
      <div class="live-header">
        <div class="live-header__left">
          <div class="live-kicker">REAL-TIME DATA</div>
          <h2 class="live-title">系统实时运营数据</h2>
          <p class="live-subtitle">以下数据直接来自生产数据库，每次刷新页面自动更新</p>
        </div>
        <div class="live-header__right">
          <div class="live-pulse" :class="{loading: loading}">
            <span class="pulse-dot"></span>
            <span class="pulse-label">{{ loading ? "数据加载中..." : "数据实时" }}</span>
          </div>
          <div class="live-updated" v-if="updatedAt">更新于 {{ updatedAt }}</div>
        </div>
      </div>

      <!-- 核心 KPI 6 格 -->
      <div class="kpi-grid">
        <div v-for="kpi in kpis" :key="kpi.key"
          class="kpi-card"
          :class="[kpi.theme, {'kpi-card--warn': kpi.warn, 'kpi-card--danger': kpi.danger}]"
          @click="goPage(kpi.path)"
        >
          <div class="kpi-card__icon">
            <component :is="kpi.icon" />
          </div>
          <div class="kpi-card__body">
            <div class="kpi-card__num">
              <span class="kpi-num-val" :data-target="kpi.value">{{ displayNums[kpi.key] ?? 0 }}</span>
              <span v-if="kpi.unit" class="kpi-num-unit">{{ kpi.unit }}</span>
            </div>
            <div class="kpi-card__label">{{ kpi.label }}</div>
            <div class="kpi-card__sub" v-if="kpi.sub">{{ kpi.sub }}</div>
          </div>
          <div v-if="kpi.warn||kpi.danger" class="kpi-card__badge">
            <span>需处理</span>
          </div>
          <div class="kpi-card__arrow">→</div>
        </div>
      </div>

      <!-- 三列详情：订单趋势 + 生产状态 + 质检概况 -->
      <div class="detail-grid">
        <!-- 订单动态 -->
        <div class="detail-card">
          <div class="detail-card__hd">
            <span class="detail-card__title">最新客户订单</span>
            <a class="detail-card__link" @click="goLogin">查看全部 →</a>
          </div>
          <div v-if="orderLoading" class="detail-loading"><div class="skeleton" v-for="i in 4" :key="i"></div></div>
          <ul v-else class="order-list">
            <li v-for="o in recentOrders" :key="o.orderId||o.id" class="order-item">
              <div class="order-item__no">{{ o.orderNo || o.id }}</div>
              <div class="order-item__meta">
                <span>{{ o.customerName || o.customer || '-' }}</span>
                <span class="order-item__qty">{{ o.totalQuantity || o.quantity || 0 }} 台</span>
              </div>
              <span class="status-tag" :class="orderTagClass(o.orderStatus || o.status)">
                {{ orderStatusCn(o.orderStatus || o.status) }}
              </span>
            </li>
            <li v-if="!recentOrders.length" class="detail-empty">暂无订单数据</li>
          </ul>
        </div>

        <!-- 生产工单 -->
        <div class="detail-card">
          <div class="detail-card__hd">
            <span class="detail-card__title">在制工单状态</span>
            <a class="detail-card__link" @click="goLogin">查看全部 →</a>
          </div>
          <div v-if="woLoading" class="detail-loading"><div class="skeleton" v-for="i in 4" :key="i"></div></div>
          <div v-else>
            <!-- 进度汇总 -->
            <div class="wo-summary">
              <div class="wo-sum-item" v-for="s in woSummary" :key="s.label">
                <div class="wo-sum-num" :style="{color: s.color}">{{ s.count }}</div>
                <div class="wo-sum-label">{{ s.label }}</div>
              </div>
            </div>
            <!-- 在制进度条 -->
            <div class="wo-progress-list">
              <div v-for="wo in activeWorkOrders" :key="wo.workOrderId||wo.id" class="wo-progress-item">
                <div class="wo-progress-item__hd">
                  <span class="wo-no">{{ wo.workOrderNo || wo.id }}</span>
                  <span class="wo-pct">{{ progressPct(wo) }}%</span>
                </div>
                <div class="wo-bar">
                  <div class="wo-bar__fill" :style="{width: progressPct(wo)+'%', background: woBarColor(wo)}"></div>
                </div>
                <div class="wo-progress-item__ft">
                  <span>{{ woName(wo) }}</span>
                  <span>{{ wo.completedQuantity || 0 }}/{{ wo.plannedQuantity || 0 }}</span>
                </div>
              </div>
              <div v-if="!activeWorkOrders.length" class="detail-empty">暂无在制工单</div>
            </div>
          </div>
        </div>

        <!-- 质检概况 -->
        <div class="detail-card">
          <div class="detail-card__hd">
            <span class="detail-card__title">质量检验概况</span>
            <a class="detail-card__link" @click="goLogin">质检台 →</a>
          </div>
          <div v-if="qcLoading" class="detail-loading"><div class="skeleton" v-for="i in 4" :key="i"></div></div>
          <div v-else>
            <!-- 合格率大数字 -->
            <div class="qc-yield-block">
              <div class="qc-yield-num">{{ yieldRate }}<span class="qc-yield-pct">%</span></div>
              <div class="qc-yield-label">综合合格率</div>
            </div>
            <!-- 状态分布 -->
            <div class="qc-status-row">
              <div v-for="s in qcStatusBars" :key="s.label" class="qc-status-item">
                <div class="qc-status-bar-wrap">
                  <div class="qc-status-bar" :style="{height: s.pct+'%', background: s.color}"></div>
                </div>
                <div class="qc-status-num" :style="{color: s.color}">{{ s.count }}</div>
                <div class="qc-status-label">{{ s.label }}</div>
              </div>
            </div>
            <!-- 待处理提示 -->
            <div v-if="qcKpi.pending>0||qcKpi.recheck>0" class="qc-alert">
              <span v-if="qcKpi.pending>0">⚡ {{ qcKpi.pending }} 单待检</span>
              <span v-if="qcKpi.recheck>0" style="margin-left:12px">🔄 {{ qcKpi.recheck }} 单待复检</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部：库存预警 + 设备状态 -->
      <div class="bottom-grid">
        <!-- 库存预警 -->
        <div class="detail-card bottom-card">
          <div class="detail-card__hd">
            <span class="detail-card__title">库存预警</span>
            <a class="detail-card__link" @click="goLogin">库存管理 →</a>
          </div>
          <div v-if="invLoading" class="detail-loading"><div class="skeleton" v-for="i in 3" :key="i"></div></div>
          <div v-else>
            <div class="inv-summary">
              <div class="inv-sum-item">
                <div class="inv-sum-num">{{ inventorySummary.total }}</div>
                <div class="inv-sum-label">物料种类</div>
              </div>
              <div class="inv-sum-item warn">
                <div class="inv-sum-num">{{ inventorySummary.low }}</div>
                <div class="inv-sum-label">低库存预警</div>
              </div>
              <div class="inv-sum-item ok">
                <div class="inv-sum-num">{{ inventorySummary.normal }}</div>
                <div class="inv-sum-label">正常库存</div>
              </div>
            </div>
            <ul class="alert-list">
              <li v-for="item in alertItems" :key="item.inventoryId||item.materialId" class="alert-item">
                <span class="alert-item__name">{{ invName(item) }}</span>
                <div class="alert-item__bar-wrap">
                  <div class="alert-item__bar" :style="{width: Math.min(invPct(item)*100,100)+'%', background: invColor(item)}"></div>
                </div>
                <span class="alert-item__qty" :style="{color: invColor(item)}">{{ item.quantityAvailable || 0 }}</span>
              </li>
              <li v-if="!alertItems.length" class="detail-empty">库存状态正常</li>
            </ul>
          </div>
        </div>

        <!-- 设备状态 -->
        <div class="detail-card bottom-card">
          <div class="detail-card__hd">
            <span class="detail-card__title">设备运行状态</span>
            <a class="detail-card__link" @click="goLogin">设备管理 →</a>
          </div>
          <div v-if="eqLoading" class="detail-loading"><div class="skeleton" v-for="i in 3" :key="i"></div></div>
          <div v-else>
            <div class="eq-summary">
              <div class="eq-sum-item ok">
                <div class="eq-sum-num">{{ eqSummary.running }}</div>
                <div class="eq-sum-label">运行中</div>
              </div>
              <div class="eq-sum-item warn">
                <div class="eq-sum-num">{{ eqSummary.idle }}</div>
                <div class="eq-sum-label">空闲</div>
              </div>
              <div class="eq-sum-item danger">
                <div class="eq-sum-num">{{ eqSummary.fault }}</div>
                <div class="eq-sum-label">故障</div>
              </div>
              <div class="eq-sum-item maint">
                <div class="eq-sum-num">{{ eqSummary.maintenance }}</div>
                <div class="eq-sum-label">维修中</div>
              </div>
            </div>
            <!-- 设备列表 -->
            <div class="eq-list">
              <div v-for="eq in equipmentList" :key="eq.equipmentId||eq.id" class="eq-item">
                <div class="eq-item__dot" :class="eqDotClass(eq.status)"></div>
                <span class="eq-item__name">{{ eqName(eq) }}</span>
                <span class="eq-item__status" :class="eqStatusClass(eq.status)">
                  {{ eqStatusCn(eq.status) }}
                </span>
              </div>
              <div v-if="!equipmentList.length" class="detail-empty">暂无设备数据</div>
            </div>
          </div>
        </div>

        <!-- 采购动态 -->
        <div class="detail-card bottom-card">
          <div class="detail-card__hd">
            <span class="detail-card__title">采购动态</span>
            <a class="detail-card__link" @click="goLogin">采购管理 →</a>
          </div>
          <div v-if="poLoading" class="detail-loading"><div class="skeleton" v-for="i in 3" :key="i"></div></div>
          <div v-else>
            <div class="po-summary">
              <div class="po-sum-item">
                <div class="po-sum-num">{{ poSummary.total }}</div>
                <div class="po-sum-label">采购订单</div>
              </div>
              <div class="po-sum-item warn">
                <div class="po-sum-num">{{ poSummary.pending }}</div>
                <div class="po-sum-label">待到货</div>
              </div>
              <div class="po-sum-item ok">
                <div class="po-sum-num">{{ poSummary.arrived }}</div>
                <div class="po-sum-label">已到货</div>
              </div>
            </div>
            <ul class="po-list">
              <li v-for="po in recentPO" :key="po.purchaseOrderId||po.id" class="po-item">
                <div class="po-item__no">{{ po.purchaseOrderNo || po.id }}</div>
                <div class="po-item__meta">
                  <span>{{ po.supplierName || '-' }}</span>
                  <span class="status-tag" :class="poTagClass(po.status)">
                    {{ poStatusCn(po.status) }}
                  </span>
                </div>
              </li>
              <li v-if="!recentPO.length" class="detail-empty">暂无采购订单</li>
            </ul>
          </div>
        </div>
      </div>

    </div>
  </section>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchOrderList, fetchWorkOrderList, fetchInventoryFullList,
  fetchEquipmentList, fetchPurchaseOrderList
} from '@/api/business'
import { fetchQualityKpi, fetchInspectionViews } from '@/api/quality'

const router = useRouter()

const loading      = ref(true)
const orderLoading = ref(true)
const woLoading    = ref(true)
const qcLoading    = ref(true)
const invLoading   = ref(true)
const eqLoading    = ref(true)
const poLoading    = ref(true)
const updatedAt    = ref(null)

const orders         = ref([])
const workOrders     = ref([])
const inventory      = ref([])
const equipment      = ref([])
const purchaseOrders = ref([])
const qcKpi          = ref({})
const inspections    = ref([])
const displayNums    = ref({})

function goLogin()    { router.push('/login') }
function goPage()     { router.push('/login') }

// 从接口响应中安全提取数组（支持 {value:[]} 和 {data:[]} 和直接数组）
function extractArray(r) {
  if (!r) return []
  if (Array.isArray(r)) return r
  if (Array.isArray(r.value)) return r.value
  if (Array.isArray(r.data)) return r.data
  return []
}

// ── KPI 卡片 ──────────────────────────────────────────────────
const kpis = computed(() => [
  {
    key: 'orders', label: '客户订单', icon: 'DocumentChecked',
    value: orders.value.length, unit: '单', path: '/order/list',
    theme: 'kpi-order',
    sub: '生产中 ' + orders.value.filter(o => ['PRODUCING','IN_PRODUCTION','CONFIRMED'].includes(o.auditStatus)).length + ' 单'
  },
  {
    key: 'workOrders', label: '生产工单', icon: 'Setting',
    value: workOrders.value.length, unit: '单', path: '/production/work-order',
    theme: 'kpi-wo',
    sub: '在制 ' + workOrders.value.filter(w => w.status === 'PRODUCING').length + ' 单'
  },
  {
    key: 'qcPending', label: '待检任务', icon: 'CircleCheck',
    value: qcKpi.value.pending ?? 0, unit: '单', path: '/quality/inspection',
    theme: 'kpi-qc',
    warn: (qcKpi.value.pending ?? 0) > 0,
    sub: '复检 ' + (qcKpi.value.recheck ?? 0) + ' 单'
  },
  {
    key: 'inventory', label: '库存物料', icon: 'Box',
    value: inventory.value.length, unit: '种', path: '/warehouse/inventory',
    theme: 'kpi-inv',
    sub: '预警 ' + inventory.value.filter(i => { const a=Number(i.quantityAvailable||0),s=Number(i.safetyStock||0); return s>0?a<s*0.3:a<=20 }).length + ' 种'
  },
  {
    key: 'equipment', label: '设备总数', icon: 'Cpu',
    value: equipment.value.length, unit: '台', path: '/device/equipment',
    theme: 'kpi-eq',
    danger: equipment.value.filter(e => ['FAULT','BREAKDOWN','ERROR'].includes(e.status)).length > 0,
    sub: '故障 ' + equipment.value.filter(e => ['FAULT','BREAKDOWN','ERROR'].includes(e.status)).length + ' 台'
  },
  {
    key: 'purchaseOrders', label: '采购订单', icon: 'ShoppingCart',
    value: purchaseOrders.value.length, unit: '单', path: '/purchase/order',
    theme: 'kpi-po',
    sub: '已收货 ' + purchaseOrders.value.filter(p => p.status === 'RECEIVED').length + ' 单'
  }
])

// ── 订单 ─────────────────────────────────────────────────────
const recentOrders = computed(() =>
  [...orders.value].sort((a, b) => (b.createdAt || '') > (a.createdAt || '') ? 1 : -1).slice(0, 5)
)
function orderTagClass(s) {
  return {
    PENDING: 'status-tag--warning',
    APPROVED: 'status-tag--processing',
    PLANNED: 'status-tag--processing',
    PRODUCING: 'status-tag--processing',
    COMPLETED: 'status-tag--success',
    DELIVERED: 'status-tag--success',
    CANCELLED: 'status-tag--normal'
  }[s] || 'status-tag--normal'
}
function orderStatusCn(s) {
  return {
    PENDING: '待审核', APPROVED: '已审批', PLANNED: '已排产',
    PRODUCING: '生产中', COMPLETED: '已完成', DELIVERED: '已发货', CANCELLED: '已取消'
  }[s] || s || '-'
}

// ── 工单 ─────────────────────────────────────────────────────
const woSummary = computed(() => {
  const all = workOrders.value
  return [
    { label: '计划', count: all.filter(w => w.status === 'PLANNED').length, color: '#1677ff' },
    { label: '在制', count: all.filter(w => w.status === 'PRODUCING').length, color: '#00d7c3' },
    { label: '完成', count: all.filter(w => w.status === 'COMPLETED').length, color: '#52c41a' },
    { label: '暂停', count: all.filter(w => w.status === 'PAUSED').length, color: '#ff4d4f' }
  ]
})
const activeWorkOrders = computed(() =>
  workOrders.value
    .filter(w => ['PRODUCING', 'PLANNED'].includes(w.status))
    .slice(0, 5)
)
function progressPct(wo) {
  const total = Number(wo.plannedQuantity || 0)
  const done  = Number(wo.completedQuantity || 0)
  if (!total) return 0
  return Math.min(Math.round(done / total * 100), 100)
}
function woBarColor(wo) {
  const pct = progressPct(wo)
  if (pct >= 80) return '#52c41a'
  if (pct >= 40) return '#00d7c3'
  return '#1677ff'
}
function woName(wo) {
  // 工单接口没有直接返回产品名，用 materialId 代替
  return 'M-' + (wo.materialId || wo.workOrderId)
}

// ── 质检 ─────────────────────────────────────────────────────
const yieldRate = computed(() => {
  const total = (qcKpi.value.passed || 0) + (qcKpi.value.failed || 0)
  if (!total) return '--'
  return ((qcKpi.value.passed / total) * 100).toFixed(1)
})
const qcStatusBars = computed(() => {
  const total = Math.max(inspections.value.length, 1)
  const items = [
    { label: '通过',   count: qcKpi.value.passed ?? 0,  color: '#52c41a' },
    { label: '待检',   count: qcKpi.value.pending ?? 0, color: '#1677ff' },
    { label: '复检',   count: qcKpi.value.recheck ?? 0, color: '#fa8c16' },
    { label: '不通过', count: qcKpi.value.failed ?? 0,  color: '#ff4d4f' }
  ]
  return items.map(i => ({ ...i, pct: Math.max(Math.round(i.count / total * 80), i.count > 0 ? 8 : 0) }))
})

// ── 库存 ─────────────────────────────────────────────────────
const inventorySummary = computed(() => {
  const all = inventory.value
  const low = all.filter(i => {
    if (i.inventoryStatus === 'ALERT') return true
    const avail = Number(i.quantityAvailable || 0)
    const safety = Number(i.safetyStock || 0)
    return safety > 0 ? avail < safety * 0.3 : avail <= 20
  })
  return { total: all.length, low: low.length, normal: all.length - low.length }
})
const alertItems = computed(() =>
  [...inventory.value]
    .sort((a, b) => {
      const ra = Number(a.quantityAvailable || 0) / Math.max(Number(a.safetyStock || 1), 1)
      const rb = Number(b.quantityAvailable || 0) / Math.max(Number(b.safetyStock || 1), 1)
      return ra - rb
    })
    .slice(0, 6)
)
function invPct(item) {
  const qty    = Number(item.quantityAvailable || 0)
  const safety = Number(item.safetyStock || 0)
  const max    = Math.max(safety * 1.5, Number(item.quantityOnHand || 100), qty)
  return max ? Math.min(qty / max, 1) : 0
}
function invColor(item) {
  const qty    = Number(item.quantityAvailable || 0)
  const safety = Number(item.safetyStock || 0)
  if (qty <= 0) return '#ff4d4f'
  if (safety > 0) {
    const ratio = qty / safety
    if (ratio < 0.1) return '#ff4d4f'
    if (ratio < 0.3) return '#fa8c16'
    return '#52c41a'
  }
  if (qty <= 20) return '#fa8c16'
  return '#52c41a'
}
function invName(item) {
  return item.materialName || item.batchNo || ('ID-' + item.materialId)
}

// ── 设备 ─────────────────────────────────────────────────────
const eqSummary = computed(() => ({
  running:     equipment.value.filter(e => ['RUNNING', 'ONLINE'].includes(e.status)).length,
  idle:        equipment.value.filter(e => ['IDLE', 'STANDBY'].includes(e.status)).length,
  fault:       equipment.value.filter(e => ['FAULT', 'BREAKDOWN', 'ERROR'].includes(e.status)).length,
  maintenance: equipment.value.filter(e => ['MAINTENANCE', 'REPAIR'].includes(e.status)).length
}))
const equipmentList = computed(() => equipment.value.slice(0, 8))
function eqStatusCn(s) {
  return { RUNNING: '运行', ONLINE: '运行', IDLE: '空闲', STANDBY: '待机',
    FAULT: '故障', BREAKDOWN: '故障', ERROR: '异常',
    MAINTENANCE: '维修', REPAIR: '维修', OFFLINE: '离线' }[s] || '空闲'
}
function eqDotClass(s) {
  if (['RUNNING', 'ONLINE'].includes(s)) return 'dot-ok'
  if (['FAULT', 'BREAKDOWN', 'ERROR'].includes(s)) return 'dot-danger'
  if (['MAINTENANCE', 'REPAIR'].includes(s)) return 'dot-warn'
  return 'dot-idle'
}
function eqStatusClass(s) {
  if (['RUNNING', 'ONLINE'].includes(s)) return 'eq-ok'
  if (['FAULT', 'BREAKDOWN', 'ERROR'].includes(s)) return 'eq-danger'
  if (['MAINTENANCE', 'REPAIR'].includes(s)) return 'eq-warn'
  return 'eq-idle'
}
function eqName(eq) {
  return eq.equipmentName || eq.equipmentCode || ('EQ-' + eq.equipmentId)
}

// ── 采购 ─────────────────────────────────────────────────────
const poSummary = computed(() => ({
  total:    purchaseOrders.value.length,
  pending:  purchaseOrders.value.filter(p => p.status === 'ORDERED').length,
  arrived:  purchaseOrders.value.filter(p => p.status === 'RECEIVED').length
}))
const recentPO = computed(() =>
  [...purchaseOrders.value]
    .sort((a, b) => (b.createdAt || '') > (a.createdAt || '') ? 1 : -1)
    .slice(0, 5)
)
function poTagClass(s) {
  return { PENDING: 'status-tag--warning', ORDERED: 'status-tag--processing',
    RECEIVED: 'status-tag--success', CANCELLED: 'status-tag--normal' }[s] || 'status-tag--normal'
}
function poStatusCn(s) {
  return { PENDING: '待审批', ORDERED: '待到货', RECEIVED: '已收货', CANCELLED: '已取消' }[s] || s || '-'
}

// ── 数字滚动 ──────────────────────────────────────────────────
function animateNum(key, target) {
  const duration = 1000
  const start    = Date.now()
  const from     = displayNums.value[key] ?? 0
  const step = () => {
    const p = Math.min((Date.now() - start) / duration, 1)
    const e = 1 - Math.pow(1 - p, 3)
    displayNums.value[key] = Math.round(from + (target - from) * e)
    if (p < 1) requestAnimationFrame(step)
    else displayNums.value[key] = target
  }
  requestAnimationFrame(step)
}

// ── 数据加载 ──────────────────────────────────────────────────
async function loadAll() {
  loading.value = true
  updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })

  await Promise.allSettled([
    fetchOrderList().then(r => {
      orders.value = extractArray(r)
      orderLoading.value = false
    }).catch(() => { orderLoading.value = false }),

    fetchWorkOrderList().then(r => {
      workOrders.value = extractArray(r)
      woLoading.value = false
    }).catch(() => { woLoading.value = false }),

    Promise.all([fetchQualityKpi(), fetchInspectionViews()]).then(([kr, ir]) => {
      qcKpi.value    = kr?.data ?? kr ?? {}
      inspections.value = extractArray(ir?.data ?? ir)
      qcLoading.value = false
    }).catch(() => { qcLoading.value = false }),

    fetchInventoryFullList().then(r => {
      inventory.value = extractArray(r)
      invLoading.value = false
    }).catch(() => { invLoading.value = false }),

    fetchEquipmentList().then(r => {
      equipment.value = extractArray(r)
      eqLoading.value = false
    }).catch(() => { eqLoading.value = false }),

    fetchPurchaseOrderList().then(r => {
      purchaseOrders.value = extractArray(r)
      poLoading.value = false
    }).catch(() => { poLoading.value = false })
  ])

  loading.value = false
  setTimeout(() => kpis.value.forEach(k => animateNum(k.key, k.value)), 200)
}

onMounted(loadAll)
</script>
<style scoped>
/* ── 整体区块 ──────────────────────────────────────────────── */
.live-section {
  padding: 80px 24px;
  background: #0d1b2e;
  position: relative;
  overflow: hidden;
}
.live-section::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 60% 40% at 20% 50%, rgba(0,215,195,0.06) 0%, transparent 70%),
    radial-gradient(ellipse 50% 60% at 80% 30%, rgba(22,119,255,0.06) 0%, transparent 70%);
  pointer-events: none;
}
.live-inner {
  max-width: 1280px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* ── 标题区 ────────────────────────────────────────────────── */
.live-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 40px;
  flex-wrap: wrap;
  gap: 16px;
}
.live-kicker {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 2.5px;
  color: var(--accent-bright);
  margin-bottom: 10px;
  text-transform: uppercase;
}
.live-title {
  margin: 0 0 10px;
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.5px;
}
.live-subtitle {
  margin: 0;
  font-size: 15px;
  color: rgba(255,255,255,0.5);
}
.live-header__right { text-align: right; }
.live-pulse {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--accent-bright);
  margin-bottom: 4px;
}
.live-pulse.loading .pulse-dot { animation: none; background: #fa8c16; }
.pulse-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--accent-bright);
  animation: pulse-blink 1.4s infinite;
}
@keyframes pulse-blink {
  0%,100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.4; transform: scale(0.7); }
}
.live-updated { font-size: 12px; color: rgba(255,255,255,0.3); }

/* ── KPI 六宫格 ────────────────────────────────────────────── */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
@media (max-width: 1100px) { .kpi-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 640px)  { .kpi-grid { grid-template-columns: repeat(2, 1fr); } }

.kpi-card {
  position: relative;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  padding: 20px 16px 16px;
  cursor: pointer;
  transition: transform .15s, border-color .15s, background .15s;
  overflow: hidden;
}
.kpi-card::after {
  content: "";
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 2px;
  opacity: 0;
  transition: opacity .15s;
}
.kpi-card:hover { transform: translateY(-3px); border-color: rgba(255,255,255,0.2); background: rgba(255,255,255,0.08); }
.kpi-card:hover::after { opacity: 1; }
.kpi-order::after  { background: #1677ff; }
.kpi-wo::after     { background: #00d7c3; }
.kpi-qc::after     { background: #52c41a; }
.kpi-inv::after    { background: #fa8c16; }
.kpi-eq::after     { background: #a855f7; }
.kpi-po::after     { background: #f59e0b; }
.kpi-card--warn    { border-color: rgba(250,140,22,0.4) !important; }
.kpi-card--danger  { border-color: rgba(255,77,79,0.4) !important; }

.kpi-card__icon {
  width: 32px; height: 32px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px;
  margin-bottom: 12px;
  background: rgba(255,255,255,0.08);
  color: rgba(255,255,255,0.7);
}
.kpi-card__num {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 4px;
}
.kpi-num-val {
  font-size: 30px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.kpi-num-unit { font-size: 13px; color: rgba(255,255,255,0.45); }
.kpi-card__label { font-size: 13px; color: rgba(255,255,255,0.55); margin-bottom: 4px; }
.kpi-card__sub   { font-size: 11px; color: rgba(255,255,255,0.3); }
.kpi-card__badge {
  position: absolute;
  top: 10px; right: 10px;
  background: #ff4d4f;
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
}
.kpi-card--warn .kpi-card__badge { background: #fa8c16; }
.kpi-card__arrow {
  position: absolute;
  bottom: 14px; right: 14px;
  font-size: 14px;
  color: rgba(255,255,255,0.2);
  transition: color .15s, transform .15s;
}
.kpi-card:hover .kpi-card__arrow { color: rgba(255,255,255,0.6); transform: translateX(3px); }

/* ── 详情三列 ──────────────────────────────────────────────── */
.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
@media (max-width: 960px) { .detail-grid { grid-template-columns: 1fr; } }

/* ── 底部三列 ──────────────────────────────────────────────── */
.bottom-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
@media (max-width: 960px) { .bottom-grid { grid-template-columns: 1fr; } }

/* ── 卡片通用 ──────────────────────────────────────────────── */
.detail-card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  padding: 20px;
}
.detail-card__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.detail-card__title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
}
.detail-card__link {
  font-size: 12px;
  color: var(--accent-bright);
  cursor: pointer;
  text-decoration: none;
  transition: opacity .15s;
}
.detail-card__link:hover { opacity: 0.75; }
.detail-empty {
  font-size: 13px;
  color: rgba(255,255,255,0.25);
  text-align: center;
  padding: 20px 0;
}

/* 骨架屏 */
.detail-loading { display: flex; flex-direction: column; gap: 8px; }
.skeleton {
  height: 32px; border-radius: 6px;
  background: linear-gradient(90deg, rgba(255,255,255,0.05) 25%, rgba(255,255,255,0.1) 50%, rgba(255,255,255,0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── 订单列表 ──────────────────────────────────────────────── */
.order-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.order-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: rgba(255,255,255,0.04);
  border-radius: 8px;
  transition: background .15s;
}
.order-item:hover { background: rgba(255,255,255,0.08); }
.order-item__no { font-size: 13px; font-weight: 600; color: #fff; flex-shrink: 0; width: 110px; }
.order-item__meta { flex: 1; display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: rgba(255,255,255,0.45); }
.order-item__qty { font-weight: 600; color: rgba(255,255,255,0.7); }

/* ── 工单 ──────────────────────────────────────────────────── */
.wo-summary {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 6px; margin-bottom: 14px;
}
.wo-sum-item { text-align: center; padding: 8px 4px; background: rgba(255,255,255,0.04); border-radius: 6px; }
.wo-sum-num  { font-size: 20px; font-weight: 700; }
.wo-sum-label{ font-size: 11px; color: rgba(255,255,255,0.4); margin-top: 2px; }
.wo-progress-list { display: flex; flex-direction: column; gap: 10px; }
.wo-progress-item__hd { display: flex; justify-content: space-between; margin-bottom: 4px; }
.wo-no  { font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.75); }
.wo-pct { font-size: 12px; color: rgba(255,255,255,0.45); }
.wo-bar { height: 4px; background: rgba(255,255,255,0.08); border-radius: 2px; overflow: hidden; }
.wo-bar__fill { height: 100%; border-radius: 2px; transition: width .6s ease; }
.wo-progress-item__ft { display: flex; justify-content: space-between; margin-top: 4px; font-size: 11px; color: rgba(255,255,255,0.35); }

/* ── 质检 ──────────────────────────────────────────────────── */
.qc-yield-block { text-align: center; padding: 8px 0 16px; }
.qc-yield-num {
  font-size: 48px; font-weight: 700; color: #fff; line-height: 1;
  font-variant-numeric: tabular-nums;
}
.qc-yield-pct { font-size: 22px; color: rgba(255,255,255,0.5); }
.qc-yield-label { font-size: 13px; color: rgba(255,255,255,0.4); margin-top: 4px; }
.qc-status-row {
  display: flex; justify-content: space-around; align-items: flex-end;
  height: 80px; padding: 0 8px; margin-bottom: 12px;
}
.qc-status-item { display: flex; flex-direction: column; align-items: center; gap: 4px; flex: 1; }
.qc-status-bar-wrap { width: 24px; background: rgba(255,255,255,0.06); border-radius: 4px; height: 60px; display: flex; align-items: flex-end; overflow: hidden; }
.qc-status-bar { width: 100%; border-radius: 4px 4px 0 0; transition: height .6s ease; min-height: 3px; }
.qc-status-num { font-size: 14px; font-weight: 700; }
.qc-status-label { font-size: 11px; color: rgba(255,255,255,0.35); }
.qc-alert {
  background: rgba(250,140,22,0.12);
  border: 1px solid rgba(250,140,22,0.3);
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 12px;
  color: #fa8c16;
}

/* ── 库存 ──────────────────────────────────────────────────── */
.inv-summary {
  display: grid; grid-template-columns: repeat(3,1fr);
  gap: 6px; margin-bottom: 14px;
}
.inv-sum-item { text-align: center; padding: 8px 4px; background: rgba(255,255,255,0.04); border-radius: 6px; }
.inv-sum-item.warn .inv-sum-num { color: #fa8c16; }
.inv-sum-item.ok   .inv-sum-num { color: #52c41a; }
.inv-sum-num   { font-size: 20px; font-weight: 700; color: #fff; }
.inv-sum-label { font-size: 11px; color: rgba(255,255,255,0.4); margin-top: 2px; }
.alert-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.alert-item { display: grid; grid-template-columns: 1fr 100px 36px; align-items: center; gap: 8px; }
.alert-item__name { font-size: 12px; color: rgba(255,255,255,0.65); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.alert-item__bar-wrap { height: 4px; background: rgba(255,255,255,0.08); border-radius: 2px; overflow: hidden; }
.alert-item__bar { height: 100%; border-radius: 2px; transition: width .6s; }
.alert-item__qty { font-size: 12px; font-weight: 600; text-align: right; }

/* ── 设备 ──────────────────────────────────────────────────── */
.eq-summary {
  display: grid; grid-template-columns: repeat(4,1fr);
  gap: 6px; margin-bottom: 14px;
}
.eq-sum-item { text-align: center; padding: 8px 4px; background: rgba(255,255,255,0.04); border-radius: 6px; }
.eq-sum-item.ok      .eq-sum-num { color: #52c41a; }
.eq-sum-item.warn    .eq-sum-num { color: #fa8c16; }
.eq-sum-item.danger  .eq-sum-num { color: #ff4d4f; }
.eq-sum-item.maint   .eq-sum-num { color: #1677ff; }
.eq-sum-num   { font-size: 20px; font-weight: 700; color: #fff; }
.eq-sum-label { font-size: 11px; color: rgba(255,255,255,0.4); margin-top: 2px; }
.eq-list { display: flex; flex-direction: column; gap: 7px; }
.eq-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; background: rgba(255,255,255,0.04); border-radius: 6px;
}
.eq-item__dot {
  width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0;
}
.dot-ok     { background: #52c41a; box-shadow: 0 0 6px rgba(82,196,26,0.6); }
.dot-danger { background: #ff4d4f; box-shadow: 0 0 6px rgba(255,77,79,0.6); }
.dot-warn   { background: #fa8c16; box-shadow: 0 0 6px rgba(250,140,22,0.6); }
.dot-idle   { background: rgba(255,255,255,0.25); }
.eq-item__name   { flex: 1; font-size: 12px; color: rgba(255,255,255,0.65); }
.eq-item__status { font-size: 11px; padding: 1px 7px; border-radius: 10px; }
.eq-ok      { background: rgba(82,196,26,0.15); color: #52c41a; }
.eq-danger  { background: rgba(255,77,79,0.15); color: #ff4d4f; }
.eq-warn    { background: rgba(250,140,22,0.15); color: #fa8c16; }
.eq-idle    { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.3); }

/* ── 采购 ──────────────────────────────────────────────────── */
.po-summary {
  display: grid; grid-template-columns: repeat(3,1fr);
  gap: 6px; margin-bottom: 14px;
}
.po-sum-item { text-align: center; padding: 8px 4px; background: rgba(255,255,255,0.04); border-radius: 6px; }
.po-sum-item.warn .po-sum-num { color: #fa8c16; }
.po-sum-item.ok   .po-sum-num { color: #52c41a; }
.po-sum-num   { font-size: 20px; font-weight: 700; color: #fff; }
.po-sum-label { font-size: 11px; color: rgba(255,255,255,0.4); margin-top: 2px; }
.po-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.po-item { padding: 9px 10px; background: rgba(255,255,255,0.04); border-radius: 6px; }
.po-item__no { font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.8); margin-bottom: 5px; }
.po-item__meta { display: flex; align-items: center; justify-content: space-between; font-size: 11px; color: rgba(255,255,255,0.4); }
</style>
