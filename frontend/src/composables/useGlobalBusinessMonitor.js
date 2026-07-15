import { onMounted, onUnmounted } from 'vue'
import { fetchAlarmViews } from '@/api/business'
import { fetchVoiceNotices } from '@/api/assistant'
import { useNotificationStore, NOTIF_TYPE } from '@/stores/notification'
import { useUserStore } from '@/stores/user'

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

  async function poll() {
    try {
      const [alarmRes, noticeRes] = await Promise.all([
        fetchAlarmViews(),
        fetchVoiceNotices().catch(() => [])
      ])
      const alarms = Array.isArray(alarmRes) ? alarmRes : alarmRes?.data || []
      const notices = Array.isArray(noticeRes) ? noticeRes : noticeRes?.data || []
      notifications.sync([...buildAlarmInbox(alarms), ...buildNoticeInbox(notices)])
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
