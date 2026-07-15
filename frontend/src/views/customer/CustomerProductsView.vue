<template>
  <div class="cp-page" v-loading="loading">
    <div class="cp-head">
      <h2 class="cp-title">产品与规格</h2>
      <p class="cp-sub">可订购成品型号及技术规格</p>
    </div>

    <el-table :data="products" border stripe size="small" style="width:100%" empty-text="暂无产品">
      <el-table-column prop="materialCode" label="产品编码" width="120" />
      <el-table-column prop="materialName" label="产品名称" min-width="160" />
      <el-table-column prop="specification" label="规格参数" min-width="200" show-overflow-tooltip />
      <el-table-column prop="unit" label="单位" width="60" align="center" />
      <el-table-column prop="standardCost" label="参考单价(元)" width="110" align="right" />
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="orderProduct(row)">订购</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCustomerProducts } from '@/api/customer'

const router = useRouter()
const loading = ref(false)
const products = ref([])

function orderProduct(row) {
  router.push({ path: '/customer/order/new', query: { materialId: row.materialId } })
}

onMounted(async () => {
  loading.value = true
  try {
    products.value = await getCustomerProducts() || []
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
</style>
