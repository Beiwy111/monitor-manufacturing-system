<template>
  <teleport to="body">
    <div v-if="modelValue" class="viewer-mask" @click.self="close">
      <div class="viewer-box">
        <!-- 头部 -->
        <div class="viewer-head">
          <div class="viewer-head__left">
            <div class="viewer-eq-info">
              <span class="viewer-code">{{ data?.equipmentCode }}</span>
              <span class="viewer-name">{{ data?.equipmentName }}</span>
              <el-tag :type="levelTagType" size="small" style="margin-left:10px">
                健康度 {{ data?.healthScore }}
              </el-tag>
            </div>
            <div class="viewer-hint">🖱 拖动旋转 · 滚轮缩放 · 右键平移</div>
          </div>
          <button class="viewer-close" @click="close">✕</button>
        </div>

        <!-- 3D canvas -->
        <canvas ref="canvasRef" class="viewer-canvas" />

        <!-- 底部工具栏 -->
        <div class="viewer-footer">
          <button class="vf-btn" @click="resetCamera">复位</button>
          <button class="vf-btn" :class="{ active: autoRotate }" @click="autoRotate = !autoRotate">
            {{ autoRotate ? '⏸ 停止旋转' : '▶ 自动旋转' }}
          </button>
          <div class="vf-divider" />
          <span class="vf-label">线框</span>
          <el-switch v-model="wireframe" size="small" @change="toggleWireframe" />
          <div class="vf-divider" />
          <div class="vf-score">
            <span class="vf-score__label">健康度</span>
            <div class="vf-score__bar">
              <div class="vf-score__fill"
                :style="{ width: (data?.healthScore ?? 0) + '%', background: levelColor }" />
            </div>
            <span class="vf-score__val" :style="{ color: levelColor }">{{ data?.healthScore ?? 0 }}</span>
          </div>
          <div class="vf-deducts">
            <span v-for="d in deductTips" :key="d.label" class="vf-deduct-tag"
              :class="d.val > 0 ? 'is-bad' : 'is-ok'">
              {{ d.label }} {{ d.val > 0 ? '-' + d.val : '✓' }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

const props = defineProps({ modelValue: { type: Boolean, default: false }, data: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue'])
const canvasRef = ref(null)
const autoRotate = ref(true)
const wireframe = ref(false)

const LEVEL_COLOR = { GOOD: '#67c23a', WARN: '#e6a23c', ALERT: '#f56c6c', DANGER: '#c0392b' }
const LEVEL_TAG   = { GOOD: 'success', WARN: 'warning', ALERT: 'danger', DANGER: 'danger' }
const levelColor   = computed(() => LEVEL_COLOR[props.data?.healthLevel] ?? '#67c23a')
const levelTagType = computed(() => LEVEL_TAG[props.data?.healthLevel]   ?? 'success')
const deductTips = computed(() => {
  const d = props.data; if (!d) return []
  return [{ label: '超时运行', val: d.deductRun??0 },{ label: '高频报警', val: d.deductAlarm??0 },{ label: '逾期保养', val: d.deductMaint??0 },{ label: '关联缺陷', val: d.deductNc??0 }]
})

let renderer, scene, camera, controls, modelGroup, animId, glowMeshes = []
const stdMat = (hex) => new THREE.MeshStandardMaterial({ color: new THREE.Color(hex), metalness: 0.55, roughness: 0.38 })
const glowMat = (hex) => new THREE.MeshStandardMaterial({ color: new THREE.Color(hex), emissive: new THREE.Color(hex), emissiveIntensity: 0.9, metalness: 0.15, roughness: 0.25 })

function buildConveyor(gc) {
  const g = new THREE.Group()
  const base = new THREE.Mesh(new THREE.BoxGeometry(3.2,.14,.9),stdMat('#2c3e50')); base.position.y=-.62; g.add(base)
  const belt = new THREE.Mesh(new THREE.BoxGeometry(3.0,.06,.72),stdMat('#1a252f')); belt.position.y=-.52; g.add(belt)
  for(let i=0;i<13;i++){const s=new THREE.Mesh(new THREE.BoxGeometry(.05,.07,.72),stdMat('#2d4059'));s.position.set(-1.44+i*.24,-.515,0);g.add(s)}
  for(const x of[-1.3,1.3])for(const z of[-.32,.32]){
    const leg=new THREE.Mesh(new THREE.CylinderGeometry(.045,.055,1.1,8),stdMat('#5d7080'));leg.position.set(x,-1.05,z);g.add(leg)
    const foot=new THREE.Mesh(new THREE.CylinderGeometry(.1,.1,.05,8),stdMat('#3a4a55'));foot.position.set(x,-1.62,z);g.add(foot)
  }
  for(const z of[-.46,.46]){const s=new THREE.Mesh(new THREE.BoxGeometry(3.05,.22,.04),stdMat('#3a5068'));s.position.set(0,-.42,z);g.add(s)}
  for(const x of[-1.52,1.52]){const d=new THREE.Mesh(new THREE.CylinderGeometry(.15,.15,.74,16),stdMat('#7f8c8d'));d.rotation.z=Math.PI/2;d.position.set(x,-.52,0);g.add(d)}
  const cab=new THREE.Mesh(new THREE.BoxGeometry(.38,.9,.32),stdMat('#34495e'));cab.position.set(-1.82,-.2,0);g.add(cab)
  const scr=new THREE.Mesh(new THREE.BoxGeometry(.22,.28,.02),stdMat('#0d1b2a'));scr.position.set(-1.82,-.08,.165);g.add(scr)
  const gl=new THREE.Mesh(new THREE.BoxGeometry(.16,.18,.01),glowMat(gc));gl.position.set(-1.82,-.08,.176);g.add(gl);glowMeshes.push(gl)
  for(const x of[-.9,0,.9]){const p=new THREE.Mesh(new THREE.CylinderGeometry(.025,.025,.42,8),stdMat('#7f8c8d'));p.position.set(x,-.1,.46);g.add(p)}
  const rail=new THREE.Mesh(new THREE.BoxGeometry(2.8,.025,.025),stdMat('#95a5a6'));rail.position.set(0,.12,.46);g.add(rail)
  return g
}

function buildMachine(gc) {
  const g = new THREE.Group()
  const body=new THREE.Mesh(new THREE.BoxGeometry(1.1,1.4,.85),stdMat('#2c3e50'));g.add(body)
  for(let i=0;i<5;i++){const sl=new THREE.Mesh(new THREE.BoxGeometry(.55,.05,.01),stdMat('#1a252f'));sl.position.set(.08,.38-i*.16,.435);g.add(sl)}
  const scr=new THREE.Mesh(new THREE.BoxGeometry(.58,.38,.015),stdMat('#0d1b2a'));scr.position.set(0,.25,.435);g.add(scr)
  const gl=new THREE.Mesh(new THREE.BoxGeometry(.5,.3,.01),glowMat(gc));gl.position.set(0,.25,.441);g.add(gl);glowMeshes.push(gl)
  const top=new THREE.Mesh(new THREE.BoxGeometry(1.22,.07,1.0),stdMat('#34495e'));top.position.y=.735;g.add(top)
  const ab=new THREE.Mesh(new THREE.CylinderGeometry(.12,.14,.16,12),stdMat('#5d6d7e'));ab.position.set(.22,.81,.18);g.add(ab)
  const a1=new THREE.Mesh(new THREE.CylinderGeometry(.04,.05,.62,8),stdMat('#85929e'));a1.rotation.z=Math.PI/5;a1.position.set(.44,1.05,.18);g.add(a1)
  const a2=new THREE.Mesh(new THREE.CylinderGeometry(.03,.04,.42,8),stdMat('#aab7c4'));a2.rotation.z=-Math.PI/4.5;a2.position.set(.72,1.24,.18);g.add(a2)
  const hd=new THREE.Mesh(new THREE.CylinderGeometry(.08,.06,.06,12),stdMat(gc));hd.position.set(.92,1.35,.18);g.add(hd)
  const foot=new THREE.Mesh(new THREE.BoxGeometry(1.35,.12,1.05),stdMat('#1a252f'));foot.position.y=-.76;g.add(foot)
  for(const x of[-.5,.5])for(const z of[-.38,.38]){const w=new THREE.Mesh(new THREE.CylinderGeometry(.07,.07,.05,12),stdMat('#2c3e50'));w.position.set(x,-.83,z);g.add(w)}
  return g
}

function buildAging(gc) {
  const g = new THREE.Group()
  for(const x of[-.72,.72])for(const z of[-.38,.38]){const p=new THREE.Mesh(new THREE.BoxGeometry(.06,2.0,.06),stdMat('#2c3e50'));p.position.set(x,0,z);g.add(p)}
  for(let i=0;i<4;i++){
    for(const z of[-.38,.38]){const b=new THREE.Mesh(new THREE.BoxGeometry(1.5,.04,.04),stdMat('#34495e'));b.position.set(0,-.75+i*.52,z);g.add(b)}
    const shelf=new THREE.Mesh(new THREE.BoxGeometry(1.42,.035,.72),stdMat('#2d3f50'));shelf.position.y=-.72+i*.52;g.add(shelf)
    for(let j=0;j<3;j++){
      const prod=new THREE.Mesh(new THREE.BoxGeometry(.3,.22,.03),stdMat('#0d1b2a'));prod.position.set(-.38+j*.38,-.6+i*.52,.2);g.add(prod)
      const s=new THREE.Mesh(new THREE.BoxGeometry(.24,.16,.01),glowMat(gc));s.position.set(-.38+j*.38,-.6+i*.52,.215);g.add(s);glowMeshes.push(s)
    }
  }
  const ctrl=new THREE.Mesh(new THREE.BoxGeometry(.22,.6,.28),stdMat('#2c3e50'));ctrl.position.set(.92,.1,0);g.add(ctrl)
  const panel=new THREE.Mesh(new THREE.BoxGeometry(.14,.32,.01),glowMat(gc));panel.position.set(.92,.12,.145);g.add(panel);glowMeshes.push(panel)
  return g
}

function buildPacking(gc) {
  const g = new THREE.Group()
  const body=new THREE.Mesh(new THREE.BoxGeometry(1.6,1.1,1.0),stdMat('#2c3e50'));g.add(body)
  const fun=new THREE.Mesh(new THREE.CylinderGeometry(.32,.14,.55,16),stdMat('#34495e'));fun.position.set(-.52,.82,0);g.add(fun)
  for(let i=0;i<3;i++){const c=new THREE.Mesh(new THREE.BoxGeometry(.28,.06,.06),stdMat('#5d6d7e'));c.rotation.z=-.25;c.position.set(.85+i*.02,.1-i*.12,.24-i*.12);g.add(c)}
  const pnl=new THREE.Mesh(new THREE.BoxGeometry(.58,.52,.02),stdMat('#1a252f'));pnl.position.set(.2,.06,.515);g.add(pnl)
  const gl=new THREE.Mesh(new THREE.BoxGeometry(.46,.38,.01),glowMat(gc));gl.position.set(.2,.06,.522);g.add(gl);glowMeshes.push(gl)
  const lid=new THREE.Mesh(new THREE.BoxGeometry(1.65,.06,1.05),stdMat('#3a4a55'));lid.position.y=.58;g.add(lid)
  for(let i=0;i<4;i++){const s=new THREE.Mesh(new THREE.CylinderGeometry(.04,.04,.1,8),stdMat('#7f8c8d'));s.rotation.z=Math.PI/2;s.position.set(-.4+i*.26,.55,.44);g.add(s)}
  const base=new THREE.Mesh(new THREE.BoxGeometry(1.75,.1,1.12),stdMat('#1a252f'));base.position.y=-.6;g.add(base)
  for(const x of[-.7,.7])for(const z of[-.42,.42]){const w=new THREE.Mesh(new THREE.CylinderGeometry(.07,.07,.06,10),stdMat('#2c3e50'));w.position.set(x,-.66,z);g.add(w)}
  return g
}

function pickBuilder(type) {
  const t = type ?? ''
  if(t.includes('流水')||t.includes('组装')) return buildConveyor
  if(t.includes('贴附')||t.includes('调校')) return buildMachine
  if(t.includes('老化')||t.includes('测试')) return buildAging
  return buildPacking
}

function initScene() {
  const canvas = canvasRef.value; if(!canvas) return
  glowMeshes = []
  const W = canvas.clientWidth||900, H = canvas.clientHeight||540
  renderer = new THREE.WebGLRenderer({ canvas, antialias:true, alpha:false })
  renderer.setSize(W,H,false); renderer.setPixelRatio(Math.min(window.devicePixelRatio,2))
  renderer.setClearColor(0x0d1117,1); renderer.shadowMap.enabled=true
  renderer.shadowMap.type=THREE.PCFSoftShadowMap
  renderer.toneMapping=THREE.ACESFilmicToneMapping; renderer.toneMappingExposure=1.1
  scene = new THREE.Scene(); scene.fog=new THREE.FogExp2(0x0d1117,.036)
  camera = new THREE.PerspectiveCamera(42,W/H,.1,200); camera.position.set(3.5,2.2,4.2)
  scene.add(new THREE.AmbientLight(0x8899bb,.55))
  const sun=new THREE.DirectionalLight(0xffffff,1.4); sun.position.set(6,8,5); sun.castShadow=true
  sun.shadow.mapSize.set(2048,2048); sun.shadow.camera.left=-5; sun.shadow.camera.right=5
  sun.shadow.camera.top=5; sun.shadow.camera.bottom=-5; scene.add(sun)
  const fill=new THREE.DirectionalLight(0x3366aa,.45); fill.position.set(-4,2,-3); scene.add(fill)
  const gc=levelColor.value
  const pt=new THREE.PointLight(new THREE.Color(gc),1.8,8); pt.position.set(0,.5,2); scene.add(pt)
  const spot=new THREE.SpotLight(0xaaccff,.6,15,Math.PI/5); spot.position.set(-3,4,-2); spot.target.position.set(0,0,0); scene.add(spot,spot.target)
  const floor=new THREE.Mesh(new THREE.PlaneGeometry(24,24),new THREE.MeshStandardMaterial({color:0x111820,metalness:.1,roughness:.95}))
  floor.rotation.x=-Math.PI/2; floor.position.y=-1.72; floor.receiveShadow=true; scene.add(floor)
  const grid=new THREE.GridHelper(20,30,0x1e3a5f,0x162838); grid.position.y=-1.71; scene.add(grid)
  const ring=new THREE.Mesh(new THREE.RingGeometry(.6,1.6,64),new THREE.MeshBasicMaterial({color:new THREE.Color(gc),transparent:true,opacity:.2,side:THREE.DoubleSide}))
  ring.rotation.x=-Math.PI/2; ring.position.y=-1.7; scene.add(ring)
  modelGroup=pickBuilder(props.data?.equipmentType)(gc)
  modelGroup.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true}}); scene.add(modelGroup)
  controls=new OrbitControls(camera,canvas)
  controls.enableDamping=true; controls.dampingFactor=.06
  controls.minDistance=1.5; controls.maxDistance=14
  controls.maxPolarAngle=Math.PI/1.85; controls.autoRotate=autoRotate.value
  controls.autoRotateSpeed=1.4; controls.target.set(0,0,0); controls.update()
  const loop=()=>{ animId=requestAnimationFrame(loop); controls.autoRotate=autoRotate.value; controls.update(); renderer.render(scene,camera) }
  loop()
}

function resetCamera() { if(!camera||!controls) return; camera.position.set(3.5,2.2,4.2); controls.target.set(0,0,0); controls.update() }
function toggleWireframe(val) { modelGroup?.traverse(o=>{if(o.isMesh)o.material.wireframe=val}) }
function destroyScene() { cancelAnimationFrame(animId); controls?.dispose(); renderer?.dispose(); scene?.clear(); glowMeshes=[] }
function close() { emit('update:modelValue',false) }

async function waitForCanvas() {
  for(let i=0;i<30;i++){
    await new Promise(r=>setTimeout(r,20))
    const c = canvasRef.value
    if(c && c.clientWidth > 0 && c.clientHeight > 0) return true
  }
  return false
}

watch(()=>props.modelValue, async(val)=>{
  if(val){ const ok = await waitForCanvas(); if(ok) initScene() }
  else { destroyScene(); wireframe.value=false; autoRotate.value=true }
})
const onResize=()=>{ if(!renderer||!canvasRef.value) return; const W=canvasRef.value.clientWidth,H=canvasRef.value.clientHeight; renderer.setSize(W,H,false); camera.aspect=W/H; camera.updateProjectionMatrix() }
window.addEventListener('resize',onResize)
onUnmounted(()=>{ destroyScene(); window.removeEventListener('resize',onResize) })
</script>

<style scoped>
.viewer-mask { position:fixed;inset:0;background:rgba(0,0,0,.82);z-index:2000;display:flex;align-items:center;justify-content:center;backdrop-filter:blur(5px) }
.viewer-box { width:min(1100px,96vw);height:min(700px,92vh);background:#0d1117;border-radius:14px;border:1px solid #1e3a5f;display:flex;flex-direction:column;overflow:hidden;box-shadow:0 24px 80px rgba(0,0,0,.8) }
.viewer-head { display:flex;align-items:center;justify-content:space-between;padding:12px 18px 10px;background:#111820;border-bottom:1px solid #1e3a5f;flex-shrink:0 }
.viewer-head__left { display:flex;flex-direction:column;gap:3px }
.viewer-eq-info { display:flex;align-items:center;gap:6px }
.viewer-code { font-size:12px;color:#7090b0;font-family:monospace }
.viewer-name { font-size:16px;font-weight:700;color:#e8edf5 }
.viewer-hint { font-size:11px;color:#445566 }
.viewer-close { width:32px;height:32px;border-radius:8px;background:#1e2d3d;border:1px solid #2a4060;color:#7090b0;font-size:16px;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:background .15s,color .15s }
.viewer-close:hover { background:#2a3f55;color:#e8edf5 }
.viewer-canvas { flex:1;width:100%;display:block }
.viewer-footer { display:flex;align-items:center;gap:12px;padding:9px 18px;background:#111820;border-top:1px solid #1e3a5f;flex-shrink:0;flex-wrap:wrap }
.vf-btn { padding:4px 12px;border-radius:6px;background:#1e2d3d;border:1px solid #2a4060;color:#a0b8d0;font-size:12px;cursor:pointer;transition:background .15s }
.vf-btn:hover { background:#263d52 }
.vf-btn.active { background:#1a3a5a;color:#60aaee;border-color:#3070a0 }
.vf-divider { width:1px;height:20px;background:#1e3a5f;flex-shrink:0 }
.vf-label { font-size:12px;color:#5a7a90 }
.vf-score { display:flex;align-items:center;gap:8px }
.vf-score__label { font-size:12px;color:#5a7a90 }
.vf-score__bar { width:100px;height:6px;background:#1e2d3d;border-radius:3px;overflow:hidden }
.vf-score__fill { height:100%;border-radius:3px;transition:width .8s ease }
.vf-score__val { font-size:13px;font-weight:700;font-family:monospace;min-width:28px }
.vf-deducts { display:flex;gap:6px;flex-wrap:wrap }
.vf-deduct-tag { font-size:10px;padding:2px 7px;border-radius:8px;border:1px solid transparent }
.vf-deduct-tag.is-ok  { background:#0d2a1a;color:#4aaa6a;border-color:#1a4a2a }
.vf-deduct-tag.is-bad { background:#2a100d;color:#e06060;border-color:#4a1a18 }
</style>
