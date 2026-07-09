<template>
  <div />
</template>
<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHomePath } from '@/utils/menuRoutes'

const router = useRouter()
onMounted(() => router.replace(getHomePath('admin')))
</script>
