import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'

const STATUS_COLOR = {
  normal: 0x1565c0,
  running: 0x43a047,
  pending: 0x78909c,
  warning: 0xef6c00,
  abnormal: 0xc62828,
  stopped: 0x8b0000
}

export const STATION_LAYOUT = [
  { key: 'order', x: -24, z: 0, type: 'order' },
  { key: 'plan', x: -18, z: 0, type: 'plan' },
  { key: 'warehouse', x: -12, z: -3, type: 'warehouse' },
  { key: 'panel', x: -6, z: 0, type: 'panel' },
  { key: 'assembly', x: 0, z: 0, type: 'assembly' },
  { key: 'aging', x: 6, z: 0, type: 'aging' },
  { key: 'quality', x: 12, z: 0, type: 'quality' },
  { key: 'packing', x: 17, z: 0, type: 'packing' },
  { key: 'alert', x: 12, z: -6, type: 'alert' },
  { key: 'delivery', x: 23, z: 0, type: 'delivery' }
]

function lambert(color, emissive = 0x000000, ei = 0) {
  return new THREE.MeshLambertMaterial({ color, emissive, emissiveIntensity: ei })
}

/** 电脑显示器实体：底座 + 支架 + 边框 + 屏幕 */
export function createMonitorModel(scale = 1, screenOn = true) {
  const g = new THREE.Group()
  const s = scale
  const base = new THREE.Mesh(new THREE.BoxGeometry(0.9 * s, 0.1 * s, 0.55 * s), lambert(0x37474f))
  base.position.y = 0.05 * s
  const neck = new THREE.Mesh(new THREE.BoxGeometry(0.12 * s, 0.45 * s, 0.1 * s), lambert(0x455a64))
  neck.position.y = 0.32 * s
  const bezel = new THREE.Mesh(new THREE.BoxGeometry(1.15 * s, 0.72 * s, 0.07 * s), lambert(0x212121))
  bezel.position.y = 0.78 * s
  const screen = new THREE.Mesh(
    new THREE.BoxGeometry(1.02 * s, 0.6 * s, 0.02 * s),
    lambert(
      screenOn ? 0x0d47a1 : 0x1a1a1a,
      screenOn ? 0x1565c0 : 0x000000,
      screenOn ? 0.35 : 0
    )
  )
  screen.position.set(0, 0.78 * s, 0.04 * s)
  const chin = new THREE.Mesh(new THREE.BoxGeometry(0.25 * s, 0.04 * s, 0.05 * s), lambert(0x616161))
  chin.position.set(0, 0.48 * s, 0.05 * s)
  g.add(base, neck, bezel, screen, chin)
  return g
}

function addStatusLight(group, x, y, z) {
  const light = new THREE.Mesh(
    new THREE.CylinderGeometry(0.12, 0.12, 0.06, 16),
    lambert(STATUS_COLOR.normal, STATUS_COLOR.normal, 0.2)
  )
  light.position.set(x, y, z)
  light.userData.isStatusLight = true
  group.add(light)
  return light
}

function buildWorkshopShell(scene) {
  const wallMat = lambert(0x1a2840)
  const wallH = 4.5
  const mkWall = (w, h, d, x, y, z) => {
    const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), wallMat)
    m.position.set(x, y, z)
    scene.add(m)
  }
  mkWall(72, wallH, 0.25, 0, wallH / 2, -14)
  mkWall(72, wallH, 0.25, 0, wallH / 2, 14)
  mkWall(0.25, wallH, 30, -36, wallH / 2, 0)
  mkWall(0.25, wallH, 30, 36, wallH / 2, 0)

  for (let x = -30; x <= 30; x += 15) {
    const pillar = new THREE.Mesh(new THREE.BoxGeometry(0.4, wallH, 0.4), lambert(0x24344d))
    pillar.position.set(x, wallH / 2, -13.5)
    scene.add(pillar)
  }

  for (let x = -28; x <= 28; x += 7) {
    const lamp = new THREE.Mesh(new THREE.BoxGeometry(2.5, 0.08, 0.4), lambert(0x37474f, 0xfff9c4, 0.15))
    lamp.position.set(x, 4.2, 0)
    scene.add(lamp)
  }

  const prodZone = new THREE.Mesh(
    new THREE.PlaneGeometry(52, 8),
    new THREE.MeshLambertMaterial({ color: 0x1e3350, transparent: true, opacity: 0.45 })
  )
  prodZone.rotation.x = -Math.PI / 2
  prodZone.position.set(0, 0.03, 0)
  scene.add(prodZone)

  const whZone = new THREE.Mesh(
    new THREE.PlaneGeometry(8, 8),
    new THREE.MeshLambertMaterial({ color: 0x2a3a28, transparent: true, opacity: 0.5 })
  )
  whZone.rotation.x = -Math.PI / 2
  whZone.position.set(-12, 0.035, -3)
  scene.add(whZone)
}

function buildConveyor(scene) {
  const belt = new THREE.Mesh(new THREE.BoxGeometry(54, 0.12, 1.4), lambert(0x455a64))
  belt.position.set(0, 0.1, 0)
  scene.add(belt)
  const railMat = lambert(0x78909c)
  ;[-0.75, 0.75].forEach((z) => {
    const rail = new THREE.Mesh(new THREE.BoxGeometry(54, 0.25, 0.08), railMat)
    rail.position.set(0, 0.22, z)
    scene.add(rail)
  })
  const rollers = []
  for (let x = -26; x <= 26; x += 1.2) {
    const roller = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.08, 1.2, 8), lambert(0x607d8b))
    roller.rotation.z = Math.PI / 2
    roller.position.set(x, 0.16, 0)
    scene.add(roller)
    rollers.push(roller)
  }
  return rollers
}

function buildStation(layout) {
  const group = new THREE.Group()
  group.userData.key = layout.key
  group.position.set(layout.x, 0, layout.z)
  const highlights = []
  const h = (mesh) => {
    highlights.push(mesh)
    return mesh
  }

  if (layout.type === 'order') {
    const desk = h(new THREE.Mesh(new THREE.BoxGeometry(2.8, 0.75, 1.4), lambert(0x546e7a)))
    desk.position.y = 0.375
    group.add(desk)
    const partition = new THREE.Mesh(new THREE.BoxGeometry(0.08, 1.8, 1.6), lambert(0x455a64))
    partition.position.set(-1.2, 0.9, 0)
    group.add(partition)
    const board = createMonitorModel(1.1, true)
    board.position.set(0, 0.75, -0.3)
    board.rotation.y = 0.15
    group.add(board)
  } else if (layout.type === 'plan') {
    const table = h(new THREE.Mesh(new THREE.BoxGeometry(2.6, 0.8, 1.3), lambert(0x546e7a)))
    table.position.y = 0.4
    group.add(table)
    for (let i = 0; i < 3; i++) {
      const doc = new THREE.Mesh(new THREE.BoxGeometry(0.35, 0.02, 0.5), lambert(0xeceff1))
      doc.position.set(-0.5 + i * 0.5, 0.82, 0.2)
      doc.rotation.x = -0.2
      group.add(doc)
    }
  } else if (layout.type === 'warehouse') {
    const frameL = new THREE.Mesh(new THREE.BoxGeometry(0.12, 3.2, 0.12), lambert(0x78909c))
    const frameR = frameL.clone()
    frameL.position.set(-1.8, 1.6, -1)
    frameR.position.set(1.8, 1.6, -1)
    group.add(frameL, frameR)
    for (let row = 0; row < 4; row++) {
      const shelf = h(new THREE.Mesh(new THREE.BoxGeometry(3.6, 0.08, 1.8), lambert(0x607d8b)))
      shelf.position.y = 0.5 + row * 0.75
      group.add(shelf)
      for (let col = 0; col < 3; col++) {
        const box = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.45, 0.5), lambert(0x8d6e63))
        box.position.set(-1 + col * 1, 0.75 + row * 0.75, 0.2)
        group.add(box)
      }
    }
    const panelStack = createMonitorModel(0.55, false)
    panelStack.position.set(0.8, 0.5, 0.5)
    panelStack.rotation.y = -0.4
    group.add(panelStack)
  } else if (layout.type === 'panel') {
    const bench = h(new THREE.Mesh(new THREE.BoxGeometry(3.2, 0.85, 1.6), lambert(0x546e7a)))
    bench.position.y = 0.425
    group.add(bench)
    const panel = new THREE.Mesh(new THREE.BoxGeometry(0.06, 1.4, 1), lambert(0x1a237e, 0x0d47a1, 0.1))
    panel.position.set(0, 1.1, -0.5)
    group.add(panel)
    const arm = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.06, 0.8, 8), lambert(0x78909c))
    arm.position.set(0.8, 1.2, 0)
    arm.rotation.z = Math.PI / 4
    group.add(arm)
  } else if (layout.type === 'assembly') {
    const bench = h(new THREE.Mesh(new THREE.BoxGeometry(3.8, 0.85, 2), lambert(0x546e7a)))
    bench.position.y = 0.425
    group.add(bench)
    const leg1 = new THREE.Mesh(new THREE.BoxGeometry(0.15, 0.85, 0.15), lambert(0x455a64))
    leg1.position.set(-1.6, 0.425, 0.8)
    group.add(leg1)
    const hero = createMonitorModel(1.25, true)
    hero.position.set(0, 0.85, 0.3)
    group.add(hero)
    group.userData.heroMonitor = hero
  } else if (layout.type === 'aging') {
    const rack = h(new THREE.Mesh(new THREE.BoxGeometry(3.2, 2.8, 1.2), lambert(0x455a64)))
    rack.position.y = 1.4
    group.add(rack)
    for (let row = 0; row < 3; row++) {
      for (let col = 0; col < 2; col++) {
        const m = createMonitorModel(0.42, true)
        m.position.set(-0.7 + col * 1.4, 0.6 + row * 0.85, 0.35)
        m.rotation.y = (col - 0.5) * 0.2
        group.add(m)
      }
    }
  } else if (layout.type === 'quality') {
    const table = h(new THREE.Mesh(new THREE.BoxGeometry(2.8, 0.8, 1.4), lambert(0x546e7a)))
    table.position.y = 0.4
    group.add(table)
    const lightBar = new THREE.Mesh(new THREE.BoxGeometry(2.2, 0.08, 0.2), lambert(0xffffff, 0xffffff, 0.25))
    lightBar.position.set(0, 1.6, 0)
    group.add(lightBar)
    const insp = createMonitorModel(0.75, true)
    insp.position.set(0, 0.8, 0.15)
    group.add(insp)
  } else if (layout.type === 'packing') {
    const table = h(new THREE.Mesh(new THREE.BoxGeometry(3, 0.8, 1.6), lambert(0x546e7a)))
    table.position.y = 0.4
    group.add(table)
    const carton = new THREE.Mesh(new THREE.BoxGeometry(0.9, 0.7, 0.7), lambert(0x8d6e63))
    carton.position.set(0.6, 0.85, 0.2)
    group.add(carton)
    const packed = createMonitorModel(0.5, false)
    packed.position.set(-0.4, 0.85, 0.2)
    group.add(packed)
  } else if (layout.type === 'alert') {
    const pole = h(new THREE.Mesh(new THREE.CylinderGeometry(0.15, 0.2, 2.2, 8), lambert(0x546e7a)))
    pole.position.y = 1.1
    group.add(pole)
    const beacon = new THREE.Mesh(new THREE.SphereGeometry(0.35, 12, 12), lambert(0xc62828, 0xc62828, 0.3))
    beacon.position.y = 2.4
    beacon.userData.isBeacon = true
    group.add(beacon)
    highlights.push(beacon)
  } else if (layout.type === 'delivery') {
    const dock = h(new THREE.Mesh(new THREE.BoxGeometry(3.5, 0.6, 2.5), lambert(0x546e7a)))
    dock.position.y = 0.3
    group.add(dock)
    const ramp = new THREE.Mesh(new THREE.BoxGeometry(2, 0.15, 1.5), lambert(0x78909c))
    ramp.position.set(0, 0.15, 1.8)
    ramp.rotation.x = -0.15
    group.add(ramp)
    for (let i = 0; i < 2; i++) {
      const pallet = new THREE.Mesh(new THREE.BoxGeometry(0.9, 0.15, 0.9), lambert(0x8d6e63))
      pallet.position.set(-0.5 + i * 1, 0.55, 0)
      group.add(pallet)
      const m = createMonitorModel(0.4, false)
      m.position.set(-0.5 + i * 1, 0.75, 0)
      group.add(m)
    }
  }

  const statusLight = addStatusLight(group, 0, 0.02, 1.2)
  highlights.push(statusLight)

  const labelEl = document.createElement('div')
  labelEl.className = 'factory-3d-label'
  labelEl.innerHTML = '<div class="factory-3d-label__name"></div><div class="factory-3d-label__meta"></div>'
  const label = new CSS2DObject(labelEl)
  label.position.set(0, 3.2, 0)
  group.add(label)

  const alertEl = document.createElement('div')
  alertEl.className = 'factory-3d-alert'
  alertEl.textContent = '!'
  const alertMarker = new CSS2DObject(alertEl)
  alertMarker.position.set(0, 3.8, 0)
  alertMarker.visible = false
  group.add(alertMarker)

  const products = new THREE.Group()
  group.add(products)

  return { group, highlights, labelEl, alertMarker, products, layout, pulse: 0, status: 'normal' }
}

export function createFactoryScene(container, { onSelectStation }) {
  const width = container.clientWidth
  const height = container.clientHeight

  const scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0a1628)
  scene.fog = new THREE.Fog(0x0a1628, 40, 90)

  const camera = new THREE.PerspectiveCamera(40, width / height, 0.5, 200)
  camera.position.set(2, 20, 34)

  const renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  container.appendChild(renderer.domElement)

  const labelRenderer = new CSS2DRenderer()
  labelRenderer.setSize(width, height)
  labelRenderer.domElement.style.cssText = 'position:absolute;top:0;left:0;pointer-events:none'
  container.appendChild(labelRenderer.domElement)

  const controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.06
  controls.target.set(0, 1, -1)
  controls.minDistance = 14
  controls.maxDistance = 58
  controls.maxPolarAngle = Math.PI / 2.15
  controls.minPolarAngle = 0.4

  scene.add(new THREE.AmbientLight(0xffffff, 0.5))
  const dir = new THREE.DirectionalLight(0xffffff, 0.8)
  dir.position.set(12, 28, 18)
  scene.add(dir)
  scene.add(new THREE.HemisphereLight(0x90caf9, 0x1a2840, 0.35))

  const floor = new THREE.Mesh(new THREE.PlaneGeometry(72, 32), lambert(0x1a2535))
  floor.rotation.x = -Math.PI / 2
  scene.add(floor)
  const grid = new THREE.GridHelper(72, 36, 0x2a4060, 0x152030)
  grid.position.y = 0.01
  scene.add(grid)

  buildWorkshopShell(scene)
  const rollers = buildConveyor(scene)

  const stationMap = new Map()
  STATION_LAYOUT.forEach((layout) => {
    const built = buildStation(layout)
    scene.add(built.group)
    stationMap.set(layout.key, built)
  })

  const raycaster = new THREE.Raycaster()
  const pointer = new THREE.Vector2()
  let selectedKey = null
  let animId = null
  let t = 0

  function pickStation(clientX, clientY) {
    const rect = container.getBoundingClientRect()
    pointer.x = ((clientX - rect.left) / rect.width) * 2 - 1
    pointer.y = -((clientY - rect.top) / rect.height) * 2 + 1
    raycaster.setFromCamera(pointer, camera)
    const meshes = []
    stationMap.forEach(({ group }) => group.traverse((c) => { if (c.isMesh) meshes.push(c) }))
    const hits = raycaster.intersectObjects(meshes, false)
    if (!hits.length) return null
    let obj = hits[0].object
    while (obj) {
      if (obj.userData?.key) return obj.userData.key
      obj = obj.parent
    }
    return null
  }

  const onClick = (e) => {
    const key = pickStation(e.clientX, e.clientY)
    if (key) onSelectStation?.(key)
  }
  container.addEventListener('click', onClick)

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
    rollers.forEach((r, i) => { r.rotation.x = t * 2 + i * 0.3 })
    stationMap.forEach((st) => {
      if (st.status === 'running') {
        st.pulse += 0.04
        const intensity = 0.15 + Math.sin(st.pulse) * 0.08
        st.highlights.forEach((m) => {
          if (m.material?.emissive) m.material.emissiveIntensity = intensity
        })
      }
      if (st.group.userData.heroMonitor) {
        st.group.userData.heroMonitor.rotation.y = Math.sin(t * 0.5) * 0.08
      }
    })
    renderer.render(scene, camera)
    labelRenderer.render(scene, camera)
  }
  animate()

  const updateStations = (stations, selected) => {
    selectedKey = selected
    stations.forEach((data) => {
      const st = stationMap.get(data.key)
      if (!st) return
      st.status = data.status
      const color = STATUS_COLOR[data.status] || STATUS_COLOR.normal
      st.highlights.forEach((mesh) => {
        mesh.material.color.setHex(color)
        mesh.material.emissive.setHex(color)
        mesh.material.emissiveIntensity = data.status === 'running' ? 0.12 : data.abnormal > 0 ? 0.1 : 0.02
      })
      st.labelEl.querySelector('.factory-3d-label__name').textContent = data.name
      st.labelEl.querySelector('.factory-3d-label__meta').textContent =
        `${data.active}/${data.total}${data.abnormal ? ' · 异常' + data.abnormal : ''}`
      st.alertMarker.visible = data.abnormal > 0
      st.products.clear()
      const count = Math.min(2, Math.max(0, Math.floor(data.active / 2)))
      for (let i = 0; i < count; i++) {
        const p = createMonitorModel(0.55, true)
        p.position.set(-0.5 + i * 1, 1.05, 1.1)
        st.products.add(p)
      }
      st.group.scale.setScalar(selectedKey === data.key ? 1.04 : 1)
    })
  }

  const dispose = () => {
    if (animId) cancelAnimationFrame(animId)
    window.removeEventListener('resize', onResize)
    container.removeEventListener('click', onClick)
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

  return { updateStations, resize: onResize, dispose }
}
