<template>
  <div ref="chartRef" class="board-chart" />
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, shallowRef } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true }
})

const chartRef = ref(null)
const chart = shallowRef(null)
let observer = null

function render() {
  if (!chartRef.value) return
  if (!chart.value) {
    chart.value = echarts.init(chartRef.value, null, { renderer: 'canvas' })
  }
  chart.value.setOption(props.option, { notMerge: true })
}

function resize() {
  chart.value?.resize()
}

watch(() => props.option, render, { deep: true })

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
.board-chart {
  width: 100%;
  height: 100%;
  min-height: 120px;
}
</style>
