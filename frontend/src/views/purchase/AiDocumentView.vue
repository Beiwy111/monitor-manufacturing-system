<template>
  <div class="ai-doc-page">
    <div class="ai-doc-header">
      <span class="ai-doc-title">AI 单据录入</span>
      <el-tag type="info" size="small">上传采购单据图片 → AI 解析 → 人工校正 → 确认录入</el-tag>
    </div>

    <div class="ai-doc-top">
      <div class="upload-card">
        <div class="upload-card__title">上传单据图片</div>
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
            <div class="upload-zone__icon">📄</div>
            <div>点击或拖拽上传</div>
            <div class="upload-zone__sub">支持 JPG、PNG 格式</div>
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
        >{{ parsing ? 'AI 解析中...' : '开始 AI 解析' }}</el-button>
      </div>

      <div class="info-card">
        <div class="info-card__title">单据基础信息</div>
        <el-form :model="baseInfo" label-width="90px">
          <el-form-item label="供应商">
            <el-input v-model="baseInfo.supplierName" placeholder="AI 解析后自动填入" :disabled="pageState === 'init'" />
          </el-form-item>
          <el-form-item label="付款方式">
            <el-input v-model="baseInfo.paymentMethod" placeholder="AI 解析后自动填入" :disabled="pageState === 'init'" />
          </el-form-item>
          <el-form-item label="文件名">
            <span class="info-value">{{ selectedFile ? selectedFile.name : '—' }}</span>
          </el-form-item>
          <el-form-item label="解析状态">
            <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
          </el-form-item>
        </el-form>
        <el-alert v-if="confirmResult" type="success" :closable="false" style="margin-top:12px">
          <template #title>采购单草稿已生成</template>
          <div style="margin-top:6px;font-size:13px;line-height:1.8">
            <div>采购单号：<strong>{{ confirmResult.purchaseOrderNo }}</strong></div>
            <div>状态：<el-tag type="info" size="small">{{ confirmResult.status }}</el-tag></div>
            <div style="margin-top:8px">
              <el-button size="small" type="primary" @click="$router.push('/purchase/order')">查看采购订单</el-button>
            </div>
          </div>
        </el-alert>
      </div>
    </div>

    <el-alert v-if="parseError" :title="parseError" type="error" closable style="margin-bottom:14px" @close="parseError = ''" />

    <div v-if="pageState === 'parsed' || pageState === 'confirmed'" class="result-section">
      <div class="result-header">
        <span class="result-title">解析结果（可编辑）</span>
        <div>
          <el-button size="small" @click="addRow">新增行</el-button>
          <el-button type="success" size="small" :loading="confirming" :disabled="confirming || pageState === 'confirmed'" @click="doConfirm">确认录入</el-button>
        </div>
      </div>
      <el-table :data="items" border stripe style="width:100%">
        <el-table-column label="物料编码" width="120">
          <template #default="{ row }"><el-input v-model="row.materialCode" size="small" placeholder="选填" /></template>
        </el-table-column>
        <el-table-column label="物料名称 *" min-width="130">
          <template #default="{ row }">
            <el-input v-model="row.materialName" size="small" placeholder="必填" />
            <div v-if="row._nameError" class="cell-error">物料名称必填</div>
          </template>
        </el-table-column>
        <el-table-column label="规格型号" width="130">
          <template #default="{ row }"><el-input v-model="row.specification" size="small" /></template>
        </el-table-column>
        <el-table-column label="单价" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.unitPrice" size="small" :min="0" :precision="2" controls-position="right" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column label="数量 *" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" size="small" :min="0.01" :precision="2" controls-position="right" style="width:100%" />
            <div v-if="row._qtyError" class="cell-error">数量必须大于0</div>
          </template>
        </el-table-column>
        <el-table-column label="交期" width="130">
          <template #default="{ row }">
            <el-date-picker v-model="row.deliveryDate" type="date" size="small" value-format="YYYY-MM-DD" style="width:100%" placeholder="选择日期" />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="100">
          <template #default="{ row }"><el-input v-model="row.remark" size="small" /></template>
        </el-table-column>
        <el-table-column label="置信度" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.confidence != null" :type="row.confidence >= 0.9 ? 'success' : row.confidence >= 0.7 ? 'warning' : 'danger'" size="small">
              {{ (row.confidence * 100).toFixed(0) }}%
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="65" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="removeRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="confirm-bar">
        <el-button type="success" :loading="confirming" :disabled="confirming || pageState === 'confirmed'" @click="doConfirm">
          {{ confirming ? '录入中...' : '确认录入为采购单草稿' }}
        </el-button>
        <span class="confirm-bar__tip">共 {{ items.length }} 行物料</span>
      </div>
    </div>

    <div v-if="pageState === 'init'" class="empty-state">
      <div class="empty-state__icon">🧾</div>
      <div class="empty-state__text">请在左上角上传采购单据图片，点击「开始 AI 解析」</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { parseAiDocument, confirmAiDocument } from '@/api/business'

const fileInputRef = ref(null)
const selectedFile = ref(null)
const previewUrl = ref('')
const dragover = ref(false)
const parsing = ref(false)
const confirming = ref(false)
const parseError = ref('')
const pageState = ref('init') // init | parsing | parsed | confirmed
const confirmResult = ref(null)

const baseInfo = reactive({ supplierName: '', paymentMethod: '' })
const items = ref([])

const statusLabel = computed(() => {
  const map = { init: '待上传', parsing: 'AI解析中', parsed: '解析成功', confirmed: '已录入' }
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
  items.value = []
  confirmResult.value = null
  parseError.value = ''
  baseInfo.supplierName = ''
  baseInfo.paymentMethod = ''
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

async function doParse() {
  if (!selectedFile.value) return
  parsing.value = true
  parseError.value = ''
  pageState.value = 'parsing'
  try {
    const result = await parseAiDocument(selectedFile.value)
    baseInfo.supplierName = result.supplierName || ''
    baseInfo.paymentMethod = result.paymentMethod || ''
    items.value = (result.items || []).map(i => ({ ...i, _nameError: false, _qtyError: false }))
    pageState.value = 'parsed'
    ElMessage.success('AI 解析成功，共识别 ' + items.value.length + ' 行物料')
  } catch (e) {
    parseError.value = e?.message || 'AI 解析失败，请重试'
    pageState.value = 'init'
  } finally {
    parsing.value = false
  }
}

function addRow() {
  items.value.push({ materialCode: '', materialName: '', specification: '', unitPrice: null, quantity: null, deliveryDate: '', remark: '', confidence: null, _nameError: false, _qtyError: false })
}

function removeRow(index) { items.value.splice(index, 1) }

async function doConfirm() {
  if (items.value.length === 0) { ElMessage.warning('至少需要一条物料行'); return }
  let hasError = false
  items.value.forEach(row => {
    row._nameError = !row.materialName
    row._qtyError = !row.quantity || row.quantity <= 0
    if (row._nameError || row._qtyError) hasError = true
  })
  if (hasError) { ElMessage.error('请填写必填字段：物料名称、数量'); return }

  confirming.value = true
  try {
    const payload = {
      supplierName: baseInfo.supplierName,
      paymentMethod: baseInfo.paymentMethod,
      items: items.value.map(({ materialCode, materialName, specification, unitPrice, quantity, deliveryDate, remark }) =>
        ({ materialCode, materialName, specification, unitPrice, quantity, deliveryDate, remark })
      )
    }
    const res = await confirmAiDocument(payload)
    confirmResult.value = res
    pageState.value = 'confirmed'
    ElMessage.success('采购单草稿已生成：' + res.purchaseOrderNo)
  } catch (e) {
    ElMessage.error(e?.message || '录入失败，请重试')
  } finally {
    confirming.value = false
  }
}
</script>

<style scoped>
.ai-doc-page { padding: 16px 20px; background: #f5f7fa; min-height: 100%; }
.ai-doc-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.ai-doc-title { font-size: 18px; font-weight: 700; color: #001b3f; }
.ai-doc-top { display: flex; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.upload-card { background: #fff; border-radius: 8px; padding: 16px; width: 260px; min-width: 220px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.upload-card__title { font-size: 14px; font-weight: 600; color: #001b3f; margin-bottom: 10px; }
.upload-zone { border: 2px dashed #d9d9d9; border-radius: 6px; min-height: 140px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: border-color .2s; padding: 12px; }
.upload-zone--hover { border-color: #4096ff; background: #f0f7ff; }
.upload-zone:hover { border-color: #4096ff; }
.upload-zone__hint { text-align: center; color: #8c8c8c; font-size: 13px; line-height: 1.8; }
.upload-zone__icon { font-size: 28px; margin-bottom: 6px; }
.upload-zone__sub { font-size: 12px; color: #bfbfbf; }
.upload-zone__selected { text-align: center; }
.upload-zone__preview { max-width: 180px; max-height: 100px; border-radius: 4px; margin-bottom: 6px; object-fit: contain; }
.upload-zone__filename { font-size: 13px; color: #001b3f; font-weight: 500; word-break: break-all; }
.upload-zone__filesize { font-size: 12px; color: #8c8c8c; }
.info-card { background: #fff; border-radius: 8px; padding: 16px; flex: 1; min-width: 280px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.info-card__title { font-size: 14px; font-weight: 600; color: #001b3f; margin-bottom: 12px; }
.info-value { font-size: 13px; color: #606266; }
.result-section { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.result-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; gap: 8px; }
.result-title { font-size: 15px; font-weight: 600; color: #001b3f; }
.cell-error { color: #f56c6c; font-size: 11px; line-height: 1.4; }
.confirm-bar { display: flex; align-items: center; gap: 16px; margin-top: 14px; padding-top: 14px; border-top: 1px solid #f0f0f0; }
.confirm-bar__tip { font-size: 13px; color: #8c8c8c; }
.empty-state { text-align: center; padding: 80px 0; color: #8c8c8c; }
.empty-state__icon { font-size: 48px; margin-bottom: 12px; }
.empty-state__text { font-size: 14px; }
</style>
