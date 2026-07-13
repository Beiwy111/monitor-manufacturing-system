<template>
  <div class="twin-panel" id="section-twin">
    <!-- 面板头部 -->
    <div class="twin-panel__head">
      <div class="twin-panel__title-row">
        <span class="twin-panel__icon">⬡</span>
        <span class="twin-panel__title">设备健康孪生</span>
        <span class="twin-panel__sub">实时评分 · 悬停查看3D · 点击详情</span>
      </div>
      <div class="twin-panel__tools">
        <el-select v-model="filterLevel" clearable placeholder="健康等级" size="small" style="width:96px">
          <el-option label="✅ 优良" value="GOOD" />
          <el-option label="⚠ 注意" value="WARN" />
          <el-option label="🔴 警告" value="ALERT" />
          <el-option label="🚨 危险" value="DANGER" />
        </el-select>
        <el-select v-model="filterWs" clearable placeholder="车间" size="small" style="width:106px">
          <el-option v-for="w in workshops" :key="w" :label="w" :value="w" />
        </el-select>
        <el-button :loading="loading" size="small" @click="load">刷新</el-button>

        <div class="twin-panel__badges">
          <span v-for="b in levelBadges" :key="b.level"
            class="level-badge"
            :style="{ background: b.color + '1a', color: b.color, borderColor: b.color + '55' }">
            {{ b.label }} {{ b.count }}
          </span>
        </div>
      </div>
    </div>

    <!-- 设备卡片网格 -->
    <div v-loading="loading" class="twin-grid">
      <EquipmentTwinCard
        v-for="eq in filtered"
        :key="eq.equipmentId"
        :data="eq"
        @click="openDetail"
      />
      <div v-if="!loading && !filtered.length" class="twin-empty">
        <el-empty description="暂无符合条件的设备" :image-size="80" />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <EquipmentDetailDrawer v-model="drawerVisible" :data="selected" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { fetchEquipmentHealth } from '@/api/business'
import EquipmentTwinCard from './EquipmentTwinCard.vue'
import EquipmentDetailDrawer from './EquipmentDetailDrawer.vue'

const list          = ref([])
const loading       = ref(false)
const filterLevel   = ref('')
const filterWs      = ref('')
const drawerVisible = ref(false)
const selected      = ref(null)
let timer = null

const workshops = computed(() =>
  [...new Set(list.value.map(e => e.workshop).filter(Boolean))]
)

const LEVEL_META = {
  GOOD:   { color: '#67c23a', label: '优良' },
  WARN:   { color: '#e6a23c', label: '注意' },
  ALERT:  { color: '#f56c6c', label: '警告' },
  DANGER: { color: '#c0392b', label: '危险' },
}

const levelBadges = computed(() =>
  Object.entries(LEVEL_META)
    .map(([level, meta]) => ({
      level, label: meta.label, color: meta.color,
      count: list.value.filter(e => e.healthLevel === level).length
    }))
    .filter(b => b.count > 0)
)

const filtered = computed(() =>
  list.value
    .filter(e => !filterLevel.value || e.healthLevel === filterLevel.value)
    .filter(e => !filterWs.value    || e.workshop    === filterWs.value)
    .sort((a, b) => {
      if (a.healthScore !== b.healthScore) return a.healthScore - b.healthScore
      return (b.alarm7d ?? 0) - (a.alarm7d ?? 0)
    })
)

function openDetail(eq) { selected.value = eq; drawerVisible.value = true }

async function load() {
  loading.value = true
  try {
    const res = await fetchEquipmentHealth()
    list.value = Array.isArray(res) ? res : (res.data ?? [])
  } catch { /* 静默 */ } finally { loading.value = false }
}

onMounted(() => { load(); timer = setInterval(load, 30000) })
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
/* ── 面板容器：与上方白卡区块区分 ── */
.twin-panel {
  background: linear-gradient(160deg, #edf3ff 0%, #f2f5fa 100%);
  border-radius: 12px;
  padding: 16px 18px 22px;
  border: 1px solid #d8e6f8;
  box-shadow: 0 3px 12px rgba(0, 27, 63, 0.07);
}

/* 头部 */
.twin-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}
.twin-panel__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.twin-panel__icon {
  font-size: 18px;
  line-height: 1;
  color: #3b82f6;
}
.twin-panel__title {
  font-size: 15px;
  font-weight: 700;
  color: #001b3f;
}
.twin-panel__sub {
  font-size: 12px;
  color: #a0a4ab;
}
.twin-panel__tools {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

/* 统计徽章 */
.twin-panel__badges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.level-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 9px;
  border-radius: 10px;
  border: 1px solid transparent;
}

/* ── 卡片网格：固定列宽，杜绝末行拉伸变形 ── */
.twin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 158px);
  justify-content: start;
  gap: 12px;
  min-height: 120px;
}

.twin-empty {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}

@media (max-width: 900px) {
  .twin-grid {
    grid-template-columns: repeat(auto-fill, 140px);
  }
}
</style>
