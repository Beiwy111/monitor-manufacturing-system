<template>
  <div class="ai-order-page">
    <div class="ai-order-header">
      <span class="ai-order-title">AI识图下单</span>
      <el-tag type="info" size="small">上传微信聊天截图 → AI 提取文字 → 校正后生成客户订单</el-tag>
    </div>

    <div class="ai-order-top">
      <div class="upload-card">
        <div class="upload-card__title">上传微信截图</div>
        <div
          class="upload-zone"
          :class="{ 'upload-zone--hover': dragover }"
          @click="triggerFileInput"
          @dragover.prevent="dragover = true"
          @dragleave="dragover = false"
          @drop.prevent="onDrop"
        >
          <input ref="fileInputRef" type="file" accept=".jpg,.jpeg,.png" style="display:none" @change="onFileChange" />
          <div v-if="!selectedFile" class="upload-zone__hint">
            <div class="upload-zone__icon">💬</div>
            <div>点击或拖拽上传微信截图</div>
            <div class="upload-zone__sub">支持 JPG、PNG，建议截取含客户名称、型号、数量、交期的对话</div>
          </div>
          <div v-else class="upload-zone__selected">
            <img v-if="previewUrl" :src="previewUrl" class="upload-zone__preview" alt="预览" />
            <div class="upload-zone__filename">{{ selectedFile.name }}</div>
            <div class="upload-zone__filesize">{{ formatSize(selectedFile.size) }}</div>
          </div>
        </div>
        <el-button
          type="primary"
          :loading="parsing"
          :disabled="!selectedFile || parsing"
          style="width:100%;margin-top:10px"
          @click="doParse"
        >{{ parsing ? 'AI 识别中...' : '开始 AI 识别' }}</el-button>
      </div>

      <div class="info-card">
        <div class="info-card__title">识别状态</div>
        <el-form label-width="88px">
          <el-form-item label="文件名">
            <span class="info-value">{{ selectedFile ? selectedFile.name : '—' }}</span>
          </el-form-item>
          <el-form-item label="解析状态">
            <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
          </el-form-item>
          <el-form-item v-if="parseMeta.confidence" label="置信度">
            <el-tag :type="parseMeta.confidence >= 0.9 ? 'success' : parseMeta.confidence >= 0.7 ? 'warning' : 'danger'" size="small">
              {{ Math.round(parseMeta.confidence * 100) }}%
            </el-tag>
          </el-form-item>
        </el-form>
        <el-alert v-if="confirmResult" type="success" :closable="false" style="margin-top:12px">
          <template #title>订单已创建</template>
          <div style="margin-top:6px;font-size:13px;line-height:1.8">
            <div>订单号：<strong>{{ confirmResult.id || confirmResult.orderNo }}</strong></div>
            <div>状态：<el-tag type="info" size="small">{{ confirmResult.status || '待审核' }}</el-tag></div>
            <div style="margin-top:8px;display:flex;gap:8px;flex-wrap:wrap">
              <el-button size="small" type="primary" @click="$router.push('/order/list')">查看客户订单</el-button>
              <el-button size="small" @click="$router.push('/order/audit')">去订单审核</el-button>
            </div>
          </div>
        </el-alert>
      </div>
    </div>

    <el-alert v-if="parseError" :title="parseError" type="error" closable style="margin-bottom:14px" @close="parseError = ''" />

    <div v-if="pageState === 'parsed' || pageState === 'confirmed'" class="result-section">
      <div class="result-header">
        <span class="result-title">订单信息（可编辑）</span>
        <el-button type="success" size="small" :loading="confirming" :disabled="confirming || pageState === 'confirmed'" @click="doConfirm">
          {{ confirming ? '创建中...' : '确认生成订单' }}
        </el-button>
      </div>

      <el-form :model="form" label-width="100px" class="order-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户名称" required>
              <el-input v-model="form.customerName" placeholder="从截图识别的客户/公司名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品型号" required>
              <el-select v-model="form.productModel" filterable allow-create default-first-option style="width:100%" placeholder="选择或输入型号">
                <el-option v-for="m in productOptions" :key="m.code" :label="`${m.name}（${m.code}）`" :value="m.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="面板类型">
              <el-input v-model="form.panelType" placeholder="LCD / OLED" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订购数量" required>
              <el-input-number v-model="form.quantity" :min="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="要求交期">
              <el-date-picker v-model="form.deliveryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="选择日期" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单金额">
              <el-input-number v-model="form.amount" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="特殊要求、聊天摘要等" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div v-if="rawText" class="raw-text">
        <span class="raw-text__label">识别原文摘要：</span>{{ rawText }}
      </div>
    </div>

    <div v-if="pageState === 'init'" class="empty-state">
      <div class="empty-state__icon">📱</div>
      <div class="empty-state__text">请上传客户微信下单截图，点击「开始 AI 识别」自动提取订单信息</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { parseOrderWechatScreenshot } from '@/api/business'

const mes = useMesStore()
const userStore = useUserStore()

const fileInputRef = ref(null)
const selectedFile = ref(null)
const previewUrl = ref('')
const dragover = ref(false)
const parsing = ref(false)
const confirming = ref(false)
const parseError = ref('')
const pageState = ref('init')
const confirmResult = ref(null)
const rawText = ref('')
const parseMeta = reactive({ confidence: null, engine: '' })

const defaultDelivery = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10)
const form = reactive({
  customerName: '',
  contactPhone: '',
  productModel: '',
  panelType: 'LCD',
  quantity: 1,
  deliveryDate: defaultDelivery,
  amount: 0,
  remark: ''
})

const productOptions = computed(() => mes.productModels || [])

const statusLabel = computed(() => {
  const map = { init: '待上传', parsing: 'AI识别中', parsed: '识别成功', confirmed: '已生成订单' }
  return map[pageState.value] || '-'
})
const statusTagType = computed(() => {
  const map = { init: 'info', parsing: 'warning', parsed: 'success', confirmed: 'primary' }
  return map[pageState.value] || 'info'
})

function triggerFileInput() { fileInputRef.value?.click() }

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (file) selectFile(file)
  e.target.value = ''
}

function onDrop(e) {
  dragover.value = false
  const file = e.dataTransfer.files?.[0]
  if (file) selectFile(file)
}

function selectFile(file) {
  const ext = file.name.split('.').pop().toLowerCase()
  if (!['jpg', 'jpeg', 'png'].includes(ext)) {
    ElMessage.error('仅支持 JPG、PNG 格式图片')
    return
  }
  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
  pageState.value = 'init'
  confirmResult.value = null
  parseError.value = ''
  rawText.value = ''
  parseMeta.confidence = null
  parseMeta.engine = ''
  resetForm()
}

function resetForm() {
  form.customerName = ''
  form.contactPhone = ''
  form.productModel = productOptions.value[0]?.name || ''
  form.panelType = productOptions.value[0]?.panelType || 'LCD'
  form.quantity = 1
  form.deliveryDate = defaultDelivery
  form.amount = 0
  form.remark = ''
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function matchProductModel(text) {
  if (!text) return productOptions.value[0]?.name || ''
  const lower = text.toLowerCase()
  const exact = productOptions.value.find((m) => m.name === text || m.code === text)
  if (exact) return exact.name
  const partial = productOptions.value.find((m) =>
    lower.includes(String(m.name || '').toLowerCase()) ||
    lower.includes(String(m.code || '').toLowerCase()) ||
    String(m.name || '').toLowerCase().includes(lower)
  )
  return partial?.name || text
}

async function doParse() {
  if (!selectedFile.value) return
  parsing.value = true
  parseError.value = ''
  pageState.value = 'parsing'
  try {
    const result = await parseOrderWechatScreenshot(selectedFile.value)
    const fields = result.fields || {}
    form.customerName = fields.customerName || ''
    form.contactPhone = fields.contactPhone || ''
    form.productModel = matchProductModel(fields.productModel)
    form.quantity = fields.quantity > 0 ? fields.quantity : 1
    form.deliveryDate = fields.deliveryDate || defaultDelivery
    form.amount = fields.amount > 0 ? fields.amount : 0
    form.remark = fields.remark || ''
    rawText.value = result.rawText || fields.rawText || ''
    parseMeta.confidence = result.confidence ?? null
    parseMeta.engine = result.engine || ''
    pageState.value = 'parsed'
    ElMessage.success('AI 识别成功，请核对后生成订单')
  } catch (e) {
    parseError.value = e?.message || 'AI 识别失败，请重试'
    pageState.value = 'init'
  } finally {
    parsing.value = false
  }
}

async function doConfirm() {
  if (!form.customerName?.trim()) {
    ElMessage.warning('请填写客户名称')
    return
  }
  if (!form.productModel?.trim()) {
    ElMessage.warning('请填写产品型号')
    return
  }
  if (!form.quantity || form.quantity <= 0) {
    ElMessage.warning('订购数量必须大于 0')
    return
  }

  const remarkParts = [form.remark]
  if (form.contactPhone) remarkParts.push(`联系电话：${form.contactPhone}`)
  if (rawText.value) remarkParts.push(`微信截图识别：${rawText.value}`)

  confirming.value = true
  try {
    const res = await mes.createOrder(
      {
        customerName: form.customerName.trim(),
        productModel: form.productModel.trim(),
        panelType: form.panelType || 'LCD',
        quantity: form.quantity,
        deliveryDate: form.deliveryDate,
        amount: form.amount,
        remark: remarkParts.filter(Boolean).join('；')
      },
      userStore.username,
      userStore.roleKey
    )
    confirmResult.value = res || { status: '待审核' }
    pageState.value = 'confirmed'
    ElMessage.success('订单已创建：' + (res?.id || res?.orderNo || ''))
  } catch (e) {
    ElMessage.error(e?.message || '创建订单失败')
  } finally {
    confirming.value = false
  }
}
</script>

<style scoped>
.ai-order-page { padding: 16px 20px; background: #f5f7fa; min-height: 100%; }
.ai-order-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.ai-order-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.ai-order-top { display: flex; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.upload-card { background: #fff; border-radius: 8px; padding: 16px; width: 280px; min-width: 240px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.upload-card__title { font-size: 14px; font-weight: 600; color: #001b3f; margin-bottom: 10px; }
.upload-zone { border: 2px dashed #d9d9d9; border-radius: 6px; min-height: 160px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: border-color .2s; padding: 12px; }
.upload-zone--hover { border-color: #4096ff; background: #f0f7ff; }
.upload-zone:hover { border-color: #4096ff; }
.upload-zone__hint { text-align: center; color: #8c8c8c; font-size: 13px; line-height: 1.8; }
.upload-zone__icon { font-size: 28px; margin-bottom: 6px; }
.upload-zone__sub { font-size: 12px; color: #bfbfbf; max-width: 220px; margin: 0 auto; }
.upload-zone__selected { text-align: center; }
.upload-zone__preview { max-width: 200px; max-height: 120px; border-radius: 4px; margin-bottom: 6px; object-fit: contain; }
.upload-zone__filename { font-size: 13px; color: #001b3f; font-weight: 500; word-break: break-all; }
.upload-zone__filesize { font-size: 12px; color: #8c8c8c; }
.info-card { background: #fff; border-radius: 8px; padding: 16px; flex: 1; min-width: 280px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.info-card__title { font-size: 14px; font-weight: 600; color: #001b3f; margin-bottom: 12px; }
.info-value { font-size: 13px; color: #606266; }
.result-section { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.result-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; gap: 8px; }
.result-title { font-size: 15px; font-weight: 600; color: #001b3f; }
.order-form { max-width: 900px; }
.raw-text { margin-top: 12px; padding: 10px 12px; background: #fafafa; border-radius: 6px; font-size: 13px; color: #606266; line-height: 1.6; }
.raw-text__label { color: #909399; }
.empty-state { text-align: center; padding: 80px 0; color: #8c8c8c; }
.empty-state__icon { font-size: 48px; margin-bottom: 12px; }
.empty-state__text { font-size: 14px; }
</style>
