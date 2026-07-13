/** 通用 MES 列表页生成 — 减少重复页面代码 */
import { computed } from 'vue'
import { useMesStore } from '@/stores/mes'
import { useMesFilter, detailRows } from '@/composables/useMesPage'

export function useMesListPage(dataKey, searchFields = ['id'], detailFields = []) {
  const mes = useMesStore()
  const source = computed(() => mes[dataKey] || [])
  const { keyword, statusFilter, selected, filtered, onRowClick } = useMesFilter(source, searchFields)
  const rows = computed(() => detailRows(selected.value, detailFields.length ? detailFields : searchFields.map(f => ({ key: f, label: f }))))
  return { mes, keyword, statusFilter, selected, filtered, onRowClick, rows }
}
