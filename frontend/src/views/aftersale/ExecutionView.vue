<template>
  <ModulePageShell>
    <div class="page-head">
      <span class="page-title">执行协同</span>
      <el-select v-model="caseNo" filterable clearable placeholder="筛选案例" size="small" style="width:200px" @change="loadTasks">
        <el-option v-for="c in caseOptions" :key="c.caseNo" :label="c.caseNo" :value="c.caseNo" />
      </el-select>
    </div>

    <div class="exec-layout" v-loading="loading">
      <ModulePanelSection>
        <div class="panel-title">执行任务</div>
        <el-table :data="tasks" border size="small">
          <el-table-column prop="caseNo" label="案例" width="130" />
          <el-table-column prop="title" label="任务" min-width="140" />
          <el-table-column prop="taskTypeCn" label="类型" width="100" />
          <el-table-column prop="assignee" label="负责人" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status==='DONE'?'success':row.status==='IN_PROGRESS'?'warning':'info'" size="small">{{ row.statusCn }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="dueAt" label="截止" width="148" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status==='PENDING'" link type="primary" size="small" @click="setStatus(row,'IN_PROGRESS')">开始</el-button>
              <el-button v-if="row.status==='IN_PROGRESS'" link type="success" size="small" @click="setStatus(row,'DONE')">完成</el-button>
            </template>
          </el-table-column>
        </el-table>
      </ModulePanelSection>

      <ModulePanelSection>
        <div class="panel-title">时间线 · {{ caseNo || '全部案例' }}</div>
        <el-timeline v-if="timeline.length">
          <el-timeline-item v-for="item in timeline" :key="item.taskId + item.status" :type="item.type" :timestamp="item.time">
            <b>{{ item.title }}</b>
            <div class="tl-sub">{{ item.statusCn }} · {{ item.assignee || '未指定' }}</div>
            <div v-if="item.remark" class="tl-remark">{{ item.remark }}</div>
          </el-timeline-item>
        </el-timeline>
        <div v-else class="empty-hint">暂无执行任务，请先在方案审批通过方案</div>
      </ModulePanelSection>
    </div>
  </ModulePageShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchCaseViews, fetchWorkflowTasks, updateWorkflowTask } from '@/api/aftersale'
import ModulePageShell from '@/components/module/ModulePageShell.vue'
import ModulePanelSection from '@/components/module/ModulePanelSection.vue'

const route = useRoute()
const caseNo = ref(route.query.caseNo || '')
const caseOptions = ref([])
const tasks = ref([])
const loading = ref(false)

const timeline = computed(() => tasks.value.flatMap(t => {
  const items = []
  if (t.createdAt) items.push({ ...t, time: t.createdAt, type: 'primary', statusCn: '任务创建' })
  if (t.startedAt) items.push({ ...t, time: t.startedAt, type: 'warning', statusCn: '开始执行' })
  if (t.completedAt) items.push({ ...t, time: t.completedAt, type: 'success', statusCn: '已完成' })
  return items
}).sort((a, b) => String(a.time).localeCompare(String(b.time))))

async function loadCases() {
  const res = await fetchCaseViews()
  caseOptions.value = (res?.data ?? res ?? []).filter(c => ['EXECUTING', 'PENDING_RECHECK'].includes(c.caseStatus))
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await fetchWorkflowTasks(caseNo.value || undefined)
    tasks.value = res?.data ?? res ?? []
  } finally { loading.value = false }
}

async function setStatus(row, status) {
  try {
    await updateWorkflowTask({ taskId: row.taskId, caseNo: row.caseNo, status, assignee: row.assignee })
    ElMessage.success('任务已更新')
    await loadTasks(); await loadCases()
  } catch (e) { ElMessage.error(e?.message || '更新失败') }
}

onMounted(async () => { await loadCases(); await loadTasks() })
</script>

<style scoped>
.page-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
.page-title { font-size:18px; font-weight:700; color:#001b3f; }
.exec-layout { display:grid; grid-template-columns:1.2fr .8fr; gap:14px; }
.panel-title { font-size:15px; font-weight:600; margin-bottom:10px; color:#001b3f; }
.tl-sub { font-size:12px; color:#7a8796; margin-top:4px; }
.tl-remark { font-size:12px; color:#50657a; margin-top:4px; }
.empty-hint { color:#99a7b5; font-size:13px; padding:24px 0; }
@media (max-width: 1100px) { .exec-layout { grid-template-columns:1fr; } }
</style>
