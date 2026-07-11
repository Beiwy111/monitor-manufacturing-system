<template>
  <div class="cp-page" v-loading="loading">
    <div class="cp-head">
      <h2 class="cp-title">新建订单</h2>
      <p class="cp-sub">分步填写产品规格、数量交期、收货信息与附件后提交</p>
    </div>

    <el-steps :active="step" finish-status="success" align-center class="cp-steps">
      <el-step title="产品规格" />
      <el-step title="数量交期" />
      <el-step title="收货信息" />
      <el-step title="附件" />
      <el-step title="确认提交" />
    </el-steps>

    <div class="cp-form-panel">
      <div v-show="step === 0" class="cp-step">
        <el-form label-width="96px" size="small">
          <el-form-item label="产品" required>
            <el-select v-model="form.materialId" filterable placeholder="选择产品" style="width:360px" @change="onProductChange">
              <el-option v-for="p in products" :key="p.materialId" :label="`${p.materialName} (${p.materialCode})`" :value="p.materialId" />
            </el-select>
          </el-form-item>
          <el-form-item label="规格">
            <el-input v-model="form.specification" style="width:360px" readonly />
          </el-form-item>
          <el-form-item label="参考单价">
            <span>{{ selectedProduct?.standardCost ?? '—' }} 元 / {{ selectedProduct?.unit || '台' }}</span>
          </el-form-item>
        </el-form>
      </div>

      <div v-show="step === 1" class="cp-step">
        <el-form label-width="96px" size="small">
          <el-form-item label="订购数量" required>
            <el-input-number v-model="form.quantity" :min="1" :step="1" />
            <span class="cp-inline">{{ selectedProduct?.unit || '台' }}</span>
          </el-form-item>
          <el-form-item label="要求交期" required>
            <el-date-picker v-model="form.requiredDeliveryDate" type="date" value-format="YYYY-MM-DD" placeholder="选择交期" />
          </el-form-item>
          <el-form-item label="预估金额">
            <strong>{{ lineAmount }}</strong> 元
          </el-form-item>
        </el-form>
      </div>

      <div v-show="step === 2" class="cp-step">
        <el-form label-width="96px" size="small">
          <el-form-item label="收货人">
            <el-input v-model="form.receiverName" style="width:280px" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.receiverPhone" style="width:280px" />
          </el-form-item>
          <el-form-item label="收货地址" required>
            <el-input v-model="form.receiverAddress" type="textarea" :rows="3" style="width:480px" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" style="width:480px" />
          </el-form-item>
        </el-form>
      </div>

      <div v-show="step === 3" class="cp-step">
        <el-upload
          :http-request="handleUpload"
          list-type="picture-card"
          :file-list="fileList"
          :on-remove="onRemove"
          accept="image/*,.pdf"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <p class="cp-hint">支持 JPG、PNG、PDF，用于合同或需求说明附件</p>
      </div>

      <div v-show="step === 4" class="cp-step">
        <el-table :data="confirmRows" border size="small" class="cp-confirm-table">
          <el-table-column prop="label" label="项目" width="120" />
          <el-table-column prop="value" label="内容" />
        </el-table>
      </div>

      <div class="cp-form-actions">
        <el-button v-if="step > 0" size="small" @click="step--">上一步</el-button>
        <el-button v-if="step < 4" type="primary" size="small" @click="nextStep">下一步</el-button>
        <el-button v-else type="primary" size="small" :loading="submitting" @click="submit">提交订单</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { createCustomerOrder, getCustomerProducts, getCustomerProfile, uploadCustomerFile } from '@/api/customer'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const step = ref(0)
const products = ref([])
const fileList = ref([])
const attachmentUrls = ref([])

const form = reactive({
  materialId: null,
  productName: '',
  specification: '',
  quantity: 1,
  requiredDeliveryDate: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  remark: ''
})

const selectedProduct = computed(() => products.value.find((p) => p.materialId === form.materialId))

const lineAmount = computed(() => {
  const price = Number(selectedProduct.value?.standardCost || 0)
  const qty = Number(form.quantity || 0)
  return (price * qty).toFixed(2)
})

const confirmRows = computed(() => [
  { label: '产品', value: selectedProduct.value?.materialName || '—' },
  { label: '规格', value: form.specification || '—' },
  { label: '数量', value: `${form.quantity} ${selectedProduct.value?.unit || '台'}` },
  { label: '交期', value: form.requiredDeliveryDate || '—' },
  { label: '金额', value: `${lineAmount.value} 元` },
  { label: '收货人', value: `${form.receiverName} ${form.receiverPhone}` },
  { label: '地址', value: form.receiverAddress || '—' },
  { label: '附件', value: attachmentUrls.value.length ? `${attachmentUrls.value.length} 个` : '无' }
])

function onProductChange(id) {
  const p = products.value.find((x) => x.materialId === id)
  if (p) {
    form.productName = p.materialName
    form.specification = p.specification
  }
}

function nextStep() {
  if (step.value === 0 && !form.materialId) {
    ElMessage.warning('请选择产品')
    return
  }
  if (step.value === 1) {
    if (!form.quantity || form.quantity <= 0) {
      ElMessage.warning('请填写数量')
      return
    }
    if (!form.requiredDeliveryDate) {
      ElMessage.warning('请选择交期')
      return
    }
  }
  if (step.value === 2 && !form.receiverAddress?.trim()) {
    ElMessage.warning('请填写收货地址')
    return
  }
  step.value++
}

async function handleUpload(opt) {
  try {
    const res = await uploadCustomerFile(opt.file)
    attachmentUrls.value.push(res.url)
    fileList.value.push({ name: opt.file.name, url: res.url })
    opt.onSuccess(res)
  } catch (e) {
    opt.onError(e)
  }
}

function onRemove(file) {
  const url = file.url
  attachmentUrls.value = attachmentUrls.value.filter((u) => u !== url)
  fileList.value = fileList.value.filter((f) => f.url !== url)
}

async function submit() {
  submitting.value = true
  try {
    const res = await createCustomerOrder({
      materialId: form.materialId,
      productName: form.productName,
      specification: form.specification,
      quantity: form.quantity,
      unit: selectedProduct.value?.unit,
      requiredDeliveryDate: form.requiredDeliveryDate,
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress,
      remark: form.remark,
      attachmentUrls: attachmentUrls.value
    })
    ElMessage.success(res?.message || '提交成功')
    router.push('/customer/orders')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [plist, profile] = await Promise.all([getCustomerProducts(), getCustomerProfile()])
    products.value = plist || []
    form.receiverName = profile?.realName || ''
    form.receiverPhone = profile?.phone || ''
    form.receiverAddress = profile?.shippingAddress || ''
    const qid = Number(route.query.materialId)
    if (qid) {
      form.materialId = qid
      onProductChange(qid)
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.cp-page { padding: 12px 16px; font-weight: 400; }
.cp-head { margin-bottom: 12px; border-bottom: 1px solid #e8e8e8; padding-bottom: 10px; }
.cp-title { margin: 0; font-size: 18px; font-weight: 500; }
.cp-sub { margin: 4px 0 0; font-size: 13px; color: #666; }
.cp-steps { margin: 16px 0; }
.cp-form-panel { border: 1px solid #e8e8e8; padding: 16px; background: #fff; }
.cp-step { min-height: 200px; }
.cp-form-actions { margin-top: 16px; padding-top: 12px; border-top: 1px solid #f0f0f0; display: flex; gap: 8px; }
.cp-inline { margin-left: 8px; color: #666; }
.cp-hint { font-size: 12px; color: #999; margin-top: 8px; }
.cp-confirm-table { max-width: 640px; }
</style>
