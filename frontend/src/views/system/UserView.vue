<template>
  <MesPageShell
    toolbar-title="用户管理"
    :toolbar-actions="[{ label: '新增用户', key: 'add', type: 'primary' }]"
    :detail-rows="rows"
    :logs="mes.operationLogs.slice(0,8)"
    @toolbar-action="openDrawer()"
  >
    <template #table>
      <el-table :data="filtered" highlight-current-row @current-change="onRowClick">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="roleName" label="角色" width="120" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="status" label="状态" width="80" />
      </el-table>
    </template>
    <template #detail-actions>
      <el-button size="small" @click="openDrawer(selected)">编辑</el-button>
      <el-button size="small" @click="toggle">{{ selected?.status === '启用' ? '禁用' : '启用' }}</el-button>
    </template>
  </MesPageShell>
  <el-drawer v-model="drawer" title="用户信息" size="360px">
    <el-form label-width="80px">
      <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.roleKey" style="width:100%">
          <el-option v-for="r in mes.sysRoles" :key="r.roleKey" :label="r.roleName" :value="r.roleKey" />
        </el-select>
      </el-form-item>
      <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
      <el-button type="primary" @click="save">保存</el-button>
    </el-form>
  </el-drawer>
</template>

<script setup>
import { computed, ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'

const mes = useMesStore()
const userStore = useUserStore()
const drawer = ref(false)
const form = reactive({ id: null, username: '', realName: '', roleKey: 'operator', roleName: '', phone: '', status: '启用' })
const { selected, filtered, onRowClick } = useMesFilter(computed(() => mes.sysUsers), ['username', 'realName'])
const rows = computed(() => detailRows(selected.value, [
  { key: 'username', label: '用户名' }, { key: 'realName', label: '姓名' }, { key: 'roleName', label: '角色' }, { key: 'status', label: '状态' }
]))
function openDrawer(row) {
  Object.assign(form, row || { id: null, username: '', realName: '', roleKey: 'operator', phone: '', status: '启用' })
  if (form.roleKey) form.roleName = mes.sysRoles.find(r => r.roleKey === form.roleKey)?.roleName
  drawer.value = true
}
function save() {
  form.roleName = mes.sysRoles.find(r => r.roleKey === form.roleKey)?.roleName
  mes.saveUser({ ...form }, userStore.displayName, userStore.roleKey)
  ElMessage.success('已保存')
  drawer.value = false
}
function toggle() {
  if (selected.value) mes.toggleUserStatus(selected.value.id, userStore.displayName, userStore.roleKey)
  ElMessage.success('状态已更新')
}
</script>
