<template>
  <div class="qc5">
    <div class="qc5__header">
      <div class="qc5__header-main">
        <h3 class="qc5__title">显示器专业五步检测</h3>
        <p class="qc5__desc">按行业终检标准逐台完成：坏点 → 色域 → 漏光 → 亮度均匀性 → 屏闪</p>
      </div>
      <div class="qc5__progress">
        <span>总进度</span>
        <el-progress :percentage="overallPct" :stroke-width="14" :color="progressColor" />
        <em>{{ completedCells }} / {{ totalCells }} 项</em>
      </div>
    </div>

    <!-- 产品选择 -->
    <div class="qc5__units">
      <span class="qc5__units-label">本批次产品（逐台检测）</span>
      <div class="qc5__unit-chips">
        <button
          v-for="(u, idx) in units"
          :key="u.serialNo"
          type="button"
          class="qc5__unit-chip"
          :class="{
            'is-active': currentUnitIndex === idx,
            'is-done': unitOverallStatus(idx) === 'PASS',
            'is-fail': unitOverallStatus(idx) === 'FAIL',
            'is-partial': unitOverallStatus(idx) === 'PARTIAL'
          }"
          @click="currentUnitIndex = idx"
        >
          <span class="qc5__unit-no">#{{ u.unitNo }}</span>
          <span class="qc5__unit-sn">{{ u.serialNo }}</span>
          <el-tag v-if="unitOverallStatus(idx) === 'PASS'" type="success" size="small">合格</el-tag>
          <el-tag v-else-if="unitOverallStatus(idx) === 'FAIL'" type="danger" size="small">不合格</el-tag>
          <el-tag v-else-if="unitOverallStatus(idx) === 'PARTIAL'" type="warning" size="small">检测中</el-tag>
          <el-tag v-else type="info" size="small">待检</el-tag>
        </button>
      </div>
    </div>

    <!-- 五大工序大卡片 -->
    <div class="qc5__stations">
      <div
        v-for="s in stations"
        :key="s.id"
        class="qc5__station"
        :class="{
          'is-active': activeStationId === s.id,
          'is-done': stationDoneCount(s.id) === units.length,
          'is-partial': stationDoneCount(s.id) > 0 && stationDoneCount(s.id) < units.length
        }"
        @click="selectStation(s.id)"
      >
        <div class="qc5__station-img">
          <img :src="s.image" :alt="s.title" />
          <span class="qc5__station-order">{{ s.order }}</span>
        </div>
        <div class="qc5__station-body">
          <h4>{{ s.title }}</h4>
          <p>{{ s.subtitle }}</p>
          <div class="qc5__station-meta">
            <el-tag size="small" :type="stationTagType(s.id)">
              {{ stationDoneCount(s.id) }}/{{ units.length }} 台
            </el-tag>
            <span v-if="currentRecord(s.id)?.saved" class="qc5__cur-result" :class="currentRecord(s.id)?.passed ? 'ok' : 'bad'">
              当前：{{ currentRecord(s.id)?.passed ? '合格' : '不合格' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 当前工序检测区 -->
    <div v-if="activeStation" class="qc5__inspect">
      <div class="qc5__inspect-hd">
        <img :src="activeStation.image" :alt="activeStation.title" class="qc5__inspect-thumb" />
        <div>
          <h4>{{ activeStation.title }} · 第 {{ currentUnit?.unitNo }} 台</h4>
          <p>序列号 {{ currentUnit?.serialNo }} · {{ activeStation.subtitle }}</p>
        </div>
        <el-input
          v-model="currentUnit.serialNo"
          size="default"
          style="width:200px;margin-left:auto"
          placeholder="可修改序列号"
        />
      </div>

      <el-collapse v-model="simExpanded" class="qc5__sim-collapse">
        <el-collapse-item title="展开仿真检测界面（可选）" name="sim">
          <component
            :is="simulatorMap[activeStation.id]"
            v-if="simExpanded.includes('sim')"
            v-model="unitRecords[activeStation.id][currentUnitIndex]"
          />
        </el-collapse-item>
      </el-collapse>

      <div class="qc5__verdict">
        <span class="qc5__verdict-label">本台本工序判定</span>
        <el-radio-group v-model="verdictPassed" size="large">
          <el-radio-button :value="true">
            <el-icon><CircleCheck /></el-icon> 合格
          </el-radio-button>
          <el-radio-button :value="false">
            <el-icon><CircleClose /></el-icon> 不合格
          </el-radio-button>
        </el-radio-group>
        <el-input
          v-model="verdictRemark"
          type="textarea"
          :rows="2"
          placeholder="检测备注（缺陷位置、数值等）"
          style="flex:1;min-width:240px"
        />
      </div>

      <div class="qc5__actions">
        <el-button type="primary" size="large" @click="saveCurrent">保存本台本工序</el-button>
        <el-button v-if="currentUnitIndex < units.length - 1" type="success" size="large" @click="saveAndNextUnit">
          保存并下一台
        </el-button>
        <el-button v-else-if="hasNextStation" type="warning" size="large" @click="saveAndNextStation">
          保存并下一工序
        </el-button>
        <el-button v-else type="success" size="large" plain :disabled="!allDone" @click="$emit('complete')">
          全部完成
        </el-button>
      </div>
    </div>

    <!-- 汇总表 -->
    <el-table v-if="matrixRows.length" :data="matrixRows" border size="small" class="qc5__matrix">
      <el-table-column prop="serialNo" label="序列号" width="140" fixed />
      <el-table-column v-for="s in stations" :key="s.id" :label="s.title" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row[s.id] === true" type="success" size="small">合格</el-tag>
          <el-tag v-else-if="row[s.id] === false" type="danger" size="small">不合格</el-tag>
          <span v-else class="qc5__pending">—</span>
        </template>
      </el-table-column>
      <el-table-column label="综合" width="88" align="center" fixed="right">
        <template #default="{ row }">
          <el-tag :type="row.overall === 'PASS' ? 'success' : row.overall === 'FAIL' ? 'danger' : 'info'" size="small">
            {{ row.overall === 'PASS' ? '合格' : row.overall === 'FAIL' ? '不合格' : '未完成' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { FP_STATIONS, buildUnitList } from '@/config/fpInspectionStations'
import DeadPixelSimulator from '@/components/quality/simulators/DeadPixelSimulator.vue'
import GamutSimulator from '@/components/quality/simulators/GamutSimulator.vue'
import LeakageSimulator from '@/components/quality/simulators/LeakageSimulator.vue'
import UniformitySimulator from '@/components/quality/simulators/UniformitySimulator.vue'
import FlickerSimulator from '@/components/quality/simulators/FlickerSimulator.vue'

const props = defineProps({
  inspection: { type: Object, required: true },
  sampleQuantity: { type: Number, default: 1 }
})

defineEmits(['complete', 'update:matrix'])

const stations = FP_STATIONS
const simulatorMap = {
  deadPixel: DeadPixelSimulator,
  gamut: GamutSimulator,
  leakage: LeakageSimulator,
  uniformity: UniformitySimulator,
  flicker: FlickerSimulator
}

const units = ref([])
const unitRecords = reactive({})
const currentUnitIndex = ref(0)
const activeStationId = ref(stations[0].id)
const simExpanded = ref([])

stations.forEach((s) => { unitRecords[s.id] = [] })

function initUnits() {
  units.value = buildUnitList(props.sampleQuantity, props.inspection?.batchNo)
  stations.forEach((s) => {
    unitRecords[s.id] = units.value.map(() => ({}))
  })
  currentUnitIndex.value = 0
  activeStationId.value = stations[0].id
}

watch(() => [props.sampleQuantity, props.inspection?.inspectionId], initUnits, { immediate: true })

const currentUnit = computed(() => units.value[currentUnitIndex.value])
const activeStation = computed(() => stations.find((s) => s.id === activeStationId.value))

const totalCells = computed(() => units.value.length * stations.length)
const completedCells = computed(() =>
  stations.reduce((sum, s) => sum + unitRecords[s.id].filter((r) => r.saved).length, 0)
)
const overallPct = computed(() =>
  totalCells.value ? Math.round((completedCells.value / totalCells.value) * 100) : 0
)
const progressColor = computed(() => {
  const p = overallPct.value
  if (p >= 100) return '#67c23a'
  if (p >= 50) return '#409eff'
  return '#e6a23c'
})

const allDone = computed(() => completedCells.value === totalCells.value && totalCells.value > 0)

const hasNextStation = computed(() => {
  const idx = stations.findIndex((s) => s.id === activeStationId.value)
  return idx >= 0 && idx < stations.length - 1
})

const verdictPassed = computed({
  get() {
    const rec = currentRecord(activeStationId.value)
    if (!rec || rec.passed === undefined) return true
    return rec.passed === true
  },
  set(v) {
    const rec = ensureRecord(activeStationId.value)
    rec.passed = v === true
  }
})

const verdictRemark = computed({
  get() {
    return currentRecord(activeStationId.value)?.remark || ''
  },
  set(v) {
    const rec = ensureRecord(activeStationId.value)
    rec.remark = v
  }
})

function ensureRecord(stationId) {
  if (!unitRecords[stationId][currentUnitIndex.value]) {
    unitRecords[stationId][currentUnitIndex.value] = {}
  }
  return unitRecords[stationId][currentUnitIndex.value]
}

function currentRecord(stationId) {
  return unitRecords[stationId]?.[currentUnitIndex.value]
}

function stationDoneCount(stationId) {
  return unitRecords[stationId].filter((r) => r.saved).length
}

function stationTagType(stationId) {
  const done = stationDoneCount(stationId)
  const total = units.value.length
  if (done === total) return 'success'
  if (done > 0) return 'warning'
  return 'info'
}

function unitOverallStatus(unitIdx) {
  const recs = stations.map((s) => unitRecords[s.id][unitIdx]).filter((r) => r?.saved)
  if (!recs.length) return 'PENDING'
  if (recs.length < stations.length) return 'PARTIAL'
  return recs.every((r) => r.passed) ? 'PASS' : 'FAIL'
}

function selectStation(id) {
  activeStationId.value = id
}

function saveCurrent() {
  const rec = ensureRecord(activeStationId.value)
  rec.saved = true
  if (rec.passed === undefined) rec.passed = true
  rec.measuredValue = rec.measuredValue || (rec.passed ? '合格' : '不合格')
  units.value[currentUnitIndex.value].status = unitOverallStatus(currentUnitIndex.value) === 'PASS' ? 'PASS'
    : unitOverallStatus(currentUnitIndex.value) === 'FAIL' ? 'FAIL' : 'PARTIAL'
  ElMessage.success('已保存')
}

function saveAndNextUnit() {
  saveCurrent()
  if (currentUnitIndex.value < units.value.length - 1) currentUnitIndex.value++
}

function saveAndNextStation() {
  saveCurrent()
  const idx = stations.findIndex((s) => s.id === activeStationId.value)
  if (idx < stations.length - 1) {
    activeStationId.value = stations[idx + 1].id
    currentUnitIndex.value = unitRecords[activeStationId.value].findIndex((r) => !r.saved)
    if (currentUnitIndex.value < 0) currentUnitIndex.value = 0
  }
}

const matrixRows = computed(() =>
  units.value.map((u, idx) => {
    const row = { serialNo: u.serialNo, unitNo: u.unitNo }
    let allSaved = true
    let anyFail = false
    stations.forEach((s) => {
      const r = unitRecords[s.id][idx]
      if (r?.saved) {
        row[s.id] = r.passed
        if (!r.passed) anyFail = true
      } else {
        row[s.id] = null
        allSaved = false
      }
    })
    row.overall = !allSaved ? 'PENDING' : anyFail ? 'FAIL' : 'PASS'
    return row
  })
)

function getInspectionSummary() {
  const matrix = matrixRows.value
  const passUnits = matrix.filter((r) => r.overall === 'PASS').length
  const failUnits = matrix.filter((r) => r.overall === 'FAIL').length
  const sampleQty = Number(props.sampleQuantity) || units.value.length
  return {
    units: units.value,
    matrix,
    stationRecords: stations.map((s) => ({
      station: s.title,
      stationId: s.id,
      records: unitRecords[s.id].map((r, i) => ({
        serialNo: units.value[i]?.serialNo,
        passed: r.passed !== false,
        saved: r.saved,
        measuredValue: r.measuredValue,
        remark: r.remark
      }))
    })),
    passUnits,
    failUnits,
    sampleQuantity: sampleQty,
    qualifiedQuantity: passUnits,
    unqualifiedQuantity: failUnits
  }
}

defineExpose({ getInspectionSummary, allDone, matrixRows })
</script>

<style scoped>
.qc5 {
  background: #fafbfc;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px 14px;
}

.qc5__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.qc5__title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.qc5__desc {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.qc5__progress {
  min-width: 220px;
  text-align: right;
  font-size: 13px;
  color: #909399;
}

.qc5__progress em {
  font-style: normal;
  font-weight: 600;
  color: #303133;
}

.qc5__units {
  margin-bottom: 12px;
}

.qc5__units-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.qc5__unit-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.qc5__unit-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all .2s;
}

.qc5__unit-chip:hover { border-color: #409eff; }
.qc5__unit-chip.is-active { border-color: #409eff; box-shadow: 0 0 0 2px rgba(64,158,255,.2); }
.qc5__unit-chip.is-done { border-color: #67c23a; background: #f0f9eb; }
.qc5__unit-chip.is-fail { border-color: #f56c6c; background: #fef0f0; }
.qc5__unit-chip.is-partial { border-color: #e6a23c; }

.qc5__unit-no {
  font-weight: 700;
  color: #409eff;
}

.qc5__unit-sn {
  font-size: 12px;
  color: #606266;
}

/* 五大工序 — 专业亮点大卡片 */
.qc5__stations {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

@media (max-width: 1400px) {
  .qc5__stations { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 900px) {
  .qc5__stations { grid-template-columns: repeat(2, 1fr); }
}

.qc5__station {
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
  transition: transform .2s, border-color .2s, box-shadow .2s;
}

.qc5__station:hover {
  transform: translateY(-2px);
  border-color: #409eff;
}

.qc5__station.is-active {
  border-color: #409eff;
  box-shadow: 0 8px 24px rgba(64, 158, 255, .25);
}

.qc5__station.is-done { border-color: #67c23a; }
.qc5__station.is-partial { border-color: #e6a23c; }

.qc5__station-img {
  position: relative;
  height: 200px;
  background: #0d1117;
  overflow: hidden;
}

.qc5__station-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.qc5__station-order {
  position: absolute;
  top: 10px;
  left: 10px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(64, 158, 255, .9);
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qc5__station-body {
  padding: 14px;
}

.qc5__station-body h4 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

.qc5__station-body p {
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
  min-height: 36px;
}

.qc5__station-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.qc5__cur-result { font-size: 12px; font-weight: 600; }
.qc5__cur-result.ok { color: #67c23a; }
.qc5__cur-result.bad { color: #f56c6c; }

.qc5__inspect {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.qc5__inspect-hd {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.qc5__inspect-thumb {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.qc5__inspect-hd h4 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #303133;
}

.qc5__inspect-hd p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.qc5__sim-collapse {
  margin-bottom: 16px;
}

.qc5__verdict {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.qc5__verdict-label {
  font-weight: 600;
  color: #303133;
  padding-top: 8px;
  white-space: nowrap;
}

.qc5__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.qc5__matrix { margin-top: 8px; }
.qc5__pending { color: #c0c4cc; }
</style>
