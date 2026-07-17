<template>
  <div class="ruoyi-page user-view">
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
        <div class="ruoyi-query user-view__query">
          <el-form :inline="true" :model="query" class="user-view__query-form" @submit.prevent="handleSearch">
            <div class="user-view__filter-row">
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
              <el-form-item class="user-view__filter-actions">
                <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
                <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
                <el-button type="primary" :icon="Plus" @click="openDialog()">新增</el-button>
                <el-button type="success" :icon="Edit" :disabled="!selectedRow" @click="openDialog(selectedRow)">修改</el-button>
                <el-button type="danger" :icon="Delete" :disabled="!selectedRows.length" @click="handleDelete">删除</el-button>
                <el-button type="warning" :icon="Download" @click="exportCsv">导出</el-button>
              </el-form-item>
            </div>
          </el-form>
          <el-tooltip content="刷新">
            <el-button circle :icon="Refresh" class="user-view__refresh" @click="reload" />
          </el-tooltip>
        </div>

        <div class="ruoyi-table-wrap user-view__table">
          <el-table
            v-loading="loading"
            :data="pageData"
            border
            stripe
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="50" align="center" />
            <el-table-column prop="id" label="用户编号" width="120" align="center" />
            <el-table-column prop="username" label="登录名称" width="120" show-overflow-tooltip />
            <el-table-column prop="realName" label="用户名称" width="110" show-overflow-tooltip />
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
            <el-table-column label="操作" width="150" align="center" fixed="right">
              <template #default="{ row }">
                <div class="user-view__row-actions">
                  <el-button link type="primary" :icon="Edit" @click="openDialog(row)">修改</el-button>
                  <el-button link type="danger" :icon="Delete" @click="removeOne(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="ruoyi-pagination">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="filtered.length"
            :page-sizes="[11, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            background
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" class="user-view__dialog" :title="form.id ? '修改用户' : '添加用户'" width="520px" destroy-on-close>
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
import { computed, reactive, ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Download } from '@element-plus/icons-vue'
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
  pageSize: 11,
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

onMounted(() => {
  document.documentElement.classList.add('user-view-active')
  reload()
})
onUnmounted(() => {
  document.documentElement.classList.remove('user-view-active')
})

async function reload() {
  loading.value = true
  try {
    const [userRows, roleRows] = await Promise.all([fetchUserList(), fetchRoleList()])
    roles.value = (roleRows || []).map((r) => mapRoleFromApi(r, userRows || []))
    users.value = (userRows || []).map((u) => mapUserFromApi(u, roleRows || []))
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
.user-view {
  --uv-text: #334155;
  --uv-title: #1f2937;
  --uv-muted: #475569;
  --uv-placeholder: #94a3b8;
  --uv-primary: #6ea9a4;
  --uv-primary-hover: #5c9792;
  --uv-primary-active: #4f8782;
  --uv-primary-disabled: #9fc5c2;
  --uv-secondary-bg: #dcebec;
  --uv-secondary-border: #b7ced0;
  --uv-secondary-text: #2f3b52;
  --uv-secondary-hover-bg: #c8dadc;
  --uv-secondary-hover-border: #a5bfc1;
  --uv-secondary-active-bg: #b8cfd2;
  --uv-secondary-active-border: #95b5b8;
  --uv-danger-bg: #d9b6be;
  --uv-danger-border: #c9a0aa;
  --uv-danger-hover: #cfa3ad;
  --uv-danger-active: #c4919c;
  --uv-danger-disabled: #e6ccd2;
  --uv-danger-text: #fff;
  --uv-link: #2d5a62;
  --uv-link-hover: #234a50;
  --uv-link-danger: #9a4f5d;
  --uv-link-danger-hover: #834252;
  --uv-disabled-opacity: 1;
  color: var(--uv-text);
}

/* —— 表单与筛选 —— */
.user-view :deep(.el-form-item__label) {
  color: var(--uv-title);
  font-weight: 600;
}

.user-view :deep(.el-input__wrapper),
.user-view :deep(.el-select__wrapper) {
  --el-input-text-color: var(--uv-text);
}

.user-view :deep(.el-input__inner::placeholder),
.user-view :deep(input::placeholder) {
  color: var(--uv-placeholder);
}

.user-view :deep(.el-select__placeholder) {
  color: var(--uv-placeholder);
}

/* —— 左侧组织树 —— */
.user-view :deep(.ruoyi-split__tree-title) {
  color: var(--uv-title);
  font-weight: 700;
}

.user-view :deep(.el-tree-node__label) {
  color: var(--uv-text);
  font-weight: 500;
}

.user-view :deep(.el-tree-node.is-current > .el-tree-node__content .el-tree-node__label) {
  color: var(--uv-title);
  font-weight: 600;
}

/* —— 表格 —— */
.user-view :deep(.el-table th.el-table__cell) {
  color: var(--uv-title);
  font-weight: 600;
}

.user-view :deep(.el-table th.el-table__cell .cell) {
  color: var(--uv-title);
  font-weight: 600;
}

.user-view :deep(.el-table td.el-table__cell) {
  color: var(--uv-text);
  font-weight: 500;
}

.user-view :deep(.el-table td.el-table__cell .cell) {
  color: var(--uv-text);
}

/* —— 分页 —— */
.user-view :deep(.ruoyi-pagination),
.user-view :deep(.el-pagination) {
  color: var(--uv-muted);
  font-weight: 500;
}

.user-view :deep(.el-pagination__total),
.user-view :deep(.el-pagination__jump) {
  color: var(--uv-muted);
}

.user-view :deep(.el-pager li) {
  color: var(--uv-text);
  font-weight: 500;
}

.user-view :deep(.el-pager li.is-active) {
  color: var(--uv-title);
  font-weight: 600;
}

/* —— 表格操作文字按钮 —— */
.user-view :deep(.el-button.is-link.el-button--primary) {
  color: var(--uv-link);
  font-weight: 600;
}

.user-view :deep(.el-button.is-link.el-button--primary:hover) {
  color: var(--uv-link-hover);
}

.user-view :deep(.el-button.is-link.el-button--danger) {
  color: var(--uv-link-danger);
  font-weight: 600;
}

.user-view :deep(.el-button.is-link.el-button--danger:hover) {
  color: var(--uv-link-danger-hover);
}

/* —— 弹窗标题与表单 —— */
.user-view__dialog {
  --uv-primary: #6ea9a4;
  --uv-primary-hover: #5c9792;
  --uv-primary-active: #4f8782;
  --uv-secondary-bg: #dcebec;
  --uv-secondary-border: #b7ced0;
  --uv-secondary-text: #2f3b52;
  --uv-text: #334155;
  --uv-title: #1f2937;
}

.user-view__dialog :deep(.el-dialog__title) {
  color: var(--uv-title);
  font-weight: 700;
}

.user-view__dialog :deep(.el-form-item__label) {
  color: var(--uv-title);
  font-weight: 600;
}

.user-view__query {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
}

.user-view__query-form {
  flex: 1;
  min-width: 0;
}

.user-view__filter-row {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}

.user-view__filter-row :deep(.el-form-item) {
  margin-bottom: 10px;
  flex-shrink: 0;
}

.user-view__filter-actions :deep(.el-form-item__label) {
  display: none;
}

.user-view__filter-actions :deep(.el-form-item__content) {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
}

.user-view__refresh {
  flex-shrink: 0;
  margin-top: 2px;
}

.user-view__table :deep(.el-table th.el-table__cell .cell) {
  white-space: nowrap;
}

.user-view__row-actions {
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}

.user-view__table :deep(.user-view__row-actions .el-button.is-link) {
  padding: 2px 4px;
}

.user-view__table :deep(.user-view__row-actions .el-button + .el-button) {
  margin-left: 0;
}

.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #b45309;
  line-height: 1.4;
  font-weight: 500;
}
</style>

<style>
/* 用户管理页激活时：提升面包屑、顶栏文字对比度（侧栏菜单保持全局原样） */
html.user-view-active #app .layout-breadcrumb,
html.user-view-active #app .layout-breadcrumb .ruoyi-breadcrumb__link {
  color: #475569 !important;
  font-weight: 500;
}

html.user-view-active #app .layout-breadcrumb .ruoyi-breadcrumb__link:hover {
  color: #334155 !important;
}

html.user-view-active #app .layout-breadcrumb .ruoyi-breadcrumb__current {
  color: #1f2937 !important;
  font-weight: 700;
}

html.user-view-active #app .layout-breadcrumb .ruoyi-breadcrumb__sep {
  color: #94a3b8 !important;
}

html.user-view-active #app .header-user__trigger {
  color: #334155 !important;
  font-weight: 500;
}

/* —— 用户管理页按钮：压过 theme.css 的 .ruoyi-page 全局规则 —— */
html.user-view-active #app .ruoyi-page.user-view .el-button--primary:not(.is-link),
html.user-view-active #app .ruoyi-page.user-view .el-button--warning:not(.is-link),
.user-view__dialog .el-button--primary:not(.is-link) {
  --el-button-bg-color: #6ea9a4 !important;
  --el-button-border-color: #6ea9a4 !important;
  --el-button-hover-bg-color: #5c9792 !important;
  --el-button-hover-border-color: #5c9792 !important;
  --el-button-active-bg-color: #4f8782 !important;
  --el-button-active-border-color: #4f8782 !important;
  --el-button-text-color: #fff !important;
  --el-button-hover-text-color: #fff !important;
  --el-button-active-text-color: #fff !important;
  --el-button-disabled-bg-color: #9fc5c2 !important;
  --el-button-disabled-border-color: #9fc5c2 !important;
  --el-button-disabled-text-color: rgba(255, 255, 255, 0.88) !important;
  background: #6ea9a4 !important;
  border-color: #6ea9a4 !important;
  color: #fff !important;
  font-weight: 600 !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button--primary:not(.is-link):hover,
html.user-view-active #app .ruoyi-page.user-view .el-button--primary:not(.is-link):focus-visible,
html.user-view-active #app .ruoyi-page.user-view .el-button--warning:not(.is-link):hover,
html.user-view-active #app .ruoyi-page.user-view .el-button--warning:not(.is-link):focus-visible,
.user-view__dialog .el-button--primary:not(.is-link):hover,
.user-view__dialog .el-button--primary:not(.is-link):focus-visible {
  background: #5c9792 !important;
  border-color: #5c9792 !important;
  color: #fff !important;
  filter: none !important;
  transform: none !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button--primary:not(.is-link):active,
html.user-view-active #app .ruoyi-page.user-view .el-button--warning:not(.is-link):active,
.user-view__dialog .el-button--primary:not(.is-link):active {
  background: #4f8782 !important;
  border-color: #4f8782 !important;
  color: #fff !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button--default:not(.is-link),
html.user-view-active #app .ruoyi-page.user-view .el-button--success:not(.is-link),
html.user-view-active #app .ruoyi-page.user-view .user-view__refresh,
.user-view__dialog .el-button--default:not(.is-link) {
  --el-button-bg-color: #dcebec !important;
  --el-button-border-color: #b7ced0 !important;
  --el-button-text-color: #2f3b52 !important;
  --el-button-hover-bg-color: #c8dadc !important;
  --el-button-hover-border-color: #a5bfc1 !important;
  --el-button-hover-text-color: #2f3b52 !important;
  --el-button-active-bg-color: #b8cfd2 !important;
  --el-button-active-border-color: #95b5b8 !important;
  --el-button-active-text-color: #2f3b52 !important;
  --el-button-disabled-bg-color: #e8f2f3 !important;
  --el-button-disabled-border-color: #c8d8da !important;
  --el-button-disabled-text-color: #94a3b8 !important;
  background: #dcebec !important;
  border-color: #b7ced0 !important;
  color: #2f3b52 !important;
  font-weight: 600 !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button--default:not(.is-link):hover,
html.user-view-active #app .ruoyi-page.user-view .el-button--default:not(.is-link):focus-visible,
html.user-view-active #app .ruoyi-page.user-view .el-button--success:not(.is-link):hover,
html.user-view-active #app .ruoyi-page.user-view .el-button--success:not(.is-link):focus-visible,
html.user-view-active #app .ruoyi-page.user-view .user-view__refresh:hover,
html.user-view-active #app .ruoyi-page.user-view .user-view__refresh:focus-visible,
.user-view__dialog .el-button--default:not(.is-link):hover,
.user-view__dialog .el-button--default:not(.is-link):focus-visible {
  background: #c8dadc !important;
  border-color: #a5bfc1 !important;
  color: #2f3b52 !important;
  filter: none !important;
  transform: none !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button--default:not(.is-link):active,
html.user-view-active #app .ruoyi-page.user-view .el-button--success:not(.is-link):active,
html.user-view-active #app .ruoyi-page.user-view .user-view__refresh:active,
.user-view__dialog .el-button--default:not(.is-link):active {
  background: #b8cfd2 !important;
  border-color: #95b5b8 !important;
  color: #2f3b52 !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button--danger:not(.is-link),
.user-view__dialog .el-button--danger:not(.is-link) {
  --el-button-bg-color: #d9b6be !important;
  --el-button-border-color: #c9a0aa !important;
  --el-button-text-color: #fff !important;
  --el-button-hover-bg-color: #cfa3ad !important;
  --el-button-hover-border-color: #bf8f9a !important;
  --el-button-hover-text-color: #fff !important;
  --el-button-active-bg-color: #c4919c !important;
  --el-button-active-border-color: #b37d89 !important;
  --el-button-active-text-color: #fff !important;
  --el-button-disabled-bg-color: #e6ccd2 !important;
  --el-button-disabled-border-color: #d9bcc3 !important;
  --el-button-disabled-text-color: rgba(255, 255, 255, 0.78) !important;
  background: #d9b6be !important;
  border-color: #c9a0aa !important;
  color: #fff !important;
  font-weight: 600 !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button--danger:not(.is-link):hover,
html.user-view-active #app .ruoyi-page.user-view .el-button--danger:not(.is-link):focus-visible,
.user-view__dialog .el-button--danger:not(.is-link):hover,
.user-view__dialog .el-button--danger:not(.is-link):focus-visible {
  background: #cfa3ad !important;
  border-color: #bf8f9a !important;
  color: #fff !important;
  filter: none !important;
  transform: none !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button--danger:not(.is-link):active,
.user-view__dialog .el-button--danger:not(.is-link):active {
  background: #c4919c !important;
  border-color: #b37d89 !important;
  color: #fff !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button--primary.is-disabled:not(.is-link),
html.user-view-active #app .ruoyi-page.user-view .el-button--warning.is-disabled:not(.is-link),
.user-view__dialog .el-button--primary.is-disabled:not(.is-link) {
  background: #9fc5c2 !important;
  border-color: #9fc5c2 !important;
  color: rgba(255, 255, 255, 0.88) !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button--default.is-disabled:not(.is-link),
html.user-view-active #app .ruoyi-page.user-view .el-button--success.is-disabled:not(.is-link),
html.user-view-active #app .ruoyi-page.user-view .user-view__refresh.is-disabled,
.user-view__dialog .el-button--default.is-disabled:not(.is-link) {
  background: #e8f2f3 !important;
  border-color: #c8d8da !important;
  color: #94a3b8 !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button--danger.is-disabled:not(.is-link),
.user-view__dialog .el-button--danger.is-disabled:not(.is-link) {
  background: #e6ccd2 !important;
  border-color: #d9bcc3 !important;
  color: rgba(255, 255, 255, 0.78) !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-button.is-disabled:not(.is-link),
html.user-view-active #app .ruoyi-page.user-view .el-button.is-disabled:not(.is-link):hover,
html.user-view-active #app .ruoyi-page.user-view .el-button.is-disabled:not(.is-link):active,
.user-view__dialog .el-button.is-disabled:not(.is-link),
.user-view__dialog .el-button.is-disabled:not(.is-link):hover,
.user-view__dialog .el-button.is-disabled:not(.is-link):active {
  opacity: 1 !important;
  cursor: not-allowed !important;
  box-shadow: none !important;
}

/* —— 表格勾选：白底绿勾，勾居中 —— */
html.user-view-active #app .ruoyi-page.user-view .el-checkbox.is-checked .el-checkbox__inner,
html.user-view-active #app .ruoyi-page.user-view .el-checkbox.is-indeterminate .el-checkbox__inner {
  background: #fff !important;
  border-color: #6ea9a4 !important;
  box-shadow: 0 0 0 1px rgba(110, 169, 164, 0.16) !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-checkbox.is-checked .el-checkbox__inner::after {
  border-color: #6ea9a4 !important;
  top: 50% !important;
  left: 50% !important;
  width: 4px !important;
  height: 8px !important;
  border-width: 0 2px 2px 0 !important;
  transform: translate(-50%, -58%) rotate(45deg) scaleY(1) !important;
}

html.user-view-active #app .ruoyi-page.user-view .el-checkbox.is-indeterminate .el-checkbox__inner::before {
  background-color: #6ea9a4 !important;
  top: 50% !important;
  left: 50% !important;
  right: auto !important;
  width: 8px !important;
  height: 2px !important;
  transform: translate(-50%, -50%) !important;
}
</style>
