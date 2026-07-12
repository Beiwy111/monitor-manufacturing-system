<template>
  <div class="sim">
    <div class="sim__grid">
      <div v-for="(cell, i) in grid" :key="i" class="sim__cell" :style="{ background: cellColor(cell) }">
        <span class="sim__cell-val">{{ cell }}%</span>
      </div>
      <div class="sim__grid-center" />
    </div>
    <p class="sim__hint">九点亮度（中心为基准 100%）· 均匀性 {{ uniformity }}%</p>
    <el-button size="small" type="primary" plain :loading="measuring" @click="autoMeasure">模拟九点测光</el-button>
    <el-form label-width="88px" size="small" class="sim__form">
      <el-form-item label="均匀性%">
        <el-input-number v-model="form.uniformity" :min="0" :max="100" :precision="1" />
      </el-form-item>
      <el-form-item label="判定">
        <el-radio-group v-model="form.passed">
          <el-radio :value="true">合格(≥80%)</el-radio>
          <el-radio :value="false">不合格</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'

const props = defineProps({ modelValue: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:modelValue'])

const measuring = ref(false)
const grid = ref([92, 88, 90, 87, 100, 89, 91, 86, 88])
const form = reactive({ uniformity: 86, passed: true })

const uniformity = computed(() => form.uniformity)

function cellColor(pct) {
  const v = Math.min(100, Math.max(70, pct))
  const g = Math.round((v / 100) * 200)
  return `rgb(${g},${g},${g})`
}

function calcUniformity(vals) {
  const center = vals[4]
  const min = Math.min(...vals)
  return +((min / center) * 100).toFixed(1)
}

function autoMeasure() {
  measuring.value = true
  setTimeout(() => {
    grid.value = grid.value.map(() => +(82 + Math.random() * 18).toFixed(0))
    grid.value[4] = 100
    form.uniformity = calcUniformity(grid.value)
    form.passed = form.uniformity >= 80
    measuring.value = false
    emitValue()
  }, 900)
}

function emitValue() {
  emit('update:modelValue', {
    grid: [...grid.value],
    uniformity: form.uniformity,
    passed: form.passed,
    measuredValue: `${form.uniformity}%`
  })
}

watch([form, grid], emitValue, { deep: true })

watch(() => props.modelValue, (v) => {
  if (!v || !Object.keys(v).length) return
  if (v.grid?.length === 9) grid.value = [...v.grid]
  form.uniformity = v.uniformity ?? 86
  form.passed = v.passed ?? true
}, { immediate: true })
</script>

<style scoped>
.sim__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  height: 200px;
  position: relative;
}
.sim__cell {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #444;
  font-size: 12px;
  color: #fff;
}
.sim__cell-val { text-shadow: 0 0 4px #000; }
.sim__hint { font-size: 12px; color: #666; margin: 8px 0; }
.sim__form { margin-top: 8px; }
</style>
