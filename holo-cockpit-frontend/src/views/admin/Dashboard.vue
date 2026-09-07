<template>
  <div class="dashboard-page">
    <!-- 欢迎条 -->
    <div class="welcome-bar">
      <div>
        <div class="welcome-text">欢迎回来，<b>{{ username }}</b></div>
        <div class="welcome-sub">
          {{ isAdmin ? '您正在以管理员身份查看全局数据' : '您正在以商家身份查看本店数据' }}
        </div>
      </div>
      <div class="welcome-date">
        <div>{{ todayStr }}</div>
        <div>{{ isAdmin ? 'ADMIN CONSOLE' : 'MERCHANT CONSOLE' }}</div>
      </div>
    </div>

    <!-- 统计加载失败提示 -->
    <div v-if="statsFailed" class="notice-bar notice-danger">
      <span>⚠️</span>
      <span>统计数据加载失败：{{ statsError }}，请检查后端服务后</span>
      <button class="btn btn-sm" @click="loadStats">重试</button>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div
        v-for="card in visibleCards"
        :key="card.key"
        class="stat-box"
        :style="{ '--c': card.color }"
      >
        <div class="stat-icon">{{ card.icon }}</div>
        <div class="stat-info">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-num">
            {{ card.format === 'money' ? formatMoney(card.value) : formatNum(card.value) }}
            <span v-if="card.format === 'money' && card.value !== null" class="stat-unit">元</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 近期订单 -->
    <div class="admin-card">
      <div class="card-title">
        近期订单
        <span class="title-en">Recent Orders</span>
      </div>

      <!-- 加载中 -->
      <div v-if="ordersLoading" class="loading-state">
        <div class="loading-dots"></div>
        <div>订单加载中…</div>
      </div>

      <!-- 加载失败 -->
      <div v-else-if="ordersFailed" class="empty-state">
        <div class="empty-icon">⚠️</div>
        <div class="empty-text">订单数据加载失败</div>
        <div class="empty-sub">{{ ordersError }}，请稍后重试</div>
        <button class="btn btn-sm" style="margin-top: 14px" @click="loadOrders">重新加载</button>
      </div>

      <!-- 空数据 -->
      <div v-else-if="!recentOrders.length" class="empty-state">
        <div class="empty-icon">📭</div>
        <div class="empty-text">暂无订单数据</div>
        <div class="empty-sub">可前往「数据导入」页面上传订单 Excel，或等待新订单产生</div>
      </div>

      <!-- 订单表格 -->
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>订单号</th>
              <th>机型</th>
              <th v-if="isAdmin">金额</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in recentOrders" :key="row.id">
              <td class="num">{{ row.createTime }}</td>
              <td class="num">{{ row.orderNo }}</td>
              <td>{{ row.modelName }}</td>
              <td v-if="isAdmin" class="num">{{ formatMoney(row.amount) }}</td>
              <td>
                <span class="tag" :class="statusTag(row.status)">{{ statusText(row.status) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api'

/* ===== 登录态 ===== */
const username = ref(localStorage.getItem('hw_username') || '用户')
const isAdmin = computed(() => localStorage.getItem('hw_role') === 'ADMIN')

/* ===== 日期显示 ===== */
const todayStr = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
})

/* ===== 统计卡片 ===== */
const stats = ref({})
const statsFailed = ref(false)
const statsError = ref('')

// 卡片定义（ADMIN 6 张 / MERCHANT 3 张，通过 key 过滤）
const cardDefs = [
  { key: 'phoneCount', label: '手机型号数', icon: '📱', color: '#00e5ff' },
  { key: 'orderCount', label: '订单总数', icon: '📦', color: '#00ffa3' },
  { key: 'userCount', label: '系统用户', icon: '👥', color: '#7b68ee', adminOnly: true },
  { key: 'todaySales', label: '今日销售额', icon: '💰', color: '#ffcc00', adminOnly: true, format: 'money' },
  { key: 'todayOrders', label: '今日订单', icon: '🧾', color: '#ff6b8a' },
  { key: 'totalSales', label: '累计销售额', icon: '📈', color: '#00a5ff', adminOnly: true, format: 'money' }
]

const visibleCards = computed(() =>
  cardDefs
    .filter(c => isAdmin.value || !c.adminOnly)
    .map(c => ({ ...c, value: stats.value[c.key] ?? null }))
)

/* ===== 数字/金额格式化 ===== */
function formatNum(v) {
  if (v === null || v === undefined || isNaN(Number(v))) return '—'
  return Number(v).toLocaleString('zh-CN')
}

function formatMoney(v) {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return isAdmin.value ? '¥ —' : '---'
  return '¥ ' + Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

/* ===== 订单状态映射：1黄 2蓝 3紫 4绿 ===== */
const STATUS_MAP = {
  1: { text: '待付款', tag: 'tag-warning' },
  2: { text: '已付款', tag: 'tag-info' },
  3: { text: '已发货', tag: 'tag-accent' },
  4: { text: '已完成', tag: 'tag-success' }
}

function statusText(s) {
  return STATUS_MAP[s]?.text || '未知'
}

function statusTag(s) {
  return STATUS_MAP[s]?.tag || 'tag-muted'
}

/* ===== 近期订单（最新 8 条） ===== */
const recentOrders = ref([])
const ordersLoading = ref(false)
const ordersFailed = ref(false)
const ordersError = ref('')

async function loadOrders() {
  ordersLoading.value = true
  ordersFailed.value = false
  try {
    const data = await adminApi.getOrders({ page: 1, size: 8 })
    recentOrders.value = data?.records || []
  } catch (e) {
    ordersFailed.value = true
    ordersError.value = e?.response?.data?.message || e?.message || '服务异常'
    recentOrders.value = []
  } finally {
    ordersLoading.value = false
  }
}

/* ===== 统计加载 ===== */
async function loadStats() {
  statsFailed.value = false
  try {
    const data = await adminApi.getStats()
    stats.value = data || {}
  } catch (e) {
    statsFailed.value = true
    statsError.value = e?.response?.data?.message || e?.message || '服务异常'
    stats.value = {}
  }
}

onMounted(() => {
  loadStats()
  loadOrders()
})
</script>

<style scoped>
.dashboard-page {
  width: 100%;
}
</style>
