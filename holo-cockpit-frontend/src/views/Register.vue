<template>
  <div class="register-page">
    <!-- 浮动光点背景 -->
    <div class="login-particles">
      <span
        v-for="n in 16"
        :key="n"
        class="particle"
        :style="particleStyle(n)"
      ></span>
    </div>

    <!-- 返回登录 -->
    <router-link to="/login" class="back-link">← 返回登录</router-link>

    <!-- ===== 表单态 ===== -->
    <div v-if="!submitted" class="register-card">
      <span class="hud hud-tl"></span>
      <span class="hud hud-tr"></span>
      <span class="hud hud-bl"></span>
      <span class="hud hud-br"></span>

      <div class="reg-head">
        <div class="reg-icon">🏪</div>
        <h1 class="reg-title">商家入驻</h1>
        <p class="reg-sub">MERCHANT · REGISTER</p>
      </div>

      <!-- 三步入驻指引 -->
      <div class="steps">
        <div class="step step-active">
          <span class="step-no">1</span>
          <span class="step-label">填写资料</span>
        </div>
        <span class="step-line"></span>
        <div class="step">
          <span class="step-no">2</span>
          <span class="step-label">管理员审核</span>
        </div>
        <span class="step-line"></span>
        <div class="step">
          <span class="step-no">3</span>
          <span class="step-label">入驻成功</span>
        </div>
      </div>

      <form class="reg-form" @submit.prevent="handleRegister">
        <div class="reg-field" :class="{ 'field-error': tried && !usernameValid, 'field-ok': usernameValid }">
          <span class="field-icon">👤</span>
          <input
            v-model.trim="username"
            type="text"
            placeholder="用户名（3-32 位，中英文/数字/下划线）"
            autocomplete="username"
            :disabled="loading"
            maxlength="32"
          />
          <span v-if="usernameValid" class="field-check">✓</span>
        </div>

        <div class="reg-field" :class="{ 'field-error': tried && !merchantValid, 'field-ok': merchantValid }">
          <span class="field-icon">🏬</span>
          <input
            v-model.trim="merchantName"
            type="text"
            placeholder="商户名称（如：华为西安旗舰店）"
            :disabled="loading"
            maxlength="50"
          />
          <span class="field-count" :class="{ 'count-warn': merchantName.length > 42 }">{{ merchantName.length }}/50</span>
        </div>

        <div class="reg-field" :class="{ 'field-error': tried && !pwdValid, 'field-ok': pwdValid }">
          <span class="field-icon">🔒</span>
          <input
            v-model="password"
            type="password"
            placeholder="密码（至少 6 位）"
            autocomplete="new-password"
            :disabled="loading"
            maxlength="64"
          />
        </div>

        <!-- 密码强度条 -->
        <div v-if="password" class="pwd-strength">
          <div class="strength-bars">
            <span
              v-for="i in 3"
              :key="i"
              class="bar"
              :class="[strengthLevel >= i ? `bar-lv${strengthLevel}` : '']"
            ></span>
          </div>
          <span class="strength-text" :class="`st-lv${strengthLevel}`">{{ strengthText }}</span>
        </div>

        <div class="reg-field" :class="{ 'field-error': tried && !confirmValid, 'field-ok': confirmValid }">
          <span class="field-icon">🔐</span>
          <input
            v-model="confirmPassword"
            type="password"
            placeholder="确认密码"
            autocomplete="new-password"
            :disabled="loading"
            maxlength="64"
          />
          <span v-if="confirmValid" class="field-check">✓</span>
        </div>
        <div v-if="confirmPassword && !confirmValid" class="confirm-mismatch">两次输入的密码不一致</div>

        <!-- 审批制说明（群邀请类比） -->
        <div class="audit-note">
          <span class="note-icon">🛡️</span>
          <div class="note-text">
            本平台采用<b>审批制入驻</b>：提交申请后需<b>管理员审核</b>，通过后方可登录系统
            <span class="note-sub">（类似 QQ / 微信群邀请，需群主或管理员同意才能进群）</span>
          </div>
        </div>

        <button type="submit" class="reg-btn btn-sweep" :disabled="loading">
          <span v-if="loading" class="btn-spinner"></span>
          <span>{{ loading ? '提交中…' : '提交入驻申请' }}</span>
        </button>
      </form>
    </div>

    <!-- ===== 已提交态：等待审核 ===== -->
    <div v-else class="register-card card-success">
      <span class="hud hud-tl"></span>
      <span class="hud hud-tr"></span>
      <span class="hud hud-bl"></span>
      <span class="hud hud-br"></span>

      <!-- 雷达扫描动画 -->
      <div class="radar">
        <div class="radar-ring"></div>
        <div class="radar-ring radar-ring2"></div>
        <div class="radar-sweep"></div>
        <div class="radar-core">⏳</div>
      </div>

      <h1 class="success-title">申请已提交</h1>
      <p class="success-sub">正在等待管理员审核，请耐心等候</p>

      <!-- 申请摘要 -->
      <div class="summary">
        <div class="summary-row">
          <span class="summary-label">用户名</span>
          <span class="summary-value">{{ submitted.username }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">商户名称</span>
          <span class="summary-value">{{ submitted.merchantName }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">当前状态</span>
          <span class="summary-value status-pending">🟡 待审核</span>
        </div>
      </div>

      <div class="success-note">
        审核通过后即可使用该账号登录系统；<br />
        若被拒绝，可根据拒绝理由修改资料后<b>重新提交</b>。
      </div>

      <button class="reg-btn" @click="goLogin">
        返回登录（{{ countdown }}s）
      </button>
    </div>

    <!-- 错误/警告 toast -->
    <transition name="toast">
      <div v-if="toast" class="toast" :class="`toast-${toast.type}`">{{ toast.msg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import '@/styles/admin.css'

const router = useRouter()

/* ===== 表单状态 ===== */
const username = ref('')
const merchantName = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const tried = ref(false)
const submitted = ref(null) // 提交成功后的摘要

/* ===== 校验 ===== */
const usernameValid = computed(() =>
  /^[A-Za-z0-9_\-\u4e00-\u9fa5]{3,32}$/.test(username.value)
)
const merchantValid = computed(() =>
  merchantName.value.length >= 2 && merchantName.value.length <= 50
)
const pwdValid = computed(() => password.value.length >= 6 && password.value.length <= 64)
const confirmValid = computed(() =>
  confirmPassword.value.length > 0 && confirmPassword.value === password.value
)

/* ===== 密码强度（1 弱 / 2 中 / 3 强） ===== */
const strengthLevel = computed(() => {
  const p = password.value
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score++
  if (/[A-Za-z]/.test(p)) score++
  if (/\d/.test(p)) score++
  if (/[^A-Za-z0-9]/.test(p)) score++
  if (p.length < 6) return 1
  if (score <= 2) return 1
  if (score === 3) return 2
  return 3
})

const strengthText = computed(() =>
  ['', '强度：弱', '强度：中', '强度：强'][strengthLevel.value]
)

/* ===== toast ===== */
const toast = ref(null)
let toastTimer = null

function showToast(msg, type = 'error') {
  toast.value = { msg, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = null), 3200)
}

/* ===== 浮动光点（与登录页一致） ===== */
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

/* ===== 提交注册 ===== */
async function handleRegister() {
  tried.value = true
  if (!usernameValid.value) return showToast('请输入 3-32 位合法用户名', 'warning')
  if (!merchantValid.value) return showToast('请填写 2-50 字的商户名称', 'warning')
  if (!pwdValid.value) return showToast('密码需为 6-64 个字符', 'warning')
  if (!confirmValid.value) return showToast('两次输入的密码不一致', 'warning')
  if (loading.value) return

  loading.value = true
  try {
    await authApi.register(username.value, password.value, merchantName.value)
    submitted.value = { username: username.value, merchantName: merchantName.value }
    startCountdown()
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '注册失败，请稍后重试'
    showToast(msg, 'error')
    loading.value = false
  }
}

/* ===== 提交成功后倒计时返回登录 ===== */
const countdown = ref(8)
let countdownTimer = null

function startCountdown() {
  countdown.value = 8
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) goLogin()
  }, 1000)
}

function goLogin() {
  if (countdownTimer) clearInterval(countdownTimer)
  router.push('/login')
}

/* 密码变更时清空确认密码（避免残留错位校验） */
watch(password, (v) => {
  if (confirmPassword.value && confirmPassword.value !== v) confirmPassword.value = ''
})

onBeforeUnmount(() => {
  clearTimeout(toastTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.register-page {
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

/* ---------- 背景光点（与登录页同款） ---------- */
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

/* ---------- 返回登录 ---------- */
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

/* ---------- 注册卡片 ---------- */
.register-card {
  position: relative;
  width: 430px;
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  padding: 36px 38px 30px;
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

/* ---------- 头部 ---------- */
.reg-head {
  text-align: center;
  margin-bottom: 18px;
}

.reg-icon {
  font-size: 38px;
  margin-bottom: 8px;
  filter: drop-shadow(0 0 14px rgba(0, 229, 255, 0.65));
  animation: icon-breathe 3.2s ease-in-out infinite;
}

@keyframes icon-breathe {
  0%, 100% { transform: translateY(0); opacity: 0.85; }
  50%      { transform: translateY(-5px); opacity: 1; }
}

.reg-title {
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 5px;
  color: var(--text-primary);
  background: linear-gradient(180deg, #ffffff 30%, var(--color-primary));
  -webkit-background-clip: text;
          background-clip: text;
  -webkit-text-fill-color: transparent;
}

.reg-sub {
  margin-top: 7px;
  font-family: var(--font-data);
  font-size: 10px;
  letter-spacing: 4px;
  color: var(--text-faint);
  text-transform: uppercase;
}

/* ---------- 三步入驻指引 ---------- */
.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 22px;
}

.step {
  display: flex;
  align-items: center;
  gap: 5px;
}

.step-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 17px;
  height: 17px;
  font-size: 10px;
  font-family: var(--font-data);
  color: var(--text-faint);
  border: 1px solid var(--border-faint);
  border-radius: 50%;
}

.step-label {
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--text-faint);
}

.step-active .step-no {
  color: #041222;
  background: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: 0 0 10px rgba(0, 229, 255, 0.6);
  animation: step-pulse 2s ease-in-out infinite;
}

.step-active .step-label {
  color: var(--color-primary);
  font-weight: 600;
}

@keyframes step-pulse {
  0%, 100% { box-shadow: 0 0 6px rgba(0, 229, 255, 0.4); }
  50%      { box-shadow: 0 0 14px rgba(0, 229, 255, 0.85); }
}

.step-line {
  width: 22px;
  height: 1px;
  background: linear-gradient(90deg, rgba(0, 229, 255, 0.45), rgba(122, 151, 197, 0.2));
}

/* ---------- 表单 ---------- */
.reg-form {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.reg-field {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  padding: 0 14px;
  border: 1px solid var(--border-cyan);
  border-radius: var(--panel-radius);
  background: rgba(2, 10, 26, 0.55);
  transition: border-color var(--dur-fast), box-shadow var(--dur-fast);
}

.reg-field:focus-within {
  border-color: rgba(0, 229, 255, 0.7);
  box-shadow: 0 0 0 1px rgba(0, 229, 255, 0.22), 0 0 16px rgba(0, 229, 255, 0.22);
}

.reg-field.field-error {
  border-color: rgba(255, 77, 106, 0.55);
}

.reg-field.field-ok {
  border-color: rgba(0, 255, 163, 0.45);
}

.reg-field .field-icon {
  font-size: 15px;
  opacity: 0.8;
}

.reg-field input {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 13px;
  letter-spacing: 1px;
}

.reg-field input::placeholder {
  color: var(--text-faint);
  font-size: 12px;
  letter-spacing: 1.5px;
}

.field-check {
  color: #00ffa3;
  font-size: 14px;
  text-shadow: 0 0 8px rgba(0, 255, 163, 0.7);
}

.field-count {
  font-family: var(--font-data);
  font-size: 10px;
  color: var(--text-faint);
}

.count-warn {
  color: #ffcc00;
}

.confirm-mismatch {
  margin-top: -6px;
  padding-left: 4px;
  font-size: 11px;
  color: #ff4d6a;
  letter-spacing: 0.5px;
}

/* ---------- 密码强度条 ---------- */
.pwd-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: -5px;
  padding: 0 4px;
}

.strength-bars {
  display: flex;
  gap: 4px;
  flex: 1;
}

.strength-bars .bar {
  flex: 1;
  height: 3px;
  border-radius: 2px;
  background: rgba(122, 151, 197, 0.18);
  transition: background var(--dur-fast), box-shadow var(--dur-fast);
}

.strength-bars .bar.bar-lv1 { background: #ff4d6a; box-shadow: 0 0 6px rgba(255, 77, 106, 0.6); }
.strength-bars .bar.bar-lv2 { background: #ffcc00; box-shadow: 0 0 6px rgba(255, 204, 0, 0.6); }
.strength-bars .bar.bar-lv3 { background: #00ffa3; box-shadow: 0 0 6px rgba(0, 255, 163, 0.6); }

.strength-text {
  font-size: 10px;
  letter-spacing: 1px;
  white-space: nowrap;
}

.st-lv1 { color: #ff4d6a; }
.st-lv2 { color: #ffcc00; }
.st-lv3 { color: #00ffa3; }

/* ---------- 审批制说明 ---------- */
.audit-note {
  display: flex;
  gap: 10px;
  padding: 11px 13px;
  border: 1px dashed rgba(0, 229, 255, 0.3);
  border-radius: var(--panel-radius);
  background: rgba(0, 165, 255, 0.06);
}

.note-icon {
  font-size: 16px;
  line-height: 1.4;
}

.note-text {
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--text-muted);
  letter-spacing: 0.3px;
}

.note-text b {
  color: var(--color-primary);
}

.note-sub {
  color: var(--text-faint);
}

/* ---------- 提交按钮 ---------- */
.reg-btn {
  position: relative;
  height: 44px;
  margin-top: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  font-family: inherit;
  font-weight: 600;
  letter-spacing: 5px;
  text-indent: 5px;
  color: #fff;
  cursor: pointer;
  border: 1px solid rgba(0, 229, 255, 0.55);
  border-radius: var(--panel-radius);
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.35), rgba(123, 104, 238, 0.35));
  overflow: hidden;
  transition: background var(--dur-fast), box-shadow var(--dur-fast), opacity var(--dur-fast);
}

.reg-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.55), rgba(123, 104, 238, 0.55));
  box-shadow: 0 0 22px rgba(0, 229, 255, 0.4);
}

.reg-btn:active:not(:disabled) {
  transform: translateY(1px);
}

.reg-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-spinner {
  width: 15px;
  height: 15px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ---------- 成功态（等待审核） ---------- */
.card-success {
  text-align: center;
}

.radar {
  position: relative;
  width: 118px;
  height: 118px;
  margin: 4px auto 18px;
  border-radius: 50%;
  border: 1px solid rgba(0, 229, 255, 0.35);
  background:
    radial-gradient(circle, rgba(0, 229, 255, 0.06) 0%, transparent 70%);
  overflow: hidden;
}

.radar-ring {
  position: absolute;
  inset: 18px;
  border-radius: 50%;
  border: 1px dashed rgba(0, 229, 255, 0.28);
}

.radar-ring2 {
  inset: 38px;
}

.radar-sweep {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: conic-gradient(from 0deg, rgba(0, 229, 255, 0.5), transparent 25%);
  animation: radar-spin 2.4s linear infinite;
}

@keyframes radar-spin {
  to { transform: rotate(360deg); }
}

.radar-core {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  filter: drop-shadow(0 0 10px rgba(0, 229, 255, 0.7));
  animation: icon-breathe 2s ease-in-out infinite;
}

.success-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 4px;
  color: var(--text-primary);
}

.success-sub {
  margin-top: 8px;
  font-size: 12.5px;
  letter-spacing: 1px;
  color: var(--text-muted);
}

.summary {
  margin: 20px 0 16px;
  padding: 4px 16px;
  border: 1px solid var(--border-faint);
  border-radius: var(--panel-radius);
  background: rgba(2, 10, 26, 0.45);
  text-align: left;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 0;
  border-bottom: 1px dashed rgba(122, 151, 197, 0.15);
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-label {
  font-size: 11.5px;
  letter-spacing: 1px;
  color: var(--text-faint);
}

.summary-value {
  font-size: 12.5px;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.status-pending {
  color: #ffcc00;
  text-shadow: 0 0 8px rgba(255, 204, 0, 0.5);
}

.success-note {
  margin-bottom: 18px;
  font-size: 11.5px;
  line-height: 1.9;
  color: var(--text-faint);
  letter-spacing: 0.4px;
}

.success-note b {
  color: var(--color-primary);
}
</style>
