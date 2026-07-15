<template>
  <div class="order-page" v-loading="loading">
    <!-- 顶部：产品信息 + 步骤 -->
    <header class="order-top">
      <div class="order-top__info">
        <p class="order-top__label">新建订单</p>
        <h1 class="order-top__name">{{ selectedProduct?.materialName || '请选择产品' }}</h1>
        <p class="order-top__meta">
          <span v-if="selectedProduct">型号 {{ selectedProduct.materialCode }}</span>
          <span v-if="selectedProduct">屏幕尺寸 {{ productSummary.size }}</span>
          <span v-if="selectedProduct">参考单价 {{ formatPrice(selectedProduct.standardCost) }} 元 / {{ selectedProduct.unit || '台' }}</span>
        </p>
      </div>
      <div class="order-top__steps">
        <div
          v-for="(s, i) in STEP_LABELS"
          :key="s"
          class="order-step"
          :class="{ 'order-step--active': step === i, 'order-step--done': step > i }"
        >
          <span class="order-step__num">{{ i + 1 }}</span>
          <span class="order-step__text">{{ s }}</span>
        </div>
      </div>
    </header>

    <div class="order-shell">
      <!-- 左侧：产品图（固定对应型号） -->
      <aside class="order-visual">
        <div class="order-visual__main">
          <img
            v-if="heroImage"
            :src="heroImage"
            :alt="selectedProduct?.materialName || '产品图'"
            class="order-visual__hero"
          />
          <div v-else class="order-visual__empty">暂无产品图</div>
        </div>
      </aside>

      <!-- 右侧 45%：配置 + 价格操作 -->
      <main class="order-config">
        <div class="order-config__body">
        <!-- Step 0：产品规格 -->
        <template v-if="step === 0">
          <section v-if="selectedProduct && specPresets.length" class="cfg-block">
            <h3 class="cfg-block__title">分辨率 / 规格配置</h3>
            <div class="opt-grid opt-grid--full opt-grid--2">
              <button
                v-for="item in specPresets"
                :key="item.label"
                type="button"
                class="opt-box opt-box--lg"
                :class="{ 'opt-box--active': form.specification === item.spec }"
                @click="applySpecPreset(item)"
              >
                <span class="opt-box__main">{{ item.label }}</span>
                <span class="opt-box__sub">{{ item.spec }}</span>
              </button>
            </div>
          </section>

          <section v-if="selectedProduct" class="cfg-block cfg-block--last">
            <h3 class="cfg-block__title">颜色</h3>
            <div class="opt-grid opt-grid--full opt-grid--colors">
              <button
                v-for="item in colorOptions"
                :key="item.value"
                type="button"
                class="opt-box opt-box--color"
                :class="{ 'opt-box--active': form.color === item.value }"
                @click="form.color = item.value"
              >
                <span class="color-swatch" :style="{ background: item.swatch }" />
                <span class="opt-box__main">{{ item.label }}</span>
              </button>
            </div>
          </section>
        </template>

        <!-- Step 1：数量交期 -->
        <template v-if="step === 1">
          <section class="cfg-block">
            <h3 class="cfg-block__title">订购数量</h3>
            <div class="opt-grid opt-grid--full opt-grid--3">
              <button
                v-for="qty in qtyPresets"
                :key="qty"
                type="button"
                class="opt-box opt-box--lg"
                :class="{ 'opt-box--active': form.quantity === qty }"
                @click="form.quantity = qty"
              >
                <span class="opt-box__main">{{ qty }} {{ selectedProduct?.unit || '台' }}</span>
              </button>
            </div>
            <div class="cfg-inline">
              <span class="cfg-inline__label">自定义数量</span>
              <el-input-number v-model="form.quantity" :min="1" :step="1" size="default" />
              <span class="cfg-inline__unit">{{ selectedProduct?.unit || '台' }}</span>
            </div>
          </section>

          <section class="cfg-block">
            <h3 class="cfg-block__title">要求交期</h3>
            <el-date-picker
              v-model="form.requiredDeliveryDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择交期日期"
              size="default"
              class="cfg-date"
            />
            <p class="cfg-hint">常规型号标准交期约 7–14 个工作日，大批量以工厂审核回复为准。</p>
          </section>
        </template>

        <!-- Step 2：收货信息 -->
        <template v-if="step === 2">
          <section class="cfg-block">
            <h3 class="cfg-block__title">收货联系人</h3>
            <div class="cfg-fields">
              <div class="cfg-field">
                <label>收货人</label>
                <el-input v-model="form.receiverName" placeholder="姓名" />
              </div>
              <div class="cfg-field">
                <label>联系电话</label>
                <el-input v-model="form.receiverPhone" placeholder="手机或固话" />
              </div>
            </div>
          </section>

          <section class="cfg-block">
            <h3 class="cfg-block__title">收货地址</h3>
            <el-input
              v-model="form.receiverAddress"
              type="textarea"
              :rows="3"
              placeholder="省市区 + 街道门牌号"
            />
          </section>

          <section class="cfg-block">
            <h3 class="cfg-block__title">订单备注</h3>
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="2"
              placeholder="特殊包装、送货时段等（选填）"
            />
          </section>
        </template>

        <!-- Step 3：附件 -->
        <template v-if="step === 3">
          <section class="cfg-block">
            <h3 class="cfg-block__title">合同与需求附件</h3>
            <el-upload
              :http-request="handleUpload"
              list-type="picture-card"
              :file-list="fileList"
              :on-remove="onRemove"
              accept="image/*,.pdf"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <p class="cfg-hint">支持 JPG、PNG、PDF，用于合同或需求说明（可选）。</p>
          </section>
        </template>

        <!-- Step 4：确认 -->
        <template v-if="step === 4">
          <section class="cfg-block">
            <h3 class="cfg-block__title">订单确认</h3>
            <dl class="confirm-dl">
              <div><dt>产品</dt><dd>{{ selectedProduct?.materialName || '—' }}</dd></div>
              <div><dt>型号</dt><dd>{{ selectedProduct?.materialCode || '—' }}</dd></div>
              <div><dt>规格</dt><dd>{{ orderSpecificationDisplay || '—' }}</dd></div>
              <div><dt>颜色</dt><dd>{{ form.color || '—' }}</dd></div>
              <div><dt>数量</dt><dd>{{ form.quantity }} {{ selectedProduct?.unit || '台' }}</dd></div>
              <div><dt>交期</dt><dd>{{ form.requiredDeliveryDate || '—' }}</dd></div>
              <div><dt>收货人</dt><dd>{{ form.receiverName || '—' }} {{ form.receiverPhone }}</dd></div>
              <div><dt>地址</dt><dd>{{ form.receiverAddress || '—' }}</dd></div>
              <div v-if="form.remark"><dt>备注</dt><dd>{{ form.remark }}</dd></div>
              <div><dt>附件</dt><dd>{{ attachmentUrls.length ? `${attachmentUrls.length} 个文件` : '无' }}</dd></div>
            </dl>
          </section>
        </template>
        </div>

        <div class="order-foot">
          <div class="order-foot__price">
            <div class="order-foot__price-main">
              <span class="order-foot__label">参考单价</span>
              <strong>{{ formatPrice(selectedProduct?.standardCost) }}</strong>
              <span class="order-foot__unit">元 / {{ selectedProduct?.unit || '台' }}</span>
            </div>
            <div v-if="step >= 1" class="order-foot__price-total">
              合计 <strong>{{ lineAmount }}</strong> 元
            </div>
          </div>
          <div class="order-actions">
            <el-button v-if="step > 0" size="large" @click="step--">上一步</el-button>
            <el-button size="large" @click="saveDraft">暂存</el-button>
            <el-button v-if="step < 4" type="primary" size="large" @click="nextStep">下一步</el-button>
            <el-button v-else type="primary" size="large" :loading="submitting" @click="submit">提交订单</el-button>
          </div>
          <p v-if="draftSavedHint" class="order-draft-hint">{{ draftSavedHint }}</p>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { createCustomerOrder, getCustomerProducts, getCustomerProfile, uploadCustomerFile } from '@/api/customer'
import { resolveCustomerProductImage } from '@/utils/productImage'

const DRAFT_KEY = 'customer-new-order-draft'
const RECENT_SPECS_KEY = 'customer-order-recent-specs'
const STEP_LABELS = ['产品规格', '数量交期', '收货信息', '附件', '确认提交']

const COLOR_OPTIONS = [
  { label: '深空黑', value: '深空黑', swatch: '#2b2b2b' },
  { label: '星耀银', value: '星耀银', swatch: '#b8bcc4' },
  { label: '云白色', value: '云白色', swatch: '#f0f0f0' },
  { label: '科技蓝', value: '科技蓝', swatch: '#3d5a80' }
]

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const step = ref(0)
const products = ref([])
const fileList = ref([])
const attachmentUrls = ref([])
const draftSavedHint = ref('')

const form = reactive({
  materialId: null,
  productName: '',
  specification: '',
  color: '',
  quantity: 1,
  requiredDeliveryDate: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  remark: ''
})

const selectedProduct = computed(() => products.value.find((p) => p.materialId === form.materialId))

const productSummary = computed(() => parseProductSummary(selectedProduct.value))

const specPresets = computed(() => buildSpecPresets(selectedProduct.value))

const colorOptions = COLOR_OPTIONS

const qtyPresets = computed(() => {
  const name = selectedProduct.value?.materialName || ''
  if (name.includes('15.6') || name.includes('21.5')) return [50, 100, 200]
  if (name.includes('23.8') || name.includes('24') || name.includes('电竞')) return [30, 50, 100]
  if (name.includes('27') || name.includes('32') || name.includes('34') || name.includes('4K')) return [20, 40, 80]
  return [10, 50, 100]
})

const lineAmount = computed(() => {
  const price = Number(selectedProduct.value?.standardCost || 0)
  const qty = Number(form.quantity || 0)
  return (price * qty).toFixed(2)
})

const heroImage = computed(() => {
  const p = selectedProduct.value
  return p ? resolveCustomerProductImage(p) : ''
})

const orderSpecificationDisplay = computed(() => buildOrderSpecification())

function buildOrderSpecification() {
  const parts = [form.specification].filter(Boolean)
  if (form.color) parts.push(`颜色：${form.color}`)
  return parts.join(' · ')
}

function formatPrice(val) {
  if (val == null || val === '') return '—'
  return Number(val).toFixed(2)
}

function parseProductSummary(product) {
  if (!product) return { size: '—', resolution: '—', ports: '—' }
  const text = `${product.materialName || ''} ${product.specification || ''}`
  const sizeMatch = text.match(/(\d+(?:\.\d+)?)\s*寸/)
  const resMatch = text.match(/(\d{3,4}\s*[x×]\s*\d{3,4})/i) || text.match(/(\dK)/i)
  const hzMatch = text.match(/(\d+)\s*Hz/i)
  let ports = product.ports || 'HDMI ×2 · DP ×1'
  if (!product.ports) {
    if (text.includes('商用') || text.includes('15.6') || text.includes('21.5')) ports = 'VGA ×1 · HDMI ×1'
    else if (text.includes('电竞') || text.includes('23.8') || text.includes('144')) ports = 'HDMI ×2 · DP ×1 · USB-C ×1'
    else if (text.includes('4K') || text.includes('27') || text.includes('OLED') || text.includes('34')) ports = 'HDMI ×2 · DP ×1 · USB-C ×1'
  }
  return {
    size: sizeMatch ? `${sizeMatch[1]} 英寸` : '—',
    resolution: resMatch ? resMatch[1].replace(/\s/g, '') + (hzMatch ? ` · ${hzMatch[1]}Hz` : '') : (product.specification || '—'),
    ports
  }
}

function buildSpecPresets(product) {
  if (!product) return []
  const name = product.materialName || ''
  if (name.includes('15.6') || name.includes('21.5')) {
    return [
      { label: '1920×1080 标准', spec: '1920x1080 商用款 · VGA+HDMI' },
      { label: '1920×1080 双HDMI', spec: '1920x1080 教育版 · HDMI ×2' }
    ]
  }
  if (name.includes('23.8') || name.includes('24') || name.includes('32') || name.includes('电竞') || name.includes('曲面')) {
    return [
      { label: '2K 144Hz', spec: '2560x1440 144Hz · HDMI+DP+USB-C' },
      { label: '2K 165Hz', spec: '2560x1440 165Hz · HDMI+DP' }
    ]
  }
  if (name.includes('27') || name.includes('4K') || name.includes('OLED') || name.includes('34')) {
    return [
      { label: '4K HDR', spec: '3840x2160 60Hz HDR · HDMI+DP' },
      { label: '超宽 3440×1440', spec: '3440x1440 100Hz · HDMI+DP+USB-C' }
    ]
  }
  return [{ label: '标准配置', spec: product.specification || '' }]
}

function saveRecentSpec(product, specification) {
  if (!product?.materialId || !specification) return
  try {
    const raw = JSON.parse(localStorage.getItem(RECENT_SPECS_KEY) || '[]')
    const entry = {
      key: `${product.materialId}-${specification}`,
      materialId: product.materialId,
      specification,
      label: `${product.materialName} · ${specification.slice(0, 20)}${specification.length > 20 ? '…' : ''}`
    }
    localStorage.setItem(
      RECENT_SPECS_KEY,
      JSON.stringify([entry, ...raw.filter((x) => x.key !== entry.key)].slice(0, 5))
    )
  } catch { /* ignore */ }
}

function onProductChange(id) {
  const p = products.value.find((x) => x.materialId === id)
  if (p) {
    form.productName = p.materialName
    const presets = buildSpecPresets(p)
    form.specification = presets[0]?.spec || p.specification
    if (!form.color) form.color = COLOR_OPTIONS[0].value
    saveRecentSpec(p, form.specification)
  }
}

function applySpecPreset(item) {
  form.specification = item.spec
  if (selectedProduct.value) saveRecentSpec(selectedProduct.value, item.spec)
}

function saveDraft() {
  localStorage.setItem(DRAFT_KEY, JSON.stringify({
    step: step.value,
    form: { ...form },
    attachmentUrls: [...attachmentUrls.value],
    fileList: fileList.value.map((f) => ({ name: f.name, url: f.url }))
  }))
  draftSavedHint.value = '已暂存 ' + new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  ElMessage.success('订单草稿已暂存')
}

function loadDraft() {
  try {
    const data = JSON.parse(localStorage.getItem(DRAFT_KEY) || 'null')
    if (!data) return
    if (data.form) Object.assign(form, data.form)
    if (typeof data.step === 'number') step.value = data.step
    if (Array.isArray(data.attachmentUrls)) attachmentUrls.value = data.attachmentUrls
    if (Array.isArray(data.fileList)) fileList.value = data.fileList
  } catch { /* ignore */ }
}

function nextStep() {
  if (step.value === 0) {
    if (!form.materialId) { ElMessage.warning('请选择产品'); return }
    if (!form.specification) { ElMessage.warning('请选择分辨率配置'); return }
    if (!form.color) { ElMessage.warning('请选择颜色'); return }
  }
  if (step.value === 1) {
    if (!form.quantity || form.quantity <= 0) { ElMessage.warning('请填写数量'); return }
    if (!form.requiredDeliveryDate) { ElMessage.warning('请选择交期'); return }
  }
  if (step.value === 2 && !form.receiverAddress?.trim()) { ElMessage.warning('请填写收货地址'); return }
  step.value++
}

async function handleUpload(opt) {
  try {
    const res = await uploadCustomerFile(opt.file)
    attachmentUrls.value.push(res.url)
    fileList.value.push({ name: opt.file.name, url: res.url })
    opt.onSuccess(res)
  } catch (e) { opt.onError(e) }
}

function onRemove(file) {
  attachmentUrls.value = attachmentUrls.value.filter((u) => u !== file.url)
  fileList.value = fileList.value.filter((f) => f.url !== file.url)
}

async function submit() {
  submitting.value = true
  try {
    const res = await createCustomerOrder({
      materialId: form.materialId,
      productName: form.productName,
      specification: buildOrderSpecification(),
      quantity: form.quantity,
      unit: selectedProduct.value?.unit,
      requiredDeliveryDate: form.requiredDeliveryDate,
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress,
      remark: form.remark,
      attachmentUrls: attachmentUrls.value
    })
    localStorage.removeItem(DRAFT_KEY)
    ElMessage.success(res?.message || '提交成功')
    router.push('/customer/orders')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [plist, profile] = await Promise.all([getCustomerProducts(), getCustomerProfile()])
    products.value = plist || []
    form.receiverName = profile?.realName || ''
    form.receiverPhone = profile?.phone || ''
    form.receiverAddress = profile?.shippingAddress || ''
    const qid = Number(route.query.materialId)
    if (qid) {
      form.materialId = qid
      onProductChange(qid)
    } else {
      loadDraft()
      if (!form.materialId && products.value.length) {
        form.materialId = products.value[0].materialId
        onProductChange(form.materialId)
      }
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.order-page {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 12px 16px 20px;
  background: #fff;
  font-weight: 400;
  box-sizing: border-box;
  min-height: var(--layout-content-min-h, calc(100vh - 92px));
}

/* ── 顶部 ── */
.order-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  padding-bottom: 16px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e8eaed;
}

.order-top__label {
  margin: 0 0 4px;
  font-size: 12px;
  color: #86909c;
  letter-spacing: 0.02em;
}

.order-top__name {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1.3;
}

.order-top__meta {
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 14px;
  color: #4e5969;
}

.order-top__steps {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.order-step {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  font-size: 12px;
  color: #86909c;
  border-bottom: 2px solid transparent;
}

.order-step--active {
  color: #1677ff;
  border-bottom-color: #1677ff;
  font-weight: 600;
}

.order-step--done {
  color: #4e5969;
}

.order-step__num {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1px solid currentColor;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}

.order-step--active .order-step__num {
  background: #1677ff;
  border-color: #1677ff;
  color: #fff;
}

/* ── 双栏主区域 ── */
.order-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.1fr);
  gap: 20px;
  align-items: start;
  width: 100%;
}

/* 左侧视觉区 */
.order-visual__main {
  aspect-ratio: 4 / 3;
  background: #f7f8fa;
  border: 1px solid #e8eaed;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.order-visual__hero {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 12px;
}

.order-visual__empty {
  font-size: 14px;
  color: #86909c;
}

/* 右侧配置区 */
.order-config {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.order-config__body {
  flex: 1;
}

.cfg-block {
  padding: 14px 0;
  border-bottom: 1px solid #eef0f3;
}

.cfg-block--last {
  border-bottom: none;
  padding-bottom: 0;
}

.cfg-block--muted {
  opacity: 0.92;
}

.cfg-block__title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}

.cfg-text {
  margin: 0;
  font-size: 14px;
  color: #4e5969;
  line-height: 1.5;
}

.cfg-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #86909c;
  line-height: 1.5;
}

/* 大号描边选择框 */
.opt-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.opt-grid--sizes,
.opt-grid--specs {
  display: grid;
  grid-template-columns: repeat(var(--size-cols, 2), minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.opt-grid--full {
  display: grid;
  gap: 10px;
  width: 100%;
}

.opt-grid--2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.opt-grid--3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.opt-grid--colors {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.opt-grid--full .opt-box {
  width: 100%;
  min-width: 0;
}

.opt-box {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-width: 100px;
  padding: 14px 16px;
  text-align: left;
  background: #fff;
  border: 2px solid #dcdfe3;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.opt-box:hover {
  border-color: #91caff;
}

.opt-box--active {
  border-color: #1677ff;
  background: #f0f7ff;
}

.opt-box--lg {
  padding: 16px 18px;
}

.opt-box--color {
  flex-direction: row;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
}

.color-swatch {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.12);
  flex-shrink: 0;
}

.opt-box__main {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1.35;
}

.opt-box__sub {
  font-size: 12px;
  color: #86909c;
  line-height: 1.4;
}

.cfg-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}

.cfg-inline__label,
.cfg-inline__unit {
  font-size: 13px;
  color: #646a73;
}

.cfg-date {
  width: 100%;
  max-width: 280px;
}

.cfg-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.cfg-field label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #646a73;
}

.confirm-dl {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.confirm-dl > div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 0;
  font-size: 13px;
  border-bottom: 1px solid #f0f1f3;
}

.confirm-dl dt {
  margin: 0;
  color: #86909c;
  flex-shrink: 0;
}

.confirm-dl dd {
  margin: 0;
  color: #1f2329;
  text-align: right;
}

/* 底部价格与操作 */
.order-foot {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #eef0f3;
}

.order-foot__price {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.order-foot__price-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.order-foot__label {
  font-size: 13px;
  color: #86909c;
}

.order-foot__price-main strong {
  font-size: 24px;
  font-weight: 700;
  color: #1f2329;
  line-height: 1;
}

.order-foot__unit {
  font-size: 13px;
  color: #4e5969;
}

.order-foot__price-total {
  font-size: 13px;
  color: #4e5969;
  white-space: nowrap;
}

.order-foot__price-total strong {
  font-size: 16px;
  font-weight: 600;
  color: #1677ff;
}

/* 操作按钮 */
.order-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.order-actions .el-button--primary {
  flex: 1;
  min-width: 120px;
}

.order-draft-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #86909c;
}

@media (max-width: 960px) {
  .order-shell {
    grid-template-columns: 1fr;
  }

  .order-top {
    flex-direction: column;
  }

  .order-top__steps {
    flex-wrap: wrap;
  }

  .opt-grid--sizes,
  .opt-grid--specs,
  .opt-grid--full,
  .opt-grid--colors {
    grid-template-columns: 1fr;
  }

  .cfg-fields {
    grid-template-columns: 1fr;
  }
}
</style>
