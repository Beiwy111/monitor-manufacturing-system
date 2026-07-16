<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :loading="loading"
    filterable
    clearable
    style="width: 100%"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-option
      v-for="slot in options"
      :key="slot.slotCode"
      :label="slot.label"
      :value="slot.slotCode"
    >
      <div class="slot-option">
        <span class="slot-option__code">{{ slot.slotCode }}</span>
        <span class="slot-option__meta">{{ slot.zoneName }} · {{ slot.locationName }} · 第{{ slot.rowNo }}层第{{ slot.colNo }}格</span>
      </div>
    </el-option>
  </el-select>
</template>

<script setup>
import { ref, watch } from 'vue'
import { fetchAvailableSlots } from '@/api/warehouse'

const props = defineProps({
  modelValue: { type: String, default: '' },
  category: { type: String, default: '' },
  zoneCode: { type: String, default: '' },
  placeholder: { type: String, default: '请选择存放库位' }
})

defineEmits(['update:modelValue'])

const loading = ref(false)
const options = ref([])

async function load() {
  loading.value = true
  try {
    const res = await fetchAvailableSlots({
      category: props.category || undefined,
      zoneCode: props.zoneCode || undefined
    })
    options.value = res?.data || res || []
  } catch {
    options.value = []
  } finally {
    loading.value = false
  }
}

watch(() => [props.category, props.zoneCode], load, { immediate: true })
</script>

<style scoped>
.slot-option {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  padding: 2px 0;
}
.slot-option__code {
  font-weight: 600;
  color: #303133;
}
.slot-option__meta {
  font-size: 12px;
  color: #909399;
}
</style>
