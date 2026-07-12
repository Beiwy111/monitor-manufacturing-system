<template>
  <div class="op-report-page">
    <!-- 工序选择 -->
    <section v-if="flowStep === 0" class="op-report-page__section">
      <h2 class="op-report-page__heading">选择报工工序</h2>
      <p class="op-report-page__sub">
        四道生产工序独立报工窗口，点击进入对应工序进行生产报工
        <span v-if="binding">（您负责：{{ binding.stageName }} · {{ binding.workshopName }}）</span>
      </p>

      <div class="op-station-grid">
        <button
          v-for="s in stations"
          :key="s.id"
          type="button"
          class="op-station-card"
          :class="{
            'is-mine': isMyStation(s),
            'is-locked': !canEnterStation(s),
            'is-active': activeStationId === s.id
          }"
          :disabled="!canEnterStation(s)"
          @click="enterStation(s)"
        >
          <div class="op-station-card__img">
            <img :src="s.image" :alt="s.title" />
            <span class="op-station-card__order">{{ s.order }}</span>
          </div>
          <div class="op-station-card__body">
            <h3>{{ s.title }}</h3>
            <p>{{ s.subtitle }}</p>
            <div class="op-station-card__meta">
              <el-tag v-if="isMyStation(s)" type="success" size="small">我的工序</el-tag>
              <el-tag v-else-if="!canEnterStation(s)" type="info" size="small">非本工序</el-tag>
              <el-tag size="small" :type="taskCount(s) > 0 ? 'warning' : 'info'">
                待报工 {{ taskCount(s) }}
              </el-tag>
            </div>
          </div>
        </button>
      </div>
    </section>

    <!-- 工序报工窗口 -->
    <section v-else class="op-report-page__section">
      <div class="op-report-page__bar">
        <el-button link type="primary" @click="backToHub">← 返回工序选择</el-button>
        <el-tag type="primary">第 {{ activeStation?.order }}/4 道工序</el-tag>
        <strong>{{ activeStation?.title }}</strong>
      </div>
      <OperatorProcessWorkbench v-if="activeStation" :station="activeStation" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { DISPATCH_ACTIVE } from '@/mock/constants'
import { operatorBinding } from '@/utils/operatorWorkshop'
import {
  OPERATOR_PROCESS_STATIONS,
  stationById,
  stationByStageName,
  dispatchMatchesStage
} from '@/config/operatorProcessStations'
import OperatorProcessWorkbench from '@/components/production/OperatorProcessWorkbench.vue'

const route = useRoute()
const router = useRouter()
const mes = useMesStore()
const userStore = useUserStore()

const flowStep = ref(0)
const activeStationId = ref('')

const stations = OPERATOR_PROCESS_STATIONS
const username = computed(() => userStore.userInfo?.username)
const binding = computed(() => operatorBinding(username.value))
const activeStation = computed(() => stationById(activeStationId.value))

const myDispatches = computed(() => mes.myDispatches(username.value))

function isMyStation(station) {
  if (!binding.value) return true
  return binding.value.stageName === station.stageName
}

function canEnterStation(station) {
  if (!binding.value) return true
  return binding.value.stageName === station.stageName
}

function taskCount(station) {
  return myDispatches.value.filter((d) =>
    DISPATCH_ACTIVE.includes(d.status) && dispatchMatchesStage(d, station)
  ).length
}

function enterStation(station) {
  if (!canEnterStation(station)) {
    ElMessage.warning(`您负责「${binding.value?.stageName}」，请进入对应工序窗口报工`)
    return
  }
  activeStationId.value = station.id
  flowStep.value = 1
  router.replace({ query: { stage: station.id } })
}

function backToHub() {
  flowStep.value = 0
  activeStationId.value = ''
  router.replace({ query: {} })
}

onMounted(async () => {
  try {
    await mes.hydrateFromApi()
  } catch { /* ignore */ }

  const qStage = route.query.stage
  if (qStage) {
    const s = stationById(String(qStage))
    if (s && canEnterStation(s)) {
      activeStationId.value = s.id
      flowStep.value = 1
      return
    }
  }

  // 已绑定操作员直接进入本工序窗口
  if (binding.value) {
    const s = stationByStageName(binding.value.stageName)
    if (s) {
      activeStationId.value = s.id
      flowStep.value = 1
      router.replace({ query: { stage: s.id } })
    }
  }
})
</script>

<style scoped>
.op-report-page {
  padding: 0;
}

.op-report-page__section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
}

.op-report-page__heading {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.op-report-page__sub {
  margin: 0 0 24px;
  font-size: 14px;
  color: #909399;
}

.op-station-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1400px) {
  .op-station-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 700px) {
  .op-station-grid { grid-template-columns: 1fr; }
}

.op-station-card {
  text-align: left;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  cursor: pointer;
  padding: 0;
  transition: transform .2s, border-color .2s, box-shadow .2s;
}

.op-station-card:hover:not(:disabled) {
  transform: translateY(-3px);
  border-color: #409eff;
  box-shadow: 0 10px 28px rgba(64, 158, 255, .18);
}

.op-station-card.is-mine {
  border-color: #67c23a;
}

.op-station-card.is-mine:hover:not(:disabled) {
  border-color: #67c23a;
  box-shadow: 0 10px 28px rgba(103, 194, 58, .2);
}

.op-station-card:disabled {
  opacity: 1;
}

.op-station-card.is-locked {
  cursor: not-allowed;
}

.op-station-card.is-locked .op-station-card__img img {
  filter: none;
  opacity: 1;
}

.op-station-card__img {
  position: relative;
  height: 200px;
  background: #0d1117;
  overflow: hidden;
}

.op-station-card__img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.op-station-card__order {
  position: absolute;
  top: 10px;
  left: 10px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(45, 138, 102, .92);
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.op-station-card__body {
  padding: 14px 16px 18px;
}

.op-station-card__body h3 {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 700;
  color: #303133;
}

.op-station-card__body p {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.55;
  color: #909399;
  min-height: 38px;
}

.op-station-card__meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.op-report-page__bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  font-size: 15px;
}
</style>
