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
        <el-table-column v-if="deleteAction" label="操作" width="72" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
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
        <el-button
          v-if="deleteAction"
          type="danger"
          size="small"
          plain
          @click="removeSelected"
        >
          删除
        </el-button>
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
import { useUserStore } from '@/stores/user'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import { useMesDelete } from '@/composables/useMesDelete'

const props = defineProps({
  title: { type: String, required: true },
  dataKey: { type: String, required: true },
  columns: { type: Array, required: true },
  detailFields: { type: Array, default: () => [] },
  filterFn: { type: Function, default: null },
  /** 如 deleteOrder、deleteWorkOrder */
  deleteAction: { type: String, default: '' },
  /** payload 中的字段名，如 orderId、workOrderId */
  deletePayloadKey: { type: String, default: 'id' },
  deleteMessage: { type: String, default: '删除后不可恢复，是否继续？' }
})

const mes = useMesStore()
const userStore = useUserStore()
const { runDelete } = useMesDelete(mes, userStore)
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

function buildPayload(row) {
  const id = row?.id ?? row?.[props.deletePayloadKey]
  return { [props.deletePayloadKey]: id }
}

async function removeRow(row) {
  if (!props.deleteAction || !row) return
  await runDelete({
    action: props.deleteAction,
    payload: buildPayload(row),
    message: props.deleteMessage,
    onSuccess: () => {
      if (selected.value?.id === row.id) selected.value = null
    }
  })
}

function removeSelected() {
  if (selected.value) removeRow(selected.value)
}
</script>

<style scoped>
.ruoyi-page--data {
  min-height: var(--layout-content-min-h, calc(100vh - 92px));
}
.ruoyi-detail__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.ruoyi-toolbar__meta {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}
</style>
