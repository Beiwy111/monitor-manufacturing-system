import { ref, onMounted, onUnmounted } from 'vue'
import { fetchDashboardSnapshot } from '@/api/mes'

const POLL_MS = 5000

const EMPTY_SNAPSHOT = {
  refreshTime: '-',
  productionOverview: { summary: {}, workshops: [] },
  kpi: [],
  equipment: [],
  alarms: [],
  workshops3d: [],
  systemStatus: '未知',
  dataSource: ''
}

export function useManagerDashboard() {
  const loading = ref(false)
  const apiError = ref('')
  const lastRefreshTime = ref('')
  const snapshot = ref({ ...EMPTY_SNAPSHOT })

  let timer = null

  async function loadDashboard() {
    loading.value = true
    try {
      const data = await fetchDashboardSnapshot()
      if (data && typeof data === 'object') {
        snapshot.value = { ...EMPTY_SNAPSHOT, ...data }
        lastRefreshTime.value = data.refreshTime || new Date().toLocaleString('zh-CN', { hour12: false })
        apiError.value = ''
      } else {
        apiError.value = '接口返回异常'
      }
    } catch (e) {
      apiError.value = e?.message || '无法连接生产大屏接口，显示占位数据'
    } finally {
      loading.value = false
    }
  }

  function startPolling() {
    loadDashboard()
    timer = setInterval(loadDashboard, POLL_MS)
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  onMounted(startPolling)
  onUnmounted(stopPolling)

  return {
    loading,
    apiError,
    lastRefreshTime,
    snapshot,
    refreshDashboard: loadDashboard
  }
}
