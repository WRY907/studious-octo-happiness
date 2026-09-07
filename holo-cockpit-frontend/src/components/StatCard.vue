<template>
  <div class="stat-card" :class="{ refreshing }" :style="{ '--card-color': color }">
    <div class="card-inner">
      <div class="card-icon">
        <svg class="icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path :d="iconPath" />
        </svg>
      </div>
      <div class="card-info">
        <div class="card-title-row">
          <span class="card-title">{{ title }}</span>
          <span class="card-en">{{ en }}</span>
        </div>
        <div class="card-value-row">
          <span class="card-value font-data">{{ displayValue }}</span>
          <span class="card-suffix">{{ suffix }}</span>
        </div>
        <div class="card-bottom">
          <div v-if="growth !== null && growth !== undefined" class="card-growth" :class="growth >= 0 ? 'growth-up' : 'growth-down'">
            <svg class="tri" viewBox="0 0 10 6" :class="{ down: growth < 0 }">
              <polygon points="5,0 10,6 0,6" fill="currentColor" />
            </svg>
            <span class="font-data">{{ Math.abs(growth) }}%</span>
            <span class="vs">环比</span>
          </div>
          <div v-else class="card-growth total-tag font-data">{{ tag || 'CUMULATIVE' }}</div>
          <!-- sparkline 迷你趋势 -->
          <svg v-if="sparkPoints" class="sparkline" :viewBox="`0 0 ${SPARK_W} ${SPARK_H}`" preserveAspectRatio="none">
            <defs>
              <linearGradient :id="`spark-${uid}`" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" :stop-color="color" stop-opacity="0.35" />
                <stop offset="100%" :stop-color="color" stop-opacity="0" />
              </linearGradient>
            </defs>
            <polygon :points="areaPoints" :fill="`url(#spark-${uid})`" />
            <polyline :points="sparkPoints" fill="none" :stroke="color" stroke-width="1.5" stroke-linejoin="round" />
            <circle :cx="lastPt.x" :cy="lastPt.y" r="2" :fill="color" />
          </svg>
        </div>
      </div>
    </div>
    <div class="card-glow"></div>
    <div class="card-scan"></div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, getCurrentInstance } from 'vue'

const props = defineProps({
  title: String,
  en: String,
  value: [String, Number],
  growth: { type: Number, default: null },
  suffix: String,
  icon: { type: String, default: 'sales' },
  color: { type: String, default: '#00e5ff' },
  // 迷你趋势数据（最近N天）
  trend: { type: Array, default: null },
  // growth 为空时的底部占位标签（如转化率卡用 "REALTIME"）
  tag: { type: String, default: null }
})

const uid = getCurrentInstance().uid
const SPARK_W = 60
const SPARK_H = 22

/* 图标路径（替代 emoji，更精致的线条图标） */
const ICON_PATHS = {
  sales: 'M12 2v20M17 6.5c0-1.9-2.2-3-5-3s-5 1.1-5 3 1.6 2.6 5 3.3 5 1.6 5 3.7-2.2 3-5 3-5-1.1-5-3',
  orders: 'M3 8l4.5-4L12 8M7.5 4v16M13 5h4l3 3v11a1 1 0 01-1 1H14a1 1 0 01-1-1V5zM15 9h3M15 13h3',
  users: 'M16 20v-2a4 4 0 00-4-4H7a4 4 0 00-4 4v2M9.5 10a3.5 3.5 0 100-7 3.5 3.5 0 000 7zM21 20v-2a4 4 0 00-3-3.85M16.5 3.13a4 4 0 010 7.75',
  visits: 'M1.5 12S5 5 12 5s10.5 7 10.5 7-3.5 7-10.5 7-10.5-7-10.5-7zM12 14.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5z',
  trophy: 'M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM7 6H4a3 3 0 003 5M17 6h3a3 3 0 01-3 5',
  target: 'M12 22a10 10 0 100-20 10 10 0 000 20zM12 18a6 6 0 100-12 6 6 0 000 12zM12 14a2 2 0 100-4 2 2 0 000 4z'
}
const iconPath = computed(() => ICON_PATHS[props.icon] || ICON_PATHS.sales)

/* ===== 数字滚动计数 ===== */
const displayValue = ref(String(props.value ?? '0'))
let rafId = null

function parseNum(str) {
  const n = parseFloat(String(str).replace(/,/g, ''))
  return isNaN(n) ? null : n
}
function decimalsOf(str) {
  const m = String(str).match(/\.(\d+)/)
  return m ? m[1].length : 0
}

function animateValue(from, to) {
  cancelAnimationFrame(rafId)
  const f = parseNum(from), t = parseNum(to)
  if (f === null || t === null) { displayValue.value = String(to); return }
  const digits = decimalsOf(to)
  const dur = 900
  const start = performance.now()
  const step = now => {
    const p = Math.min(1, (now - start) / dur)
    const e = 1 - Math.pow(1 - p, 3) // easeOutCubic
    const cur = f + (t - f) * e
    displayValue.value = cur.toLocaleString('zh-CN', {
      minimumFractionDigits: digits, maximumFractionDigits: digits
    })
    if (p < 1) rafId = requestAnimationFrame(step)
  }
  rafId = requestAnimationFrame(step)
}

watch(() => props.value, (nv, ov) => {
  animateValue(ov, nv)
  // 数据刷新脉冲波纹（非首次）
  if (ov !== undefined && parseNum(ov) !== null) {
    refreshing.value = true
    clearTimeout(refreshTimer)
    refreshTimer = setTimeout(() => (refreshing.value = false), 700)
  }
})

/* 刷新脉冲状态 */
const refreshing = ref(false)
let refreshTimer = null

onMounted(() => {
  // 首次入场：从 0 滚动到目标值
  const digits = decimalsOf(props.value)
  animateValue('0' + (digits ? '.' + '0'.repeat(digits) : ''), props.value)
})

/* ===== Sparkline ===== */
const spark = computed(() => {
  if (!props.trend || props.trend.length < 2) return null
  const vals = props.trend.slice(-14).map(Number)
  const min = Math.min(...vals), max = Math.max(...vals)
  const span = max - min || 1
  return vals.map((v, i) => ({
    x: (i / (vals.length - 1)) * (SPARK_W - 4) + 2,
    y: SPARK_H - 3 - ((v - min) / span) * (SPARK_H - 6)
  }))
})
const sparkPoints = computed(() => spark.value ? spark.value.map(p => `${p.x},${p.y}`).join(' ') : null)
const areaPoints = computed(() => {
  if (!spark.value) return null
  const pts = spark.value
  return `${pts[0].x},${SPARK_H} ` + pts.map(p => `${p.x},${p.y}`).join(' ') + ` ${pts[pts.length-1].x},${SPARK_H}`
})
const lastPt = computed(() => spark.value ? spark.value[spark.value.length - 1] : { x: 0, y: 0 })
</script>

<style scoped>
/* ===== 数据刷新脉冲波纹（30s 轮询数据更新时触发） ===== */
.stat-card.refreshing {
  animation: refresh-pulse 0.7s ease;
}
.stat-card.refreshing::after {
  content: '';
  position: absolute;
  inset: 0;
  border: 1px solid var(--card-color);
  border-radius: inherit;
  animation: refresh-ripple 0.7s ease-out forwards;
  pointer-events: none;
}
@keyframes refresh-pulse {
  0% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--card-color) 45%, transparent); }
  100% { box-shadow: 0 0 0 10px transparent; }
}
@keyframes refresh-ripple {
  0% { opacity: 0.9; transform: scale(1); }
  100% { opacity: 0; transform: scale(1.12); }
}

.stat-card {
  --card-color: #00e5ff;
  position: relative;
  padding: 11px 12px;
  background: linear-gradient(150deg, rgba(10, 30, 66, 0.75), rgba(4, 14, 34, 0.85));
  border: 1px solid rgba(0, 229, 255, 0.14);
  border-radius: 3px;
  overflow: hidden;
  transition: transform 0.3s var(--ease-out), border-color 0.3s;
  animation: fade-in-up 0.55s var(--ease-out) both;
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--card-color), transparent 70%);
  opacity: 0.7;
}
.stat-card:hover {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--card-color) 45%, transparent);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4), 0 0 16px color-mix(in srgb, var(--card-color) 12%, transparent);
}

.card-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.card-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--card-color);
  background: linear-gradient(135deg, color-mix(in srgb, var(--card-color) 16%, transparent), rgba(255,255,255,0.015));
  border: 1px solid color-mix(in srgb, var(--card-color) 35%, transparent);
  border-radius: 6px;
  flex-shrink: 0;
  box-shadow: inset 0 0 12px color-mix(in srgb, var(--card-color) 12%, transparent);
}
.icon-svg { width: 20px; height: 20px; filter: drop-shadow(0 0 4px color-mix(in srgb, var(--card-color) 60%, transparent)); }

.card-info { flex: 1; min-width: 0; }

.card-title-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 3px;
}
.card-title {
  font-size: 11.5px;
  color: var(--text-secondary);
  font-weight: 500;
  white-space: nowrap;
}
.card-en {
  font-family: var(--font-data);
  font-size: 8px;
  font-weight: 600;
  color: var(--text-faint);
  letter-spacing: 1.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-value-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 3px;
}
.card-value {
  font-size: 21px;
  font-weight: 700;
  color: var(--card-color);
  letter-spacing: 0.5px;
  line-height: 1.1;
  text-shadow: 0 0 12px color-mix(in srgb, var(--card-color) 55%, transparent);
  font-variant-numeric: tabular-nums;
}
.card-suffix {
  font-size: 10px;
  color: var(--text-muted);
}

.card-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 6px;
  min-height: 20px;
}

.card-growth {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  line-height: 1;
}
.tri { width: 8px; height: 5px; }
.tri.down { transform: rotate(180deg); }
.vs { font-size: 9px; color: var(--text-muted); margin-left: 1px; }
.card-growth.growth-up { color: var(--color-success); }
.card-growth.growth-down { color: var(--color-danger); }
.card-growth.total-tag {
  color: var(--text-faint);
  font-size: 8px;
  letter-spacing: 1.5px;
  opacity: 0.8;
}

.sparkline {
  width: 56px;
  height: 22px;
  flex-shrink: 0;
  overflow: visible;
}

.card-glow {
  position: absolute;
  top: -50%;
  right: -30%;
  width: 130px;
  height: 130px;
  background: radial-gradient(circle, color-mix(in srgb, var(--card-color) 15%, transparent), transparent 70%);
  pointer-events: none;
}

/* hover 扫光 */
.card-scan {
  position: absolute;
  top: 0; left: 0;
  width: 40%; height: 100%;
  background: linear-gradient(100deg, transparent, rgba(255,255,255,0.06), transparent);
  transform: translateX(-150%) skewX(-15deg);
  pointer-events: none;
}
.stat-card:hover .card-scan {
  animation: title-sweep 0.9s ease;
}
</style>
