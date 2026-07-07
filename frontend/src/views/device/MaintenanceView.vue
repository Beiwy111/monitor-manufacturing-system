<template>
  <MesPageShell toolbar-title="维修处理" :detail-rows="rows">
    <template #table>
      <el-table :data="faultList" highlight-current-row @current-change="onRowClick">
        <el-table-column prop="name" label="设备" min-width="140" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column prop="downtimeHours" label="停机(h)" width="90" />
      </el-table>
    </template>
    <template #detail-extra>
      <el-input v-if="selected" v-model="note" type="textarea" rows="3" placeholder="维修记录" style="margin-top:12px" />
      <el-button v-if="selected" type="primary" style="margin-top:8px" @click="save">保存维修记录</el-button>
    </template>
  </MesPageShell>
</template>
<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useMesFilter, detailRows } from '@/composables/useMesPage'
import MesPageShell from '@/components/mes/MesPageShell.vue'
const mes = useMesStore()
const userStore = useUserStore()
const note = ref('')
const faultList = computed(() => mes.equipment.filter(e => e.status === '故障' || e.downtimeHours > 0))
const { selected, onRowClick } = useMesFilter(faultList, ['name'])
const rows = computed(() => detailRows(selected.value, [{ key: 'name', label: '设备' }, { key: 'status', label: '状态' }]))
function save() {
  mes.updateEquipment(selected.value.id, { status: '运行中', repairNote: note.value, downtimeHours: 0 }, userStore.displayName, userStore.roleKey)
  ElMessage.success('维修记录已保存')
  note.value = ''
}
</script>
