<template>
  <div class="shelf-scene">
    <canvas ref="canvasRef" class="shelf-scene__canvas" />
    <div class="shelf-scene__toolbar">
      <button type="button" class="shelf-scene__btn" @click="resetCamera">复位视角</button>
      <button type="button" class="shelf-scene__btn" :class="{ 'is-active': autoRotate }" @click="autoRotate = !autoRotate">
        {{ autoRotate ? '停止旋转' : '自动旋转' }}
      </button>
      <span class="shelf-scene__hint">拖转视角 · 滚轮缩放</span>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import {
  buildShelfRack,
  cellWorldPosition,
  createCargoMesh,
  createHighlightRing,
  createPallet
} from '@/utils/warehouseShelfMeshes'

const props = defineProps({
  location: { type: Object, required: true },
  zoneType: { type: String, default: 'FG' },
  focusSlotId: { type: String, default: '' },
  bins: { type: Array, default: () => [] }
})

const canvasRef = ref(null)
const autoRotate = ref(true)

let renderer, scene, camera, controls, animId, rackGroup, highlightRing

function disposeScene() {
  if (animId) cancelAnimationFrame(animId)
  controls?.dispose()
  renderer?.dispose()
  if (scene) {
    scene.traverse((obj) => {
      if (obj.geometry) obj.geometry.dispose()
      if (obj.material) {
        if (Array.isArray(obj.material)) obj.material.forEach((m) => m.dispose())
        else obj.material.dispose()
      }
    })
  }
  scene = null
}

function buildScene() {
  disposeScene()
  if (!canvasRef.value) return

  const width = canvasRef.value.clientWidth || 720
  const height = canvasRef.value.clientHeight || 420

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xe8eef5)
  scene.fog = new THREE.Fog(0xe8eef5, 8, 22)

  camera = new THREE.PerspectiveCamera(42, width / height, 0.1, 100)
  camera.position.set(2.8, 2.1, 3.4)

  renderer = new THREE.WebGLRenderer({ canvas: canvasRef.value, antialias: true, alpha: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.06
  controls.target.set(0, 0.9, 0)
  controls.maxPolarAngle = Math.PI / 2.05
  controls.minDistance = 2
  controls.maxDistance = 9

  const ambient = new THREE.AmbientLight(0xffffff, 0.55)
  scene.add(ambient)
  const key = new THREE.DirectionalLight(0xffffff, 0.9)
  key.position.set(4, 8, 5)
  key.castShadow = true
  scene.add(key)
  const fill = new THREE.DirectionalLight(0xbfd7ff, 0.35)
  fill.position.set(-3, 4, -2)
  scene.add(fill)

  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(12, 12),
    new THREE.MeshStandardMaterial({ color: 0xdbe4ef, roughness: 0.95 })
  )
  floor.rotation.x = -Math.PI / 2
  floor.receiveShadow = true
  scene.add(floor)

  const grid = new THREE.GridHelper(10, 20, 0xb8c5d6, 0xd5dee8)
  grid.position.y = 0.001
  scene.add(grid)

  rackGroup = new THREE.Group()
  const rack = buildShelfRack(3, 3)
  rackGroup.add(rack)

  const bins = props.bins || []
  bins.forEach((bin, idx) => {
    const row = Math.floor(idx / 3)
    const col = idx % 3
    const pos = cellWorldPosition(rack, row, col)

    const cellGroup = new THREE.Group()
    cellGroup.position.copy(pos)
    cellGroup.userData.slotId = bin.id

    const pallet = createPallet()
    cellGroup.add(pallet)

    const cargo = createCargoMesh(props.zoneType, bin.materialName, bin.occupied > 0)
    if (cargo) {
      cargo.position.y = 0.06
      cellGroup.add(cargo)
    } else {
      const emptyMark = new THREE.Mesh(
        new THREE.BoxGeometry(0.5, 0.02, 0.4),
        new THREE.MeshStandardMaterial({ color: 0xcbd5e1, transparent: true, opacity: 0.35 })
      )
      emptyMark.position.y = 0.08
      cellGroup.add(emptyMark)
    }

    rackGroup.add(cellGroup)
  })

  scene.add(rackGroup)
  highlightRing = createHighlightRing()
  scene.add(highlightRing)
  updateHighlight()

  const loop = () => {
    animId = requestAnimationFrame(loop)
    if (autoRotate.value && rackGroup) {
      rackGroup.rotation.y += 0.003
    }
    controls.update()
    renderer.render(scene, camera)
  }
  loop()
}

function updateHighlight() {
  if (!highlightRing || !rackGroup) return
  const focusId = props.focusSlotId
  if (!focusId) {
    highlightRing.visible = false
    return
  }
  let target = null
  rackGroup.children.forEach((child) => {
    if (child.userData?.slotId === focusId) target = child
  })
  if (!target) {
    highlightRing.visible = false
    return
  }
  highlightRing.visible = true
  const wp = new THREE.Vector3()
  target.getWorldPosition(wp)
  highlightRing.position.set(wp.x, wp.y + 0.02, wp.z)
}

function resetCamera() {
  if (!camera || !controls) return
  camera.position.set(2.8, 2.1, 3.4)
  controls.target.set(0, 0.9, 0)
  controls.update()
  if (rackGroup) rackGroup.rotation.y = 0
}

function onResize() {
  if (!canvasRef.value || !camera || !renderer) return
  const w = canvasRef.value.clientWidth
  const h = canvasRef.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

let resizeObserver
onMounted(() => {
  buildScene()
  resizeObserver = new ResizeObserver(onResize)
  if (canvasRef.value) resizeObserver.observe(canvasRef.value)
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  disposeScene()
})

watch(
  () => [props.bins, props.focusSlotId, props.zoneType],
  () => {
    buildScene()
  },
  { deep: true }
)

defineExpose({ resetCamera })
</script>

<style scoped>
.shelf-scene {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 400px;
  border-radius: 10px;
  overflow: hidden;
  background: linear-gradient(180deg, #edf2f7 0%, #dbe4ef 100%);
}

.shelf-scene__canvas {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 400px;
}

.shelf-scene__toolbar {
  position: absolute;
  left: 12px;
  bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.shelf-scene__btn {
  border: 1px solid rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.92);
  color: #334155;
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 16px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
}

.shelf-scene__btn.is-active {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.shelf-scene__hint {
  font-size: 11px;
  color: #64748b;
  background: rgba(255, 255, 255, 0.75);
  padding: 4px 10px;
  border-radius: 12px;
}
</style>
