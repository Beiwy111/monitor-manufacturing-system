<template>
  <el-dialog
    :model-value="visible"
    title="上报安灯报警"
    width="480px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <!-- 设备选择 -->
      <el-form-item label="设备" prop="equipmentId">
        <el-select
          v-model="form.equipmentId"
          filterable
          placeholder="搜索设备编码或名称"
          style="width:100%"
          :loading="eqLoading"
        >
          <el-option
            v-for="eq in equipmentList"
            :key="eq.equipmentId"
            :label="`${eq.equipmentCode} · ${eq.equipmentName}`"
            :value="eq.equipmentId"
          >
            <span style="float:left">{{ eq.equipmentCode }}</span>
            <span style="float:right;color:#909399;font-size:12px">{{ eq.equipmentName }}</span>
          </el-option>
        </el-select>
      </el-form-item>

      <!-- 报警类型 -->
      <el-form-item label="报警类型" prop="alarmType">
        <el-select v-model="form.alarmType" style="width:100%">
          <el-option label="设备异常" value="EQUIPMENT" />
          <el-option label="质量异常" value="QUALITY" />
          <el-option label="物料异常" value="MATERIAL" />
          <el-option label="工艺异常" value="PROCESS" />
          <el-option label="安全隐患" value="SAFETY" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </el-form-item>

      <!-- 紧急程度 -->
      <el-form-item label="紧急程度" prop="alarmLevel">
        <div class="level-group">
          <div
            v-for="lv in LEVELS"
            :key="lv.value"
            class="level-card"
            :class="{ selected: form.alarmLevel === lv.value, [`lv-${lv.value.toLowerCase()}`]: true }"
            @click="form.alarmLevel = lv.value"
          >
            <span class="level-card__icon">{{ lv.icon }}</span>
            <span class="level-card__label">{{ lv.label }}</span>
            <span class="level-card__desc">{{ lv.desc }}</span>
          </div>
        </div>
      </el-form-item>

      <!-- 故障描述 -->
      <el-form-item label="故障描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          :maxlength="300"
          show-word-limit
          placeholder="请详细描述异常现象，如：传感器读数异常、设备异响、产品质量偏差等（最少10字）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button
        type="danger"
        :loading="submitting"
        @click="submit"
      >
        确认上报
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { fetchEquipmentViews, triggerAlarm } from '@/api/business'

const props = defineProps({
  visible:    { type: Boolean, default: false },
  // 预填设备（来自操作员当前工位派工），可不传
  presetEquipmentId: { type: Number, default: null },
})
const emit = defineEmits(['update:visible', 'reported'])

const userStore   = useUserStore()
const formRef     = ref(null)
const submitting  = ref(false)
const eqLoading   = ref(false)
const equipmentList = ref([])

const LEVELS = [
  { value: 'GENERAL',   icon: '🔵', label: '一般',   desc: '不影响产线，可择期处理' },
  { value: 'IMPORTANT', icon: '🟠', label: '重要',   desc: '产线受影响，4小时内处理' },
  { value: 'URGENT',    icon: '🔴', label: '紧急',   desc: '产线停产，需立即处理' },
]

const form = reactive({
  equipmentId:  null,
  alarmType:    'EQUIPMENT',
  alarmLevel:   'GENERAL',
  description:  '',
})

const rules = {
  equipmentId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  alarmType:   [{ required: true, message: '请选择报警类型', trigger: 'change' }],
  alarmLevel:  [{ required: true, message: '请选择紧急程度', trigger: 'change' }],
  description: [
    { required: true, message: '请输入故障描述', trigger: 'blur' },
    { min: 10, message: '描述不少于10字', trigger: 'blur' },
  ],
}

onMounted(async () => {
  eqLoading.value = true
  try {
    const res = await fetchEquipmentViews()
    equipmentList.value = Array.isArray(res) ? res : (res.data ?? [])
  } catch { /* 静默 */ } finally {
    eqLoading.value = false
  }
  if (props.presetEquipmentId) form.equipmentId = props.presetEquipmentId
})

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await triggerAlarm({
      equipmentId: form.equipmentId,
      alarmType:   form.alarmType,
      alarmLevel:  form.alarmLevel,
      description: form.description,
      operator:    userStore.userInfo?.username || '',
    })

    ElMessage.success('安灯报警已上报，设备已转为故障状态')
    emit('update:visible', false)
    emit('reported')
  } catch (e) {
    ElMessage.error(e?.message || '上报失败，请重试')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.equipmentId = props.presetEquipmentId ?? null
  form.alarmType   = 'EQUIPMENT'
  form.alarmLevel  = 'GENERAL'
  form.description = ''
  formRef.value?.clearValidate()
}
</script>

<style scoped>
/* 紧急程度卡片选择器 */
.level-group {
  display: flex;
  gap: 10px;
  width: 100%;
}
.level-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 6px;
  border-radius: 10px;
  border: 2px solid #e5e7eb;
  cursor: pointer;
  transition: border-color .15s, background .15s, transform .12s;
  background: #f9fafb;
  user-select: none;
}
.level-card:hover { transform: translateY(-2px); }
.level-card__icon  { font-size: 22px; line-height: 1; }
.level-card__label { font-size: 13px; font-weight: 700; color: #1a2a40; }
.level-card__desc  { font-size: 10px; color: #9aa5b4; text-align: center; line-height: 1.3; }

/* 选中状态 */
.level-card.lv-general.selected   { border-color: #3b82f6; background: #eff6ff; }
.level-card.lv-important.selected { border-color: #f59e0b; background: #fffbeb; }
.level-card.lv-urgent.selected    { border-color: #ef4444; background: #fef2f2; }
.level-card.lv-general.selected   .level-card__label { color: #1d4ed8; }
.level-card.lv-important.selected .level-card__label { color: #b45309; }
.level-card.lv-urgent.selected    .level-card__label { color: #b91c1c; }
</style>
