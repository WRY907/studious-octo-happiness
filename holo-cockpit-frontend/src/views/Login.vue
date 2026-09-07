<template>
  <div class="login-page">
    <!-- 浮动光点背景 -->
    <div class="login-particles">
      <span
        v-for="n in 16"
        :key="n"
        class="particle"
        :style="particleStyle(n)"
      ></span>
    </div>

    <!-- 返回大屏 -->
    <router-link to="/" class="back-link">🖥 返回大屏</router-link>

    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- 四角 HUD 括号 -->
      <span class="hud hud-tl"></span>
      <span class="hud hud-tr"></span>
      <span class="hud hud-bl"></span>
      <span class="hud hud-br"></span>

      <div class="login-head">
        <div class="login-icon">📱</div>
        <h1 class="login-title">华为数据驾驶舱</h1>
        <p class="login-sub">管理后台 · ADMIN CONSOLE</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="login-field" :class="{ 'field-error': tried && !username }">
          <span class="field-icon">👤</span>
          <input
            v-model.trim="username"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
            :disabled="loading"
            maxlength="32"
          />
        </div>
        <div class="login-field" :class="{ 'field-error': tried && !password }">
          <span class="field-icon">🔒</span>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            :disabled="loading"
            maxlength="64"
          />
        </div>

        <button type="submit" class="login-btn btn-sweep" :disabled="loading">
          <span v-if="loading" class="btn-spinner"></span>
          <span>{{ loading ? '登录中…' : '登 录' }}</span>
        </button>
      </form>

      <div class="login-hint">
        <span class="hint-chip">admin / admin123 管理员</span>
        <span class="hint-chip">merchant / merchant123 商家</span>
      </div>

      <!-- 商家注册入口 -->
      <div class="register-bar">
        <span class="reg-text">想入驻平台的商家？</span>
        <router-link to="/register" class="reg-link">🏪 商家注册</router-link>
        <span class="reg-note">注册需管理员审核通过</span>
      </div>
    </div>

    <!-- 错误/警告 toast -->
    <transition name="toast">
      <div v-if="toast" class="toast" :class="`toast-${toast.type}`">{{ toast.msg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import '@/styles/admin.css'

const router = useRouter()

// 表单状态
const username = ref('')
const password = ref('')
const loading = ref(false)
const tried = ref(false) // 是否点击过登录（用于空值红框提示）

// toast 状态
const toast = ref(null)
let toastTimer = null

function showToast(msg, type = 'error') {
  toast.value = { msg, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = null), 2800)
}

// 浮动光点：确定性伪随机分布（仅 transform/opacity 动画）
function particleStyle(n) {
  const seed = (n * 137.5) % 100
  const top = (n * 41.7) % 100
  const size = 2 + (n % 4)
  const dur = 7 + (n % 6) * 2.2
  const delay = (n % 8) * 0.85
  return {
    left: `${seed}%`,
    top: `${top}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDuration: `${dur}s`,
    animationDelay: `${delay}s`
  }
}

// 登录
async function handleLogin() {
  tried.value = true
  if (!username.value) return showToast('请输入用户名', 'warning')
  if (!password.value) return showToast('请输入密码', 'warning')
  if (loading.value) return

  loading.value = true
  try {
    const data = await authApi.login(username.value, password.value)
    // 持久化登录态
    localStorage.setItem('hw_token', data.token)
    localStorage.setItem('hw_role', data.role || '')
    localStorage.setItem('hw_username', data.username || username.value)
    localStorage.setItem('hw_merchant', data.merchantName || '')
    showToast('登录成功，正在进入全息驾驶舱…', 'success')
    // 登录后直接进入全息大屏（大屏右上角可进入管理后台）
    router.push('/')
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '登录失败，请检查用户名或密码'
    showToast(msg, 'error')
    loading.value = false
  }
}

onBeforeUnmount(() => clearTimeout(toastTimer))
</script>

<style scoped>
.login-page {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background:
    radial-gradient(900px 500px at 20% 10%, rgba(0, 165, 255, 0.1), transparent 60%),
    radial-gradient(800px 480px at 80% 90%, rgba(123, 104, 238, 0.1), transparent 60%),
    var(--bg-dark);
}

/* ---------- 浮动光点 ---------- */
.login-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: var(--color-primary);
  box-shadow: 0 0 8px rgba(0, 229, 255, 0.8);
  opacity: 0;
  animation-name: particle-float;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
}

@keyframes particle-float {
  0%   { transform: translateY(0) scale(1); opacity: 0; }
  12%  { opacity: 0.7; }
  50%  { transform: translateY(-46px) scale(1.25); opacity: 0.45; }
  88%  { opacity: 0.25; }
  100% { transform: translateY(-92px) scale(0.7); opacity: 0; }
}

/* ---------- 返回大屏 ---------- */
.back-link {
  position: absolute;
  top: 22px;
  left: 22px;
  z-index: 10;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--text-secondary);
  text-decoration: none;
  border: 1px solid var(--border-cyan);
  border-radius: var(--panel-radius);
  background: rgba(6, 20, 48, 0.5);
  backdrop-filter: blur(6px);
  transition: color var(--dur-fast), box-shadow var(--dur-fast), background var(--dur-fast);
}

.back-link:hover {
  color: var(--color-primary);
  background: rgba(0, 229, 255, 0.1);
  box-shadow: var(--glow-soft);
}

/* ---------- 登录卡片（玻璃拟态 + 青色发光边） ---------- */
.login-card {
  position: relative;
  width: 400px;
  max-width: calc(100vw - 40px);
  padding: 44px 38px 32px;
  border-radius: 10px;
  border: 1px solid var(--border-cyan);
  background: linear-gradient(160deg, rgba(10, 30, 68, 0.55), rgba(4, 14, 34, 0.72));
  backdrop-filter: blur(16px);
  box-shadow:
    0 0 30px rgba(0, 229, 255, 0.12),
    0 0 80px rgba(0, 165, 255, 0.08),
    inset 0 1px 0 rgba(0, 229, 255, 0.12),
    0 24px 60px rgba(0, 0, 0, 0.5);
  animation: card-in 0.5s var(--ease-out);
}

@keyframes card-in {
  from { transform: translateY(26px) scale(0.97); opacity: 0; }
  to   { transform: translateY(0) scale(1); opacity: 1; }
}

/* 四角 HUD 括号 */
.hud {
  position: absolute;
  width: 18px;
  height: 18px;
  pointer-events: none;
}

.hud-tl { top: 6px; left: 6px; border-top: 2px solid var(--color-primary); border-left: 2px solid var(--color-primary); }
.hud-tr { top: 6px; right: 6px; border-top: 2px solid var(--color-primary); border-right: 2px solid var(--color-primary); }
.hud-bl { bottom: 6px; left: 6px; border-bottom: 2px solid var(--color-accent); border-left: 2px solid var(--color-accent); }
.hud-br { bottom: 6px; right: 6px; border-bottom: 2px solid var(--color-accent); border-right: 2px solid var(--color-accent); }

/* 头部 */
.login-head {
  text-align: center;
  margin-bottom: 28px;
}

.login-icon {
  font-size: 40px;
  margin-bottom: 10px;
  filter: drop-shadow(0 0 14px rgba(0, 229, 255, 0.65));
  animation: icon-breathe 3.2s ease-in-out infinite;
}

@keyframes icon-breathe {
  0%, 100% { transform: translateY(0); opacity: 0.85; }
  50% { transform: translateY(-5px); opacity: 1; }
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 4px;
  color: var(--text-primary);
  background: linear-gradient(180deg, #ffffff 30%, var(--color-primary));
  -webkit-background-clip: text;
          background-clip: text;
  -webkit-text-fill-color: transparent;
}

.login-sub {
  margin-top: 8px;
  font-family: var(--font-data);
  font-size: 11px;
  letter-spacing: 4px;
  color: var(--text-faint);
  text-transform: uppercase;
}

/* ---------- 表单 ---------- */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-field {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 46px;
  padding: 0 14px;
  border: 1px solid var(--border-cyan);
  border-radius: var(--panel-radius);
  background: rgba(2, 10, 26, 0.55);
  transition: border-color var(--dur-fast), box-shadow var(--dur-fast);
}

.login-field:focus-within {
  border-color: rgba(0, 229, 255, 0.7);
  box-shadow: 0 0 0 1px rgba(0, 229, 255, 0.22), 0 0 16px rgba(0, 229, 255, 0.22);
}

.login-field.field-error {
  border-color: rgba(255, 77, 106, 0.55);
}

.login-field .field-icon {
  font-size: 16px;
  opacity: 0.8;
}

.login-field input {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 14px;
  letter-spacing: 1px;
}

.login-field input::placeholder {
  color: var(--text-faint);
  letter-spacing: 2px;
}

/* 登录按钮：渐变青紫 + hover 扫光 */
.login-btn {
  position: relative;
  height: 46px;
  margin-top: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 15px;
  font-family: inherit;
  font-weight: 600;
  letter-spacing: 6px;
  text-indent: 6px;
  color: #fff;
  cursor: pointer;
  border: 1px solid rgba(0, 229, 255, 0.55);
  border-radius: var(--panel-radius);
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.35), rgba(123, 104, 238, 0.35));
  overflow: hidden;
  transition: background var(--dur-fast), box-shadow var(--dur-fast), opacity var(--dur-fast);
}

.login-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.55), rgba(123, 104, 238, 0.55));
  box-shadow: 0 0 22px rgba(0, 229, 255, 0.4);
}

.login-btn:active:not(:disabled) {
  transform: translateY(1px);
}

.login-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* ---------- 快捷提示 ---------- */
.login-hint {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 22px;
}

.hint-chip {
  padding: 4px 10px;
  font-size: 11px;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  border: 1px solid var(--border-faint);
  border-radius: 999px;
  background: rgba(2, 10, 26, 0.4);
}

/* ---------- 商家注册入口 ---------- */
.register-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed rgba(0, 229, 255, 0.18);
}

.reg-text {
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

.reg-link {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--color-primary);
  text-decoration: none;
  transition: text-shadow var(--dur-fast), transform var(--dur-fast);
}

.reg-link:hover {
  text-shadow: 0 0 12px rgba(0, 229, 255, 0.8);
}

.reg-note {
  font-size: 10px;
  color: var(--text-faint);
  letter-spacing: 0.5px;
}
</style>
