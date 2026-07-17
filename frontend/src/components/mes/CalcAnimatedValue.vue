<template>
  <span class="calc-anim-val" :class="{ 'calc-anim-val--lit': lit }">{{ display }}</span>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  value: { type: [Number, String], default: 0 },
  duration: { type: Number, default: 520 },
  decimals: { type: Number, default: 0 },
  suffix: { type: String, default: '' },
  lit: { type: Boolean, default: true },
  animate: { type: Boolean, default: true }
})

const display = ref(format(0))

let raf = 0
let startTs = 0
let fromNum = 0
let toNum = 0

function parseNum(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

function format(n) {
  const fixed = props.decimals > 0 ? n.toFixed(props.decimals) : String(Math.round(n))
  return `${fixed}${props.suffix}`
}

function tick(ts) {
  if (!startTs) startTs = ts
  const p = Math.min(1, (ts - startTs) / props.duration)
  const eased = 1 - (1 - p) ** 3
  display.value = format(fromNum + (toNum - fromNum) * eased)
  if (p < 1) {
    raf = requestAnimationFrame(tick)
  }
}

function runAnimation() {
  cancelAnimationFrame(raf)
  toNum = parseNum(props.value)
  if (!props.animate || !props.lit) {
    display.value = format(toNum)
    fromNum = toNum
    return
  }
  startTs = 0
  raf = requestAnimationFrame(tick)
}

watch(
  () => [props.value, props.lit, props.animate],
  () => {
    fromNum = parseNum(display.value)
    runAnimation()
  },
  { immediate: true }
)

onBeforeUnmount(() => cancelAnimationFrame(raf))
</script>

<style scoped>
.calc-anim-val {
  font-variant-numeric: tabular-nums;
  transition: color 0.35s ease, opacity 0.35s ease;
  color: #9ca3af;
}
.calc-anim-val--lit {
  color: #15803d;
  font-weight: 700;
}
</style>
