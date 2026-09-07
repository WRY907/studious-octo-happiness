<template>
  <canvas ref="canvasEl" class="particle-bg"></canvas>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

/* 全屏粒子背景：漂移粒子 + 近距连线（青/紫双色），DPR 适配 */
const canvasEl = ref(null)
let ctx = null
let particles = []
let rafId = null
let W = 0
let H = 0
let running = false

const LINK_DIST = 120 // 连线距离阈值

function makeParticles() {
  // 数量按屏幕面积缩放（1920×1080 基准约 120 个）
  const base = (1920 * 1080) / 120
  const count = Math.min(160, Math.max(40, Math.round((W * H) / base)))
  particles = Array.from({ length: count }, () => ({
    x: Math.random() * W,
    y: Math.random() * H,
    vx: (Math.random() - 0.5) * 0.3,
    vy: (Math.random() - 0.5) * 0.3,
    r: 1 + Math.random() * 1.4,
    cyan: Math.random() > 0.4 // 青色 / 紫色交替
  }))
}

function resize() {
  const dpr = Math.min(2, window.devicePixelRatio || 1)
  W = window.innerWidth
  H = window.innerHeight
  const c = canvasEl.value
  c.width = W * dpr
  c.height = H * dpr
  c.style.width = W + 'px'
  c.style.height = H + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  makeParticles()
}

function draw() {
  ctx.clearRect(0, 0, W, H)
  const n = particles.length
  // 连线
  for (let i = 0; i < n; i++) {
    const a = particles[i]
    for (let j = i + 1; j < n; j++) {
      const b = particles[j]
      const dx = a.x - b.x
      const dy = a.y - b.y
      const d2 = dx * dx + dy * dy
      if (d2 < LINK_DIST * LINK_DIST) {
        const alpha = (1 - Math.sqrt(d2) / LINK_DIST) * 0.22
        ctx.strokeStyle = (a.cyan && b.cyan)
          ? `rgba(0, 229, 255, ${alpha})`
          : `rgba(123, 104, 238, ${alpha})`
        ctx.lineWidth = 1
        ctx.beginPath()
        ctx.moveTo(a.x, a.y)
        ctx.lineTo(b.x, b.y)
        ctx.stroke()
      }
    }
  }
  // 粒子
  for (let i = 0; i < n; i++) {
    const p = particles[i]
    p.x += p.vx
    p.y += p.vy
    if (p.x < -10) p.x = W + 10
    if (p.x > W + 10) p.x = -10
    if (p.y < -10) p.y = H + 10
    if (p.y > H + 10) p.y = -10
    ctx.fillStyle = p.cyan ? 'rgba(0, 229, 255, 0.55)' : 'rgba(123, 104, 238, 0.5)'
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fill()
  }
  rafId = requestAnimationFrame(draw)
}

function start() {
  if (running) return
  running = true
  rafId = requestAnimationFrame(draw)
}
function stop() {
  running = false
  if (rafId) cancelAnimationFrame(rafId)
  rafId = null
}

function onVis() {
  document.hidden ? stop() : start()
}

onMounted(() => {
  // 无障碍：减少动态效果时完全跳过
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  ctx = canvasEl.value.getContext('2d')
  resize()
  start()
  window.addEventListener('resize', resize)
  document.addEventListener('visibilitychange', onVis)
})

onBeforeUnmount(() => {
  stop()
  window.removeEventListener('resize', resize)
  document.removeEventListener('visibilitychange', onVis)
})
</script>

<style scoped>
.particle-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
</style>
