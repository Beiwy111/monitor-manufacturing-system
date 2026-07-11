<template>
  <div class="cp-page" v-loading="loading">
    <div class="cp-head">
      <h2 class="cp-title">提交反馈</h2>
      <p class="cp-sub">关联订单与序列号，描述问题并上传图片</p>
    </div>

    <el-form :model="form" label-width="96px" size="small" class="cp-form-panel">
      <el-form-item label="关联订单">
        <el-select v-model="form.orderId" clearable filterable placeholder="可选" style="width:320px">
          <el-option v-for="o in orders" :key="o.orderId" :label="`${o.orderNo} (${o.currentStage})`" :value="o.orderId" />
        </el-select>
      </el-form-item>
      <el-form-item label="序列号/批次">
        <el-input v-model="form.serialNo" placeholder="产品序列号或批次号" style="width:320px" />
      </el-form-item>
      <el-form-item label="问题类型" required>
        <el-select v-model="form.problemType" placeholder="选择类型" style="width:240px">
          <el-option v-for="t in problemTypes" :key="t" :label="t" :value="t" />
        </el-select>
      </el-form-item>
      <el-form-item label="问题描述" required>
        <el-input v-model="form.problemDescription" type="textarea" :rows="4" style="width:480px" />
      </el-form-item>
      <el-form-item label="图片附件">
        <el-upload :http-request="handleUpload" list-type="picture-card" :file-list="fileList" :on-remove="onRemove" accept="image/*">
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交反馈</el-button>
        <el-button @click="$router.push('/customer/feedback/list')">查看我的反馈</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getCustomerOrders, submitCustomerFeedback, uploadCustomerFile } from '@/api/customer'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const orders = ref([])
const fileList = ref([])
const attachmentUrls = ref([])

const problemTypes = ['显示异常', '性能问题', '外观缺陷', '物流损坏', '咨询服务', '其他']

const form = reactive({
  orderId: null,
  serialNo: '',
  problemType: '',
  problemDescription: ''
})

async function handleUpload(opt) {
  const res = await uploadCustomerFile(opt.file)
  attachmentUrls.value.push(res.url)
  fileList.value.push({ name: opt.file.name, url: res.url })
  opt.onSuccess(res)
}

function onRemove(file) {
  attachmentUrls.value = attachmentUrls.value.filter((u) => u !== file.url)
  fileList.value = fileList.value.filter((f) => f.url !== file.url)
}

async function submit() {
  if (!form.problemType) {
    ElMessage.warning('请选择问题类型')
    return
  }
  if (!form.problemDescription?.trim()) {
    ElMessage.warning('请填写问题描述')
    return
  }
  submitting.value = true
  try {
    const res = await submitCustomerFeedback({
      orderId: form.orderId,
      serialNo: form.serialNo,
      problemType: form.problemType,
      problemDescription: form.problemDescription,
      attachmentUrls: attachmentUrls.value
    })
    ElMessage.success(res?.message || '提交成功')
    router.push('/customer/feedback/list')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    orders.value = await getCustomerOrders() || []
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
.cp-form-panel { border: 1px solid #e8e8e8; padding: 16px; background: #fff; max-width: 720px; }
</style>
