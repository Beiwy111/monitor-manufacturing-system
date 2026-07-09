<template>
  <div class="ruoyi-page">
    <div class="ruoyi-split">
      <aside class="ruoyi-split__tree">
        <div class="ruoyi-split__tree-title">组织架构</div>
        <el-tree
          :data="deptTree"
          node-key="id"
          default-expand-all
          highlight-current
          :expand-on-click-node="false"
          @node-click="onDeptClick"
        />
      </aside>

      <div class="ruoyi-split__main">
        <div class="ruoyi-query">
          <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
            <el-form-item label="登录名称">
              <el-input v-model="query.username" placeholder="请输入登录名称" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="手机号码">
              <el-input v-model="query.phone" placeholder="请输入手机号码" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" placeholder="用户状态" clearable style="width: 120px">
                <el-option label="启用" value="启用" />
                <el-option label="禁用" value="禁用" />
                <el-option label="待分配角色" value="待分配角色" />
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
          <el-button type="warning" :icon="Download" @click="exportCsv">导出</el-button>
          <div class="ruoyi-toolbar__right">
            <el-tooltip content="刷新">
              <el-button circle :icon="Refresh" @click="reload" />
            </el-tooltip>
          </div>
        </div>

        <div class="ruoyi-table-wrap">
          <el-table
            v-loading="loading"
            :data="pageData"
            border
            stripe
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="50" align="center" />
            <el-table-column prop="id" label="用户编号" width="90" align="center" />
            <el-table-column prop="username" label="登录名称" width="120" show-overflow-tooltip />
            <el-table-column prop="realName" label="用户名称" width="100" />
            <el-table-column prop="roleName" label="角色" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.pendingRole" type="warning" size="small">待分配</el-tag>
                <span v-else>{{ row.roleName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="department" label="部门" min-width="120" show-overflow-tooltip />
            <el-table-column prop="phone" label="手机号码" width="130" />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.status === '启用'"
                  inline-prompt
                  active-text="启"
                  inactive-text="停"
                  @change="(val) => toggleStatus(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Edit" @click="openDialog(row)">修改</el-button>
                <el-button link type="danger" :icon="Delete" @click="removeOne(row)">删除</el-button>
                <el-dropdown trigger="click" @command="(cmd) => onMore(cmd, row)">
                  <el-button link type="primary">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="resetPwd">重置密码</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
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
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改用户' : '添加用户'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="登录名称" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="请输入登录名称" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="用户密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="用户名称" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入用户名称" />
        </el-form-item>
        <el-form-item label="归属部门" prop="department">
          <el-select v-model="form.department" filterable allow-create style="width: 100%">
            <el-option v-for="d in deptOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="角色" prop="roleKey">
          <el-select v-model="form.roleKey" style="width: 100%" placeholder="请选择角色">
            <el-option v-for="r in roleOptions" :key="r.roleKey" :label="r.roleName" :value="r.roleKey" />
          </el-select>
          <div v-if="form.pendingRole" class="form-tip">该用户尚未分配角色，请选择角色并启用账号</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="启用">正常</el-radio>
            <el-radio value="禁用">停用</el-radio>
          </el-radio-group>
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
import { Search, Refresh, Plus, Edit, Delete, Download, ArrowDown } from '@element-plus/icons-vue'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useRuoyiTable } from '@/composables/useRuoyiTable'
import { fetchUserList, fetchRoleList, deleteUser } from '@/api/system'
import { mapUserFromApi, mapRoleFromApi } from '@/utils/systemMappers'

const mes = useMesStore()
const userStore = useUserStore()
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const activeDept = ref('')
const users = ref([])
const roles = ref([])

const userList = computed(() => users.value.map((u) => ({
  ...u,
  department: u.department || '未分配'
})))

const roleOptions = computed(() =>
  roles.value.map((r) => ({ roleKey: r.roleKey, roleName: r.roleName }))
)

const deptOptions = computed(() => {
  const set = new Set(userList.value.map((u) => u.department).filter(Boolean))
  return [...set]
})

const deptTree = computed(() => [{
  id: 'all',
  label: '显示器制造公司',
  children: deptOptions.value.map((d) => ({ id: d, label: d }))
}])

const defaultQuery = { username: '', phone: '', status: '' }

const {
  query, pageNum, pageSize, selectedRows, selectedRow, filtered, pageData,
  handleSearch, handleReset, handleSelectionChange
} = useRuoyiTable(userList, {
  filterFn(list, q) {
    return list.filter((u) => {
      if (activeDept.value && activeDept.value !== 'all' && u.department !== activeDept.value) return false
      if (q.username && !u.username?.includes(q.username) && !u.realName?.includes(q.username)) return false
      if (q.phone && !u.phone?.includes(q.phone)) return false
      if (q.status === '待分配角色') {
        if (!u.pendingRole) return false
      } else if (q.status && u.status !== q.status) {
        return false
      }
      return true
    })
  }
})

const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  department: '',
  phone: '',
  email: '',
  roleKey: '',
  roleName: '',
  pendingRole: false,
  status: '启用'
})

const rules = {
  username: [{ required: true, message: '请输入登录名称', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入用户名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

onMounted(() => reload())

async function reload() {
  loading.value = true
  try {
    const [userRows, roleRows] = await Promise.all([fetchUserList(), fetchRoleList()])
    roles.value = (roleRows || []).map((r) => mapRoleFromApi(r, userRows || []))
    users.value = (userRows || []).map((u) => mapUserFromApi(u, roleRows || []))
    await mes.hydrateFromApi()
  } catch (e) {
    ElMessage.error(e?.message || '加载用户数据失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  handleReset({ ...defaultQuery })
  activeDept.value = ''
}

function onDeptClick(node) {
  activeDept.value = node.id
  handleSearch()
}

function openDialog(row) {
  Object.assign(form, {
    id: null,
    username: '',
    password: '',
    realName: '',
    department: deptOptions.value[0] || '信息部',
    phone: '',
    email: '',
    roleKey: 'operator',
    roleName: '',
    pendingRole: false,
    status: '启用',
    ...(row || {})
  })
  if (row?.pendingRole) {
    form.roleKey = ''
    form.status = '禁用'
  }
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  if (!form.id && !form.password) {
    ElMessage.warning('请设置初始密码')
    return
  }
  form.roleName = roleOptions.value.find((r) => r.roleKey === form.roleKey)?.roleName
  await mes.saveUser({ ...form }, userStore.displayName, userStore.roleKey)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await reload()
}

async function toggleStatus(row, enabled) {
  if ((row.status === '启用') === enabled) return
  await mes.toggleUserStatus(row.id, userStore.displayName, userStore.roleKey)
  ElMessage.success('状态已更新')
  await reload()
}

async function removeOne(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  await reload()
}

async function handleDelete() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 个用户？`, '提示', { type: 'warning' })
  for (const row of selectedRows.value) {
    await deleteUser(row.id)
  }
  ElMessage.success('删除成功')
  await reload()
}

async function onMore(cmd, row) {
  if (cmd !== 'resetPwd') return
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码', `重置密码 · ${row.username}`, {
      inputType: 'password',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValidator: (v) => (v && v.length >= 6 ? true : '密码至少 6 位')
    })
    await mes.resetUserPassword(row.id, value, userStore.displayName, userStore.roleKey)
    ElMessage.success(`用户 ${row.username} 密码已重置`)
  } catch {
    /* cancelled */
  }
}

function exportCsv() {
  const header = '用户编号,登录名称,用户名称,部门,手机,状态,创建时间\n'
  const rows = filtered.value.map((u) =>
    [u.id, u.username, u.realName, u.department, u.phone, u.status, u.createdAt].join(',')
  ).join('\n')
  const blob = new Blob(['\ufeff' + header + rows], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '用户列表.csv'
  a.click()
  ElMessage.success('导出成功')
}
</script>

<style scoped>
.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.4;
}
</style>
