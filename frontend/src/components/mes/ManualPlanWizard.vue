<template>
  <el-dialog v-model="visible" title="手动排产" width="960px" destroy-on-close class="manual-wizard" @closed="reset">
    <el-steps :active="step" finish-status="success" align-center class="wizard-steps">
      <el-step title="订单与数量" />
      <el-step title="工艺分配" />
      <el-step title="甘特预览" />
      <el-step title="保存" />
    </el-steps>

    <div v-show="step === 0" class="wizard-panel">
      <el-form label-width="96px">
        <el-form-item label="待排订单" required>
          <el-select v-model="form.orderId" style="width:100%" @change="loadWizard">
            <el-option v-for="o in pendingOrders" :key="o.id" :label="`${o.id} · ${o.productModel}`" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划数量" required>
          <el-input-number v-model="form.plannedQty" :min="1" :max="99999" @change="loadWizard" />
        </el-form-item>
        <el-form-item label="计划周期">
          <el-date-picker v-model="form.planStart" type="date" value-format="YYYY-MM-DD" />
          <span class="range-sep">至</span>
          <el-date-picker v-model="form.planEnd" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <el-alert v-if="estimatedDays" :title="`按标准工时预计工期约 ${estimatedDays} 天`" type="info" :closable="false" />
    </div>

    <div v-show="step === 1" class="wizard-panel">
      <el-table :data="schedules" border stripe size="small">
        <el-table-column prop="stepName" label="工序" width="120" />
        <el-table-column label="车间" min-width="130">
          <template #default="{ row }">
            <el-select v-model="row.workshop" size="small" @change="onWorkshopChange(row)">
              <el-option v-for="w in workshops" :key="w.key" :label="w.name" :value="w.name" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="设备" min-width="130">
          <template #default="{ row }">
            <el-select v-model="row.equipmentCode" size="small" @change="onEquipmentChange(row)">
              <el-option v-for="e in equipmentForWorkshop(row.workshop)" :key="e.equipmentCode" :label="e.equipmentName" :value="e.equipmentCode" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.plannedQuantity" size="small" :min="1" :max="form.plannedQty" />
          </template>
        </el-table-column>
        <el-table-column label="开始" width="170">
          <template #default="{ row }">
            <el-date-picker v-model="row.plannedStart" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="结束" width="170">
          <template #default="{ row }">
            <el-date-picker v-model="row.plannedEnd" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" size="small" />
          </template>
        </el-table-column>
      </el-table>
      <p class="hint">可将同一工序拆分到不同车间/设备，数量之和建议等于计划数量。</p>
    </div>

    <div v-show="step === 2" class="wizard-panel">
      <div class="gantt">
        <div v-for="row in schedules" :key="row.stepName + row.workshop" class="gantt-row">
          <div class="gantt-row__label">{{ row.stepName }} · {{ row.workshop }}</div>
          <div class="gantt-row__bar-wrap">
            <div class="gantt-row__bar" :style="ganttStyle(row)">{{ row.plannedQuantity }}台</div>
          </div>
        </div>
      </div>
      <el-table v-if="conflicts.length" :data="conflicts" border stripe size="small" class="conflict-table">
        <el-table-column label="级别" width="72">
          <template #default="{ row }">
            <el-tag :type="row.level === 'danger' ? 'danger' : 'warning'" size="small">{{ row.level === 'danger' ? '严重' : '警告' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="label" label="类型" width="100" />
        <el-table-column prop="detail" label="说明" min-width="200" />
      </el-table>
    </div>

    <div v-show="step === 3" class="wizard-panel">
      <el-form label-width="96px">
        <el-form-item label="保存方式">
          <el-radio-group v-model="form.saveAction">
            <el-radio value="draft">保存草稿</el-radio>
            <el-radio value="publish">待提交</el-radio>
            <el-radio value="submit">提交主管</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="form.adjustReason" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <el-alert v-if="hasDanger" type="error" :closable="false" title="存在严重冲突，无法提交主管，请返回修改或保存草稿" />
    </div>

    <template #footer>
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <el-button v-if="step < 3" type="primary" @click="nextStep">下一步</el-button>
      <el-button v-if="step === 3" type="primary" :loading="saving" :disabled="form.saveAction === 'submit' && hasDanger" @click="save">确认保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { postLoadManualPlanWizard, postSaveProductionPlan, postValidateProductionPlan } from '@/api/planner'

const props = defineProps({ modelValue: Boolean, defaultOrderId: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue', 'success'])

const mes = useMesStore()
const userStore = useUserStore()
const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })

const step = ref(0)
const saving = ref(false)
const workshops = ref([])
const equipment = ref([])
const schedules = ref([])
const conflicts = ref([])
const estimatedDays = ref(0)
const hasDanger = ref(false)

const today = new Date().toISOString().slice(0, 10)
const defaultEnd = new Date(Date.now() + 21 * 86400000).toISOString().slice(0, 10)
const form = reactive({ orderId: '', plannedQty: 1, planStart: today, planEnd: defaultEnd, saveAction: 'draft', adjustReason: '' })

const pendingOrders = computed(() => mes.pendingPlanOrders)

watch(visible, (v) => {
  if (v) {
    form.orderId = props.defaultOrderId || pendingOrders.value[0]?.id || ''
    if (form.orderId) loadWizard()
  }
})

function equipmentForWorkshop(workshop) {
  return equipment.value.filter((e) => !workshop || e.workshop === workshop)
}

function onWorkshopChange(row) {
  const list = equipmentForWorkshop(row.workshop)
  if (list.length) {
    row.equipmentCode = list[0].equipmentCode
    row.equipmentId = list[0].equipmentId
  }
}

function onEquipmentChange(row) {
  const eq = equipment.value.find((e) => e.equipmentCode === row.equipmentCode)
  row.equipmentId = eq?.equipmentId || null
}

async function loadWizard() {
  if (!form.orderId) return
  try {
    const data = await postLoadManualPlanWizard({ orderId: form.orderId, plannedQty: form.plannedQty, operator: userStore.username })
    schedules.value = (data.schedules || []).map((r) => ({ ...r }))
    workshops.value = data.workshops || []
    equipment.value = data.equipment || []
    estimatedDays.value = data.estimatedDays || 0
    if (data.plannedQty) form.plannedQty = data.plannedQty
    if (estimatedDays.value && form.planStart) {
      const d = new Date(form.planStart)
      d.setDate(d.getDate() + estimatedDays.value)
      form.planEnd = d.toISOString().slice(0, 10)
    }
  } catch {
    ElMessage.error('加载工艺路线失败')
  }
}

async function validate() {
  const res = await postValidateProductionPlan({
    orderId: form.orderId,
    plannedQty: form.plannedQty,
    planStart: form.planStart,
    planEnd: form.planEnd,
    schedules: schedules.value,
    operator: userStore.username
  })
  conflicts.value = res.conflicts || []
  hasDanger.value = !!res.hasDanger
}

async function nextStep() {
  if (step.value === 1) await loadWizard()
  if (step.value === 2) await validate()
  if (step.value === 0 && !form.orderId) {
    ElMessage.warning('请选择订单')
    return
  }
  step.value++
  if (step.value === 2) await validate()
}

function ganttStyle(row) {
  const start = new Date(String(row.plannedStart).replace(/-/g, '/')).getTime()
  const end = new Date(String(row.plannedEnd).replace(/-/g, '/')).getTime()
  const min = new Date(form.planStart).getTime()
  const max = new Date(form.planEnd).getTime() + 86400000
  const total = Math.max(max - min, 1)
  const left = Math.max(0, ((start - min) / total) * 100)
  const width = Math.max(8, ((end - start) / total) * 100)
  return { marginLeft: `${left}%`, width: `${Math.min(width, 100 - left)}%` }
}

async function save() {
  if (form.saveAction === 'submit') await validate()
  if (form.saveAction === 'submit' && hasDanger.value) return
  saving.value = true
  try {
    await postSaveProductionPlan({
      orderId: form.orderId,
      plannedQty: form.plannedQty,
      planStart: form.planStart,
      planEnd: form.planEnd,
      schedules: schedules.value,
      saveAction: form.saveAction,
      adjustReason: form.adjustReason,
      schedulingMode: 'MANUAL',
      operator: userStore.username
    })
    ElMessage.success('计划已保存')
    visible.value = false
    emit('success')
    await mes.hydrateFromApi()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function reset() {
  step.value = 0
  conflicts.value = []
  hasDanger.value = false
}
</script>

<style scoped>
.wizard-steps { margin-bottom: 16px; }
.wizard-panel { min-height: 280px; }
.range-sep { margin: 0 8px; color: var(--el-text-color-secondary); }
.hint { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 8px; }
.gantt-row { display: flex; align-items: center; margin-bottom: 10px; }
.gantt-row__label { width: 180px; font-size: 12px; flex-shrink: 0; }
.gantt-row__bar-wrap { flex: 1; height: 24px; background: var(--el-fill-color-light); border-radius: 4px; overflow: hidden; }
.gantt-row__bar { height: 100%; background: var(--el-color-primary-light-5); border: 1px solid var(--el-color-primary); font-size: 11px; line-height: 22px; text-align: center; min-width: 40px; }
.conflict-table { margin-top: 12px; }
</style>
