<template>
  <header class="home-navbar" @mouseleave="closeProductsMenu">
    <div class="navbar-top">
      <div class="navbar-inner">
        <BrandLogo :size="40" variant="light" :text="BRAND_NAME" class="navbar-brand-logo" />
        <nav class="navbar-links">
          <a href="#hero" @click.prevent="scrollTo('hero')">首页</a>
          <div
            class="nav-dropdown"
            @mouseenter="openProductsMenu"
          >
            <button
              type="button"
              class="nav-dropdown__trigger"
              :class="{ 'nav-dropdown__trigger--active': productsOpen }"
              @click="toggleProductsMenu"
            >
              产品
            </button>
          </div>
          <a href="#products" @click.prevent="scrollTo('products')">核心能力</a>
          <a href="#process" @click.prevent="scrollTo('process')">业务流程</a>
          <a href="#modules" @click.prevent="scrollTo('modules')">功能模块</a>
          <a href="#roles" @click.prevent="scrollTo('roles')">角色工作台</a>
        </nav>
        <a class="login-link" @click="goLogin">登录</a>
      </div>
    </div>

    <Transition name="mega-fade">
      <div v-if="productsOpen" class="products-mega" @mouseenter="openProductsMenu">
        <div class="products-mega__inner">
          <h3 class="products-mega__heading">产品</h3>
          <div v-if="products.length" class="products-mega__grid">
            <router-link
              v-for="item in products"
              :key="item.materialId"
              :to="`/products/${item.materialId}`"
              class="products-mega__item"
              @click="closeProductsMenu"
            >
              <span class="products-mega__title">{{ item.materialName }} ›</span>
              <span class="products-mega__desc">{{ item.productSummary || item.specification }}</span>
            </router-link>
          </div>
          <p v-else class="products-mega__empty">正在加载产品…</p>
        </div>
      </div>
    </Transition>

    <Transition name="mega-fade">
      <div v-if="productsOpen" class="products-mega__backdrop" @click="closeProductsMenu" />
    </Transition>
  </header>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCustomerProducts } from '@/api/customer'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import { BRAND_NAME } from '@/constants/brand'

const router = useRouter()
const productsOpen = ref(false)
const products = ref([])

function scrollTo(id) {
  closeProductsMenu()
  if (router.currentRoute.value.path !== '/') {
    router.push('/').then(() => {
      requestAnimationFrame(() => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' }))
    })
    return
  }
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}

function goLogin() {
  router.push('/login')
}

function openProductsMenu() {
  productsOpen.value = true
}

function closeProductsMenu() {
  productsOpen.value = false
}

function toggleProductsMenu() {
  productsOpen.value = !productsOpen.value
}

onMounted(async () => {
  try {
    products.value = await getCustomerProducts() || []
  } catch {
    products.value = []
  }
})
</script>

<style scoped>
.home-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 200;
}

.navbar-top {
  position: relative;
  z-index: 202;
  background: rgba(0, 27, 63, 0.96);
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: var(--nav-height);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 32px;
}

.navbar-brand-logo {
  flex-shrink: 0;
  margin-right: 44px;
}

.navbar-brand-logo :deep(.brand-logo__img) {
  background: transparent;
}

.navbar-links {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 32px;
}

.navbar-links a,
.nav-dropdown__trigger {
  color: rgba(255, 255, 255, 0.78);
  text-decoration: none;
  font-size: var(--fs-nav);
  font-weight: var(--nav-weight);
  cursor: pointer;
  transition: color 0.15s;
  background: none;
  border: none;
  padding: 0;
  font-family: inherit;
}

.navbar-links a:hover,
.nav-dropdown__trigger:hover,
.nav-dropdown__trigger--active {
  color: #fff;
}

.nav-dropdown__trigger--active {
  box-shadow: inset 0 -2px 0 #fff;
}

.login-link {
  color: #fff;
  font-size: var(--fs-nav);
  font-weight: var(--nav-weight);
  cursor: pointer;
  text-decoration: none;
  transition: color 0.15s;
}

.login-link:hover {
  color: var(--accent-bright);
}

.products-mega__backdrop {
  position: fixed;
  inset: var(--nav-height) 0 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.45);
}

.products-mega {
  position: fixed;
  top: var(--nav-height);
  left: 0;
  right: 0;
  z-index: 201;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

.products-mega__inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 32px 28px;
}

.products-mega__heading {
  margin: 0 0 18px;
  font-size: 18px;
  font-weight: 700;
  color: #1f2329;
}

.products-mega__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px 28px;
}

.products-mega__item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-decoration: none;
  padding: 4px 0;
  transition: opacity 0.15s;
}

.products-mega__item:hover .products-mega__title {
  color: #000;
}

.products-mega__title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2329;
  line-height: 1.35;
}

.products-mega__desc {
  font-size: 13px;
  line-height: 1.5;
  color: #86909c;
  font-weight: 400;
}

.products-mega__empty {
  margin: 0;
  font-size: 14px;
  color: #86909c;
}

.mega-fade-enter-active,
.mega-fade-leave-active {
  transition: opacity 0.18s ease;
}

.mega-fade-enter-from,
.mega-fade-leave-to {
  opacity: 0;
}

@media (max-width: 960px) {
  .products-mega__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .navbar-inner {
    padding: 0 20px;
  }

  .navbar-links {
    display: none;
  }

  .navbar-brand {
    margin-right: 0;
  }
}
</style>
