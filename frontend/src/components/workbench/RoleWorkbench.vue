<template>
  <div :class="embedded ? 'ruoyi-workbench-embed' : 'ruoyi-page ruoyi-workbench'">
    <div v-if="statusItems.length" class="ruoyi-stats">
      <span
        v-for="item in statusItems"
        :key="item.label"
        class="ruoyi-stats__item"
        :class="{
          'ruoyi-stats__item--warn': item.warn,
          'ruoyi-stats__item--danger': item.danger
        }"
      >
        {{ item.label }}：<em>{{ item.value }}</em>
      </span>
    </div>

    <div v-if="shortcuts.length" class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">快捷入口</span>
      <router-link
        v-for="s in shortcuts"
        :key="s.path"
        :to="s.path"
        class="ruoyi-link-btn"
      >
        {{ s.label }}
      </router-link>
    </div>

    <div class="ruoyi-toolbar ruoyi-toolbar--sub">
      <span class="ruoyi-toolbar__title">待办事项</span>
      <el-tag size="small" type="info">{{ todos.length }} 项</el-tag>
    </div>

    <div class="ruoyi-table-wrap">
      <el-table :data="todos" highlight-current-row @row-click="goTodo">
        <el-table-column prop="type" label="类型" width="90" align="center" />
        <el-table-column prop="title" label="待办内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ref" label="单号" width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="goTodo(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMesStore } from '@/stores/mes'

const props = defineProps({
  roleKey: { type: String, required: true },
  statusItems: { type: Array, default: () => [] },
  shortcuts: { type: Array, default: () => [] },
  embedded: { type: Boolean, default: false }
})

const router = useRouter()
const userStore = useUserStore()
const mesStore = useMesStore()

const todos = computed(() => mesStore.todosForRole(props.roleKey, userStore.userInfo?.username))

function goTodo(row) {
  if (row.path) router.push(row.path)
}
</script>
