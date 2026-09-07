<template>
  <div class="order-page">
    <!-- 工具栏 -->
    <div class="admin-card">
      <div class="toolbar">
        <span class="toolbar-title">📦 订单列表</span>
        <div class="toolbar-spacer"></div>
        <button class="btn btn-outline" @click="loadData">🔄 刷新</button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <div class="loading-dots"></div>
        <div>订单加载中…</div>
      </div>

      <!-- 空数据 -->
      <div v-else-if="!records.length" class="empty-state">
        <div class="empty-icon">📦</div>
        <div class="empty-text">暂无订单数据</div>
        <div class="empty-sub">可前往「数据导入」上传订单 Excel，或等待新订单产生</div>
      </div>

      <!-- 表格 -->
      <template v-else>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>订单号</th>
                <th>用户</th>
                <th>机型</th>
                <th>金额</th>
                <th>省份</th>
                <th>城市</th>
                <th>状态</th>
                <th>时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in records" :key="row.id">
                <td class="num">{{ row.orderNo }}</td>
                <td>{{ row.userName }}</td>
                <td>{{ row.modelName }}</td>
                <td class="num">{{ isAdmin ? formatMoney(row.amount) : '---' }}</td>
                <td>{{ row.province || '—' }}</td>
                <td>{{ row.city || '—' }}</td>
                <td>
                  <span class="tag" :class="statusTag(row.status)">{{ statusText(row.status) }}</span>
                </td>
                <td class="num">{{ row.createTime || '—' }}</td>
                <td>
                  <div class="cell-actions">
                    <button class="btn btn-sm btn-outline" @click="openStatus(row)">修改状态</button>
                    <button v-if="isAdmin" class="btn btn-sm btn-danger" @click="askDelete(row)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <div class="pagination">
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
      </template>
    </div>

    <!-- 修改状态弹窗（按钮组，两角色均可操作） -->
    <transition name="modal">
      <div v-if="statusRow" class="modal-overlay" @click.self="statusRow = null">
        <div class="modal-box modal-sm">
          <div class="modal-header">
            <span class="modal-title">修改订单状态</span>
            <span class="modal-close" @click="statusRow = null">✕</span>
          </div>
          <div class="modal-body">
            <div class="confirm-sub" style="margin-bottom: 14px">
              订单号：{{ statusRow.orderNo }} · {{ statusRow.modelName }}
            </div>
            <div class="status-btn-group">
              <button
                v-for="(s, k) in STATUS_MAP"
                :key="k"
                class="status-btn"
                :class="[`s${k}`, { 'is-current': Number(statusRow.status) === Number(k) }]"
                :disabled="updating || Number(statusRow.status) === Number(k)"
                @click="handleUpdateStatus(Number(k))"
              >
                {{ Number(statusRow.status) === Number(k) ? `✓ ${s.text}（当前）` : s.text }}
              </button>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn" :disabled="updating" @click="statusRow = null">关 闭</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 删除确认弹窗（仅管理员） -->
    <transition name="modal">
      <div v-if="deleteRow" class="modal-overlay" @click.self="deleteRow = null">
        <div class="modal-box modal-sm">
          <div class="modal-header">
            <span class="modal-title">删除确认</span>
            <span class="modal-close" @click="deleteRow = null">✕</span>
          </div>
          <div class="modal-body">
            <div class="confirm-icon">🗑️</div>
            <div class="confirm-text">确认删除订单「{{ deleteRow.orderNo }}」？</div>
            <div class="confirm-sub">删除后不可恢复，销售统计将同步更新</div>
          </div>
          <div class="modal-footer">
            <button class="btn" @click="deleteRow = null">取 消</button>
            <button class="btn btn-danger" :disabled="deleting" @click="handleDelete">
              <span v-if="deleting" class="btn-spinner"></span>
              {{ deleting ? '删除中…' : '确认删除' }}
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

/* ===== 角色 ===== */
const isAdmin = computed(() => localStorage.getItem('hw_role') === 'ADMIN')

/* ===== 状态映射：1待付款黄 2已付款蓝 3已发货紫 4已完成绿 ===== */
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

/* ===== 金额格式化（商家不可见金额） ===== */
function formatMoney(v) {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '¥ —'
  return '¥ ' + Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

/* ===== 列表状态 ===== */
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

const pageList = computed(() => {
  const tp = totalPages.value
  const start = Math.max(1, Math.min(page.value - 2, tp - 4))
  const end = Math.min(tp, start + 4)
  const list = []
  for (let i = start; i <= end; i++) list.push(i)
  return list
})

/* ===== toast ===== */
const toast = ref(null)
let toastTimer = null

function showToast(msg, type = 'success') {
  toast.value = { msg, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = null), 2600)
}

function handleErr(e, fallback = '操作失败') {
  if (e?.response?.status === 403) {
    showToast('无操作权限', 'error')
  } else {
    showToast(e?.response?.data?.message || e?.message || fallback, 'error')
  }
}

/* ===== 加载列表 ===== */
async function loadData() {
  loading.value = true
  try {
    const data = await adminApi.getOrders({ page: page.value, size })
    records.value = data?.records || []
    total.value = data?.total || 0
    // 边界：当前页超出总页数时回退
    if (page.value > totalPages.value && totalPages.value > 0) {
      page.value = totalPages.value
      return loadData()
    }
  } catch (e) {
    handleErr(e, '订单加载失败')
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return
  page.value = p
  loadData()
}

/* ===== 修改状态（按钮组，行内更新） ===== */
const statusRow = ref(null)
const updating = ref(false)

function openStatus(row) {
  statusRow.value = row
}

async function handleUpdateStatus(newStatus) {
  if (!statusRow.value || updating.value) return
  updating.value = true
  try {
    await adminApi.updateOrderStatus(statusRow.value.id, newStatus)
    // 行内更新状态
    statusRow.value.status = newStatus
    showToast(`订单已更新为「${STATUS_MAP[newStatus].text}」`)
    statusRow.value = null
  } catch (e) {
    handleErr(e, '状态更新失败')
  } finally {
    updating.value = false
  }
}

/* ===== 删除（仅管理员） ===== */
const deleteRow = ref(null)
const deleting = ref(false)

function askDelete(row) {
  deleteRow.value = row
}

async function handleDelete() {
  if (!deleteRow.value || deleting.value) return
  deleting.value = true
  try {
    await adminApi.deleteOrder(deleteRow.value.id)
    showToast('订单删除成功')
    deleteRow.value = null
    // 边界：末页仅剩一条被删除后，回退一页
    if (records.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    loadData()
  } catch (e) {
    handleErr(e, '删除失败')
  } finally {
    deleting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.order-page {
  width: 100%;
}

.toolbar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 1px;
}
</style>
