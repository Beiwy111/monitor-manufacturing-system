<template>
  <div v-loading="loading" class="location-map-page">
    <section class="location-map-page__hero">
      <div class="hero-head">
        <div>
          <h2 class="hero-title">库位图</h2>
          <p class="hero-sub">仓储容量占位可视化 · 数据来自数据库</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新数据</el-button>
      </div>

      <div class="chart-row">
        <div class="chart-panel">
          <div class="chart-panel__title">储位占用分布</div>
          <BoardChart :option="pieOption" class="chart-panel__body" />
        </div>
        <div class="chart-panel">
          <div class="chart-panel__title">整体利用率</div>
          <BoardChart :option="gaugeOption" class="chart-panel__body chart-panel__body--gauge" />
        </div>
        <div class="chart-panel chart-panel--wide">
          <div class="chart-panel__title">各库区利用率</div>
          <BoardChart :option="barOption" class="chart-panel__body" />
        </div>
      </div>

      <div class="hero-legend">
        <span class="legend-title">状态图例</span>
        <div class="legend-items">
          <span v-for="leg in legendItems" :key="leg.level" class="legend-chip">
            <i class="legend-dot" :class="`is-${leg.level}`" />
            {{ leg.label }}
          </span>
        </div>
      </div>
    </section>

    <div class="zone-tabs">
      <el-radio-group v-model="activeZoneId" size="default">
        <el-radio-button value="all">全部库区</el-radio-button>
        <el-radio-button v-for="z in zones" :key="z.id" :value="z.id">
          {{ z.name }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <el-empty v-if="!loading && !zones.length" description="暂无库位数据，请先执行 warehouse_location_map.sql 迁移" />

    <section
      v-for="zone in visibleZones"
      :key="zone.id"
      class="zone-panel"
    >
      <header class="zone-panel__head">
        <div>
          <h3 class="zone-panel__title">{{ zone.name }}</h3>
          <p class="zone-panel__desc">{{ zone.code }} · {{ zone.description }}</p>
        </div>
        <el-tag type="info" effect="plain" size="small">
          {{ zone.locations?.length || 0 }} 个库位 · 利用率 {{ zone.utilization ?? 0 }}%
        </el-tag>
      </header>

      <div class="location-grid">
        <article
          v-for="loc in zone.locations"
          :key="loc.id"
          class="location-card"
          :class="`is-${occupancyLevel(loc.occupied, loc.capacity)}`"
        >
          <div class="location-card__head">
            <div class="location-card__id">{{ loc.id }}</div>
            <el-tag size="small" :type="tagType(loc.occupied, loc.capacity)" effect="light">
              {{ levelLabel(occupancyLevel(loc.occupied, loc.capacity)) }}
            </el-tag>
          </div>
          <div class="location-card__name">{{ loc.name }}</div>
          <div class="location-card__meta">
            <span>{{ loc.occupied }}/{{ loc.capacity }}</span>
            <span class="location-card__rate">{{ occupancyRate(loc.occupied, loc.capacity) }}%</span>
          </div>

          <div class="bin-grid">
            <el-tooltip
              v-for="cell in loc.bins"
              :key="cell.id"
              placement="top"
              :show-after="120"
            >
              <template #content>
                <div class="bin-tooltip">
                  <div><b>储位编号</b> {{ cell.id }}</div>
                  <div v-if="cell.slotLabel"><b>位置</b> {{ cell.slotLabel }}</div>
                  <div v-else-if="cell.rowNo"><b>位置</b> 第{{ cell.rowNo }}层第{{ cell.colNo }}格</div>
                  <div><b>物料名称</b> {{ cell.materialName || '空闲' }}</div>
                  <div><b>容量</b> {{ cell.capacity }}</div>
                  <div><b>已占用</b> {{ cell.occupied }}</div>
                  <div><b>占用率</b> {{ occupancyRate(cell.occupied, cell.capacity) }}%</div>
                  <div v-if="canOpen3d(zone)" class="bin-tooltip__action">点击查看 3D 货架</div>
                </div>
              </template>
              <button
                type="button"
                class="bin-cell"
                :class="[
                  `is-${occupancyLevel(cell.occupied, cell.capacity)}`,
                  { 'is-clickable': canOpen3d(zone), 'is-focused': sceneCtx?.focusSlotId === cell.id }
                ]"
                :disabled="!canOpen3d(zone)"
                @click="openShelfScene(zone, loc, cell)"
              >
                <span class="bin-cell__text">{{ cell.occupied ? '占' : '空' }}</span>
                <span v-if="canOpen3d(zone)" class="bin-cell__3d">3D</span>
              </button>
            </el-tooltip>
          </div>

          <div class="location-card__foot">
            <span class="foot-material">{{ loc.materialName }}</span>
          </div>
        </article>
      </div>
    </section>

    <WarehouseAlertPanel />

    <el-dialog
      v-model="sceneVisible"
      width="860px"
      top="6vh"
      destroy-on-close
      class="shelf-3d-dialog"
      :title="sceneTitle"
    >
      <div v-if="sceneCtx" class="shelf-3d-dialog__meta">
        <el-tag size="small" :type="sceneCtx.cell.occupied ? 'warning' : 'info'">
          {{ sceneCtx.cell.occupied ? '已占用' : '空闲' }}
        </el-tag>
        <span>储位 <strong>{{ sceneCtx.focusSlotId }}</strong></span>
        <span>物料 {{ sceneCtx.cell.materialName || '—' }}</span>
        <span>{{ sceneZoneLabel }}</span>
      </div>
      <WarehouseShelfScene3D
        v-if="sceneVisible && sceneCtx"
        :location="sceneCtx.location"
        :zone-type="sceneCtx.zoneType"
        :focus-slot-id="sceneCtx.focusSlotId"
        :bins="sceneCtx.location.bins"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import BoardChart from '@/components/board/BoardChart.vue'
import WarehouseAlertPanel from './components/WarehouseAlertPanel.vue'
import WarehouseShelfScene3D from '@/components/warehouse/WarehouseShelfScene3D.vue'
import { fetchWarehouseLocationMap } from '@/api/warehouse'
import { occupancyRate, occupancyLevel, levelLabel } from '@/utils/warehouseLocation'

const loading = ref(false)
const summary = ref(null)
const zones = ref([])
const activeZoneId = ref('all')
const sceneVisible = ref(false)
const sceneCtx = ref(null)

const sceneTitle = computed(() => {
  if (!sceneCtx.value) return '3D 货架场景'
  return `${sceneCtx.value.location.name} · 3D 货架`
})

const sceneZoneLabel = computed(() => {
  const t = sceneCtx.value?.zoneType
  if (t === 'FG') return '成品仓 · 显示器模型'
  if (t === 'RM') return '原材料仓 · 原料模型'
  return '辅料仓 · 物料模型'
})

function canOpen3d(zone) {
  return zone?.id === 'FG' || zone?.id === 'RM'
}

function openShelfScene(zone, location, cell) {
  if (!canOpen3d(zone)) return
  sceneCtx.value = {
    zoneType: zone.id,
    zoneName: zone.name,
    location,
    focusSlotId: cell.id,
    cell
  }
  sceneVisible.value = true
}

const legendItems = [
  { level: 'empty', label: '空闲' },
  { level: 'partial-low', label: '部分占用（≤50%）' },
  { level: 'partial-mid', label: '部分占用（50%~85%）' },
  { level: 'near-full', label: '接近满仓（≥85%）' },
  { level: 'full', label: '已满' }
]

const visibleZones = computed(() =>
  activeZoneId.value === 'all'
    ? zones.value
    : zones.value.filter((z) => z.id === activeZoneId.value)
)

const pieOption = computed(() => {
  const s = summary.value || {}
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}', fontSize: 11 },
      data: [
        { name: '已占用储位', value: s.occupiedBins || 0, itemStyle: { color: '#f59e42' } },
        { name: '空闲储位', value: s.freeBins || 0, itemStyle: { color: '#94b8e8' } }
      ]
    }]
  }
})

const gaugeOption = computed(() => {
  const util = summary.value?.utilization ?? 0
  return {
    series: [{
      type: 'gauge',
      center: ['50%', '58%'],
      radius: '88%',
      min: 0,
      max: 100,
      progress: { show: true, width: 14, itemStyle: { color: '#2d8a66' } },
      axisLine: { lineStyle: { width: 14, color: [[1, '#e8edf5']] } },
      axisTick: { show: false },
      splitLine: { length: 8, lineStyle: { color: '#c5d0e0' } },
      axisLabel: { distance: 18, fontSize: 10, color: '#909399' },
      pointer: { width: 5, itemStyle: { color: '#2d8a66' } },
      anchor: { show: true, size: 10, itemStyle: { color: '#2d8a66' } },
      title: { offsetCenter: [0, '72%'], fontSize: 12, color: '#909399' },
      detail: {
        valueAnimation: true,
        fontSize: 28,
        fontWeight: 700,
        color: '#2d8a66',
        offsetCenter: [0, '38%'],
        formatter: '{value}%'
      },
      data: [{ value: util, name: '整体利用率' }]
    }]
  }
})

const barOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 48, right: 16, top: 24, bottom: 28 },
  xAxis: {
    type: 'category',
    data: zones.value.map((z) => z.name),
    axisLabel: { fontSize: 11, color: '#606266' },
    axisLine: { lineStyle: { color: '#e4e9f2' } }
  },
  yAxis: {
    type: 'value',
    max: 100,
    axisLabel: { formatter: '{value}%', fontSize: 10, color: '#909399' },
    splitLine: { lineStyle: { color: '#f0f2f5' } }
  },
  series: [{
    type: 'bar',
    barWidth: '42%',
    data: zones.value.map((z) => ({
      value: z.utilization ?? 0,
      itemStyle: {
        color: (z.utilization ?? 0) >= 85 ? '#f87171' : (z.utilization ?? 0) >= 50 ? '#fdba74' : '#67c7c0',
        borderRadius: [6, 6, 0, 0]
      }
    })),
    label: { show: true, position: 'top', formatter: '{c}%', fontSize: 11, color: '#606266' }
  }]
}))

function tagType(occupied, capacity) {
  const level = occupancyLevel(occupied, capacity)
  if (level === 'empty') return 'info'
  if (level === 'full' || level === 'near-full') return 'danger'
  return 'warning'
}

async function loadData() {
  loading.value = true
  try {
    const data = await fetchWarehouseLocationMap()
    summary.value = data?.summary || null
    zones.value = data?.zones || []
  } catch (e) {
    ElMessage.error(e?.message || '加载库位数据失败')
    summary.value = null
    zones.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.location-map-page {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.location-map-page__hero {
  background: linear-gradient(135deg, #f8fafc 0%, #eef4ff 100%);
  border: 1px solid #e4e9f2;
  border-radius: 12px;
  padding: 20px 24px;
}

.hero-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.hero-title {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.hero-sub {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1.4fr;
  gap: 14px;
  margin-bottom: 16px;
}

@media (max-width: 1100px) {
  .chart-row { grid-template-columns: 1fr 1fr; }
  .chart-panel--wide { grid-column: 1 / -1; }
}
@media (max-width: 700px) {
  .chart-row { grid-template-columns: 1fr; }
}

.chart-panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px 14px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .04);
}

.chart-panel__title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 4px;
}

.chart-panel__body {
  height: 200px;
}

.chart-panel__body--gauge {
  height: 210px;
}

.hero-legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.legend-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.legend-items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.legend-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 20px;
  padding: 4px 12px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.legend-dot.is-empty { background: #dce4ef; border: 1px solid #c5d0e0; }
.legend-dot.is-partial-low { background: #fde68a; border: 1px solid #f5d565; }
.legend-dot.is-partial-mid { background: #fdba74; border: 1px solid #f59e42; }
.legend-dot.is-near-full { background: #fb923c; border: 1px solid #ea580c; }
.legend-dot.is-full { background: #f87171; border: 1px solid #dc2626; }

.zone-tabs {
  display: flex;
  align-items: center;
}

.zone-panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 20px 24px 24px;
}

.zone-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0f2f5;
}

.zone-panel__title {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 700;
  color: #303133;
}

.zone-panel__desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.location-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1400px) {
  .location-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 1000px) {
  .location-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .location-grid { grid-template-columns: 1fr; }
}

.location-card {
  border-radius: 12px;
  border: 1px solid #e4e9f2;
  padding: 14px 14px 12px;
  background: #fafbfc;
  transition: box-shadow .2s, transform .2s;
}

.location-card:hover {
  box-shadow: 0 8px 24px rgba(64, 158, 255, .12);
  transform: translateY(-2px);
}

.location-card.is-empty { border-left: 4px solid #c5d0e0; }
.location-card.is-partial-low { border-left: 4px solid #f5d565; }
.location-card.is-partial-mid { border-left: 4px solid #f59e42; }
.location-card.is-near-full { border-left: 4px solid #ea580c; }
.location-card.is-full { border-left: 4px solid #dc2626; }

.location-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.location-card__id {
  font-size: 12px;
  font-weight: 700;
  color: #409eff;
  font-family: ui-monospace, monospace;
}

.location-card__name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  line-height: 1.4;
}

.location-card__meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.location-card__rate {
  font-weight: 700;
  color: #606266;
}

.bin-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-bottom: 10px;
}

.bin-cell {
  aspect-ratio: 1;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  cursor: default;
  transition: transform .15s, box-shadow .15s;
  border: 1px solid transparent;
  padding: 0;
  width: 100%;
  font: inherit;
  background: none;
}

.bin-cell.is-clickable {
  cursor: pointer;
}

.bin-cell.is-clickable:hover {
  transform: scale(1.06);
  box-shadow: 0 4px 12px rgba(0, 0, 0, .12);
  z-index: 1;
}

.bin-cell.is-focused {
  outline: 2px solid #38bdf8;
  outline-offset: 1px;
}

.bin-cell:disabled {
  cursor: not-allowed;
  opacity: 0.85;
}

.bin-cell__3d {
  font-size: 9px;
  font-weight: 700;
  color: #409eff;
  opacity: 0.85;
}

.bin-cell__text {
  font-size: 11px;
  font-weight: 700;
  color: rgba(0, 0, 0, .45);
}

.bin-cell.is-empty {
  background: linear-gradient(145deg, #eef2f7, #dce4ef);
  border-color: #c5d0e0;
}

.bin-cell.is-partial-low {
  background: linear-gradient(145deg, #fef9c3, #fde68a);
  border-color: #f5d565;
}
.bin-cell.is-partial-low .bin-cell__text { color: #a16207; }

.bin-cell.is-partial-mid {
  background: linear-gradient(145deg, #ffedd5, #fdba74);
  border-color: #f59e42;
}
.bin-cell.is-partial-mid .bin-cell__text { color: #c2410c; }

.bin-cell.is-near-full {
  background: linear-gradient(145deg, #fed7aa, #fb923c);
  border-color: #ea580c;
}
.bin-cell.is-near-full .bin-cell__text { color: #9a3412; }

.bin-cell.is-full {
  background: linear-gradient(145deg, #fecaca, #f87171);
  border-color: #dc2626;
}
.bin-cell.is-full .bin-cell__text { color: #991b1b; }

.location-card__foot {
  padding-top: 8px;
  border-top: 1px dashed #e4e7ed;
}

.foot-material {
  font-size: 12px;
  color: #606266;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bin-tooltip {
  font-size: 12px;
  line-height: 1.7;
  min-width: 160px;
}

.bin-tooltip b {
  color: #909399;
  font-weight: 600;
  margin-right: 6px;
}

.bin-tooltip__action {
  margin-top: 6px;
  color: #409eff;
  font-size: 11px;
}

:deep(.shelf-3d-dialog .el-dialog__body) {
  padding-top: 8px;
}

.shelf-3d-dialog__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
}

.shelf-3d-dialog__meta strong {
  color: #303133;
}
</style>
