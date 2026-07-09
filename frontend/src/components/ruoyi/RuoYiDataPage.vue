<template>
  <div class="ruoyi-page">
    <div v-if="showQuery" class="ruoyi-query">
      <el-form :inline="true" @submit.prevent="emit('search')">
        <slot name="query" />
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="emit('search')">搜索</el-button>
          <el-button :icon="Refresh" @click="emit('reset')">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div v-if="$slots.toolbar" class="ruoyi-toolbar">
      <slot name="toolbar" />
    </div>
    <div class="ruoyi-table-wrap">
      <el-table
        v-loading="loading"
        :data="data"
        border
        stripe
        highlight-current-row
        @current-change="(row) => emit('row-change', row)"
      >
        <slot />
      </el-table>
    </div>
    <div v-if="total > 0" class="ruoyi-pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { Search, Refresh } from '@element-plus/icons-vue'

defineProps({
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  showQuery: { type: Boolean, default: false }
})

const page = defineModel('page', { type: Number, default: 1 })
const size = defineModel('size', { type: Number, default: 10 })

const emit = defineEmits(['search', 'reset', 'row-change'])
</script>
