<template>
  <div class="ruoyi-page bom-guide-page">
    <div class="guide-header">
      <h2>显示器 BOM 装配清单</h2>
      <p>{{ guide.summary || '每台显示器由 4 类组装部件构成，不同型号选用不同规格组合' }}</p>
    </div>

    <el-row :gutter="16" class="group-cards">
      <el-col v-for="group in groupedOptions" :key="group.key" :span="6">
        <el-card shadow="hover" class="group-card">
          <template #header>
            <strong>{{ group.name }}</strong>
            <el-tag size="small" type="info">{{ group.items.length }} 种可选</el-tag>
          </template>
          <div v-for="item in group.items" :key="item.materialCode" class="option-item">
            <span class="option-item__name">{{ item.materialName }}</span>
            <span class="option-item__spec">{{ item.specification }}</span>
            <span class="option-item__code">{{ item.materialCode }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-divider content-position="left">各型号所需部件</el-divider>

    <el-select v-model="selectedProduct" style="width: 320px; margin-bottom: 16px" placeholder="选择成品型号">
      <el-option
        v-for="p in products"
        :key="p.productCode"
        :label="`${p.productName}（${p.productCode}）`"
        :value="p.productCode"
      />
    </el-select>

    <el-table v-if="currentProduct" :data="currentProduct.components" border stripe>
      <el-table-column prop="assemblyGroup" label="组装部件" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.assemblyGroup }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="materialName" label="选用规格" min-width="180" />
      <el-table-column prop="materialCode" label="物料编码" width="120" />
      <el-table-column prop="specification" label="说明" min-width="160" />
      <el-table-column label="用量" width="100" align="right">
        <template #default="{ row }">{{ row.quantity }} {{ row.unit }}</template>
      </el-table-column>
    </el-table>

    <el-empty v-else description="请选择成品型号查看 BOM" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useMesStore } from '@/stores/mes'

const mes = useMesStore()
const selectedProduct = ref('')

const guide = computed(() => mes.bomGuide || {})
const products = computed(() => guide.value.products || [])
const currentProduct = computed(() =>
  products.value.find((p) => p.productCode === selectedProduct.value) || products.value[0] || null
)

const groupedOptions = computed(() => {
  const groups = guide.value.groups || [
    { key: 'panel', name: '① 显示面板', codePrefix: 'MAT-P' },
    { key: 'backlight', name: '② 背光模组', codePrefix: 'MAT-B' },
    { key: 'mainboard', name: '③ 主控电路', codePrefix: 'MAT-M' },
    { key: 'structure', name: '④ 结构附件', codePrefix: 'MAT-S' }
  ]
  const options = guide.value.options || []
  return groups.map((g) => ({
    ...g,
    items: options.filter((o) => (o.materialCode || '').startsWith(g.codePrefix))
  }))
})

onMounted(async () => {
  try {
    await mes.hydrateForPage()
  } catch {
    /* ignore */
  }
  selectedProduct.value = products.value[0]?.productCode || ''
})
</script>

<style scoped>
.bom-guide-page {
  padding: 16px;
  min-height: var(--layout-content-min-h, calc(100vh - 92px));
}
.guide-header h2 {
  margin: 0 0 6px;
  font-size: 18px;
  color: #001b3f;
}
.guide-header p {
  margin: 0 0 16px;
  font-size: 13px;
  color: #606266;
}
.group-cards {
  margin-bottom: 8px;
}
.group-card :deep(.el-card__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
}
.option-item {
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
.option-item:last-child {
  border-bottom: none;
}
.option-item__name {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.option-item__spec {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.option-item__code {
  font-size: 11px;
  color: #c0c4cc;
}
</style>
