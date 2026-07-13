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
      <el-tag size="small" type="info">{{ mergedTodos.length }} 项</el-tag>
    </div>

    <div class="ruoyi-table-wrap">
      <el-table :data="mergedTodos" border stripe highlight-current-row @row-click="goTodo">
        <el-table-column prop="type" label="类型" width="90" align="center" />
        <el-table-column prop="title" label="待办内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ref" label="单号" width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <template v-if="row.rcaTask">
              <el-button v-if="row.status==='PENDING'" link type="primary" @click.stop="acceptRca(row)">接收</el-button>
              <el-button v-else-if="row.status!=='COMPLETED'" link type="success" @click.stop="completeRca(row)">完成</el-button>
              <el-tag v-else size="small" type="success">已完成</el-tag>
            </template>
            <el-button v-else link type="primary" @click.stop="goTodo(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <OperationLogPanel :logs="recentLogs" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMesStore } from '@/stores/mes'
import OperationLogPanel from '@/components/mes/OperationLogPanel.vue'
import { fetchRcaTasks, updateRcaTask } from '@/api/aftersale'
import { ElMessage, ElMessageBox } from 'element-plus'

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
const rcaTasks = ref([])
const departmentMap = { quality: 'QUALITY', purchase: 'PURCHASE', device: 'DEVICE', cost: 'COST', manager: 'PRODUCTION', planner: 'PRODUCTION' }
const pathMap = { QUALITY: '/dashboard/quality', PURCHASE: '/dashboard/purchase', DEVICE: '/dashboard/device', COST: '/dashboard/cost', PRODUCTION: '/dashboard/manager' }
const mergedTodos = computed(() => [
  ...rcaTasks.value.map((task) => ({
    type: '售后协查', title: task.title, ref: task.caseNo,
    path: pathMap[task.department] || props.shortcuts?.[0]?.path,
    priority: task.priority, status: task.status, taskId: task.taskId, rcaTask: true
  })),
  ...todos.value
])
const recentLogs = computed(() => mesStore.operationLogs.slice(0, 12))

function goTodo(row) {
  if (row.path) router.push(row.path)
}

async function acceptRca(row) {
  await updateRcaTask({ taskId: row.taskId, status: 'ACCEPTED', result: '任务已接收，开始协查' })
  const task = rcaTasks.value.find((x) => x.taskId === row.taskId)
  if (task) task.status = 'ACCEPTED'
  ElMessage.success('协同任务已接收')
}

async function completeRca(row) {
  const { value } = await ElMessageBox.prompt('填写现场核查、复检或处理结论', '完成售后协查', {
    inputType: 'textarea', inputPlaceholder: '请输入有证据支撑的处理结果',
    inputValidator: (v) => !!v?.trim() || '处理结果不能为空'
  })
  await updateRcaTask({ taskId: row.taskId, status: 'COMPLETED', result: value })
  const task = rcaTasks.value.find((x) => x.taskId === row.taskId)
  if (task) { task.status = 'COMPLETED'; task.result = value }
  ElMessage.success('处理结果已回流售后事件')
}

onMounted(async () => {
  const department = departmentMap[props.roleKey]
  if (!department) return
  try { rcaTasks.value = await fetchRcaTasks(department) || [] }
  catch { rcaTasks.value = [] }
})
</script>
