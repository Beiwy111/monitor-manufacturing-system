<template>
  <div class="sim">
    <div class="sim__screen" :style="{ background: screenColor }" @click="onScreenClick">
      <div v-for="(p, i) in marks" :key="i" class="sim__mark" :class="`is-${p.type}`" :style="{ left: p.x + '%', top: p.y + '%' }" />
      <div class="sim__screen-tip">{{ colorTabs[colorIdx].tip }}</div>
    </div>
    <div class="sim__tabs">
      <button v-for="(t, i) in colorTabs" :key="t.key" type="button" class="sim__tab" :class="{ 'is-active': colorIdx === i }" @click="colorIdx = i">{{ t.label }}</button>
    </div>
    <el-form label-width="88px" size="small" class="sim__form">
      <el-form-item label="亮点数">
        <el-input-number v-model="form.brightDots" :min="0" :max="99" />
      </el-form-item>
      <el-form-item label="暗点数">
        <el-input-number v-model="form.darkDots" :min="0" :max="99" />
      </el-form-item>
      <el-form-item label="判定">
        <el-radio-group v-model="form.passed">
          <el-radio :value="true">合格</el-radio>
          <el-radio :value="false">不合格</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="缺陷位置描述" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue'])

const colorTabs = [
  { key: 'red', label: '纯红色', color: '#e60000', tip: '检查暗点（始终发黑）' },
  { key: 'green', label: '纯绿色', color: '#00b400', tip: '检查暗点与异物' },
  { key: 'blue', label: '纯蓝色', color: '#0058e6', tip: '检查暗点' },
  { key: 'white', label: '纯白色', color: '#f5f5f5', tip: '检查暗点与脏污' },
  { key: 'black', label: '纯黑色', color: '#0a0a0a', tip: '检查亮点（常亮像素）' }
]
const colorIdx = ref(4)
const marks = ref([])

const form = reactive({
  brightDots: 0,
  darkDots: 0,
  passed: true,
  remark: ''
})

const screenColor = computed(() => colorTabs[colorIdx.value].color)

function onScreenClick(e) {
  const rect = e.currentTarget.getBoundingClientRect()
  const x = ((e.clientX - rect.left) / rect.width) * 100
  const y = ((e.clientY - rect.top) / rect.height) * 100
  const type = colorIdx.value === 4 ? 'bright' : 'dark'
  marks.value.push({ x, y, type })
  if (type === 'bright') form.brightDots++
  else form.darkDots++
  emitValue()
}

function emitValue() {
  emit('update:modelValue', {
    brightDots: form.brightDots,
    darkDots: form.darkDots,
    passed: form.passed,
    remark: form.remark,
    measuredValue: `亮点${form.brightDots} 暗点${form.darkDots}`,
    marks: [...marks.value]
  })
}

watch(form, emitValue, { deep: true })

watch(() => props.modelValue, (v) => {
  if (!v || Object.keys(v).length === 0) return
  form.brightDots = v.brightDots ?? 0
  form.darkDots = v.darkDots ?? 0
  form.passed = v.passed ?? true
  form.remark = v.remark ?? ''
  marks.value = v.marks || []
}, { immediate: true })
</script>

<style scoped>
.sim__screen {
  position: relative;
  height: 220px;
  border: 1px solid #303030;
  cursor: crosshair;
  overflow: hidden;
}
.sim__screen-tip {
  position: absolute;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,.55);
  color: #fff;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 2px;
}
.sim__mark { position: absolute; width: 8px; height: 8px; border-radius: 50%; transform: translate(-50%,-50%); }
.sim__mark.is-bright { background: #fff; box-shadow: 0 0 6px #fff; }
.sim__mark.is-dark { background: #111; border: 1px solid #666; }
.sim__tabs { display: flex; gap: 4px; margin: 8px 0; flex-wrap: wrap; }
.sim__tab {
  border: 1px solid #dcdfe6;
  background: #fff;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}
.sim__tab.is-active { background: #409eff; color: #fff; border-color: #409eff; }
.sim__form { margin-top: 8px; }
</style>
