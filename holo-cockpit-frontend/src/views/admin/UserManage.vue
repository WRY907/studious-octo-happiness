<template>
  <div class="user-page">
    <!-- 非管理员防护提示（路由守卫已拦截，双保险） -->
    <div v-if="!isAdmin" class="notice-bar notice-danger">
      <span>⛔</span>
      <span>无操作权限：用户管理仅管理员可见</span>
    </div>

    <template v-else>
      <div class="admin-card">
        <div class="toolbar">
          <span class="toolbar-title">👥 系统用户</span>
          <div class="toolbar-spacer"></div>
          <button class="btn btn-outline" @click="loadData">🔄 刷新</button>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="loading-state">
          <div class="loading-dots"></div>
          <div>用户数据加载中…</div>
        </div>

        <!-- 加载失败 -->
        <div v-else-if="loadFailed" class="empty-state">
          <div class="empty-icon">⚠️</div>
          <div class="empty-text">用户数据加载失败</div>
          <div class="empty-sub">{{ loadError }}，请稍后重试</div>
          <button class="btn btn-sm" style="margin-top: 14px" @click="loadData">重新加载</button>
        </div>

        <!-- 空数据 -->
        <div v-else-if="!records.length" class="empty-state">
          <div class="empty-icon">👥</div>
          <div class="empty-text">暂无系统用户</div>
        </div>

        <!-- 表格 -->
        <template v-else>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户名</th>
                  <th>角色</th>
                  <th>创建时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in records" :key="row.id">
                  <td class="num">{{ row.id }}</td>
                  <td>{{ row.username }}</td>
                  <td>
                    <span v-if="row.role === 'ADMIN'" class="tag tag-primary">👑 管理员</span>
                    <span v-else class="tag tag-accent">
                      🏪 商家<template v-if="row.merchantName"> · {{ row.merchantName }}</template>
                    </span>
                  </td>
                  <td class="num">{{ row.createTime || '—' }}</td>
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
    </template>

    <!-- toast -->
    <transition name="toast">
      <div v-if="toast" class="toast" :class="`toast-${toast.type}`">{{ toast.msg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api'

/* ===== 角色防护 ===== */
const isAdmin = computed(() => localStorage.getItem('hw_role') === 'ADMIN')

/* ===== 列表状态 ===== */
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const loadFailed = ref(false)
const loadError = ref('')

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

function showToast(msg, type = 'error') {
  toast.value = { msg, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = null), 2600)
}

/* ===== 加载列表 ===== */
async function loadData() {
  if (!isAdmin.value) return
  loading.value = true
  loadFailed.value = false
  try {
    const data = await adminApi.getUsers({ page: page.value, size })
    records.value = data?.records || []
    total.value = data?.total || 0
    // 边界：当前页超出总页数时回退
    if (page.value > totalPages.value && totalPages.value > 0) {
      page.value = totalPages.value
      return loadData()
    }
  } catch (e) {
    loadFailed.value = true
    loadError.value = e?.response?.status === 403
      ? '无操作权限'
      : (e?.response?.data?.message || e?.message || '服务异常')
    records.value = []
    total.value = 0
    if (e?.response?.status === 403) showToast('无操作权限', 'error')
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return
  page.value = p
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.user-page {
  width: 100%;
}

.toolbar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 1px;
}
</style>
