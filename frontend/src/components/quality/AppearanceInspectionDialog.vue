<template>
  <el-dialog v-model="visible" title="AI 外观检测" width="900px" :close-on-click-modal="false" @closed="reset">
    <el-alert title="辅助检测，不会自动修改质检结果" type="info" :closable="false" show-icon>
      当前模型用于电脑显示器屏幕表面划痕检测。检测结论需由质检员人工确认，关闭弹窗不会写入质检数据。
    </el-alert>

    <div class="vision-meta">
      <span>质检单：{{ inspection?.inspectionNo || '-' }}</span>
      <span>产品：{{ inspection?.materialName || '-' }}</span>
      <span>批次：{{ inspection?.batchNo || '-' }}</span>
    </div>

    <el-upload class="vision-upload" drag :auto-upload="false" :limit="1" accept="image/*"
      :on-change="onFileChange" :on-remove="resetFile">
      <div class="vision-upload__icon">+</div>
      <div>拖入或点击选择电脑显示器屏幕图片</div>
    </el-upload>

    <div v-if="previewUrl || result?.resultImage" class="vision-grid">
      <div class="vision-card"><b>原始图片</b><img v-if="previewUrl" :src="previewUrl" alt="原始质检图片" /></div>
      <div class="vision-card"><b>AI 分割结果</b><img v-if="result?.resultImage" :src="result.resultImage" alt="AI外观检测结果" /><el-empty v-else description="点击开始检测" /></div>
    </div>

    <div v-if="result" class="vision-result" :class="{ defect: result.defect }">
      <div><strong>{{ result.defect ? '发现划痕' : '未发现划痕' }}</strong><span>模型结论</span></div>
      <div><strong>{{ result.count }}</strong><span>缺陷区域</span></div>
      <div><strong>{{ confidenceText }}</strong><span>最高置信度</span></div>
      <div><strong>YOLOv8-Seg</strong><span>检测模型</span></div>
    </div>

    <el-table v-if="result?.detections?.length" :data="result.detections" border size="small">
      <el-table-column type="index" label="#" width="48" />
      <el-table-column label="缺陷类型" width="120"><template #default>屏幕划痕</template></el-table-column>
      <el-table-column label="置信度" width="100"><template #default="{ row }">{{ Math.round(row.confidence * 100) }}%</template></el-table-column>
      <el-table-column label="检测框"><template #default="{ row }">{{ row.box.join(', ') }}</template></el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" :disabled="!file" :loading="detecting" @click="detect">开始检测</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { detectAppearance } from '@/api/quality'

defineProps({ inspection: { type: Object, default: null } })
const visible = defineModel({ type: Boolean, default: false })
const file = ref(null)
const previewUrl = ref('')
const detecting = ref(false)
const result = ref(null)
const confidenceText = computed(() => result.value?.maxConfidence ? `${Math.round(result.value.maxConfidence * 100)}%` : '-')

function onFileChange(uploadFile) {
  resetFile()
  file.value = uploadFile.raw
  previewUrl.value = URL.createObjectURL(uploadFile.raw)
}

function resetFile() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  file.value = null
  previewUrl.value = ''
  result.value = null
}

async function detect() {
  if (!file.value) return
  detecting.value = true
  try {
    result.value = await detectAppearance(file.value)
    ElMessage.success(result.value.defect ? `检测到 ${result.value.count} 处划痕` : '未检测到划痕')
  } catch (error) {
    ElMessage.error(error?.message || 'AI 外观检测失败')
  } finally {
    detecting.value = false
  }
}

function reset() { resetFile() }
</script>

<style scoped>
.vision-meta{display:flex;gap:24px;margin:14px 0;color:#65758b;font-size:12px}.vision-upload{margin-bottom:14px}.vision-upload__icon{font-size:34px;line-height:1;color:#409eff}.vision-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin:14px 0}.vision-card{min-height:260px;border:1px solid #e3e9f0;border-radius:10px;padding:10px;background:#f8fafc}.vision-card b{display:block;margin-bottom:8px}.vision-card img{display:block;width:100%;height:240px;object-fit:contain;background:#111827;border-radius:6px}.vision-result{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin:12px 0;padding:12px;border-radius:10px;background:#ecfdf5}.vision-result.defect{background:#fff1f2}.vision-result div{text-align:center}.vision-result strong,.vision-result span{display:block}.vision-result strong{font-size:18px;color:#047857}.vision-result.defect strong{color:#be123c}.vision-result span{margin-top:4px;font-size:11px;color:#7b8798}@media(max-width:760px){.vision-grid{grid-template-columns:1fr}.vision-result{grid-template-columns:repeat(2,1fr)}}
</style>
