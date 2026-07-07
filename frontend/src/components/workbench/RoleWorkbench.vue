<template>
  <div class="mes-workbench">
    <div class="mes-status-strip">
      <div v-for="item in statusItems" :key="item.label" class="mes-status-item">
        <div class="mes-status-label">{{ item.label }}</div>
        <div class="mes-status-value" :class="{ 'mes-status-value--warn': item.warn, 'mes-status-value--danger': item.danger }">
          {{ item.value }}
        </div>
      </div>
    </div>
    <div class="mes-workbench-body">
      <div class="mes-workbench-main">
        <div class="mes-toolbar">
          <span class="mes-toolbar-title">待办事项</span>
          <el-tag size="small">{{ todos.length }} 项</el-tag>
        </div>
        <div class="mes-table-wrap">
          <el-table :data="todos" highlight-current-row style="width:100%" @row-click="goTodo">
            <el-table-column prop="type" label="类型" width="80" />
            <el-table-column prop="title" label="待办内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="ref" label="单号" width="140" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="goTodo(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <OperationLogPanel :logs="recentLogs" />
      </div>
      <div class="mes-workbench-side">
        <div class="mes-side-header">快捷入口</div>
        <div class="mes-shortcut-list">
          <router-link v-for="s in shortcuts" :key="s.path" :to="s.path" class="mes-shortcut-item">
            {{ s.label }}
          </router-link>
        </div>
        <div class="mes-side-header">最近日志</div>
        <div class="mes-side-body" style="flex:1;overflow:auto">
          <div v-for="log in recentLogs.slice(0, 8)" :key="log.id" class="mes-log-item" style="padding:8px 0;border:none">
            <span class="mes-log-time">{{ log.createdAt?.slice(11) }}</span>
            <span>{{ log.action }} · {{ log.target }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMesStore } from '@/stores/mes'
import OperationLogPanel from '@/components/mes/OperationLogPanel.vue'

const props = defineProps({
  roleKey: { type: String, required: true },
  statusItems: { type: Array, default: () => [] },
  shortcuts: { type: Array, default: () => [] }
})

const router = useRouter()
const userStore = useUserStore()
const mesStore = useMesStore()

const todos = computed(() => mesStore.todosForRole(props.roleKey, userStore.userInfo?.username))
const recentLogs = computed(() => mesStore.operationLogs.slice(0, 15))

function goTodo(row) {
  if (row.path) router.push(row.path)
}
</script>
