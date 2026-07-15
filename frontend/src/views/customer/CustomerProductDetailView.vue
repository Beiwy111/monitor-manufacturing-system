<template>
  <div class="cp-detail" v-loading="loading">
    <button type="button" class="cp-detail__back" @click="$router.push('/customer/home')">← 返回产品中心</button>

    <template v-if="product">
      <div class="cp-detail__hero">
        <div class="cp-detail__media">
          <img
            :src="resolveCustomerProductImage(product)"
            :alt="product.materialName"
          />
        </div>
        <div class="cp-detail__info">
          <p class="cp-detail__code">{{ product.materialCode }}</p>
          <h2 class="cp-detail__title">{{ product.materialName }}</h2>
          <p class="cp-detail__summary">{{ product.productSummary || product.specification }}</p>

          <dl class="cp-detail__meta">
            <div><dt>尺寸</dt><dd>{{ meta.size }}</dd></div>
            <div><dt>分辨率</dt><dd>{{ meta.resolution }}</dd></div>
            <div><dt>接口</dt><dd>{{ product.ports || '—' }}</dd></div>
            <div><dt>参考单价</dt><dd>{{ formatProductPrice(product.standardCost) }} 元 / {{ product.unit || '台' }}</dd></div>
          </dl>

          <div class="cp-detail__actions">
            <el-button type="primary" @click="goOrder">立即下单</el-button>
            <el-button @click="$router.push('/customer/products')">查看规格表</el-button>
          </div>
        </div>
      </div>

      <section class="cp-detail__section">
        <h3>规格参数</h3>
        <p>{{ product.specification }}</p>
      </section>
    </template>

    <el-empty v-else-if="!loading" description="产品不存在或已下架" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCustomerProduct } from '@/api/customer'
import { formatProductPrice, parseProductDisplayMeta, resolveCustomerProductImage } from '@/utils/productImage'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const product = ref(null)

const meta = computed(() => parseProductDisplayMeta(product.value))

function goOrder() {
  router.push({ path: '/customer/order/new', query: { materialId: product.value.materialId } })
}

onMounted(async () => {
  loading.value = true
  try {
    product.value = await getCustomerProduct(route.params.materialId)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.cp-detail {
  max-width: 960px;
  margin: 0 auto;
  padding: 16px 20px 32px;
}

.cp-detail__back {
  border: none;
  background: none;
  padding: 0;
  margin-bottom: 14px;
  font-size: 13px;
  color: #4096ff;
  cursor: pointer;
}

.cp-detail__hero {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e8eaed;
}

.cp-detail__media {
  aspect-ratio: 4 / 3;
  background: #f5f6f8;
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.cp-detail__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cp-detail__code {
  margin: 0 0 6px;
  font-size: 12px;
  color: #86909c;
}

.cp-detail__title {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
}

.cp-detail__summary {
  margin: 0 0 16px;
  font-size: 14px;
  line-height: 1.6;
  color: #4e5969;
}

.cp-detail__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 20px;
  margin: 0 0 20px;
}

.cp-detail__meta > div {
  display: flex;
  gap: 8px;
  font-size: 13px;
}

.cp-detail__meta dt {
  margin: 0;
  color: #86909c;
  white-space: nowrap;
}

.cp-detail__meta dd {
  margin: 0;
  color: #1f2329;
}

.cp-detail__actions {
  display: flex;
  gap: 10px;
}

.cp-detail__section {
  margin-top: 20px;
}

.cp-detail__section h3 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

.cp-detail__section p {
  margin: 0;
  font-size: 14px;
  color: #4e5969;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .cp-detail__hero {
    grid-template-columns: 1fr;
  }
}
</style>
