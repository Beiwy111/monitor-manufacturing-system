<template>
  <div class="cp-page" v-loading="loading">
    <div class="cp-head">
      <h2 class="cp-title">我的订单</h2>
      <el-button type="primary" size="small" @click="$router.push('/customer/order/new')">新建订单</el-button>
    </div>

    <el-table :data="orders" border stripe size="small" style="width:100%" highlight-current-row @current-change="onSelect">
      <el-table-column prop="orderNo" label="订单号" width="130" fixed />
      <el-table-column prop="orderDate" label="下单日期" width="110" />
      <el-table-column prop="requiredDeliveryDate" label="要求交期" width="110" />
      <el-table-column prop="currentStage" label="当前阶段" width="100" />
      <el-table-column label="生产进度" width="180">
        <template #default="{ row }">
          <el-progress :percentage="row.progressPercent || 0" :stroke-width="8" />
        </template>
      </el-table-column>
      <el-table-column prop="auditStatus" label="审核状态" width="90" />
      <el-table-column prop="orderAmount" label="金额" width="100" align="right" />
      <el-table-column label="产品" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.items?.[0]?.productName || '—' }}</template>
      </el-table-column>
    </el-table>

    <div v-if="detail" class="cp-detail">
      <div class="cp-detail__bar">
        <span>订单详情：{{ detail.orderNo }}</span>
        <span class="cp-muted">预计交付 {{ detail.requiredDeliveryDate || '—' }}</span>
      </div>

      <el-table :data="detail.items || []" border stripe size="small" style="width:100%;margin-bottom:12px">
        <el-table-column prop="productName" label="产品" min-width="140" />
        <el-table-column prop="specification" label="规格" min-width="140" />
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column prop="unit" label="单位" width="60" />
        <el-table-column prop="lineAmount" label="金额" width="100" align="right" />
      </el-table>

      <div class="cp-timeline-title">订单进度</div>
      <div class="cp-timeline">
        <div v-for="(s, i) in detail.timeline || []" :key="i" class="cp-timeline__item" :class="`is-${s.status}`">
          <div class="cp-timeline__dot" />
          <div class="cp-timeline__body">
            <div class="cp-timeline__name">{{ s.name }}</div>
            <div class="cp-timeline__detail">{{ s.detail || '—' }}</div>
            <div class="cp-timeline__time">{{ formatTime(s.time) }}</div>
          </div>
        </div>
      </div>

      <div v-if="detail.deliveries?.length" class="cp-deliveries">
        <div class="cp-timeline-title">发货记录</div>
        <el-table :data="detail.deliveries" border stripe size="small" style="width:100%">
          <el-table-column prop="deliveryNo" label="发货单" width="130" />
          <el-table-column prop="deliveryQuantity" label="数量" width="80" align="right" />
          <el-table-column prop="deliveryStatus" label="状态" width="90" />
          <el-table-column prop="logisticsCompany" label="物流" width="100" />
          <el-table-column prop="logisticsNo" label="运单号" min-width="140" />
          <el-table-column prop="deliveryDate" label="发货日期" width="110" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getCustomerOrderDetail, getCustomerOrders } from '@/api/customer'

const route = useRoute()
const loading = ref(false)
const orders = ref([])
const detail = ref(null)

function formatTime(v) {
  if (!v) return ''
  return String(v).replace('T', ' ').slice(0, 16)
}

async function loadOrders() {
  loading.value = true
  try {
    orders.value = await getCustomerOrders() || []
  } finally {
    loading.value = false
  }
}

async function loadDetail(orderId) {
  if (!orderId) {
    detail.value = null
    return
  }
  loading.value = true
  try {
    detail.value = await getCustomerOrderDetail(orderId)
  } finally {
    loading.value = false
  }
}

function onSelect(row) {
  if (row?.orderId) loadDetail(row.orderId)
}

watch(() => route.query.orderId, (id) => {
  if (id) loadDetail(Number(id))
}, { immediate: true })

onMounted(loadOrders)
</script>

<style scoped>
.cp-page { padding: 12px 16px; font-weight: 400; }
.cp-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; border-bottom: 1px solid #e8e8e8; padding-bottom: 10px; }
.cp-title { margin: 0; font-size: 18px; font-weight: 500; }
.cp-detail { margin-top: 12px; border: 1px solid #e8e8e8; padding: 12px; background: #fff; }
.cp-detail__bar { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; }
.cp-muted { color: #666; font-size: 13px; }
.cp-timeline-title { font-size: 14px; margin: 8px 0; color: #333; }
.cp-timeline { display: flex; gap: 0; overflow-x: auto; padding: 8px 0 12px; }
.cp-timeline__item { flex: 1; min-width: 120px; position: relative; padding-left: 14px; }
.cp-timeline__dot { width: 10px; height: 10px; border-radius: 50%; background: #d9d9d9; position: absolute; left: 0; top: 4px; }
.cp-timeline__item.is-done .cp-timeline__dot { background: #52c41a; }
.cp-timeline__item.is-active .cp-timeline__dot { background: #1677ff; }
.cp-timeline__name { font-size: 13px; font-weight: 500; }
.cp-timeline__detail { font-size: 12px; color: #666; margin-top: 2px; }
.cp-timeline__time { font-size: 11px; color: #999; margin-top: 2px; }
.cp-deliveries { margin-top: 12px; }
</style>
