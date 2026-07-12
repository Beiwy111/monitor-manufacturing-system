<template>
  <div class="sim">
    <div class="sim__chart-wrap">
      <div class="sim__chart">
        <svg viewBox="0 0 320 220" class="sim__svg">
          <defs>
            <linearGradient id="specGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#4a90e2" />
              <stop offset="50%" stop-color="#50c878" />
              <stop offset="100%" stop-color="#e74c3c" />
            </linearGradient>
          </defs>
          <path d="M40,180 Q120,20 280,160 L40,180 Z" fill="url(#specGrad)" opacity=".35" />
          <polygon :points="gamutTriangle" fill="none" stroke="#1677ff" stroke-width="2" />
          <text x="8" y="16" font-size="11" fill="#666">CIE 1931 色域示意</text>
        </svg>
      </div>
      <div class="sim__readout">
        <div class="sim__readout-row"><span>sRGB</span><strong>{{ form.srgb }}%</strong></div>
        <div class="sim__readout-row"><span>DCI-P3</span><strong>{{ form.dcip3 }}%</strong></div>
        <div class="sim__readout-row"><span>ΔE</span><strong>{{ form.deltaE }}</strong></div>
      </div>
    </div>
    <el-button type="primary" size="small" :loading="scanning" @click="runScan">开始色域扫描</el-button>
    <el-progress v-if="scanning" :percentage="scanProgress" :stroke-width="8" style="margin-top:8px" />
    <el-form label-width="88px" size="small" class="sim__form">
      <el-form-item label="sRGB%">
        <el-input-number v-model="form.srgb" :min="0" :max="150" :step="0.1" :precision="1" />
      </el-form-item>
      <el-form-item label="DCI-P3%">
        <el-input-number v-model="form.dcip3" :min="0" :max="150" :step="0.1" :precision="1" />
      </el-form-item>
      <el-form-item label="色准 ΔE">
        <el-input-number v-model="form.deltaE" :min="0" :max="10" :step="0.1" :precision="1" />
      </el-form-item>
      <el-form-item label="判定">
        <el-radio-group v-model="form.passed">
          <el-radio :value="true">合格</el-radio>
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

const scanning = ref(false)
const scanProgress = ref(0)
const form = reactive({ srgb: 99.0, dcip3: 92.0, deltaE: 1.8, passed: true })

const gamutTriangle = computed(() => {
  const s = form.srgb / 100
  const d = form.dcip3 / 100
  return `80,${200 - s * 40} 200,${60 + d * 30} 140,180`
})

function runScan() {
  scanning.value = true
  scanProgress.value = 0
  const timer = setInterval(() => {
    scanProgress.value += 12
    if (scanProgress.value >= 100) {
      clearInterval(timer)
      form.srgb = +(96 + Math.random() * 4).toFixed(1)
      form.dcip3 = +(88 + Math.random() * 8).toFixed(1)
      form.deltaE = +(1.2 + Math.random() * 1.5).toFixed(1)
      form.passed = form.deltaE <= 2
      scanning.value = false
      emitValue()
    }
  }, 180)
}

function emitValue() {
  emit('update:modelValue', {
    ...form,
    measuredValue: `sRGB ${form.srgb}% / P3 ${form.dcip3}% / ΔE ${form.deltaE}`
  })
}

watch(form, emitValue, { deep: true })

watch(() => props.modelValue, (v) => {
  if (!v || !Object.keys(v).length) return
  Object.assign(form, { srgb: v.srgb ?? 99, dcip3: v.dcip3 ?? 92, deltaE: v.deltaE ?? 1.8, passed: v.passed ?? true })
}, { immediate: true })
</script>

<style scoped>
.sim__chart-wrap { display: flex; gap: 12px; margin-bottom: 8px; }
.sim__chart { flex: 1; border: 1px solid #e8e8e8; background: #fafafa; }
.sim__svg { width: 100%; height: 160px; }
.sim__readout { width: 120px; font-size: 13px; }
.sim__readout-row { display: flex; justify-content: space-between; margin-bottom: 6px; }
.sim__form { margin-top: 8px; }
</style>
