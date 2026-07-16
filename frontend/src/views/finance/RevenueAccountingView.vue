<template>
  <ModulePageShell>
    <div class="page-head">
      <span class="page-title">收益核算</span>
      <el-button size="small" :loading="loading" @click="reload">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="finance-tabs">
      <el-tab-pane label="订单收益" name="orders">
        <el-table :data="orders" border stripe size="small" v-loading="loading" max-height="520">
          <el-table-column prop="orderNo" label="订单编号" width="130" fixed />
          <el-table-column prop="customerName" label="客户" width="120" show-overflow-tooltip />
          <el-table-column prop="productName" label="产品" width="130" show-overflow-tooltip />
          <el-table-column prop="deliveredQty" label="交付数量" width="90" align="right" />
          <el-table-column label="销售单价" width="95" align="right">
            <template #default="{ row }">¥ {{ fmtMoney(row.unitPrice) }}</template>
          </el-table-column>
          <el-table-column label="销售收入" width="100" align="right">
            <template #default="{ row }">¥ {{ fmtMoney(row.salesRevenue) }}</template>
          </el-table-column>
          <el-table-column label="折扣" width="85" align="right">
            <template #default="{ row }">¥ {{ fmtMoney(row.discountAmount) }}</template>
          </el-table-column>
          <el-table-column label="退款" width="85" align="right">
            <template #default="{ row }">¥ {{ fmtMoney(row.refundAmount) }}</template>
          </el-table-column>
          <el-table-column label="税费" width="85" align="right">
            <template #default="{ row }">¥ {{ fmtMoney(row.taxAmount) }}</template>
          </el-table-column>
          <el-table-column label="实际收入" width="100" align="right">
            <template #default="{ row }"><b>¥ {{ fmtMoney(row.actualIncome) }}</b></template>
          </el-table-column>
          <el-table-column label="总成本" width="100" align="right">
            <template #default="{ row }">¥ {{ fmtMoney(row.totalCost) }}</template>
          </el-table-column>
          <el-table-column label="利润" width="95" align="right" fixed="right">
            <template #default="{ row }">
              <span :style="{ color: Number(row.profit) < 0 ? '#d94848' : '#2a7a4b', fontWeight: 600 }">
                ¥ {{ fmtMoney(row.profit) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="毛利率" width="82" align="right" fixed="right">
            <template #default="{ row }">
              <el-tag :type="marginTagType(row.grossMargin)" size="small">{{ fmtPct(row.grossMargin) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="核算状态" width="95" fixed="right">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.accountingStatusCn }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="回款管理" name="payments">
        <el-table :data="payments" border stripe size="small" v-loading="loading" max-height="520">
          <el-table-column prop="orderNo" label="订单" width="130" />
          <el-table-column prop="customerName" label="客户" width="120" show-overflow-tooltip />
          <el-table-column label="合同金额" width="100" align="right">
            <template #default="{ row }">¥ {{ fmtMoney(row.contractAmount) }}</template>
          </el-table-column>
          <el-table-column label="应收" width="95" align="right">
            <template #default="{ row }">¥ {{ fmtMoney(row.receivableAmount) }}</template>
          </el-table-column>
          <el-table-column label="已收" width="95" align="right">
            <template #default="{ row }">¥ {{ fmtMoney(row.receivedAmount) }}</template>
          </el-table-column>
          <el-table-column label="待收" width="95" align="right">
            <template #default="{ row }"><b>¥ {{ fmtMoney(row.pendingAmount) }}</b></template>
          </el-table-column>
          <el-table-column prop="plannedDate" label="计划回款" width="105" />
          <el-table-column prop="actualDate" label="实际回款" width="105" />
          <el-table-column label="状态" width="95">
            <template #default="{ row }">
              <el-tag :type="paymentTagType(row.paymentStatus)" size="small">{{ row.paymentStatusCn }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="应收账款" name="receivables">
        <el-table :data="receivables" border stripe size="small" v-loading="loading" max-height="420" @row-click="openReceivable">
          <el-table-column prop="customerName" label="客户" width="150" />
          <el-table-column label="欠款" width="110" align="right">
            <template #default="{ row }">¥ {{ fmtMoney(row.totalDebt) }}</template>
          </el-table-column>
          <el-table-column label="逾期金额" width="110" align="right">
            <template #default="{ row }">
              <span :style="Number(row.overdueAmount)>0?'color:#d94848;font-weight:600':''">
                ¥ {{ fmtMoney(row.overdueAmount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="overdueDays" label="逾期天数" width="90" align="center" />
          <el-table-column label="信用风险" width="95">
            <template #default="{ row }">
              <el-tag :type="riskTagType(row.creditRisk)" size="small">{{ row.creditRiskCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="催款记录" min-width="200">
            <template #default="{ row }">
              <span class="muted">{{ (row.collectionLogs || [])[0]?.note || '暂无' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="利润分析" name="profit">
        <div class="profit-grid" v-loading="loading">
          <ModulePanelSection>
            <div class="panel-title">订单利润排行</div>
            <el-table :data="profit.orderRank || []" border stripe size="small" max-height="220">
              <el-table-column prop="name" label="订单" width="120" />
              <el-table-column prop="customerName" label="客户" show-overflow-tooltip />
              <el-table-column label="利润" width="95" align="right">
                <template #default="{ row }">¥ {{ fmtMoney(row.profit) }}</template>
              </el-table-column>
              <el-table-column label="毛利率" width="80" align="right">
                <template #default="{ row }">{{ fmtPct(row.grossMargin) }}</template>
              </el-table-column>
            </el-table>
          </ModulePanelSection>
          <ModulePanelSection>
            <div class="panel-title">客户利润排行</div>
            <el-table :data="profit.customerRank || []" border stripe size="small" max-height="220">
              <el-table-column prop="name" label="客户" />
              <el-table-column label="收入" width="95" align="right">
                <template #default="{ row }">¥ {{ fmtMoney(row.income) }}</template>
              </el-table-column>
              <el-table-column label="利润" width="95" align="right">
                <template #default="{ row }">¥ {{ fmtMoney(row.profit) }}</template>
              </el-table-column>
            </el-table>
          </ModulePanelSection>
          <ModulePanelSection>
            <div class="panel-title">产品型号排行</div>
            <el-table :data="profit.productRank || []" border stripe size="small" max-height="220">
              <el-table-column prop="name" label="产品" show-overflow-tooltip />
              <el-table-column label="利润" width="95" align="right">
                <template #default="{ row }">¥ {{ fmtMoney(row.profit) }}</template>
              </el-table-column>
              <el-table-column label="毛利率" width="80" align="right">
                <template #default="{ row }">{{ fmtPct(row.grossMargin) }}</template>
              </el-table-column>
            </el-table>
          </ModulePanelSection>
          <ModulePanelSection>
            <div class="panel-title">亏损 / 低毛利预警</div>
            <el-table :data="profit.alerts || []" border stripe size="small" max-height="220">
              <el-table-column prop="name" label="订单" width="120" />
              <el-table-column prop="flagCn" label="标记" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.flag==='LOSS'?'danger':'warning'" size="small">{{ row.flagCn }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="利润" width="95" align="right">
                <template #default="{ row }">¥ {{ fmtMoney(row.profit) }}</template>
              </el-table-column>
              <el-table-column label="毛利率" width="80" align="right">
                <template #default="{ row }">{{ fmtPct(row.grossMargin) }}</template>
              </el-table-column>
            </el-table>
          </ModulePanelSection>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="recvDrawer" title="催款记录" size="380px">
      <template v-if="recvDetail">
        <p><b>{{ recvDetail.customerName }}</b> · 欠款 ¥ {{ fmtMoney(recvDetail.totalDebt) }}</p>
        <ul class="log-list">
          <li v-for="log in recvDetail.collectionLogs" :key="log.logId">
            <time>{{ log.createdAt }}</time>
            <p>{{ log.note }}</p>
            <small>{{ log.operator || '系统' }}</small>
          </li>
        </ul>
      </template>
    </el-drawer>
  </ModulePageShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import ModulePanelSection from '@/components/module/ModulePanelSection.vue'
import {
  fetchOrderRevenue, fetchFinancePayments, fetchFinanceReceivables, fetchProfitAnalysis
} from '@/api/finance'
import { fmtMoney, fmtPct, marginTagType, paymentTagType, riskTagType } from '@/constants/financeWorkflow'

const activeTab = ref('orders')
const loading = ref(false)
const orders = ref([])
const payments = ref([])
const receivables = ref([])
const profit = ref({})
const recvDrawer = ref(false)
const recvDetail = ref(null)

function openReceivable(row) {
  recvDetail.value = row
  recvDrawer.value = true
}

async function reload() {
  loading.value = true
  try {
    const [o, p, r, a] = await Promise.all([
      fetchOrderRevenue(),
      fetchFinancePayments(),
      fetchFinanceReceivables(),
      fetchProfitAnalysis()
    ])
    orders.value = o ?? []
    payments.value = p ?? []
    receivables.value = r ?? []
    profit.value = a ?? {}
  } finally { loading.value = false }
}

onMounted(reload)
</script>

<style scoped>
.page-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
.page-title { font-size:18px; font-weight:700; color:#001b3f; }
.profit-grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.panel-title { font-size:14px; font-weight:600; margin-bottom:8px; color:#2a4560; }
.muted { color:#8a9aae; font-size:12px; }
.log-list { margin:12px 0 0; padding:0; list-style:none; }
.log-list li { padding:10px 0; border-bottom:1px solid #e8edf2; }
.log-list time { font-size:11px; color:#8a9aae; }
.log-list p { margin:4px 0; font-size:13px; }
.log-list small { color:#a0aec0; }
@media (max-width: 1100px) { .profit-grid { grid-template-columns:1fr; } }
</style>
