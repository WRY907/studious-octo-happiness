<template>
  <div ref="containerEl" class="three-map">
    <!-- hover tooltip -->
    <transition name="tip-fade">
      <div v-if="tip.visible" class="holo-tip" :style="{ left: tip.x + 'px', top: tip.y + 'px' }">
        <div class="tip-title">{{ tip.name }}</div>
        <div class="tip-row">销售额 <b :class="{ masked: masked }">{{ masked ? '🔒 ***' : '¥' + tip.sales }}</b></div>
        <div class="tip-row">订单数 <b>{{ tip.orders }}</b></div>
        <div class="tip-row">用户数 <b>{{ tip.users }}</b></div>
      </div>
    </transition>
    <div class="three-hint font-data">🖱 拖拽旋转 · 滚轮缩放</div>
    <div v-if="failed" class="three-error font-data">3D MAP UNAVAILABLE</div>
  </div>
</template>

<script setup>
/* ============================================================
   ThreeMap3D · 陕西省 3D 数字孪生地图
   技术参考：ThreeMaps(Apache-2.0) 的 ExtrudeMap/FlyLine 模式
   自实现：Mercator 投影（零依赖）、挤出地图、销售光柱、
   贝塞尔飞线流光、CSS2D 标签、扫描波、星尘粒子
   ============================================================ */
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'

const props = defineProps({
  cityData: { type: Array, default: () => [] }, // [{city, sales, orders, users}]
  masked: { type: Boolean, default: false }
})

const containerEl = ref(null)
const failed = ref(false)
const tip = ref({ visible: false, x: 0, y: 0, name: '', sales: '', orders: '', users: '' })

let renderer = null
let labelRenderer = null
let scene = null
let camera = null
let controls = null
let rafId = 0
let threeObjects = [] // 待 dispose 的 geometry/material
let dataGroup = null // 光柱/飞线/标签容器（数据变化时重建）
let mapMeshes = [] // 城市网格（hover 用）
let labelEls = [] // CSS2D 标签元素（清理用）
let flyBalls = [] // 飞线流光球 {mesh, curve, t, speed}
let scanRings = [] // 扫描环 {mesh, t}
let floatDots = [] // 顶端呼吸光点 {mesh, baseY, phase}
let entranceT = 0 // 入场进度 0~1

/* ================= Mercator 投影（以数据包围盒自适应居中缩放） ================= */
let projFn = null
function buildProjection(geoJson) {
  // 计算包围盒
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity
  const R = 6378137 * Math.PI / 180
  const raw = ([lng, lat]) => [
    lng * R,
    Math.log(Math.tan(Math.PI / 4 + (lat * Math.PI / 360)))
  ]
  geoJson.features.forEach(f => {
    f.geometry.coordinates.forEach(multi => multi.forEach(poly => poly.forEach(([lng, lat]) => {
      if (lng == null || lat == null) return
      const [x, y] = raw([lng, lat])
      if (x < minX) minX = x; if (x > maxX) maxX = x
      if (y < minY) minY = y; if (y > maxY) maxY = y
    })))
  })
  const cx = (minX + maxX) / 2
  const cy = (minY + maxY) / 2
  const span = Math.max(maxX - minX, (maxY - minY) * 1.15)
  const scale = 11 / span // 目标尺寸约 11 单位
  // 返回投影：x 东、z 南（y 保留给高度轴）
  projFn = ([lng, lat]) => {
    const [x, y] = raw([lng, lat])
    return [(x - cx) * scale, -(y - cy) * scale]
  }
}

/* ================= 3D 挤出地图 ================= */
const DEPTH = 0.32
function buildExtrudeMap(geoJson) {
  const mapGroup = new THREE.Group()
  geoJson.features.forEach(f => {
    const name = f.properties.name
    const group = new THREE.Group()
    f.geometry.coordinates.forEach(multi => multi.forEach(poly => {
      const shape = new THREE.Shape()
      let started = false
      poly.forEach(([lng, lat], i) => {
        if (lng == null || lat == null) return
        const [x, z] = projFn([lng, lat])
        if (!started) { shape.moveTo(x, z); started = true }
        else shape.lineTo(x, z)
      })
      if (!started) return
      const geo = new THREE.ExtrudeGeometry(shape, {
        depth: DEPTH,
        bevelEnabled: true,
        bevelSegments: 1,
        bevelThickness: 0.04,
        bevelSize: 0.03
      })
      // 烘焙躺平：shape 平面 x-z（北为 -z），挤出 +y 朝上
      geo.rotateX(-Math.PI / 2)
      const mesh = new THREE.Mesh(geo, [
        topMat(name), sideMat()
      ])
      mesh.userData = { name, feature: f }
      group.add(mesh)
      mapMeshes.push(mesh)
    }))
    mapGroup.add(group)
  })
  return mapGroup
}

function topMat(name) {
  const m = new THREE.MeshLambertMaterial({ color: 0x16305a })
  m.userData = { baseColor: new THREE.Color(0x16305a), hoverColor: new THREE.Color(0x2a6db5) }
  return m
}
function sideMat() {
  return new THREE.MeshLambertMaterial({ color: 0x081226 })
}

/* 城市边界发光线 */
function buildOutline(geoJson) {
  const group = new THREE.Group()
  const mat = new THREE.LineBasicMaterial({ color: 0x37c6ff, transparent: true, opacity: 0.85 })
  threeObjects.push(mat)
  geoJson.features.forEach(f => {
    f.geometry.coordinates.forEach(multi => multi.forEach(poly => {
      const pts = []
      poly.forEach(([lng, lat]) => {
        if (lng == null || lat == null) return
        const [x, z] = projFn([lng, lat])
        pts.push(new THREE.Vector3(x, DEPTH + 0.012, z))
      })
      if (pts.length < 2) return
      const geo = new THREE.BufferGeometry().setFromPoints(pts)
      threeObjects.push(geo)
      group.add(new THREE.Line(geo, mat))
    }))
  })
  return group
}

/* ================= 数据层：光柱 / 飞线 / 标签 / 扫描环 ================= */
function buildDataLayer(geoJson) {
  dataGroup = new THREE.Group()
  const features = geoJson.features || []
  const list = props.cityData || []
  const norm = s => String(s || '').replace(/(市|地区|自治州)$/, '')

  // 归一化销售额 → 光柱高度
  const maxSales = Math.max(...list.map(d => Number(d.sales || 0)), 1)
  const xianFeature = features.find(f => /西安/.test(f.properties.name || ''))

  features.forEach(f => {
    const name = f.properties.name || ''
    const center = f.properties.center || f.properties.centroid
    if (!center) return
    const rec = list.find(d => norm(d.city) === norm(name)) || {}
    const sales = Number(rec.sales || 0)
    const [x, z] = projFn(center)
    const isXian = /西安/.test(name)
    const h = 0.55 + (sales / maxSales) * 2.1

    /* --- 光柱（顶点色渐变透明） --- */
    const colTop = isXian ? 0xffd76a : 0x2fd8ff
    const colBot = isXian ? 0x9a7b1e : 0x0a3d66
    const barGeo = new THREE.CylinderGeometry(0.06, 0.11, h, 6, 1, true)
    const pos = barGeo.attributes.position
    const colors = new Float32Array(pos.count * 3)
    const cT = new THREE.Color(colTop), cB = new THREE.Color(colBot)
    for (let i = 0; i < pos.count; i++) {
      const t = (pos.getY(i) + h / 2) / h // 0 底 1 顶
      const c = cB.clone().lerp(cT, t)
      colors[i * 3] = c.r; colors[i * 3 + 1] = c.g; colors[i * 3 + 2] = c.b
    }
    barGeo.setAttribute('color', new THREE.BufferAttribute(colors, 3))
    const barMat = new THREE.MeshBasicMaterial({
      vertexColors: true, transparent: true, opacity: 0.92,
      blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide
    })
    threeObjects.push(barGeo, barMat)
    const bar = new THREE.Mesh(barGeo, barMat)
    bar.position.set(x, DEPTH + h / 2, z)
    bar.userData = { name, rec, targetH: h }
    dataGroup.add(bar)

    /* --- 顶端呼吸光点 --- */
    const dotGeo = new THREE.SphereGeometry(0.09, 12, 12)
    const dotMat = new THREE.MeshBasicMaterial({
      color: colTop, transparent: true, opacity: 0.95,
      blending: THREE.AdditiveBlending, depthWrite: false
    })
    threeObjects.push(dotGeo, dotMat)
    const dot = new THREE.Mesh(dotGeo, dotMat)
    dot.position.set(x, DEPTH + h + 0.05, z)
    dataGroup.add(dot)
    floatDots.push({ mesh: dot, baseY: DEPTH + h + 0.05, phase: Math.random() * Math.PI * 2 })

    /* --- 城市底座光环 --- */
    const ringGeo = new THREE.RingGeometry(0.16, 0.24, 24)
    const ringMat = new THREE.MeshBasicMaterial({
      color: colTop, transparent: true, opacity: 0.55,
      blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide
    })
    threeObjects.push(ringGeo, ringMat)
    const ring = new THREE.Mesh(ringGeo, ringMat)
    ring.rotation.x = -Math.PI / 2
    ring.position.set(x, DEPTH + 0.02, z)
    dataGroup.add(ring)

    /* --- CSS2D 市名+销售额标签 --- */
    const el = document.createElement('div')
    el.className = 'holo-label' + (isXian ? ' is-xian' : '')
    el.innerHTML = `<span class="hl-name">${name}</span><span class="hl-val">${props.masked ? '***' : fmt(sales)}</span>`
    const label = new CSS2DObject(el)
    label.position.set(x, DEPTH + h + 0.32, z)
    dataGroup.add(label)
    labelEls.push(el)

    /* --- 飞线：各地市 → 西安 --- */
    if (!isXian && xianFeature) {
      const xc = xianFeature.properties.center || xianFeature.properties.centroid
      if (xc) {
        const [x2, z2] = projFn(xc)
        const start = new THREE.Vector3(x, DEPTH + 0.1, z)
        const end = new THREE.Vector3(x2, DEPTH + 0.1, z2)
        const mid = start.clone().add(end).multiplyScalar(0.5)
        mid.y += 1.2 + start.distanceTo(end) * 0.22
        const curve = new THREE.QuadraticBezierCurve3(start, mid, end)
        // 轨迹线
        const pts = curve.getPoints(48)
        const lineGeo = new THREE.BufferGeometry().setFromPoints(pts)
        const lineMat = new THREE.LineBasicMaterial({
          color: 0x3fbfff, transparent: true, opacity: 0.32
        })
        threeObjects.push(lineGeo, lineMat)
        dataGroup.add(new THREE.Line(lineGeo, lineMat))
        // 流光球
        const ballGeo = new THREE.SphereGeometry(0.055, 8, 8)
        const ballMat = new THREE.MeshBasicMaterial({
          color: 0x9ff3ff, transparent: true, opacity: 0.95,
          blending: THREE.AdditiveBlending, depthWrite: false
        })
        threeObjects.push(ballGeo, ballMat)
        const ball = new THREE.Mesh(ballGeo, ballMat)
        dataGroup.add(ball)
        flyBalls.push({ mesh: ball, curve, t: Math.random(), speed: 0.0016 + Math.random() * 0.0014 })
      }
    }
  })

  /* --- 西安中心扫描波（2 个错相扩散环） --- */
  if (xianFeature) {
    const xc = xianFeature.properties.center || xianFeature.properties.centroid
    if (xc) {
      const [x, z] = projFn(xc)
      for (let k = 0; k < 2; k++) {
        const geo = new THREE.RingGeometry(0.96, 1.0, 48)
        const mat = new THREE.MeshBasicMaterial({
          color: 0x37e0ff, transparent: true, opacity: 0.7,
          blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide
        })
        threeObjects.push(geo, mat)
        const ring = new THREE.Mesh(geo, mat)
        ring.rotation.x = -Math.PI / 2
        ring.position.set(x, DEPTH + 0.025, z)
        dataGroup.add(ring)
        scanRings.push({ mesh: ring, t: k * 0.5 })
      }
    }
  }
}

function fmt(v) {
  const n = Number(v || 0)
  if (n >= 1e8) return (n / 1e8).toFixed(2) + '亿'
  if (n >= 1e4) return (n / 1e4).toFixed(0) + '万'
  return n.toLocaleString('zh-CN')
}

/* ================= 星尘粒子 ================= */
function buildStardust() {
  const N = 260
  const pos = new Float32Array(N * 3)
  const col = new Float32Array(N * 3)
  const cA = new THREE.Color(0x37c6ff), cB = new THREE.Color(0x7b68ee)
  for (let i = 0; i < N; i++) {
    pos[i * 3] = (Math.random() - 0.5) * 15
    pos[i * 3 + 1] = 0.4 + Math.random() * 5
    pos[i * 3 + 2] = (Math.random() - 0.5) * 12
    const c = cA.clone().lerp(cB, Math.random())
    col[i * 3] = c.r; col[i * 3 + 1] = c.g; col[i * 3 + 2] = c.b
  }
  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
  geo.setAttribute('color', new THREE.BufferAttribute(col, 3))
  const mat = new THREE.PointsMaterial({
    size: 0.05, vertexColors: true, transparent: true, opacity: 0.6,
    blending: THREE.AdditiveBlending, depthWrite: false
  })
  threeObjects.push(geo, mat)
  const pts = new THREE.Points(geo, mat)
  pts.userData.isDust = true
  return pts
}

/* ================= 主流程 ================= */
async function init() {
  try {
    const el = containerEl.value
    if (!el) return

    // geojson 本地加载（与 2D 模式同源）
    const res = await fetch('/shaanxi.json')
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const geoJson = await res.json()
    buildProjection(geoJson)

    // 渲染器
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.setSize(el.clientWidth, el.clientHeight)
    renderer.domElement.classList.add('three-canvas')
    el.appendChild(renderer.domElement)

    labelRenderer = new CSS2DRenderer()
    labelRenderer.setSize(el.clientWidth, el.clientHeight)
    // JS 创建的 DOM 不带 scoped 属性，样式必须内联（否则标签层定位失效）
    const ls = labelRenderer.domElement.style
    ls.position = 'absolute'
    ls.top = '0'
    ls.left = '0'
    ls.pointerEvents = 'none'
    ls.overflow = 'hidden'
    el.appendChild(labelRenderer.domElement)

    // 场景与相机（入场：从远处拉近）
    scene = new THREE.Scene()
    camera = new THREE.PerspectiveCamera(42, el.clientWidth / el.clientHeight, 0.1, 100)
    camera.position.set(0, 13.5, 9.5)

    // 灯光
    scene.add(new THREE.AmbientLight(0x8fb8ff, 0.85))
    const dir = new THREE.DirectionalLight(0x66d9ff, 0.7)
    dir.position.set(4, 10, 4)
    scene.add(dir)

    // 地图 + 边界
    const mapGroup = buildExtrudeMap(geoJson)
    scene.add(mapGroup)
    scene.add(buildOutline(geoJson))
    scene.add(buildStardust())

    // 数据层
    buildDataLayer(geoJson)

    // 交互控制
    controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.dampingFactor = 0.08
    controls.minDistance = 6
    controls.maxDistance = 20
    controls.maxPolarAngle = Math.PI / 2.25
    controls.target.set(0, 0.4, 0)
    controls.addEventListener('start', () => hideTip())

    // hover 拾取
    renderer.domElement.addEventListener('mousemove', onHover)
    renderer.domElement.addEventListener('mouseleave', hideTip)

    // resize（容器尺寸守卫）
    ro = new ResizeObserver(() => {
      const w = el.clientWidth, h = el.clientHeight
      if (w === 0 || h === 0 || !camera) return
      camera.aspect = w / h
      camera.updateProjectionMatrix()
      renderer.setSize(w, h)
      labelRenderer.setSize(w, h)
    })
    ro.observe(el)

    animate()
  } catch (e) {
    console.warn('[ThreeMap3D] init failed:', e)
    failed.value = true
  }
}

let ro = null

/* ================= hover ================= */
const raycaster = new THREE.Raycaster()
const pointer = new THREE.Vector2()
function onHover(e) {
  if (!renderer || !camera) return
  const rect = renderer.domElement.getBoundingClientRect()
  pointer.x = ((e.clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((e.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(pointer, camera)
  const hits = raycaster.intersectObjects(mapMeshes, false)
  const first = hits.find(h => h.object.userData && h.object.userData.name)
  if (first) {
    const { name, feature } = first.object.userData
    // 高亮
    mapMeshes.forEach(m => {
      const mat = Array.isArray(m.material) ? m.material[0] : m.material
      if (mat && mat.userData && mat.userData.baseColor) {
        mat.color.copy(m === first.object ? mat.userData.hoverColor : mat.userData.baseColor)
      }
    })
    // tooltip 数据
    const norm = s => String(s || '').replace(/(市|地区|自治州)$/, '')
    const rec = (props.cityData || []).find(d => norm(d.city) === norm(name)) || {}
    tip.value = {
      visible: true, x: e.clientX - rect.left + 16, y: e.clientY - rect.top - 10,
      name,
      sales: Number(rec.sales || 0).toLocaleString('zh-CN'),
      orders: Number(rec.orders || 0).toLocaleString('zh-CN'),
      users: Number(rec.users || 0).toLocaleString('zh-CN')
    }
    renderer.domElement.style.cursor = 'pointer'
  } else {
    hideTip()
  }
}
function hideTip() {
  if (tip.value.visible) tip.value.visible = false
  mapMeshes.forEach(m => {
    const mat = Array.isArray(m.material) ? m.material[0] : m.material
    if (mat && mat.userData && mat.userData.baseColor) mat.color.copy(mat.userData.baseColor)
  })
  if (renderer) renderer.domElement.style.cursor = ''
}

/* ================= 动画循环 ================= */
const clock = new THREE.Clock()
function animate() {
  rafId = requestAnimationFrame(animate)
  const dt = Math.min(clock.getDelta(), 0.05)
  const t = clock.elapsedTime

  // 入场：相机拉近 + 缓动
  if (entranceT < 1) {
    entranceT = Math.min(1, entranceT + dt / 1.6)
    const e = 1 - Math.pow(1 - entranceT, 3)
    camera.position.set(0, 13.5 - 3.6 * e, 9.5 - 2.2 * e)
  }

  // 光柱入场：从 0 长到目标高（stagger 由飞线/其它动画带出层次感）
  if (dataGroup) {
    dataGroup.children.forEach(c => {
      if (c.userData && c.userData.targetH && c.type === 'Mesh' && c.geometry && c.geometry.type === 'CylinderGeometry') {
        const grow = Math.min(1, entranceT * 1.4 - (c.position.x + 6) / 40)
        const hh = Math.max(0.02, c.userData.targetH * Math.max(0, grow))
        c.scale.y = hh / c.userData.targetH
        c.position.y = DEPTH + hh / 2
      }
    })
  }

  // 顶端光点呼吸
  floatDots.forEach(d => {
    d.mesh.position.y = d.baseY + Math.sin(t * 2.2 + d.phase) * 0.06
    d.mesh.material.opacity = 0.7 + Math.sin(t * 2.2 + d.phase) * 0.3
  })

  // 飞线流光球
  flyBalls.forEach(f => {
    f.t = (f.t + f.speed) % 1
    const p = f.curve.getPoint(f.t)
    f.mesh.position.copy(p)
    f.mesh.material.opacity = 0.4 + Math.sin(f.t * Math.PI) * 0.6
  })

  // 扫描波扩散
  scanRings.forEach(s => {
    s.t = (s.t + dt * 0.5) % 1
    const sc = 0.15 + s.t * 3.2
    s.mesh.scale.set(sc, sc, sc)
    s.mesh.material.opacity = 0.65 * (1 - s.t)
  })

  controls && controls.update()
  renderer && renderer.render(scene, camera)
  labelRenderer && labelRenderer.render(scene, camera)
}

/* ================= 数据变化 → 重建数据层 ================= */
let geoJsonCache = null
watch(() => props.cityData, async (val) => {
  if (!val || !val.length || !scene) return
  if (!geoJsonCache) {
    try {
      const res = await fetch('/shaanxi.json')
      geoJsonCache = await res.json()
    } catch (e) { return }
  }
  // 清理旧数据层
  if (dataGroup) {
    disposeObject(dataGroup)
    scene.remove(dataGroup)
    flyBalls = []; floatDots = []; scanRings = []
    labelEls.forEach(el => el.remove()); labelEls = []
    dataGroup = null
  }
  entranceT = Math.max(entranceT, 0.99) // 已入场状态，光柱直接生长
  buildDataLayer(geoJsonCache)
  scene.add(dataGroup)
}, { deep: true })

function disposeObject(obj) {
  obj.traverse(o => {
    if (o.geometry) o.geometry.dispose()
    if (o.material) {
      const mats = Array.isArray(o.material) ? o.material : [o.material]
      mats.forEach(m => m.dispose())
    }
    if (o.element) o.element.remove() // CSS2DObject
  })
}

onMounted(async () => { await nextTick(); init() })

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  if (ro) { ro.disconnect(); ro = null }
  if (renderer) {
    renderer.domElement.removeEventListener('mousemove', onHover)
    renderer.domElement.removeEventListener('mouseleave', hideTip)
  }
  if (controls) controls.dispose()
  // 全场景 dispose
  if (scene) {
    scene.traverse(o => {
      if (o.geometry) o.geometry.dispose()
      if (o.material) {
        (Array.isArray(o.material) ? o.material : [o.material]).forEach(m => {
          if (m.map) m.map.dispose()
          m.dispose()
        })
      }
      if (o.element) o.element.remove()
    })
  }
  threeObjects.forEach(o => o.dispose && o.dispose())
  threeObjects = []
  if (renderer) { renderer.dispose(); renderer.domElement.remove(); renderer = null }
  if (labelRenderer) { labelRenderer.domElement.remove(); labelRenderer = null }
  mapMeshes = []; flyBalls = []; floatDots = []; scanRings = []; labelEls = []
  scene = null; camera = null; controls = null
})
</script>

<style scoped>
.three-map {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
.three-canvas {
  display: block;
}

/* CSS2D 标签样式（非 scoped 作用，CSS2DRenderer 挂载到 body 级 DOM 时仍生效） */
:deep(.holo-label) {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  transform: translateY(-4px);
  pointer-events: none;
  font-family: 'Rajdhani', 'Microsoft YaHei', sans-serif;
}
:deep(.hl-name) {
  font-size: 12px;
  font-weight: 600;
  color: #bfe9ff;
  text-shadow: 0 0 8px rgba(0, 229, 255, 0.7), 0 1px 2px #000;
  letter-spacing: 1px;
}
:deep(.hl-val) {
  font-size: 11px;
  color: #57e6c8;
  text-shadow: 0 0 6px rgba(0, 255, 163, 0.5);
}
:deep(.holo-label.is-xian .hl-name) {
  color: #ffe2a0;
  font-size: 13px;
  text-shadow: 0 0 10px rgba(255, 215, 106, 0.9);
}
:deep(.holo-label.is-xian .hl-val) {
  color: #ffd76a;
}

/* hover tooltip */
.holo-tip {
  position: absolute;
  z-index: 10;
  min-width: 132px;
  padding: 8px 12px;
  border-radius: 6px;
  background: rgba(3, 11, 34, 0.92);
  border: 1px solid rgba(0, 229, 255, 0.45);
  box-shadow: 0 0 18px rgba(0, 229, 255, 0.25);
  pointer-events: none;
  font-size: 12px;
  color: #c9e1ff;
}
.tip-title {
  font-weight: 700;
  color: #7fe6ff;
  margin-bottom: 4px;
  letter-spacing: 1px;
}
.tip-row {
  line-height: 1.7;
  color: #8fb4dd;
}
.tip-row b {
  color: #ffcc00;
  font-family: 'Rajdhani', monospace;
}
.tip-row b.masked {
  color: #ffcc00aa;
}
.tip-fade-enter-active, .tip-fade-leave-active {
  transition: opacity 0.18s;
}
.tip-fade-enter-from, .tip-fade-leave-to {
  opacity: 0;
}

.three-hint {
  position: absolute;
  right: 8px;
  bottom: 6px;
  z-index: 5;
  font-size: 10px;
  letter-spacing: 1px;
  color: rgba(111, 140, 184, 0.75);
  pointer-events: none;
}
.three-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ff4d6a;
  font-size: 13px;
  letter-spacing: 2px;
}
</style>
