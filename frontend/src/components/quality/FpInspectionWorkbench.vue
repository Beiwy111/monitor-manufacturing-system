<template>
  <div class="fpwb">
    <div class="fpwb__head">
      <span class="fpwb__title">五步仿真检测</span>
      <span class="fpwb__sub">按顺序完成：坏点 → 色域 → 漏光 → 均匀性 → 屏闪</span>
      <el-button v-if="allStationsDone" size="small" type="primary" :loading="syncing" @click="syncToItems">同步到检测项</el-button>
    </div>

    <el-steps :active="activeStep" finish-status="success" align-center class="fpwb__steps" simple>
      <el-step v-for="s in stations" :key="s.id" :title="s.title" />
    </el-steps>

    <div class="fpwb__hub">
      <div
        v-for="s in stations"
        :key="s.id"
        class="fpwb__card"
        :class="{
          'is-done': stationStatus[s.id] === 'done',
          'is-active': stationStatus[s.id] === 'active',
          'is-locked': !canOpenStation(s)
        }"
        @click="openStation(s)"
      >
        <div class="fpwb__img-wrap">
          <img :src="s.image" :alt="s.title" class="fpwb__img" />
        </div>
        <div class="fpwb__label">{{ s.title }}</div>
        <div class="fpwb__status">
          <el-tag v-if="stationStatus[s.id] === 'done'" type="success" size="small">已完成</el-tag>
          <el-tag v-else-if="stationStatus[s.id] === 'active'" type="primary" size="small">进行中</el-tag>
          <el-tag v-else-if="!canOpenStation(s)" type="info" size="small">待解锁</el-tag>
          <el-tag v-else size="small">待检测</el-tag>
        </div>
      </div>
    </div>

    <el-table v-if="summaryRows.length" :data="summaryRows" border size="small" style="width:100%;margin-top:12px">
      <el-table-column prop="station" label="工位" width="120" />
      <el-table-column prop="done" label="已检台数" width="88" align="center" />
      <el-table-column prop="pass" label="合格" width="72" align="center" />
      <el-table-column prop="fail" label="不合格" width="72" align="center" />
      <el-table-column prop="lastMeasured" label="最近实测" min-width="160" show-overflow-tooltip />
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="`${currentStation?.title} · ${currentUnit?.serialNo || ''}`"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
      class="fpwb-dialog"
      @closed="onDialogClosed"
    >
      <template v-if="currentStation && currentUnit">
        <div class="fpwb-dialog__meta">
          <span>第 {{ currentUnit.unitNo }} / {{ units.length }} 台</span>
          <span>序列号 {{ currentUnit.serialNo }}</span>
          <el-input v-model="currentUnit.serialNo" size="small" style="width:160px" placeholder="可修改序列号" />
        </div>

        <component
          :is="simulatorMap[currentStation.id]"
          v-model="unitRecords[currentStation.id][currentUnitIndex]"
        />

        <div class="fpwb-dialog__actions">
          <el-button size="small" @click="dialogVisible = false">暂存退出</el-button>
          <el-button size="small" type="primary" @click="saveCurrentUnit">保存本台</el-button>
          <el-button v-if="currentUnitIndex < units.length - 1" size="small" type="success" @click="saveAndNext">保存并下一台</el-button>
          <el-button v-else size="small" type="warning" @click="finishStation">完成本工位</el-button>
        </div>

        <el-table :data="units" border size="small" style="width:100%;margin-top:12px" max-height="160">
          <el-table-column prop="unitNo" label="#" width="48" />
          <el-table-column prop="serialNo" label="序列号" width="140" />
          <el-table-column label="状态" width="80">
            <template #default="{ row, $index }">
              <el-tag :type="unitTag(row, $index)" size="small">{{ unitLabel(row, $index) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="实测" min-width="120">
            <template #default="{ $index }">
              {{ unitRecords[currentStation.id][$index]?.measuredValue || '—' }}
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { FP_STATIONS, buildUnitList } from '@/config/fpInspectionStations'
import DeadPixelSimulator from '@/components/quality/simulators/DeadPixelSimulator.vue'
import GamutSimulator from '@/components/quality/simulators/GamutSimulator.vue'
import LeakageSimulator from '@/components/quality/simulators/LeakageSimulator.vue'
import UniformitySimulator from '@/components/quality/simulators/UniformitySimulator.vue'
import FlickerSimulator from '@/components/quality/simulators/FlickerSimulator.vue'
import { saveInspectionItems } from '@/api/quality'

const props = defineProps({
  inspection: { type: Object, required: true },
  items: { type: Array, default: () => [] },
  sampleQuantity: { type: Number, default: 1 }
})
const emit = defineEmits(['synced', 'update:items'])

const stations = FP_STATIONS
const simulatorMap = {
  deadPixel: DeadPixelSimulator,
  gamut: GamutSimulator,
  leakage: LeakageSimulator,
  uniformity: UniformitySimulator,
  flicker: FlickerSimulator
}

const dialogVisible = ref(false)
const currentStation = ref(null)
const currentUnitIndex = ref(0)
const syncing = ref(false)

const units = ref([])
const stationStatus = reactive({})
const unitRecords = reactive({})

stations.forEach((s, i) => {
  stationStatus[s.id] = i === 0 ? 'active' : 'locked'
  unitRecords[s.id] = []
})

function initUnits() {
  units.value = buildUnitList(props.sampleQuantity, props.inspection?.batchNo)
  stations.forEach((s) => {
    unitRecords[s.id] = units.value.map(() => ({}))
  })
}

watch(() => [props.sampleQuantity, props.inspection?.inspectionId], initUnits, { immediate: true })

const currentUnit = computed(() => units.value[currentUnitIndex.value])

const activeStep = computed(() => {
  const done = stations.filter((s) => stationStatus[s.id] === 'done').length
  return Math.min(done, stations.length - 1)
})

const allStationsDone = computed(() => stations.every((s) => stationStatus[s.id] === 'done'))

function canOpenStation(s) {
  const idx = stations.findIndex((x) => x.id === s.id)
  if (idx === 0) return true
  return stationStatus[stations[idx - 1].id] === 'done' || stationStatus[s.id] !== 'locked'
}

function openStation(s) {
  if (!canOpenStation(s) && stationStatus[s.id] === 'locked') {
    ElMessage.warning('请先完成上一工位检测')
    return
  }
  currentStation.value = s
  stationStatus[s.id] = stationStatus[s.id] === 'done' ? 'done' : 'active'
  const firstPending = unitRecords[s.id].findIndex((r) => !r.saved)
  currentUnitIndex.value = firstPending >= 0 ? firstPending : 0
  dialogVisible.value = true
}

function saveCurrentUnit() {
  const sid = currentStation.value.id
  const rec = unitRecords[sid][currentUnitIndex.value]
  rec.saved = true
  rec.passed = rec.passed !== false
  units.value[currentUnitIndex.value].status = rec.passed ? 'PASS' : 'FAIL'
  ElMessage.success('已保存')
}

function saveAndNext() {
  saveCurrentUnit()
  if (currentUnitIndex.value < units.value.length - 1) {
    currentUnitIndex.value++
  }
}

function finishStation() {
  saveCurrentUnit()
  const sid = currentStation.value.id
  const pending = unitRecords[sid].some((r) => !r.saved)
  if (pending) {
    ElMessage.warning('仍有未保存的显示器，请逐台录入')
    return
  }
  stationStatus[sid] = 'done'
  const next = stations.find((s) => stationStatus[s.id] !== 'done')
  if (next) stationStatus[next.id] = 'active'
  dialogVisible.value = false
  ElMessage.success(`${currentStation.value.title} 工位已完成`)
}

function onDialogClosed() {
  currentStation.value = null
}

function unitTag(row, idx) {
  const sid = currentStation.value?.id
  if (!sid) return 'info'
  const r = unitRecords[sid][idx]
  if (!r?.saved) return 'info'
  return r.passed ? 'success' : 'danger'
}

function unitLabel(row, idx) {
  const sid = currentStation.value?.id
  if (!sid) return '待检'
  const r = unitRecords[sid][idx]
  if (!r?.saved) return '待检'
  return r.passed ? '合格' : '不合格'
}

const summaryRows = computed(() =>
  stations.map((s) => {
    const recs = unitRecords[s.id].filter((r) => r.saved)
    return {
      station: s.title,
      done: recs.length,
      pass: recs.filter((r) => r.passed).length,
      fail: recs.filter((r) => !r.passed).length,
      lastMeasured: recs[recs.length - 1]?.measuredValue || '—'
    }
  }).filter((r) => r.done > 0)
)

function matchItems(station, items) {
  const codes = new Set(station.itemCodes || [])
  const keys = station.itemNameKeys || []
  return items.filter((it) =>
    codes.has(it.itemCode) || keys.some((k) => (it.itemName || '').includes(k))
  )
}

function aggregateStation(stationId) {
  const recs = unitRecords[stationId].filter((r) => r.saved)
  if (!recs.length) return null
  const anyFail = recs.some((r) => !r.passed)
  const last = recs[recs.length - 1]
  return { passed: !anyFail, measuredValue: last.measuredValue, remark: `仿真检测${recs.length}台` }
}

async function syncToItems() {
  if (!props.inspection?.inspectionId) return
  let items = [...(props.items || [])]
  if (!items.length) {
    ElMessage.warning('请先生成默认检测项')
    return
  }
  syncing.value = true
  try {
    for (const station of stations) {
      const agg = aggregateStation(station.id)
      if (!agg) continue
      const matched = matchItems(station, items)
      if (matched.length) {
        matched.forEach((it) => {
          it.measuredValue = agg.measuredValue
          it.result = agg.passed ? 'PASSED' : 'FAILED'
          if (!agg.passed) it.defectLevel = it.defectLevel || 'MAJOR'
        })
      } else {
        const leak = station.id === 'leakage'
        const flick = station.id === 'flicker'
        if (leak || flick) {
          const target = items.find((it) => (it.itemName || '').includes('亮度')) || items[0]
          if (target) {
            target.measuredValue = (target.measuredValue ? target.measuredValue + '; ' : '') + `${station.title}:${agg.measuredValue}`
            if (!agg.passed) target.result = 'FAILED'
          }
        }
      }
    }
    await saveInspectionItems(props.inspection.inspectionId, items)
    emit('update:items', items)
    emit('synced', items)
    ElMessage.success('五步检测结果已同步到质检单')
  } catch (e) {
    ElMessage.error(e?.message || '同步失败')
  } finally {
    syncing.value = false
  }
}
</script>

<style scoped>
.fpwb {
  border: 1px solid #e8e8e8;
  background: #fff;
  padding: 12px;
  margin-bottom: 12px;
}
.fpwb__head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.fpwb__title { font-size: 14px; font-weight: 500; color: #303133; }
.fpwb__sub { font-size: 12px; color: #909399; flex: 1; }
.fpwb__steps { margin-bottom: 14px; }
.fpwb__hub {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}
@media (max-width: 1200px) {
  .fpwb__hub { grid-template-columns: repeat(3, 1fr); }
}
.fpwb__card {
  border: 1px solid #dcdfe6;
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s;
  text-align: center;
  background: #fafafa;
}
.fpwb__card:hover:not(.is-locked) { border-color: #409eff; }
.fpwb__card.is-active { border-color: #409eff; box-shadow: 0 0 0 1px #409eff; }
.fpwb__card.is-done { border-color: #67c23a; }
.fpwb__card.is-locked { opacity: .55; cursor: not-allowed; }
.fpwb__img-wrap {
  height: 100px;
  overflow: hidden;
  background: #111;
  display: flex;
  align-items: center;
  justify-content: center;
}
.fpwb__img { max-width: 100%; max-height: 100%; object-fit: cover; }
.fpwb__label { font-size: 13px; padding: 6px 4px 2px; color: #303133; }
.fpwb__status { padding-bottom: 8px; }
.fpwb-dialog__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
}
.fpwb-dialog__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
</style>
