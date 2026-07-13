<template>
  <div class="cp-page" v-loading="loading">
    <div class="cp-head">
      <h2 class="cp-title">个人中心</h2>
      <p class="cp-sub">账户信息与默认收货地址</p>
    </div>

    <el-form :model="form" label-width="96px" size="small" class="cp-form-panel">
      <el-form-item label="登录账号">
        <el-input v-model="form.username" disabled style="width:280px" />
      </el-form-item>
      <el-form-item label="企业名称">
        <el-input v-model="form.customerName" disabled style="width:360px" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="form.realName" style="width:280px" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" style="width:280px" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" style="width:360px" />
      </el-form-item>
      <el-form-item label="默认地址">
        <el-input v-model="form.shippingAddress" type="textarea" :rows="3" style="width:480px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCustomerProfile, updateCustomerProfile } from '@/api/customer'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  username: '',
  customerName: '',
  realName: '',
  phone: '',
  email: '',
  shippingAddress: ''
})

async function load() {
  loading.value = true
  try {
    const p = await getCustomerProfile()
    Object.assign(form, p || {})
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await updateCustomerProfile({
      realName: form.realName,
      phone: form.phone,
      email: form.email,
      shippingAddress: form.shippingAddress
    })
    await userStore.refreshUserInfo()
    ElMessage.success('已保存')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.cp-page { padding: 12px 16px; font-weight: 400; }
.cp-head { margin-bottom: 12px; border-bottom: 1px solid #e8e8e8; padding-bottom: 10px; }
.cp-title { margin: 0; font-size: 18px; font-weight: 500; }
.cp-sub { margin: 4px 0 0; font-size: 13px; color: #666; }
.cp-form-panel { border: 1px solid #e8e8e8; padding: 16px; background: #fff; max-width: 640px; }
</style>
