<template>
  <div class="landing" ref="rootEl">
    <!-- Three.js 星云粒子背景 -->
    <canvas ref="bgCanvas" class="bg-canvas"></canvas>
    <div class="bg-vignette"></div>

    <!-- 顶部导航 -->
    <header class="nav" :class="{ 'nav-scrolled': scrolled }">
      <div class="nav-logo">
        <span class="logo-dot"></span>
        <span class="logo-text">HOLO<span class="logo-thin">COCKPIT</span></span>
      </div>
      <nav class="nav-links">
        <a href="#showcase" class="nav-link" @click.prevent="scrollToSection('showcase')">机型矩阵</a>
        <a href="#features" class="nav-link" @click.prevent="scrollToSection('features')">核心能力</a>
        <a href="#stats" class="nav-link" @click.prevent="scrollToSection('stats')">数据规模</a>
      </nav>
      <div class="nav-cta">
        <router-link to="/login" class="nav-login">登录</router-link>
        <router-link to="/cockpit" class="nav-enter">
          进入驾驶舱
          <span class="nav-enter-arrow">→</span>
        </router-link>
      </div>
    </header>

    <!-- ============ HERO ============ -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-badge reveal" style="--d: 0.1s">
          <span class="badge-pulse"></span>
          NEXT-GEN RETAIL INTELLIGENCE
        </div>

        <h1 class="hero-title" aria-label="全息数据驾驶舱">
          <span
            v-for="(ch, i) in titleChars"
            :key="i"
            class="hero-char"
            :style="{ '--ci': i }"
          >{{ ch }}</span>
        </h1>

        <div class="hero-sub-en">
          <span class="type-en">{{ typedText }}</span><span class="type-caret"></span>
        </div>

        <p class="hero-desc reveal" style="--d: 1.2s">
          面向华为终端业务的全景数据可视化平台 —— 实时大屏 · AI 智能分析 · 双角色管理
        </p>

        <div class="hero-cta reveal" style="--d: 1.4s">
          <button ref="magnetBtn" class="btn-primary" @click="goCockpit">
            <span class="btn-shine"></span>
            <span class="btn-label">进入全息驾驶舱</span>
            <span class="btn-arrow">→</span>
          </button>
          <a href="#showcase" class="btn-ghost" @click.prevent="scrollToSection('showcase')">
            探索机型矩阵
            <span class="btn-ghost-line"></span>
          </a>
        </div>

        <!-- 悬浮数据芯片 -->
        <div class="hero-chip chip-a reveal" style="--d: 1.6s">
          <span class="chip-value">¥ 2.4亿+</span>
          <span class="chip-label">累计销售额</span>
        </div>
        <div class="hero-chip chip-b reveal" style="--d: 1.8s">
          <span class="chip-value">10ms</span>
          <span class="chip-label">实时刷新</span>
        </div>
        <div class="hero-chip chip-c reveal" style="--d: 2.0s">
          <span class="chip-value">AI 驱动</span>
          <span class="chip-label">智能决策</span>
        </div>
      </div>

      <div class="scroll-hint">
        <span class="scroll-text">SCROLL</span>
        <span class="scroll-line"></span>
      </div>
    </section>

    <!-- ============ MARQUEE 滚动带 ============ -->
    <section class="marquee-band">
      <div class="marquee marquee-left">
        <span v-for="n in 2" :key="'l' + n" class="marquee-track">
          <em v-for="k in marqueeWords" :key="k" class="marquee-word">{{ k }}</em>
        </span>
      </div>
      <div class="marquee marquee-right">
        <span v-for="n in 2" :key="'r' + n" class="marquee-track">
          <em v-for="k in marqueeWords2" :key="k" class="marquee-word dim">{{ k }}</em>
        </span>
      </div>
    </section>

    <!-- ============ 机型矩阵 ============ -->
    <section class="phones" id="showcase">
      <div class="sec-head">
        <span class="sec-no reveal">01</span>
        <h2 class="sec-title reveal" style="--d: 0.1s">
          真机机型矩阵<span class="sec-title-en">DEVICE MATRIX</span>
        </h2>
        <p class="sec-desc reveal" style="--d: 0.2s">
          来自华为商城的真实机型数据 · 实时价格 · 全系覆盖
        </p>
      </div>

      <div class="phone-grid" ref="phoneGrid">
        <div
          v-for="(p, i) in phones"
          :key="p.file"
          class="phone-card reveal"
          :style="{ '--d': (i % 6) * 0.08 + 's' }"
          :data-tilt="i"
        >
          <div class="card-glow"></div>
          <div class="card-img-wrap">
            <img :src="p.local" :alt="p.name" class="card-img" loading="lazy" draggable="false" />
            <div class="card-scan"></div>
          </div>
          <div class="card-info">
            <span class="card-name">{{ p.name }}</span>
            <span class="card-price">
              <i>¥</i>{{ p.price.toLocaleString() }}
            </span>
          </div>
          <span class="card-corner c-tl"></span>
          <span class="card-corner c-tr"></span>
          <span class="card-corner c-bl"></span>
          <span class="card-corner c-br"></span>
        </div>
      </div>
    </section>

    <!-- ============ 核心能力 ============ -->
    <section class="features" id="features">
      <div class="sec-head">
        <span class="sec-no reveal">02</span>
        <h2 class="sec-title reveal" style="--d: 0.1s">
          核心能力<span class="sec-title-en">CAPABILITIES</span>
        </h2>
        <p class="sec-desc reveal" style="--d: 0.2s">四大能力矩阵，驱动业务全链路智能化</p>
      </div>

      <div class="feature-grid">
        <div
          v-for="(f, i) in features"
          :key="f.title"
          class="feature-card reveal"
          :style="{ '--d': i * 0.12 + 's', '--fc': f.color }"
          :data-tilt="'f' + i"
        >
          <div class="feature-icon" v-html="f.icon"></div>
          <h3 class="feature-title">{{ f.title }}</h3>
          <p class="feature-desc">{{ f.desc }}</p>
          <span class="feature-num">0{{ i + 1 }}</span>
          <div class="feature-border"></div>
        </div>
      </div>
    </section>

    <!-- ============ 数据规模 ============ -->
    <section class="stats" id="stats">
      <div class="sec-head">
        <span class="sec-no reveal">03</span>
        <h2 class="sec-title reveal" style="--d: 0.1s">
          数据规模<span class="sec-title-en">DATA SCALE</span>
        </h2>
      </div>

      <div class="stats-grid">
        <div v-for="(s, i) in statItems" :key="s.label" class="stat-block reveal" :style="{ '--d': i * 0.1 + 's' }">
          <div class="stat-value">
            <span class="stat-num" :data-target="s.value" :data-suffix="s.suffix">0</span>
          </div>
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-bar"><span :style="{ width: '0%' }" :data-w="s.bar"></span></div>
        </div>
      </div>
    </section>

    <!-- ============ 最终 CTA ============ -->
    <section class="final-cta">
      <div class="cta-glow"></div>
      <h2 class="cta-title reveal">准备好进入<span class="cta-gradient">全息视界</span>了吗？</h2>
      <p class="cta-desc reveal" style="--d: 0.15s">开箱即用 · 演示数据已就绪 · AI 能力已接入</p>
      <div class="cta-actions reveal" style="--d: 0.3s">
        <button class="btn-primary btn-lg" @click="goCockpit">
          <span class="btn-shine"></span>
          <span class="btn-label">立即体验</span>
          <span class="btn-arrow">→</span>
        </button>
        <router-link to="/login" class="btn-ghost btn-lg">
          管理后台登录
          <span class="btn-ghost-line"></span>
        </router-link>
      </div>
      <div class="cta-accounts reveal" style="--d: 0.45s">
        <span class="acc-chip">admin / admin123 · 管理员</span>
        <span class="acc-chip">merchant / merchant123 · 商家</span>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer-inner">
        <span class="footer-logo">HOLO<span class="logo-thin">COCKPIT</span></span>
        <span class="footer-note">课程设计 · 数据为仿真演示 · 与华为真实业务无关</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as THREE from 'three'

const router = useRouter()
const rootEl = ref(null)
const bgCanvas = ref(null)
const phoneGrid = ref(null)
const magnetBtn = ref(null)
const scrolled = ref(false)

/* ===== 标题逐字 ===== */
const TITLE = '全息数据驾驶舱'
const titleChars = TITLE.split('')

/* ===== 打字机 ===== */
const TYPE_TEXT = 'HOLO DATA COCKPIT · SEE THE FUTURE'
const typedText = ref('')
let typeTimer = null

/* ===== Marquee 词条 ===== */
const marqueeWords = ['REALTIME DATA', 'AI ANALYSIS', 'HOLO UI', 'NL2SQL', 'SMART ALERT', 'E-COMMERCE', 'DATA VIZ']
const marqueeWords2 = ['Vue 3', 'Spring Boot', 'ECharts', 'Three.js', 'MyBatis-Plus', 'Doubao AI', 'JWT Auth']

/* ===== 爬取的真实机型数据 ===== */
const phones = [
  { name: 'Mate XT 2 非凡大师', file: 'mate-xt2', price: 23999, local: '/images/phones/mate-xt2.png' },
  { name: 'Mate X7', file: 'mate-x7', price: 11999, local: '/images/phones/mate-x7.png' },
  { name: 'Pura X Max', file: 'pura-x-max', price: 11999, local: '/images/phones/pura-x-max.png' },
  { name: 'Mate X6', file: 'mate-x6', price: 9999, local: '/images/phones/mate-x6.png' },
  { name: 'Mate 80 RS 非凡大师', file: 'mate-80-rs', price: 10999, local: '/images/phones/mate-80-rs.png' },
  { name: 'Mate 80 Pro Max', file: 'mate-80-pro-max', price: 8999, local: '/images/phones/mate-80-pro-max.png' },
  { name: 'Pura X View', file: 'pura-x-view', price: 6999, local: '/images/phones/pura-x-view.png' },
  { name: 'Mate 80 Pro', file: 'mate-80-pro', price: 6999, local: '/images/phones/mate-80-pro.png' },
  { name: 'Pura 90 Pro Max', file: 'pura-90-pro-max', price: 6499, local: '/images/phones/pura-90-pro-max.png' },
  { name: 'Pura 90 Pro', file: 'pura-90-pro', price: 5499, local: '/images/phones/pura-90-pro.png' },
  { name: 'Mate 80', file: 'mate-80', price: 5499, local: '/images/phones/mate-80.png' },
  { name: 'nova 16', file: 'nova-16', price: 2999, local: '/images/phones/nova-16.png' },
  { name: 'nova 16 SE', file: 'nova-16-se', price: 2699, local: '/images/phones/nova-16-se.png' },
  { name: '华为畅享 90 Pro Max', file: 'changxiang-90-pro-max', price: 1899, local: '/images/phones/changxiang-90-pro-max.png' }
]

/* ===== 特性卡 ===== */
const features = [
  {
    title: '全息数据大屏',
    desc: '深空科技风大屏，销售额翻牌动画、双地图飞线、粒子星空、矩阵雨与程序化音频。',
    color: '#00e5ff',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/><path d="M7 8l3 3-3 3M13 14h4" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  },
  {
    title: 'AI 智能分析',
    desc: '接入豆包大模型：自然语言查数 NL2SQL、智能预警、AI 对话助手、一键分析报告。',
    color: '#7b68ee',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2a7 7 0 0 1 7 7c0 2.4-1.2 4.1-2.5 5.4-.7.7-1.5 1.6-1.5 2.6h-6c0-1-.8-1.9-1.5-2.6C6.2 13.1 5 11.4 5 9a7 7 0 0 1 7-7z"/><path d="M9 21h6M10 24h4" stroke-linecap="round"/><path d="M12 6v6M9.5 9L12 11.5 14.5 9" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  },
  {
    title: '双角色权限体系',
    desc: 'JWT 认证 + 管理员/商家两层权限：商家金额脱敏、商户注册审批、字段级差异。',
    color: '#00ffa3',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2l8 4v6c0 5-3.5 8.5-8 10-4.5-1.5-8-5-8-10V6l8-4z"/><path d="M9 12l2 2 4-4" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  },
  {
    title: 'Excel 批量导入',
    desc: '8 类业务表模板下载、逐行校验、错误行定位、机型去重更新，20MB 大文件无忧。',
    color: '#ffcc00',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6z"/><path d="M14 2v6h6"/><path d="M8 13h8M8 17h8M8 9h2" stroke-linecap="round"/></svg>'
  }
]

/* ===== 统计 ===== */
const statItems = [
  { value: 12, suffix: ' 张', label: '业务数据表', bar: '92%' },
  { value: 30, suffix: ' 天', label: '仿真数据周期', bar: '80%' },
  { value: 14, suffix: ' 款', label: '真实机型覆盖', bar: '70%' },
  { value: 9, suffix: ' 组', label: '大屏实时图表', bar: '86%' }
]

function goCockpit() {
  router.push('/cockpit')
}

/* 锚点平滑滚动（避免与 hash 路由冲突） */
function scrollToSection(id) {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/* ================= Three.js 星云背景 ================= */
let three = null // { renderer, scene, camera, points, raf, mx, my }

function initThree() {
  const canvas = bgCanvas.value
  if (!canvas || !window.THREE_OK) return
  try {
    const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.setSize(window.innerWidth, window.innerHeight)

    const scene = new THREE.Scene()
    const camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 100)
    camera.position.z = 8

    /* 星云粒子：青→紫渐变顶点色，三层深度 */
    const COUNT = 2600
    const positions = new Float32Array(COUNT * 3)
    const colors = new Float32Array(COUNT * 3)
    const cA = new THREE.Color('#00e5ff')
    const cB = new THREE.Color('#7b68ee')
    const cC = new THREE.Color('#0a2a66')
    for (let i = 0; i < COUNT; i++) {
      /* 螺旋星系分布 */
      const r = Math.pow(Math.random(), 0.6) * 9
      const branch = i % 3
      const spin = r * 0.9 + (Math.random() - 0.5) * 0.6
      const angle = branch * (Math.PI * 2 / 3) + spin
      positions[i * 3] = Math.cos(angle) * r + (Math.random() - 0.5) * 0.8
      positions[i * 3 + 1] = (Math.random() - 0.5) * (3.2 - r * 0.25)
      positions[i * 3 + 2] = Math.sin(angle) * r * 0.55 + (Math.random() - 0.5) * 1.2

      const t = Math.random()
      const c = t < 0.12 ? cA.clone().lerp(cB, Math.random()) : (Math.random() < 0.5 ? cA : cC).clone().lerp(cB, Math.random() * 0.5)
      colors[i * 3] = c.r
      colors[i * 3 + 1] = c.g
      colors[i * 3 + 2] = c.b
    }
    const geo = new THREE.BufferGeometry()
    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    geo.setAttribute('color', new THREE.BufferAttribute(colors, 3))
    const mat = new THREE.PointsMaterial({
      size: 0.035,
      vertexColors: true,
      transparent: true,
      opacity: 0.85,
      depthWrite: false,
      blending: THREE.AdditiveBlending
    })
    const points = new THREE.Points(geo, mat)
    points.rotation.x = 0.35
    scene.add(points)

    /* 鼠标视差 */
    three = {
      renderer, scene, camera, points,
      mx: 0, my: 0, tmx: 0, tmy: 0, raf: 0
    }

    const onMouse = (e) => {
      three.tmx = (e.clientX / window.innerWidth - 0.5) * 2
      three.tmy = (e.clientY / window.innerHeight - 0.5) * 2
    }
    const onResize = () => {
      camera.aspect = window.innerWidth / window.innerHeight
      camera.updateProjectionMatrix()
      renderer.setSize(window.innerWidth, window.innerHeight)
    }
    window.addEventListener('mousemove', onMouse)
    window.addEventListener('resize', onResize)

    const tick = () => {
      three.mx += (three.tmx - three.mx) * 0.04
      three.my += (three.tmy - three.my) * 0.04
      points.rotation.y += 0.0009
      camera.position.x = three.mx * 0.9
      camera.position.y = -three.my * 0.55
      camera.lookAt(0, 0, 0)
      renderer.render(scene, camera)
      three.raf = requestAnimationFrame(tick)
    }
    tick()
    three.cleanup = () => {
      cancelAnimationFrame(three.raf)
      window.removeEventListener('mousemove', onMouse)
      window.removeEventListener('resize', onResize)
      geo.dispose()
      mat.dispose()
      renderer.dispose()
    }
  } catch (e) { /* WebGL 不可用时静默降级为纯 CSS 背景 */ }
}

/* ================= 滚动显现 ================= */
let io = null

function initReveal() {
  io = new IntersectionObserver((entries) => {
    entries.forEach((en) => {
      if (en.isIntersecting) {
        en.target.classList.add('in')
        /* 数字滚动 */
        const num = en.target.querySelector('.stat-num')
        if (num && !num.dataset.done) {
          num.dataset.done = '1'
          animateCounter(num)
        }
        /* 进度条 */
        const bar = en.target.querySelector('.stat-bar span')
        if (bar && bar.dataset.w) bar.style.width = bar.dataset.w
      }
    })
  }, { threshold: 0.15 })
  document.querySelectorAll('.reveal').forEach((el) => io.observe(el))
}

function animateCounter(el) {
  const target = parseInt(el.dataset.target, 10)
  const suffix = el.dataset.suffix || ''
  const dur = 1600
  const t0 = performance.now()
  const step = (t) => {
    const p = Math.min(1, (t - t0) / dur)
    const eased = 1 - Math.pow(1 - p, 3)
    el.textContent = Math.round(target * eased) + suffix
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

/* ================= 3D Tilt 卡片 ================= */
function initTilt() {
  const handler = (e) => {
    const card = e.target.closest('[data-tilt]')
    if (!card) return
    const rect = card.getBoundingClientRect()
    const x = (e.clientX - rect.left) / rect.width - 0.5
    const y = (e.clientY - rect.top) / rect.height - 0.5
    card.style.transform = `perspective(700px) rotateY(${x * 14}deg) rotateX(${-y * 14}deg) translateY(-4px)`
    const glow = card.querySelector('.card-glow, .feature-border')
    if (glow && card.querySelector('.card-glow')) {
      glow.style.background = `radial-gradient(circle at ${x * 100 + 50}% ${y * 100 + 50}%, rgba(0,229,255,0.22), transparent 65%)`
    }
  }
  const leave = (e) => {
    const card = e.target.closest('[data-tilt]')
    if (card) card.style.transform = ''
  }
  document.addEventListener('mousemove', handler)
  document.addEventListener('mouseout', leave)
  return () => {
    document.removeEventListener('mousemove', handler)
    document.removeEventListener('mouseout', leave)
  }
}
let tiltCleanup = null

/* ================= 磁吸按钮 ================= */
function initMagnet() {
  const btn = magnetBtn.value
  if (!btn) return
  const move = (e) => {
    const r = btn.getBoundingClientRect()
    const x = e.clientX - r.left - r.width / 2
    const y = e.clientY - r.top - r.height / 2
    btn.style.transform = `translate(${x * 0.18}px, ${y * 0.3}px)`
  }
  const leave = () => { btn.style.transform = '' }
  btn.addEventListener('mousemove', move)
  btn.addEventListener('mouseleave', leave)
  return () => {
    btn.removeEventListener('mousemove', move)
    btn.removeEventListener('mouseleave', leave)
  }
}
let magnetCleanup = null

/* ================= 导航滚动态 ================= */
function onScroll() {
  const st = rootEl.value ? rootEl.value.scrollTop : 0
  scrolled.value = st > 40
}

/* ================= 生命周期 ================= */
onMounted(async () => {
  window.THREE_OK = true
  await nextTick()
  initThree()
  initReveal()
  tiltCleanup = initTilt()
  magnetCleanup = initMagnet()
  if (rootEl.value) rootEl.value.addEventListener('scroll', onScroll, { passive: true })

  /* 打字机 */
  let ti = 0
  typeTimer = setInterval(() => {
    typedText.value = TYPE_TEXT.slice(0, ++ti)
    if (ti >= TYPE_TEXT.length) clearInterval(typeTimer)
  }, 55)
})

onBeforeUnmount(() => {
  clearInterval(typeTimer)
  if (rootEl.value) rootEl.value.removeEventListener('scroll', onScroll)
  if (io) io.disconnect()
  if (tiltCleanup) tiltCleanup()
  if (magnetCleanup) magnetCleanup()
  if (three && three.cleanup) three.cleanup()
})
</script>

<style scoped>
/* ================= 基础 ================= */
.landing {
  position: relative;
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  background: #030812;
  color: #c9e1ff;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
  scroll-behavior: smooth;
}

/* 自定义滚动条 */
.landing::-webkit-scrollbar {
  width: 8px;
}
.landing::-webkit-scrollbar-track {
  background: rgba(3, 8, 18, 0.9);
}
.landing::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(0, 229, 255, 0.35), rgba(123, 104, 238, 0.35));
  border-radius: 4px;
}
.landing::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(0, 229, 255, 0.6), rgba(123, 104, 238, 0.6));
}

.bg-canvas {
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.bg-vignette {
  position: fixed;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
    radial-gradient(ellipse 80% 60% at 50% -10%, rgba(0, 165, 255, 0.13), transparent 60%),
    radial-gradient(ellipse 70% 50% at 85% 110%, rgba(123, 104, 238, 0.1), transparent 60%);
}

.landing > section,
.landing > header,
.landing > footer {
  position: relative;
  z-index: 2;
}

/* ================= 滚动显现 ================= */
.reveal {
  opacity: 0;
  transform: translateY(36px);
  transition:
    opacity 0.9s cubic-bezier(0.16, 1, 0.3, 1) var(--d, 0s),
    transform 0.9s cubic-bezier(0.16, 1, 0.3, 1) var(--d, 0s);
}
.reveal.in {
  opacity: 1;
  transform: none;
}

/* ================= 导航 ================= */
.nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 48px;
  transition: all 0.4s ease;
}
.nav-scrolled {
  padding: 12px 48px;
  background: rgba(3, 8, 18, 0.72);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(0, 229, 255, 0.1);
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: 'Orbitron', monospace;
  font-weight: 900;
  font-size: 19px;
  letter-spacing: 2px;
  color: #f0f8ff;
}
.logo-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #00e5ff;
  box-shadow: 0 0 14px #00e5ff;
  animation: dotPulse 2.4s ease-in-out infinite;
}
@keyframes dotPulse {
  0%, 100% { box-shadow: 0 0 8px #00e5ff; }
  50% { box-shadow: 0 0 22px #00e5ff, 0 0 40px rgba(0, 229, 255, 0.4); }
}
.logo-thin { font-weight: 500; color: #6f8cb8; }

.nav-links {
  display: flex;
  gap: 36px;
}
.nav-link {
  color: #c9e1ff;
  text-decoration: none;
  font-size: 14px;
  letter-spacing: 1px;
  position: relative;
  padding: 4px 0;
  transition: color 0.3s;
}
.nav-link::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 0;
  height: 1px;
  background: linear-gradient(90deg, #00e5ff, #7b68ee);
  transition: width 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
.nav-link:hover { color: #f0f8ff; }
.nav-link:hover::after { width: 100%; }

.nav-cta { display: flex; align-items: center; gap: 18px; }
.nav-login {
  color: #c9e1ff;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}
.nav-login:hover { color: #00e5ff; }

.nav-enter {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 22px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  color: #03121f;
  text-decoration: none;
  background: linear-gradient(135deg, #00e5ff, #00a5ff);
  box-shadow: 0 0 20px rgba(0, 229, 255, 0.35);
  transition: all 0.3s;
}
.nav-enter:hover {
  box-shadow: 0 0 34px rgba(0, 229, 255, 0.6);
  transform: translateY(-1px);
}
.nav-enter-arrow { transition: transform 0.3s; }
.nav-enter:hover .nav-enter-arrow { transform: translateX(4px); }

/* ================= HERO ================= */
.hero {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 120px 24px 80px;
}

.hero-inner {
  text-align: center;
  max-width: 1000px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 20px;
  border-radius: 999px;
  border: 1px solid rgba(0, 229, 255, 0.25);
  background: rgba(0, 229, 255, 0.06);
  font-family: 'Rajdhani', monospace;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 3px;
  color: #00e5ff;
  margin-bottom: 36px;
}
.badge-pulse {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #00ffa3;
  animation: dotPulse 2s infinite;
}

.hero-title {
  font-size: clamp(52px, 9vw, 120px);
  font-weight: 900;
  line-height: 1.08;
  letter-spacing: 6px;
  margin: 0 0 28px;
  /* 三段均匀过渡 + 提亮末端：底部对比度 ≥8:1，避免下半部发暗发糊 */
  background: linear-gradient(180deg, #ffffff 0%, #a8e6ff 48%, #78b4ff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  /* 辉光减弱：小半径、轻偏移，避免文字下缘雾化 */
  filter: drop-shadow(0 3px 16px rgba(0, 165, 255, 0.18));
}

.hero-char {
  display: inline-block;
  opacity: 0;
  transform: translateY(70px) rotateX(60deg);
  filter: blur(10px);
  animation: charIn 1s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  animation-delay: calc(var(--ci) * 0.09s + 0.2s);
}
@keyframes charIn {
  to {
    opacity: 1;
    transform: none;
    filter: blur(0);
  }
}

.hero-sub-en {
  font-family: 'Orbitron', monospace;
  font-size: clamp(15px, 2vw, 20px);
  font-weight: 600;
  letter-spacing: 5px;
  color: #33e5ff;
  min-height: 26px;
  margin-bottom: 18px;
  text-shadow: 0 0 18px rgba(0, 229, 255, 0.35);
}
.type-caret {
  display: inline-block;
  width: 10px;
  height: 18px;
  margin-left: 4px;
  background: #00e5ff;
  vertical-align: -3px;
  animation: caretBlink 0.9s step-end infinite;
}
@keyframes caretBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.hero-desc {
  font-size: clamp(15px, 1.8vw, 19px);
  color: #8fb4dd;
  letter-spacing: 2px;
  margin-bottom: 48px;
}

/* CTA 按钮 */
.hero-cta {
  display: flex;
  gap: 24px;
  justify-content: center;
  flex-wrap: wrap;
}

.btn-primary {
  position: relative;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 18px 44px;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #02131f;
  background: linear-gradient(135deg, #00e5ff 0%, #00a5ff 60%, #7b68ee 130%);
  box-shadow:
    0 0 30px rgba(0, 229, 255, 0.4),
    0 8px 30px rgba(0, 0, 0, 0.5);
  transition: box-shadow 0.35s, transform 0.25s;
  font-family: inherit;
}
.btn-primary:hover {
  box-shadow: 0 0 55px rgba(0, 229, 255, 0.65), 0 8px 40px rgba(0, 0, 0, 0.5);
}
.btn-lg { padding: 21px 54px; font-size: 18px; }

.btn-shine {
  position: absolute;
  top: 0;
  left: -80%;
  width: 50%;
  height: 100%;
  background: linear-gradient(105deg, transparent, rgba(255, 255, 255, 0.55), transparent);
  transform: skewX(-20deg);
  animation: shine 3.2s ease-in-out infinite;
}
@keyframes shine {
  0% { left: -80%; }
  55%, 100% { left: 140%; }
}

.btn-arrow { font-family: 'Rajdhani', monospace; font-size: 20px; }

.btn-ghost {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 18px 40px;
  border-radius: 999px;
  border: 1px solid rgba(0, 229, 255, 0.35);
  color: #c9e1ff;
  text-decoration: none;
  font-size: 16px;
  letter-spacing: 2px;
  transition: all 0.35s;
  background: rgba(0, 229, 255, 0.03);
}
.btn-ghost:hover {
  border-color: rgba(0, 229, 255, 0.8);
  background: rgba(0, 229, 255, 0.08);
  box-shadow: inset 0 0 24px rgba(0, 229, 255, 0.06);
  color: #f0f8ff;
}

/* 悬浮数据芯片 */
.hero-chip {
  position: absolute;
  padding: 14px 22px;
  border-radius: 12px;
  background: rgba(6, 20, 48, 0.55);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(0, 229, 255, 0.18);
  display: flex;
  flex-direction: column;
  gap: 4px;
  animation: chipFloat 5s ease-in-out infinite;
}
.chip-a { top: 26%; left: 8%; animation-delay: 0s; }
.chip-b { top: 40%; right: 9%; animation-delay: 1.4s; }
.chip-c { bottom: 22%; left: 14%; animation-delay: 2.6s; }
@keyframes chipFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-14px); }
}
.chip-value {
  font-family: 'Rajdhani', monospace;
  font-size: 22px;
  font-weight: 700;
  color: #00e5ff;
  text-shadow: 0 0 16px rgba(0, 229, 255, 0.5);
}
.chip-label { font-size: 12px; color: #6f8cb8; letter-spacing: 1px; }

@media (max-width: 900px) {
  .hero-chip { display: none; }
  .nav-links { display: none; }
  .nav { padding: 14px 20px; }
}

/* 滚动提示 */
.scroll-hint {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.scroll-text {
  font-family: 'Orbitron', monospace;
  font-size: 10px;
  letter-spacing: 4px;
  color: #46618a;
}
.scroll-line {
  width: 1px;
  height: 48px;
  background: linear-gradient(180deg, #00e5ff, transparent);
  animation: scrollLine 1.8s ease-in-out infinite;
}
@keyframes scrollLine {
  0% { transform: scaleY(0); transform-origin: top; }
  45% { transform: scaleY(1); transform-origin: top; }
  55% { transform: scaleY(1); transform-origin: bottom; }
  100% { transform: scaleY(0); transform-origin: bottom; }
}

/* ================= MARQUEE ================= */
.marquee-band {
  padding: 10px 0 30px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
  -webkit-mask-image: linear-gradient(90deg, transparent, #000 12%, #000 88%, transparent);
  mask-image: linear-gradient(90deg, transparent, #000 12%, #000 88%, transparent);
}
.marquee {
  display: flex;
  overflow: hidden;
  white-space: nowrap;
}
.marquee-track {
  display: inline-flex;
  align-items: center;
  padding-right: 24px;
  animation: marqueeMove 26s linear infinite;
}
.marquee-right .marquee-track {
  animation-direction: reverse;
  animation-duration: 32s;
}
@keyframes marqueeMove {
  to { transform: translateX(-100%); }
}
.marquee-word {
  font-family: 'Orbitron', monospace;
  font-style: normal;
  font-size: clamp(20px, 3vw, 34px);
  font-weight: 900;
  letter-spacing: 3px;
  color: transparent;
  -webkit-text-stroke: 1px rgba(0, 229, 255, 0.45);
  margin: 0 26px;
  transition: all 0.3s;
}
.marquee-word:hover {
  color: rgba(0, 229, 255, 0.9);
  -webkit-text-stroke: 1px transparent;
  text-shadow: 0 0 26px rgba(0, 229, 255, 0.6);
}
.marquee-word.dim { -webkit-text-stroke: 1px rgba(123, 104, 238, 0.4); }
.marquee-word.dim:hover { color: rgba(123, 104, 238, 0.9); }

/* ================= 机型矩阵 ================= */
.phones {
  padding: 110px 48px 60px;
  max-width: 1440px;
  margin: 0 auto;
}

.sec-head {
  text-align: center;
  margin-bottom: 64px;
}
.sec-no {
  font-family: 'Orbitron', monospace;
  font-size: 13px;
  letter-spacing: 6px;
  color: #00e5ff;
  display: block;
  margin-bottom: 14px;
}
.sec-title {
  font-size: clamp(30px, 4.5vw, 46px);
  font-weight: 900;
  letter-spacing: 4px;
  color: #f0f8ff;
  display: inline-flex;
  align-items: baseline;
  gap: 16px;
  flex-wrap: wrap;
  justify-content: center;
}
.sec-title-en {
  font-family: 'Orbitron', monospace;
  font-size: clamp(12px, 1.4vw, 15px);
  font-weight: 500;
  letter-spacing: 4px;
  color: #46618a;
}
.sec-desc {
  margin-top: 16px;
  color: #6f8cb8;
  font-size: 15px;
  letter-spacing: 2px;
}

.phone-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 26px;
}

.phone-card {
  position: relative;
  border-radius: 16px;
  background: linear-gradient(165deg, rgba(8, 26, 60, 0.65), rgba(4, 14, 34, 0.85));
  border: 1px solid rgba(0, 229, 255, 0.12);
  padding: 26px 20px 20px;
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1), border-color 0.35s, box-shadow 0.35s;
  will-change: transform;
  cursor: default;
}
.phone-card:hover {
  border-color: rgba(0, 229, 255, 0.45);
  box-shadow:
    0 20px 50px rgba(0, 0, 0, 0.55),
    0 0 34px rgba(0, 229, 255, 0.12);
}

.card-glow {
  position: absolute;
  inset: 0;
  border-radius: 16px;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.35s;
}
.phone-card:hover .card-glow { opacity: 1; }

.card-img-wrap {
  position: relative;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
  border-radius: 10px;
  background:
    radial-gradient(circle at 50% 42%, rgba(0, 165, 255, 0.1), transparent 68%),
    rgba(2, 10, 26, 0.5);
  overflow: hidden;
}
.card-img {
  width: 150px;
  height: 150px;
  object-fit: contain;
  filter: drop-shadow(0 12px 24px rgba(0, 0, 0, 0.55));
  transition: transform 0.45s cubic-bezier(0.16, 1, 0.3, 1);
}
.phone-card:hover .card-img {
  transform: scale(1.12) translateY(-4px);
}

/* 全息扫描线 */
.card-scan {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, #00e5ff, transparent);
  opacity: 0;
}
.phone-card:hover .card-scan {
  opacity: 0.8;
  animation: cardScan 1.6s ease-in-out infinite;
}
@keyframes cardScan {
  0% { top: 0; }
  100% { top: 100%; }
}

.card-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.card-name {
  font-size: 14px;
  font-weight: 600;
  color: #c9e1ff;
  letter-spacing: 0.5px;
}
.card-price {
  font-family: 'Rajdhani', monospace;
  font-size: 17px;
  font-weight: 700;
  color: #00ffa3;
  white-space: nowrap;
}
.card-price i {
  font-style: normal;
  font-size: 12px;
  margin-right: 1px;
  color: #00ffa3aa;
}

.card-corner {
  position: absolute;
  width: 14px;
  height: 14px;
  border-color: rgba(0, 229, 255, 0.55);
  border-style: solid;
  opacity: 0;
  transition: opacity 0.35s;
}
.phone-card:hover .card-corner { opacity: 1; }
.c-tl { top: 6px; left: 6px; border-width: 1.5px 0 0 1.5px; }
.c-tr { top: 6px; right: 6px; border-width: 1.5px 1.5px 0 0; }
.c-bl { bottom: 6px; left: 6px; border-width: 0 0 1.5px 1.5px; }
.c-br { bottom: 6px; right: 6px; border-width: 0 1.5px 1.5px 0; }

/* ================= 核心能力 ================= */
.features {
  padding: 110px 48px;
  max-width: 1440px;
  margin: 0 auto;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(290px, 1fr));
  gap: 26px;
}

.feature-card {
  position: relative;
  padding: 40px 32px 36px;
  border-radius: 18px;
  background: linear-gradient(165deg, rgba(8, 26, 60, 0.5), rgba(4, 14, 34, 0.8));
  border: 1px solid rgba(0, 229, 255, 0.1);
  overflow: hidden;
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1), border-color 0.35s, box-shadow 0.35s;
  will-change: transform;
}
.feature-card:hover {
  border-color: color-mix(in srgb, var(--fc) 45%, transparent);
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.5);
}

.feature-border {
  position: absolute;
  inset: 0;
  border-radius: 18px;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.35s;
}
.feature-card:hover .feature-border { opacity: 1; }

.feature-icon {
  width: 58px;
  height: 58px;
  margin-bottom: 26px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--fc) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--fc) 25%, transparent);
  color: var(--fc);
}
.feature-icon svg { width: 30px; height: 30px; }

.feature-title {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #f0f8ff;
  margin-bottom: 14px;
}
.feature-desc {
  font-size: 14.5px;
  line-height: 1.85;
  color: #8fb4dd;
}

.feature-num {
  position: absolute;
  top: 22px;
  right: 26px;
  font-family: 'Orbitron', monospace;
  font-size: 44px;
  font-weight: 900;
  color: transparent;
  -webkit-text-stroke: 1px color-mix(in srgb, var(--fc) 30%, transparent);
  opacity: 0.7;
}

/* ================= 数据规模 ================= */
.stats {
  padding: 110px 48px 130px;
  max-width: 1200px;
  margin: 0 auto;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 22px;
}

.stat-block {
  text-align: center;
  padding: 44px 20px 38px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(8, 26, 60, 0.4), rgba(4, 14, 34, 0.7));
  border: 1px solid rgba(0, 229, 255, 0.1);
  position: relative;
  overflow: hidden;
}
.stat-block::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 1px;
  background: linear-gradient(90deg, transparent, #00e5ff, transparent);
}

.stat-num {
  font-family: 'Rajdhani', monospace;
  font-size: clamp(46px, 5vw, 64px);
  font-weight: 700;
  background: linear-gradient(180deg, #f0f8ff, #00e5ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 4px 24px rgba(0, 229, 255, 0.35));
}
.stat-label {
  margin-top: 10px;
  font-size: 14px;
  color: #6f8cb8;
  letter-spacing: 3px;
}
.stat-bar {
  margin: 22px auto 0;
  width: 56%;
  height: 3px;
  border-radius: 3px;
  background: rgba(0, 229, 255, 0.1);
  overflow: hidden;
}
.stat-bar span {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #00e5ff, #7b68ee);
  box-shadow: 0 0 12px rgba(0, 229, 255, 0.6);
  transition: width 1.6s cubic-bezier(0.16, 1, 0.3, 1) 0.3s;
}

/* ================= 最终 CTA ================= */
.final-cta {
  position: relative;
  text-align: center;
  padding: 140px 24px 150px;
  overflow: hidden;
}
.cta-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 720px;
  height: 720px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 165, 255, 0.14), rgba(123, 104, 238, 0.08) 45%, transparent 70%);
  pointer-events: none;
  animation: ctaGlow 6s ease-in-out infinite;
}
@keyframes ctaGlow {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.9; }
  50% { transform: translate(-50%, -50%) scale(1.12); opacity: 1; }
}

.cta-title {
  font-size: clamp(34px, 5.5vw, 58px);
  font-weight: 900;
  letter-spacing: 5px;
  color: #f0f8ff;
  margin-bottom: 20px;
}
.cta-gradient {
  background: linear-gradient(90deg, #00e5ff, #7b68ee, #00e5ff);
  background-size: 200% auto;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: gradientFlow 4s linear infinite;
}
@keyframes gradientFlow {
  to { background-position: 200% center; }
}

.cta-desc {
  color: #6f8cb8;
  font-size: 16px;
  letter-spacing: 3px;
  margin-bottom: 46px;
}

.cta-actions {
  display: flex;
  gap: 22px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 36px;
}

.cta-accounts {
  display: flex;
  gap: 14px;
  justify-content: center;
  flex-wrap: wrap;
}
.acc-chip {
  padding: 8px 18px;
  border-radius: 999px;
  font-family: 'Rajdhani', monospace;
  font-size: 13px;
  letter-spacing: 1px;
  color: #6f8cb8;
  border: 1px solid rgba(110, 140, 184, 0.25);
  background: rgba(6, 20, 48, 0.4);
}

/* ================= Footer ================= */
.footer {
  border-top: 1px solid rgba(0, 229, 255, 0.08);
  padding: 34px 48px;
}
.footer-inner {
  max-width: 1440px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 14px;
}
.footer-logo {
  font-family: 'Orbitron', monospace;
  font-weight: 900;
  letter-spacing: 2px;
  color: #c9e1ff;
  font-size: 16px;
}
.footer-note {
  font-size: 12.5px;
  color: #46618a;
  letter-spacing: 1px;
}

@media (max-width: 720px) {
  .phones, .features, .stats { padding-left: 20px; padding-right: 20px; }
  .footer { padding: 26px 20px; }
}
</style>
