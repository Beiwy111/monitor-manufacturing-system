import * as THREE from 'three'

export const STATUS_COLOR = {
  IDLE:        '#52c1a2',
  RUNNING:     '#3b82f6',
  FAULT:       '#ef4444',
  MAINTAINING: '#f59e0b',
  SCRAPPED:    '#9ca3af',
}
export const HEALTH_COLOR = {
  GOOD:   '#52b788',
  WARN:   '#f59e0b',
  ALERT:  '#ef4444',
  DANGER: '#b91c1c',
}

// 普通金属材质
function stdMat(hex) {
  return new THREE.MeshStandardMaterial({
    color: new THREE.Color(hex),
    metalness: 0.45,
    roughness: 0.55,
  })
}

// 发光材质，打 isGlow 标记供 updateStatus 识别
function glowMat(hex) {
  const m = new THREE.MeshStandardMaterial({
    color: new THREE.Color(hex),
    emissive: new THREE.Color(hex),
    emissiveIntensity: 0.75,
    metalness: 0.1,
    roughness: 0.3,
  })
  m.userData = { isGlow: true }
  return m
}

// ── 流水线 / 组装线 ──────────────────────────────────────────────
function buildConveyor(sc) {
  const g = new THREE.Group()
  const base = new THREE.Mesh(new THREE.BoxGeometry(2.8, 0.12, 0.75), stdMat('#b0bec5'))
  base.position.y = -0.5; g.add(base)
  const belt = new THREE.Mesh(new THREE.BoxGeometry(2.6, 0.05, 0.58), stdMat('#455a64'))
  belt.position.y = -0.42; g.add(belt)
  for (let i = 0; i < 11; i++) {
    const s = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.055, 0.58), stdMat('#37474f'))
    s.position.set(-1.25 + i * 0.25, -0.42, 0); g.add(s)
  }
  for (const x of [-1.1, 1.1]) for (const z of [-0.28, 0.28]) {
    const leg = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.05, 0.9, 8), stdMat('#78909c'))
    leg.position.set(x, -0.9, z); g.add(leg)
    const foot = new THREE.Mesh(new THREE.CylinderGeometry(0.09, 0.09, 0.04, 8), stdMat('#546e7a'))
    foot.position.set(x, -1.36, z); g.add(foot)
  }
  for (const z of [-0.4, 0.4]) {
    const p = new THREE.Mesh(new THREE.BoxGeometry(2.62, 0.18, 0.035), stdMat('#90a4ae'))
    p.position.set(0, -0.35, z); g.add(p)
  }
  for (const x of [-1.32, 1.32]) {
    const d = new THREE.Mesh(new THREE.CylinderGeometry(0.13, 0.13, 0.6, 14), stdMat('#607d8b'))
    d.rotation.z = Math.PI / 2; d.position.set(x, -0.42, 0); g.add(d)
  }
  const cab = new THREE.Mesh(new THREE.BoxGeometry(0.32, 0.72, 0.28), stdMat('#eceff1'))
  cab.position.set(-1.7, -0.22, 0); g.add(cab)
  const screen = new THREE.Mesh(new THREE.BoxGeometry(0.18, 0.22, 0.01), glowMat(sc))
  screen.position.set(-1.7, -0.14, 0.145); g.add(screen)
  return g
}

// ── 贴附机 / 调校台 ──────────────────────────────────────────────
function buildMachine(sc) {
  const g = new THREE.Group()
  const body = new THREE.Mesh(new THREE.BoxGeometry(0.95, 1.2, 0.75), stdMat('#eceff1'))
  g.add(body)
  for (let i = 0; i < 4; i++) {
    const sl = new THREE.Mesh(new THREE.BoxGeometry(0.48, 0.04, 0.01), stdMat('#b0bec5'))
    sl.position.set(0.06, 0.32 - i * 0.14, 0.38); g.add(sl)
  }
  const scr = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.32, 0.012), glowMat(sc))
  scr.position.set(0, 0.22, 0.382); g.add(scr)
  const top = new THREE.Mesh(new THREE.BoxGeometry(1.05, 0.06, 0.86), stdMat('#cfd8dc'))
  top.position.y = 0.63; g.add(top)
  const ab = new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.12, 0.14, 10), stdMat('#90a4ae'))
  ab.position.set(0.18, 0.72, 0.15); g.add(ab)
  const a1 = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.045, 0.55, 8), stdMat('#b0bec5'))
  a1.rotation.z = Math.PI / 5; a1.position.set(0.36, 0.92, 0.15); g.add(a1)
  const a2 = new THREE.Mesh(new THREE.CylinderGeometry(0.028, 0.035, 0.36, 8), stdMat('#cfd8dc'))
  a2.rotation.z = -Math.PI / 4.5; a2.position.set(0.6, 1.08, 0.15); g.add(a2)
  // 臂末端用 stdMat 但颜色跟状态一致（不发光，不被 updateStatus 染色）
  const head = new THREE.Mesh(new THREE.CylinderGeometry(0.07, 0.055, 0.055, 10), stdMat(sc))
  head.position.set(0.78, 1.17, 0.15); g.add(head)
  const foot = new THREE.Mesh(new THREE.BoxGeometry(1.15, 0.1, 0.9), stdMat('#b0bec5'))
  foot.position.y = -0.65; g.add(foot)
  return g
}

// ── 老化测试架 ────────────────────────────────────────────────────
function buildAging(sc) {
  const g = new THREE.Group()
  for (const x of [-0.62, 0.62]) for (const z of [-0.33, 0.33]) {
    const p = new THREE.Mesh(new THREE.BoxGeometry(0.055, 1.8, 0.055), stdMat('#90a4ae'))
    p.position.set(x, 0, z); g.add(p)
  }
  for (let i = 0; i < 4; i++) {
    const y = -0.65 + i * 0.46
    for (const z of [-0.33, 0.33]) {
      const b = new THREE.Mesh(new THREE.BoxGeometry(1.3, 0.035, 0.035), stdMat('#b0bec5'))
      b.position.set(0, y, z); g.add(b)
    }
    const shelf = new THREE.Mesh(new THREE.BoxGeometry(1.22, 0.03, 0.62), stdMat('#cfd8dc'))
    shelf.position.y = y; g.add(shelf)
    for (let j = 0; j < 3; j++) {
      const prod = new THREE.Mesh(new THREE.BoxGeometry(0.26, 0.19, 0.025), stdMat('#263238'))
      prod.position.set(-0.34 + j * 0.34, y + 0.115, 0.17); g.add(prod)
      const glow = new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.14, 0.01), glowMat(sc))
      glow.position.set(-0.34 + j * 0.34, y + 0.115, 0.183); g.add(glow)
    }
  }
  const ctrl = new THREE.Mesh(new THREE.BoxGeometry(0.19, 0.52, 0.24), stdMat('#eceff1'))
  ctrl.position.set(0.82, 0.08, 0); g.add(ctrl)
  const cp = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.28, 0.01), glowMat(sc))
  cp.position.set(0.82, 0.1, 0.125); g.add(cp)
  return g
}

// ── 包装线 / 通用 ─────────────────────────────────────────────────
function buildPacking(sc) {
  const g = new THREE.Group()
  const body = new THREE.Mesh(new THREE.BoxGeometry(1.45, 0.95, 0.88), stdMat('#eceff1'))
  g.add(body)
  const fun = new THREE.Mesh(new THREE.CylinderGeometry(0.28, 0.12, 0.48, 14), stdMat('#b0bec5'))
  fun.position.set(-0.44, 0.72, 0); g.add(fun)
  for (let i = 0; i < 3; i++) {
    const c = new THREE.Mesh(new THREE.BoxGeometry(0.24, 0.055, 0.055), stdMat('#90a4ae'))
    c.rotation.z = -0.22; c.position.set(0.78 + i * 0.018, 0.08 - i * 0.1, 0.22 - i * 0.1); g.add(c)
  }
  const pnl = new THREE.Mesh(new THREE.BoxGeometry(0.52, 0.45, 0.015), stdMat('#263238'))
  pnl.position.set(0.18, 0.04, 0.45); g.add(pnl)
  const gl = new THREE.Mesh(new THREE.BoxGeometry(0.4, 0.33, 0.01), glowMat(sc))
  gl.position.set(0.18, 0.04, 0.457); g.add(gl)
  const lid = new THREE.Mesh(new THREE.BoxGeometry(1.5, 0.055, 0.93), stdMat('#cfd8dc'))
  lid.position.y = 0.503; g.add(lid)
  for (let i = 0; i < 4; i++) {
    const s = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.035, 0.09, 8), stdMat('#78909c'))
    s.rotation.z = Math.PI / 2; s.position.set(-0.34 + i * 0.23, 0.49, 0.4); g.add(s)
  }
  const base = new THREE.Mesh(new THREE.BoxGeometry(1.6, 0.09, 1.0), stdMat('#b0bec5'))
  base.position.y = -0.52; g.add(base)
  for (const x of [-0.62, 0.62]) for (const z of [-0.38, 0.38]) {
    const w = new THREE.Mesh(new THREE.CylinderGeometry(0.065, 0.065, 0.055, 10), stdMat('#78909c'))
    w.position.set(x, -0.578, z); g.add(w)
  }
  return g
}

function pickBuilder(type) {
  const t = type ?? ''
  if (t.includes('流水') || t.includes('组装')) return buildConveyor
  if (t.includes('贴附') || t.includes('调校')) return buildMachine
  if (t.includes('老化') || t.includes('测试')) return buildAging
  return buildPacking
}

export function createEquipmentMesh(equipData) {
  const sc      = STATUS_COLOR[equipData.status] ?? STATUS_COLOR.IDLE
  const model   = pickBuilder(equipData.equipmentType)(sc)
  model.scale.setScalar(0.72)

  // 底盘发光圈
  const discMat = new THREE.MeshBasicMaterial({
    color: new THREE.Color(sc), transparent: true, opacity: 0.20, side: THREE.DoubleSide,
  })
  const disc = new THREE.Mesh(new THREE.CircleGeometry(0.78, 32), discMat)
  disc.rotation.x = -Math.PI / 2; disc.position.y = -0.51

  const group = new THREE.Group()
  group.add(model); group.add(disc)

  // 只收集打了 isGlow 标记的发光 mesh
  const glowMeshes = []
  model.traverse(o => {
    if (o.isMesh && o.material?.userData?.isGlow) glowMeshes.push(o)
  })

  function updateStatus(newStatus) {
    const c   = STATUS_COLOR[newStatus] ?? STATUS_COLOR.IDLE
    const col = new THREE.Color(c)
    glowMeshes.forEach(m => {
      m.material.color.set(col)
      m.material.emissive.set(col)
    })
    discMat.color.set(col)
  }

  return { group, updateStatus }
}
