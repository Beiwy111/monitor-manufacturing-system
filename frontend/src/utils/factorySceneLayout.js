import { PRODUCTION_STAGES, PRODUCTION_WORKSHOPS } from '@/utils/productionProgress'

/** 与后端 ProductionWorkshopCatalog 一致的生产设备编码（共 20 台） */
export const PRODUCTION_EQUIPMENT_CODES = [
  'EQ-MB-01', 'EQ-MB-02', 'EQ-MB-03',
  'EQ-PB-01', 'EQ-PB-02',
  'EQ-IF-01', 'EQ-IF-02',
  'EQ-DISP-01', 'EQ-DISP-02', 'EQ-DISP-03',
  'EQ-001', 'EQ-002', 'EQ-010',
  'EQ-SH-01', 'EQ-SH-02',
  'EQ-003', 'EQ-007', 'EQ-012',
  'EQ-BR-01', 'EQ-BR-02'
]

/** 后置工序区设备（老化 / 调校 / 包装） */
export const POST_PRODUCTION_CODES = ['EQ-004', 'EQ-005', 'EQ-006', 'EQ-008', 'EQ-009', 'EQ-011']

const STAGE_ACCENT = {
  motherboard: 0x2e7d32,
  powerboard: 0x1565c0,
  interface: 0x6a1b9a,
  display: 0x7b1fa2,
  attach: 0x00838f,
  shell: 0x558b2f,
  assembly: 0x0277bd,
  bracket: 0x8d6e63,
  post: 0x546e7a
}

const LAYOUT = {
  stageGap: 5.8,
  wsGap: 2.8,
  eqSlotGap: 1.35,
  postZoneX: 24.5
}

const CODE_TO_WORKSHOP_KEY = {
  'EQ-MB-01': 'mb-1', 'EQ-MB-02': 'mb-2', 'EQ-MB-03': 'mb-3',
  'EQ-PB-01': 'pb-1', 'EQ-PB-02': 'pb-2',
  'EQ-IF-01': 'if-1', 'EQ-IF-02': 'if-2',
  'EQ-DISP-01': 'display-1', 'EQ-DISP-02': 'display-2', 'EQ-DISP-03': 'display-3',
  'EQ-001': 'attach-1', 'EQ-002': 'attach-2', 'EQ-010': 'attach-2',
  'EQ-SH-01': 'shell-1', 'EQ-SH-02': 'shell-2',
  'EQ-003': 'assembly-1', 'EQ-007': 'assembly-2', 'EQ-012': 'assembly-3',
  'EQ-BR-01': 'bracket-1', 'EQ-BR-02': 'bracket-2'
}

function workshopKeyForEquipment(eq) {
  const code = eq?.equipmentCode || ''
  const wsName = (eq?.workshop || '').trim()
  if (wsName) {
    const hit = PRODUCTION_WORKSHOPS.find(
      (w) => w.name === wsName || wsName.includes(w.name) || w.name.includes(wsName)
    )
    if (hit) return hit.key
  }
  return CODE_TO_WORKSHOP_KEY[code] || null
}

export function isProductionEquipment(eq) {
  const code = eq?.equipmentCode || ''
  if (PRODUCTION_EQUIPMENT_CODES.includes(code)) return true
  return !!workshopKeyForEquipment(eq)
}

export function isSceneEquipment(eq) {
  const code = eq?.equipmentCode || ''
  return isProductionEquipment(eq) || POST_PRODUCTION_CODES.includes(code)
}

/** 八道生产工序 + 后置区 */
export function buildStageZones() {
  const { stageGap } = LAYOUT
  const startX = -((PRODUCTION_STAGES.length - 1) * stageGap) / 2
  const zones = PRODUCTION_STAGES.map((stage, idx) => {
    const count = stage.workshops.length
    const depth = Math.max(4.8, (count - 1) * LAYOUT.wsGap + 3.6)
    return {
      key: stage.stepKey,
      name: stage.stepName,
      order: stage.order,
      x: startX + idx * stageGap,
      z: 0,
      halfW: 2.55,
      halfD: depth / 2 + 0.6,
      color: STAGE_ACCENT[stage.stepKey] ?? 0x3b82f6,
      workshopCount: count
    }
  })
  zones.push({
    key: 'post',
    name: '检测·老化·包装',
    order: 9,
    x: LAYOUT.postZoneX,
    z: 0,
    halfW: 3.2,
    halfD: 4.2,
    color: STAGE_ACCENT.post,
    workshopCount: 1
  })
  return zones
}

/** 工位垫、缓存区、AGV 周转区、主输送线 */
export function buildSceneInfrastructure() {
  const { stageGap, postZoneX } = LAYOUT
  const startX = -((PRODUCTION_STAGES.length - 1) * stageGap) / 2
  const endX = startX + (PRODUCTION_STAGES.length - 1) * stageGap

  const conveyors = []
  for (let i = 0; i < PRODUCTION_STAGES.length - 1; i++) {
    const x1 = startX + i * stageGap + 2.6
    const x2 = startX + (i + 1) * stageGap - 2.6
    conveyors.push({ x: (x1 + x2) / 2, z: -3.8, length: x2 - x1, axis: 'x' })
  }
  conveyors.push({ x: (endX + postZoneX) / 2, z: -3.8, length: postZoneX - endX - 3, axis: 'x' })

  const buffers = PRODUCTION_STAGES.map((stage, idx) => ({
    key: `buf-${stage.stepKey}`,
    label: '缓存区',
    x: startX + idx * stageGap,
    z: 3.6,
    w: 1.8,
    d: 1.2
  }))

  const workstations = []
  PRODUCTION_STAGES.forEach((stage, stageIdx) => {
    const stageX = startX + stageIdx * stageGap
    stage.workshops.forEach((ws, wsIdx) => {
      const wsCount = stage.workshops.length
      const startZ = -((wsCount - 1) * LAYOUT.wsGap) / 2
      workstations.push({
        key: ws.key,
        label: ws.name.replace('车间', ''),
        x: stageX,
        z: startZ + wsIdx * LAYOUT.wsGap,
        w: 1.6,
        d: 1.4
      })
    })
  })

  return {
    conveyors,
    buffers,
    workstations,
    agvZone: { x: startX - 4.5, z: 5.5, w: 5.5, d: 3.2, label: 'AGV 周转区' },
    floorMarkings: [
      { x1: startX - 3.5, z1: -5.2, x2: postZoneX + 3.5, z2: -5.2, color: 0xf9a825 },
      { x1: startX - 3.5, z1: 5.2, x2: postZoneX + 3.5, z2: 5.2, color: 0xf9a825 },
      { x1: startX - 3.5, z1: -5.2, x2: startX - 3.5, z2: 5.2, color: 0xf9a825 },
      { x1: postZoneX + 3.5, z1: -5.2, x2: postZoneX + 3.5, z2: 5.2, color: 0xf9a825 }
    ],
    postSlots: [
      { x: postZoneX - 1.8, z: -1.5, label: '老化' },
      { x: postZoneX, z: -1.5, label: '调校' },
      { x: postZoneX + 1.8, z: -1.5, label: '包装' },
      { x: postZoneX - 0.9, z: 1.5, label: '老化B' },
      { x: postZoneX + 0.9, z: 1.5, label: '包装B' }
    ]
  }
}

export function buildEquipmentPositions(equipmentList = []) {
  const positions = {}
  const { stageGap, wsGap, eqSlotGap, postZoneX } = LAYOUT
  const startX = -((PRODUCTION_STAGES.length - 1) * stageGap) / 2

  const production = (equipmentList || []).filter(isProductionEquipment)
  const byWorkshopKey = {}
  for (const eq of production) {
    const key = workshopKeyForEquipment(eq)
    if (!key) continue
    if (!byWorkshopKey[key]) byWorkshopKey[key] = []
    byWorkshopKey[key].push(eq)
  }

  PRODUCTION_STAGES.forEach((stage, stageIdx) => {
    const stageX = startX + stageIdx * stageGap
    const wsCount = stage.workshops.length
    const startZ = -((wsCount - 1) * wsGap) / 2

    stage.workshops.forEach((ws, wsIdx) => {
      const wsZ = startZ + wsIdx * wsGap
      const eqs = (byWorkshopKey[ws.key] || []).sort((a, b) =>
        String(a.equipmentCode).localeCompare(String(b.equipmentCode))
      )
      eqs.forEach((eq, eqIdx) => {
        const slotOffset = (eqIdx - (eqs.length - 1) / 2) * eqSlotGap
        positions[eq.equipmentCode] = {
          x: stageX,
          z: wsZ + slotOffset,
          rotY: 0,
          stageKey: stage.stepKey,
          stageName: stage.stepName,
          workshopKey: ws.key,
          workshopName: ws.name
        }
      })
    })
  })

  const postEqs = (equipmentList || []).filter((e) => POST_PRODUCTION_CODES.includes(e.equipmentCode))
  const postSlots = buildSceneInfrastructure().postSlots
  postEqs.sort((a, b) => String(a.equipmentCode).localeCompare(String(b.equipmentCode)))
  postEqs.forEach((eq, i) => {
    const slot = postSlots[i % postSlots.length]
    positions[eq.equipmentCode] = {
      x: slot?.x ?? postZoneX,
      z: slot?.z ?? 0,
      rotY: 0,
      stageKey: 'post',
      stageName: '检测·老化·包装',
      workshopKey: 'post',
      workshopName: slot?.label || '后置区'
    }
  })

  return positions
}
