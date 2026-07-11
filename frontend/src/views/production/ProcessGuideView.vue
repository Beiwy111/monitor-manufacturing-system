<template>
  <div class="ruoyi-page process-guide-page">
    <div class="ruoyi-toolbar">
      <span class="ruoyi-toolbar__title">工艺路线</span>
      <el-input v-model="keyword" clearable placeholder="产品 / 路线名称" style="width: 220px" />
      <el-button @click="loadData">刷新</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="filteredRoutes"
      border
      stripe
      row-key="routeId"
      :expand-row-keys="expandedKeys"
      @expand-change="onExpand"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="step-panel">
            <div class="step-panel__head">
              <span>工序明细（拖拽行可调整顺序）</span>
              <el-button size="small" type="primary" :disabled="!stepOrderDirty" @click="saveStepOrder(row)">保存排序</el-button>
            </div>
            <el-table :data="row.steps" border stripe size="small" row-key="stepId" class="step-table">
              <el-table-column prop="stepNo" label="序号" width="64" align="center" />
              <el-table-column prop="stepCode" label="编码" width="100" />
              <el-table-column prop="stepName" label="工序名称" min-width="120" />
              <el-table-column prop="standardEquipmentType" label="建议设备" width="110" />
              <el-table-column prop="standardWorkHours" label="标准工时(h)" width="100" align="right" />
              <el-table-column prop="qualityRequiredText" label="质检" width="72" align="center">
                <template #default="{ row: step }">
                  <el-tag size="small" :type="step.qualityRequired === 1 ? 'primary' : 'info'">{{ step.qualityRequiredText }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="排序" width="120">
                <template #default="{ row: step, $index }">
                  <el-button link :disabled="$index === 0" @click="moveStep(row, $index, -1)">上移</el-button>
                  <el-button link :disabled="$index === row.steps.length - 1" @click="moveStep(row, $index, 1)">下移</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="routeCode" label="路线编号" width="130" />
      <el-table-column prop="routeName" label="路线名称" min-width="160" />
      <el-table-column prop="materialName" label="适用产品" min-width="140" />
      <el-table-column prop="materialCode" label="物料编码" width="110" />
      <el-table-column prop="versionNo" label="版本" width="72" />
      <el-table-column prop="statusText" label="状态" width="80" />
    </el-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchProcessSnapshot, postReorderProcessSteps } from '@/api/planner'

const loading = ref(false)
const keyword = ref('')
const routes = ref([])
const expandedKeys = ref([])
const stepOrderDirty = ref(false)
const dirtyRouteId = ref(null)

const filteredRoutes = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return routes.value
  return routes.value.filter((r) =>
    [r.routeName, r.routeCode, r.materialName, r.materialCode].some((v) => String(v || '').toLowerCase().includes(kw))
  )
})

async function loadData() {
  loading.value = true
  try {
    const data = await fetchProcessSnapshot()
    routes.value = (data.routes || []).map((r) => ({ ...r, steps: [...(r.steps || [])] }))
    stepOrderDirty.value = false
  } finally {
    loading.value = false
  }
}

function onExpand(row, expanded) {
  expandedKeys.value = expanded ? [row.routeId] : []
}

function moveStep(route, index, delta) {
  const steps = route.steps
  const target = index + delta
  if (target < 0 || target >= steps.length) return
  const tmp = steps[index]
  steps[index] = steps[target]
  steps[target] = tmp
  steps.forEach((s, i) => { s.stepNo = (i + 1) * 10 })
  stepOrderDirty.value = true
  dirtyRouteId.value = route.routeId
}

async function saveStepOrder(route) {
  try {
    await postReorderProcessSteps({
      routeId: route.routeId,
      stepIds: route.steps.map((s) => s.stepId)
    })
    ElMessage.success('工序顺序已保存')
    stepOrderDirty.value = false
    await loadData()
  } catch {
    ElMessage.error('保存排序失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.process-guide-page { min-height: 100%; }
.step-panel { padding: 8px 12px 12px; background: #fafafa; }
.step-panel__head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 13px; color: #606266; }
</style>
