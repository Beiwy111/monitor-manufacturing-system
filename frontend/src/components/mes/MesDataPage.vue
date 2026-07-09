<template>
  <div class="ruoyi-page ruoyi-page--data">
    <div class="ruoyi-query">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="keyword"
            :placeholder="`搜索${title}`"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">{{ title }}</span>
      <span class="ruoyi-toolbar__meta">共 {{ filtered.length }} 条</span>
    </div>

    <div class="ruoyi-table-wrap">
      <el-table
        :data="pageData"
        border
        stripe
        highlight-current-row
        @current-change="onRowClick"
      >
        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          show-overflow-tooltip
        />
      </el-table>
    </div>

    <div v-if="total > pageSize" class="ruoyi-pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <div v-if="selected" class="ruoyi-detail">
      <div class="ruoyi-detail__head">
        <span class="ruoyi-detail__title">详情</span>
      </div>
      <div class="ruoyi-detail__body">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item
            v-for="row in rows"
            :key="row.label"
            :label="row.label"
          >
            {{ row.value }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { useMesStore } from '@/stores/mes'
import { useMesFilter, detailRows } from '@/composables/useMesPage'

const props = defineProps({
  title: { type: String, required: true },
  dataKey: { type: String, required: true },
  columns: { type: Array, required: true },
  detailFields: { type: Array, default: () => [] },
  filterFn: { type: Function, default: null }
})

const mes = useMesStore()
const pageNum = ref(1)
const pageSize = ref(10)

const list = computed(() => {
  let data = mes[props.dataKey] || []
  if (props.filterFn) data = props.filterFn(data)
  return data
})

const { selected, filtered, onRowClick, keyword } = useMesFilter(list, props.columns.map((c) => c.prop))

const total = computed(() => filtered.value.length)
const pageData = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

const rows = computed(() =>
  detailRows(
    selected.value,
    props.detailFields.length
      ? props.detailFields
      : props.columns.map((c) => ({ key: c.prop, label: c.label }))
  )
)

function handleSearch() {
  pageNum.value = 1
}

function resetQuery() {
  keyword.value = ''
  pageNum.value = 1
}
</script>

<style scoped>
.ruoyi-page--data {
  min-height: calc(100vh - 130px);
}
.ruoyi-toolbar__meta {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}
</style>
