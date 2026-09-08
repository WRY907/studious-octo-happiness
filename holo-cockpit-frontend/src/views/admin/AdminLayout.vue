<template>
  <div class="admin-layout">
    <!-- ===== 左侧侧边栏 ===== -->
    <aside class="admin-sidebar">
      <!-- Logo -->
      <div class="sidebar-logo">
        <span class="logo-icon">📱</span>
        <span class="logo-text">华为驾驶舱</span>
      </div>

      <!-- 角色徽章 -->
      <div class="sidebar-role">
        <span>{{ isAdmin ? '👑' : '🏪' }}</span>
        <span class="role-name">{{ isAdmin ? '管理员' : '商家' }}</span>
        <span v-if="!isAdmin && merchantName" class="role-merchant">{{ merchantName }}</span>
      </div>

      <!-- 导航 -->
      <nav class="sidebar-nav">
        <router-link to="/admin/dashboard" class="nav-item" active-class="active">
          <span class="nav-icon">📊</span>数据总览
        </router-link>
        <router-link to="/trend" class="nav-item" active-class="active">
          <span class="nav-icon">📈</span>销售趋势分析
        </router-link>
        <router-link to="/hotmodels" class="nav-item" active-class="active">
          <span class="nav-icon">🏆</span>热销机型分析
        </router-link>
        <router-link to="/traffic" class="nav-item" active-class="active">
          <span class="nav-icon">🛰</span>流量来源分析
        </router-link>
        <router-link to="/profile" class="nav-item" active-class="active">
          <span class="nav-icon">👥</span>用户画像分析
        </router-link>
                <router-link to="/admin/phones" class="nav-item" active-class="active">
          <span class="nav-icon">📱</span>手机型号
        </router-link>
        <router-link to="/admin/orders" class="nav-item" active-class="active">
          <span class="nav-icon">📦</span>订单管理
        </router-link>
        <router-link v-if="isAdmin" to="/admin/import" class="nav-item" active-class="active">
          <span class="nav-icon">📥</span>数据导入
        </router-link>
        <router-link v-if="isAdmin" to="/admin/users" class="nav-item" active-class="active">
          <span class="nav-icon">👥</span>用户管理
        </router-link>
        <router-link v-if="isAdmin" to="/admin/audit" class="nav-item" active-class="active">
          <span class="nav-icon">🛡️</span>商户审批
          <span v-if="pendingCount > 0" class="nav-badge" :title="`${pendingCount} 条待审核申请`">
            {{ pendingCount > 99 ? '99+' : pendingCount }}
          </span>
        </router-link>
      </nav>

      <!-- 底部用户区 -->
      <div class="sidebar-footer">
        <div class="user-avatar">{{ isAdmin ? '👑' : '🏪' }}</div>
        <div class="user-name">
          <div class="name-text">{{ username }}</div>
          <div class="name-sub">{{ isAdmin ? 'ADMIN' : 'MERCHANT' }}</div>
        </div>
        <button class="btn btn-sm btn-danger" title="退出登录" @click="handleLogout">退出</button>
      </div>
    </aside>

    <!-- ===== 主区域 ===== -->
    <div class="admin-main">
      <!-- 顶栏 -->
      <header class="admin-header">
        <div class="header-left">
          <span class="breadcrumb">
            华为数据驾驶舱
            <span class="sep">/</span>
            <span class="current">{{ currentTitle }}</span>
          </span>
        </div>
        <div class="header-right">
          <router-link to="/cockpit" class="link-bigscreen">🖥 查看大屏</router-link>
          <span class="header-time">{{ currentTime }}</span>
        </div>
      </header>

      <!-- 内容区（滚动 + 淡入过渡） -->
      <div class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>

    <!-- 新申请到达提醒 toast -->
    <transition name="toast">
      <div v-if="toast" class="layout-toast" :class="`toast-${toast.type}`">{{ toast.msg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminApi } from '@/api'
import '@/styles/admin.css'

const route = useRoute()
const router = useRouter()

/* ===== 登录态信息（localStorage） ===== */
const username = ref(localStorage.getItem('hw_username') || '未登录')
const merchantName = ref(localStorage.getItem('hw_merchant') || '')
const isAdmin = computed(() => localStorage.getItem('hw_role') === 'ADMIN')

/* ===== 待审批角标（管理员）：进入轮询，跨页签同步 ===== */
const pendingCount = ref(0)
let pollTimer = null
let notifiedCount = -1 // 已 toast 提醒过的数量（避免重复打扰）

async function loadPending() {
  if (!isAdmin.value) return
  try {
    const stats = await adminApi.getAuditStats()
    const n = Number(stats?.pendingCount || 0)
    pendingCount.value = n
    // 新申请到达时提醒一次（首次加载记录基线，之后有增量才提醒）
    if (notifiedCount >= 0 && n > notifiedCount) {
      showToast(`📥 有 ${n - notifiedCount} 条新的商家入驻申请待审核`, 'success')
    }
    notifiedCount = n
  } catch (e) { /* 静默失败：角标非关键功能 */ }
}

/* 审批页操作后即时同步角标（事件由 MerchantAudit 派发，避免 30s 轮询延迟） */
function onAuditChanged(e) {
  const n = Number(e?.detail || 0)
  pendingCount.value = n
  notifiedCount = n // 同步提醒基线，避免自己审批后误触发"新申请"提醒
}

/* ===== toast ===== */
const toast = ref(null)
let toastTimer = null
function showToast(msg, type = 'success') {
  toast.value = { msg, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = null), 3200)
}

/* ===== 面包屑标题 ===== */
const titleMap = {
  '/admin/dashboard': '数据总览',
  '/admin/phones': '手机型号管理',
  '/admin/orders': '订单管理',
  '/admin/import': 'Excel 数据导入',
  '/admin/users': '用户管理',
  '/admin/audit': '商户审批'
}
const currentTitle = computed(() => titleMap[route.path] || '管理后台')

/* ===== 顶栏时间 ===== */
const currentTime = ref('')
let timer = null

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

function updateTime() {
  const d = new Date()
  currentTime.value =
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/* ===== 退出登录：清空登录态并跳转 ===== */
function handleLogout() {
  localStorage.removeItem('hw_token')
  localStorage.removeItem('hw_role')
  localStorage.removeItem('hw_username')
  localStorage.removeItem('hw_merchant')
  router.push('/login')
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  // 待审角标：进入加载 + 每 30 秒轮询 + 监听审批页即时同步事件
  if (isAdmin.value) {
    loadPending()
    pollTimer = setInterval(loadPending, 30000)
    window.addEventListener('hw-audit-changed', onAuditChanged)
  }
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  if (pollTimer) clearInterval(pollTimer)
  window.removeEventListener('hw-audit-changed', onAuditChanged)
  clearTimeout(toastTimer)
})
</script>

<style scoped>
/* ===== 侧边栏待审角标 ===== */
.nav-item {
  position: relative;
}

.nav-badge {
  position: absolute;
  top: 50%;
  right: 10px;
  transform: translateY(-50%);
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  font-family: var(--font-data, monospace);
  color: #fff;
  background: linear-gradient(135deg, #ff4d6a, #ff7a45);
  border-radius: 999px;
  box-shadow: 0 0 8px rgba(255, 77, 106, 0.7);
  animation: badge-pulse 1.8s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% { box-shadow: 0 0 6px rgba(255, 77, 106, 0.55); }
  50%      { box-shadow: 0 0 14px rgba(255, 77, 106, 0.95); }
}

/* ===== 新申请提醒 toast（右下角，不遮内容） ===== */
.layout-toast {
  position: fixed;
  right: 24px;
  bottom: 28px;
  z-index: 999;
  padding: 12px 20px;
  font-size: 13px;
  letter-spacing: 0.5px;
  border-radius: 8px;
  border: 1px solid rgba(0, 229, 255, 0.4);
  background: rgba(6, 24, 56, 0.92);
  color: #c9e1ff;
  box-shadow: 0 0 24px rgba(0, 229, 255, 0.25);
  backdrop-filter: blur(8px);
}

.layout-toast.toast-error {
  border-color: rgba(255, 77, 106, 0.5);
  box-shadow: 0 0 24px rgba(255, 77, 106, 0.25);
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(12px);
}
</style>
