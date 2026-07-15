import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchRoleWorkbenchDashboard } from '@/api/workbench'

const EMPTY = { metrics: [], panels: [], charts: [], tables: [], refreshTime: '' }

export function useRoleWorkbenchDashboard(roleKey) {
  const loading = ref(false)
  const data = ref({ ...EMPTY })
  const days = ref(7)
  const status = ref('')
  let timer = null

  async function load() {
    loading.value = true
    try {
      const params = { days: days.value }
      if (status.value) params.status = status.value
      const res = await fetchRoleWorkbenchDashboard(roleKey, params)
      data.value = res && typeof res === 'object' ? res : { ...EMPTY }
    } catch (e) {
      ElMessage.error(e?.message || '加载工作台数据失败')
    } finally {
      loading.value = false
    }
  }

  function onFilterChange(f) {
    if (f?.days != null) days.value = f.days
    if (f?.status != null) status.value = f.status
    load()
  }

  onMounted(() => {
    load()
    timer = setInterval(load, 30000)
  })

  onUnmounted(() => {
    if (timer) clearInterval(timer)
  })

  return { loading, data, load, days, status, onFilterChange }
}
