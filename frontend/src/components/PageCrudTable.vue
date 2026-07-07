<template>
  <div class="page-card">
    <div class="page-title">{{ title }}</div>
    <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :min-width="col.minWidth || 120"
        show-overflow-tooltip
      />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const props = defineProps({
  title: { type: String, default: '列表' },
  columns: { type: Array, default: () => [] },
  fetchApi: { type: Function, required: true }
})

const loading = ref(false)
const tableData = ref([])

async function loadData() {
  loading.value = true
  try {
    const data = await props.fetchApi()
    tableData.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
