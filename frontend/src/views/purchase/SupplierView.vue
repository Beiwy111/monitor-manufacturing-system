<template>
  <div class="supplier-page">
    <div class="supplier-header">
      <div class="supplier-header__left">
        <span class="page-title">供应商管理</span>
        <el-tag type="info" size="small" style="margin-left:10px">共 {{ total }} 家</el-tag>
      </div>
      <div class="supplier-header__right">
        <el-input v-model="keyword" placeholder="搜索供应商名称/联系人" clearable style="width:220px;margin-right:8px" @input="handleSearch" />
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width:110px;margin-right:8px" @change="handleSearch">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增供应商</el-button>
      </div>
    </div>

    <div class="supplier-table-wrap" v-loading="loading">
      <el-table :data="pageData" border stripe highlight-current-row>
        <el-table-column prop="supplierNo" label="编号" width="120" />
        <el-table-column prop="supplierName" label="供应商名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="supplyMaterials" label="供应物料" min-width="160" show-overflow-tooltip />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="contactEmail" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" size="small" @click="openMaterials(row)">供货物料</el-button>
            <el-popconfirm title="确定删除该供应商？" @confirm="doDelete(row)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && filtered.length === 0" class="supplier-empty">暂无供应商数据</div>
    </div>

    <div v-if="total > pageSize" class="supplier-pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- 供货物料管理弹窗 -->
    <el-dialog v-model="matDialogVisible" :title="`供货物料 — ${currentSupplier?.supplierName}`" width="640px" @open="loadMaterials">
      <div class="mat-dialog-tip">
        勾选该供应商负责供货的物料，保存后缺料计算将自动关联供应商。
      </div>
      <el-table v-loading="matLoading" :data="allMaterials" border stripe height="360px">
        <el-table-column width="50" align="center">
          <template #header>
            <el-checkbox
              :model-value="isAllChecked"
              :indeterminate="isIndeterminate"
              @change="toggleAll"
            />
          </template>
          <template #default="{ row }">
            <el-checkbox
              :model-value="selectedMatIds.has(row.materialId)"
              @change="(v) => toggleMat(row.materialId, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="materialCode" label="物料编码" width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="150" />
        <el-table-column prop="materialType" label="类型" width="100" />
        <el-table-column prop="specification" label="规格" min-width="120" show-overflow-tooltip />
        <el-table-column label="当前供应商" width="130">
          <template #default="{ row }">
            <span v-if="row.supplierId === currentSupplier?.supplierId" style="color:#67c23a;font-weight:600">本供应商</span>
            <span v-else-if="row.supplierId" style="color:#909399;font-size:12px">其他供应商</span>
            <span v-else style="color:#bfbfbf;font-size:12px">未分配</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span style="flex:1;color:#606266;font-size:13px">已选 {{ selectedMatIds.size }} 种物料</span>
        <el-button @click="matDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="matSaving" @click="saveMaterials">保存绑定</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑供应商' : '新增供应商'" width="560px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="供应商名称" prop="supplierName">
          <el-input v-model="form.supplierName" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="供应物料" prop="supplyMaterials">
          <el-input v-model="form.supplyMaterials" placeholder="如：LCD面板、背光板、驱动芯片" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="联系邮箱" prop="contactEmail">
          <el-input v-model="form.contactEmail" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { fetchSupplierList, addSupplier, updateSupplier, deleteSupplier } from '@/api/business'
import request from '@/utils/request'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const keyword = ref('')
const filterStatus = ref('')
const pageNum = ref(1)
const pageSize = ref(10)

const filtered = computed(() => {
  let data = list.value
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    data = data.filter(s =>
      (s.supplierName || '').toLowerCase().includes(kw) ||
      (s.contactPerson || '').toLowerCase().includes(kw) ||
      (s.supplierNo || '').toLowerCase().includes(kw)
    )
  }
  if (filterStatus.value) {
    data = data.filter(s => s.status === filterStatus.value)
  }
  return data
})

const total = computed(() => filtered.value.length)
const pageData = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

async function loadList() {
  loading.value = true
  try {
    list.value = await fetchSupplierList() || []
  } catch {
    ElMessage.error('加载供应商列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
}

// 表单
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({
  supplierId: null,
  supplierName: '',
  supplyMaterials: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  address: '',
  status: 'ACTIVE',
  remark: ''
})

const rules = {
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }]
}

function openAdd() {
  isEdit.value = false
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = {
    supplierId: row.supplierId,
    supplierName: row.supplierName,
    supplyMaterials: row.supplyMaterials || '',
    contactPerson: row.contactPerson || '',
    contactPhone: row.contactPhone || '',
    contactEmail: row.contactEmail || '',
    address: row.address || '',
    status: row.status || 'ACTIVE',
    remark: row.remark || ''
  }
  dialogVisible.value = true
}

function resetForm() {
  form.value = {
    supplierId: null, supplierName: '', supplyMaterials: '',
    contactPerson: '', contactPhone: '', contactEmail: '',
    address: '', status: 'ACTIVE', remark: ''
  }
  formRef.value?.resetFields()
}

async function doSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateSupplier(form.value)
      ElMessage.success('更新成功')
    } else {
      await addSupplier(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function doDelete(row) {
  try {
    await deleteSupplier(row.supplierId)
    ElMessage.success('删除成功')
    await loadList()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

// ---- 供货物料管理 ----
const matDialogVisible = ref(false)
const matLoading = ref(false)
const matSaving = ref(false)
const currentSupplier = ref(null)
const allMaterials = ref([])
const selectedMatIds = ref(new Set())

const isAllChecked = computed(() =>
  allMaterials.value.length > 0 && selectedMatIds.value.size === allMaterials.value.length
)
const isIndeterminate = computed(() =>
  selectedMatIds.value.size > 0 && selectedMatIds.value.size < allMaterials.value.length
)

function toggleMat(id, checked) {
  const s = new Set(selectedMatIds.value)
  checked ? s.add(id) : s.delete(id)
  selectedMatIds.value = s
}

function toggleAll(checked) {
  selectedMatIds.value = checked
    ? new Set(allMaterials.value.map(m => m.materialId))
    : new Set()
}

async function openMaterials(row) {
  currentSupplier.value = row
  matDialogVisible.value = true
}

async function loadMaterials() {
  matLoading.value = true
  try {
    const res = await request.get('/material/material/list')
    allMaterials.value = (res || []).filter(m => m.status === 1)
    // pre-select materials already bound to this supplier
    selectedMatIds.value = new Set(
      allMaterials.value
        .filter(m => m.supplierId === currentSupplier.value?.supplierId)
        .map(m => m.materialId)
    )
  } catch {
    ElMessage.error('加载物料列表失败')
  } finally {
    matLoading.value = false
  }
}

async function saveMaterials() {
  matSaving.value = true
  try {
    const sid = currentSupplier.value.supplierId
    // For all materials: bind selected ones, unbind previously-bound-but-now-deselected ones
    const tasks = allMaterials.value
      .filter(m => m.supplierId === sid || selectedMatIds.value.has(m.materialId))
      .map(m => {
        const newSupplierId = selectedMatIds.value.has(m.materialId) ? sid : null
        if (newSupplierId === m.supplierId) return null
        const url = newSupplierId
          ? `/material/material/bindSupplier?materialId=${m.materialId}&supplierId=${newSupplierId}`
          : `/material/material/bindSupplier?materialId=${m.materialId}`
        return request.post(url)
      })
      .filter(Boolean)

    await Promise.all(tasks)
    ElMessage.success(`已绑定 ${selectedMatIds.value.size} 种物料`)
    matDialogVisible.value = false
    await loadList()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    matSaving.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.supplier-page {
  padding: 16px 20px;
  background: #f5f7fa;
  min-height: 100%;
}
.supplier-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 10px;
}
.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #001b3f;
}
.supplier-table-wrap {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  min-height: 200px;
}
.supplier-empty {
  text-align: center;
  color: #909399;
  padding: 60px 0;
  font-size: 14px;
}
.supplier-pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
.mat-dialog-tip {
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f0f9eb;
  border-radius: 4px;
}
</style>
