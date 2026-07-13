<template>
  <div class="cp-page" v-loading="loading">
    <div class="cp-head">
      <h2 class="cp-title">我的反馈</h2>
      <el-button type="primary" size="small" @click="$router.push('/customer/feedback/submit')">提交反馈</el-button>
    </div>

    <el-table :data="feedbacks" border stripe size="small" style="width:100%" highlight-current-row @current-change="onSelect">
      <el-table-column prop="caseNo" label="案例号" width="130" fixed />
      <el-table-column prop="orderNo" label="关联订单" width="130" />
      <el-table-column prop="problemType" label="问题类型" width="100" />
      <el-table-column prop="batchNo" label="序列号/批次" width="130" />
      <el-table-column prop="caseStatus" label="状态" width="90" />
      <el-table-column prop="openedAt" label="提交时间" width="160">
        <template #default="{ row }">{{ formatTime(row.openedAt) }}</template>
      </el-table-column>
      <el-table-column prop="problemDescription" label="描述" min-width="180" show-overflow-tooltip />
    </el-table>

    <div v-if="selected" class="cp-detail">
      <div class="cp-detail__bar">处理进度：{{ selected.caseNo }}</div>
      <div class="cp-timeline">
        <div v-for="(s, i) in selected.progressSteps || []" :key="i" class="cp-timeline__item" :class="`is-${s.status}`">
          <div class="cp-timeline__dot" />
          <div class="cp-timeline__body">
            <div class="cp-timeline__name">{{ s.name }}</div>
            <div class="cp-timeline__detail">{{ s.detail || '—' }}</div>
            <div class="cp-timeline__time">{{ formatTime(s.time) }}</div>
          </div>
        </div>
      </div>
      <div v-if="selected.handleResult" class="cp-result">处理结果：{{ selected.handleResult }}</div>
      <div v-if="attachments.length" class="cp-attachments">
        <span>附件：</span>
        <a v-for="(url, i) in attachments" :key="i" :href="url" target="_blank" class="cp-attach-link">图片{{ i + 1 }}</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCustomerFeedbacks } from '@/api/customer'

const loading = ref(false)
const feedbacks = ref([])
const selected = ref(null)

const attachments = computed(() => {
  const raw = selected.value?.attachmentUrls
  if (!raw) return []
  return String(raw).split(',').filter(Boolean)
})

function formatTime(v) {
  if (!v) return ''
  return String(v).replace('T', ' ').slice(0, 16)
}

function onSelect(row) {
  selected.value = row
}

onMounted(async () => {
  loading.value = true
  try {
    feedbacks.value = await getCustomerFeedbacks() || []
    if (feedbacks.value.length) selected.value = feedbacks.value[0]
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.cp-page { padding: 12px 16px; font-weight: 400; }
.cp-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; border-bottom: 1px solid #e8e8e8; padding-bottom: 10px; }
.cp-title { margin: 0; font-size: 18px; font-weight: 500; }
.cp-detail { margin-top: 12px; border: 1px solid #e8e8e8; padding: 12px; background: #fff; }
.cp-detail__bar { font-size: 14px; margin-bottom: 10px; }
.cp-timeline { display: flex; gap: 0; overflow-x: auto; padding: 8px 0; }
.cp-timeline__item { flex: 1; min-width: 110px; position: relative; padding-left: 14px; }
.cp-timeline__dot { width: 10px; height: 10px; border-radius: 50%; background: #d9d9d9; position: absolute; left: 0; top: 4px; }
.cp-timeline__item.is-done .cp-timeline__dot { background: #52c41a; }
.cp-timeline__item.is-active .cp-timeline__dot { background: #1677ff; }
.cp-timeline__name { font-size: 13px; }
.cp-timeline__detail { font-size: 12px; color: #666; }
.cp-timeline__time { font-size: 11px; color: #999; }
.cp-result { margin-top: 10px; font-size: 13px; }
.cp-attachments { margin-top: 8px; font-size: 13px; }
.cp-attach-link { margin-right: 10px; color: #1677ff; }
</style>
