<template>
  <div class="ruoyi-tags">
    <router-link
      v-for="tag in tagsView.visitedViews"
      :key="tag.path"
      :to="tag.path"
      class="ruoyi-tags__item"
      :class="{ 'is-active': route.path === tag.path }"
      @click.middle.prevent="closeTag(tag)"
    >
      {{ tag.title }}
      <span
        v-if="!tag.affix"
        class="ruoyi-tags__close"
        @click.prevent.stop="closeTag(tag)"
      >×</span>
    </router-link>
  </div>
</template>

<script setup>
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTagsViewStore } from '@/stores/tagsView'

import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const tagsView = useTagsViewStore()
const userStore = useUserStore()

watch(
  () => route.path,
  () => tagsView.addView(route),
  { immediate: true }
)

function closeTag(tag) {
  tagsView.removeView(tag.path)
  if (route.path === tag.path) {
    const last = tagsView.visitedViews[tagsView.visitedViews.length - 1]
    router.push(last?.path || userStore.dashboardPath)
  }
}
</script>
