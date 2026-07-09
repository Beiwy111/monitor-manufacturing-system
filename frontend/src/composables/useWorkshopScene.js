import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'

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

/** 五车间布局：略呈弧形排布，便于全景俯视 */
export const WORKSHOP_LAYOUT = [
  { key: 'attach', x: -20, z: 0, rot: 0, w: 9.2, d: 8.2, h: 5.2, accent: 0x1565c0, name: '贴附车间' },
  { key: 'assembly', x: -10, z: 0, rot: 0, w: 9.6, d: 8.6, h: 5.5, accent: 0x00838f, name: '组装车间' },
  { key: 'aging', x: 0, z: 0, rot: 0, w: 9.2, d: 8.2, h: 5.8, accent: 0x6a1b9a, name: '老化测试车间' },
  { key: 'tuning', x: 10, z: 0, rot: 0, w: 8.8, d: 8.2, h: 5.1, accent: 0xef6c00, name: '调校质检车间' },
  { key: 'packing', x: 20, z: 0, rot: 0, w: 9.2, d: 8.6, h: 5.2, accent: 0x2e7d32, name: '包装发货车间' }
]

/** 将 API 返回与 3D 布局合并（不注入假数据，仅补全布局 key） */
export function mergeWorkshopData(apiList) {
  return WORKSHOP_LAYOUT.map((layout) => {
    const hit = (apiList || []).find((w) => w.key === layout.key)
    if (!hit) {
      return {
        key: layout.key,
        name: layout.name,
        department: '',
        taskTitle: '',
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
        machines: []
      }
    }
    const progress = hit.progress ?? 0
    return {
      ...hit,
      key: layout.key,
      name: hit.name || layout.name,
      progress,
      completedQty: hit.completedQty ?? 0,
      plannedQty: hit.plannedQty ?? 0,
      progressLabel: hit.progressLabel || (hit.plannedQty ? `已完成 ${hit.completedQty ?? 0} / ${hit.plannedQty} 台` : '暂无派工任务'),
      machines: Array.isArray(hit.machines) ? hit.machines : [],
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
  labelEl.querySelector('.workshop-3d-card__task').textContent = data.taskTitle || '当前工序'
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
  attach: 0x3949ab,
  assembly: 0x00838f,
  aging: 0x7b1fa2,
  tuning: 0xff8f00,
  packing: 0x2e7d32
}

/** 各车间成品/在制品展示架 */
function addWorkshopShowcase(showcaseGroup, layout, data) {
  const { w, d } = layout
  const progress = data?.progress ?? 0
  const running = (data?.running ?? 0) > 0 || progress > 0
  const hue = WORKSHOP_SCREEN_HUES[layout.key] || 0x1565c0
  showcaseGroup.clear()

  if (layout.key === 'attach') {
    for (let i = 0; i < 3; i++) {
      const m = createMonitor(0.35, running, hue)
      m.position.set(-w * 0.28 + i * 0.55, 0.45, d * 0.22)
      m.rotation.y = -0.35
      showcaseGroup.add(m)
    }
  } else if (layout.key === 'assembly') {
    for (let i = 0; i < 4; i++) {
      const m = createMonitor(0.32, running || i < 2, hue)
      m.position.set(-w * 0.3 + (i % 2) * 0.7, 0.4 + Math.floor(i / 2) * 0.55, d * 0.25)
      showcaseGroup.add(m)
    }
  } else if (layout.key === 'aging') {
    const rack = box(1.6, 1.8, 0.35, mat(0x37474f, { transparent: true, opacity: 0.6 }), w * 0.28, 1.1, d * 0.18)
    showcaseGroup.add(rack)
    for (let row = 0; row < 2; row++) {
      for (let col = 0; col < 3; col++) {
        const on = running && (row * 3 + col) <= Math.floor(progress / 20)
        const m = createMonitor(0.28, on, hue)
        m.position.set(w * 0.28 - 0.55 + col * 0.45, 0.55 + row * 0.65, d * 0.18)
        showcaseGroup.add(m)
      }
    }
  } else if (layout.key === 'tuning') {
    const desk = box(1.8, 0.08, 0.9, mat(0x455a64), -w * 0.25, 0.35, d * 0.2)
    showcaseGroup.add(desk)
    const m1 = createMonitor(0.55, running, hue)
    m1.position.set(-w * 0.25, 0.75, d * 0.2)
    const m2 = createMonitor(0.45, running, 0x1565c0)
    m2.position.set(-w * 0.25 + 0.9, 0.65, d * 0.2)
    m2.rotation.y = 0.25
    showcaseGroup.add(m1, m2)
  } else if (layout.key === 'packing') {
    const finished = Math.max(1, Math.floor(progress / 25))
    for (let i = 0; i < Math.min(4, finished); i++) {
      const carton = box(0.45, 0.35, 0.45, mat(0x8d6e63), -w * 0.28 + (i % 2) * 0.55, 0.55, d * 0.22 + Math.floor(i / 2) * 0.5)
      showcaseGroup.add(carton)
      const m = createMonitor(0.25, false, hue)
      m.position.set(-w * 0.28 + (i % 2) * 0.55, 0.78, d * 0.22 + Math.floor(i / 2) * 0.5)
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
  const builder = MACHINE_BUILDERS[data.key] || buildAssemblyLine

  if (machines.length) {
    machines.forEach((m, idx) => {
      const mesh = builder(m.status || 'IDLE')
      const col = idx % 2
      const row = Math.floor(idx / 2)
      mesh.position.set(-1.2 + col * 2.4, 0, -0.8 + row * 1.6)
      mesh.scale.setScalar(1.05)
      ws.machinesGroup.add(mesh)
    })
  } else {
    const mesh = builder(data.running > 0 ? 'RUNNING' : 'IDLE')
    mesh.scale.setScalar(1.1)
    ws.machinesGroup.add(mesh)
  }

  addWorkshopShowcase(ws.showcaseGroup, ws.layout, data)

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

/** 组装线：传送带 + 工位 */
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

/** 老化架：多层显示器 */
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

/** 调校台 */
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

/** 包装线 */
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
  attach: buildAttachMachine,
  assembly: buildAssemblyLine,
  aging: buildAgingRack,
  tuning: buildTuningStation,
  packing: buildPackingLine
}

function buildWorkshopBuilding(layout) {
  const group = new THREE.Group()
  group.userData.key = layout.key
  group.position.set(layout.x, 0, layout.z)
  group.rotation.y = layout.rot

  const { w, d, h, accent } = layout

  // 地坪 + 安全线（开放式，无围挡）
  const floor = box(w, 0.15, d, mat(0x455a64, { rough: 0.85 }), 0, 0.075, 0)
  group.add(floor)
  const lineMat = mat(0xffd600, { emissive: 0xffd600, ei: 0.25 })
  group.add(box(w * 0.85, 0.02, 0.08, lineMat, 0, 0.16, d * 0.38))
  group.add(box(w * 0.85, 0.02, 0.08, lineMat, 0, 0.16, -d * 0.38))

  // 地面标识牌
  const sign = box(w * 0.55, 0.06, 0.12, mat(accent, { emissive: accent, ei: 0.2 }), 0, 0.2, -d / 2 + 0.35)
  group.add(sign)

  const interiorLight = new THREE.PointLight(accent, 1.2, w * 2.5)
  interiorLight.position.set(0, h * 0.45, 0)
  group.add(interiorLight)

  const machinesGroup = new THREE.Group()
  machinesGroup.position.y = 0.15
  group.add(machinesGroup)

  const showcaseGroup = new THREE.Group()
  showcaseGroup.position.y = 0.15
  group.add(showcaseGroup)

  const labelEl = document.createElement('div')
  labelEl.className = 'workshop-3d-label'
  labelEl.innerHTML = buildLabelCardHtml()
  const label = new CSS2DObject(labelEl)
  label.position.set(0, h + 1.25, -d * 0.32)
  group.add(label)

  const glowRing = new THREE.Mesh(
    new THREE.RingGeometry(w * 0.42, w * 0.48, 32),
    mat(accent, { transparent: true, opacity: 0.35, emissive: accent, ei: 0.2 })
  )
  glowRing.rotation.x = -Math.PI / 2
  glowRing.position.y = 0.17
  glowRing.userData.isGlow = true
  group.add(glowRing)

  return { group, labelEl, machinesGroup, showcaseGroup, layout, glowRing, interiorLight, status: 'pending', pulse: 0, data: null }
}

export function createWorkshopScene(container, { onSelectWorkshop, onHoverWorkshop }) {
  const width = container.clientWidth
  const height = container.clientHeight

  const scene = new THREE.Scene()
  scene.background = new THREE.Color(0x040c18)
  scene.fog = new THREE.FogExp2(0x040c18, 0.018)

  const camera = new THREE.PerspectiveCamera(34, width / height, 0.5, 250)
  camera.position.set(0, 21, 30)

  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMappingExposure = 1.35
  container.appendChild(renderer.domElement)

  const labelRenderer = new CSS2DRenderer()
  labelRenderer.setSize(width, height)
  labelRenderer.domElement.style.cssText = 'position:absolute;top:0;left:0;pointer-events:none'
  container.appendChild(labelRenderer.domElement)

  const controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.target.set(0, 2.2, 0)
  controls.minDistance = 18
  controls.maxDistance = 54
  controls.maxPolarAngle = Math.PI / 2.15
  controls.minPolarAngle = 0.25

  scene.add(new THREE.AmbientLight(0xb0c4de, 0.75))
  const sun = new THREE.DirectionalLight(0xffffff, 1.45)
  sun.position.set(15, 30, 20)
  sun.castShadow = true
  sun.shadow.mapSize.set(1024, 1024)
  scene.add(sun)
  const fill = new THREE.DirectionalLight(0x4fc3f7, 0.55)
  fill.position.set(-12, 18, -10)
  scene.add(fill)
  scene.add(new THREE.HemisphereLight(0x90caf9, 0x1a2836, 0.55))

  const ground = new THREE.Mesh(new THREE.PlaneGeometry(76, 34), mat(0x0a1420, { rough: 0.95 }))
  ground.rotation.x = -Math.PI / 2
  ground.receiveShadow = true
  scene.add(ground)

  const grid = new THREE.GridHelper(76, 38, 0x1e3a5f, 0x0d1828)
  grid.position.y = 0.02
  scene.add(grid)

  // 厂区道路
  const road = box(52, 0.04, 3, mat(0x1c2833), 0, 0.03, -11)
  scene.add(road)

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
    })

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
