import * as THREE from 'three'

const MONITOR_SPECS = {
  '15.6': { screen: [0.42, 0.26, 0.04], color: 0x1a1a2e },
  '23.8': { screen: [0.52, 0.32, 0.04], color: 0x0f172a },
  '27': { screen: [0.58, 0.36, 0.04], color: 0x111827 },
  default: { screen: [0.48, 0.30, 0.04], color: 0x1e293b }
}

function monitorSize(materialName = '') {
  if (materialName.includes('15.6')) return MONITOR_SPECS['15.6']
  if (materialName.includes('23.8')) return MONITOR_SPECS['23.8']
  if (materialName.includes('27')) return MONITOR_SPECS['27']
  return MONITOR_SPECS.default
}

/** 成品仓：显示器模型 */
export function createMonitorMesh(materialName = '') {
  const spec = monitorSize(materialName)
  const group = new THREE.Group()
  const [sw, sh, sd] = spec.screen

  const stand = new THREE.Mesh(
    new THREE.BoxGeometry(0.14, 0.04, 0.12),
    new THREE.MeshStandardMaterial({ color: 0x2d3748, metalness: 0.55, roughness: 0.35 })
  )
  stand.position.y = 0.02
  group.add(stand)

  const neck = new THREE.Mesh(
    new THREE.BoxGeometry(0.04, 0.12, 0.04),
    new THREE.MeshStandardMaterial({ color: 0x4a5568, metalness: 0.5, roughness: 0.4 })
  )
  neck.position.y = 0.1
  group.add(neck)

  const bezel = new THREE.Mesh(
    new THREE.BoxGeometry(sw + 0.04, sh + 0.04, sd),
    new THREE.MeshStandardMaterial({ color: 0x1f2937, metalness: 0.35, roughness: 0.45 })
  )
  bezel.position.y = 0.22 + sh / 2
  group.add(bezel)

  const screen = new THREE.Mesh(
    new THREE.BoxGeometry(sw, sh, sd * 0.6),
    new THREE.MeshStandardMaterial({
      color: spec.color,
      emissive: 0x1e3a5f,
      emissiveIntensity: 0.35,
      metalness: 0.2,
      roughness: 0.25
    })
  )
  screen.position.set(0, 0.22 + sh / 2, sd * 0.25)
  group.add(screen)

  const glow = new THREE.Mesh(
    new THREE.PlaneGeometry(sw * 0.85, sh * 0.82),
    new THREE.MeshBasicMaterial({ color: 0x60a5fa, transparent: true, opacity: 0.12 })
  )
  glow.position.set(0, 0.22 + sh / 2, sd * 0.55)
  group.add(glow)

  group.scale.setScalar(0.95)
  return group
}

/** 原材料仓：按物料名称生成不同造型 */
export function createRawMaterialMesh(materialName = '') {
  const name = materialName || ''
  const group = new THREE.Group()

  if (/LCD|面板/i.test(name)) {
    const panel = new THREE.Mesh(
      new THREE.BoxGeometry(0.55, 0.38, 0.03),
      new THREE.MeshStandardMaterial({ color: 0x0b1220, metalness: 0.15, roughness: 0.3, emissive: 0x111827, emissiveIntensity: 0.2 })
    )
    panel.position.y = 0.2
    group.add(panel)
    const frame = new THREE.Mesh(
      new THREE.BoxGeometry(0.58, 0.41, 0.02),
      new THREE.MeshStandardMaterial({ color: 0x94a3b8, metalness: 0.6, roughness: 0.35 })
    )
    frame.position.set(0, 0.2, -0.02)
    group.add(frame)
    return group
  }

  if (/背光/i.test(name)) {
    const mod = new THREE.Mesh(
      new THREE.BoxGeometry(0.5, 0.34, 0.06),
      new THREE.MeshStandardMaterial({ color: 0xf8fafc, emissive: 0xfff7ed, emissiveIntensity: 0.45, roughness: 0.5 })
    )
    mod.position.y = 0.18
    group.add(mod)
    return group
  }

  if (/PCB|主板/i.test(name)) {
    const board = new THREE.Mesh(
      new THREE.BoxGeometry(0.48, 0.36, 0.025),
      new THREE.MeshStandardMaterial({ color: 0x166534, metalness: 0.1, roughness: 0.7 })
    )
    board.position.y = 0.16
    group.add(board)
    for (let i = 0; i < 6; i++) {
      const chip = new THREE.Mesh(
        new THREE.BoxGeometry(0.06, 0.02, 0.06),
        new THREE.MeshStandardMaterial({ color: 0x1e293b, metalness: 0.4, roughness: 0.4 })
      )
      chip.position.set(-0.14 + (i % 3) * 0.14, 0.19, -0.08 + Math.floor(i / 3) * 0.16)
      group.add(chip)
    }
    return group
  }

  if (/驱动|IC/i.test(name)) {
    const tray = new THREE.Mesh(
      new THREE.BoxGeometry(0.36, 0.04, 0.28),
      new THREE.MeshStandardMaterial({ color: 0x334155, metalness: 0.35, roughness: 0.5 })
    )
    tray.position.y = 0.08
    group.add(tray)
    for (let i = 0; i < 8; i++) {
      const ic = new THREE.Mesh(
        new THREE.BoxGeometry(0.05, 0.03, 0.05),
        new THREE.MeshStandardMaterial({ color: 0x0f172a, metalness: 0.5, roughness: 0.35 })
      )
      ic.position.set(-0.1 + (i % 4) * 0.07, 0.12, -0.08 + Math.floor(i / 4) * 0.14)
      group.add(ic)
    }
    return group
  }

  if (/边框|铝合金/i.test(name)) {
    const frame = new THREE.Mesh(
      new THREE.BoxGeometry(0.5, 0.36, 0.05),
      new THREE.MeshStandardMaterial({ color: 0xc0c8d4, metalness: 0.75, roughness: 0.28 })
    )
    frame.position.y = 0.18
    group.add(frame)
    return group
  }

  if (/电源|适配器/i.test(name)) {
    const brick = new THREE.Mesh(
      new THREE.BoxGeometry(0.22, 0.12, 0.32),
      new THREE.MeshStandardMaterial({ color: 0x1f2937, metalness: 0.3, roughness: 0.55 })
    )
    brick.position.y = 0.1
    group.add(brick)
    const plug = new THREE.Mesh(
      new THREE.BoxGeometry(0.08, 0.06, 0.06),
      new THREE.MeshStandardMaterial({ color: 0x64748b, metalness: 0.5, roughness: 0.4 })
    )
    plug.position.set(0, 0.08, 0.2)
    group.add(plug)
    return group
  }

  const crate = new THREE.Mesh(
    new THREE.BoxGeometry(0.38, 0.28, 0.38),
    new THREE.MeshStandardMaterial({ color: 0xb45309, roughness: 0.85 })
  )
  crate.position.y = 0.16
  group.add(crate)
  const strap = new THREE.Mesh(
    new THREE.BoxGeometry(0.4, 0.03, 0.4),
    new THREE.MeshStandardMaterial({ color: 0xfbbf24, roughness: 0.7 })
  )
  strap.position.y = 0.28
  group.add(strap)
  return group
}

export function createCargoMesh(zoneType, materialName, occupied) {
  if (!occupied) return null
  if (zoneType === 'FG') return createMonitorMesh(materialName)
  return createRawMaterialMesh(materialName)
}

/** 3×3 货架主体 */
export function buildShelfRack(rows = 3, cols = 3) {
  const rack = new THREE.Group()
  const cellW = 0.72
  const cellD = 0.62
  const shelfH = 0.42
  const totalW = cols * cellW + 0.2
  const totalD = cellD + 0.24
  const totalH = rows * shelfH + 0.12

  const postMat = new THREE.MeshStandardMaterial({ color: 0x64748b, metalness: 0.65, roughness: 0.35 })
  const shelfMat = new THREE.MeshStandardMaterial({ color: 0x94a3b8, metalness: 0.45, roughness: 0.5 })

  const posts = [
    [-totalW / 2, 0, -totalD / 2],
    [totalW / 2, 0, -totalD / 2],
    [-totalW / 2, 0, totalD / 2],
    [totalW / 2, 0, totalD / 2]
  ]
  posts.forEach(([x, , z]) => {
    const post = new THREE.Mesh(new THREE.BoxGeometry(0.05, totalH, 0.05), postMat)
    post.position.set(x, totalH / 2, z)
    rack.add(post)
  })

  for (let r = 0; r < rows; r++) {
    const y = 0.06 + r * shelfH
    const shelf = new THREE.Mesh(new THREE.BoxGeometry(totalW, 0.04, totalD), shelfMat)
    shelf.position.y = y
    shelf.castShadow = true
    shelf.receiveShadow = true
    rack.add(shelf)

    for (let c = 0; c < cols; c++) {
      const divider = new THREE.Mesh(new THREE.BoxGeometry(0.02, shelfH * 0.85, totalD * 0.92), postMat)
      const x = -totalW / 2 + cellW * (c + 0.5)
      divider.position.set(x, y + shelfH * 0.4, 0)
      if (c < cols - 1) rack.add(divider)
    }
  }

  rack.userData = { cellW, cellD, shelfH, totalW, totalD, rows, cols }
  return rack
}

export function cellWorldPosition(rack, row, col) {
  const { cellW, cellD, shelfH, totalW } = rack.userData
  const x = -totalW / 2 + cellW * (col + 0.5)
  const z = 0
  const y = 0.06 + row * shelfH + 0.06
  return new THREE.Vector3(x, y, z)
}

export function createHighlightRing() {
  const ring = new THREE.Mesh(
    new THREE.RingGeometry(0.22, 0.28, 32),
    new THREE.MeshBasicMaterial({ color: 0x38bdf8, transparent: true, opacity: 0.85, side: THREE.DoubleSide })
  )
  ring.rotation.x = -Math.PI / 2
  ring.position.y = 0.02
  return ring
}

export function createPallet() {
  const pallet = new THREE.Mesh(
    new THREE.BoxGeometry(0.58, 0.05, 0.48),
    new THREE.MeshStandardMaterial({ color: 0xd4a574, roughness: 0.9 })
  )
  pallet.position.y = 0.025
  return pallet
}
