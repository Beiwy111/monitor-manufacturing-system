<template>
  <div class="progress-chart">
    <div v-if="loading" class="progress-chart__overlay">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中</span>
    </div>
    <div v-else-if="error" class="progress-chart__overlay progress-chart__overlay--error">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ error }}</span>
    </div>
    <div v-else-if="empty" class="progress-chart__overlay progress-chart__overlay--empty">
      <el-icon><Document /></el-icon>
      <span>{{ emptyText }}</span>
    </div>
    <div ref="chartRef" class="progress-chart__canvas" />
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, shallowRef } from 'vue'
import { Loading, WarningFilled, Document } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { mergeChartOption } from '@/styles/chartTheme'

const props = defineProps({
  option: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' }
})

const chartRef = ref(null)
const chart = shallowRef(null)
let observer = null

function render() {
  if (!chartRef.value || props.loading || props.error || props.empty) return
  if (!chart.value) {
    chart.value = echarts.init(chartRef.value, 'mes', { renderer: 'canvas' })
  }
  chart.value.setOption(mergeChartOption(props.option), {
    notMerge: false,
    lazyUpdate: true,
    silent: true
  })
}

function resize() {
  chart.value?.resize()
}

watch(
  () => [props.option, props.loading, props.error, props.empty],
  () => {
    if (props.loading || props.error || props.empty) {
      chart.value?.clear()
      return
    }
    render()
  },
  { deep: true }
)

onMounted(() => {
  render()
  observer = new ResizeObserver(resize)
  if (chartRef.value) observer.observe(chartRef.value)
})

onUnmounted(() => {
  observer?.disconnect()
  chart.value?.dispose()
  chart.value = null
})
</script>

<style scoped>
.progress-chart {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 200px;
}
.progress-chart__canvas {
  width: 100%;
  height: 100%;
  min-height: 200px;
}
.progress-chart__overlay {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
  background: #fafbfc;
}
.progress-chart__overlay--error {
  color: #b45309;
}
.progress-chart__overlay--empty {
  color: #94a3b8;
}
</style>
