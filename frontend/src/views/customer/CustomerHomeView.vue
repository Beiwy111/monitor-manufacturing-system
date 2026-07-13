<template>
  <div class="cp-page" v-loading="loading">
    <div class="cp-head">
      <div>
        <h2 class="cp-title">客户首页</h2>
        <p class="cp-sub">{{ data.customerName || '—' }}</p>
      </div>
      <div class="cp-head-actions">
        <el-button type="primary" size="small" @click="$router.push('/customer/order/new')">新建订单</el-button>
        <el-button size="small" @click="$router.push('/customer/orders')">我的订单</el-button>
      </div>
    </div>

    <div class="cp-stats">
      <div class="cp-stat"><span class="cp-stat__label">全部订单</span><span class="cp-stat__val">{{ data.stats?.orderCount ?? 0 }}</span></div>
      <div class="cp-stat"><span class="cp-stat__label">生产中</span><span class="cp-stat__val">{{ data.stats?.inProgressCount ?? 0 }}</span></div>
      <div class="cp-stat"><span class="cp-stat__label">待审核</span><span class="cp-stat__val">{{ data.stats?.pendingOrderCount ?? 0 }}</span></div>
      <div class="cp-stat"><span class="cp-stat__label">待处理反馈</span><span class="cp-stat__val">{{ data.stats?.openFeedbackCount ?? 0 }}</span></div>
    </div>

    <div class="cp-grid">
      <section class="cp-section cp-section--wide">
        <div class="cp-section__head">
          <span class="cp-section__title">最近订单生产进度</span>
        </div>
        <el-table :data="data.recentOrders || []" border stripe size="small" style="width:100%" empty-text="暂无订单">
          <el-table-column prop="orderNo" label="订单号" width="130" />
          <el-table-column prop="currentStage" label="当前阶段" width="100" />
          <el-table-column label="进度" width="160">
            <template #default="{ row }">
              <el-progress :percentage="row.progressPercent || 0" :stroke-width="8" />
            </template>
          </el-table-column>
          <el-table-column prop="estimatedDelivery" label="预计交付" width="110" />
          <el-table-column prop="orderAmount" label="金额" width="100" align="right" />
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="goOrder(row.orderId)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="cp-section">
        <div class="cp-section__head">
          <span class="cp-section__title">待确认事项</span>
        </div>
        <el-table :data="data.pendingItems || []" border stripe size="small" style="width:100%" empty-text="暂无待确认事项">
          <el-table-column prop="title" label="事项" width="100" />
          <el-table-column prop="refNo" label="单号" width="130" />
          <el-table-column prop="detail" label="说明" min-width="160" show-overflow-tooltip />
        </el-table>
      </section>

      <section class="cp-section">
        <div class="cp-section__head">
          <span class="cp-section__title">最近售后反馈</span>
          <el-button link type="primary" size="small" @click="$router.push('/customer/feedback/list')">全部</el-button>
        </div>
        <el-table :data="data.recentFeedbacks || []" border stripe size="small" style="width:100%" empty-text="暂无反馈">
          <el-table-column prop="caseNo" label="案例号" width="130" />
          <el-table-column prop="problemType" label="类型" width="90" />
          <el-table-column prop="caseStatus" label="状态" width="90" />
          <el-table-column prop="problemDescription" label="描述" min-width="140" show-overflow-tooltip />
        </el-table>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCustomerDashboard } from '@/api/customer'

const router = useRouter()
const loading = ref(false)
const data = reactive({
  customerName: '',
  stats: {},
  recentOrders: [],
  pendingItems: [],
  recentFeedbacks: []
})

async function load() {
  loading.value = true
  try {
    const res = await getCustomerDashboard()
    Object.assign(data, res || {})
  } finally {
    loading.value = false
  }
}

function goOrder(orderId) {
  router.push({ path: '/customer/orders', query: { orderId } })
}

onMounted(load)
</script>

<style scoped>
.cp-page { padding: 12px 16px; font-weight: 400; }
.cp-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; border-bottom: 1px solid #e8e8e8; padding-bottom: 10px; }
.cp-title { margin: 0; font-size: 18px; font-weight: 500; color: #1f1f1f; }
.cp-sub { margin: 4px 0 0; font-size: 13px; color: #666; }
.cp-head-actions { display: flex; gap: 8px; }
.cp-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 12px; }
.cp-stat { border: 1px solid #e8e8e8; padding: 10px 12px; background: #fafafa; }
.cp-stat__label { display: block; font-size: 12px; color: #666; }
.cp-stat__val { font-size: 20px; color: #1f1f1f; }
.cp-grid { display: flex; flex-direction: column; gap: 12px; }
.cp-section { border: 1px solid #e8e8e8; background: #fff; }
.cp-section--wide { width: 100%; }
.cp-section__head { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; border-bottom: 1px solid #f0f0f0; background: #fafafa; }
.cp-section__title { font-size: 14px; color: #333; }
.cp-section :deep(.el-table) { border: none; }
</style>
