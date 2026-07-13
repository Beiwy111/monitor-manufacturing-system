import { ref, onUnmounted } from 'vue'
import { fetchEquipmentHealth, fetchAlarmViews } from '@/api/business'
import { useNotificationStore, NOTIF_TYPE } from '@/stores/notification.js'

const STATUS_CN = {
  IDLE:        '空闲',
  RUNNING:     '运行中',
  FAULT:       '故障',
  MAINTAINING: '维保中',
  SCRAPPED:    '已报废',
}

// 状态变化 → 通知类型 + 标题模板
function resolveChangeNotif(code, name, prev, next) {
  // 变为故障
  if (next === 'FAULT') return {
    type: NOTIF_TYPE.ALARM,
    title: `${name}（${code}）状态变为故障`,
    content: `设备由「${STATUS_CN[prev] ?? prev}」转为「故障」，请立即排查并触发安灯报警。`,
    from: '设备实时监控',
  }
  // 故障 / 维保 → 正常
  if ((prev === 'FAULT' || prev === 'MAINTAINING') && (next === 'IDLE' || next === 'RUNNING')) return {
    type: NOTIF_TYPE.PROCESS,
    title: `${name}（${code}）已恢复正常`,
    content: `设备由「${STATUS_CN[prev] ?? prev}」恢复为「${STATUS_CN[next] ?? next}」，可重新投入使用。`,
    from: '设备实时监控',
  }
  // 开始维保
  if (next === 'MAINTAINING') return {
    type: NOTIF_TYPE.MAINT,
    title: `${name}（${code}）开始维保`,
    content: `设备由「${STATUS_CN[prev] ?? prev}」转为维保中，预计影响产能，请关注完成时间。`,
    from: '维保管理模块',
  }
  // 空闲 → 运行（生产人员启动设备）
  if (prev === 'IDLE' && next === 'RUNNING') return {
    type: NOTIF_TYPE.PROCESS,
    title: `${name}（${code}）已启动运行`,
    content: `设备由空闲状态切换为运行中，生产任务已下发。`,
    from: '生产调度',
  }
  // 运行 → 空闲
  if (prev === 'RUNNING' && next === 'IDLE') return {
    type: NOTIF_TYPE.PROCESS,
    title: `${name}（${code}）已停机待机`,
    content: `设备由运行中切换为空闲，当前无生产任务。`,
    from: '生产调度',
  }
  return null
}

/**
 * 设备实时监控 composable
 *
 * 用法：
 *   const { start, stop } = useEquipmentMonitor(factorySceneRef)
 *   onMounted(start)
 *   onUnmounted(stop)
 *
 * @param {Ref<{syncEquipments: Function} | null>} sceneRef  FactoryScene 组件 ref
 * @param {number} interval  轮询间隔 ms（默认 15000）
 */
export function useEquipmentMonitor(sceneRef, interval = 15000) {
  const notifStore = useNotificationStore()

  // 上一轮的状态快照：Map<equipmentCode, status>
  const prevStatusMap = new Map()
  // 上一轮已知的报警ID集合
  const knownAlarmIds = new Set()

  let healthTimer = null
  let alarmTimer  = null
  let initialized = false

  // ── 健康/状态轮询 ─────────────────────────────────────────────
  async function pollHealth() {
    try {
      const res  = await fetchEquipmentHealth()
      const list = Array.isArray(res) ? res : (res.data ?? [])
      if (!list.length) return

      // 首次初始化快照，不推通知
      if (!initialized) {
        list.forEach(eq => prevStatusMap.set(eq.equipmentCode, eq.status))
        initialized = true
        // 场景首次同步
        sceneRef?.value?.syncEquipments(list)
        return
      }

      // 检测状态变化
      list.forEach(eq => {
        const prev = prevStatusMap.get(eq.equipmentCode)
        const next = eq.status
        if (prev && prev !== next) {
          const notif = resolveChangeNotif(eq.equipmentCode, eq.equipmentName, prev, next)
          if (notif) notifStore.push(notif)
        }
        prevStatusMap.set(eq.equipmentCode, next)
      })

      // 同步 3D 场景颜色
      sceneRef?.value?.syncEquipments(list)

    } catch { /* 静默，下次轮询重试 */ }
  }

  // ── 报警轮询 ──────────────────────────────────────────────────
  async function pollAlarms() {
    try {
      const res  = await fetchAlarmViews()
      const list = Array.isArray(res) ? res : (res.data ?? [])
      if (!list.length) return

      // 首次初始化，记录已存在的报警ID，不推通知（避免重启后刷屏）
      if (knownAlarmIds.size === 0) {
        list.forEach(a => knownAlarmIds.add(a.alarmId))
        return
      }

      // 检测新增的 OPEN 报警
      list
        .filter(a => a.alarmStatus === 'OPEN' && !knownAlarmIds.has(a.alarmId))
        .forEach(a => {
          knownAlarmIds.add(a.alarmId)
          notifStore.push({
            type:    NOTIF_TYPE.ALARM,
            title:   `${a.equipmentName} 触发${a.alarmLevelCn ?? ''}报警`,
            content: a.alarmDescription ?? `报警单号：${a.alarmNo}，请及时处理。`,
            from:    '安灯报警系统',
          })
        })

      // 更新已知报警集合（已关闭的也保留，防止重复推送）
      list.forEach(a => knownAlarmIds.add(a.alarmId))

    } catch { /* 静默 */ }
  }

  function start() {
    // 立即执行一次，再定时
    pollHealth()
    pollAlarms()
    healthTimer = setInterval(pollHealth,  interval)
    alarmTimer  = setInterval(pollAlarms, interval)
  }

  function stop() {
    clearInterval(healthTimer)
    clearInterval(alarmTimer)
    healthTimer = null
    alarmTimer  = null
  }

  // 组件卸载时自动停止
  onUnmounted(stop)

  return { start, stop }
}
