<template>
  <div class="phone-page">
    <!-- 商家只读提示 -->
    <div v-if="!isAdmin" class="notice-bar">
      <span>🔒</span>
      <span>商家角色：只读查看（新增 / 编辑 / 删除仅管理员可用）</span>
    </div>

    <!-- 搜索栏 -->
    <div class="admin-card">
      <div class="toolbar">
        <div class="search-box">
          <span>🔍</span>
          <input
            v-model.trim="keyword"
            placeholder="搜索机型名称 / 系列"
            maxlength="30"
            @keyup.enter="handleSearch"
          />
        </div>
        <button class="btn btn-primary btn-sweep" @click="handleSearch">查 询</button>
        <button v-if="isAdmin" class="btn btn-success btn-sweep" @click="openAdd">＋ 新增机型</button>
        <div class="toolbar-spacer"></div>
        <button class="btn btn-outline" @click="loadData">🔄 刷新</button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <div class="loading-dots"></div>
        <div>数据加载中…</div>
      </div>

      <!-- 空数据 -->
      <div v-else-if="!records.length" class="empty-state">
        <div class="empty-icon">📱</div>
        <div class="empty-text">{{ keyword ? `未找到与「${keyword}」相关的机型` : '暂无手机型号数据' }}</div>
        <div class="empty-sub">{{ keyword ? '试试更换关键词，或点击「查询」刷新' : '管理员可点击右上角「新增机型」添加，或前往「数据导入」批量导入' }}</div>
      </div>

      <!-- 数据表格 -->
      <template v-else>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>机型名称</th>
                <th>系列</th>
                <th>价格</th>
                <th>评分</th>
                <th>创建时间</th>
                <th v-if="isAdmin">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in records" :key="row.id">
                <td class="num">{{ row.id }}</td>
                <td>{{ row.modelName }}</td>
                <td>
                  <span v-if="row.series" class="tag tag-primary">{{ row.series }}</span>
                  <span v-else class="tag tag-muted">未分类</span>
                </td>
                <td class="num">¥ {{ formatPrice(row.price) }}</td>
                <td class="num">{{ row.rating != null ? '★ ' + row.rating : '—' }}</td>
                <td class="num">{{ row.createTime || '—' }}</td>
                <td v-if="isAdmin">
                  <div class="cell-actions">
                    <button class="btn btn-sm btn-outline" @click="openEdit(row)">编辑</button>
                    <button class="btn btn-sm btn-danger" @click="askDelete(row)">删除</button>
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

    <!-- 新增 / 编辑弹窗 -->
    <transition name="modal">
      <div v-if="showForm" class="modal-overlay" @click.self="closeForm">
        <div class="modal-box modal-sm">
          <div class="modal-header">
            <span class="modal-title">{{ editingId ? '编辑机型' : '新增机型' }}</span>
            <span class="modal-close" @click="closeForm">✕</span>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>机型名称<span class="required">*</span></label>
              <input
                v-model.trim="form.modelName"
                class="form-control"
                :class="{ 'input-error': errors.modelName }"
                placeholder="如：HUAWEI Mate 70 Pro"
                maxlength="60"
              />
              <div v-if="errors.modelName" class="form-error">{{ errors.modelName }}</div>
            </div>
            <div class="form-group">
              <label>系列</label>
              <input v-model.trim="form.series" class="form-control" placeholder="如：Mate 系列" maxlength="30" />
              <div class="form-hint">选填，用于大屏系列归类展示</div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>价格（元）<span class="required">*</span></label>
                <input
                  v-model.trim="form.price"
                  class="form-control"
                  :class="{ 'input-error': errors.price }"
                  placeholder="如：5999"
                  maxlength="12"
                />
                <div v-if="errors.price" class="form-error">{{ errors.price }}</div>
              </div>
              <div class="form-group">
                <label>评分（0 - 5）</label>
                <input
                  v-model.trim="form.rating"
                  class="form-control"
                  :class="{ 'input-error': errors.rating }"
                  placeholder="如：4.8"
                  maxlength="5"
                />
                <div v-if="errors.rating" class="form-error">{{ errors.rating }}</div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn" @click="closeForm">取 消</button>
            <button class="btn btn-primary btn-sweep" :disabled="saving" @click="handleSave">
              <span v-if="saving" class="btn-spinner"></span>
              {{ saving ? '保存中…' : '保 存' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 删除确认弹窗 -->
    <transition name="modal">
      <div v-if="deleteRow" class="modal-overlay" @click.self="deleteRow = null">
        <div class="modal-box modal-sm">
          <div class="modal-header">
            <span class="modal-title">删除确认</span>
            <span class="modal-close" @click="deleteRow = null">✕</span>
          </div>
          <div class="modal-body">
            <div class="confirm-icon">🗑️</div>
            <div class="confirm-text">确认删除机型「{{ deleteRow.modelName }}」？</div>
            <div class="confirm-sub">删除后不可恢复，相关统计将同步更新</div>
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

/* ===== 列表状态 ===== */
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')
const loading = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

// 页码窗口（最多展示 5 个页码）
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

// 统一错误处理（403 → 无操作权限）
function handleErr(e, fallback = '操作失败') {
  if (e?.response?.status === 403) {
    showToast('无操作权限', 'error')
  } else {
    showToast(e?.response?.data?.message || e?.message || fallback, 'error')
  }
}

/* ===== 价格格式化 ===== */
function formatPrice(v) {
  if (v === null || v === undefined || isNaN(Number(v))) return '—'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

/* ===== 加载列表 ===== */
async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (keyword.value) params.keyword = keyword.value
    const data = await adminApi.getPhones(params)
    records.value = data?.records || []
    total.value = data?.total || 0
    // 边界：当前页超出总页数（如删除后），回退到最后一页
    if (page.value > totalPages.value && totalPages.value > 0) {
      page.value = totalPages.value
      return loadData()
    }
  } catch (e) {
    handleErr(e, '列表加载失败')
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  if (page.value !== 1) {
    page.value = 1
  }
  loadData()
}

function changePage(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return
  page.value = p
  loadData()
}

/* ===== 新增 / 编辑弹窗 ===== */
const showForm = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = ref({ modelName: '', series: '', price: '', rating: '' })
const errors = ref({})

function openAdd() {
  editingId.value = null
  form.value = { modelName: '', series: '', price: '', rating: '' }
  errors.value = {}
  showForm.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = {
    modelName: row.modelName || '',
    series: row.series || '',
    price: row.price != null ? String(row.price) : '',
    rating: row.rating != null ? String(row.rating) : ''
  }
  errors.value = {}
  showForm.value = true
}

function closeForm() {
  showForm.value = false
}

// 表单校验：机型名称必填、价格必填且为数字、评分选填但须为 0-5 数字
function validate() {
  const e = {}
  if (!form.value.modelName) {
    e.modelName = '请输入机型名称'
  }
  if (form.value.price === '') {
    e.price = '请输入价格'
  } else if (isNaN(Number(form.value.price)) || Number(form.value.price) < 0) {
    e.price = '价格必须为非负数字'
  }
  if (form.value.rating !== '') {
    const r = Number(form.value.rating)
    if (isNaN(r) || r < 0 || r > 5) e.rating = '评分须为 0 - 5 的数字'
  }
  errors.value = e
  return !Object.keys(e).length
}

async function handleSave() {
  if (!validate() || saving.value) return
  saving.value = true
  try {
    const payload = {
      modelName: form.value.modelName,
      series: form.value.series || null,
      price: Number(form.value.price),
      rating: form.value.rating === '' ? null : Number(form.value.rating)
    }
    if (editingId.value) {
      await adminApi.updatePhone({ id: editingId.value, ...payload })
      showToast('机型更新成功')
    } else {
      await adminApi.addPhone(payload)
      showToast('机型新增成功')
    }
    showForm.value = false
    loadData()
  } catch (e) {
    handleErr(e, '保存失败')
  } finally {
    saving.value = false
  }
}

/* ===== 删除 ===== */
const deleteRow = ref(null)
const deleting = ref(false)

function askDelete(row) {
  deleteRow.value = row
}

async function handleDelete() {
  if (!deleteRow.value || deleting.value) return
  deleting.value = true
  try {
    await adminApi.deletePhone(deleteRow.value.id)
    showToast('删除成功')
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
.phone-page {
  width: 100%;
}
</style>
