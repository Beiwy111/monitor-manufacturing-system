<template>
  <div class="ruoyi-page">
    <div class="ruoyi-query">
      <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
        <el-form-item label="权限名称">
          <el-input v-model="query.name" placeholder="请输入权限名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="权限字符">
          <el-input v-model="query.code" placeholder="请输入权限字符" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="ruoyi-toolbar">
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增</el-button>
      <el-button type="success" :icon="Edit" :disabled="!selectedRow" @click="openDialog(selectedRow)">修改</el-button>
      <el-button type="danger" :icon="Delete" :disabled="!selectedRows.length" @click="handleDelete">删除</el-button>
    </div>

    <div class="ruoyi-table-wrap">
      <el-table v-loading="loading" :data="pageData" border stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="permissionId" label="编号" width="80" align="center" />
        <el-table-column prop="permissionCode" label="权限字符" min-width="160" />
        <el-table-column prop="permissionName" label="权限名称" min-width="140" />
        <el-table-column prop="resourceType" label="类型" width="90" />
        <el-table-column prop="resourcePath" label="资源路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">修改</el-button>
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
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <el-dialog v-model="dialogVisible" title="权限信息" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="权限名称" prop="permissionName">
          <el-input v-model="form.permissionName" />
        </el-form-item>
        <el-form-item label="权限字符" prop="permissionCode">
          <el-input v-model="form.permissionCode" />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="form.resourceType" style="width: 100%">
            <el-option label="菜单" value="MENU" />
            <el-option label="按钮" value="BUTTON" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源路径">
          <el-input v-model="form.resourcePath" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { useRuoyiTable } from '@/composables/useRuoyiTable'
import { fetchPermissionList, insertPermission, updatePermission, deletePermission } from '@/api/system'

const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const permList = ref([])

const listRef = computed(() => permList.value)
const defaultQuery = { name: '', code: '' }
const { query, pageNum, pageSize, selectedRows, selectedRow, filtered, pageData, handleSearch, handleReset, handleSelectionChange } =
  useRuoyiTable(listRef, {
    filterFn(list, q) {
      return list.filter((p) => {
        if (q.name && !p.permissionName?.includes(q.name)) return false
        if (q.code && !p.permissionCode?.includes(q.code)) return false
        return true
      })
    }
  })

const form = reactive({
  permissionId: null,
  permissionName: '',
  permissionCode: '',
  resourceType: 'MENU',
  resourcePath: '',
  status: 1,
  roleId: 1
})

const rules = {
  permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  permissionCode: [{ required: true, message: '请输入权限字符', trigger: 'blur' }]
}

onMounted(() => reload())

async function reload() {
  loading.value = true
  try {
    permList.value = await fetchPermissionList()
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  handleReset({ ...defaultQuery })
}

function openDialog(row) {
  Object.assign(form, {
    permissionId: null,
    permissionName: '',
    permissionCode: '',
    resourceType: 'MENU',
    resourcePath: '',
    status: 1,
    roleId: 1,
    ...(row || {})
  })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  if (form.permissionId) await updatePermission({ ...form })
  else await insertPermission({ ...form })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await reload()
}

async function removeOne(row) {
  await ElMessageBox.confirm(`确认删除权限「${row.permissionName}」？`, '提示', { type: 'warning' })
  await deletePermission(row.permissionId)
  ElMessage.success('删除成功')
  await reload()
}

async function handleDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 条权限？`, '提示', { type: 'warning' })
  for (const row of selectedRows.value) await deletePermission(row.permissionId)
  ElMessage.success('删除成功')
  await reload()
}
</script>
