<template>
  <div class="ruoyi-page order-audit-page">
    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">订单审核</span>
      <el-input v-model="keyword" clearable placeholder="订单号 / 客户" style="width: 220px" />
      <el-button @click="refresh">刷新</el-button>
    </div>

    <div class="order-audit-layout">
      <div class="order-audit-layout__table ruoyi-table-wrap">
        <el-table
          :data="filteredOrders"
          border
          stripe
          highlight-current-row
          :current-row-key="selected?.id"
          row-key="id"
          @current-change="onRowSelect"
        >
          <el-table-column prop="id" label="订单号" width="130" />
          <el-table-column prop="customerName" label="客户" min-width="120" show-overflow-tooltip />
          <el-table-column prop="productModel" label="型号" min-width="130" show-overflow-tooltip />
          <el-table-column prop="quantity" label="数量" width="72" align="right" />
          <el-table-column label="金额" width="100" align="right">
            <template #default="{ row }">{{ formatCurrency(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="deliveryDate" label="交期" width="110" />
          <el-table-column label="风险" width="72" align="center">
            <template #default="{ row }">
              <el-tag v-if="riskCountOf(row)" :type="riskTagType(row)" size="small">{{ riskCountOf(row) }}</el-tag>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><StatusBadge :status="row.status" /></template>
          </el-table-column>
        </el-table>
      </div>

      <div class="order-audit-layout__detail">
        <template v-if="selected">
          <div class="ruoyi-detail__head order-audit-detail-head">
            <div>
              <span class="ruoyi-detail__title">{{ selected.id }}</span>
              <span class="order-audit-detail-head__sub">{{ selected.customerName }} · {{ selected.productModel }}</span>
            </div>
            <div class="order-audit-detail-head__tags">
              <StatusBadge :status="selected.status" />
              <el-tag v-if="selected.auditFlag" type="warning" size="small">{{ selected.auditFlag }}</el-tag>
            </div>
          </div>

          <div v-if="risks.length" class="order-audit-risks">
            <el-tag
              v-for="risk in risks"
              :key="`${risk.code}-${risk.label}`"
              :type="risk.level === 'danger' ? 'danger' : risk.level === 'warning' ? 'warning' : 'info'"
              size="small"
              class="order-audit-risks__tag"
            >
              {{ risk.label }}：{{ risk.detail }}
            </el-tag>
          </div>

          <el-tabs v-model="activeTab" class="order-audit-tabs">
            <el-tab-pane label="订单详情" name="detail">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="订单号">{{ selected.id }}</el-descriptions-item>
                <el-descriptions-item label="客户">{{ selected.customerName }}</el-descriptions-item>
                <el-descriptions-item label="产品型号">{{ selected.productModel }}</el-descriptions-item>
                <el-descriptions-item label="物料编码">{{ selected.materialCode || '—' }}</el-descriptions-item>
                <el-descriptions-item label="规格" :span="2">{{ selected.specification || '—' }}</el-descriptions-item>
                <el-descriptions-item label="数量">{{ selected.quantity }} 台</el-descriptions-item>
                <el-descriptions-item label="单价">{{ formatCurrency(selected.unitPrice) }}</el-descriptions-item>
                <el-descriptions-item label="订单金额">{{ formatCurrency(selected.amount) }}</el-descriptions-item>
                <el-descriptions-item label="要求交期">{{ selected.deliveryDate || '—' }}</el-descriptions-item>
                <el-descriptions-item label="销售员">{{ selected.salesPerson || '—' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ selected.createdAt || '—' }}</el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{ selected.remark || '—' }}</el-descriptions-item>
              </el-descriptions>

              <el-divider content-position="left">BOM 清单</el-divider>
              <el-table v-if="bomProduct?.components?.length" :data="bomProduct.components" border stripe size="small">
                <el-table-column prop="assemblyGroup" label="组件类别" width="120" />
                <el-table-column prop="materialCode" label="物料编码" width="110" />
                <el-table-column prop="materialName" label="物料名称" min-width="140" />
                <el-table-column prop="specification" label="规格" min-width="120" show-overflow-tooltip />
                <el-table-column prop="quantity" label="用量" width="72" align="right" />
                <el-table-column prop="unit" label="单位" width="60" />
              </el-table>
              <el-empty v-else description="未找到 BOM 配置" :image-size="64" />
            </el-tab-pane>

            <el-tab-pane label="审核检查" name="checklist">
              <el-table :data="checklist" border stripe size="small">
                <el-table-column prop="label" label="检查项" width="140" />
                <el-table-column label="结果" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="checkTagType(row.status)" size="small">
                      {{ row.status === 'pass' ? '通过' : row.status === 'warn' ? '提示' : '不通过' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="message" label="说明" min-width="260" show-overflow-tooltip />
              </el-table>

              <el-divider content-position="left">智能规则审核</el-divider>
              <el-table :data="risks" border stripe size="small">
                <el-table-column label="级别" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.level === 'danger' ? 'danger' : row.level === 'warning' ? 'warning' : 'info'" size="small">
                      {{ row.level === 'danger' ? '高' : row.level === 'warning' ? '中' : '低' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="label" label="规则" width="120" />
                <el-table-column prop="detail" label="说明" min-width="280" show-overflow-tooltip />
              </el-table>
              <el-empty v-if="!risks.length" description="未发现风险项" :image-size="64" />
            </el-tab-pane>

            <el-tab-pane label="订单附件" name="attachment">
              <template v-if="attachments.length">
                <el-table :data="attachments" border stripe size="small" highlight-current-row @current-change="onAttachmentSelect">
                  <el-table-column prop="label" label="附件名称" min-width="140" />
                  <el-table-column prop="type" label="类型" width="100">
                    <template #default="{ row }">{{ attachmentTypeLabel(row.type) }}</template>
                  </el-table-column>
                  <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
                  <el-table-column label="操作" width="100">
                    <template #default="{ row }">
                      <el-button link type="primary" @click="runOcr(row)">OCR识别</el-button>
                    </template>
                  </el-table-column>
                </el-table>

                <div v-if="activeAttachment" class="order-audit-attachment-preview">
                  <div class="order-audit-attachment-preview__title">附件预览</div>
                  <img :src="activeAttachment.url" :alt="activeAttachment.label" class="order-audit-attachment-preview__img" />
                </div>

                <template v-if="ocrResult">
                  <el-divider content-position="left">OCR 识别结果（{{ ocrResult.engine }} · 置信度 {{ Math.round((ocrResult.confidence || 0) * 100) }}%）</el-divider>
                  <el-table :data="ocrCompareRows" border stripe size="small">
                    <el-table-column prop="label" label="字段" width="100" />
                    <el-table-column label="系统订单" min-width="120">
                      <template #default="{ row }">{{ row.order ?? '—' }}</template>
                    </el-table-column>
                    <el-table-column label="OCR识别" min-width="120">
                      <template #default="{ row }">{{ row.ocr ?? '—' }}</template>
                    </el-table-column>
                    <el-table-column label="比对" width="80" align="center">
                      <template #default="{ row }">
                        <el-tag :type="row.match ? 'success' : 'danger'" size="small">{{ row.match ? '一致' : '不一致' }}</el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                  <div v-if="ocrResult.rawText" class="order-audit-note">识别原文：{{ ocrResult.rawText }}</div>
                </template>
              </template>
              <el-empty v-else description="该订单无附件，可直接依据系统字段审核" :image-size="80" />
            </el-tab-pane>

            <el-tab-pane label="审核记录" name="timeline">
              <el-timeline v-if="auditRecords.length">
                <el-timeline-item
                  v-for="record in auditRecords"
                  :key="record.id"
                  :timestamp="record.createdAt"
                  placement="top"
                >
                  <div class="order-audit-timeline__title">{{ record.actionLabel || record.action }}</div>
                  <div class="order-audit-timeline__meta">{{ record.operatorName || record.operator }}</div>
                  <div v-if="record.reason" class="order-audit-timeline__reason">{{ record.reason }}</div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无审核记录" :image-size="64" />
            </el-tab-pane>
          </el-tabs>

          <div v-if="selected.status === '待审核'" class="order-audit-actions">
            <el-button
              v-for="act in AUDIT_ACTIONS"
              :key="act.action"
              :type="act.type"
              size="small"
              @click="openAuditDialog(act)"
            >
              {{ act.label }}
            </el-button>
            <el-button type="danger" size="small" plain @click="removeOrder(selected)">删除订单</el-button>
          </div>
        </template>

        <div v-else class="ruoyi-detail__empty">请在左侧选择待审核订单</div>
      </div>
    </div>

    <el-dialog v-model="auditDialogVisible" :title="auditDialogTitle" width="480px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item v-if="pendingAction?.needReason" label="原因" required>
          <el-input v-model="auditReason" type="textarea" :rows="4" placeholder="请填写审核原因或补充说明" />
        </el-form-item>
        <el-form-item v-else label="说明">
          <span class="text-muted">审核通过后将进入计划员待办</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditing" @click="confirmAudit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesDelete } from '@/composables/useMesDelete'
import { postOrderOcrRecognize } from '@/api/mes'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import {
  AUDIT_ACTIONS,
  buildAuditChecklist,
  compareOcrWithOrder,
  detectAuditRisks,
  findBomProduct,
  formatCurrency
} from '@/utils/orderAudit'

const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)

const keyword = ref('')
const selected = ref(null)
const activeTab = ref('detail')
const activeAttachment = ref(null)
const ocrResult = ref(null)
const ocrLoading = ref(false)
const auditDialogVisible = ref(false)
const pendingAction = ref(null)
const auditReason = ref('')
const auditing = ref(false)

const pendingOrders = computed(() => mes.orders.filter((o) => o.status === '待审核'))

const filteredOrders = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return pendingOrders.value
  return pendingOrders.value.filter((o) =>
    [o.id, o.customerName, o.productModel].some((v) => String(v || '').toLowerCase().includes(kw))
  )
})

const bomProduct = computed(() => (selected.value ? findBomProduct(selected.value, mes.bomGuide) : null))
const checklist = computed(() =>
  selected.value ? buildAuditChecklist(selected.value, mes.bomGuide, mes.processGuide) : []
)
const risks = computed(() =>
  selected.value ? detectAuditRisks(selected.value, mes.bomGuide, mes.processGuide) : []
)
const attachments = computed(() => selected.value?.attachments || [])
const auditRecords = computed(() => selected.value?.auditRecords || [])
const ocrCompareRows = computed(() =>
  selected.value && ocrResult.value?.fields
    ? compareOcrWithOrder(selected.value, ocrResult.value.fields)
    : []
)
const auditDialogTitle = computed(() => pendingAction.value?.label || '订单审核')

watch(selected, () => {
  activeTab.value = 'detail'
  activeAttachment.value = attachments.value[0] || null
  ocrResult.value = null
})

function riskCountOf(row) {
  return detectAuditRisks(row, mes.bomGuide, mes.processGuide).length
}

function riskTagType(row) {
  const list = detectAuditRisks(row, mes.bomGuide, mes.processGuide)
  if (list.some((r) => r.level === 'danger')) return 'danger'
  if (list.some((r) => r.level === 'warning')) return 'warning'
  return 'info'
}

function checkTagType(status) {
  if (status === 'pass') return 'success'
  if (status === 'warn') return 'warning'
  return 'danger'
}

function attachmentTypeLabel(type) {
  return { contract: '合同', purchase: '采购单', spec: '规格书' }[type] || type || '—'
}

function onRowSelect(row) {
  selected.value = row || null
}

function onAttachmentSelect(row) {
  activeAttachment.value = row || null
  ocrResult.value = null
}

async function refresh() {
  await mes.hydrateForPage()
  if (selected.value) {
    selected.value = mes.orders.find((o) => o.id === selected.value.id) || null
  }
}

async function runOcr(file) {
  if (!selected.value || !file) return
  activeAttachment.value = file
  ocrLoading.value = true
  try {
    const res = await postOrderOcrRecognize({
      orderId: selected.value.id,
      fileName: file.fileName,
      operator: userStore.username,
      roleKey: userStore.roleKey
    })
    ocrResult.value = res
    ElMessage.success('OCR 识别完成（模拟）')
  } catch {
    ElMessage.error('OCR 识别失败')
  } finally {
    ocrLoading.value = false
  }
}

function openAuditDialog(action) {
  pendingAction.value = action
  auditReason.value = ''
  auditDialogVisible.value = true
}

async function confirmAudit() {
  if (!selected.value || !pendingAction.value) return
  if (pendingAction.value.needReason && !auditReason.value.trim()) {
    ElMessage.warning('请填写审核原因')
    return
  }
  auditing.value = true
  try {
    const ok = await mes.auditOrder(
      selected.value.id,
      { action: pendingAction.value.action, reason: auditReason.value.trim() },
      userStore.username,
      userStore.roleKey
    )
    if (!ok) {
      ElMessage.error('审核未生效，请刷新后重试（若后端未重启，请联系管理员）')
      return
    }
    const msg = {
      pass: '审核通过，订单已进入计划员待办',
      reject: '订单已驳回作废',
      supplement: '已标记为待补充资料',
      defer: '已暂缓审核'
    }[pendingAction.value.action]
    ElMessage.success(msg)
    auditDialogVisible.value = false
    selected.value = null
    await mes.hydrateFromApi({ force: true })
  } finally {
    auditing.value = false
  }
}

function removeOrder(row) {
  if (!row) return
  runDelete({
    action: 'deleteOrder',
    payload: { orderId: row.id },
    message: `确定删除订单 ${row.id}？关联计划、工单等记录将一并删除。`,
    onSuccess: () => {
      if (selected.value?.id === row.id) selected.value = null
    }
  }).catch(() => {})
}
</script>

<style scoped>
.order-audit-page {
  min-height: 100%;
}

.order-audit-layout {
  display: grid;
  grid-template-columns: minmax(420px, 1fr) minmax(480px, 1.1fr);
  gap: 12px;
  align-items: start;
}

.order-audit-layout__detail {
  border: 1px solid var(--el-border-color-light);
  background: transparent;
  padding: 12px 14px;
  min-height: 520px;
}

.order-audit-detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.order-audit-detail-head__sub {
  display: block;
  margin-top: 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.order-audit-detail-head__tags {
  display: flex;
  gap: 6px;
  align-items: center;
}

.order-audit-risks {
  margin-bottom: 10px;
}

.order-audit-risks__tag {
  margin: 0 6px 6px 0;
  max-width: 100%;
  height: auto;
  white-space: normal;
  line-height: 1.4;
}

.order-audit-tabs :deep(.el-tabs__content) {
  padding-top: 8px;
}

.order-audit-note {
  margin-top: 8px;
  padding: 8px 10px;
  background: var(--el-fill-color-light);
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
}

.order-audit-attachment-preview {
  margin-top: 12px;
}

.order-audit-attachment-preview__title {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

.order-audit-attachment-preview__img {
  max-width: 100%;
  max-height: 220px;
  border: 1px solid var(--el-border-color-lighter);
  background: #fafafa;
}

.order-audit-actions {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.order-audit-timeline__title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.order-audit-timeline__meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.order-audit-timeline__reason {
  margin-top: 4px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.text-muted {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

@media (max-width: 1200px) {
  .order-audit-layout {
    grid-template-columns: 1fr;
  }
}
</style>
