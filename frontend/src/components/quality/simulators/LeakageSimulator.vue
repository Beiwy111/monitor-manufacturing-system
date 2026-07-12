<template>
  <div class="sim">
    <div class="sim__darkroom">
      <div class="sim__panel">
        <div v-for="c in corners" :key="c.key" class="sim__corner" :class="`lv-${form[c.key]}`">
          <span class="sim__corner-label">{{ c.label }}</span>
        </div>
        <div class="sim__center-glow" :class="`lv-${form.center}`" />
      </div>
      <p class="sim__hint">暗室黑场 — 调节四角漏光等级（0无 1轻微 2明显）</p>
    </div>
    <el-form label-width="72px" size="small" class="sim__form">
      <el-form-item v-for="c in corners" :key="c.key" :label="c.label">
        <el-slider v-model="form[c.key]" :min="0" :max="2" :step="1" show-stops :marks="{ 0: '无', 1: '轻', 2: '重' }" />
      </el-form-item>
      <el-form-item label="中心">
        <el-slider v-model="form.center" :min="0" :max="2" :step="1" show-stops />
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
import { reactive, watch } from 'vue'

const props = defineProps({ modelValue: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:modelValue'])

const corners = [
  { key: 'tl', label: '左上' },
  { key: 'tr', label: '右上' },
  { key: 'bl', label: '左下' },
  { key: 'br', label: '右下' }
]

const form = reactive({ tl: 0, tr: 0, bl: 0, br: 0, center: 0, passed: true })

function emitValue() {
  const max = Math.max(form.tl, form.tr, form.bl, form.br, form.center)
  const levels = ['无漏光', '轻微', '明显']
  emit('update:modelValue', {
    ...form,
    maxLevel: max,
    measuredValue: `四角最大${levels[max]}`,
    passed: form.passed && max <= 1
  })
}

watch(form, emitValue, { deep: true })

watch(() => props.modelValue, (v) => {
  if (!v || !Object.keys(v).length) return
  Object.assign(form, { tl: v.tl ?? 0, tr: v.tr ?? 0, bl: v.bl ?? 0, br: v.br ?? 0, center: v.center ?? 0, passed: v.passed ?? true })
}, { immediate: true })
</script>

<style scoped>
.sim__darkroom { background: #000; padding: 16px; border-radius: 4px; }
.sim__panel {
  position: relative;
  height: 200px;
  background: #111;
  border: 2px solid #333;
}
.sim__corner {
  position: absolute;
  width: 48%;
  height: 48%;
}
.sim__corner:nth-child(1) { top: 0; left: 0; }
.sim__corner:nth-child(2) { top: 0; right: 0; }
.sim__corner:nth-child(3) { bottom: 0; left: 0; }
.sim__corner:nth-child(4) { bottom: 0; right: 0; }
.sim__corner.lv-1 { box-shadow: inset 0 0 40px rgba(255,255,200,.25); }
.sim__corner.lv-2 { box-shadow: inset 0 0 60px rgba(255,255,220,.45); }
.sim__center-glow {
  position: absolute;
  inset: 25%;
  border-radius: 50%;
}
.sim__center-glow.lv-1 { background: radial-gradient(circle, rgba(80,80,80,.3), transparent); }
.sim__center-glow.lv-2 { background: radial-gradient(circle, rgba(120,120,120,.5), transparent); }
.sim__corner-label { position: absolute; top: 4px; left: 6px; font-size: 11px; color: #888; }
.sim__hint { color: #aaa; font-size: 12px; margin: 8px 0 0; text-align: center; }
.sim__form { margin-top: 12px; }
</style>
