<template>

  <MesPageShell toolbar-title="安灯报警" :status-options="ALARM_STATUS" :toolbar-actions="[{ label: '上报安灯', key: 'alarm', type: 'danger' }]" :detail-rows="rows" :logs="mes.operationLogs.slice(0,8)" @toolbar-action="showDialog=true">

    <template #table>

      <el-table :data="mes.alarms" border stripe highlight-current-row @current-change="onRowClick">

        <el-table-column prop="id" label="报警号" width="130" />

        <el-table-column prop="type" label="类型" width="90" />

        <el-table-column prop="source" label="来源" min-width="120" />

        <el-table-column prop="level" label="级别" width="70" />

        <el-table-column prop="reporterName" label="上报人" width="90" />

        <el-table-column prop="status" label="状态" width="90">

          <template #default="{ row }"><StatusBadge :status="row.status" /></template>

        </el-table-column>

      </el-table>

    </template>

    <template #detail-actions>

      <el-button v-if="selected?.status==='已上报'" size="small" :loading="acting" @click="handle('receive')">接收</el-button>

      <el-button v-if="selected?.status==='已接收'" size="small" :loading="acting" @click="handle('processing')">处理中</el-button>

      <el-button v-if="selected?.status!=='已关闭'" size="small" type="primary" :loading="acting" @click="handle('close')">关闭</el-button>

    </template>

  </MesPageShell>

  <el-dialog v-model="showDialog" title="上报安灯" width="420px">

    <el-input v-model="desc" type="textarea" rows="3" placeholder="描述异常" />

    <template #footer><el-button @click="showDialog=false">取消</el-button><el-button type="danger" :loading="acting" @click="report">上报</el-button></template>

  </el-dialog>

</template>



<script setup>

import { computed, ref, onMounted } from 'vue'

import { ElMessage } from 'element-plus'

import { useMesStore } from '@/stores/mes'

import { useUserStore } from '@/stores/user'

import { ALARM_STATUS } from '@/mock/constants'

import { useMesFilter, detailRows } from '@/composables/useMesPage'

import MesPageShell from '@/components/mes/MesPageShell.vue'

import StatusBadge from '@/components/mes/StatusBadge.vue'



const mes = useMesStore()

const userStore = useUserStore()

const showDialog = ref(false)

const desc = ref('')

const acting = ref(false)

const { selected, onRowClick } = useMesFilter(computed(() => mes.alarms), ['id'])

const rows = computed(() => detailRows(selected.value, [

  { key: 'type', label: '类型' }, { key: 'description', label: '描述' }, { key: 'assigneeName', label: '处理人' }, { key: 'status', label: '状态' }

]))



onMounted(async () => {

  try {

    await mes.hydrateFromApi()

  } catch {

    /* ignore */

  }

})



async function handle(action) {

  if (!selected.value || acting.value) return

  acting.value = true

  try {

    await mes.handleAlarm(selected.value.id, action, userStore.userInfo.username, userStore.roleKey, userStore.displayName)

    ElMessage.success('状态已更新')

  } catch (e) {

    ElMessage.error(e?.message || '操作失败')

  } finally {

    acting.value = false

  }

}



async function report() {

  if (acting.value) return

  acting.value = true

  try {

    await mes.createAlarm({

      type: '生产异常',

      source: '工位',

      description: desc.value,

      reporterName: userStore.displayName

    }, userStore.userInfo.username, userStore.roleKey)

    ElMessage.success('安灯已上报')

    showDialog.value = false

    desc.value = ''

  } catch (e) {

    ElMessage.error(e?.message || '上报失败')

  } finally {

    acting.value = false

  }

}

</script>


