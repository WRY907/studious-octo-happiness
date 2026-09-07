<template>
  <div class="alert-toast-wrap">
    <transition-group name="toast">
      <div
        v-for="t in toasts"
        :key="t.key"
        class="toast-card"
        :class="'lv' + t.level"
      >
        <div class="toast-head">
          <span class="toast-level">{{ levelText(t.level) }}</span>
          <span class="toast-title">{{ t.title }}</span>
          <button class="toast-close" title="关闭" @click="removeToast(t.key)">✕</button>
        </div>
        <div class="toast-body">{{ t.content }}</div>
        <div v-if="t.aiAdvice" class="toast-advice">
          <span class="advice-tag font-data">AI 建议</span>{{ t.aiAdvice }}
        </div>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { aiApi } from '@/api'
import { audioManager } from '@/audio/manager'

/* 智能预警 Toast：挂载拉取当前预警 + 每 60s 轮询新预警 */
const toasts = ref([])
let knownIds = new Set()
let timer = null
let keySeq = 0
const shown = new Set() // 防止重复弹出

const LEVEL_TEXT = { 1: '提示', 2: '警告', 3: '紧急' }
function levelText(l) { return LEVEL_TEXT[Number(l)] || '提示' }

function pushToast(alert) {
  const id = String(alert.id ?? alert.title)
  if (shown.has(id)) return
  shown.add(id)
  knownIds.add(id)
  const key = 't' + (++keySeq)
  toasts.value.push({
    key,
    id,
    level: Number(alert.level) || 1,
    title: alert.title || alert.alertType || '数据预警',
    content: alert.content || '',
    aiAdvice: alert.aiAdvice || ''
  })
  audioManager.alert()
  // 5s 自动消失
  setTimeout(() => removeToast(key), 5000)
}

function removeToast(key) {
  const idx = toasts.value.findIndex(t => t.key === key)
  if (idx > -1) toasts.value.splice(idx, 1)
}

/* 后端不可用时的演示预警（与驾驶舱 mock 降级一致） */
function mockAlerts() {
  const now = new Date().toISOString()
  return [
    {
      id: 'demo-a1',
      alertType: '库存预警',
      title: 'Mate 60 Pro 库存偏低',
      content: '西安仓 Mate 60 Pro（12G+512G）仅剩 86 台，按当前销量预计 2 天内售罄。',
      aiAdvice: '建议从深圳中心仓紧急调拨 500 台，并开启每人限购 2 台。',
      level: 2,
      createTime: now
    },
    {
      id: 'demo-a2',
      alertType: '流量预警',
      title: '宝鸡市流量异常上涨',
      content: '宝鸡市近 1 小时访问量环比上涨 186%，转化率同步走高。',
      aiAdvice: '疑似本地促销活动引流，建议加大服务器带宽并追加备货。',
      level: 1,
      createTime: now
    }
  ]
}

async function initAlerts() {
  try {
    const list = await aiApi.getAlerts()
    ;(list || []).slice(-3).reverse().forEach(pushToast)
  } catch (e) {
    mockAlerts().forEach(pushToast)
  }
}

async function pollAlerts() {
  try {
    const list = await aiApi.checkAlerts()
    ;(list || []).forEach(pushToast)
  } catch (e) { /* 轮询失败静默，下轮重试 */ }
}

onMounted(() => {
  initAlerts()
  timer = setInterval(pollAlerts, 60000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.alert-toast-wrap {
  position: fixed;
  top: 104px;
  right: 20px;
  z-index: 998;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 330px;
  pointer-events: none; /* 容器不拦截，卡片自身可交互 */
}

.toast-card {
  pointer-events: auto;
  position: relative;
  padding: 10px 12px;
  background: rgba(5, 18, 42, 0.94);
  border-radius: 5px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.5);
  overflow: hidden;
}
/* 左侧等级色条 */
.toast-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
}
.toast-card.lv1 { border: 1px solid rgba(0, 165, 255, 0.45); }
.toast-card.lv1::before { background: #00a5ff; box-shadow: 0 0 10px rgba(0, 165, 255, 0.8); }
.toast-card.lv2 { border: 1px solid rgba(255, 159, 67, 0.5); }
.toast-card.lv2::before { background: #ff9f43; box-shadow: 0 0 10px rgba(255, 159, 67, 0.8); }
.toast-card.lv3 { border: 1px solid rgba(255, 77, 106, 0.55); }
.toast-card.lv3::before { background: #ff4d6a; box-shadow: 0 0 10px rgba(255, 77, 106, 0.85); }

.toast-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}
.toast-level {
  flex-shrink: 0;
  padding: 1px 8px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 2px;
  border-radius: 2px;
}
.toast-card.lv1 .toast-level { color: #38c6ff; border: 1px solid rgba(0, 165, 255, 0.5); }
.toast-card.lv2 .toast-level { color: #ffb27a; border: 1px solid rgba(255, 159, 67, 0.55); }
.toast-card.lv3 .toast-level { color: #ff8ba0; border: 1px solid rgba(255, 77, 106, 0.6); }

.toast-title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.toast-close {
  flex-shrink: 0;
  background: transparent;
  border: none;
  color: var(--text-faint);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 2px;
  transition: all var(--dur-fast);
}
.toast-close:hover { color: #fff; background: rgba(255, 77, 106, 0.3); }

.toast-body {
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--text-secondary);
}
.toast-advice {
  margin-top: 6px;
  padding: 6px 8px;
  font-size: 11px;
  line-height: 1.55;
  color: #9fe8d0;
  background: rgba(0, 255, 163, 0.06);
  border: 1px dashed rgba(0, 255, 163, 0.3);
  border-radius: 3px;
}
.advice-tag {
  margin-right: 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: var(--color-success);
}

/* 右上滑入 / 右上滑出（仅 transform/opacity） */
.toast-enter-active { transition: opacity 0.35s var(--ease-out), transform 0.35s var(--ease-out); }
.toast-leave-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.toast-enter-from { opacity: 0; transform: translateX(110%); }
.toast-leave-to { opacity: 0; transform: translateX(110%); }
.toast-move { transition: transform 0.3s var(--ease-out); }
</style>
