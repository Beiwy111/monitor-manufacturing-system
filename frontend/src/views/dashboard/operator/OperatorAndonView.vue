<template>
  <div class="operator-andon">
    <section class="andon-hero">
      <div>
        <span class="andon-hero__eyebrow">OPERATOR ANDON REPORT</span>
        <h2>安灯异常上报</h2>
        <p>发现设备、质量、物料、工艺或安全异常时立即上报，由设备人员接警处置。</p>
      </div>
      <el-button type="danger" size="large" @click="reportVisible = true">上报安灯</el-button>
    </section>

    <section class="andon-summary">
      <div><strong>{{ myAlarms.length }}</strong><span>我的上报</span></div>
      <div><strong>{{ statusCount('OPEN', '已上报', '待接收') }}</strong><span>等待接警</span></div>
      <div><strong>{{ statusCount('RECEIVED', 'PROCESSING', '已确认', '处理中') }}</strong><span>处理中</span></div>
      <div><strong>{{ statusCount('CLOSED', '已关闭') }}</strong><span>已完成</span></div>
    </section>

    <section class="andon-panel">
      <div class="andon-panel__head">
        <div><b>我的安灯记录</b><span>仅展示由当前账号上报的异常及设备人员处理进度</span></div>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
      <el-empty v-if="!loading && !myAlarms.length" description="暂未上报安灯异常" />
      <el-table v-else v-loading="loading" :data="myAlarms" border stripe>
        <el-table-column prop="alarmNo" label="报警号" width="150" />
        <el-table-column label="设备" min-width="170">
          <template #default="{ row }">{{ row.equipmentCode }} · {{ row.equipmentName || row.source }}</template>
        </el-table-column>
        <el-table-column prop="alarmTypeCn" label="类型" width="90" />
        <el-table-column prop="alarmDescription" label="异常描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="处理进度" width="130">
          <template #default="{ row }"><el-tag :type="statusType(row.alarmStatus || row.status)">{{ row.alarmStatusCn || row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="处理人" width="110">
          <template #default="{ row }">{{ row.receiverName || row.assigneeName || '待接警' }}</template>
        </el-table-column>
        <el-table-column prop="reportedAt" label="上报时间" width="165" />
        <el-table-column label="处理结果" min-width="160">
          <template #default="{ row }">{{ row.closeResult || row.remark || '-' }}</template>
        </el-table-column>
      </el-table>
    </section>

    <AlarmReportDialog v-model:visible="reportVisible" @reported="onReported" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { fetchAlarmViews } from '@/api/business'
import AlarmReportDialog from '@/views/device/AlarmReportDialog.vue'

const user = useUserStore()
const alarms = ref([])
const loading = ref(false)
const reportVisible = ref(false)

const identities = computed(() => [user.userInfo?.username, user.userInfo?.realName, user.displayName].filter(Boolean))
const myAlarms = computed(() => alarms.value.filter(item => identities.value.includes(item.reporterName || item.reporter)))

function statusCount(...statuses) {
  return myAlarms.value.filter(item => statuses.includes(item.alarmStatus || item.status)).length
}

function statusType(status) {
  return { OPEN: 'danger', RECEIVED: 'warning', PROCESSING: 'primary', CLOSED: 'success', 已上报: 'danger', 待接收: 'danger', 已确认: 'warning', 处理中: 'primary', 已关闭: 'success' }[status] || 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await fetchAlarmViews()
    alarms.value = Array.isArray(data) ? data : data?.data || []
  } catch (error) {
    ElMessage.error(error?.message || '安灯记录加载失败')
  } finally {
    loading.value = false
  }
}

async function onReported() {
  ElMessage.success('安灯已上报，设备人员将收到接警通知')
  await load()
}

onMounted(load)
</script>

<style scoped>
.operator-andon{padding:18px;background:#f4f7fb;min-height:100%}.andon-hero{display:flex;align-items:center;justify-content:space-between;padding:24px 28px;border-radius:14px;background:linear-gradient(125deg,#7f1d1d,#dc2626);color:#fff;box-shadow:0 12px 30px #991b1b2b}.andon-hero__eyebrow{font-size:11px;letter-spacing:1.5px;color:#fecaca}.andon-hero h2{margin:7px 0 5px;font-size:26px}.andon-hero p{margin:0;color:#fee2e2}.andon-summary{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin:14px 0}.andon-summary div{padding:18px;background:#fff;border:1px solid #e5eaf0;border-radius:12px;text-align:center}.andon-summary strong,.andon-summary span{display:block}.andon-summary strong{font-size:27px;color:#b91c1c}.andon-summary span{margin-top:5px;color:#77869a;font-size:12px}.andon-panel{background:#fff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden}.andon-panel__head{display:flex;align-items:center;justify-content:space-between;padding:15px 18px;border-bottom:1px solid #edf1f5}.andon-panel__head b,.andon-panel__head span{display:block}.andon-panel__head span{font-size:11px;color:#8a98a8;margin-top:4px}@media(max-width:900px){.andon-summary{grid-template-columns:repeat(2,1fr)}}
</style>
