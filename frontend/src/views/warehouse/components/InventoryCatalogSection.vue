<template>
  <div class="catalog-shell">
    <div class="catalog-toolbar">
      <div class="catalog-toolbar__left">
        <div class="catalog-toolbar__title">{{ title }}</div>
        <span class="catalog-toolbar__meta">
          {{ warehouseLabel }} · 共 {{ filteredRows.length }} 种 · 库存 {{ totalQty }}
        </span>
      </div>
      <div class="catalog-toolbar__filters">
        <el-input
          :model-value="keyword"
          clearable
          placeholder="物料编码 / 名称"
          style="width: 220px"
          @update:model-value="$emit('update:keyword', $event)"
        />
        <el-select
          :model-value="categoryFilter"
          clearable
          placeholder="所属分类"
          style="width: 140px"
          @update:model-value="$emit('update:categoryFilter', $event)"
        >
          <el-option v-for="opt in categoryOptions" :key="opt" :label="opt" :value="opt" />
        </el-select>
        <el-button type="primary" @click="$emit('refresh')">查询</el-button>
      </div>
    </div>

    <div class="catalog-body">
      <div class="catalog-side">
        <div class="catalog-side__title">所属分类</div>
        <div
          class="catalog-side__item"
          :class="{ 'is-active': !categoryFilter }"
          @click="$emit('update:categoryFilter', '')"
        >
          全部
        </div>
        <div
          v-for="opt in categoryOptions"
          :key="opt"
          class="catalog-side__item"
          :class="{ 'is-active': categoryFilter === opt }"
          @click="$emit('update:categoryFilter', opt)"
        >
          {{ opt }}
        </div>
      </div>

      <div class="catalog-main">
        <el-table :data="filteredRows" border stripe v-loading="loading" class="catalog-table">
          <el-table-column label="物料图片" width="100" align="center">
            <template #default="{ row }">
              <el-image
                :src="thumbSrc(row)"
                fit="cover"
                class="material-thumb"
                :preview-src-list="previewable(row) ? [thumbSrc(row)] : []"
                preview-teleported
                referrerpolicy="no-referrer"
                @error="onThumbError(row)"
              >
                <template #error>
                  <div class="material-thumb material-thumb--placeholder">
                    <span>暂无</span>
                  </div>
                </template>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column prop="materialCode" label="物料编码" width="120" />
          <el-table-column prop="materialName" label="物料名称" min-width="160" />
          <el-table-column prop="specification" label="规格型号" min-width="180" show-overflow-tooltip />
          <el-table-column prop="unit" label="单位" width="70" align="center" />
          <el-table-column prop="categoryLabel" label="所属分类" width="110" />
          <el-table-column label="库存数量" width="100" align="right">
            <template #default="{ row }">
              <strong class="qty-strong">{{ row.displayQty ?? 0 }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="safetyStock" label="安全库存" width="90" align="right" />
          <el-table-column prop="batchCount" label="批次数" width="80" align="right" />
          <el-table-column label="库存状态" width="90">
            <template #default="{ row }">
              <StatusBadge :status="row.stockStatus" />
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import { MATERIAL_PLACEHOLDER, materialImageUrl } from '@/utils/materialImages'

const brokenThumbs = reactive({})

function thumbKey(row) {
  return row.materialCode || row.materialName || ''
}

function thumbSrc(row) {
  const key = thumbKey(row)
  if (brokenThumbs[key]) return MATERIAL_PLACEHOLDER
  return materialImageUrl(row)
}

function previewable(row) {
  return !brokenThumbs[thumbKey(row)]
}

function onThumbError(row) {
  const key = thumbKey(row)
  if (key) brokenThumbs[key] = true
}

const props = defineProps({
  title: String,
  warehouseLabel: String,
  rows: { type: Array, default: () => [] },
  loading: Boolean,
  keyword: { type: String, default: '' },
  categoryFilter: { type: String, default: '' }
})

defineEmits(['update:keyword', 'update:categoryFilter', 'refresh'])

const categoryOptions = computed(() => {
  const set = new Set((props.rows || []).map((r) => r.categoryLabel).filter(Boolean))
  return Array.from(set)
})

const filteredRows = computed(() => {
  const kw = (props.keyword || '').trim().toLowerCase()
  return (props.rows || []).filter((row) => {
    if (props.categoryFilter && row.categoryLabel !== props.categoryFilter) return false
    if (!kw) return true
    return [row.materialCode, row.materialName, row.specification, row.categoryLabel]
      .some((v) => String(v || '').toLowerCase().includes(kw))
  })
})

const totalQty = computed(() =>
  filteredRows.value.reduce((sum, row) => sum + Number(row.displayQty || 0), 0)
)
</script>

<style scoped>
.catalog-shell {
  margin: 12px 16px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.catalog-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
}

.catalog-toolbar__left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.catalog-toolbar__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.catalog-toolbar__meta {
  font-size: 12px;
  color: #909399;
}

.catalog-toolbar__filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.catalog-body {
  display: flex;
  min-height: 520px;
}

.catalog-side {
  width: 148px;
  border-right: 1px solid #ebeef5;
  background: #fcfcfd;
  padding: 12px 0;
}

.catalog-side__title {
  padding: 0 16px 10px;
  font-size: 13px;
  color: #909399;
}

.catalog-side__item {
  padding: 10px 16px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.15s ease;
}

.catalog-side__item:hover {
  background: #f0f7ff;
  color: #409eff;
}

.catalog-side__item.is-active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
  border-right: 2px solid #409eff;
}

.catalog-main {
  flex: 1;
  padding: 12px;
  overflow: auto;
}

.catalog-table {
  width: 100%;
}

.catalog-table :deep(.material-thumb) {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  background: #f5f7fa;
}

.material-thumb--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #c0c4cc;
}

.qty-strong {
  color: #409eff;
  font-size: 15px;
}
</style>
