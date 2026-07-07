import { ref, computed } from 'vue'

export function useMesFilter(source, searchFields = []) {
  const keyword = ref('')
  const statusFilter = ref('')
  const selected = ref(null)

  const filtered = computed(() => {
    let list = source.value || source
    if (typeof list === 'function') list = list()
    if (!Array.isArray(list)) list = []
    if (statusFilter.value) {
      list = list.filter((row) => row.status === statusFilter.value)
    }
    if (keyword.value) {
      const kw = keyword.value.toLowerCase()
      list = list.filter((row) =>
        searchFields.some((f) => String(row[f] ?? '').toLowerCase().includes(kw))
      )
    }
    return list
  })

  function onRowClick(row) {
    selected.value = row
  }

  return { keyword, statusFilter, selected, filtered, onRowClick }
}

export function detailRows(row, fields) {
  if (!row) return []
  return fields.map(({ key, label, badge }) => ({
    label,
    value: row[key],
    badge: badge || (key === 'status')
  }))
}
