import { onMounted, onUnmounted } from 'vue'
import { fetchAlarmViews } from '@/api/business'
import { fetchVoiceNotices } from '@/api/assistant'
import { fetchOperatorNotifications } from '@/api/mes'
import { useNotificationStore, NOTIF_TYPE } from '@/stores/notification'
import { useUserStore } from '@/stores/user'
import { resolveOperatorUsername } from '@/utils/operatorWorkshop'

const POLL_INTERVAL = 15000

/** 协办通知目标模块 → 可见角色（admin 全可见） */
const NOTICE_AUDIENCE = {
  aftersale:  ['aftersale'],
  device:     ['device'],
  production: ['manager', 'operator', 'planner'],
  purchase:   ['purchase'],
  warehouse:  ['warehouse'],
  quality:    ['quality'],
  order:      ['order'],
  cost:       ['cost'],
  system:     ['admin'],
}

export function useGlobalBusinessMonitor() {
  const notifications = useNotificationStore()
  const user = useUserStore()
  let timer = null

  function sameUser(name) {
    const normalize = value => String(value || '').replace(/\s+/g, '').toLowerCase()
    const target = normalize(name)
    return !!target && [user.userInfo?.username, user.userInfo?.realName, user.displayName]
      .map(normalize)
      .some(value => value && value === target)
  }

  function buildAlarmInbox(alarms) {
    const role = user.roleKey
    return (alarms || []).flatMap(alarm => {
      const status = alarm.alarmStatus
      const isReporter = sameUser(alarm.reporterName)
      const base = {
        type: NOTIF_TYPE.ALARM,
        content: alarm.alarmDescription || '',
        createdAt: Date.parse(alarm.reportedAt) || Date.now()
      }

      if (status === 'OPEN' && ['device', 'manager'].includes(role) && !isReporter) {
        return [{
          ...base,
          sourceKey: `alarm:${alarm.alarmId}:OPEN:${user.userInfo?.username}`,
          title: `待接收安灯 ${alarm.alarmNo}`,
          from: `${alarm.reporterName || '操作员'} 安灯上报`,
          link: '/device/alarm'
        }]
      }

      if (isReporter && ['RECEIVED', 'PROCESSING', 'CLOSED'].includes(status)) {
        return [{
          ...base,
          sourceKey: `alarm:${alarm.alarmId}:${status}:${user.userInfo?.username}`,
          title: `安灯处理进度 ${alarm.alarmNo}`,
          content: status === 'CLOSED' ? (alarm.closeResult || '报警已解除') : `${alarm.receiverName || '设备人员'}已${status === 'RECEIVED' ? '接收' : '开始处理'}`,
          from: `${alarm.receiverName || '设备人员'} 处理回执`,
          link: '/production/andon-report'
        }]
      }

      return []
    })
  }

  /** 语音助手跨模块协办通知：只投递给目标模块的角色（仅通知，不改业务数据） */
  function buildNoticeInbox(notices) {
    const role = user.roleKey
    return (notices || [])
      .filter(n => role === 'admin' || (NOTICE_AUDIENCE[n.targetModule] || []).includes(role))
      .map(n => ({
        type: NOTIF_TYPE.SYSTEM,
        sourceKey: `voice-notice:${n.id}:${user.userInfo?.username}`,
        title: n.title || '协办通知',
        content: n.content || '',
        from: n.from || '语音助手',
        createdAt: Date.parse(n.createdAt) || Date.now()
      }))
  }

  /** 派工 / 质检待检通知：按 targetUsername 投递 */
  function buildDispatchInbox(notices) {
    const username = resolveOperatorUsername(user.roleKey, user.userInfo?.username)
    if (!username) return []
    return (notices || []).map(n => ({
      type: NOTIF_TYPE.PROCESS,
      sourceKey: `${n.kind || 'dispatch'}-notice:${n.id}:${username}`,
      title: n.title || (n.kind === 'qc' ? '新成品质检任务' : '新派工任务'),
      content: n.content || '',
      from: n.from || (n.kind === 'qc' ? '操作员' : '生产主管'),
      link: n.link || (n.kind === 'qc' ? '/quality/fp/inspection' : '/production/my-dispatch'),
      createdAt: Date.parse(n.createdAt) || Date.now()
    }))
  }

  async function poll() {
    try {
      const username = resolveOperatorUsername(user.roleKey, user.userInfo?.username)
      const [alarmRes, noticeRes, dispatchRes] = await Promise.all([
        fetchAlarmViews(),
        fetchVoiceNotices().catch(() => []),
        username ? fetchOperatorNotifications(username).catch(() => []) : Promise.resolve([])
      ])
      const alarms = Array.isArray(alarmRes) ? alarmRes : alarmRes?.data || []
      const notices = Array.isArray(noticeRes) ? noticeRes : noticeRes?.data || []
      const dispatchNotices = Array.isArray(dispatchRes) ? dispatchRes : dispatchRes?.data || []
      notifications.sync([
        ...buildAlarmInbox(alarms),
        ...buildNoticeInbox(notices),
        ...buildDispatchInbox(dispatchNotices)
      ])
    } catch {
      // 保留上一次成功同步的收件箱。
    }
  }

  onMounted(() => {
    notifications.configureAudience(user.userInfo)
    poll()
    timer = window.setInterval(poll, POLL_INTERVAL)
  })
  onUnmounted(() => window.clearInterval(timer))
}
