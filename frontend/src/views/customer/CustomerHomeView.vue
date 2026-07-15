<template>
  <div class="cp-home" v-loading="loading">
    <header class="cp-home__head">
      <div>
        <h2 class="cp-home__title">产品中心</h2>
        <p class="cp-home__sub">浏览可订购显示器型号，点击选择规格并下单</p>
      </div>
      <el-button size="small" @click="$router.push('/customer/orders')">我的订单</el-button>
    </header>

    <div v-if="products.length" class="cp-home__grid">
      <article
        v-for="item in products"
        :key="item.materialId"
        class="product-card"
        @click="openOrder(item.materialId)"
      >
        <div class="product-card__media">
          <img
            :src="resolveCustomerProductImage(item)"
            :alt="item.materialName"
          />
        </div>
        <div class="product-card__body">
          <h3 class="product-card__name">{{ item.materialName }}</h3>
          <p class="product-card__meta">
            <span>{{ item.materialCode }}</span>
            <span>{{ parseProductDisplayMeta(item).resolution }}</span>
          </p>
          <p class="product-card__price">参考价 {{ formatProductPrice(item.standardCost) }} 元</p>
        </div>
      </article>
    </div>

    <el-empty v-if="!loading && !products.length" description="暂无可订购产品" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCustomerProducts } from '@/api/customer'
import {
  formatProductPrice,
  parseProductDisplayMeta,
  resolveCustomerProductImage
} from '@/utils/productImage'

const router = useRouter()
const loading = ref(false)
const products = ref([])

function openOrder(materialId) {
  router.push({ path: '/customer/order/new', query: { materialId } })
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
.cp-home {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: calc(100vh - var(--layout-header-h, 52px));
  padding: 10px 14px 12px;
}

.cp-home__head {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8eaed;
}

.cp-home__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
}

.cp-home__sub {
  margin: 2px 0 0;
  font-size: 12px;
  color: #646a73;
}

.cp-home__grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.product-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid #e8eaed;
  background: #fff;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.product-card:hover {
  border-color: #4096ff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.product-card__media {
  flex: 1;
  min-height: 0;
  background: #f5f6f8;
  overflow: hidden;
}

.product-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}

.product-card__body {
  flex-shrink: 0;
  padding: 10px 12px 12px;
  border-top: 1px solid #eef0f3;
}

.product-card__name {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-card__meta {
  margin: 0 0 6px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #86909c;
  line-height: 1.4;
}

.product-card__meta span:last-child {
  color: #4e5969;
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-card__price {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1677ff;
}

@media (max-width: 1100px) {
  .cp-home__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .cp-home__grid {
    grid-template-columns: 1fr;
    grid-template-rows: none;
    overflow-y: auto;
  }

  .product-card {
    min-height: 280px;
  }
}
</style>
