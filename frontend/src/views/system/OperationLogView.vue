<template>
  <div class="ruoyi-page">
    <div class="ruoyi-query">
      <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
        <el-form-item label="操作人员">
          <el-input v-model="query.operator" placeholder="请输入操作人员" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="请输入模块" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="ruoyi-toolbar">
      <el-button type="danger" :icon="Delete" :disabled="!selectedRows.length" @click="handleDelete">删除</el-button>
      <el-button type="danger" plain @click="clearAll">清空</el-button>
      <div class="ruoyi-toolbar__right">
        <el-button circle :icon="Refresh" @click="reload" />
      </div>
    </div>

    <div class="ruoyi-table-wrap">
      <el-table v-loading="loading" :data="pageData" border stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="id" label="日志编号" width="90" align="center" />
        <el-table-column prop="module" label="系统模块" width="120" />
        <el-table-column prop="action" label="操作类型" width="120" />
        <el-table-column prop="operator" label="操作人员" width="110" />
        <el-table-column prop="target" label="操作对象" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="操作时间" min-width="170" />
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeOne(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="ruoyi-pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="filtered.length"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Delete } from '@element-plus/icons-vue'
import { useMesStore } from '@/stores/mes'
import { useRuoyiTable } from '@/composables/useRuoyiTable'
import { deleteOperationLog, fetchOperationLogList, fetchUserList } from '@/api/system'
import { mapOperationLogFromApi } from '@/utils/systemMappers'

const mes = useMesStore()
const loading = ref(false)
const logs = ref([])

const logList = computed(() => logs.value)
const defaultQuery = { operator: '', module: '', dateRange: null }

const { query, pageNum, pageSize, selectedRows, filtered, pageData, handleSearch, handleReset, handleSelectionChange } =
  useRuoyiTable(logList, {
    filterFn(list, q) {
      return list.filter((l) => {
        if (q.operator && !l.operator?.includes(q.operator)) return false
        if (q.module && !l.module?.includes(q.module)) return false
        if (q.dateRange?.length === 2) {
          const day = (l.createdAt || '').slice(0, 10)
          if (day < q.dateRange[0] || day > q.dateRange[1]) return false
        }
        return true
      })
    }
  })

onMounted(() => reload())

async function reload() {
  loading.value = true
  try {
    const [logRows, userRows] = await Promise.all([fetchOperationLogList(), fetchUserList()])
    logs.value = (logRows || []).map((l) => mapOperationLogFromApi(l, userRows || []))
    await mes.hydrateFromApi()
  } catch (e) {
    ElMessage.error(e?.message || '加载操作日志失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  handleReset({ ...defaultQuery })
}

async function removeOne(row) {
  await ElMessageBox.confirm('确认删除该日志？', '提示', { type: 'warning' })
  try {
    await deleteOperationLog(row.id)
  } catch {
    /* mes 运行时日志可能无 DB id */
  }
  ElMessage.success('删除成功')
  await reload()
}

async function handleDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 条日志？`, '提示', { type: 'warning' })
  for (const row of selectedRows.value) {
    try { await deleteOperationLog(row.id) } catch { /* ignore */ }
  }
  ElMessage.success('删除成功')
  await reload()
}

async function clearAll() {
  await ElMessageBox.confirm('确认清空所有操作日志？此操作不可恢复', '警告', { type: 'warning' })
  loading.value = true
  try {
    for (const row of [...logs.value]) {
      await deleteOperationLog(row.id)
    }
    ElMessage.success('日志已清空')
    await reload()
  } catch (e) {
    ElMessage.error(e?.message || '清空失败')
  } finally {
    loading.value = false
  }
}
</script>
