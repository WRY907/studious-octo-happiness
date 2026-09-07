<template>
  <canvas ref="canvasEl" class="matrix-rain"></canvas>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

/* 左右屏幕边缘 60px 宽的竖向数字雨（绿/青色，低透明度），字符：0 1 华 为 */
const EDGE_W = 60
const COL_W = 15
const CHARS = '01华为'
const COLORS = ['#00ffa3', '#00e5ff']

const canvasEl = ref(null)
let ctx = null
let drops = [] // 每列当前的行位置
let cols = []  // 列的 x 坐标
let W = 0
let H = 0
let rafId = null
let lastTime = 0
let running = false

function setup() {
  const dpr = Math.min(2, window.devicePixelRatio || 1)
  W = window.innerWidth
  H = window.innerHeight
  const c = canvasEl.value
  c.width = W * dpr
  c.height = H * dpr
  c.style.width = W + 'px'
  c.style.height = H + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  // 只在左右两条 60px 边缘带内布列
  cols = []
  for (let x = 0; x < EDGE_W; x += COL_W) cols.push(x)
  for (let x = W - EDGE_W; x < W; x += COL_W) cols.push(x)
  drops = cols.map(() => Math.floor(Math.random() * (H / 16)))
}

function draw(now) {
  rafId = requestAnimationFrame(draw)
  // 节流到约 60ms 一帧，营造雨滴下落的顿挫感
  if (now - lastTime < 60) return
  lastTime = now
  // 用 destination-out 淡出旧字符，保持画布透明
  ctx.globalCompositeOperation = 'destination-out'
  ctx.fillStyle = 'rgba(0, 0, 0, 0.18)'
  ctx.fillRect(0, 0, W, H)
  ctx.globalCompositeOperation = 'source-over'
  ctx.font = '13px monospace'
  ctx.textBaseline = 'top'
  cols.forEach((x, i) => {
    const ch = CHARS[Math.floor(Math.random() * CHARS.length)]
    const color = COLORS[Math.floor(Math.random() * COLORS.length)]
    // 雨滴头亮、其余暗
    ctx.fillStyle = Math.random() > 0.85 ? color : 'rgba(0, 200, 160, 0.55)'
    ctx.fillText(ch, x, drops[i] * 16)
    const speed = Math.random() > 0.92 ? 2 : 1
    drops[i] = (drops[i] + speed) * 16 > H ? 0 : drops[i] + speed
  })
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

function onResize() { setup() }
function onVis() { document.hidden ? stop() : start() }

onMounted(() => {
  // 无障碍：减少动态效果时跳过
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  ctx = canvasEl.value.getContext('2d')
  setup()
  start()
  window.addEventListener('resize', onResize)
  document.addEventListener('visibilitychange', onVis)
})

onBeforeUnmount(() => {
  stop()
  window.removeEventListener('resize', onResize)
  document.removeEventListener('visibilitychange', onVis)
})
</script>

<style scoped>
.matrix-rain {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.15; /* 低透明度，仅作氛围层 */
}
</style>
