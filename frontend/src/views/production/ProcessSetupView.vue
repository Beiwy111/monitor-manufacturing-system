<template>
  <div class="process-page">
    <el-tabs v-model="activeTab" class="process-tabs">
      <el-tab-pane label="工序设置" name="step">
        <div class="query-bar">
          <el-form inline>
            <el-form-item label="工序编码">
              <el-input v-model="stepQuery.code" clearable placeholder="请输入工序编码" />
            </el-form-item>
            <el-form-item label="工序名称">
              <el-input v-model="stepQuery.name" clearable placeholder="请输入工序名称" />
            </el-form-item>
            <el-form-item label="所属工艺">
              <el-select v-model="stepQuery.routeId" clearable placeholder="全部" style="width: 190px">
                <el-option v-for="route in routeRows" :key="route.routeId" :label="route.routeName" :value="route.routeId" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadData()">查询</el-button>
              <el-button @click="resetStepQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="table-actions">
          <el-button type="primary" plain @click="openStepDialog()">新增</el-button>
          <el-button type="success" plain :disabled="selectedStepRows.length !== 1" @click="openStepDialog(selectedStepRows[0])">修改</el-button>
          <el-button type="danger" plain :disabled="!selectedStepRows.length" @click="disableSelectedSteps">删除</el-button>
          <el-button type="warning" plain @click="loadData()">刷新</el-button>
        </div>

        <el-table
          v-loading="loading"
          :data="filteredStepRows"
          border
          stripe
          class="full-table"
          @selection-change="selectedStepRows = $event"
        >
          <el-table-column type="selection" width="46" align="center" />
          <el-table-column prop="stepCode" label="工序编码" min-width="140" />
          <el-table-column prop="stepName" label="工序名称" min-width="140" />
          <el-table-column prop="routeName" label="所属工艺流程" min-width="170" />
          <el-table-column prop="standardEquipmentType" label="建议设备" min-width="120" />
          <el-table-column prop="standardWorkHours" label="标准工时" width="100" align="center" />
          <el-table-column prop="qualityRequiredText" label="是否质检" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.qualityRequired === 1 ? 'primary' : 'info'">{{ row.qualityRequiredText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="statusText" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="140">
            <template #default="{ row }">第 {{ row.stepNo }} 道工序</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openStepDialog(row)">修改</el-button>
              <el-button link type="danger" @click="disableStep(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager-line">共 {{ filteredStepRows.length }} 条</div>
      </el-tab-pane>

      <el-tab-pane label="工艺流程" name="route">
        <div class="query-bar">
          <el-form inline>
            <el-form-item label="工艺路线编号">
              <el-input v-model="routeQuery.code" clearable placeholder="请输入工艺路线编号" />
            </el-form-item>
            <el-form-item label="工艺路线名称">
              <el-input v-model="routeQuery.name" clearable placeholder="请输入工艺路线名称" />
            </el-form-item>
            <el-form-item label="适用产品">
              <el-input v-model="routeQuery.material" clearable placeholder="请输入产品名称" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadData()">查询</el-button>
              <el-button @click="resetRouteQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="table-actions">
          <el-button type="primary" plain @click="openRouteDialog()">新增</el-button>
          <el-button type="success" plain :disabled="selectedRouteRows.length !== 1" @click="openRouteDialog(selectedRouteRows[0])">修改</el-button>
          <el-button type="danger" plain :disabled="!selectedRouteRows.length" @click="disableSelectedRoutes">删除</el-button>
          <el-button type="warning" plain @click="loadData()">刷新</el-button>
        </div>

        <el-table
          v-loading="loading"
          :data="filteredRouteRows"
          border
          stripe
          class="full-table"
          @selection-change="selectedRouteRows = $event"
        >
          <el-table-column type="selection" width="46" align="center" />
          <el-table-column prop="routeCode" label="工艺路线编号" min-width="150" />
          <el-table-column prop="routeName" label="工艺路线名称" min-width="160" />
          <el-table-column label="工艺路线流程" min-width="340" align="center">
            <template #default="{ row }">
              <span class="route-process">{{ routeProcessText(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="materialName" label="适用产品" min-width="140" />
          <el-table-column prop="statusText" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="120">
            <template #default="{ row }">{{ row.versionNo }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRouteDialog(row)">修改</el-button>
              <el-button link type="danger" @click="disableRoute(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager-line">共 {{ filteredRouteRows.length }} 条</div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="routeDialogVisible" :title="routeForm.routeId ? '编辑工艺流程' : '新增工艺流程'" width="560px">
      <el-form :model="routeForm" label-width="96px">
        <el-form-item label="适用产品" required>
          <el-select v-model="routeForm.materialId" filterable placeholder="请选择成品物料" style="width:100%">
            <el-option
              v-for="item in materials"
              :key="item.materialId"
              :label="`${item.materialCode} / ${item.materialName}`"
              :value="item.materialId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="路线编码" required>
          <el-input v-model="routeForm.routeCode" placeholder="如 RT-PRD001-V1" />
        </el-form-item>
        <el-form-item label="路线名称" required>
          <el-input v-model="routeForm.routeName" placeholder="如 商用显示器工艺路线" />
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="routeForm.versionNo" placeholder="V1.0" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="routeForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="routeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRoute">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stepDialogVisible" :title="stepForm.stepId ? '编辑工序' : '新增工序'" width="620px">
      <el-form :model="stepForm" label-width="110px">
        <el-form-item label="所属工艺流程" required>
          <el-select v-model="stepForm.routeId" filterable placeholder="请选择工艺流程" style="width:100%">
            <el-option v-for="route in routeRows" :key="route.routeId" :label="route.routeName" :value="route.routeId" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="工序序号" required>
              <el-input-number v-model="stepForm.stepNo" :min="1" :step="10" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工序编码" required>
              <el-input v-model="stepForm.stepCode" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="工序名称" required>
          <el-input v-model="stepForm.stepName" placeholder="显示屏加工 / 主板装配 / 面板贴附 / 整机组装" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="建议设备">
              <el-select v-model="stepForm.standardEquipmentType" allow-create filterable default-first-option style="width:100%">
                <el-option v-for="item in equipmentTypes" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标准工时">
              <el-input-number v-model="stepForm.standardWorkHours" :min="0" :precision="2" :step="0.5" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="质检确认">
              <el-switch v-model="stepForm.qualityRequired" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用状态">
              <el-switch v-model="stepForm.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="stepDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitStep">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  disableProcessRoute,
  disableProcessStep,
  fetchProcessSnapshot,
  saveProcessRoute,
  saveProcessStep
} from '@/api/process'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const routeDialogVisible = ref(false)
const stepDialogVisible = ref(false)
const activeTab = ref('step')
const routeRows = ref([])
const allSteps = ref([])
const materials = ref([])
const selectedStepRows = ref([])
const selectedRouteRows = ref([])
const stepQuery = reactive({ code: '', name: '', routeId: '' })
const routeQuery = reactive({ code: '', name: '', material: '' })

const equipmentTypes = ['显示屏线', '主板线', '贴附机', '组装线']
const routeForm = reactive(defaultRouteForm())
const stepForm = reactive(defaultStepForm())

const stepRows = computed(() => allSteps.value.map((step) => {
  const route = routeRows.value.find((r) => r.routeId === step.routeId)
  return {
    ...step,
    routeCode: route?.routeCode || '',
    routeName: route?.routeName || ''
  }
}))

const filteredStepRows = computed(() => stepRows.value.filter((row) => {
  const matchCode = !stepQuery.code || row.stepCode?.includes(stepQuery.code)
  const matchName = !stepQuery.name || row.stepName?.includes(stepQuery.name)
  const matchRoute = !stepQuery.routeId || row.routeId === stepQuery.routeId
  return matchCode && matchName && matchRoute
}))

const filteredRouteRows = computed(() => routeRows.value.filter((row) => {
  const matchCode = !routeQuery.code || row.routeCode?.includes(routeQuery.code)
  const matchName = !routeQuery.name || row.routeName?.includes(routeQuery.name)
  const matchMaterial = !routeQuery.material || row.materialName?.includes(routeQuery.material)
  return matchCode && matchName && matchMaterial
}))

const selectedRouteForNewStep = computed(() => {
  if (stepQuery.routeId) return routeRows.value.find((r) => r.routeId === stepQuery.routeId)
  if (selectedRouteRows.value.length === 1) return selectedRouteRows.value[0]
  return routeRows.value[0] || null
})

function defaultRouteForm() {
  return { routeId: null, materialId: null, routeCode: '', routeName: '', versionNo: 'V1.0', status: 1 }
}

function defaultStepForm() {
  return {
    stepId: null,
    routeId: null,
    stepNo: 10,
    stepCode: 'STEP-01',
    stepName: '',
    standardWorkHours: 0,
    standardEquipmentType: '',
    qualityRequired: 0,
    status: 1
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await fetchProcessSnapshot()
    routeRows.value = data.routes || []
    allSteps.value = data.steps || []
    materials.value = data.materials || []
  } finally {
    loading.value = false
  }
}

function openRouteDialog(row) {
  Object.assign(routeForm, defaultRouteForm(), row || {})
  routeDialogVisible.value = true
}

function openStepDialog(row) {
  const route = selectedRouteForNewStep.value
  if (!row && !route) {
    ElMessage.warning('请先新增或选择一个工艺流程')
    return
  }
  const routeId = row?.routeId || route.routeId
  const sameRouteSteps = allSteps.value.filter((s) => s.routeId === routeId)
  const nextNo = Math.max(0, ...sameRouteSteps.map((s) => Number(s.stepNo || 0))) + 10
  Object.assign(stepForm, defaultStepForm(), {
    routeId,
    stepNo: nextNo,
    stepCode: `STEP-${String(sameRouteSteps.length + 1).padStart(2, '0')}`
  }, row || {})
  stepDialogVisible.value = true
}

async function submitRoute() {
  if (!routeForm.materialId || !routeForm.routeCode || !routeForm.routeName) {
    ElMessage.warning('请补齐产品、路线编码和路线名称')
    return
  }
  saving.value = true
  try {
    const saved = await saveProcessRoute(routeForm, userStore.displayName)
    ElMessage.success('工艺流程已保存')
    routeDialogVisible.value = false
    await loadData()
    stepQuery.routeId = saved.routeId || stepQuery.routeId
  } finally {
    saving.value = false
  }
}

async function submitStep() {
  if (!stepForm.routeId || !stepForm.stepNo || !stepForm.stepCode || !stepForm.stepName) {
    ElMessage.warning('请补齐工序序号、编码和名称')
    return
  }
  saving.value = true
  try {
    await saveProcessStep(stepForm)
    ElMessage.success('工序已保存')
    stepDialogVisible.value = false
    stepQuery.routeId = stepForm.routeId
    await loadData()
  } finally {
    saving.value = false
  }
}

async function disableRoute(row) {
  await ElMessageBox.confirm(`确定停用工艺流程 ${row.routeName}？相关工序也会停用。`, '停用确认', { type: 'warning' })
  await disableProcessRoute(row.routeId)
  ElMessage.success('工艺流程已停用')
  await loadData()
}

async function disableStep(row) {
  await ElMessageBox.confirm(`确定停用工序 ${row.stepName}？`, '停用确认', { type: 'warning' })
  await disableProcessStep(row.stepId)
  ElMessage.success('工序已停用')
  await loadData()
}

async function disableSelectedRoutes() {
  for (const row of selectedRouteRows.value) {
    await disableProcessRoute(row.routeId)
  }
  ElMessage.success('已停用选中的工艺流程')
  selectedRouteRows.value = []
  await loadData()
}

async function disableSelectedSteps() {
  for (const row of selectedStepRows.value) {
    await disableProcessStep(row.stepId)
  }
  ElMessage.success('已停用选中的工序')
  selectedStepRows.value = []
  await loadData()
}

function resetStepQuery() {
  Object.assign(stepQuery, { code: '', name: '', routeId: '' })
}

function resetRouteQuery() {
  Object.assign(routeQuery, { code: '', name: '', material: '' })
}

function routeProcessText(row) {
  const steps = (row.steps || [])
    .filter((step) => step.status === 1)
    .sort((a, b) => Number(a.stepNo || 0) - Number(b.stepNo || 0))
    .map((step) => step.stepName)
  return steps.length ? steps.join(' --> ') : '暂无工序'
}

onMounted(loadData)
</script>

<style scoped>
.process-page {
  min-height: calc(100vh - 84px);
  padding: 12px 16px 18px;
  background: #fff;
}
.process-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}
.query-bar {
  padding: 4px 0 2px;
  border-bottom: 1px solid #eef0f3;
}
.query-bar :deep(.el-form-item) {
  margin-bottom: 10px;
}
.table-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
}
.full-table {
  width: 100%;
}
.route-process {
  color: #3b4658;
  line-height: 1.6;
}
.pager-line {
  display: flex;
  justify-content: flex-end;
  padding: 12px 4px 0;
  color: #6b778c;
  font-size: 13px;
}
</style>
