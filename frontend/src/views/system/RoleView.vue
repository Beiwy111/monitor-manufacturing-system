<template>
  <div class="ruoyi-page">
    <div class="ruoyi-query">
      <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="请输入角色名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="权限字符">
          <el-input v-model="query.roleKey" placeholder="请输入权限字符" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 120px">
            <el-option label="启用" value="启用" />
            <el-option label="禁用" value="禁用" />
          </el-select>
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
      <div class="ruoyi-toolbar__right">
        <el-button circle :icon="Refresh" @click="reload" />
      </div>
    </div>

    <div class="ruoyi-table-wrap">
      <el-table v-loading="loading" :data="pageData" border stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="id" label="角色编号" width="90" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleKey" label="权限字符" min-width="120" />
        <el-table-column prop="userCount" label="用户数" width="90" align="center" />
        <el-table-column prop="permCount" label="权限数" width="90" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '启用' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
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
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改角色' : '添加角色'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="如 planner、manager" />
        </el-form-item>
        <el-form-item label="角色顺序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="启用">正常</el-radio>
            <el-radio value="禁用">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
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
import { useMesStore } from '@/stores/mes'
import { useRuoyiTable } from '@/composables/useRuoyiTable'
import { fetchRoleList, fetchUserList, insertRole, updateRole, deleteRole } from '@/api/system'
import { mapRoleFromApi } from '@/utils/systemMappers'

const mes = useMesStore()
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const roles = ref([])

const roleList = computed(() => roles.value)

const defaultQuery = { roleName: '', roleKey: '', status: '' }
const { query, pageNum, pageSize, selectedRows, selectedRow, filtered, pageData, handleSearch, handleReset, handleSelectionChange } =
  useRuoyiTable(roleList, {
    filterFn(list, q) {
      return list.filter((r) => {
        if (q.roleName && !r.roleName?.includes(q.roleName)) return false
        if (q.roleKey && !r.roleKey?.includes(q.roleKey)) return false
        if (q.status && r.status !== q.status) return false
        return true
      })
    }
  })

const form = reactive({ id: null, roleName: '', roleKey: '', sort: 0, status: '启用', remark: '' })
const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }]
}

onMounted(() => reload())

async function reload() {
  loading.value = true
  try {
    const [roleRows, userRows] = await Promise.all([fetchRoleList(), fetchUserList()])
    roles.value = (roleRows || []).map((r) => mapRoleFromApi(r, userRows || []))
    await mes.hydrateFromApi()
  } catch (e) {
    ElMessage.error(e?.message || '加载角色数据失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  handleReset({ ...defaultQuery })
}

function openDialog(row) {
  Object.assign(form, { id: null, roleName: '', roleKey: '', sort: 0, status: '启用', remark: '', ...(row || {}) })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  const payload = {
    roleId: form.id,
    roleName: form.roleName,
    roleCode: form.roleKey.toUpperCase(),
    roleDescription: form.remark,
    status: form.status === '启用' ? 1 : 0
  }
  if (form.id) await updateRole(payload)
  else await insertRole(payload)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await reload()
}

async function removeOne(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  await reload()
}

async function handleDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 个角色？`, '提示', { type: 'warning' })
  for (const row of selectedRows.value) await deleteRole(row.id)
  ElMessage.success('删除成功')
  await reload()
}
</script>
