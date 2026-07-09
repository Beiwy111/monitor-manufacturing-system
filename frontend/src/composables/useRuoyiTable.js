import { ref, computed } from 'vue'

export function useRuoyiTable(sourceList, options = {}) {
  const { pageSize: defaultPageSize = 10, filterFn } = options
  const query = ref({})
  const pageNum = ref(1)
  const pageSize = ref(defaultPageSize)
  const selectedRows = ref([])
  const selectedRow = ref(null)

  const filtered = computed(() => {
    let list = sourceList.value || []
    if (filterFn) list = filterFn(list, query.value)
    return list
  })

  const pageData = computed(() => {
    const start = (pageNum.value - 1) * pageSize.value
    return filtered.value.slice(start, start + pageSize.value)
  })

  function handleSearch() {
    pageNum.value = 1
  }

  function handleReset(resetQuery) {
    Object.assign(query.value, resetQuery)
    pageNum.value = 1
  }

  function handleSelectionChange(rows) {
    selectedRows.value = rows
    selectedRow.value = rows.length === 1 ? rows[0] : rows[0] || null
  }

  return {
    query,
    pageNum,
    pageSize,
    selectedRows,
    selectedRow,
    filtered,
    pageData,
    handleSearch,
    handleReset,
    handleSelectionChange
  }
}
