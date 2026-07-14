<template>
  <div class="factory-wrap">
    <canvas ref="canvasRef" class="factory-canvas" />

    <div class="factory-labels">
      <div
        v-for="item in labelItems" :key="item.id"
        class="eq-label" :class="`lv-${item.level}`"
        :style="{ left: item.x+'px', top: item.y+'px', opacity: item.visible ? 1 : 0 }"
        @click.stop="selectEq(item.id)"
      >
        <span class="eq-label__score" :style="{ color: item.levelColor }">{{ item.score }}</span>
        <span class="eq-label__name">{{ item.name }}</span>
        <span class="eq-label__dot" :style="{ background: item.statusColor }"></span>
      </div>
    </div>

    <div class="factory-toolbar">
      <button class="ft-btn" @click="resetCamera">⊙ 复位</button>
      <button class="ft-btn" :class="{ active: autoRotate }" @click="autoRotate = !autoRotate">
        {{ autoRotate ? '⏸ 停止' : '▶ 旋转' }}
      </button>
      <div class="ft-sep"/>
      <span v-for="s in STATUS_LEGEND" :key="s.label" class="ft-legend">
        <span class="ft-dot" :style="{ background: s.color }"/>{{ s.label }}
      </span>
      <div class="ft-sep"/>
      <span class="ft-tip">🖱 拖转 · 滚缩 · 右键平移 · 点设备查详情</span>
    </div>

    <transition name="slide-panel">
      <div v-if="selectedEq" class="eq-panel">
        <div class="ep-head">
          <div>
            <div class="ep-code">{{ selectedEq.equipmentCode }}</div>
            <div class="ep-name">{{ selectedEq.equipmentName }}</div>
          </div>
          <button class="ep-close" @click="selectedEq = null">✕</button>
        </div>
        <div class="ep-tags">
          <el-tag :type="statusTag(selectedEq.status)" size="small">{{ selectedEq.statusCn }}</el-tag>
          <el-tag :type="levelTag(selectedEq.healthLevel)" size="small" style="margin-left:6px">
            健康度 {{ selectedEq.healthScore }}
          </el-tag>
        </div>
        <div class="ep-score-row">
          <svg viewBox="0 0 80 80" width="80" height="80">
            <circle cx="40" cy="40" r="32" fill="none" stroke="#eef1f5" stroke-width="7"/>
            <circle cx="40" cy="40" r="32" fill="none"
              :stroke="hlColor(selectedEq.healthLevel)" stroke-width="7" stroke-linecap="round"
              :stroke-dasharray="`${(201.1*selectedEq.healthScore/100).toFixed(1)} 201.1`"
              stroke-dashoffset="50.3" style="transition:stroke-dasharray .8s ease"/>
            <text x="40" y="45" text-anchor="middle" font-size="18" font-weight="800"
              :fill="hlColor(selectedEq.healthLevel)">{{ selectedEq.healthScore }}</text>
          </svg>
          <div class="ep-advice">{{ selectedEq.advice }}</div>
        </div>
        <div class="ep-deducts">
          <div v-for="d in deductRows(selectedEq)" :key="d.label"
            class="ep-dd" :class="d.val>0?'bad':'ok'">
            <span class="ep-dd__label">{{ d.label }}</span>
            <span class="ep-dd__note">{{ d.note }}</span>
            <span class="ep-dd__val">{{ d.val>0?'-'+d.val:'✓' }}</span>
          </div>
        </div>
        <div class="ep-sub-title">
          近7天报警
          <em :style="{ color: selectedEq.alarm7d>0?'#ef4444':'#52b788' }">{{ selectedEq.alarm7d }} 次</em>
        </div>
        <el-table v-if="selectedEq.alarmList7d?.length"
          :data="selectedEq.alarmList7d.slice(0,4)" size="small" max-height="120">
          <el-table-column prop="alarmNo" label="单号" width="104"/>
          <el-table-column prop="alarmLevelCn" label="级别" width="48" align="center"/>
          <el-table-column prop="alarmStatusCn" label="状态" width="60" align="center"/>
        </el-table>
        <div v-else class="ep-empty">近7天无报警 ✓</div>
        <div class="ep-sub-title" style="margin-top:10px">最近维保记录</div>
        <el-table v-if="selectedEq.maintList?.length"
          :data="selectedEq.maintList.slice(0,3)" size="small" max-height="100">
          <el-table-column prop="maintenanceNo" label="单号" width="104"/>
          <el-table-column prop="maintenanceTypeCn" label="类型" width="48" align="center"/>
          <el-table-column prop="maintenanceResultCn" label="结果" width="60" align="center"/>
        </el-table>
        <div v-else class="ep-empty">暂无维保记录</div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { createEquipmentMesh, STATUS_COLOR, HEALTH_COLOR } from './EquipmentMesh.js'
import { fetchEquipmentHealth } from '@/api/business'

const LAYOUT = {
  'EQ-001': { x:-5.5, z:-2.0 }, 'EQ-002': { x:-5.5, z: 2.0 }, 'EQ-010': { x:-5.5, z: 0.0 },
  'EQ-003': { x:-2.4, z:-1.2 }, 'EQ-007': { x:-2.4, z: 2.0 },
  'EQ-004': { x: 1.6, z:-2.4 }, 'EQ-009': { x: 1.6, z: 0.2 },
  'EQ-005': { x: 4.6, z:-2.4 }, 'EQ-011': { x: 4.6, z: 0.2 },
  'EQ-006': { x: 4.6, z: 2.6 }, 'EQ-008': { x: 1.6, z: 2.6 },
}
const STATUS_LEGEND = [
  { label:'空闲/运行', color: STATUS_COLOR.IDLE },
  { label:'故障',      color: STATUS_COLOR.FAULT },
  { label:'维保中',    color: STATUS_COLOR.MAINTAINING },
]

const canvasRef  = ref(null)
const autoRotate = ref(true)
const labelItems = ref([])
const selectedEq = ref(null)
const equipList  = ref([])

let renderer, scene, camera, controls, animId, timer = null
const meshMap = new Map()

const hlColor   = (l) => HEALTH_COLOR[l] ?? '#52b788'
const levelTag  = (l) => ({ GOOD:'success', WARN:'warning', ALERT:'danger', DANGER:'danger' })[l] ?? 'info'
const statusTag = (s) => ({ RUNNING:'success', IDLE:'info', FAULT:'danger', MAINTAINING:'warning' })[s] ?? 'info'
const deductRows = (d) => [
  { label:'超时运行', val:d.deductRun??0,   note:`运行 ${d.runHours}h` },
  { label:'高频报警', val:d.deductAlarm??0,  note:`近7天 ${d.alarm7d} 次` },
  { label:'逾期保养', val:d.deductMaint??0,  note:`已 ${d.daysSinceMaint} 天` },
  { label:'关联缺陷', val:d.deductNc??0,     note:`近30天 ${d.faultCount30d??0} 次` },
]
function selectEq(id) { selectedEq.value = equipList.value.find(e => e.equipmentId === id) ?? null }

function initScene() {
  const canvas = canvasRef.value; if (!canvas) return
  const W = canvas.clientWidth||900, H = canvas.clientHeight||520
  renderer = new THREE.WebGLRenderer({ canvas, antialias:true })
  renderer.setSize(W,H,false); renderer.setPixelRatio(Math.min(window.devicePixelRatio,2))
  renderer.setClearColor(0xf0f4f8,1); renderer.shadowMap.enabled=true
  renderer.shadowMap.type=THREE.PCFSoftShadowMap
  renderer.toneMapping=THREE.ACESFilmicToneMapping; renderer.toneMappingExposure=1.05
  scene = new THREE.Scene(); scene.fog=new THREE.Fog(0xf0f4f8,20,40)
  camera = new THREE.PerspectiveCamera(46,W/H,0.1,200)
  camera.position.set(0,8.5,12); camera.lookAt(0,0,0)
  scene.add(new THREE.AmbientLight(0xffffff,0.72))
  const sun=new THREE.DirectionalLight(0xffffff,1.2); sun.position.set(8,14,10); sun.castShadow=true
  sun.shadow.mapSize.set(2048,2048); sun.shadow.camera.left=-14; sun.shadow.camera.right=14
  sun.shadow.camera.top=14; sun.shadow.camera.bottom=-14; scene.add(sun)
  const fill=new THREE.DirectionalLight(0xc8daff,0.3); fill.position.set(-6,4,-5); scene.add(fill)
  const floor=new THREE.Mesh(new THREE.PlaneGeometry(32,26),new THREE.MeshStandardMaterial({color:0xe8eef5,roughness:0.9,metalness:0.05}))
  floor.rotation.x=-Math.PI/2; floor.position.y=-0.52; floor.receiveShadow=true; scene.add(floor)
  const grid=new THREE.GridHelper(32,32,0xc8d6e8,0xd8e4f0); grid.position.y=-0.51; scene.add(grid)
  buildZone(-3.8,0,5.2,5.0,'生产一部',0x3b82f6)
  buildZone(3.4,0,5.8,6.4,'生产二部',0x10b981)
  controls=new OrbitControls(camera,canvas)
  controls.enableDamping=true; controls.dampingFactor=0.06
  controls.minDistance=4; controls.maxDistance=30; controls.maxPolarAngle=Math.PI/2.1
  controls.autoRotate=true; controls.autoRotateSpeed=0.6
  controls.target.set(0,0,0); controls.update()
  canvas.addEventListener('click',onCanvasClick)
  animate()
}

function buildZone(cx,cz,hw,hd,label,colorHex) {
  const c=new THREE.Color(colorHex), hex=c.getHexString()
  const pts=[[-hw,-hd],[hw,-hd],[hw,hd],[-hw,hd],[-hw,-hd]].map(([x,z])=>new THREE.Vector3(cx+x,0.04,cz+z))
  scene.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(pts),new THREE.LineBasicMaterial({color:c,transparent:true,opacity:0.55})))
  const fm=new THREE.Mesh(new THREE.PlaneGeometry(hw*2,hd*2),new THREE.MeshBasicMaterial({color:c,transparent:true,opacity:0.05,side:THREE.DoubleSide}))
  fm.rotation.x=-Math.PI/2; fm.position.set(cx,-0.50,cz); scene.add(fm)
  const cv=document.createElement('canvas'); cv.width=256; cv.height=64
  const ctx=cv.getContext('2d')
  ctx.clearRect(0,0,256,64)
  ctx.beginPath(); ctx.rect(3,3,250,58)
  ctx.fillStyle='#'+hex+'20'; ctx.fill()
  ctx.strokeStyle='#'+hex+'cc'; ctx.lineWidth=2.5; ctx.stroke()
  ctx.fillStyle='#'+hex; ctx.font='bold 26px sans-serif'
  ctx.textAlign='center'; ctx.textBaseline='middle'; ctx.fillText(label,128,32)
  const board=new THREE.Mesh(new THREE.PlaneGeometry(2.5,0.68),new THREE.MeshBasicMaterial({map:new THREE.CanvasTexture(cv),transparent:true,side:THREE.DoubleSide}))
  board.position.set(cx,1.0,cz-hd-0.2); board.rotation.x=-0.10; scene.add(board)
}

function placeEquipments(list) {
  list.forEach(eq=>{
    const pos=LAYOUT[eq.equipmentCode]; if(!pos) return
    if(meshMap.has(eq.equipmentCode)){ meshMap.get(eq.equipmentCode).updateStatus(eq.status); return }
    const {group,updateStatus}=createEquipmentMesh(eq)
    group.position.set(pos.x,0,pos.z)
    group.userData={equipmentId:eq.equipmentId,equipmentCode:eq.equipmentCode}
    group.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true}})
    scene.add(group); meshMap.set(eq.equipmentCode,{group,updateStatus})
  })
}

const raycaster=new THREE.Raycaster(), mouse=new THREE.Vector2()
function onCanvasClick(e) {
  const canvas=canvasRef.value; if(!canvas) return
  const r=canvas.getBoundingClientRect()
  mouse.x=((e.clientX-r.left)/r.width)*2-1; mouse.y=-((e.clientY-r.top)/r.height)*2+1
  raycaster.setFromCamera(mouse,camera)
  const targets=[]; meshMap.forEach(({group})=>group.traverse(o=>{if(o.isMesh)targets.push(o)}))
  const hits=raycaster.intersectObjects(targets,false); if(!hits.length) return
  let obj=hits[0].object
  while(obj&&!obj.userData?.equipmentCode) obj=obj.parent
  if(!obj?.userData?.equipmentId) return
  selectEq(obj.userData.equipmentId)
  const pos=LAYOUT[obj.userData.equipmentCode]
  if(pos) flyTo(
    new THREE.Vector3(pos.x, 3.5, pos.z + 5),  // 相机目标位置
    new THREE.Vector3(pos.x, 0,   pos.z)         // 环绕中心 = 设备位置
  )
}

function flyTo(camTarget, lookTarget) {
  const camStart  = camera.position.clone()
  const lookStart = controls.target.clone()
  const t0 = performance.now(), dur = 700
  const tick = () => {
    const p = Math.min((performance.now() - t0) / dur, 1)
    const e = p < 0.5 ? 2*p*p : -1 + (4 - 2*p)*p   // ease-in-out
    camera.position.lerpVectors(camStart, camTarget, e)
    controls.target.lerpVectors(lookStart, lookTarget, e)
    controls.update()
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

function updateLabels() {
  const canvas=canvasRef.value; if(!canvas||!camera) return
  const W=canvas.clientWidth,H=canvas.clientHeight
  labelItems.value=equipList.value.map(eq=>{
    const pos=LAYOUT[eq.equipmentCode]; if(!pos) return null
    const v=new THREE.Vector3(pos.x,1.9,pos.z).project(camera)
    const x=(v.x*.5+.5)*W,y=(-v.y*.5+.5)*H
    return{id:eq.equipmentId,name:eq.equipmentCode,score:eq.healthScore??'-',level:(eq.healthLevel??'GOOD').toLowerCase(),levelColor:HEALTH_COLOR[eq.healthLevel]??'#52b788',statusColor:STATUS_COLOR[eq.status]??'#52c1a2',x:x-32,y:y-16,visible:v.z<1&&x>0&&x<W&&y>0&&y<H}
  }).filter(Boolean)
}

function animate(){animId=requestAnimationFrame(animate);controls.autoRotate=autoRotate.value;controls.update();renderer.render(scene,camera);updateLabels()}
function resetCamera(){camera.position.set(0,8.5,12);controls.target.set(0,0,0);controls.update()}

async function loadData(){
  try{const res=await fetchEquipmentHealth();const list=Array.isArray(res)?res:(res.data??[]);equipList.value=list;if(scene)placeEquipments(list)}catch{/**/ }
}
function onResize(){const c=canvasRef.value;if(!c||!renderer||!camera)return;const W=c.clientWidth,H=c.clientHeight;renderer.setSize(W,H,false);camera.aspect=W/H;camera.updateProjectionMatrix()}

onMounted(async()=>{await new Promise(r=>setTimeout(r,30));initScene();await loadData();timer=setInterval(loadData,30000);window.addEventListener('resize',onResize)})
onUnmounted(()=>{cancelAnimationFrame(animId);canvasRef.value?.removeEventListener('click',onCanvasClick);controls?.dispose();renderer?.dispose();scene?.clear();meshMap.clear();clearInterval(timer);window.removeEventListener('resize',onResize)})
watch(autoRotate,v=>{if(controls)controls.autoRotate=v})

// 供父组件调用：把最新设备列表同步到 3D 场景
function syncEquipments(list) {
  if (!Array.isArray(list) || !list.length) return
  equipList.value = list
  if (scene) placeEquipments(list)
}

defineExpose({ syncEquipments })
</script>

<style scoped>
.factory-wrap{position:relative;width:100%;height:580px;background:#f0f4f8;border-radius:12px;overflow:hidden;border:1px solid #dce8f8;box-shadow:0 2px 12px rgba(0,27,63,.08)}
.factory-canvas{width:100%;height:100%;display:block}
.factory-labels{position:absolute;inset:0;pointer-events:none}
.eq-label{position:absolute;display:flex;align-items:center;gap:4px;background:rgba(255,255,255,.90);border:1px solid #dde8f5;border-radius:8px;padding:2px 7px;font-size:11px;cursor:pointer;pointer-events:all;transition:opacity .15s,transform .15s;box-shadow:0 2px 6px rgba(0,27,63,.10);white-space:nowrap}
.eq-label:hover{transform:scale(1.08);box-shadow:0 4px 12px rgba(0,27,63,.16)}
.eq-label.lv-alert,.eq-label.lv-danger{border-color:#fca5a5;background:rgba(255,245,245,.93)}
.eq-label.lv-warn{border-color:#fcd34d;background:rgba(255,251,235,.93)}
.eq-label__score{font-size:13px;font-weight:800;line-height:1}
.eq-label__name{font-size:10px;color:#6b7a90}
.eq-label__dot{width:7px;height:7px;border-radius:50%;flex-shrink:0}
.factory-toolbar{position:absolute;bottom:14px;left:50%;transform:translateX(-50%);display:flex;align-items:center;gap:10px;background:rgba(255,255,255,.93);border:1px solid #dce8f8;border-radius:24px;padding:6px 18px;backdrop-filter:blur(6px);box-shadow:0 2px 12px rgba(0,27,63,.10);font-size:12px;flex-wrap:wrap}
.ft-btn{padding:3px 12px;border-radius:12px;background:#f0f4f8;border:1px solid #dce8f8;color:#3b5a80;font-size:11px;cursor:pointer;transition:background .15s}
.ft-btn:hover,.ft-btn.active{background:#dbeafe;border-color:#93c5fd;color:#1d4ed8}
.ft-sep{width:1px;height:16px;background:#dce8f8}
.ft-legend{display:flex;align-items:center;gap:4px;color:#6b7a90;font-size:11px}
.ft-dot{width:8px;height:8px;border-radius:50%}
.ft-tip{font-size:10px;color:#9aa5b4}
.eq-panel{position:absolute;top:0;right:0;bottom:0;width:300px;background:rgba(255,255,255,.97);border-left:1px solid #dce8f8;padding:16px 14px;overflow-y:auto;backdrop-filter:blur(8px);box-shadow:-4px 0 20px rgba(0,27,63,.10);display:flex;flex-direction:column;gap:10px}
.ep-head{display:flex;align-items:flex-start;justify-content:space-between}
.ep-code{font-size:12px;color:#8090a8;font-family:monospace}
.ep-name{font-size:15px;font-weight:700;color:#001b3f;margin-top:2px}
.ep-close{width:28px;height:28px;border-radius:7px;background:#f0f4f8;border:1px solid #dce8f8;color:#8090a8;cursor:pointer;display:flex;align-items:center;justify-content:center;font-size:14px;transition:background .15s;flex-shrink:0}
.ep-close:hover{background:#fde8e8;color:#e05050}
.ep-tags{display:flex;flex-wrap:wrap;gap:4px}
.ep-score-row{display:flex;align-items:center;gap:12px}
.ep-advice{font-size:12px;color:#5a6a80;line-height:1.55;flex:1}
.ep-deducts{display:flex;flex-direction:column;gap:4px;background:#f7faff;border-radius:8px;padding:8px 10px}
.ep-dd{display:flex;align-items:center;font-size:11px;gap:4px}
.ep-dd.bad{color:#c0392b}
.ep-dd.ok{color:#52b788}
.ep-dd__label{flex:0 0 62px;font-weight:500}
.ep-dd__note{flex:1;color:#8090a8;font-size:10px}
.ep-dd__val{flex:0 0 30px;text-align:right;font-weight:700}
.ep-sub-title{font-size:12px;font-weight:600;color:#3b5a80;border-left:3px solid #3b82f6;padding-left:7px}
.ep-sub-title em{font-style:normal;margin-left:6px;font-size:13px}
.ep-empty{font-size:11px;color:#52b788;text-align:center;padding:8px 0}
.slide-panel-enter-active,.slide-panel-leave-active{transition:transform .28s ease,opacity .28s ease}
.slide-panel-enter-from,.slide-panel-leave-to{transform:translateX(100%);opacity:0}
</style>
