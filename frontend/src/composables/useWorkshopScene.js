import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'
import { PRODUCTION_STAGES } from '@/utils/productionProgress'
import { operatorsForWorkshop } from '@/utils/operatorWorkshop'
import {
  populateWorkshopOperators,
  updateWorkshopOperators,
  updateCrossWorkshopInteractions
} from '@/composables/workshopOperators'

const STATUS_COLOR = {
  running: 0x43a047,
  pending: 0x546e7a,
  warning: 0xef6c00,
  abnormal: 0xc62828
}

const MACHINE_STATUS = {
  RUNNING: 0x43a047,
  IDLE: 0x546e7a,
  FAULT: 0xc62828,
  MAINTENANCE: 0xef6c00
}

const STAGE_ACCENT = {
  motherboard: 0x506784,
  powerboard: 0x5b6e8c,
  interface: 0x4f6d8a,
  display: 0x7c6aad,
  attach: 0x5a8f96,
  shell: 0x6a8f7a,
  assembly: 0x5a9a7a,
  bracket: 0x8a7a5a
}

/** 大屏整体放大系数（车间 + 设备 + 展示显示器） */
const SCENE_SCALE = 1.15
const MACHINE_SCALE = 1.22
const SHOWCASE_MONITOR_SCALE = 1.25

/** 车间布局参数（工序间距 / 同工序车间间距） */
const LAYOUT = {
  stageGap: 13,
  wsGap: 6.2,
  w: 6.8 * SCENE_SCALE,
  d: 6.0 * SCENE_SCALE,
  h: 4.4 * SCENE_SCALE
}

/** 八道生产工序 × 每道 2~3 车间：从左往右流水线排布（X 轴=工序，Z 轴=同工序内车间） */
function buildWorkshopLayout() {
  const layouts = []
  const { stageGap, wsGap, w, d, h } = LAYOUT
  const startX = -((PRODUCTION_STAGES.length - 1) * stageGap) / 2

  PRODUCTION_STAGES.forEach((stage, stageIdx) => {
    const count = stage.workshops.length
    const startZ = -((count - 1) * wsGap) / 2
    stage.workshops.forEach((ws, wsIdx) => {
      layouts.push({
        key: ws.key,
        parentStepKey: stage.stepKey,
        parentStepName: stage.stepName,
        x: startX + stageIdx * stageGap,
        z: startZ + wsIdx * wsGap,
        rot: 0,
        w,
        d,
        h,
        accent: STAGE_ACCENT[stage.stepKey] || 0x1565c0,
        name: ws.name
      })
    })
  })
  return layouts
}

export const WORKSHOP_LAYOUT = buildWorkshopLayout()

/** 将 API 返回与 3D 布局合并 */
export function mergeWorkshopData(apiList) {
  return WORKSHOP_LAYOUT.map((layout) => {
    const hit = (apiList || []).find((w) => w.key === layout.key)
    if (!hit) {
      return {
        key: layout.key,
        name: layout.name,
        parentStepKey: layout.parentStepKey,
        parentStepName: layout.parentStepName,
        department: '',
        taskTitle: layout.parentStepName,
        taskDescription: '',
        workOrderNo: '',
        progress: 0,
        completedQty: 0,
        plannedQty: 0,
        progressLabel: '等待数据同步…',
        total: 0,
        running: 0,
        idle: 0,
        fault: 0,
        status: 'pending',
        machines: [],
        operators: operatorsForWorkshop(layout.key)
      }
    }
    const progress = hit.progress ?? 0
    return {
      ...hit,
      key: layout.key,
      name: hit.name || layout.name,
      parentStepKey: hit.parentStepKey || layout.parentStepKey,
      parentStepName: hit.parentStepName || layout.parentStepName,
      progress,
      completedQty: hit.completedQty ?? 0,
      plannedQty: hit.plannedQty ?? 0,
      progressLabel: hit.progressLabel || (hit.plannedQty ? `已完成 ${hit.completedQty ?? 0} / ${hit.plannedQty} 台` : '暂无派工任务'),
      machines: Array.isArray(hit.machines) ? hit.machines : [],
      operators: operatorsForWorkshop(layout.key),
      lines: Array.isArray(hit.lines) ? hit.lines : [],
      dailyCapacity: hit.dailyCapacity,
      availableMachines: hit.availableMachines,
      equipmentType: hit.equipmentType,
      total: hit.total ?? hit.machines?.length ?? 0,
      running: hit.running ?? 0,
      fault: hit.fault ?? 0,
      status: hit.status || 'pending'
    }
  })
}
function buildLabelCardHtml() {
  return `
    <div class="workshop-3d-card">
      <div class="workshop-3d-card__name"></div>
      <div class="workshop-3d-card__task"></div>
      <div class="workshop-3d-card__bar"><span></span></div>
      <div class="workshop-3d-card__meta"></div>
    </div>
  `
}

function updateLabelCard(labelEl, data) {
  if (!labelEl || !data) return
  const pct = Math.min(100, Math.max(0, data.progress ?? 0))
  labelEl.querySelector('.workshop-3d-card__name').textContent = data.name || ''
  labelEl.querySelector('.workshop-3d-card__task').textContent = data.taskTitle || data.parentStepName || '当前工序'
  labelEl.querySelector('.workshop-3d-card__bar span').style.width = `${pct}%`
  labelEl.querySelector('.workshop-3d-card__meta').textContent =
    data.progressLabel || `已完成 ${data.completedQty ?? 0} / ${data.plannedQty ?? 0} 台 · ${pct}%`
  labelEl.classList.toggle('is-hovered', !!data._hovered)
}

function mat(color, opts = {}) {
  return new THREE.MeshStandardMaterial({
    color,
    metalness: opts.metal ?? 0.15,
    roughness: opts.rough ?? 0.65,
    emissive: opts.emissive ?? 0x000000,
    emissiveIntensity: opts.ei ?? 0,
    transparent: opts.transparent ?? false,
    opacity: opts.opacity ?? 1
  })
}

function box(w, h, d, material, x = 0, y = 0, z = 0) {
  const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), material)
  m.position.set(x, y, z)
  m.castShadow = true
  m.receiveShadow = true
  return m
}

function cyl(rt, rb, h, material, x = 0, y = 0, z = 0) {
  const m = new THREE.Mesh(new THREE.CylinderGeometry(rt, rb, h, 10), material)
  m.position.set(x, y, z)
  m.castShadow = true
  m.receiveShadow = true
  return m
}

/** 传送带段（axis: 'x' | 'z'） */
function createConveyorSegment(length, axis = 'x') {
  const g = new THREE.Group()
  const beltMat = mat(0x455a64, { rough: 0.75 })
  const railMat = mat(0x78909c, { metal: 0.35, rough: 0.45 })
  const isX = axis === 'x'
  const belt = isX
    ? box(length, 0.07, 0.82, beltMat, 0, 0.1, 0)
    : box(0.82, 0.07, length, beltMat, 0, 0.1, 0)
  const railA = isX
    ? box(length, 0.09, 0.05, railMat, 0, 0.2, -0.44)
    : box(0.05, 0.09, length, railMat, -0.44, 0.2, 0)
  const railB = isX
    ? box(length, 0.09, 0.05, railMat, 0, 0.2, 0.44)
    : box(0.05, 0.09, length, railMat, 0.44, 0.2, 0)
  g.add(belt, railA, railB)

  const rollerStep = 1.1
  const count = Math.max(2, Math.floor(length / rollerStep))
  for (let i = 0; i < count; i++) {
    const t = -length / 2 + (i + 0.5) * (length / count)
    const roller = cyl(0.1, 0.1, 0.78, mat(0x607d8b, { metal: 0.4 }), 0, 0.1, 0)
    roller.rotation.x = Math.PI / 2
    if (isX) roller.position.set(t, 0.1, 0)
    else roller.position.set(0, 0.1, t)
    g.add(roller)
  }
  return g
}

function getLayoutBounds(layouts) {
  let minX = Infinity
  let maxX = -Infinity
  let minZ = Infinity
  let maxZ = -Infinity
  layouts.forEach((l) => {
    minX = Math.min(minX, l.x - l.w * 0.5)
    maxX = Math.max(maxX, l.x + l.w * 0.5)
    minZ = Math.min(minZ, l.z - l.d * 0.5)
    maxZ = Math.max(maxZ, l.z + l.d * 0.5)
  })
  return { minX, maxX, minZ, maxZ }
}

/** 工序间 + 同工序车间间传送带连接 */
function buildPipelineConnections(scene, layouts) {
  const stageKeys = PRODUCTION_STAGES.map((s) => s.stepKey)

  stageKeys.forEach((sk) => {
    const wss = layouts.filter((l) => l.parentStepKey === sk).sort((a, b) => a.z - b.z)
    for (let j = 0; j < wss.length - 1; j++) {
      const a = wss[j]
      const b = wss[j + 1]
      const segLen = b.z - a.z - a.d * 0.42 - b.d * 0.42
      if (segLen < 0.6) continue
      const conv = createConveyorSegment(segLen, 'z')
      conv.position.set(a.x, 0, (a.z + b.z) / 2)
      scene.add(conv)
    }
  })

  for (let i = 0; i < stageKeys.length - 1; i++) {
    const currList = layouts.filter((l) => l.parentStepKey === stageKeys[i])
    const nextList = layouts.filter((l) => l.parentStepKey === stageKeys[i + 1])
    if (!currList.length || !nextList.length) continue

    currList.forEach((curr, idx) => {
      const next = nextList[Math.min(idx, nextList.length - 1)]
      const segLen = next.x - curr.x - curr.w * 0.44 - next.w * 0.44
      if (segLen < 0.6) return
      const conv = createConveyorSegment(segLen, 'x')
      conv.position.set((curr.x + next.x) / 2, 0, (curr.z + next.z) / 2)
      scene.add(conv)
    })
  }

  const midLayouts = stageKeys.map((sk) => {
    const list = layouts.filter((l) => l.parentStepKey === sk).sort((a, b) => a.z - b.z)
    return list[Math.floor(list.length / 2)] || list[0]
  }).filter(Boolean)

  for (let i = 0; i < midLayouts.length - 1; i++) {
    const a = midLayouts[i]
    const b = midLayouts[i + 1]
    const segLen = b.x - a.x - a.w * 0.44 - b.w * 0.44
    if (segLen < 0.6) continue
    const spine = createConveyorSegment(segLen, 'x')
    spine.position.set((a.x + b.x) / 2, 0.02, 0)
    scene.add(spine)
  }
}

function createPalletStack(layers = 2) {
  const g = new THREE.Group()
  const palletMat = mat(0x8d6e63)
  const boxMat = mat(0xa1887f)
  for (let i = 0; i < layers; i++) {
    g.add(box(0.75, 0.08, 0.55, palletMat, 0, 0.12 + i * 0.36, 0))
    g.add(box(0.65, 0.28, 0.48, boxMat, 0.04, 0.32 + i * 0.36, 0))
  }
  return g
}

function createControlCabinet(accent = 0x546e7a) {
  const g = new THREE.Group()
  g.add(box(0.38, 1.05, 0.28, mat(0x90a4ae), 0, 0.52, 0))
  g.add(box(0.34, 0.22, 0.02, mat(accent, { emissive: accent, ei: 0.15 }), 0, 0.72, 0.15))
  g.add(box(0.12, 0.55, 0.02, mat(0x263238), 0, 0.45, 0.15))
  return g
}

function createFireCabinet() {
  const g = new THREE.Group()
  g.add(box(0.28, 0.55, 0.12, mat(0xc62828), 0, 0.28, 0))
  g.add(box(0.22, 0.38, 0.02, mat(0xffffff), 0, 0.3, 0.07))
  return g
}

function createStorageRack(levels = 4, cols = 3) {
  const g = new THREE.Group()
  const frameMat = mat(0x1565c0, { metal: 0.4, rough: 0.4 })
  const boxMat = mat(0x8d6e63)
  const w = cols * 0.55 + 0.3
  const h = levels * 0.42 + 0.2
  g.add(box(w, h, 0.55, frameMat, 0, h / 2, 0))
  for (let lv = 0; lv < levels; lv++) {
    for (let c = 0; c < cols; c++) {
      const bx = -w / 2 + 0.35 + c * 0.55
      const by = 0.28 + lv * 0.42
      g.add(box(0.42, 0.28, 0.38, boxMat, bx, by, 0.05))
    }
  }
  return g
}

function createSafetyFence(width, depth) {
  const g = new THREE.Group()
  const postMat = mat(0x2e7d32, { metal: 0.2 })
  const barMat = mat(0x66bb6a, { transparent: true, opacity: 0.42 })
  const posts = [
    [-width / 2, -depth / 2], [width / 2, -depth / 2],
    [width / 2, depth / 2], [-width / 2, depth / 2]
  ]
  posts.forEach(([px, pz]) => {
    const post = cyl(0.04, 0.04, 0.9, postMat, px, 0.45, pz)
    g.add(post)
  })
  g.add(box(width, 0.04, 0.04, barMat, 0, 0.75, -depth / 2))
  g.add(box(width, 0.04, 0.04, barMat, 0, 0.45, -depth / 2))
  g.add(box(0.04, 0.04, depth, barMat, -width / 2, 0.6, 0))
  g.add(box(0.04, 0.04, depth, barMat, width / 2, 0.6, 0))
  return g
}

function createForklift() {
  const g = new THREE.Group()
  g.add(box(0.55, 0.18, 0.85, mat(0xf9a825), 0, 0.18, 0))
  g.add(box(0.35, 0.45, 0.42, mat(0xffb300), 0, 0.48, -0.15))
  g.add(box(0.06, 0.55, 0.06, mat(0x546e7a), 0.28, 0.55, 0.35))
  g.add(box(0.5, 0.04, 0.35, mat(0x8d6e63), 0.35, 0.35, 0.55))
  return g
}

/** 厂区地面标识、围栏、货架、托盘等装饰 */
function buildSceneDecorations(scene, layouts) {
  const bounds = getLayoutBounds(layouts)
  const padX = 6
  const padZ = 5
  const floorW = bounds.maxX - bounds.minX + padX * 2
  const floorD = bounds.maxZ - bounds.minZ + padZ * 2
  const cx = (bounds.minX + bounds.maxX) / 2
  const cz = (bounds.minZ + bounds.maxZ) / 2

  const mainAisle = box(floorW * 0.92, 0.025, 2.0, mat(0xfff9c4, { transparent: true, opacity: 0.35 }), cx, 0.06, cz)
  scene.add(mainAisle)

  const crossAisle = box(2.0, 0.025, floorD * 0.88, mat(0xfff9c4, { transparent: true, opacity: 0.28 }), cx, 0.055, cz)
  scene.add(crossAisle)

  layouts.forEach((l, idx) => {
    const zone = box(l.w * 0.88, 0.02, l.d * 0.88, mat(0xc62828, { transparent: true, opacity: 0.12 }), l.x, 0.07, l.z)
    scene.add(zone)

    if (idx % 2 === 0) {
      const pallet = createPalletStack(1 + (idx % 3))
      pallet.position.set(l.x + l.w * 0.32, 0, l.z + l.d * 0.28)
      scene.add(pallet)
    }

    const cabinet = createControlCabinet(l.accent)
    cabinet.position.set(l.x - l.w * 0.38, 0, l.z - l.d * 0.32)
    scene.add(cabinet)

    if (idx % 4 === 1) {
      const ext = createFireCabinet()
      ext.position.set(l.x + l.w * 0.4, 0, l.z - l.d * 0.35)
      scene.add(ext)
    }
  })

  const rackZ = bounds.minZ - padZ + 1.2
  for (let x = bounds.minX; x <= bounds.maxX; x += 9) {
    const rack = createStorageRack(4, 2 + (Math.abs(Math.floor(x)) % 2))
    rack.position.set(x, 0, rackZ)
    scene.add(rack)
  }

  const fenceW = floorW * 0.95
  const fence = createSafetyFence(fenceW, floorD * 0.35)
  fence.position.set(cx, 0, bounds.maxZ + padZ * 0.55)
  scene.add(fence)

  const fence2 = createSafetyFence(floorD * 0.55, 2.5)
  fence2.rotation.y = Math.PI / 2
  fence2.position.set(bounds.minX - padX + 1.5, 0, cz)
  scene.add(fence2)

  const fork = createForklift()
  fork.position.set(bounds.maxX + padX * 0.35, 0, cz - 2)
  fork.rotation.y = -0.6
  scene.add(fork)

  const fork2 = createForklift()
  fork2.position.set(bounds.minX - padX * 0.2, 0, cz + 3)
  fork2.rotation.y = 0.4
  scene.add(fork2)

  PRODUCTION_STAGES.forEach((stage, idx) => {
    if (idx === 0) return
    const x = -((PRODUCTION_STAGES.length - 1) * LAYOUT.stageGap) / 2 + idx * LAYOUT.stageGap - LAYOUT.stageGap / 2
    const arrow = box(0.12, 0.03, 1.2, mat(STAGE_ACCENT[stage.stepKey] || 0x506784, { emissive: STAGE_ACCENT[stage.stepKey] || 0x506784, ei: 0.08 }), x, 0.08, cz + 1.1)
    scene.add(arrow)
  })
}

function createMonitor(scale = 1, on = true, screenHue = 0x1565c0) {
  const g = new THREE.Group()
  const s = scale
  const basePlate = box(0.65 * s, 0.06 * s, 0.45 * s, mat(0x263238), 0, 0.03 * s, 0)
  const stand = box(0.35 * s, 0.1 * s, 0.28 * s, mat(0x37474f), 0, 0.1 * s, 0)
  const neck = box(0.08 * s, 0.45 * s, 0.06 * s, mat(0x455a64), 0, 0.35 * s, 0)
  const chin = box(0.55 * s, 0.04 * s, 0.05 * s, mat(0x212121), 0, 0.58 * s, 0.02 * s)
  const bezel = box(1.15 * s, 0.72 * s, 0.07 * s, mat(0x1a1a1a, { metal: 0.35, rough: 0.4 }), 0, 0.78 * s, 0)
  const screenMat = mat(on ? screenHue : 0x111111, {
    emissive: on ? screenHue : 0,
    ei: on ? 0.65 : 0,
    metal: 0.05,
    rough: 0.25
  })
  const screen = box(0.98 * s, 0.6 * s, 0.02 * s, screenMat, 0, 0.78 * s, 0.04 * s)
  screen.userData.isScreen = true
  if (on) {
    const uiLine = box(0.7 * s, 0.04 * s, 0.01 * s, mat(0x64b5f6, { emissive: 0x64b5f6, ei: 0.4 }), 0, 0.88 * s, 0.05 * s)
    const uiBar = box(0.5 * s, 0.03 * s, 0.01 * s, mat(0x81c784, { emissive: 0x81c784, ei: 0.35 }), -0.1 * s, 0.68 * s, 0.05 * s)
    g.add(uiLine, uiBar)
  }
  const led = new THREE.Mesh(
    new THREE.SphereGeometry(0.025 * s, 8, 8),
    mat(on ? 0x4caf50 : 0x37474f, { emissive: on ? 0x4caf50 : 0, ei: on ? 0.9 : 0 })
  )
  led.position.set(0.42 * s, 0.58 * s, 0.06 * s)
  g.add(basePlate, stand, neck, chin, bezel, screen, led)
  g.userData.screens = [screen]
  return g
}

const WORKSHOP_SCREEN_HUES = {
  motherboard: 0x1565c0,
  powerboard: 0x5b6e8c,
  interface: 0x4f6d8a,
  display: 0x5e35b1,
  attach: 0x00838f,
  shell: 0x6a8f7a,
  assembly: 0x2e7d32,
  bracket: 0x8a7a5a
}

function addWorkshopShowcase(showcaseGroup, layout, data) {
  const { w, d } = layout
  const progress = data?.progress ?? 0
  const running = (data?.running ?? 0) > 0 || progress > 0
  const stepKey = layout.parentStepKey || layout.key
  const hue = WORKSHOP_SCREEN_HUES[stepKey] || 0x1565c0
  showcaseGroup.clear()

  if (stepKey === 'display') {
    for (let i = 0; i < 4; i++) {
      const m = createMonitor(0.32 * SHOWCASE_MONITOR_SCALE, running || i < 2, hue)
      m.position.set(-w * 0.3 + (i % 2) * 0.7, 0.42 + Math.floor(i / 2) * 0.55, d * 0.22)
      showcaseGroup.add(m)
    }
  } else if (stepKey === 'motherboard') {
    for (let i = 0; i < 3; i++) {
      const pcb = box(0.55 * SCENE_SCALE, 0.04 * SCENE_SCALE, 0.4 * SCENE_SCALE, mat(0x1b5e20, { emissive: running ? 0x2e7d32 : 0, ei: running ? 0.25 : 0 }), -w * 0.22 + i * 0.55, 0.55, d * 0.2)
      showcaseGroup.add(pcb)
    }
  } else if (stepKey === 'attach') {
    for (let i = 0; i < 3; i++) {
      const m = createMonitor(0.35 * SHOWCASE_MONITOR_SCALE, running, hue)
      m.position.set(-w * 0.28 + i * 0.55, 0.45, d * 0.22)
      m.rotation.y = -0.35
      showcaseGroup.add(m)
    }
  } else if (stepKey === 'assembly') {
    for (let i = 0; i < 4; i++) {
      const m = createMonitor(0.32 * SHOWCASE_MONITOR_SCALE, running || i < 2, hue)
      m.position.set(-w * 0.3 + (i % 2) * 0.7, 0.4 + Math.floor(i / 2) * 0.55, d * 0.25)
      showcaseGroup.add(m)
    }
  } else if (stepKey === 'powerboard' || stepKey === 'interface') {
    for (let i = 0; i < 3; i++) {
      const pcb = box(0.5 * SCENE_SCALE, 0.04 * SCENE_SCALE, 0.38 * SCENE_SCALE, mat(0x1b5e20, { emissive: running ? 0x2e7d32 : 0, ei: running ? 0.25 : 0 }), -w * 0.2 + i * 0.5, 0.52, d * 0.18)
      showcaseGroup.add(pcb)
    }
  } else if (stepKey === 'shell' || stepKey === 'bracket') {
    for (let i = 0; i < 2; i++) {
      const m = createMonitor(0.3 * SHOWCASE_MONITOR_SCALE, running, hue)
      m.position.set(-w * 0.22 + i * 0.55, 0.42, d * 0.2)
      showcaseGroup.add(m)
    }
  }
}

function fillWorkshopContent(ws, data) {
  ws.status = data.status || 'pending'
  ws.data = data
  updateLabelCard(ws.labelEl, data)

  ws.machinesGroup.clear()
  const machines = data.machines || []
  const stepKey = data.parentStepKey || ws.layout.parentStepKey || data.key
  const builder = MACHINE_BUILDERS[stepKey] || buildAssemblyLine

  if (machines.length) {
    machines.forEach((m, idx) => {
      const mesh = builder(m.status || 'IDLE')
      const col = idx % 2
      const row = Math.floor(idx / 2)
      mesh.position.set(-1.2 + col * 2.4, 0, -0.8 + row * 1.6)
      mesh.scale.setScalar(MACHINE_SCALE)
      ws.machinesGroup.add(mesh)
    })
  } else {
    const mesh = builder(data.running > 0 ? 'RUNNING' : 'IDLE')
    mesh.scale.setScalar(MACHINE_SCALE * 1.05)
    ws.machinesGroup.add(mesh)
  }

  addWorkshopShowcase(ws.showcaseGroup, ws.layout, data)

  populateWorkshopOperators(ws, data)

  const zoneColor = STATUS_COLOR[ws.status] || STATUS_COLOR.pending
  if (ws.glowRing?.material) {
    ws.glowRing.material.color.setHex(zoneColor)
    ws.glowRing.material.emissive.setHex(zoneColor)
  }
  if (ws.interiorLight) {
    ws.interiorLight.color.setHex(ws.layout.accent)
    ws.interiorLight.intensity = data.status === 'running' ? 1.4 : 0.7
  }
}

function findWorkshopKeyFromObject(obj) {
  let cur = obj
  while (cur) {
    if (cur.userData?.key) return cur.userData.key
    cur = cur.parent
  }
  return null
}

function buildDisplayLine(status) {
  const g = new THREE.Group()
  const c = MACHINE_STATUS[status] || MACHINE_STATUS.IDLE
  const bench = box(2.8, 0.7, 1.2, mat(0x455a64), 0, 0.35, 0)
  g.add(bench)
  for (let i = -1; i <= 1; i++) {
    const m = createMonitor(0.5, status === 'RUNNING', 0x5e35b1)
    m.position.set(i * 0.85, 0.72, 0.1)
    g.add(m)
  }
  const light = new THREE.Mesh(new THREE.SphereGeometry(0.1, 10, 10), mat(c, { emissive: c, ei: status === 'RUNNING' ? 0.7 : 0.12 }))
  light.position.set(1.2, 1.1, 0.5)
  light.userData.isStatusLight = true
  g.add(light)
  return g
}

function buildMotherboardLine(status) {
  const g = new THREE.Group()
  const c = MACHINE_STATUS[status] || MACHINE_STATUS.IDLE
  const table = box(2.6, 0.75, 1.1, mat(0x546e7a), 0, 0.375, 0)
  g.add(table)
  for (let i = 0; i < 3; i++) {
    const pcb = box(0.7, 0.05, 0.5, mat(0x1b5e20, { emissive: status === 'RUNNING' ? 0x2e7d32 : 0, ei: status === 'RUNNING' ? 0.3 : 0 }), -0.7 + i * 0.7, 0.78, 0)
    g.add(pcb)
  }
  const tower = box(0.35, 1.0, 0.35, mat(c, { emissive: c, ei: 0.12 }), 1.05, 0.85, -0.4)
  tower.userData.isStatusLight = true
  g.add(tower)
  return g
}

function buildAttachMachine(status) {
  const g = new THREE.Group()
  const c = MACHINE_STATUS[status] || MACHINE_STATUS.IDLE
  const base = box(2.2, 0.35, 1.6, mat(0x37474f), 0, 0.175, 0)
  const frameL = box(0.15, 1.8, 0.15, mat(0x546e7a), -0.9, 1.05, 0.6)
  const frameR = frameL.clone()
  frameR.position.x = 0.9
  const gantry = box(2.0, 0.2, 0.2, mat(c, { emissive: c, ei: status === 'RUNNING' ? 0.15 : 0.02 }), 0, 1.6, 0.6)
  const head = box(0.5, 0.35, 0.5, mat(0x78909c), 0, 1.25, 0.6)
  const panel = box(0.6, 0.8, 0.12, mat(0x263238), -1.0, 0.85, -0.5)
  const wipPanel = box(0.55, 0.35, 0.04, mat(0x1a237e, { emissive: 0x283593, ei: 0.2 }), 0, 0.55, 0.35)
  const wipMon = createMonitor(0.48, status === 'RUNNING', 0x3949ab)
  wipMon.position.set(0, 0.55, 0.55)
  wipMon.rotation.x = -0.15
  const light = new THREE.Mesh(new THREE.SphereGeometry(0.1, 12, 12), mat(c, { emissive: c, ei: status === 'RUNNING' ? 0.8 : 0.15 }))
  light.position.set(0.95, 1.85, 0.75)
  light.userData.isStatusLight = true
  g.add(base, frameL, frameR, gantry, head, panel, wipPanel, wipMon, light)
  g.userData.spinParts = status === 'RUNNING' ? [head] : []
  return g
}

/** 缁勮绾匡細浼犻€佸甫 + 宸ヤ綅 */
function buildAssemblyLine(status) {
  const g = new THREE.Group()
  const c = MACHINE_STATUS[status] || MACHINE_STATUS.IDLE
  const belt = box(3.2, 0.15, 1.0, mat(0x455a64), 0, 0.25, 0)
  const rail1 = box(3.2, 0.12, 0.06, mat(0x78909c), 0, 0.38, -0.48)
  const rail2 = rail1.clone()
  rail2.position.z = 0.48
  g.add(belt, rail1, rail2)
  for (let i = -1; i <= 1; i++) {
    const mon = createMonitor(0.55, status === 'RUNNING')
    mon.position.set(i * 1.0, 0.5, 0.15)
    mon.rotation.y = -0.2
    g.add(mon)
  }
  const tower = box(0.4, 1.2, 0.4, mat(c, { emissive: c, ei: 0.1 }), 1.35, 0.85, -0.55)
  tower.userData.isStatusLight = true
  g.add(tower)
  g.userData.belt = belt
  return g
}

/** 鑰佸寲鏋讹細澶氬眰鏄剧ず鍣?*/
function buildAgingRack(status) {
  const g = new THREE.Group()
  const c = MACHINE_STATUS[status] || MACHINE_STATUS.IDLE
  const frame = box(2.4, 2.8, 0.8, mat(0x37474f, { transparent: true, opacity: 0.85 }), 0, 1.5, 0)
  g.add(frame)
  for (let row = 0; row < 3; row++) {
    for (let col = 0; col < 2; col++) {
      const m = createMonitor(0.38, status === 'RUNNING')
      m.position.set(-0.55 + col * 1.1, 0.55 + row * 0.85, 0.35)
      g.add(m)
    }
  }
  const beacon = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.08, 0.5, 8), mat(c, { emissive: c, ei: status === 'RUNNING' ? 0.6 : 0.1 }))
  beacon.position.set(1.1, 2.9, 0)
  beacon.userData.isStatusLight = true
  g.add(beacon)
  return g
}

/** 璋冩牎鍙?*/
function buildTuningStation(status) {
  const g = new THREE.Group()
  const c = MACHINE_STATUS[status] || MACHINE_STATUS.IDLE
  const desk = box(2.0, 0.75, 1.2, mat(0x546e7a), 0, 0.375, 0)
  const lamp = box(1.4, 0.06, 0.25, mat(0xffffff, { emissive: 0xfff9c4, ei: 0.35 }), 0, 1.55, 0)
  const mon = createMonitor(0.85, status === 'RUNNING')
  mon.position.set(0, 0.78, 0.1)
  const tester = box(0.5, 0.35, 0.5, mat(c, { emissive: c, ei: 0.12 }), 0.65, 0.95, 0.2)
  tester.userData.isStatusLight = true
  g.add(desk, lamp, mon, tester)
  return g
}

/** 鍖呰绾?*/
function buildPackingLine(status) {
  const g = new THREE.Group()
  const c = MACHINE_STATUS[status] || MACHINE_STATUS.IDLE
  const line = box(3.0, 0.7, 1.4, mat(0x455a64), 0, 0.35, 0)
  const armBase = box(0.45, 0.9, 0.45, mat(0x546e7a), -1.1, 0.85, 0.35)
  const arm = box(1.2, 0.12, 0.12, mat(0x78909c), -0.5, 1.35, 0.35)
  arm.rotation.z = -0.35
  const carton = box(0.7, 0.55, 0.7, mat(0x8d6e63), 0.5, 0.95, 0)
  const packed = createMonitor(0.42, false)
  packed.position.set(0.5, 1.05, 0)
  const light = new THREE.Mesh(new THREE.SphereGeometry(0.1, 10, 10), mat(c, { emissive: c, ei: status === 'RUNNING' ? 0.7 : 0.12 }))
  light.position.set(1.35, 1.2, 0.55)
  light.userData.isStatusLight = true
  g.add(line, armBase, arm, carton, packed, light)
  g.userData.arm = arm
  return g
}

const MACHINE_BUILDERS = {
  display: buildDisplayLine,
  motherboard: buildMotherboardLine,
  powerboard: buildMotherboardLine,
  interface: buildMotherboardLine,
  attach: buildAttachMachine,
  shell: buildAttachMachine,
  assembly: buildAssemblyLine,
  bracket: buildAssemblyLine
}

function buildWorkshopBuilding(layout) {
  const group = new THREE.Group()
  group.userData.key = layout.key
  group.position.set(layout.x, 0, layout.z)
  group.rotation.y = layout.rot

  const { w, d, h, accent } = layout

  // 地坪 + 安全线 + 出料传送带接口
  const floor = box(w, 0.15, d, mat(0xe2e8f0, { rough: 0.9 }), 0, 0.075, 0)
  group.add(floor)
  const lineMat = mat(0xe8d48b, { emissive: 0xe8d48b, ei: 0.08 })
  group.add(box(w * 0.85, 0.02, 0.08, lineMat, 0, 0.16, d * 0.38))
  group.add(box(w * 0.85, 0.02, 0.08, lineMat, 0, 0.16, -d * 0.38))
  const outConv = createConveyorSegment(w * 0.35, 'x')
  outConv.position.set(w * 0.42, 0, 0)
  group.add(outConv)

  // 地面标识牌
  const sign = box(w * 0.55, 0.06, 0.12, mat(accent, { emissive: accent, ei: 0.2 }), 0, 0.2, -d / 2 + 0.35)
  group.add(sign)

  const interiorLight = new THREE.PointLight(accent, 0.45, w * 2.5)
  interiorLight.position.set(0, h * 0.45, 0)
  group.add(interiorLight)

  const machinesGroup = new THREE.Group()
  machinesGroup.position.y = 0.15
  group.add(machinesGroup)

  const showcaseGroup = new THREE.Group()
  showcaseGroup.position.y = 0.15
  group.add(showcaseGroup)

  const operatorsGroup = new THREE.Group()
  operatorsGroup.position.y = 0.15
  group.add(operatorsGroup)

  const labelEl = document.createElement('div')
  labelEl.className = 'workshop-3d-label'
  labelEl.innerHTML = buildLabelCardHtml()
  const label = new CSS2DObject(labelEl)
  label.position.set(0, h + 1.6, -d * 0.32)
  group.add(label)

  const glowRing = new THREE.Mesh(
    new THREE.RingGeometry(w * 0.42, w * 0.48, 32),
    mat(accent, { transparent: true, opacity: 0.18, emissive: accent, ei: 0.06 })
  )
  glowRing.rotation.x = -Math.PI / 2
  glowRing.position.y = 0.17
  glowRing.userData.isGlow = true
  group.add(glowRing)

  return { group, labelEl, machinesGroup, showcaseGroup, operatorsGroup, operators: [], layout, glowRing, interiorLight, status: 'pending', pulse: 0, data: null }
}

export function createWorkshopScene(container, { onSelectWorkshop, onHoverWorkshop }) {
  const width = container.clientWidth
  const height = container.clientHeight

  const scene = new THREE.Scene()
  scene.background = new THREE.Color(0xf4f6fa)
  scene.fog = new THREE.FogExp2(0xf4f6fa, 0.008)

  const camera = new THREE.PerspectiveCamera(36, width / height, 0.5, 280)
  camera.position.set(0, 24, 36)

  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMappingExposure = 1.05
  container.appendChild(renderer.domElement)

  const labelRenderer = new CSS2DRenderer()
  labelRenderer.setSize(width, height)
  labelRenderer.domElement.style.cssText = 'position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none;overflow:hidden'
  container.appendChild(labelRenderer.domElement)

  const controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.target.set(0, 2.2, 0)
  controls.minDistance = 16
  controls.maxDistance = 58
  controls.maxPolarAngle = Math.PI / 2.15
  controls.minPolarAngle = 0.25

  scene.add(new THREE.AmbientLight(0xffffff, 0.85))
  const sun = new THREE.DirectionalLight(0xffffff, 1.1)
  sun.position.set(15, 30, 20)
  sun.castShadow = true
  sun.shadow.mapSize.set(1024, 1024)
  scene.add(sun)
  const fill = new THREE.DirectionalLight(0xf0f4f8, 0.45)
  fill.position.set(-12, 18, -10)
  scene.add(fill)
  scene.add(new THREE.HemisphereLight(0xffffff, 0xe8ecf0, 0.65))

  const bounds = getLayoutBounds(WORKSHOP_LAYOUT)
  const padX = 8
  const padZ = 7
  const groundW = bounds.maxX - bounds.minX + padX * 2
  const groundD = bounds.maxZ - bounds.minZ + padZ * 2

  const ground = new THREE.Mesh(new THREE.PlaneGeometry(groundW, groundD), mat(0xe8ecf0, { rough: 0.95 }))
  ground.rotation.x = -Math.PI / 2
  ground.receiveShadow = true
  scene.add(ground)

  const grid = new THREE.GridHelper(groundW, Math.round(groundW / 2), 0xd8dee8, 0xeef1f6)
  grid.position.y = 0.02
  scene.add(grid)

  const road = box(groundW * 0.88, 0.04, 2.4, mat(0xdce1e9), 0, 0.03, 0)
  scene.add(road)

  // 工序分区标识线
  const { stageGap } = LAYOUT
  const startX = -((PRODUCTION_STAGES.length - 1) * stageGap) / 2
  PRODUCTION_STAGES.forEach((stage, idx) => {
    if (idx === 0) return
    const x = startX + idx * stageGap - stageGap / 2
    const divider = box(0.06, 0.06, groundD * 0.72, mat(STAGE_ACCENT[stage.stepKey] || 0x506784, { emissive: STAGE_ACCENT[stage.stepKey] || 0x506784, ei: 0.05 }), x, 0.04, 0)
    scene.add(divider)
  })

  buildPipelineConnections(scene, WORKSHOP_LAYOUT)
  buildSceneDecorations(scene, WORKSHOP_LAYOUT)

  const workshopMap = new Map()
  WORKSHOP_LAYOUT.forEach((layout) => {
    const built = buildWorkshopBuilding(layout)
    scene.add(built.group)
    workshopMap.set(layout.key, built)
  })

  const raycaster = new THREE.Raycaster()
  const pointer = new THREE.Vector2()
  let animId = null
  let t = 0
  let hoveredKey = ''

  const pickWorkshopKey = () => {
    raycaster.setFromCamera(pointer, camera)
    const roots = []
    workshopMap.forEach(({ group }) => roots.push(group))
    const hits = raycaster.intersectObjects(roots, true)
    if (!hits.length) return null
    return findWorkshopKeyFromObject(hits[0].object)
  }

  const setHover = (key) => {
    if (hoveredKey === key) return
    hoveredKey = key || ''
    workshopMap.forEach((ws, k) => {
      const hovered = k === hoveredKey
      ws.labelEl.classList.toggle('is-hovered', hovered)
      if (ws.data) {
        updateLabelCard(ws.labelEl, { ...ws.data, _hovered: hovered })
      }
    })
    onHoverWorkshop?.(hoveredKey ? workshopMap.get(hoveredKey)?.data : null)
  }

  const onClick = (e) => {
    const rect = container.getBoundingClientRect()
    pointer.x = ((e.clientX - rect.left) / rect.width) * 2 - 1
    pointer.y = -((e.clientY - rect.top) / rect.height) * 2 + 1
    const key = pickWorkshopKey()
    if (key) onSelectWorkshop?.(key)
  }
  container.addEventListener('click', onClick)

  const onMouseMove = (e) => {
    const rect = container.getBoundingClientRect()
    pointer.x = ((e.clientX - rect.left) / rect.width) * 2 - 1
    pointer.y = -((e.clientY - rect.top) / rect.height) * 2 + 1
    setHover(pickWorkshopKey() || '')
  }
  container.addEventListener('mousemove', onMouseMove)

  const onMouseLeave = () => setHover('')
  container.addEventListener('mouseleave', onMouseLeave)

  const onResize = () => {
    const w = container.clientWidth
    const h = container.clientHeight
    if (!w || !h) return
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h)
    labelRenderer.setSize(w, h)
  }
  window.addEventListener('resize', onResize)

  const animate = () => {
    animId = requestAnimationFrame(animate)
    t += 0.016
    controls.update()

    workshopMap.forEach((ws) => {
      if (ws.status === 'running' || ws.status === 'warning') {
        ws.pulse += 0.06
        const intensity = 0.25 + Math.sin(ws.pulse) * 0.15
        ws.group.traverse((obj) => {
          if (obj.userData?.isStatusLight && obj.material) {
            obj.material.emissiveIntensity = intensity
          }
        })
        if (ws.glowRing?.material) {
          ws.glowRing.material.opacity = 0.25 + Math.sin(ws.pulse) * 0.12
        }
      }
      ws.machinesGroup.children.forEach((machine) => {
        if (machine.userData.arm) {
          machine.userData.arm.rotation.z = -0.35 + Math.sin(t * 1.5) * 0.12
        }
        if (machine.userData.spinParts) {
          machine.userData.spinParts.forEach((p) => { p.position.x = Math.sin(t * 2) * 0.15 })
        }
      })
      if (ws.status === 'running') {
        ws.group.traverse((obj) => {
          if (obj.userData?.isScreen && obj.material) {
            obj.material.emissiveIntensity = 0.5 + Math.sin(t * 3 + ws.pulse) * 0.15
          }
        })
      }
      updateWorkshopOperators(ws, t, 0.016, camera)
    })

    updateCrossWorkshopInteractions(workshopMap, t)

    renderer.render(scene, camera)
    labelRenderer.render(scene, camera)
  }
  animate()

  const updateWorkshops = (workshops, selectedKey) => {
    mergeWorkshopData(workshops).forEach((data) => {
      const ws = workshopMap.get(data.key)
      if (!ws) return
      fillWorkshopContent(ws, data)
      ws.group.scale.setScalar(selectedKey === data.key ? 1.05 : 1)
    })
  }

  const dispose = () => {
    if (animId) cancelAnimationFrame(animId)
    window.removeEventListener('resize', onResize)
    container.removeEventListener('click', onClick)
    container.removeEventListener('mousemove', onMouseMove)
    container.removeEventListener('mouseleave', onMouseLeave)
    controls.dispose()
    renderer.dispose()
    renderer.domElement.remove()
    labelRenderer.domElement.remove()
    scene.traverse((obj) => {
      if (obj.geometry) obj.geometry.dispose()
      if (obj.material) {
        if (Array.isArray(obj.material)) obj.material.forEach((m) => m.dispose())
        else obj.material.dispose()
      }
    })
  }

  return { updateWorkshops, resize: onResize, dispose }
}
