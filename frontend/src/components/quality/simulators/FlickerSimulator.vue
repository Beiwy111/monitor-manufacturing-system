<template>
  <div class="sim">
    <div class="sim__display" :class="{ 'is-flicker': flickering }">
      <div class="sim__bars" v-if="flickering">
        <span v-for="n in 24" :key="n" class="sim__bar" />
      </div>
      <div v-else class="sim__stable">稳定画面</div>
      <div class="sim__freq">{{ form.frequency }} Hz</div>
    </div>
    <el-button type="primary" size="small" :loading="testing" @click="runTest">开始屏闪检测</el-button>
    <el-form label-width="88px" size="small" class="sim__form">
      <el-form-item label="PWM频率">
        <el-input-number v-model="form.frequency" :min="0" :max="5000" :step="50" />
        <span class="sim__unit">Hz</span>
      </el-form-item>
      <el-form-item label="肉眼可见">
        <el-switch v-model="form.visibleFlicker" />
      </el-form-item>
      <el-form-item label="判定">
        <el-radio-group v-model="form.passed">
          <el-radio :value="true">合格(≥1000Hz)</el-radio>
          <el-radio :value="false">不合格</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'

const props = defineProps({ modelValue: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:modelValue'])

const testing = ref(false)
const flickering = ref(false)
const form = reactive({ frequency: 1200, visibleFlicker: false, passed: true })

function runTest() {
  testing.value = true
  flickering.value = true
  let step = 0
  const timer = setInterval(() => {
    step++
    form.frequency = 800 + step * 80
    if (step >= 6) {
      clearInterval(timer)
      form.frequency = 1150 + Math.round(Math.random() * 200)
      form.visibleFlicker = form.frequency < 1000
      form.passed = form.frequency >= 1000 && !form.visibleFlicker
      flickering.value = false
      testing.value = false
      emitValue()
    }
  }, 350)
}

function emitValue() {
  emit('update:modelValue', {
    ...form,
    measuredValue: `${form.frequency}Hz${form.visibleFlicker ? ' 可见闪烁' : ''}`
  })
}

watch(form, emitValue, { deep: true })

watch(() => props.modelValue, (v) => {
  if (!v || !Object.keys(v).length) return
  Object.assign(form, {
    frequency: v.frequency ?? 1200,
    visibleFlicker: v.visibleFlicker ?? false,
    passed: v.passed ?? true
  })
}, { immediate: true })
</script>

<style scoped>
.sim__display {
  height: 200px;
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  border: 1px solid #333;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sim__display.is-flicker { animation: pulse 0.15s infinite alternate; }
@keyframes pulse { from { opacity: 1; } to { opacity: 0.85; } }
.sim__bars { display: flex; width: 100%; height: 100%; }
.sim__bar { flex: 1; background: repeating-linear-gradient(90deg, #fff 0 2px, transparent 2px 6px); opacity: .15; }
.sim__stable { color: #8ec5ff; font-size: 14px; }
.sim__freq {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #52c41a;
  font-family: monospace;
}
.sim__unit { margin-left: 6px; color: #999; font-size: 12px; }
.sim__form { margin-top: 12px; }
</style>
