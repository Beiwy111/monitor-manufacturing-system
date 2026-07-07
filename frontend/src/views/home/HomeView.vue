<template>
  <div class="home-page">
    <HomeNavbar />
    <HeroSection @enter="goLogin" />
    <IntroBannerSection @process="scrollTo('process')" />
    <ProductShowcaseSection />
    <ProcessFlowSection />
    <ModuleGridSection />
    <RoleWorkbenchSection @login-as="goLoginAs" />
    <HomeFooter />
  </div>
</template>

<script setup>
import HomeNavbar from '@/components/home/HomeNavbar.vue'
import HeroSection from '@/components/home/HeroSection.vue'
import IntroBannerSection from '@/components/home/IntroBannerSection.vue'
import ProductShowcaseSection from '@/components/home/ProductShowcaseSection.vue'
import ProcessFlowSection from '@/components/home/ProcessFlowSection.vue'
import ModuleGridSection from '@/components/home/ModuleGridSection.vue'
import RoleWorkbenchSection from '@/components/home/RoleWorkbenchSection.vue'
import HomeFooter from '@/components/home/HomeFooter.vue'
import { useRouter } from 'vue-router'

const router = useRouter()

function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}

function goLogin() {
  router.push('/login')
}

function goLoginAs(account) {
  router.push({ path: '/login', query: { username: account } })
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #fff;
}
</style>
