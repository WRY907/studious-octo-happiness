<template>
  <div class="rank-list">
    <div
      v-for="(item, idx) in displayList"
      :key="item.rankNo || idx"
      class="rank-item"
      :style="{ animationDelay: (idx * 0.08) + 's' }"
    >
      <div class="rank-no" :class="'r' + ((item.rankNo || idx + 1))">
        {{ item.rankNo || idx + 1 }}
      </div>
      <div class="rank-info">
        <div class="rank-name">{{ item.modelName }}</div>
        <div class="rank-bar-wrap">
          <div
            class="rank-bar"
            :style="{ width: barWidth(item) + '%', '--bar-color': barColor(item, idx) }"
          ></div>
        </div>
        <div class="rank-stats">
          <span class="sales">销售额 <b>¥{{ formatAmt(item.salesAmount) }}</b></span>
          <span class="count">销量 {{ formatNum(item.salesCount) }}</span>
          <span
            class="growth"
            :class="Number(item.growth) >= 0 ? 'up' : 'down'"
          >{{ Number(item.growth) >= 0 ? '+' : '' }}{{ item.growth }}%</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ data: { type: Array, default: () => [] } })

const displayList = computed(() => (props.data || []).slice(0, 10))

/* 条形度量：优先销售额；商家脱敏（null）时退化为销量相对值 */
function metric(item) {
  if (item.salesAmount !== null && item.salesAmount !== undefined) return Number(item.salesAmount)
  return Number(item.salesCount || 0)
}
const maxSales = computed(() => {
  let max = 0
  displayList.value.forEach(i => { const v = metric(i); if (v > max) max = v })
  return max || 1
})

function barWidth(item) {
  return (metric(item) / maxSales.value) * 100
}

function barColor(item, idx) {
  const r = item.rankNo || idx + 1
  if (r === 1) return '#ffd700'
  if (r === 2) return '#c0c0c0'
  if (r === 3) return '#cd7f32'
  return '#00d4ff'
}

function formatAmt(v) {
  // 商家脱敏：金额为 null 时显示 ---
  if (v === null || v === undefined) return '---'
  return (Number(v) / 10000).toFixed(2) + '万'
}
function formatNum(v) { return Number(v || 0).toLocaleString() }
</script>

<style scoped>
.rank-list {
  height: 100%;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.rank-item {
  display: flex;
  gap: 12px;
  padding: 8px 10px;
  background: linear-gradient(90deg, rgba(0, 212, 255, 0.06), transparent);
  border-left: 2px solid rgba(0, 212, 255, 0.4);
  animation: fade-in-up 0.5s ease both;
  transition: transform 0.2s, background 0.2s;
}

.rank-item:hover {
  transform: translateX(2px);
  background: linear-gradient(90deg, rgba(0, 212, 255, 0.12), transparent);
}

.rank-no {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: #fff;
  background: rgba(122, 151, 197, 0.2);
  border-radius: 4px;
}
.rank-no.r1 { background: linear-gradient(135deg, #ffd700, #ff9500); box-shadow: 0 0 10px rgba(255, 215, 0, 0.5); color: #222; }
.rank-no.r2 { background: linear-gradient(135deg, #e8e8e8, #a0a0a0); box-shadow: 0 0 8px rgba(192, 192, 192, 0.4); color: #222; }
.rank-no.r3 { background: linear-gradient(135deg, #e8a36a, #8b5a2b); box-shadow: 0 0 8px rgba(205, 127, 50, 0.4); }

.rank-info { flex: 1; min-width: 0; }
.rank-name {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rank-bar-wrap {
  height: 5px;
  background: rgba(122, 151, 197, 0.1);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 4px;
}
.rank-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--bar-color, #00d4ff), #7b68ee);
  border-radius: 3px;
  box-shadow: 0 0 6px color-mix(in srgb, var(--bar-color, #00d4ff) 60%, transparent);
  transition: width 0.8s ease;
}
.rank-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  color: var(--text-muted);
}
.rank-stats .sales b { color: var(--color-warning); font-family: monospace; font-weight: 600; }
.rank-stats .count { color: #a8c4e8; }
.rank-stats .growth { margin-left: auto; font-family: monospace; font-weight: 600; }
.rank-stats .growth.up { color: var(--color-success); }
.rank-stats .growth.down { color: var(--color-danger); }
</style>
