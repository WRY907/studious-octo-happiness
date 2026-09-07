<template>
  <div class="order-marquee">
    <!-- 左侧固定标签 -->
    <div class="marquee-label">
      <span class="live-dot"></span>
      <span class="label-cn">实时订单</span>
      <span class="label-en font-data">LIVE</span>
    </div>

    <!-- 滚动视口 -->
    <div class="marquee-viewport">
      <div v-if="loopList.length" class="marquee-track">
        <!-- 内容渲染两遍，translateX(-50%) 实现无缝循环 -->
        <div class="marquee-group">
          <div v-for="(o, i) in loopList" :key="'a' + i" class="order-item">
            <span class="cell time font-data">{{ fmtTime(o.createTime) }}</span>
            <span class="divider">|</span>
            <span class="cell order-no font-data">{{ o.orderNo }}</span>
            <span class="divider">|</span>
            <span class="cell user">{{ o.userName }}</span>
            <span class="divider">|</span>
            <span class="cell model">{{ o.modelName }}</span>
            <span class="divider">|</span>
            <span class="cell amount font-data">¥{{ fmtAmount(o.amount) }}</span>
            <span class="divider">|</span>
            <span class="cell status" :class="'s' + o.status">{{ statusText(o.status) }}</span>
          </div>
        </div>
        <div class="marquee-group" aria-hidden="true">
          <div v-for="(o, i) in loopList" :key="'b' + i" class="order-item">
            <span class="cell time font-data">{{ fmtTime(o.createTime) }}</span>
            <span class="divider">|</span>
            <span class="cell order-no font-data">{{ o.orderNo }}</span>
            <span class="divider">|</span>
            <span class="cell user">{{ o.userName }}</span>
            <span class="divider">|</span>
            <span class="cell model">{{ o.modelName }}</span>
            <span class="divider">|</span>
            <span class="cell amount font-data">¥{{ fmtAmount(o.amount) }}</span>
            <span class="divider">|</span>
            <span class="cell status" :class="'s' + o.status">{{ statusText(o.status) }}</span>
          </div>
        </div>
      </div>
      <div v-else class="marquee-empty font-data">WAITING FOR ORDERS…</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  orders: { type: Array, default: () => [] }
})

/* 状态：1待付款黄 2已付款蓝 3已发货紫 4已完成绿 */
const STATUS = { 1: '待付款', 2: '已付款', 3: '已发货', 4: '已完成' }
function statusText(s) { return STATUS[Number(s)] || '未知' }

/* 列表过短时重复补足，保证轨道宽度超过视口（两半各渲染一遍） */
const loopList = computed(() => {
  const list = props.orders || []
  if (!list.length) return []
  const out = []
  while (out.length < 12) out.push(...list)
  return out.slice(0, Math.max(12, list.length))
})

function fmtTime(t) {
  if (!t) return '--:--:--'
  const d = new Date(String(t).replace(' ', 'T'))
  if (isNaN(d.getTime())) return '--:--:--'
  const p = n => (n < 10 ? '0' + n : n)
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function fmtAmount(v) {
  // 商家脱敏：金额为 null 时显示 ---
  if (v === null || v === undefined) return '---'
  const n = Number(v || 0)
  return isNaN(n) ? '0.00' : n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.order-marquee {
  height: 54px;
  flex-shrink: 0;
  display: flex;
  align-items: stretch;
  background: linear-gradient(160deg, rgba(8, 26, 60, 0.72), rgba(4, 14, 34, 0.82));
  border: 1px solid rgba(0, 229, 255, 0.14);
  border-radius: var(--panel-radius);
  overflow: hidden;
  position: relative;
  z-index: 1;
}

/* 左侧固定「实时订单 LIVE」 */
.marquee-label {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  background: linear-gradient(90deg, rgba(0, 60, 120, 0.45), rgba(0, 30, 70, 0.2));
  border-right: 1px solid rgba(0, 229, 255, 0.25);
  white-space: nowrap;
}
.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-danger);
  box-shadow: 0 0 8px var(--color-danger);
  animation: pulse-scale 1.4s infinite;
}
.label-cn {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 3px;
  color: var(--text-primary);
}
.label-en {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--color-danger);
}

/* 滚动视口 */
.marquee-viewport {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
}
.marquee-track {
  display: flex;
  width: max-content;
  animation: marquee-scroll 75s linear infinite; /* 75s 慢速滚动，便于观察 */
  will-change: transform;
}
.order-marquee:hover .marquee-track {
  animation-play-state: paused; /* hover 暂停 */
}
@keyframes marquee-scroll {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

.marquee-group {
  display: flex;
  flex-shrink: 0;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 22px;
  height: 54px;
  white-space: nowrap;
  border-right: 1px dashed rgba(0, 229, 255, 0.1);
}
.cell { font-size: 13px; color: var(--text-secondary); }
.cell.time { color: var(--text-muted); letter-spacing: 1px; }
.cell.order-no { color: #8fd8ff; letter-spacing: 0.5px; }
.cell.user { color: var(--text-secondary); }
.cell.model { color: var(--text-primary); font-weight: 500; }
.cell.amount {
  color: var(--color-warning);
  font-weight: 700;
  font-size: 14px;
  text-shadow: 0 0 10px rgba(255, 204, 0, 0.35);
}
.divider { color: rgba(0, 229, 255, 0.2); font-size: 11px; }

/* 状态标签 */
.cell.status {
  padding: 2px 10px;
  border-radius: 2px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
}
.cell.status.s1 { color: #ffcc00; border: 1px solid rgba(255, 204, 0, 0.45); background: rgba(255, 204, 0, 0.08); }
.cell.status.s2 { color: #38c6ff; border: 1px solid rgba(0, 165, 255, 0.45); background: rgba(0, 165, 255, 0.08); }
.cell.status.s3 { color: #a89bff; border: 1px solid rgba(123, 104, 238, 0.5); background: rgba(123, 104, 238, 0.1); }
.cell.status.s4 { color: #00ffa3; border: 1px solid rgba(0, 255, 163, 0.45); background: rgba(0, 255, 163, 0.08); }

.marquee-empty {
  width: 100%;
  text-align: center;
  font-size: 11px;
  letter-spacing: 3px;
  color: var(--text-faint);
}
</style>
