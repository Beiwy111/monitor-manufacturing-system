<template>
  <div class="inventory-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="库存查询" name="stock">
        <MesPageShell toolbar-title="库存查询（按组装部件归类）">
          <template #table>
            <el-table :data="groupedInventory" border stripe highlight-current-row row-key="group" default-expand-all>
              <el-table-column type="expand">
                <template #default="{ row }">
                  <el-table :data="row.items" border size="small" style="margin: 8px 0">
                    <el-table-column prop="materialCode" label="编码" width="110" />
                    <el-table-column prop="materialName" label="物料名称" min-width="140" />
                    <el-table-column prop="specification" label="规格说明" min-width="160" />
                    <el-table-column prop="quantity" label="库存量" width="90" align="right" />
                    <el-table-column prop="safeQty" label="安全库存" width="90" align="right" />
                    <el-table-column prop="location" label="库位" width="100" />
                    <el-table-column prop="status" label="状态" width="80">
                      <template #default="{ row: item }"><StatusBadge :status="item.status" /></template>
                    </el-table-column>
                  </el-table>
                </template>
              </el-table-column>
              <el-table-column prop="group" label="组装分类" width="140">
                <template #default="{ row }">
                  <el-tag :type="groupTagType(row.group)" size="small">{{ row.group }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="count" label="物料种类" width="100" align="center" />
              <el-table-column prop="totalQty" label="库存合计" width="100" align="right" />
              <el-table-column label="说明" min-width="200">
                <template #default="{ row }">{{ groupHint(row.group) }}</template>
              </el-table-column>
            </el-table>
          </template>
        </MesPageShell>
      </el-tab-pane>

      <el-tab-pane label="BOM 装配清单" name="bom">
        <BomGuideView />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useMesStore } from '@/stores/mes'
import MesPageShell from '@/components/mes/MesPageShell.vue'
import StatusBadge from '@/components/mes/StatusBadge.vue'
import BomGuideView from '@/views/warehouse/BomGuideView.vue'

const mes = useMesStore()
const activeTab = ref('stock')

const GROUP_ORDER = ['显示面板', '背光模组', '主控电路', '结构附件', '成品', '其他']

const groupedInventory = computed(() => {
  const map = new Map()
  for (const item of mes.inventory) {
    const group = item.assemblyGroup || '其他'
    if (!map.has(group)) {
      map.set(group, [])
    }
    map.get(group).push(item)
  }
  return GROUP_ORDER.filter((g) => map.has(g)).map((group) => {
    const items = map.get(group)
    return {
      group,
      count: items.length,
      totalQty: items.reduce((sum, i) => sum + Number(i.quantity || 0), 0),
      items
    }
  })
})

function groupTagType(group) {
  const map = { 显示面板: 'primary', 背光模组: 'success', 主控电路: 'warning', 结构附件: 'info', 成品: '' }
  return map[group] || 'info'
}

function groupHint(group) {
  const hints = {
    显示面板: '决定分辨率与面板类型（IPS/OLED）',
    背光模组: '与面板尺寸匹配，提供均匀背光',
    主控电路: '主控板 + 驱动芯片，决定刷新率与功能',
    结构附件: '边框套件与电源适配器',
    成品: '已组装完成的显示器成品'
  }
  return hints[group] || ''
}
</script>

<style scoped>
.inventory-page :deep(.el-tabs__header) {
  margin: 0 16px;
  padding-top: 8px;
}
</style>
