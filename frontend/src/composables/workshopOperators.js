import * as THREE from 'three'
import { CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'
import { operatorsForWorkshop } from '@/utils/operatorWorkshop'

const OPERATOR_SHIRT_COLORS = [
  0x506784, 0x5a8f96, 0x5a9a7a, 0x6a8f7a, 0x7c6aad, 0x5b6e8c, 0x4f6d8a, 0x8a7a5a
]

const LABEL_SHOW_DIST = 26
const CHAT_COOLDOWN_SEC = 12

function shirtColorFor(username) {
  let h = 0
  for (let i = 0; i < username.length; i++) h = (h * 31 + username.charCodeAt(i)) >>> 0
  return OPERATOR_SHIRT_COLORS[h % OPERATOR_SHIRT_COLORS.length]
}

function mat(color, opts = {}) {
  return new THREE.MeshStandardMaterial({
    color,
    roughness: opts.rough ?? 0.72,
    metalness: opts.metal ?? 0.05,
    emissive: opts.emissive ?? 0x000000,
    emissiveIntensity: opts.ei ?? 0
  })
}

/**
 * 操作员模型：root 只负责位移（不旋转），body 负责朝向，标签挂在 root 上随人物移动但不随转身旋转。
 */
export function createOperatorFigure({ username, displayName }) {
  const root = new THREE.Group()
  root.userData.isOperator = true
  root.userData.username = username

  const body = new THREE.Group()
  root.add(body)

  const shirt = shirtColorFor(username)
  const bodyMat = mat(shirt)
  const pantsMat = mat(0x37474f)
  const skinMat = mat(0xe8b89a, { rough: 0.65 })
  const helmetMat = mat(0xf9a825, { rough: 0.45 })

  const legL = new THREE.Mesh(new THREE.BoxGeometry(0.11, 0.4, 0.13), pantsMat)
  legL.position.set(-0.09, 0.2, 0)
  legL.userData.isLeg = true
  const legR = legL.clone()
  legR.position.x = 0.09

  const torso = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.4, 0.18), bodyMat)
  torso.position.y = 0.62

  const head = new THREE.Mesh(new THREE.SphereGeometry(0.14, 10, 8), skinMat)
  head.position.y = 1.02

  const helmet = new THREE.Mesh(new THREE.BoxGeometry(0.26, 0.09, 0.24), helmetMat)
  helmet.position.y = 1.14

  const armL = new THREE.Mesh(new THREE.BoxGeometry(0.09, 0.34, 0.09), bodyMat)
  armL.position.set(-0.2, 0.66, 0)
  armL.userData.isArm = true
  const armR = armL.clone()
  armR.position.x = 0.2

  body.add(legL, legR, torso, head, helmet, armL, armR)

  const labelEl = document.createElement('div')
  labelEl.className = 'operator-3d-label'
  labelEl.textContent = displayName
  const label = new CSS2DObject(labelEl)
  label.position.set(0, 1.38, 0)
  label.center.set(0.5, 1)
  root.add(label)

  const chatEl = document.createElement('div')
  chatEl.className = 'operator-3d-chat'
  chatEl.style.display = 'none'
  chatEl.textContent = '💬'
  const chat = new CSS2DObject(chatEl)
  chat.position.set(0, 1.58, 0)
  chat.center.set(0.5, 1)
  root.add(chat)

  root.userData.body = body
  root.userData.parts = { legL, legR, armL, armR }
  root.userData.chatEl = chatEl
  root.userData.labelEl = labelEl
  root.userData.nameLabel = label
  root.userData.chatLabel = chat
  return root
}

function buildWaypoints(w, d) {
  return [
    new THREE.Vector3(0, 0, 0),
    new THREE.Vector3(w * 0.22, 0, -d * 0.18),
    new THREE.Vector3(-w * 0.2, 0, d * 0.16),
    new THREE.Vector3(w * 0.12, 0, d * 0.22),
    new THREE.Vector3(-w * 0.14, 0, -d * 0.2),
    new THREE.Vector3(0, 0, d * 0.08)
  ]
}

function getMachineWorkSlot(machineMesh, side = 1) {
  return {
    x: machineMesh.position.x + side * 0.85,
    z: machineMesh.position.z + 0.25,
    rotY: side > 0 ? -Math.PI / 2 : Math.PI / 2
  }
}

function setBodyFacing(agent, rotY) {
  if (agent.mesh.userData.body) {
    agent.mesh.userData.body.rotation.y = rotY
  }
}

function updateLabelVisibility(agent, camera) {
  const nameLabel = agent.mesh.userData.nameLabel
  const chatLabel = agent.mesh.userData.chatLabel
  if (!nameLabel || !camera) return

  const worldPos = new THREE.Vector3()
  agent.mesh.getWorldPosition(worldPos)
  worldPos.y += 1.38
  const dist = camera.position.distanceTo(worldPos)
  const near = dist < LABEL_SHOW_DIST
  const showName = near || agent.state === 'working' || agent.state === 'chatting'
  nameLabel.visible = showName
  if (chatLabel) {
    chatLabel.visible = agent.state === 'chatting' && near
  }
}

/** 根据车间数据布置操作员 */
export function populateWorkshopOperators(ws, data) {
  ws.operatorsGroup.clear()
  ws.operators = []

  const opDefs = operatorsForWorkshop(ws.layout.key)
  if (!opDefs.length) return

  const machines = ws.machinesGroup.children
  const runningSlots = []
  const runningCount = data.running ?? 0
  machines.forEach((mesh, idx) => {
    const mData = (data.machines || [])[idx]
    let isRunning = mData?.status === 'RUNNING'
    if (!isRunning && !mData && data.status === 'running' && idx === 0) isRunning = true
    if (!isRunning && mData?.status == null && idx < runningCount) isRunning = true
    if (isRunning) {
      runningSlots.push(getMachineWorkSlot(mesh, idx % 2 === 0 ? 1 : -1))
    }
  })

  const w = ws.layout.w
  const d = ws.layout.d
  const waypoints = buildWaypoints(w, d)

  opDefs.forEach((op, idx) => {
    const mesh = createOperatorFigure(op)
    const agent = {
      id: op.username,
      displayName: op.displayName,
      mesh,
      state: 'walking',
      walkPhase: Math.random() * Math.PI * 2,
      waypointIdx: idx % waypoints.length,
      waypoints,
      chatUntil: 0,
      chatCooldown: 0,
      chatPartner: null,
      workSlot: null
    }

    if (idx < runningSlots.length) {
      agent.state = 'working'
      agent.workSlot = runningSlots[idx]
      mesh.position.set(agent.workSlot.x, 0, agent.workSlot.z)
      setBodyFacing(agent, agent.workSlot.rotY)
      mesh.userData.labelEl.classList.add('is-working')
    } else {
      const wp = waypoints[agent.waypointIdx]
      mesh.position.set(wp.x, 0, wp.z)
      const next = waypoints[(agent.waypointIdx + 1) % waypoints.length]
      setBodyFacing(agent, Math.atan2(next.x - wp.x, next.z - wp.z))
    }

    ws.operatorsGroup.add(mesh)
    ws.operators.push(agent)
  })
}

function animateLimbs(agent, t, mode) {
  const { legL, legR, armL, armR } = agent.mesh.userData.parts
  if (!legL) return
  if (mode === 'walk') {
    const s = Math.sin(agent.walkPhase) * 0.55
    legL.rotation.x = s
    legR.rotation.x = -s
    armL.rotation.x = -s * 0.7
    armR.rotation.x = s * 0.7
  } else if (mode === 'work') {
    legL.rotation.x = 0
    legR.rotation.x = 0
    armL.rotation.x = -0.35
    armR.rotation.x = -0.9 + Math.sin(t * 5 + agent.walkPhase) * 0.25
  } else {
    legL.rotation.x = 0
    legR.rotation.x = 0
    armL.rotation.x = Math.sin(t * 2) * 0.15
    armR.rotation.x = Math.sin(t * 2 + 1) * 0.15
  }
}

function moveToward(pos, target, speed, dt) {
  const dx = target.x - pos.x
  const dz = target.z - pos.z
  const dist = Math.sqrt(dx * dx + dz * dz)
  if (dist < 0.08) return true
  const step = Math.min(speed * dt, dist)
  pos.x += (dx / dist) * step
  pos.z += (dz / dist) * step
  return false
}

/** 每帧更新车间内操作员动画 */
export function updateWorkshopOperators(ws, t, dt = 0.016, camera = null) {
  if (!ws.operators?.length) return

  ws.operators.forEach((agent) => {
    const mesh = agent.mesh
    const chatEl = mesh.userData.chatEl

    if (agent.state === 'chatting') {
      if (t >= agent.chatUntil) {
        agent.state = agent.workSlot ? 'working' : 'walking'
        agent.chatPartner = null
        if (chatEl) chatEl.style.display = 'none'
        mesh.userData.labelEl?.classList.remove('is-chatting')
      } else {
        animateLimbs(agent, t, 'chat')
        updateLabelVisibility(agent, camera)
        return
      }
    }

    if (agent.state === 'working') {
      animateLimbs(agent, t, 'work')
      updateLabelVisibility(agent, camera)
      return
    }

    agent.walkPhase += dt * 6
    const nextIdx = (agent.waypointIdx + 1) % agent.waypoints.length
    const next = agent.waypoints[nextIdx]
    const arrived = moveToward(mesh.position, next, 1.1, dt)
    setBodyFacing(agent, Math.atan2(next.x - mesh.position.x, next.z - mesh.position.z))
    animateLimbs(agent, t, 'walk')

    if (arrived) {
      agent.waypointIdx = nextIdx
    }
    updateLabelVisibility(agent, camera)
  })

  const ops = ws.operators
  for (let i = 0; i < ops.length; i++) {
    for (let j = i + 1; j < ops.length; j++) {
      const a = ops[i]
      const b = ops[j]
      if (a.state === 'chatting' || b.state === 'chatting') continue
      if (a.state === 'working' || b.state === 'working') continue
      if (a.chatCooldown > t || b.chatCooldown > t) continue
      const dx = a.mesh.position.x - b.mesh.position.x
      const dz = a.mesh.position.z - b.mesh.position.z
      if (dx * dx + dz * dz > 1.0) continue
      if (Math.random() < 0.002) {
        startChat(a, b, t)
      }
    }
  }
}

function startChat(a, b, t) {
  a.state = 'chatting'
  b.state = 'chatting'
  a.chatPartner = b.id
  b.chatPartner = a.id
  a.chatUntil = t + 3
  b.chatUntil = t + 3
  a.chatCooldown = t + CHAT_COOLDOWN_SEC
  b.chatCooldown = t + CHAT_COOLDOWN_SEC

  const pa = new THREE.Vector3()
  const pb = new THREE.Vector3()
  a.mesh.getWorldPosition(pa)
  b.mesh.getWorldPosition(pb)
  setBodyFacing(a, Math.atan2(pb.x - pa.x, pb.z - pa.z))
  setBodyFacing(b, Math.atan2(pa.x - pb.x, pa.z - pb.z))

  if (a.mesh.userData.chatEl) {
    a.mesh.userData.chatEl.style.display = 'block'
    a.mesh.userData.chatEl.textContent = '交接'
  }
  if (b.mesh.userData.chatEl) {
    b.mesh.userData.chatEl.style.display = 'block'
    b.mesh.userData.chatEl.textContent = '确认'
  }
  a.mesh.userData.labelEl?.classList.add('is-chatting')
  b.mesh.userData.labelEl?.classList.add('is-chatting')
}

/** 跨车间过道偶遇交互（同工序相邻车间） */
export function updateCrossWorkshopInteractions(workshopMap, t) {
  const all = []
  workshopMap.forEach((ws) => {
    ws.operators?.forEach((op) => {
      if (op.state === 'walking' && op.chatCooldown <= t) {
        const worldPos = new THREE.Vector3()
        op.mesh.getWorldPosition(worldPos)
        all.push({ op, ws, worldPos })
      }
    })
  })

  for (let i = 0; i < all.length; i++) {
    for (let j = i + 1; j < all.length; j++) {
      const a = all[i]
      const b = all[j]
      if (a.ws.layout.key === b.ws.layout.key) continue
      if (a.ws.layout.parentStepKey !== b.ws.layout.parentStepKey) continue
      const dx = a.worldPos.x - b.worldPos.x
      const dz = a.worldPos.z - b.worldPos.z
      if (dx * dx + dz * dz > 3.5) continue
      if (Math.random() < 0.001) {
        startChat(a.op, b.op, t)
      }
    }
  }
}
