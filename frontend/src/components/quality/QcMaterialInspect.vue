<template>
  <div class="qcm">
    <div class="qcm__header">
      <h3>物料来料检验</h3>
      <p>对来料批次抽样，逐项勾选合格/不合格并填写实测值</p>
    </div>

    <el-form label-width="90px" size="default" class="qcm__sampling">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-form-item label="到货数量">
            <el-input-number v-model="lotQuantity" :min="1" :max="99999" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="抽检数量">
            <el-input-number v-model="sampleQty" :min="1" :max="lotQuantity" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="合格数量">
            <el-input-number v-model="qualifiedQty" :min="0" :max="sampleQty" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="不良数量">
            <el-input-number v-model="unqualifiedQty" :min="0" :max="sampleQty" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="qcm__toolbar">
      <el-button type="primary" plain :loading="itemsLoading" @click="generateItems">生成默认检测项</el-button>
      <span v-if="sampleQty" class="qcm__yield">合格率 {{ yieldPct }}%</span>
    </div>

    <el-table v-if="items.length" :data="items" border stripe>
      <el-table-column prop="itemCode" label="编号" width="90" />
      <el-table-column prop="itemName" label="检测项" min-width="120" />
      <el-table-column prop="standardValue" label="标准值" width="140" show-overflow-tooltip />
      <el-table-column label="实测值" width="120">
        <template #default="{ row }">
          <el-input v-model="row.measuredValue" size="small" placeholder="实测" />
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="60" align="center" />
      <el-table-column label="是否合格" width="160" align="center">
        <template #default="{ row }">
          <el-radio-group v-model="row.result" size="small">
            <el-radio-button value="PASSED">合格</el-radio-button>
            <el-radio-button value="FAILED">不合格</el-radio-button>
          </el-radio-group>
        </template>
      </el-table-column>
      <el-table-column label="缺陷等级" width="100">
        <template #default="{ row }">
          <el-select v-if="row.result === 'FAILED'" v-model="row.defectLevel" size="small" style="width:88px">
            <el-option label="轻微" value="MINOR" />
            <el-option label="严重" value="MAJOR" />
            <el-option label="致命" value="CRITICAL" />
          </el-select>
          <span v-else class="qcm__dash">—</span>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="点击「生成默认检测项」开始物料检验" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { generateDefaultItems } from '@/api/quality'

const props = defineProps({
  inspection: { type: Object, required: true },
  items: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:items', 'update:sampling'])

const itemsLoading = ref(false)
const localItems = ref([...props.items])
const lotQuantity = ref(100)
const sampleQty = ref(10)
const qualifiedQty = ref(0)
const unqualifiedQty = ref(0)

watch(() => props.items, (v) => { localItems.value = [...(v || [])] }, { deep: true })
watch(localItems, (v) => emit('update:items', v), { deep: true })

watch(() => props.inspection, (row) => {
  if (!row) return
  const lotMatch = String(row.remark || '').match(/来料批次\s+(\d+)/)
  lotQuantity.value = lotMatch ? Number(lotMatch[1]) : Math.max(Number(row.sampleQuantity) || 1, 50)
  sampleQty.value = Number(row.sampleQuantity) || 10
  qualifiedQty.value = Number(row.qualifiedQuantity) || 0
  unqualifiedQty.value = Number(row.unqualifiedQuantity) || 0
}, { immediate: true })

watch([sampleQty, qualifiedQty, unqualifiedQty, lotQuantity], () => {
  emit('update:sampling', {
    lotQuantity: lotQuantity.value,
    sampleQuantity: sampleQty.value,
    qualifiedQuantity: qualifiedQty.value,
    unqualifiedQuantity: unqualifiedQty.value
  })
}, { immediate: true })

const items = computed(() => localItems.value)

const yieldPct = computed(() => {
  const s = sampleQty.value
  const q = qualifiedQty.value
  if (!s) return '0.0'
  return Math.min(100, Math.max(0, Math.round((q / s) * 1000) / 10)).toFixed(1)
})

async function generateItems() {
  if (!props.inspection?.inspectionId) return
  itemsLoading.value = true
  try {
    const res = await generateDefaultItems(props.inspection.inspectionId)
    const generated = res.data ?? res
    localItems.value = (generated || []).map((i) => ({
      ...i,
      result: i.result || 'PENDING',
      resultCn: resultCn(i.result)
    }))
    ElMessage.success('已生成默认检测项')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '生成失败')
  } finally {
    itemsLoading.value = false
  }
}

function resultCn(s) {
  return { PASSED: '合格', FAILED: '不合格', WARNING: '警告', PENDING: '待检' }[s] || '待检'
}

function getInspectionSummary() {
  const passed = localItems.value.filter((i) => i.result === 'PASSED').length
  const failed = localItems.value.filter((i) => i.result === 'FAILED').length
  return {
    sampleQuantity: sampleQty.value,
    qualifiedQuantity: qualifiedQty.value,
    unqualifiedQuantity: unqualifiedQty.value,
    items: localItems.value,
    passItems: passed,
    failItems: failed
  }
}

defineExpose({ getInspectionSummary, generateItems })
</script>

<style scoped>
.qcm {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
}

.qcm__header h3 {
  margin: 0 0 6px;
  font-size: 18px;
  color: #303133;
}

.qcm__header p {
  margin: 0 0 16px;
  font-size: 13px;
  color: #909399;
}

.qcm__toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.qcm__yield {
  font-size: 13px;
  color: #67c23a;
  font-weight: 600;
}

.qcm__dash { color: #c0c4cc; }
</style>
