<template>
  <MesPageShell toolbar-title="工艺说明" :show-log="false">
    <template #table>
      <div style="padding:16px">
        <el-select v-model="model" style="width:240px;margin-bottom:16px">
          <el-option v-for="m in models" :key="m" :label="m" :value="m" />
        </el-select>
        <div v-if="guide" style="max-width:720px">
          <h4 style="font-weight:600;margin:0 0 8px">工序流程</h4>
          <ProcessTimeline :items="guide.steps.map(s => ({ title: s }))" />
          <p style="margin-top:16px;color:#4f5f73;line-height:1.8;font-size:15px">{{ guide.keyPoints }}</p>
        </div>
        <el-empty v-else description="暂无工艺数据，请确认后端已配置工艺路线" />
      </div>
    </template>
  </MesPageShell>
</template>
<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useMesStore } from '@/stores/mes'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import ProcessTimeline from '@/components/mes/ProcessTimeline.vue'

const mes = useMesStore()
const models = computed(() => Object.keys(mes.processGuide || {}))
const model = ref('')
const guide = computed(() => mes.processGuide[model.value])

watch(models, (list) => {
  if (!model.value && list.length) model.value = list[0]
}, { immediate: true })

onMounted(async () => {
  try {
    await mes.hydrateFromApi()
    if (!model.value && models.value.length) model.value = models.value[0]
  } catch {
    /* ignore */
  }
})
</script>
