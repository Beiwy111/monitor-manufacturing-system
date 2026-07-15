<template>
  <RoleWorkbenchScreen
    :data="data"
    :loading="loading"
    theme="operator"
    :filter-days="days"
    @refresh="load"
    @filter-change="onFilterChange"
  >
    <template #toolbar>
      <div v-if="data.attendance" class="op-toolbar">
        <div class="op-toolbar__att">
          <strong>今日考勤</strong>
          <span>上班 {{ data.attendance.checkInTime || '未打卡' }}</span>
          <span>下班 {{ data.attendance.checkOutTime || '未打卡' }}</span>
          <el-tag v-if="data.attendance.status" size="small" type="success">{{ data.attendance.status }}</el-tag>
        </div>
        <div class="op-toolbar__actions">
          <el-button size="small" type="primary" @click="$router.push('/production/my-dispatch')">我的派工</el-button>
          <el-button size="small" @click="$router.push('/production/report')">工序报工</el-button>
          <el-button size="small" type="danger" plain @click="showAlarm = true">触发安灯</el-button>
        </div>
      </div>
    </template>
  </RoleWorkbenchScreen>

  <el-dialog v-model="showAlarm" title="触发安灯报警" width="420px">
    <el-input v-model="alarmDesc" type="textarea" rows="3" placeholder="描述异常情况" />
    <template #footer>
      <el-button @click="showAlarm = false">取消</el-button>
      <el-button type="danger" :loading="acting" @click="submitAlarm">上报</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import RoleWorkbenchScreen from '@/components/workbench/RoleWorkbenchScreen.vue'
import { useRoleWorkbenchDashboard } from '@/composables/useRoleWorkbenchDashboard'
import { useMesStore } from '@/stores/mes'
import { useOperatorIdentity } from '@/composables/useOperatorIdentity'
import { pickCurrentDispatch } from '@/utils/operatorWorkshop'
import { DISPATCH_ACTIVE } from '@/mock/constants'
import { triggerAlarm } from '@/api/business'

const { loading, data, load, days, onFilterChange } = useRoleWorkbenchDashboard('operator')
const mes = useMesStore()
const { operatorUsername, loginUsername } = useOperatorIdentity()

const showAlarm = ref(false)
const alarmDesc = ref('')
const acting = ref(false)

async function submitAlarm() {
  if (acting.value) return
  await mes.hydrateForPage().catch(() => {})
  const dispatch = pickCurrentDispatch(
    mes.myDispatches(operatorUsername.value).filter((d) => DISPATCH_ACTIVE.includes(d.status))
  )
  if (!dispatch?.equipmentId) {
    ElMessage.warning('请先接收派工并关联设备后再触发安灯')
    return
  }
  if (!alarmDesc.value.trim()) {
    ElMessage.warning('请描述异常情况')
    return
  }
  acting.value = true
  try {
    await triggerAlarm({
      equipmentId: dispatch.equipmentId,
      alarmType: 'EQUIPMENT',
      alarmLevel: 'IMPORTANT',
      description: alarmDesc.value.trim(),
      operator: loginUsername.value
    })
    ElMessage.success('安灯已上报')
    showAlarm.value = false
    alarmDesc.value = ''
  } catch (e) {
    ElMessage.error(e?.message || '上报失败')
  } finally {
    acting.value = false
  }
}
</script>

<style scoped>
.op-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px;
  margin: 0 14px 10px;
  background: #fff;
  border: 1px solid #e8ece9;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(30, 50, 40, 0.04);
  flex-shrink: 0;
}

.op-toolbar__att {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #606266;
}

.op-toolbar__att strong {
  color: #303133;
  margin-right: 4px;
}

.op-toolbar__actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>
