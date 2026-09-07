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
          <router-link to="/" class="link-bigscreen">🖥 查看大屏</router-link>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import '@/styles/admin.css'

const route = useRoute()
const router = useRouter()

/* ===== 登录态信息（localStorage） ===== */
const username = ref(localStorage.getItem('hw_username') || '未登录')
const merchantName = ref(localStorage.getItem('hw_merchant') || '')
const isAdmin = computed(() => localStorage.getItem('hw_role') === 'ADMIN')

/* ===== 面包屑标题 ===== */
const titleMap = {
  '/admin/dashboard': '数据总览',
  '/admin/phones': '手机型号管理',
  '/admin/orders': '订单管理',
  '/admin/import': 'Excel 数据导入',
  '/admin/users': '用户管理'
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
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>
