<template>
  <div class="home-product-page">
    <HomeNavbar />
    <main class="home-product-main" v-loading="loading">
      <button type="button" class="home-product-back" @click="$router.push('/')">← 返回首页</button>

      <template v-if="product">
        <div class="home-product-hero">
          <div class="home-product-media">
            <img
              :src="resolveCustomerProductImage(product)"
              :alt="product.materialName"
            />
          </div>
          <div class="home-product-info">
            <p class="home-product-code">{{ product.materialCode }}</p>
            <h1 class="home-product-title">{{ product.materialName }}</h1>
            <p class="home-product-summary">{{ product.productSummary || product.specification }}</p>

            <dl class="home-product-meta">
              <div><dt>屏幕尺寸</dt><dd>{{ meta.size }}</dd></div>
              <div><dt>分辨率</dt><dd>{{ meta.resolution }}</dd></div>
              <div><dt>接口配置</dt><dd>{{ product.ports || '—' }}</dd></div>
              <div><dt>参考单价</dt><dd>{{ formatProductPrice(product.standardCost) }} 元 / {{ product.unit || '台' }}</dd></div>
            </dl>

            <div class="home-product-actions">
              <el-button type="primary" size="large" @click="goOrder">登录下单</el-button>
              <el-button size="large" @click="$router.push('/')">浏览更多产品</el-button>
            </div>
          </div>
        </div>

        <section class="home-product-section">
          <h2>规格参数</h2>
          <p>{{ product.specification }}</p>
        </section>
      </template>

      <el-empty v-else-if="!loading" description="产品不存在或已下架" />
    </main>
    <HomeFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import HomeNavbar from '@/components/home/HomeNavbar.vue'
import HomeFooter from '@/components/home/HomeFooter.vue'
import { getCustomerProduct } from '@/api/customer'
import { formatProductPrice, parseProductDisplayMeta, resolveCustomerProductImage } from '@/utils/productImage'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const product = ref(null)

const meta = computed(() => parseProductDisplayMeta(product.value))

function goOrder() {
  router.push({
    path: '/login',
    query: { redirect: `/customer/order/new?materialId=${product.value?.materialId || route.params.materialId}` }
  })
}

onMounted(async () => {
  loading.value = true
  try {
    product.value = await getCustomerProduct(route.params.materialId)
  } catch {
    product.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home-product-page {
  min-height: 100vh;
  background: #fff;
}

.home-product-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: calc(var(--nav-height, 52px) + 24px) 32px 48px;
}

.home-product-back {
  border: none;
  background: none;
  padding: 0;
  margin-bottom: 20px;
  font-size: 14px;
  color: #1677ff;
  cursor: pointer;
}

.home-product-hero {
  display: grid;
  grid-template-columns: minmax(0, 420px) minmax(0, 1fr);
  gap: 40px;
  padding-bottom: 32px;
  border-bottom: 1px solid #e8eaed;
}

.home-product-media {
  aspect-ratio: 4 / 3;
  background: #f5f6f8;
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.home-product-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.home-product-code {
  margin: 0 0 8px;
  font-size: 13px;
  color: #86909c;
}

.home-product-title {
  margin: 0 0 12px;
  font-size: 28px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1.3;
}

.home-product-summary {
  margin: 0 0 20px;
  font-size: 15px;
  line-height: 1.65;
  color: #4e5969;
}

.home-product-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
  margin: 0 0 24px;
}

.home-product-meta > div {
  display: flex;
  gap: 8px;
  font-size: 14px;
}

.home-product-meta dt {
  margin: 0;
  color: #86909c;
  white-space: nowrap;
}

.home-product-meta dd {
  margin: 0;
  color: #1f2329;
}

.home-product-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.home-product-section {
  margin-top: 28px;
}

.home-product-section h2 {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
}

.home-product-section p {
  margin: 0;
  font-size: 15px;
  color: #4e5969;
  line-height: 1.65;
}

@media (max-width: 900px) {
  .home-product-main {
    padding-left: 20px;
    padding-right: 20px;
  }

  .home-product-hero {
    grid-template-columns: 1fr;
  }
}
</style>
