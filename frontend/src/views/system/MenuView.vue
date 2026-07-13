<template>
  <div class="ruoyi-page">
    <div class="ruoyi-toolbar" style="padding-top: 16px">
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增</el-button>
      <el-button type="success" :icon="Edit" :disabled="!selectedRow" @click="openDialog(selectedRow)">修改</el-button>
      <el-button type="danger" :icon="Delete" :disabled="!selectedRow" @click="removeOne(selectedRow)">删除</el-button>
      <div class="ruoyi-toolbar__right">
        <el-button circle :icon="Refresh" @click="reload" />
      </div>
    </div>

    <div class="ruoyi-table-wrap">
      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="menuId"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
        highlight-current-row
        @current-change="(row) => (selectedRow = row)"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="menuCode" label="菜单编码" min-width="160" />
        <el-table-column prop="apiPath" label="接口路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="businessTable" label="业务表" width="140" />
        <el-table-column prop="sortNo" label="排序" width="70" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">修改</el-button>
            <el-button link type="primary" @click="openDialog(null, row.menuId)">新增</el-button>
            <el-button link type="danger" @click="removeOne(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.menuId ? '修改菜单' : '添加菜单'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="parentTree"
            :props="{ value: 'menuId', label: 'menuName', children: 'children' }"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item label="菜单编码" prop="menuCode">
          <el-input v-model="form.menuCode" />
        </el-form-item>
        <el-form-item label="接口路径">
          <el-input v-model="form.apiPath" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
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
import { Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { fetchMenuList, insertMenu, updateMenu, deleteMenu } from '@/api/system'

const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const menuFlat = ref([])
const selectedRow = ref(null)

const treeData = computed(() => buildTree(menuFlat.value))
const parentTree = computed(() => [{ menuId: null, menuName: '主类目', children: treeData.value }])

const form = reactive({
  menuId: null,
  parentId: null,
  menuName: '',
  menuCode: '',
  apiPath: '',
  businessTable: '',
  menuLevel: 2,
  sortNo: 0,
  status: 1
})

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuCode: [{ required: true, message: '请输入菜单编码', trigger: 'blur' }]
}

onMounted(() => reload())

function buildTree(list, parentId = null) {
  return list
    .filter((m) => (m.parentId ?? null) === parentId)
    .sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0))
    .map((m) => ({ ...m, children: buildTree(list, m.menuId) }))
}

async function reload() {
  loading.value = true
  try {
    menuFlat.value = await fetchMenuList()
  } finally {
    loading.value = false
  }
}

function openDialog(row, parentId) {
  Object.assign(form, {
    menuId: null,
    parentId: parentId ?? row?.parentId ?? null,
    menuName: '',
    menuCode: '',
    apiPath: '',
    businessTable: '',
    menuLevel: parentId ? 2 : 1,
    sortNo: 0,
    status: 1,
    ...(row || {})
  })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  if (form.menuId) await updateMenu({ ...form })
  else await insertMenu({ ...form })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await reload()
}

async function removeOne(row) {
  if (!row) return
  await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」？`, '提示', { type: 'warning' })
  await deleteMenu(row.menuId)
  ElMessage.success('删除成功')
  await reload()
}
</script>
