<template>
  <div class="mes-toolbar">
    <span v-if="title" class="mes-toolbar-title">{{ title }}</span>
    <slot name="prefix" />
    <el-input
      v-if="showSearch"
      v-model="keyword"
      :placeholder="searchPlaceholder"
      clearable
      style="width: 200px"
      @input="emitChange"
      @clear="emitChange"
    />
    <el-select
      v-if="statusOptions.length"
      v-model="status"
      placeholder="状态筛选"
      clearable
      style="width: 140px"
      @change="emitChange"
    >
      <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
    </el-select>
    <slot name="extra" />
    <el-button v-for="btn in actions" :key="btn.label" :type="btn.type || 'default'" @click="$emit('action', btn.key)">
      {{ btn.label }}
    </el-button>
    <slot name="suffix" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  showSearch: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '搜索...' },
  statusOptions: { type: Array, default: () => [] },
  actions: { type: Array, default: () => [] },
  modelKeyword: { type: String, default: '' },
  modelStatus: { type: String, default: '' }
})

const emit = defineEmits(['update:modelKeyword', 'update:modelStatus', 'change', 'action'])

const keyword = ref(props.modelKeyword)
const status = ref(props.modelStatus)

watch(() => props.modelKeyword, (v) => { keyword.value = v })
watch(() => props.modelStatus, (v) => { status.value = v })

function emitChange() {
  emit('update:modelKeyword', keyword.value)
  emit('update:modelStatus', status.value)
  emit('change', { keyword: keyword.value, status: status.value })
}
</script>
