<template>
  <div class="isc-scene">
    <canvas ref="canvasRef" class="isc-scene__canvas" />

    <!-- 悬浮设备标签 -->
    <div class="isc-scene__labels">
      <div
        v-for="item in labelItems"
        :key="item.id"
        class="isc-eq-tag"
        :class="[
          `isc-eq-tag--${item.level}`,
          { 'isc-eq-tag--active': selectedId === item.id, 'isc-eq-tag--expanded': item.expanded }
        ]"
        :style="{ left: item.x + 'px', top: item.y + 'px', opacity: item.visible ? 1 : 0 }"
        @click.stop="selectEq(item.id)"
      >
        <template v-if="item.expanded">
          <div class="isc-eq-card__head">
            <span class="isc-eq-card__code">{{ item.code }}</span>
            <button class="isc-eq-card__close" @click.stop="closeDetail">×</button>
          </div>
          <div class="isc-eq-card__name">{{ item.name }}</div>
          <div class="isc-eq-card__meta">{{ item.stageName || '—' }} · {{ item.workshopName || '—' }}</div>
          <div class="isc-eq-card__status">
            <span class="isc-eq-card__pill" :style="{ borderColor: item.statusColor, color: item.statusColor }">
              {{ item.statusCn }}
            </span>
            <span class="isc-eq-card__score" :style="{ color: item.levelColor }">健康 {{ item.score }}</span>
          </div>
          <div class="isc-eq-card__grid">
            <span>运行 <em>{{ item.runHours ?? '—' }}h</em></span>
            <span>报警7d <em :class="{ 'isc-eq-card__warn': item.alarm7d > 0 }">{{ item.alarm7d ?? 0 }}</em></span>
            <span>保养 <em>{{ item.daysSinceMaint ?? '—' }}天前</em></span>
            <span>缺陷30d <em>{{ item.faultCount30d ?? 0 }}</em></span>
          </div>
          <div v-if="item.advice" class="isc-eq-card__advice">{{ item.advice }}</div>
        </template>
        <template v-else>
          <span class="isc-eq-tag__dot" :style="{ background: item.statusColor }" />
          <span class="isc-eq-tag__code">{{ item.code }}</span>
          <span class="isc-eq-tag__status">{{ item.statusCn }}</span>
          <span class="isc-eq-tag__score" :style="{ color: item.levelColor }">{{ item.score }}</span>
        </template>
      </div>
    </div>

    <!-- 场景内工具条 -->
    <div class="isc-scene__toolbar">
      <button class="isc-tbtn" @click="resetCamera">复位</button>
      <button class="isc-tbtn" :class="{ 'isc-tbtn--on': autoRotate }" @click="autoRotate = !autoRotate">
        {{ autoRotate ? '停止旋转' : '自动旋转' }}
      </button>
      <span class="isc-tbtn isc-tbtn--hint">拖转 · 滚轮缩放 · 右键平移 · 点击设备查看信息</span>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import {
  createEquipmentMesh,
  createConveyorMesh,
  createBufferRack,
  createAgvMesh,
  createWorkstationPad,
  STATUS_COLOR,
  HEALTH_COLOR
} from './EquipmentMesh.js'
import { fetchEquipmentHealth } from '@/api/business'
import {
  buildEquipmentPositions,
  buildStageZones,
  buildSceneInfrastructure,
  isSceneEquipment
} from '@/utils/factorySceneLayout'

const emit = defineEmits(['select', 'deselect'])

const canvasRef = ref(null)
const autoRotate = ref(false)
const labelItems = ref([])
const selectedEq = ref(null)
const selectedId = ref(null)
const equipList = ref([])

let renderer, scene, camera, controls, animId, timer = null
const meshMap = new Map()
let layoutMap = {}

const STATUS_CN = { RUNNING: '运行', IDLE: '空闲', FAULT: '故障', MAINTAINING: '维保', SCRAPPED: '报废' }

function getPos(code) { return layoutMap[code] || null }
function rebuildLayout(list) { layoutMap = buildEquipmentPositions(list) }

function enrichEq(eq) {
  const pos = getPos(eq.equipmentCode)
  return {
    ...eq,
    stageName: pos?.stageName || eq.parentStepName || '',
    workshopName: pos?.workshopName || eq.workshop || ''
  }
}

function selectEq(id) {
  selectedId.value = id
  const eq = equipList.value.find((e) => e.equipmentId === id)
  selectedEq.value = eq ? enrichEq(eq) : null
  if (selectedEq.value) emit('select', selectedEq.value)
}

function closeDetail() {
  selectedId.value = null
  selectedEq.value = null
  emit('deselect')
}

function initScene() {
  const canvas = canvasRef.value
  if (!canvas) return
  const W = canvas.clientWidth || 900
  const H = canvas.clientHeight || 600

  renderer = new THREE.WebGLRenderer({ canvas, antialias: true })
  renderer.setSize(W, H, false)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setClearColor(0xf5f7fa, 1)
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.05

  scene = new THREE.Scene()
  scene.fog = new THREE.Fog(0xf5f7fa, 50, 95)

  camera = new THREE.PerspectiveCamera(44, W / H, 0.1, 200)
  camera.position.set(2, 7.5, 12)
  camera.lookAt(4, 0, 0)

  scene.add(new THREE.AmbientLight(0xffffff, 0.75))
  const key = new THREE.DirectionalLight(0xffffff, 1.15)
  key.position.set(12, 22, 14)
  key.castShadow = true
  key.shadow.mapSize.set(2048, 2048)
  key.shadow.camera.left = -30
  key.shadow.camera.right = 30
  key.shadow.camera.top = 20
  key.shadow.camera.bottom = -20
  scene.add(key)
  const rim = new THREE.DirectionalLight(0xd0e4f7, 0.4)
  rim.position.set(-10, 8, -8)
  scene.add(rim)

  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(62, 28),
    new THREE.MeshStandardMaterial({ color: 0xe8ecf0, roughness: 0.88, metalness: 0.06 })
  )
  floor.rotation.x = -Math.PI / 2
  floor.position.y = -0.52
  floor.receiveShadow = true
  scene.add(floor)

  const grid = new THREE.GridHelper(62, 62, 0xc8d6e8, 0xdce4ef)
  grid.position.y = -0.51
  scene.add(grid)

  buildIndustrialInfrastructure()
  buildStageZones().forEach((z) => buildZone(z))

  controls = new OrbitControls(camera, canvas)
  controls.enableDamping = true
  controls.dampingFactor = 0.06
  controls.minDistance = 5
  controls.maxDistance = 32
  controls.maxPolarAngle = Math.PI / 2.15
  controls.autoRotate = false
  controls.target.set(4, 0, 0)
  controls.update()

  canvas.addEventListener('click', onCanvasClick)
  animate()
}

function buildIndustrialInfrastructure() {
  const infra = buildSceneInfrastructure()

  infra.floorMarkings.forEach((m) => {
    const pts = [
      new THREE.Vector3(m.x1, 0.02, m.z1),
      new THREE.Vector3(m.x2, 0.02, m.z2)
    ]
    scene.add(new THREE.Line(
      new THREE.BufferGeometry().setFromPoints(pts),
      new THREE.LineBasicMaterial({ color: m.color, linewidth: 2 })
    ))
  })

  infra.conveyors.forEach((c) => {
    const seg = createConveyorMesh(c.length, c.axis)
    seg.position.set(c.x, -0.46, c.z)
    scene.add(seg)
  })

  infra.buffers.forEach((b) => {
    const rack = createBufferRack()
    rack.position.set(b.x, -0.46, b.z)
    scene.add(rack)
  })

  infra.workstations.forEach((ws) => {
    const pad = createWorkstationPad(ws.w, ws.d)
    pad.position.set(ws.x, 0, ws.z)
    scene.add(pad)
  })

  const agv = infra.agvZone
  const agvPad = new THREE.Mesh(
    new THREE.PlaneGeometry(agv.w, agv.d),
    new THREE.MeshBasicMaterial({ color: 0xe8f5e9, transparent: true, opacity: 0.85 })
  )
  agvPad.rotation.x = -Math.PI / 2
  agvPad.position.set(agv.x, -0.49, agv.z)
  scene.add(agvPad)
  for (let i = 0; i < 3; i++) {
    const v = createAgvMesh(i === 1 ? '#ff6f00' : '#ff8f00')
    v.position.set(agv.x - 1.2 + i * 1.2, 0, agv.z)
    scene.add(v)
  }
  addZoneLabel(agv.x, agv.z - agv.d / 2 - 0.5, agv.label, 0xff8f00)
}

function buildZone(zone) {
  const c = new THREE.Color(zone.color)
  const hw = zone.halfW
  const hd = zone.halfD
  const pts = [[-hw, -hd], [hw, -hd], [hw, hd], [-hw, hd], [-hw, -hd]]
    .map(([x, z]) => new THREE.Vector3(zone.x + x, 0.03, zone.z + z))
  scene.add(new THREE.Line(
    new THREE.BufferGeometry().setFromPoints(pts),
    new THREE.LineBasicMaterial({ color: c, transparent: true, opacity: 0.65 })
  ))
  const fill = new THREE.Mesh(
    new THREE.PlaneGeometry(hw * 2, hd * 2),
    new THREE.MeshBasicMaterial({ color: c, transparent: true, opacity: 0.07, side: THREE.DoubleSide })
  )
  fill.rotation.x = -Math.PI / 2
  fill.position.set(zone.x, -0.495, zone.z)
  scene.add(fill)
  addZoneLabel(zone.x, zone.z - hd - 0.45, `${zone.order}. ${zone.name}`, zone.color)
}

function addZoneLabel(x, z, text, colorHex) {
  const cv = document.createElement('canvas')
  cv.width = 512
  cv.height = 64
  const ctx = cv.getContext('2d')
  const hex = new THREE.Color(colorHex).getHexString()
  ctx.fillStyle = `#${hex}33`
  ctx.fillRect(0, 0, 512, 64)
  ctx.strokeStyle = `#${hex}`
  ctx.lineWidth = 2
  ctx.strokeRect(1, 1, 510, 62)
  ctx.fillStyle = `#${hex}`
  ctx.font = 'bold 26px "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(text, 256, 32)
  const board = new THREE.Mesh(
    new THREE.PlaneGeometry(3.6, 0.45),
    new THREE.MeshBasicMaterial({ map: new THREE.CanvasTexture(cv), transparent: true, side: THREE.DoubleSide })
  )
  board.position.set(x, 0.55, z)
  board.rotation.x = -0.15
  scene.add(board)
}

function placeEquipments(list) {
  const sceneEqs = (list || []).filter(isSceneEquipment)
  rebuildLayout(sceneEqs)
  const codes = new Set(sceneEqs.map((e) => e.equipmentCode))

  meshMap.forEach((entry, code) => {
    if (!codes.has(code)) {
      scene.remove(entry.group)
      meshMap.delete(code)
    }
  })

  sceneEqs.forEach((eq) => {
    const pos = getPos(eq.equipmentCode)
    if (!pos) return
    if (meshMap.has(eq.equipmentCode)) {
      meshMap.get(eq.equipmentCode).updateStatus(eq.status)
      return
    }
    const pad = createWorkstationPad(2.1, 1.7)
    pad.position.set(pos.x, 0, pos.z)
    scene.add(pad)

    const { group, updateStatus } = createEquipmentMesh(eq)
    group.position.set(pos.x, 0, pos.z)
    group.rotation.y = pos.rotY ?? 0
    group.scale.setScalar(1.75)
    group.userData = { equipmentId: eq.equipmentId, equipmentCode: eq.equipmentCode }
    group.traverse((o) => { if (o.isMesh) { o.castShadow = true; o.receiveShadow = true } })
    scene.add(group)
    meshMap.set(eq.equipmentCode, { group, updateStatus })
  })

  if (selectedId.value) {
    const hit = sceneEqs.find((e) => e.equipmentId === selectedId.value)
    if (hit) selectedEq.value = enrichEq(hit)
  }
}

const raycaster = new THREE.Raycaster()
const mouse = new THREE.Vector2()

function onCanvasClick(e) {
  const canvas = canvasRef.value
  if (!canvas) return
  const r = canvas.getBoundingClientRect()
  mouse.x = ((e.clientX - r.left) / r.width) * 2 - 1
  mouse.y = -((e.clientY - r.top) / r.height) * 2 + 1
  raycaster.setFromCamera(mouse, camera)
  const targets = []
  meshMap.forEach(({ group }) => group.traverse((o) => { if (o.isMesh) targets.push(o) }))
  const hits = raycaster.intersectObjects(targets, false)
  if (!hits.length) {
    closeDetail()
    return
  }
  let obj = hits[0].object
  while (obj && !obj.userData?.equipmentCode) obj = obj.parent
  if (!obj?.userData?.equipmentId) return
  selectEq(obj.userData.equipmentId)
  const pos = getPos(obj.userData.equipmentCode)
  if (pos) {
    flyTo(new THREE.Vector3(pos.x, 7, pos.z + 9), new THREE.Vector3(pos.x, 0, pos.z))
  }
}

function flyTo(camTarget, lookTarget) {
  const camStart = camera.position.clone()
  const lookStart = controls.target.clone()
  const t0 = performance.now()
  const dur = 650
  const tick = () => {
    const p = Math.min((performance.now() - t0) / dur, 1)
    const e = p < 0.5 ? 2 * p * p : -1 + (4 - 2 * p) * p
    camera.position.lerpVectors(camStart, camTarget, e)
    controls.target.lerpVectors(lookStart, lookTarget, e)
    controls.update()
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

function updateLabels() {
  const canvas = canvasRef.value
  if (!canvas || !camera) return
  const W = canvas.clientWidth
  const H = canvas.clientHeight
  labelItems.value = equipList.value.filter(isSceneEquipment).map((eq, idx) => {
    const pos = getPos(eq.equipmentCode)
    if (!pos) return null
    const enriched = enrichEq(eq)
    const expanded = selectedId.value === eq.equipmentId
    const anchorY = expanded ? 4.8 : 3.2 + (idx % 2) * 0.7
    const v = new THREE.Vector3(pos.x, anchorY, pos.z).project(camera)
    const x = (v.x * 0.5 + 0.5) * W
    const y = (-v.y * 0.5 + 0.5) * H
    const cardW = expanded ? 108 : 48
    const cardH = expanded ? 72 : 28
    return {
      id: eq.equipmentId,
      code: eq.equipmentCode,
      name: eq.equipmentName,
      stageName: enriched.stageName,
      workshopName: enriched.workshopName,
      statusCn: eq.statusCn || STATUS_CN[eq.status] || eq.status,
      score: eq.healthScore ?? '—',
      runHours: eq.runHours,
      alarm7d: eq.alarm7d,
      daysSinceMaint: eq.daysSinceMaint,
      faultCount30d: eq.faultCount30d,
      advice: eq.advice,
      level: (eq.healthLevel ?? 'GOOD').toLowerCase(),
      levelColor: HEALTH_COLOR[eq.healthLevel] ?? '#3dd68c',
      statusColor: STATUS_COLOR[eq.status] ?? '#52c1a2',
      expanded,
      x: x - cardW,
      y: y - cardH,
      visible: v.z < 1 && x > 60 && x < W - 60 && y > 30 && y < H - 30
    }
  }).filter(Boolean)
}

function animate() {
  animId = requestAnimationFrame(animate)
  controls.autoRotate = autoRotate.value
  controls.autoRotateSpeed = 0.35
  controls.update()
  renderer.render(scene, camera)
  updateLabels()
}

function resetCamera() {
  camera.position.set(2, 7.5, 12)
  controls.target.set(4, 0, 0)
  controls.update()
}

let resizeObserver = null

async function loadData() {
  try {
    const res = await fetchEquipmentHealth()
    const list = Array.isArray(res) ? res : (res.data ?? [])
    equipList.value = list
    if (scene) placeEquipments(list)
  } catch { /* ignore */ }
}

function onResize() {
  const c = canvasRef.value
  if (!c || !renderer || !camera) return
  const W = c.clientWidth
  const H = c.clientHeight
  renderer.setSize(W, H, false)
  camera.aspect = W / H
  camera.updateProjectionMatrix()
}

onMounted(async () => {
  await new Promise((r) => setTimeout(r, 30))
  initScene()
  await loadData()
  timer = setInterval(loadData, 30000)
  window.addEventListener('resize', onResize)
  if (canvasRef.value?.parentElement) {
    resizeObserver = new ResizeObserver(onResize)
    resizeObserver.observe(canvasRef.value.parentElement)
  }
})

onUnmounted(() => {
  cancelAnimationFrame(animId)
  canvasRef.value?.removeEventListener('click', onCanvasClick)
  controls?.dispose()
  renderer?.dispose()
  scene?.clear()
  meshMap.clear()
  clearInterval(timer)
  window.removeEventListener('resize', onResize)
  resizeObserver?.disconnect()
})

watch(autoRotate, (v) => { if (controls) controls.autoRotate = v })

function syncEquipments(list) {
  if (!Array.isArray(list) || !list.length) return
  equipList.value = list
  if (scene) placeEquipments(list)
}

defineExpose({ syncEquipments })
</script>

<style scoped>
.isc-scene {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #f5f7fa;
  overflow: hidden;
}

.isc-scene__canvas {
  width: 100%;
  height: 100%;
  display: block;
}

.isc-scene__labels {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.isc-eq-tag {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  background: #fff;
  border: 1px solid #d8dee8;
  font-size: 10px;
  font-family: Consolas, 'Courier New', monospace;
  color: #4a5568;
  pointer-events: all;
  cursor: pointer;
  white-space: nowrap;
  transition: border-color 0.15s, box-shadow 0.15s;
  box-shadow: 0 1px 4px rgba(0, 27, 63, 0.08);
}

.isc-eq-tag--expanded {
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
  width: 216px;
  padding: 8px 10px;
  white-space: normal;
  border-color: #3b82f6;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.18);
  z-index: 6;
  cursor: default;
}

.isc-eq-tag:hover,
.isc-eq-tag--active:not(.isc-eq-tag--expanded) {
  border-color: #3b82f6;
  background: #f0f7ff;
}

.isc-eq-tag--danger,
.isc-eq-tag--alert { border-color: #f56c6c; background: #fff5f5; }
.isc-eq-tag--warn { border-color: #e6a23c; background: #fffbf0; }

.isc-eq-tag__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.isc-eq-tag__code { font-weight: 700; color: #001b3f; }
.isc-eq-tag__status { color: #909399; font-size: 9px; }
.isc-eq-tag__score { font-weight: 700; margin-left: 2px; }

.isc-eq-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.isc-eq-card__code {
  font-size: 10px;
  color: #909399;
}

.isc-eq-card__close {
  width: 18px;
  height: 18px;
  border: 1px solid #e4e7ed;
  background: #f5f7fa;
  color: #909399;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 0;
}

.isc-eq-card__name {
  font-size: 13px;
  font-weight: 700;
  color: #001b3f;
  font-family: 'Microsoft YaHei', sans-serif;
}

.isc-eq-card__meta {
  font-size: 10px;
  color: #909399;
  font-family: 'Microsoft YaHei', sans-serif;
}

.isc-eq-card__status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.isc-eq-card__pill {
  padding: 1px 6px;
  border: 1px solid;
  font-size: 10px;
  background: #fafbfc;
  font-family: 'Microsoft YaHei', sans-serif;
}

.isc-eq-card__score {
  font-size: 11px;
  font-weight: 700;
}

.isc-eq-card__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px 8px;
  font-size: 10px;
  color: #909399;
  font-family: 'Microsoft YaHei', sans-serif;
}

.isc-eq-card__grid em {
  font-style: normal;
  color: #001b3f;
  font-weight: 600;
}

.isc-eq-card__warn { color: #f56c6c !important; }

.isc-eq-card__advice {
  font-size: 10px;
  color: #606266;
  line-height: 1.4;
  border-top: 1px dashed #eef1f5;
  padding-top: 4px;
  font-family: 'Microsoft YaHei', sans-serif;
}

.isc-scene__toolbar {
  position: absolute;
  bottom: 10px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 2;
}

.isc-tbtn {
  padding: 4px 12px;
  background: #fff;
  border: 1px solid #d8dee8;
  color: #4a5568;
  font-size: 11px;
  cursor: pointer;
}

.isc-tbtn:hover,
.isc-tbtn--on {
  border-color: #3b82f6;
  color: #1d4ed8;
  background: #f0f7ff;
}

.isc-tbtn--hint {
  border: none;
  background: transparent;
  color: #909399;
  cursor: default;
  padding-left: 4px;
}
</style>
