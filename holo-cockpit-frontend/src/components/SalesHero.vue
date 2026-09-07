<template>
  <div class="sales-hero">
    <!-- 径向光晕（呼吸） -->
    <div class="hero-glow"></div>

    <div class="hero-label">
      <span class="label-cn">今日销售额</span>
      <span class="label-en font-data">TODAY SALES</span>
    </div>

    <!-- 超大金额：¥ + 千分位数字，逐字符渲染（翻牌感）；脱敏模式显示锁定 -->
    <div v-if="locked" class="hero-locked">
      <span class="lock-icon">🔒</span>
      <span class="lock-text">商家视角 · 金额已脱敏</span>
      <span class="lock-en font-data">MERCHANT VIEW · MASKED</span>
    </div>
    <div v-else class="hero-value font-data" :class="{ flash }" :aria-label="'今日销售额 ' + formatFull">
      <span class="currency">¥</span>
      <span
        v-for="(ch, i) in displayChars"
        :key="i"
        class="char"
        :class="{ digit: /\d/.test(ch) }"
        :style="{ animationDelay: (i * 0.035) + 's' }"
      >{{ ch }}</span>
    </div>

    <!-- 环比 badge -->
    <div
      v-if="growth !== null && growth !== undefined"
      class="hero-growth font-data"
      :class="growth >= 0 ? 'up' : 'down'"
    >
      <span class="arrow">{{ growth >= 0 ? '▲' : '▼' }}</span>
      {{ Math.abs(Number(growth)).toFixed(2) }}%
      <span class="vs">环比昨日</span>
    </div>

    <!-- 底部对称装饰线 -->
    <div class="hero-deco">
      <span class="deco-line"></span>
      <span class="deco-diamond"></span>
      <span class="deco-line"></span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps({
  sales: { type: Number, default: 0 },
  growth: { type: Number, default: null }
})

/* 脱敏模式：sales 为 null 时显示锁定视图 */
const locked = computed(() => props.sales === null || props.sales === undefined)
const displaySales = computed(() => (locked.value ? 0 : Number(props.sales) || 0))

/* easeOut 900ms 数字滚动 */
const displayValue = ref(0)
let rafId = null

function animateTo(target) {
  cancelAnimationFrame(rafId)
  const from = displayValue.value
  const to = Number(target) || 0
  const dur = 900
  const start = performance.now()
  const step = now => {
    const p = Math.min(1, (now - start) / dur)
    const e = 1 - Math.pow(1 - p, 3) // easeOutCubic
    displayValue.value = Math.round(from + (to - from) * e)
    if (p < 1) rafId = requestAnimationFrame(step)
  }
  rafId = requestAnimationFrame(step)
}

onMounted(() => animateTo(displaySales.value))
watch(displaySales, (nv, ov) => {
  animateTo(nv)
  // 数据刷新闪光（非首次）
  if (ov !== undefined && nv !== ov) {
    flash.value = true
    clearTimeout(flashTimer)
    flashTimer = setTimeout(() => (flash.value = false), 800)
  }
})

/* 刷新闪光状态 */
const flash = ref(false)
let flashTimer = null

/* 千分位格式化 → 逐字符 */
const displayChars = computed(() =>
  Math.round(displayValue.value).toLocaleString('zh-CN').split('')
)
const formatFull = computed(() => '¥' + Math.round(displaySales.value).toLocaleString('zh-CN'))
</script>

<style scoped>
.sales-hero {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px 8px;
  background:
    linear-gradient(160deg, rgba(8, 26, 60, 0.55), rgba(4, 14, 34, 0.7));
  border: 1px solid rgba(0, 229, 255, 0.16);
  border-radius: var(--panel-radius);
  overflow: hidden;
  backdrop-filter: blur(8px);
}

/* 径向光晕呼吸 */
.hero-glow {
  position: absolute;
  left: 50%;
  top: 46%;
  width: 130%;
  aspect-ratio: 2.4 / 1;
  transform: translate(-50%, -50%);
  background: radial-gradient(ellipse at center,
    rgba(0, 229, 255, 0.16) 0%,
    rgba(0, 165, 255, 0.08) 38%,
    transparent 70%);
  pointer-events: none;
  animation: hero-breathe 4.5s ease-in-out infinite;
}
@keyframes hero-breathe {
  0%, 100% { opacity: 0.45; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 1; transform: translate(-50%, -50%) scale(1.06); }
}

.hero-label {
  display: flex;
  align-items: baseline;
  gap: 10px;
  position: relative;
}
.label-cn {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  color: var(--text-secondary);
}
.label-en {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 3px;
  color: var(--text-faint);
}

.hero-value {
  position: relative;
  display: flex;
  align-items: baseline;
  font-weight: 700;
  font-size: clamp(48px, 5vw, 72px);
  line-height: 1.05;
  letter-spacing: 2px;
  color: #aef4ff;
  font-variant-numeric: tabular-nums;
  text-shadow:
    0 0 18px rgba(0, 229, 255, 0.55),
    0 0 46px rgba(0, 165, 255, 0.35);
}
/* 数据刷新闪光（数字更新时文字光晕爆发） */
.hero-value.flash {
  animation: value-flash 0.8s ease;
}
@keyframes value-flash {
  0% {
    text-shadow:
      0 0 18px rgba(0, 229, 255, 0.55),
      0 0 46px rgba(0, 165, 255, 0.35),
      0 0 0 rgba(255, 255, 255, 0);
  }
  35% {
    text-shadow:
      0 0 24px rgba(0, 229, 255, 0.9),
      0 0 60px rgba(0, 165, 255, 0.55),
      0 0 30px rgba(255, 255, 255, 0.65);
  }
  100% {
    text-shadow:
      0 0 18px rgba(0, 229, 255, 0.55),
      0 0 46px rgba(0, 165, 255, 0.35),
      0 0 0 rgba(255, 255, 255, 0);
  }
}

/* 脱敏锁定视图（商家角色） */
.hero-locked {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  border: 1px dashed rgba(255, 204, 0, 0.4);
  border-radius: 6px;
  background: rgba(255, 204, 0, 0.05);
}
.lock-icon {
  font-size: 40px;
  filter: drop-shadow(0 0 12px rgba(255, 204, 0, 0.5));
  animation: pulse-glow 3s infinite;
}
.lock-text {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 3px;
  color: var(--color-warning);
}
.lock-en {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 2px;
  color: var(--text-faint);
}

.currency {
  font-size: 0.52em;
  margin-right: 8px;
  color: var(--color-primary);
  text-shadow: 0 0 14px rgba(0, 229, 255, 0.7);
}
.char { display: inline-block; }
/* 逐字符入场（仅 transform/opacity） */
.char.digit { animation: char-flip 0.5s var(--ease-out) both; }
@keyframes char-flip {
  from { transform: translateY(0.5em) scaleY(0.4); opacity: 0; }
  to { transform: translateY(0) scaleY(1); opacity: 1; }
}

.hero-growth {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
}
.hero-growth.up {
  color: var(--color-success);
  background: rgba(0, 255, 163, 0.08);
  border: 1px solid rgba(0, 255, 163, 0.35);
}
.hero-growth.down {
  color: var(--color-danger);
  background: rgba(255, 77, 106, 0.08);
  border: 1px solid rgba(255, 77, 106, 0.35);
}
.hero-growth .arrow { font-size: 10px; }
.hero-growth .vs {
  font-size: 10px;
  font-weight: 400;
  color: var(--text-muted);
  letter-spacing: 2px;
}

/* 底部对称装饰线 */
.hero-deco {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  margin-top: 2px;
}
.deco-line {
  flex: 1;
  height: 1px;
  max-width: 220px;
}
.hero-deco .deco-line:first-child {
  background: linear-gradient(90deg, transparent, rgba(0, 229, 255, 0.55));
}
.hero-deco .deco-line:last-child {
  background: linear-gradient(90deg, rgba(123, 104, 238, 0.55), transparent);
}
.deco-diamond {
  width: 6px;
  height: 6px;
  background: var(--color-primary);
  transform: rotate(45deg);
  box-shadow: 0 0 8px rgba(0, 229, 255, 0.7);
  animation: pulse-glow 3s infinite;
}
</style>
