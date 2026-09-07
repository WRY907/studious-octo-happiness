<template>
  <div class="audit-page">
    <!-- 非管理员防护提示（路由守卫已拦截，双保险） -->
    <div v-if="!isAdmin" class="notice-bar notice-danger">
      <span>⛔</span>
      <span>无操作权限：商户审批仅管理员可见</span>
    </div>

    <template v-else>
      <!-- ===== 统计卡 ===== -->
      <div class="stat-row">
        <div class="stat-card stat-pending" :class="{ 'stat-alert': stats.pendingCount > 0 }">
          <div class="stat-icon">⏳</div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.pendingCount }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </div>
        <div class="stat-card stat-approved">
          <div class="stat-icon">✅</div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.approvedCount }}</div>
            <div class="stat-label">已通过</div>
          </div>
        </div>
        <div class="stat-card stat-rejected">
          <div class="stat-icon">❌</div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.rejectedCount }}</div>
            <div class="stat-label">已拒绝</div>
          </div>
        </div>
        <div class="stat-card stat-disabled">
          <div class="stat-icon">🚫</div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.disabledCount }}</div>
            <div class="stat-label">已停用</div>
          </div>
        </div>
        <div class="stat-card stat-rate">
          <div class="stat-icon">📊</div>
          <div class="stat-body">
            <div class="stat-value">{{ approvalRate }}%</div>
            <div class="stat-label">审批通过率</div>
          </div>
        </div>
      </div>

      <!-- ===== 状态流转示意 ===== -->
      <div class="admin-card flow-card">
        <div class="flow-line">
          <span class="flow-node n-pending">提交申请<br /><em>PENDING</em></span>
          <span class="flow-arrow">→</span>
          <span class="flow-node n-review">管理员审核<br /><em>REVIEW</em></span>
          <span class="flow-arrow">→</span>
          <span class="flow-node n-approved">通过·可登录<br /><em>APPROVED</em></span>
        </div>
        <div class="flow-sub">
          <div class="flow-branch">
            <span class="flow-node n-rejected">拒绝（附理由）<br /><em>REJECTED</em></span>
            <span class="flow-back">商家可修改资料 → 重新申请</span>
          </div>
          <div class="flow-branch">
            <span class="flow-node n-disabled">停用·禁止登录<br /><em>DISABLED</em></span>
            <span class="flow-back">管理员可随时重新启用</span>
          </div>
        </div>
      </div>

      <!-- ===== 列表 ===== -->
      <div class="admin-card">
        <div class="toolbar">
          <div class="tab-group">
            <button class="tab-btn" :class="{ active: tab === 'pending' }" @click="switchTab('pending')">
              ⏳ 待审核
              <span v-if="stats.pendingCount > 0" class="tab-badge">{{ stats.pendingCount }}</span>
            </button>
            <button class="tab-btn" :class="{ active: tab === 'handled' }" @click="switchTab('handled')">
              📋 已处理
            </button>
          </div>
          <div class="toolbar-spacer"></div>
          <button v-if="tab === 'pending' && records.length > 1" class="btn btn-outline" :disabled="loading" @click="confirmApproveAll">
            ⚡ 一键通过全部
          </button>
          <button class="btn btn-outline" @click="loadAll">🔄 刷新</button>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="loading-state">
          <div class="loading-dots"></div>
          <div>审批数据加载中…</div>
        </div>

        <!-- 加载失败 -->
        <div v-else-if="loadFailed" class="empty-state">
          <div class="empty-icon">⚠️</div>
          <div class="empty-text">审批数据加载失败</div>
          <div class="empty-sub">{{ loadError }}，请稍后重试</div>
          <button class="btn btn-sm" style="margin-top: 14px" @click="loadAll">重新加载</button>
        </div>

        <!-- 空数据 -->
        <div v-else-if="!records.length" class="empty-state">
          <div class="empty-icon">{{ tab === 'pending' ? '🎉' : '📭' }}</div>
          <div class="empty-text">{{ tab === 'pending' ? '暂无待审核的入驻申请' : '暂无已处理的审批记录' }}</div>
          <div v-if="tab === 'pending'" class="empty-sub">商家可通过登录页「商家注册」入口提交入驻申请</div>
        </div>

        <!-- 待审核表 -->
        <template v-else-if="tab === 'pending'">
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户名</th>
                  <th>商户名称</th>
                  <th>申请时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in records" :key="row.id" class="row-pending">
                  <td class="num">{{ row.id }}</td>
                  <td>{{ row.username }}</td>
                  <td>
                    <span class="merchant-chip">🏪 {{ row.merchantName || '—' }}</span>
                  </td>
                  <td>
                    <div class="time-cell">
                      <div>{{ fmtTime(row.createTime) }}</div>
                      <div class="time-rel">{{ relTime(row.createTime) }}</div>
                    </div>
                  </td>
                  <td>
                    <div class="op-group">
                      <button class="btn btn-sm btn-primary" :disabled="acting" @click="approve(row)">✅ 通过</button>
                      <button class="btn btn-sm btn-danger" :disabled="acting" @click="openReject(row)">❌ 拒绝</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>

        <!-- 已处理表 -->
        <template v-else>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户名</th>
                  <th>商户名称</th>
                  <th>状态</th>
                  <th>拒绝理由</th>
                  <th>审批时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in records" :key="row.id">
                  <td class="num">{{ row.id }}</td>
                  <td>{{ row.username }}</td>
                  <td>
                    <span class="merchant-chip">🏪 {{ row.merchantName || '—' }}</span>
                  </td>
                  <td>
                    <span class="tag" :class="statusTagClass(row.status)">{{ statusText(row.status) }}</span>
                  </td>
                  <td class="reason-cell" :title="row.rejectReason || ''">
                    {{ row.rejectReason || '—' }}
                  </td>
                  <td class="num">{{ fmtTime(row.auditTime) }}</td>
                  <td>
                    <div class="op-group">
                      <button
                        v-if="row.status === 'APPROVED'"
                        class="btn btn-sm btn-danger"
                        :disabled="acting"
                        @click="disable(row)"
                      >🚫 停用</button>
                      <button
                        v-if="row.status === 'DISABLED'"
                        class="btn btn-sm btn-primary"
                        :disabled="acting"
                        @click="enable(row)"
                      >🔄 启用</button>
                      <span v-if="row.status === 'REJECTED'" class="tag tag-muted">等待商家重新申请</span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>

        <!-- 分页 -->
        <div v-if="records.length || page > 1" class="pagination">
          <span class="page-info">共 <span class="num">{{ total }}</span> 条 · 第 {{ page }} / {{ totalPages }} 页</span>
          <button class="page-btn" :disabled="page <= 1 || loading" @click="changePage(page - 1)">‹ 上一页</button>
          <button
            v-for="p in pageList"
            :key="p"
            class="page-btn"
            :class="{ active: p === page }"
            :disabled="loading"
            @click="changePage(p)"
          >{{ p }}</button>
          <button class="page-btn" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页 ›</button>
        </div>
      </div>
    </template>

    <!-- ===== 拒绝理由弹窗 ===== -->
    <transition name="fade">
      <div v-if="rejectModal.show" class="modal-mask" @click.self="closeReject">
        <div class="modal-card">
          <span class="hud hud-tl"></span>
          <span class="hud hud-tr"></span>
          <span class="hud hud-bl"></span>
          <span class="hud hud-br"></span>
          <div class="modal-title">拒绝入驻申请</div>
          <div class="modal-target">
            🏪 {{ rejectModal.row?.merchantName }} · @{{ rejectModal.row?.username }}
          </div>
          <div class="modal-label">选择或填写拒绝理由（商家登录时可见）：</div>
          <div class="quick-reasons">
            <button
              v-for="r in quickReasons"
              :key="r"
              class="reason-chip"
              :class="{ active: rejectModal.reason === r }"
              @click="rejectModal.reason = rejectModal.reason === r ? '' : r"
            >{{ r }}</button>
          </div>
          <textarea
            v-model="rejectModal.reason"
            class="reason-input"
            rows="3"
            maxlength="200"
            placeholder="拒绝理由（200 字以内，可选）"
          ></textarea>
          <div class="modal-actions">
            <button class="btn btn-sm" @click="closeReject">取消</button>
            <button class="btn btn-sm btn-danger" :disabled="acting || !rejectModal.reason.trim()" @click="confirmReject">
              {{ acting ? '提交中…' : '确认拒绝' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- toast -->
    <transition name="toast">
      <div v-if="toast" class="toast" :class="`toast-${toast.type}`">{{ toast.msg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api'
import { audioManager } from '@/audio/manager'

/* ===== 角色防护 ===== */
const isAdmin = computed(() => localStorage.getItem('hw_role') === 'ADMIN')

/* ===== 统计 ===== */
const stats = ref({ pendingCount: 0, approvedCount: 0, rejectedCount: 0, disabledCount: 0 })

const approvalRate = computed(() => {
  const approved = Number(stats.value.approvedCount || 0)
  const rejected = Number(stats.value.rejectedCount || 0)
  const total = approved + rejected
  return total ? ((approved / total) * 100).toFixed(1) : '—'
})

/* ===== 列表状态 ===== */
const tab = ref('pending') // pending / handled
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const loadFailed = ref(false)
const loadError = ref('')
const acting = ref(false) // 操作进行中（防连点）

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

const pageList = computed(() => {
  const tp = totalPages.value
  const start = Math.max(1, Math.min(page.value - 2, tp - 4))
  const end = Math.min(tp, start + 4)
  const list = []
  for (let i = start; i <= end; i++) list.push(i)
  return list
})

/* ===== 拒绝弹窗 ===== */
const quickReasons = ['资料不完整', '商户名称不合规', '重复申请', '暂不开放入驻']
const rejectModal = ref({ show: false, row: null, reason: '' })

/* ===== toast ===== */
const toast = ref(null)
let toastTimer = null

function showToast(msg, type = 'error') {
  toast.value = { msg, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = null), 3000)
}

/* ===== 数据加载 ===== */
async function loadStats() {
  try {
    stats.value = await adminApi.getAuditStats() || stats.value
    // 即时同步侧边栏待审角标（AdminLayout 监听，避免 30s 轮询延迟）
    window.dispatchEvent(new CustomEvent('hw-audit-changed', {
      detail: Number(stats.value.pendingCount || 0)
    }))
  } catch (e) { /* 静默 */ }
}

async function loadList() {
  loading.value = true
  loadFailed.value = false
  try {
    const data = await adminApi.getAuditList({ tab: tab.value, page: page.value, size })
    records.value = data?.records || []
    total.value = data?.total || 0
    if (page.value > totalPages.value && totalPages.value > 0) {
      page.value = totalPages.value
      return loadList()
    }
  } catch (e) {
    loadFailed.value = true
    loadError.value = e?.response?.status === 403
      ? '无操作权限'
      : (e?.response?.data?.message || e?.message || '服务异常')
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function loadAll() {
  loadStats()
  loadList()
}

function switchTab(t) {
  if (tab.value === t) return
  tab.value = t
  page.value = 1
  loadList()
}

function changePage(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return
  page.value = p
  loadList()
}

/* ===== 审批操作 ===== */
async function approve(row) {
  if (acting.value) return
  acting.value = true
  try {
    const msg = await adminApi.approveAudit(row.id)
    audioManager.success() // 审批通过音
    showToast(msg || '已通过', 'success')
    loadAll()
  } catch (e) {
    showToast(e?.response?.data?.message || e?.message || '操作失败', 'error')
  } finally {
    acting.value = false
  }
}

async function confirmApproveAll() {
  if (acting.value) return
  if (!window.confirm(`确定一键通过全部 ${stats.value.pendingCount} 条待审申请吗？`)) return
  acting.value = true
  try {
    const msg = await adminApi.approveAllAudit()
    audioManager.success() // 批量通过音
    showToast(msg || '已一键通过', 'success')
    loadAll()
  } catch (e) {
    showToast(e?.response?.data?.message || e?.message || '操作失败', 'error')
  } finally {
    acting.value = false
  }
}

function openReject(row) {
  rejectModal.value = { show: true, row, reason: '' }
}

function closeReject() {
  rejectModal.value.show = false
}

async function confirmReject() {
  const { row, reason } = rejectModal.value
  if (!reason.trim() || acting.value) return
  acting.value = true
  try {
    const msg = await adminApi.rejectAudit(row.id, reason.trim())
    audioManager.error() // 审批拒绝音
    showToast(msg || '已拒绝', 'success')
    closeReject()
    loadAll()
  } catch (e) {
    showToast(e?.response?.data?.message || e?.message || '操作失败', 'error')
  } finally {
    acting.value = false
  }
}

async function disable(row) {
  if (acting.value) return
  if (!window.confirm(`确定停用商家「${row.merchantName}」吗？停用后该账号将无法登录。`)) return
  acting.value = true
  try {
    const msg = await adminApi.disableMerchant(row.id)
    audioManager.error() // 停用音
    showToast(msg || '已停用', 'success')
    loadAll()
  } catch (e) {
    showToast(e?.response?.data?.message || e?.message || '操作失败', 'error')
  } finally {
    acting.value = false
  }
}

async function enable(row) {
  if (acting.value) return
  acting.value = true
  try {
    const msg = await adminApi.enableMerchant(row.id)
    audioManager.success() // 启用音
    showToast(msg || '已启用', 'success')
    loadAll()
  } catch (e) {
    showToast(e?.response?.data?.message || e?.message || '操作失败', 'error')
  } finally {
    acting.value = false
  }
}

/* ===== 展示辅助 ===== */
function fmtTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

function relTime(t) {
  if (!t) return ''
  const s = String(t).replace('T', ' ').slice(0, 19)
  const d = new Date(s.replace(/-/g, '/'))
  if (isNaN(d.getTime())) return ''
  const diff = (Date.now() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  return `${Math.floor(diff / 86400)} 天前`
}

function statusText(s) {
  return { APPROVED: '✅ 已通过', REJECTED: '❌ 已拒绝', DISABLED: '🚫 已停用', PENDING: '⏳ 待审核' }[s] || s
}

function statusTagClass(s) {
  return { APPROVED: 'tag-success', REJECTED: 'tag-danger', DISABLED: 'tag-muted', PENDING: 'tag-warn' }[s] || 'tag-muted'
}

onMounted(loadAll)
</script>

<style scoped>
.audit-page {
  width: 100%;
}

/* ===== 统计卡 ===== */
.stat-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 10px;
  border: 1px solid var(--border-faint);
  background: linear-gradient(160deg, rgba(10, 30, 68, 0.45), rgba(4, 14, 34, 0.65));
  transition: transform var(--dur-fast), box-shadow var(--dur-fast);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
}

.stat-icon {
  font-size: 26px;
  filter: drop-shadow(0 0 8px rgba(0, 229, 255, 0.4));
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  font-family: var(--font-data);
  color: var(--text-primary);
  line-height: 1.15;
}

.stat-label {
  margin-top: 3px;
  font-size: 11.5px;
  letter-spacing: 1.5px;
  color: var(--text-faint);
}

.stat-pending .stat-value { color: #ffcc00; }
.stat-pending .stat-icon { filter: drop-shadow(0 0 8px rgba(255, 204, 0, 0.5)); }

.stat-alert {
  border-color: rgba(255, 204, 0, 0.45);
  animation: stat-pulse 1.8s ease-in-out infinite;
}

@keyframes stat-pulse {
  0%, 100% { box-shadow: 0 0 10px rgba(255, 204, 0, 0.12); }
  50%      { box-shadow: 0 0 22px rgba(255, 204, 0, 0.3); }
}

.stat-approved .stat-value { color: #00ffa3; }
.stat-rejected .stat-value { color: #ff4d6a; }
.stat-disabled .stat-value { color: #8aa4c8; }
.stat-rate .stat-value { color: var(--color-primary); }

/* ===== 状态流转图 ===== */
.flow-card {
  margin-bottom: 16px;
  padding: 16px 22px;
}

.flow-line {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.flow-node {
  padding: 8px 16px;
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
  letter-spacing: 1px;
  color: var(--text-secondary);
  border: 1px solid var(--border-faint);
  border-radius: 8px;
  background: rgba(2, 10, 26, 0.4);
}

.flow-node em {
  font-style: normal;
  font-family: var(--font-data);
  font-size: 9.5px;
  letter-spacing: 1.5px;
  color: var(--text-faint);
}

.flow-arrow {
  color: var(--color-primary);
  font-size: 15px;
  text-shadow: 0 0 8px rgba(0, 229, 255, 0.6);
}

.n-pending { border-color: rgba(255, 204, 0, 0.4); color: #ffcc00; }
.n-review  { border-color: rgba(0, 229, 255, 0.4); color: var(--color-primary); }
.n-approved { border-color: rgba(0, 255, 163, 0.4); color: #00ffa3; }
.n-rejected { border-color: rgba(255, 77, 106, 0.4); color: #ff4d6a; }
.n-disabled { border-color: rgba(138, 164, 200, 0.4); color: #8aa4c8; }

.flow-sub {
  display: flex;
  justify-content: center;
  gap: 40px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed rgba(122, 151, 197, 0.18);
}

.flow-branch {
  display: flex;
  align-items: center;
  gap: 10px;
}

.flow-back {
  font-size: 11px;
  color: var(--text-faint);
  letter-spacing: 0.5px;
}

/* ===== 工具栏 / Tab ===== */
.tab-group {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--border-faint);
  border-radius: 8px;
  background: rgba(2, 10, 26, 0.4);
}

.tab-btn {
  position: relative;
  padding: 7px 18px;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--text-muted);
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  transition: all var(--dur-fast);
}

.tab-btn.active {
  color: #041222;
  font-weight: 600;
  background: var(--color-primary);
  box-shadow: 0 0 12px rgba(0, 229, 255, 0.45);
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 17px;
  height: 17px;
  margin-left: 6px;
  padding: 0 5px;
  font-size: 10px;
  font-weight: 700;
  font-family: var(--font-data);
  color: #fff;
  background: #ff4d6a;
  border-radius: 999px;
}

/* ===== 表格 ===== */
.merchant-chip {
  padding: 3px 10px;
  font-size: 12px;
  letter-spacing: 0.5px;
  color: #c9e1ff;
  border: 1px solid rgba(0, 229, 255, 0.25);
  border-radius: 999px;
  background: rgba(0, 165, 255, 0.08);
}

.row-pending td {
  background: rgba(255, 204, 0, 0.025);
}

.time-cell .time-rel {
  margin-top: 2px;
  font-size: 10.5px;
  color: var(--text-faint);
}

.op-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.reason-cell {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #ff9db0;
}

.tag-success {
  color: #00ffa3;
  border: 1px solid rgba(0, 255, 163, 0.35);
  background: rgba(0, 255, 163, 0.07);
}

.tag-danger {
  color: #ff4d6a;
  border: 1px solid rgba(255, 77, 106, 0.35);
  background: rgba(255, 77, 106, 0.07);
}

.tag-warn {
  color: #ffcc00;
  border: 1px solid rgba(255, 204, 0, 0.35);
  background: rgba(255, 204, 0, 0.07);
}

.tag-muted {
  color: var(--text-faint);
  border: 1px solid var(--border-faint);
  background: transparent;
}

/* ===== 拒绝弹窗 ===== */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(2, 8, 22, 0.72);
  backdrop-filter: blur(4px);
}

.modal-card {
  position: relative;
  width: 430px;
  max-width: calc(100vw - 40px);
  padding: 26px 28px 22px;
  border-radius: 10px;
  border: 1px solid var(--border-cyan);
  background: linear-gradient(160deg, rgba(10, 30, 68, 0.92), rgba(4, 14, 34, 0.96));
  box-shadow: 0 0 40px rgba(0, 229, 255, 0.15), 0 24px 60px rgba(0, 0, 0, 0.6);
}

.hud {
  position: absolute;
  width: 16px;
  height: 16px;
  pointer-events: none;
}

.hud-tl { top: 5px; left: 5px; border-top: 2px solid var(--color-primary); border-left: 2px solid var(--color-primary); }
.hud-tr { top: 5px; right: 5px; border-top: 2px solid var(--color-primary); border-right: 2px solid var(--color-primary); }
.hud-bl { bottom: 5px; left: 5px; border-bottom: 2px solid var(--color-accent); border-left: 2px solid var(--color-accent); }
.hud-br { bottom: 5px; right: 5px; border-bottom: 2px solid var(--color-accent); border-right: 2px solid var(--color-accent); }

.modal-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--text-primary);
}

.modal-target {
  margin-top: 8px;
  font-size: 13px;
  letter-spacing: 0.5px;
  color: var(--color-primary);
}

.modal-label {
  margin: 16px 0 10px;
  font-size: 12px;
  letter-spacing: 0.5px;
  color: var(--text-muted);
}

.quick-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.reason-chip {
  padding: 6px 13px;
  font-size: 12px;
  letter-spacing: 0.5px;
  color: var(--text-secondary);
  border: 1px solid var(--border-faint);
  border-radius: 999px;
  background: rgba(2, 10, 26, 0.4);
  cursor: pointer;
  transition: all var(--dur-fast);
}

.reason-chip:hover {
  color: var(--color-primary);
  border-color: rgba(0, 229, 255, 0.45);
}

.reason-chip.active {
  color: #ff8fa3;
  border-color: rgba(255, 77, 106, 0.55);
  background: rgba(255, 77, 106, 0.1);
  box-shadow: 0 0 10px rgba(255, 77, 106, 0.25);
}

.reason-input {
  width: 100%;
  padding: 10px 13px;
  font-family: inherit;
  font-size: 12.5px;
  letter-spacing: 0.5px;
  color: var(--text-primary);
  border: 1px solid var(--border-cyan);
  border-radius: 8px;
  background: rgba(2, 10, 26, 0.55);
  outline: none;
  resize: none;
  transition: border-color var(--dur-fast), box-shadow var(--dur-fast);
}

.reason-input:focus {
  border-color: rgba(255, 77, 106, 0.5);
  box-shadow: 0 0 14px rgba(255, 77, 106, 0.18);
}

.reason-input::placeholder {
  color: var(--text-faint);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

/* ===== 响应式 ===== */
@media (max-width: 1100px) {
  .stat-row { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 760px) {
  .stat-row { grid-template-columns: repeat(2, 1fr); }
  .flow-sub { gap: 16px; }
}
</style>
