<template>
  <div v-if="showSearch || statusOptions.length" class="ruoyi-query">
    <el-form :inline="true" @submit.prevent="emitChange">
      <el-form-item v-if="showSearch" label="关键字">
        <el-input
          v-model="keyword"
          :placeholder="searchPlaceholder"
          clearable
          style="width: 180px"
          @input="emitChange"
          @clear="emitChange"
        />
      </el-form-item>
      <el-form-item v-if="statusOptions.length" label="状态">
        <el-select
          v-model="status"
          placeholder="全部状态"
          clearable
          style="width: 130px"
          @change="emitChange"
        >
          <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <slot name="prefix" />
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="emitChange">搜索</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </el-form-item>
      <slot name="extra" />
    </el-form>
  </div>

  <div v-if="title || actions.length || $slots.suffix" class="ruoyi-toolbar">
    <span v-if="title" class="ruoyi-toolbar__title ruoyi-page__title">{{ title }}</span>
    <el-button
      v-for="btn in actions"
      :key="btn.label"
      :type="btn.type || 'default'"
      @click="$emit('action', btn.key)"
    >
      {{ btn.label }}
    </el-button>
    <slot name="suffix" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, default: '' },
  showSearch: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '请输入关键字' },
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

function reset() {
  keyword.value = ''
  status.value = ''
  emitChange()
}
</script>
