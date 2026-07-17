<template>
  <div class="lite-wb" :class="`lite-wb--${theme}`">
    <!-- 顶栏：筛选 + 刷新 -->
    <header class="lite-topbar">
      <div class="lite-topbar__left">
        <h2 class="lite-topbar__title">{{ data.title || '工作台' }}</h2>
        <span class="lite-topbar__time">{{ data.refreshTime || '—' }}</span>
      </div>
      <div class="lite-topbar__tools">
        <slot name="header-actions" />
        <el-select v-model="days" size="small" class="lite-select" @change="onFilterChange">
          <el-option :value="7" label="近7天" />
          <el-option :value="14" label="近14天" />
          <el-option :value="30" label="近30天" />
        </el-select>
        <el-select
          v-if="statusOptionsList.length"
          v-model="statusFilter"
          size="small"
          clearable
          placeholder="状态"
          class="lite-select"
          @change="onFilterChange"
        >
          <el-option v-for="o in statusOptionsList" :key="o.value" :value="o.value" :label="o.label" />
        </el-select>
        <el-button size="small" round :loading="loading" @click="$emit('refresh')">刷新</el-button>
      </div>
    </header>

    <slot name="toolbar" />

    <div v-loading="loading" class="lite-cols">
      <!-- 左栏：个人信息 -->
      <aside class="lite-col lite-col--left">
        <section class="lite-card lite-profile">
          <div class="lite-profile__avatar">
            <img :src="avatarSrc" alt="avatar" @error="onAvatarError" />
          </div>
          <h3 class="lite-profile__name">{{ profile.name }}</h3>
          <p class="lite-profile__role">{{ profile.role }}</p>
          <span class="lite-profile__online">
            <i />在线
          </span>
          <div class="lite-profile__chips">
            <span class="lite-chip lite-chip--blue">今日任务 {{ profileTodayCount }}</span>
          </div>
          <div class="lite-quick">
            <button
              v-for="link in quickLinks"
              :key="link.path"
              type="button"
              class="lite-quick__btn"
              @click="go(link.path)"
            >
              {{ link.label }}
            </button>
          </div>
        </section>

        <section class="lite-card">
          <header class="lite-card__hd">个人工作统计</header>
          <ul class="lite-stat-list">
            <li
              v-for="m in personalStats"
              :key="m.key"
              class="lite-stat-list__item"
              :class="statusClass(m.status || (m.warn ? 'warn' : 'normal'))"
              @click="m.link && go(m.link)"
            >
              <span class="lite-stat-list__label">{{ m.label }}</span>
              <span class="lite-stat-list__val">{{ m.value }}<small v-if="m.suffix">{{ m.suffix }}</small></span>
            </li>
          </ul>
        </section>

        <section class="lite-card lite-card--inbox">
          <header class="lite-card__hd lite-card__hd--row">
            <span>通知与待办</span>
            <span class="lite-card__badge">{{ notifications.length }}</span>
          </header>
          <ul v-if="notifications.length" class="lite-inbox">
            <li
              v-for="n in notifications"
              :key="n.id"
              class="lite-inbox__item"
              :class="{ 'is-pinned': n.pinned, [`is-${n.status}`]: n.status !== 'normal' }"
              @click="n.link && go(n.link)"
            >
              <div class="lite-inbox__avatar">{{ n.title.charAt(0) }}</div>
              <div class="lite-inbox__body">
                <strong>{{ n.title }}</strong>
                <p>{{ n.content }}</p>
              </div>
              <span class="lite-inbox__time">{{ n.time }}</span>
            </li>
          </ul>
          <p v-else class="lite-empty">暂无新通知</p>
        </section>

        <section v-if="data.attendance" class="lite-card lite-card--compact">
          <header class="lite-card__hd">今日考勤</header>
          <div class="lite-att">
            <span>上班 {{ data.attendance.checkInTime || '未打卡' }}</span>
            <span>下班 {{ data.attendance.checkOutTime || '未打卡' }}</span>
            <el-tag v-if="data.attendance.status" size="small" type="success">{{ data.attendance.status }}</el-tag>
          </div>
        </section>
      </aside>

      <!-- 中栏：日历 + 任务 -->
      <main class="lite-col lite-col--mid">
        <section class="lite-card">
          <header class="lite-card__hd lite-card__hd--row">
            <span>工作日历</span>
            <div class="lite-cal-nav">
              <button type="button" @click="shiftMonth(-1)">‹</button>
              <strong>{{ calYear }}年{{ calMonth + 1 }}月</strong>
              <button type="button" @click="shiftMonth(1)">›</button>
              <button type="button" class="lite-cal-nav__today" @click="goToday">今天</button>
            </div>
          </header>
          <div class="lite-cal">
            <div class="lite-cal__week">
              <span v-for="w in weekLabels" :key="w">{{ w }}</span>
            </div>
            <div class="lite-cal__grid">
              <button
                v-for="(dk, idx) in monthDays"
                :key="idx"
                type="button"
                class="lite-cal__day"
                :class="{
                  'is-empty': !dk,
                  'is-today': dk === todayKeyStr,
                  'is-selected': dk === selectedDate,
                  [`is-mark-${calendarMarks[dk]?.level || 0}`]: dk && calendarMarks[dk]
                }"
                :disabled="!dk"
                @click="dk && selectDate(dk)"
              >
                <template v-if="dk">{{ Number(dk.split('-')[2]) }}</template>
              </button>
            </div>
          </div>
        </section>

        <section class="lite-card">
          <header class="lite-card__hd lite-card__hd--row">
            <span>{{ selectedDate === todayKeyStr ? '今日待办' : `${selectedDate} 任务` }}</span>
            <span class="lite-card__badge">{{ dayTasks.length }} 项</span>
          </header>
          <ul v-if="dayTasks.length" class="lite-todo">
            <li v-for="(t, i) in dayTasks" :key="i" class="lite-todo__item" @click="t.link && go(t.link)">
              <span class="lite-todo__dot" :style="{ background: calendarMarks[t.date]?.color || accentColor }" />
              <div class="lite-todo__body">
                <strong>{{ t.title }}</strong>
                <span>{{ t.subtitle }}</span>
              </div>
            </li>
          </ul>
          <p v-else class="lite-empty">该日暂无任务安排</p>
        </section>

        <section class="lite-card">
          <header class="lite-card__hd lite-card__hd--row">
            <span>近期任务</span>
            <span class="lite-card__link" @click="ongoingTasks[0]?.link && go(ongoingTasks[0].link)">查看全部 ›</span>
          </header>
          <div v-if="ongoingTasks.length" class="lite-task-row">
            <article
              v-for="(task, i) in ongoingTasks"
              :key="i"
              class="lite-task-card"
              :style="{ background: task.color }"
              @click="task.link && go(task.link)"
            >
              <h4>{{ task.title }}</h4>
              <p>{{ task.subtitle }}</p>
              <div class="lite-task-card__bar">
                <div class="lite-task-card__fill" :style="{ width: task.progress + '%' }" />
              </div>
              <span v-if="task.daysLabel" class="lite-task-card__days">{{ task.daysLabel }}</span>
            </article>
          </div>
          <p v-else class="lite-empty">暂无进行中的任务</p>
        </section>

        <section v-if="timelineEvents.length" class="lite-card lite-card--compact">
          <header class="lite-card__hd">重要时间节点</header>
          <ul class="lite-timeline">
            <li v-for="(ev, i) in timelineEvents" :key="i" @click="ev.link && go(ev.link)">
              <span class="lite-timeline__date">{{ ev.date.slice(5) }}</span>
              <span class="lite-timeline__title">{{ ev.title }}</span>
            </li>
          </ul>
        </section>
      </main>

      <!-- 右栏：图表 + 表格 -->
      <aside class="lite-col lite-col--right">
        <section v-for="panel in chartPanels" :key="panel.key" class="lite-card lite-card--chart">
          <header class="lite-card__hd lite-card__hd--row">
            <span>{{ panel.title }}</span>
            <span v-if="panel.link" class="lite-card__link" @click="go(panel.link)">详情 ›</span>
          </header>
          <div class="lite-chart-wrap">
            <BoardChart
              v-if="panelHasData(panel)"
              :option="chartOptions[panel.key]"
              class="lite-chart"
              :class="`lite-chart--${panel.type}`"
            />
            <p v-else class="lite-empty">暂无数据</p>
          </div>
        </section>

        <section v-if="primaryTable" class="lite-card">
          <header class="lite-card__hd lite-card__hd--row">
            <span>{{ primaryTable.title }}</span>
            <span class="lite-card__link" @click="go(primaryTable.link)">{{ primaryTable.linkText || '下钻 ›' }}</span>
          </header>
          <div v-if="primaryTable.rows?.length" class="lite-table-wrap">
            <table class="lite-table">
              <thead>
                <tr>
                  <th v-for="col in primaryTable.columns?.slice(0, 4)" :key="col.prop">{{ col.label }}</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, ri) in primaryTable.rows.slice(0, 5)"
                  :key="ri"
                  @click="go(primaryTable.link)"
                >
                  <td v-for="col in primaryTable.columns?.slice(0, 4)" :key="col.prop">{{ row[col.prop] }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="lite-empty lite-empty--ok">✓ 暂无待处理项</p>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import BoardChart from '@/components/board/BoardChart.vue'
import { buildPanelOption, panelHasData } from '@/utils/workbenchCharts'
import { statusClass } from '@/constants/workbenchTheme'
import { LITE_THEME } from '@/constants/workbenchLayout'
import { useWorkbenchLayout, buildMonthGrid, todayKey } from '@/composables/useWorkbenchLayout'
import defaultAvatar from '@/assets/images/avatar/operator.png'

const props = defineProps({
  data: { type: Object, default: () => ({ metrics: [], panels: [], charts: [], tables: [], refreshTime: '' }) },
  loading: { type: Boolean, default: false },
  theme: { type: String, default: 'default' },
  filterDays: { type: Number, default: 7 },
  filterStatus: { type: String, default: '' },
  statusOptions: { type: Array, default: () => [] }
})

const emit = defineEmits(['refresh', 'filter-change'])

const router = useRouter()
const userStore = useUserStore()
const go = (path) => router.push(path)

const days = ref(props.filterDays)
const statusFilter = ref(props.filterStatus || '')
const statusOptionsList = computed(() => props.statusOptions?.length ? props.statusOptions : (props.data.statusOptions || []))
const now = new Date()
const calYear = ref(now.getFullYear())
const calMonth = ref(now.getMonth())
const selectedDate = ref(todayKey())
const todayKeyStr = todayKey()

watch(() => props.filterDays, (v) => { days.value = v })
watch(() => props.filterStatus, (v) => { statusFilter.value = v || '' })

function onFilterChange() {
  emit('filter-change', { days: days.value, status: statusFilter.value || '' })
}

const {
  quickLinks,
  profile,
  personalStats,
  calendarMarks,
  dayTasks,
  ongoingTasks,
  notifications,
  chartPanels,
  timelineEvents
} = useWorkbenchLayout(
  computed(() => props.data),
  props.theme,
  userStore,
  selectedDate,
  computed(() => ({ year: calYear.value, month: calMonth.value }))
)

const avatarFailed = ref(false)
const avatarSrc = computed(() => {
  const url = userStore.userInfo?.avatar || userStore.userInfo?.avatarUrl
  return !avatarFailed.value && url ? url : defaultAvatar
})
function onAvatarError() {
  avatarFailed.value = true
}

const profileTodayCount = computed(() => {
  const m = props.data.metrics?.find((x) => /待|today|pending/i.test(String(x.key) + x.label))
  return m ? m.value : dayTasks.value.length
})

const accentColor = computed(() => LITE_THEME.accent[props.theme] || LITE_THEME.accent.order)

const monthDays = computed(() => buildMonthGrid(calYear.value, calMonth.value))
const weekLabels = ['日', '一', '二', '三', '四', '五', '六']

const primaryTable = computed(() => props.data.tables?.[0])

const chartOptions = computed(() => {
  const map = {}
  for (const p of chartPanels.value) {
    map[p.key] = buildPanelOption(p)
  }
  return map
})

function selectDate(dk) {
  selectedDate.value = dk
}

function shiftMonth(delta) {
  let m = calMonth.value + delta
  let y = calYear.value
  if (m < 0) { m = 11; y -= 1 }
  if (m > 11) { m = 0; y += 1 }
  calMonth.value = m
  calYear.value = y
}

function goToday() {
  const t = new Date()
  calYear.value = t.getFullYear()
  calMonth.value = t.getMonth()
  selectedDate.value = todayKey()
}
</script>

<style scoped>
.lite-wb {
  --lite-accent: #6b8fc7;
  min-height: calc(100vh - 92px);
  background: #eef5f1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.lite-wb--order { --lite-accent: #6b8fc7; }
.lite-wb--admin { --lite-accent: #9b8ec4; }
.lite-wb--planner { --lite-accent: #5ba8a8; }
.lite-wb--operator { --lite-accent: #6aab7a; }
.lite-wb--cost { --lite-accent: #c9956a; }
.lite-wb--purchase { --lite-accent: #6b9fd4; }
.lite-wb--warehouse { --lite-accent: #7aab8f; }

.lite-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  flex-shrink: 0;
}

.lite-topbar__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #2c3540;
}

.lite-topbar__time {
  margin-left: 12px;
  font-size: 11px;
  color: #8a9199;
}

.lite-topbar__tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lite-select { width: 96px; }

.lite-cols {
  display: grid;
  grid-template-columns: minmax(232px, 24fr) minmax(0, 46fr) minmax(272px, 30fr);
  gap: 12px;
  padding: 0 14px 14px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.lite-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: thin;
}

.lite-card {
  background: #fff;
  border: 1px solid #e8ece9;
  border-radius: 14px;
  padding: 12px 14px;
  box-shadow: 0 2px 12px rgba(30, 50, 40, 0.05);
  flex-shrink: 0;
}

.lite-card--compact { padding: 10px 14px; }
.lite-card--chart { padding-bottom: 6px; }

.lite-col--left .lite-card--inbox {
  flex: 1;
  min-height: 180px;
  display: flex;
  flex-direction: column;
}

.lite-col--left .lite-card--inbox .lite-inbox {
  flex: 1;
  max-height: none;
  min-height: 120px;
}

.lite-col--left .lite-inbox__body p {
  max-width: 100%;
}

.lite-card__hd {
  display: flex;
  align-items: center;
  min-height: 24px;
  font-size: 13px;
  font-weight: 700;
  color: #2c3540;
  margin-bottom: 8px;
}

.lite-card__hd--row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.lite-card__badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 999px;
  background: #eef5f1;
  color: var(--lite-accent);
}

.lite-card__link {
  font-size: 11px;
  color: var(--lite-accent);
  cursor: pointer;
}

/* 左栏 profile */
.lite-profile {
  text-align: center;
  padding-top: 16px;
}

.lite-profile__avatar {
  width: 72px;
  height: 72px;
  margin: 0 auto 8px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #e8ece9;
  background: #f0f5f2;
}

.lite-profile__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.lite-profile__name {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 700;
  color: #2c3540;
}

.lite-profile__role {
  margin: 0 0 10px;
  font-size: 12px;
  color: #8a9199;
}

.lite-profile__online {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #5a9a6a;
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid #c8e6d0;
  background: #f0faf3;
  margin-bottom: 12px;
}

.lite-profile__online i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #5a9a6a;
}

.lite-profile__chips { margin-bottom: 14px; }

.lite-chip {
  display: inline-block;
  font-size: 11px;
  padding: 4px 12px;
  border-radius: 999px;
}

.lite-chip--blue { background: #d8ecfd; color: #4a6fa5; }

.lite-quick {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}

.lite-quick__btn {
  border: 1px solid #e8ece9;
  background: #fafcfb;
  border-radius: 999px;
  padding: 5px 12px;
  font-size: 11px;
  color: #5c6672;
  cursor: pointer;
  transition: all 0.15s;
}

.lite-quick__btn:hover {
  border-color: var(--lite-accent);
  color: var(--lite-accent);
  background: #fff;
}

.lite-stat-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.lite-stat-list__item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 0;
  border-bottom: 1px solid #f0f2f0;
  font-size: 12px;
  cursor: pointer;
}

.lite-stat-list__item:hover { color: var(--lite-accent); }
.lite-stat-list__item.is-warn .lite-stat-list__val { color: #b89a4a; }
.lite-stat-list__item.is-danger .lite-stat-list__val { color: #c45a5a; }

.lite-stat-list__label { color: #8a9199; }
.lite-stat-list__val { font-weight: 700; color: #2c3540; }
.lite-stat-list__val small { font-weight: 500; font-size: 10px; margin-left: 2px; }

.lite-att {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  color: #606266;
}

/* 日历 */
.lite-cal-nav {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.lite-cal-nav button {
  border: none;
  background: #f0f2f0;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  cursor: pointer;
  color: #5c6672;
}

.lite-cal-nav__today {
  width: auto !important;
  padding: 0 8px !important;
  font-size: 11px;
}

.lite-cal {
  max-height: 500px;
}

.lite-cal__week {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 10px;
  color: #8a9199;
  margin-bottom: 4px;
}

.lite-cal__grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.lite-cal__day {
  height: 56px;
  border: none;
  border-radius: 8px;
  background: #f8faf9;
  font-size: 12px;
  color: #5c6672;
  cursor: pointer;
  transition: all 0.12s;
}

.lite-cal__day.is-empty { visibility: hidden; pointer-events: none; }
.lite-cal__day.is-today { border: 2px solid var(--lite-accent); font-weight: 700; }
.lite-cal__day.is-selected { background: var(--lite-accent); color: #fff; }
.lite-cal__day.is-mark-1 { background: #fde3e5; }
.lite-cal__day.is-mark-2 { background: #fef4cf; }
.lite-cal__day.is-mark-3 { background: #d8ecfd; font-weight: 600; }

/* 待办 */
.lite-todo {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 180px;
  overflow-y: auto;
  scrollbar-width: thin;
}

.lite-todo__item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f0;
  cursor: pointer;
}

.lite-todo__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.lite-todo__body strong {
  display: block;
  font-size: 13px;
  color: #2c3540;
}

.lite-todo__body span {
  font-size: 11px;
  color: #8a9199;
}

/* 任务卡片 */
.lite-task-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.lite-task-card {
  border-radius: 12px;
  padding: 12px;
  cursor: pointer;
  min-height: 88px;
  position: relative;
  transition: transform 0.15s;
}

.lite-task-card:hover { transform: translateY(-2px); }

.lite-task-card h4 {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 700;
  color: #2c3540;
}

.lite-task-card p {
  margin: 0 0 10px;
  font-size: 11px;
  color: #5c6672;
}

.lite-task-card__bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 2px;
  overflow: hidden;
}

.lite-task-card__fill {
  height: 100%;
  background: rgba(44, 53, 64, 0.35);
  border-radius: 2px;
}

.lite-task-card__days {
  position: absolute;
  right: 10px;
  bottom: 10px;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  color: #5c6672;
}

/* 时间线 */
.lite-timeline {
  list-style: none;
  margin: 0;
  padding: 0;
}

.lite-timeline li {
  display: flex;
  gap: 10px;
  padding: 6px 0;
  font-size: 11px;
  cursor: pointer;
  border-bottom: 1px dashed #eef1ef;
}

.lite-timeline__date {
  color: var(--lite-accent);
  font-weight: 600;
  min-width: 36px;
}

.lite-timeline__title { color: #5c6672; }

/* 收件箱 */
.lite-inbox {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 220px;
  overflow-y: auto;
  scrollbar-width: thin;
}

.lite-inbox__item {
  display: grid;
  grid-template-columns: 32px 1fr auto;
  gap: 8px;
  padding: 8px;
  border-radius: 10px;
  margin-bottom: 4px;
  cursor: pointer;
  transition: background 0.12s;
}

.lite-inbox__item:hover { background: #f8faf9; }
.lite-inbox__item.is-pinned {
  background: #2c3540;
  color: #fff;
}

.lite-inbox__item.is-pinned .lite-inbox__body p,
.lite-inbox__item.is-pinned .lite-inbox__time { color: rgba(255, 255, 255, 0.75); }

.lite-inbox__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #eef5f1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--lite-accent);
}

.lite-inbox__item.is-pinned .lite-inbox__avatar {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.lite-inbox__body strong {
  display: block;
  font-size: 12px;
  margin-bottom: 2px;
}

.lite-inbox__body p {
  margin: 0;
  font-size: 11px;
  color: #8a9199;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 160px;
}

.lite-inbox__time {
  font-size: 10px;
  color: #8a9199;
  white-space: nowrap;
}

/* 图表 */
.lite-chart-wrap { min-height: 0; }

.lite-chart {
  width: 100%;
  height: 160px;
}

.lite-chart--donut,
.lite-chart--pie {
  height: 150px;
}

.lite-chart--horizontalBar {
  height: 140px;
}

/* 表格 */
.lite-table-wrap { overflow-x: auto; }

.lite-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 11px;
}

.lite-table th {
  text-align: left;
  padding: 6px 4px;
  color: #8a9199;
  font-weight: 600;
  border-bottom: 1px solid #eef1ef;
}

.lite-table td {
  padding: 7px 4px;
  color: #5c6672;
  border-bottom: 1px solid #f8faf9;
}

.lite-table tbody tr { cursor: pointer; }
.lite-table tbody tr:hover { background: #f8faf9; }

.lite-empty {
  text-align: center;
  color: #8a9199;
  font-size: 12px;
  padding: 16px 0;
  margin: 0;
}

.lite-empty--ok { color: #5a9a6a; }

@media (min-width: 1920px) {
  .lite-cols {
    grid-template-columns: minmax(260px, 24fr) minmax(0, 46fr) minmax(320px, 30fr);
    gap: 16px;
    padding: 0 20px 18px;
  }

  .lite-cal__day { height: 58px; }
  .lite-task-row { grid-template-columns: repeat(4, 1fr); }
  .lite-chart { height: 180px; }
  .lite-chart--donut,
  .lite-chart--pie { height: 168px; }
  .lite-inbox { max-height: 260px; }
  .lite-todo { max-height: 220px; }
}

@media (max-width: 1280px) {
  .lite-cols {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }

  .lite-col { overflow: visible; }
  .lite-task-row { grid-template-columns: 1fr 1fr; }
}
</style>
