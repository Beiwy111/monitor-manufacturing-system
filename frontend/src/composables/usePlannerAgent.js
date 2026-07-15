import { computed, reactive, ref, unref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMesStore } from '@/stores/mes'
import { useUserStore } from '@/stores/user'
import { useSchedulingFlow } from '@/composables/useSchedulingFlow'
import { postComparePlanSchemes, postSaveBatchProductionPlans } from '@/api/planner'

export const PLANNER_BATCH_SIZE = 500

export function usePlannerAgent(options = {}) {
  const { combinedBatch = false, onSuccess, onClose } = options
  const combinedBatchFlag = combinedBatch

  const mes = useMesStore()
  const userStore = useUserStore()
  const previewLoading = ref(false)
  const schemeLoading = ref(false)
  const submitLoading = ref(false)
  const analysis = ref(null)
  const schemeData = ref(null)
  const selectedScheme = ref(null)
  const phase = ref('setup')

  const {
    activeStep,
    activeStepKey,
    selectedStepKey,
    thoughtStream,
    evidenceList,
    allEvidence,
    currentDetail,
    reset: resetFlow,
    runAnimatedPreview,
    selectStep
  } = useSchedulingFlow()

  const form = reactive({
    orderIds: [],
    planStart: new Date().toISOString().slice(0, 10),
    planEnd: new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10),
    plannedQty: 0
  })

  const pendingOrders = computed(() => mes.pendingPlanOrders)

  const phaseIndex = computed(() => {
    if (phase.value === 'setup') return 0
    if (phase.value === 'analyzing' || phase.value === 'review') return 1
    return 2
  })

  const selectedOrders = computed(() =>
    form.orderIds
      .map((id) => pendingOrders.value.find((o) => o.id === id))
      .filter(Boolean)
  )

  const totalQty = computed(() =>
    selectedOrders.value.reduce((sum, o) => sum + (Number(o.quantity) || 0), 0)
  )

  const isCombinedBatch = computed(() =>
    unref(combinedBatchFlag) && selectedOrders.value.length > 1
      && new Set(selectedOrders.value.map((o) => o.productModel)).size === 1
  )

  const combinedBatchCount = computed(() => {
    if (!isCombinedBatch.value) return 0
    return Math.ceil(totalQty.value / PLANNER_BATCH_SIZE)
  })

  const resultMetrics = computed(() => {
    const s = selectedScheme.value
    if (!s) return null
    const delay = Number(s.delayDays) || 0
    return {
      onTimeRate: delay === 0 ? 100 : Math.max(0, 100 - delay * 12),
      equipmentUtilization: s.equipmentUtilization ?? '—',
      lineChanges: s.lineChanges ?? 0,
      materialShortage: s.materialShortage ?? 0,
      conflictCount: (s.conflicts || []).length
    }
  })

  const schemeRecommend = computed(() => {
    const c = schemeData.value?.conclusion
    if (!c) return null
    let summary = c.summary || ''
    if (c.label && summary.startsWith(c.label)) {
      summary = summary.slice(c.label.length).replace(/^[：:\s]+/, '')
    }
    return { label: c.label, summary }
  })

  const batchColumns = computed(() => {
    if (isCombinedBatch.value) {
      return [
        { prop: 'batchNo', label: '联合批次', width: '80px', align: 'center' },
        { prop: 'orderId', label: '订单编号', width: '130px', align: 'left' },
        { prop: 'productModel', label: '产品型号', align: 'left' },
        { prop: 'batchQty', label: '本批数量', width: '80px', align: 'right' },
        { prop: 'batchTotal', label: '联合批次合计', width: '100px', align: 'right' },
        { prop: 'window', label: '预计周期', width: '180px', align: 'center' }
      ]
    }
    return [
      { prop: 'orderId', label: '订单编号', width: '130px', align: 'left' },
      { prop: 'productModel', label: '产品型号', align: 'left' },
      { prop: 'orderQty', label: '订单数量', width: '80px', align: 'right' },
      { prop: 'batchNo', label: '批次', width: '70px', align: 'center' },
      { prop: 'batchQty', label: '本批数量', width: '80px', align: 'right' },
      { prop: 'window', label: '预计周期', width: '180px', align: 'center' }
    ]
  })

  function fmtDate(d) {
    return d.toISOString().slice(0, 10)
  }

  function buildBatchWindow(batchNo, batchCount, start, end) {
    const totalDays = Math.max(1, Math.round((end - start) / 86400000) + 1)
    const daysPerBatch = Math.max(1, Math.floor(totalDays / batchCount))
    const bStart = new Date(start.getTime() + (batchNo - 1) * daysPerBatch * 86400000)
    const bEnd = batchNo === batchCount ? end : new Date(bStart.getTime() + (daysPerBatch - 1) * 86400000)
    return `${fmtDate(bStart)} ~ ${fmtDate(bEnd > end ? end : bEnd)}`
  }

  const batchPreview = computed(() => {
    const rows = []
    const start = form.planStart ? new Date(form.planStart) : new Date()
    const end = form.planEnd ? new Date(form.planEnd) : new Date(Date.now() + 14 * 86400000)

    if (isCombinedBatch.value) {
      const orders = selectedOrders.value.map((o) => ({
        id: o.id,
        productModel: o.productModel,
        remaining: Number(o.quantity) || 0
      })).filter((o) => o.remaining > 0)
      const total = orders.reduce((sum, o) => sum + o.remaining, 0)
      if (total <= 0) return rows

      const batchCount = Math.ceil(total / PLANNER_BATCH_SIZE)
      let orderIdx = 0
      let orderRemain = orders[0]?.remaining || 0
      let globalLeft = total

      for (let b = 1; b <= batchCount; b++) {
        const batchCapacity = Math.min(PLANNER_BATCH_SIZE, globalLeft)
        globalLeft -= batchCapacity
        const window = buildBatchWindow(b, batchCount, start, end)
        let capacityLeft = batchCapacity
        const batchLines = []

        while (capacityLeft > 0 && orderIdx < orders.length) {
          const take = Math.min(orderRemain, capacityLeft)
          batchLines.push({
            orderId: orders[orderIdx].id,
            productModel: orders[orderIdx].productModel,
            batchQty: take,
            batchNo: b,
            batchCount,
            batchTotal: batchCapacity,
            window
          })
          capacityLeft -= take
          orderRemain -= take
          if (orderRemain <= 0) {
            orderIdx += 1
            orderRemain = orders[orderIdx]?.remaining || 0
          }
        }
        rows.push(...batchLines)
      }
      return rows
    }

    const totalDays = Math.max(1, Math.round((end - start) / 86400000) + 1)
    for (const o of selectedOrders.value) {
      const qty = Number(o.quantity) || 0
      if (qty <= 0) continue
      const batchCount = Math.ceil(qty / PLANNER_BATCH_SIZE)
      const daysPerBatch = Math.max(1, Math.floor(totalDays / batchCount))
      let remaining = qty
      for (let b = 1; b <= batchCount; b++) {
        const batchQty = Math.min(PLANNER_BATCH_SIZE, remaining)
        remaining -= batchQty
        const bStart = new Date(start.getTime() + (b - 1) * daysPerBatch * 86400000)
        const bEnd = b === batchCount ? end : new Date(bStart.getTime() + (daysPerBatch - 1) * 86400000)
        rows.push({
          orderId: o.id,
          productModel: o.productModel,
          orderQty: qty,
          batchNo: b,
          batchCount,
          batchQty,
          window: `${fmtDate(bStart)} ~ ${fmtDate(bEnd > end ? end : bEnd)}`
        })
      }
    }
    return rows
  })

  const submitPlanCount = computed(() => batchPreview.value.length)

  function resetState() {
    analysis.value = null
    schemeData.value = null
    selectedScheme.value = null
    phase.value = 'setup'
    resetFlow()
  }

  function initOrders(orderIds = []) {
    const ids = orderIds.filter(Boolean)
    const validIds = ids.filter((id) => pendingOrders.value.some((o) => o.id === id))
    form.orderIds = validIds.length ? validIds : (pendingOrders.value[0]?.id ? [pendingOrders.value[0].id] : [])
    resetState()
  }

  function onOrderChange() {
    if (phase.value === 'result' || phase.value === 'review') {
      analysis.value = null
      schemeData.value = null
      selectedScheme.value = null
      phase.value = 'setup'
      resetFlow()
    }
  }

  function isOrderSelected(orderId) {
    return form.orderIds.includes(orderId)
  }

  function toggleOrderSelection(orderId) {
    const idx = form.orderIds.indexOf(orderId)
    if (idx >= 0) {
      form.orderIds.splice(idx, 1)
    } else {
      form.orderIds.push(orderId)
    }
    onOrderChange()
  }

  function onSchemeKeySelect(key) {
    const row = schemeData.value?.schemes?.find((s) => s.key === key)
    selectedScheme.value = row || null
  }

  function backToSetup() {
    phase.value = 'setup'
  }

  function backToReview() {
    phase.value = 'review'
  }

  function restartAnalysis() {
    analysis.value = null
    schemeData.value = null
    selectedScheme.value = null
    resetFlow()
    startAnalysis()
  }

  async function startAnalysis() {
    const firstOrderId = form.orderIds[0]
    if (!firstOrderId || !form.planStart || !form.planEnd) {
      ElMessage.warning('请选择订单并设置排产周期')
      return
    }
    phase.value = 'analyzing'
    previewLoading.value = true
    analysis.value = null
    schemeData.value = null
    selectedScheme.value = null
    resetFlow()
    try {
      analysis.value = await runAnimatedPreview(async () => {
        return await mes.previewPlanAgent(
          { orderId: firstOrderId, planStart: form.planStart, planEnd: form.planEnd, plannedQty: 0 },
          userStore.username,
          userStore.roleKey
        )
      }, {
        stepPauseMs: 900,
        charMs: 20,
        evidenceMs: 240
      })
      phase.value = 'review'
    } catch (e) {
      phase.value = 'setup'
      const msg = e?.message || ''
      if (!msg.includes('订单状态不允许 Agent 排产')) {
        ElMessage.error(msg || '分析失败')
      }
    } finally {
      previewLoading.value = false
    }
  }

  async function confirmAndLoadSchemes() {
    const firstOrderId = form.orderIds[0]
    if (!firstOrderId || !analysis.value) return
    schemeLoading.value = true
    try {
      schemeData.value = await postComparePlanSchemes({
        orderId: firstOrderId,
        planStart: form.planStart,
        planEnd: form.planEnd,
        plannedQty: 0
      })
      selectedScheme.value = schemeData.value?.schemes?.find((s) => s.key === schemeData.value?.conclusion?.key)
        || schemeData.value?.schemes?.[0]
        || null
      phase.value = 'result'
    } catch (e) {
      ElMessage.error(e?.message || '方案生成失败')
    } finally {
      schemeLoading.value = false
    }
  }

  async function runCreate() {
    if (!selectedScheme.value) {
      ElMessage.warning('请先选择一种排产方案')
      return
    }
    if (!batchPreview.value.length) {
      ElMessage.warning('请先勾选订单')
      return
    }
    const conflicts = selectedScheme.value.conflicts || []
    if (conflicts.length) {
      const lines = conflicts.map((c) => `· ${c.label}：${c.detail}`).join('\n')
      try {
        await ElMessageBox.confirm(
          `所选「${selectedScheme.value.label}」存在以下提示，确认仍要提交吗？\n\n${lines}`,
          '确认排产方案',
          { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '返回修改' }
        )
      } catch {
        return
      }
    }
    submitLoading.value = true
    try {
      const res = await postSaveBatchProductionPlans({
        orders: selectedOrders.value.map((o) => ({ orderId: o.id, plannedQty: Number(o.quantity) || 0 })),
        planStart: form.planStart,
        planEnd: form.planEnd,
        batchSize: PLANNER_BATCH_SIZE,
        schedulingMode: selectedScheme.value.key,
        saveAction: 'submit',
        combinedBatch: isCombinedBatch.value,
        operator: userStore.username
      })
      ElMessage.success(res?.message || `已生成 ${submitPlanCount.value} 个计划并提交生产主管`)
      onSuccess?.()
      await mes.hydrateForPage()
    } catch (e) {
      ElMessage.error(e?.message || '创建计划失败')
    } finally {
      submitLoading.value = false
    }
  }

  function handleCancel() {
    if (previewLoading.value && phase.value === 'analyzing') return
    onClose?.()
  }

  return {
    BATCH_SIZE: PLANNER_BATCH_SIZE,
    previewLoading,
    schemeLoading,
    submitLoading,
    analysis,
    schemeData,
    selectedScheme,
    phase,
    phaseIndex,
    form,
    pendingOrders,
    selectedOrders,
    totalQty,
    isCombinedBatch,
    combinedBatchCount,
    resultMetrics,
    schemeRecommend,
    batchColumns,
    batchPreview,
    submitPlanCount,
    activeStep,
    activeStepKey,
    selectedStepKey,
    thoughtStream,
    evidenceList,
    allEvidence,
    currentDetail,
    selectStep,
    initOrders,
    resetState,
    onOrderChange,
    isOrderSelected,
    toggleOrderSelection,
    onSchemeKeySelect,
    backToSetup,
    backToReview,
    restartAnalysis,
    startAnalysis,
    confirmAndLoadSchemes,
    runCreate,
    handleCancel
  }
}

export function navigateToSmartScheduling(router, options = {}) {
  const { orderId, orderIds, combined, from } = options
  const query = {}
  if (orderIds?.length) query.orderIds = orderIds.join(',')
  else if (orderId) query.orderId = orderId
  if (combined) query.combined = '1'
  query.from = from || router.currentRoute.value.fullPath
  return router.push({ path: '/production/smart-scheduling', query })
}

export function resolvePlannerOrderIds(defaultOrderId, defaultOrderIds) {
  if (defaultOrderIds?.length) return [...defaultOrderIds]
  if (defaultOrderId) return [defaultOrderId]
  return []
}
