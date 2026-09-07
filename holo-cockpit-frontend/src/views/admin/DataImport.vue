<template>
  <div class="import-page">
    <!-- 非管理员防护（路由守卫已拦截，双保险） -->
    <div v-if="!isAdmin" class="notice-bar notice-danger">
      <span>⛔</span>
      <span>无操作权限：数据导入仅管理员可用</span>
    </div>

    <template v-else>
      <!-- 页头 -->
      <div class="page-header">
        <div>
          <div class="page-title">Excel 数据导入</div>
          <div class="page-sub">支持 8 张驾驶舱数据表批量导入 · 流程：下载模板 → 按模板填写数据 → 上传解析入库</div>
        </div>
      </div>

      <!-- ===== 表类型选择 ===== -->
      <div class="admin-card">
        <div class="card-title">
          选择数据表
          <span class="title-en">Table Type</span>
        </div>
        <div class="tab-bar">
          <div
            v-for="t in TABLE_DEFS"
            :key="t.table"
            class="tab-item"
            :class="{ active: activeTable === t.table }"
            @click="switchTable(t.table)"
          >
            <span class="tab-icon">{{ t.icon }}</span>{{ tableLabel(t.table) }}
          </div>
        </div>
        <!-- 元信息加载失败提示 -->
        <div v-if="metaFailed" class="notice-bar notice-danger" style="margin-top: 14px; margin-bottom: 0">
          <span>⚠️</span>
          <span>表列定义加载失败：{{ metaError }}，可先下载模板参考列格式</span>
          <button class="btn btn-sm" @click="loadMeta">重试</button>
        </div>
      </div>

      <!-- ===== 表信息卡：列定义 + 模板下载 ===== -->
      <div class="admin-card">
        <div class="card-title">
          {{ tableLabel(activeTable) }} · 列定义
          <span class="title-en">Columns</span>
        </div>

        <div class="toolbar">
          <button class="btn btn-primary btn-sweep" :disabled="downloading" @click="downloadTemplate">
            <span v-if="downloading" class="btn-spinner"></span>
            {{ downloading ? '模板生成中…' : `⬇ 下载「${tableLabel(activeTable)}」导入模板` }}
          </button>
          <span class="form-hint">模板文件名：{{ activeTable }}_template.xlsx（.xlsx 格式）</span>
        </div>

        <!-- 列定义加载中 -->
        <div v-if="metaLoading" class="loading-state">
          <div class="loading-dots"></div>
          <div>列定义加载中…</div>
        </div>

        <!-- 列定义表格 -->
        <div v-else-if="currentColumns.length" class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th style="width: 80px">序号</th>
                <th>列名（Excel 表头）</th>
                <th>字段（后端）</th>
                <th style="width: 120px">是否必填</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(col, i) in currentColumns" :key="col.field + i">
                <td class="num">{{ i + 1 }}</td>
                <td>{{ col.label }}</td>
                <td class="num">{{ col.field }}</td>
                <td>
                  <span v-if="col.required" class="tag tag-danger">必填 *</span>
                  <span v-else class="tag tag-muted">选填</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 无列定义（元信息失败时的兜底提示） -->
        <div v-else class="empty-state">
          <div class="empty-icon">📋</div>
          <div class="empty-text">暂无「{{ tableLabel(activeTable) }}」的列定义</div>
          <div class="empty-sub">请下载模板查看该表所需的列格式</div>
        </div>
      </div>

      <!-- ===== 上传卡 ===== -->
      <div class="admin-card">
        <div class="card-title">
          上传文件
          <span class="title-en">Upload</span>
        </div>

        <!-- 操作引导（始终展示） -->
        <div class="step-guide">
          <div class="step-item">
            <span class="step-no">1</span>
            <span class="step-text">点击上方按钮，下载 <b>{{ tableLabel(activeTable) }}</b> 模板文件</span>
          </div>
          <div class="step-item">
            <span class="step-no">2</span>
            <span class="step-text">按模板列定义填写数据，<b>必填列（*）</b>不能为空</span>
          </div>
          <div class="step-item">
            <span class="step-no">3</span>
            <span class="step-text">将填好的 <b>.xlsx / .xls</b> 文件拖入或点击下方区域上传</span>
          </div>
        </div>

        <!-- 拖拽上传区（未选文件 & 非上传中） -->
        <div
          v-if="!file"
          class="upload-area"
          :class="{ dragover }"
          @click="triggerSelect"
          @dragover.prevent="dragover = true"
          @dragleave.prevent="dragover = false"
          @drop.prevent="onDrop"
        >
          <div class="upload-icon">📥</div>
          <div class="upload-title">将 Excel 文件拖拽到此处，或点击选择文件</div>
          <div class="upload-tip">当前导入表：{{ tableLabel(activeTable) }}（{{ activeTable }}） · 仅支持 .xlsx / .xls</div>
        </div>

        <!-- 已选文件 / 上传中 / 结果 -->
        <template v-else>
          <!-- 文件信息 + 确认上传 -->
          <div class="upload-file">
            <span class="file-icon">📄</span>
            <div class="file-info">
              <div class="file-name">{{ file.name }}</div>
              <div class="file-size">{{ formatSize(file.size) }} · 导入至「{{ tableLabel(activeTable) }}」</div>
            </div>
            <button
              class="btn btn-success btn-sweep"
              :disabled="uploadState === 'uploading'"
              @click="handleUpload"
            >
              <span v-if="uploadState === 'uploading'" class="btn-spinner"></span>
              {{ uploadState === 'uploading' ? (uploadPhase === 'upload' ? '上传中…' : '解析中…') : '✓ 确认上传' }}
            </button>
            <button class="btn btn-danger" :disabled="uploadState === 'uploading'" @click="removeFile">移除</button>
          </div>

          <!-- 上传进度（上传中 / 解析中） -->
          <div v-if="uploadState === 'uploading'" class="upload-progress">
            <div class="progress-text">
              <span>{{ uploadPhase === 'upload' ? '正在上传文件…' : '后端解析 Excel 并写入数据库…' }}</span>
              <span>{{ uploadPhase === 'upload' ? '上传中' : '解析中' }}</span>
            </div>
            <div class="progress-track">
              <div class="progress-bar"></div>
            </div>
          </div>

          <!-- 上传失败 -->
          <div v-if="uploadState === 'error'" class="notice-bar notice-danger" style="margin-top: 14px">
            <span>❌</span>
            <span>导入失败：{{ uploadError }}</span>
            <button class="btn btn-sm" @click="removeFile">重新选择文件</button>
          </div>

          <!-- 导入结果面板 -->
          <div v-if="uploadState === 'done' && result" class="import-result">
            <div class="result-grid">
              <div class="result-box r-total">
                <div class="result-num">{{ result.total ?? 0 }}</div>
                <div class="result-label">总 行 数</div>
              </div>
              <div class="result-box r-success">
                <div class="result-num">{{ result.success ?? 0 }}</div>
                <div class="result-label">成 功 数</div>
              </div>
              <div class="result-box r-fail">
                <div class="result-num">{{ result.fail ?? 0 }}</div>
                <div class="result-label">失 败 数</div>
              </div>
            </div>

            <!-- 错误明细 -->
            <template v-if="result.errors && result.errors.length">
              <div class="card-title" style="margin-top: 4px">
                错误明细
                <span class="title-en">Errors</span>
              </div>
              <div class="table-wrap">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th style="width: 120px">行号</th>
                      <th>错误原因</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(err, i) in visibleErrors" :key="i">
                      <td class="num">{{ err.row ?? '—' }}</td>
                      <td>{{ err.message }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-if="result.errors.length > 50" class="form-hint" style="margin-top: 8px">
                仅展示前 50 条错误，共 {{ result.errors.length }} 条，请修正后重新上传
              </div>
            </template>
            <div v-else-if="(result.fail ?? 0) > 0" class="form-hint">存在失败行，但后端未返回错误明细</div>

            <div class="toolbar" style="margin-top: 16px">
              <button class="btn btn-primary btn-sweep" @click="resetImport">↻ 重新导入</button>
              <button class="btn btn-outline" @click="removeFile">更换文件</button>
            </div>
          </div>
        </template>
      </div>
    </template>

    <!-- 隐藏的文件选择器 -->
    <input
      ref="fileInput"
      type="file"
      accept=".xlsx,.xls"
      style="display: none"
      @change="onFileChange"
    />

    <!-- toast -->
    <transition name="toast">
      <div v-if="toast" class="toast" :class="`toast-${toast.type}`">{{ toast.msg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { importApi } from '@/api'

/* ===== 角色防护 ===== */
const isAdmin = computed(() => localStorage.getItem('hw_role') === 'ADMIN')

/* ===== 8 张表的兜底定义（中文名以接口元信息优先） ===== */
const TABLE_DEFS = [
  { table: 'phone',   icon: '📱', name: '手机型号' },
  { table: 'region',  icon: '🗺️', name: '省份销售' },
  { table: 'city',    icon: '🏙️', name: '城市销售' },
  { table: 'order',   icon: '📦', name: '实时订单' },
  { table: 'hot',     icon: '🔥', name: '热销机型' },
  { table: 'traffic', icon: '🚦', name: '流量来源' },
  { table: 'profile', icon: '👤', name: '用户画像' },
  { table: 'trend',   icon: '📈', name: '销售趋势' }
]

/* ===== 元信息 ===== */
const metaList = ref([])
const metaLoading = ref(false)
const metaFailed = ref(false)
const metaError = ref('')

const metaMap = computed(() => {
  const map = {}
  for (const m of metaList.value) {
    if (m?.table) map[m.table] = m
  }
  return map
})

async function loadMeta() {
  metaLoading.value = true
  metaFailed.value = false
  try {
    const data = await importApi.getMeta()
    metaList.value = Array.isArray(data) ? data : []
  } catch (e) {
    metaFailed.value = true
    metaError.value = e?.response?.data?.message || e?.message || '服务异常'
    metaList.value = []
  } finally {
    metaLoading.value = false
  }
}

/* ===== 当前选中表 ===== */
const activeTable = ref('phone')

// 表中文名：优先接口元信息，兜底本地定义
function tableLabel(t) {
  return metaMap.value[t]?.name || TABLE_DEFS.find(x => x.table === t)?.name || t
}

// 当前表列定义
const currentColumns = computed(() => metaMap.value[activeTable.value]?.columns || [])

// 切换表：重置上传状态，避免文件与表类型错配
function switchTable(t) {
  if (t === activeTable.value) return
  activeTable.value = t
  resetImport()
}

/* ===== toast ===== */
const toast = ref(null)
let toastTimer = null

function showToast(msg, type = 'success') {
  toast.value = { msg, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = null), 3000)
}

function handleErr(e, fallback = '操作失败') {
  if (e?.response?.status === 403) {
    showToast('无操作权限', 'error')
  } else {
    showToast(e?.response?.data?.message || e?.message || fallback, 'error')
  }
}

/* ===== 模板下载（blob → a 标签触发下载） ===== */
const downloading = ref(false)

async function downloadTemplate() {
  if (downloading.value) return
  downloading.value = true
  try {
    const blob = await importApi.downloadTemplate(activeTable.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${activeTable.value}_template.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    showToast('模板下载成功，请按模板列定义填写数据')
  } catch (e) {
    handleErr(e, '模板下载失败')
  } finally {
    downloading.value = false
  }
}

/* ===== 文件选择（拖拽 + 点击） ===== */
const fileInput = ref(null)
const file = ref(null)
const dragover = ref(false)

function triggerSelect() {
  if (uploadState.value === 'uploading') return
  fileInput.value?.click()
}

function onFileChange(e) {
  const f = e.target.files?.[0]
  e.target.value = '' // 允许重复选择同一文件
  pickFile(f)
}

function onDrop(e) {
  dragover.value = false
  const f = e.dataTransfer?.files?.[0]
  pickFile(f)
}

// 文件校验：仅允许 .xlsx / .xls
function pickFile(f) {
  if (!f) return
  if (uploadState.value === 'uploading') return
  const name = f.name.toLowerCase()
  if (!name.endsWith('.xlsx') && !name.endsWith('.xls')) {
    showToast('文件类型不符：仅支持 .xlsx / .xls 格式的 Excel 文件', 'warning')
    return
  }
  file.value = f
  result.value = null
  uploadError.value = ''
  uploadState.value = 'idle'
}

function removeFile() {
  if (uploadState.value === 'uploading') return
  file.value = null
  result.value = null
  uploadError.value = ''
  uploadState.value = 'idle'
}

// 重置整个导入流程（保留当前表选中）
function resetImport() {
  file.value = null
  result.value = null
  uploadError.value = ''
  uploadState.value = 'idle'
}

/* ===== 文件大小格式化 ===== */
function formatSize(bytes) {
  if (bytes === null || bytes === undefined) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

/* ===== 上传导入 ===== */
const uploadState = ref('idle') // idle | uploading | done | error
const uploadPhase = ref('upload') // 上传中 | 解析中
const uploadError = ref('')
const result = ref(null)
let phaseTimer = null

// 错误明细最多展示前 50 条
const visibleErrors = computed(() => (result.value?.errors || []).slice(0, 50))

async function handleUpload() {
  if (!file.value || uploadState.value === 'uploading') return
  uploadState.value = 'uploading'
  uploadPhase.value = 'upload'
  uploadError.value = ''
  result.value = null

  // 上传 → 解析 两阶段提示（长耗时导入的状态反馈）
  clearTimeout(phaseTimer)
  phaseTimer = setTimeout(() => {
    if (uploadState.value === 'uploading') uploadPhase.value = 'parse'
  }, 2200)

  try {
    const res = await importApi.upload(activeTable.value, file.value)
    result.value = {
      total: res?.total ?? 0,
      success: res?.success ?? 0,
      fail: res?.fail ?? 0,
      errors: res?.errors || []
    }
    uploadState.value = 'done'
    if ((res?.fail ?? 0) > 0) {
      showToast(`导入完成：成功 ${res.success} 行，失败 ${res.fail} 行，请查看错误明细`, 'warning')
    } else {
      showToast(`导入完成：共 ${res?.total ?? 0} 行全部成功`)
    }
  } catch (e) {
    // 表类型与文件不匹配等后端校验错误在此展示
    uploadState.value = 'error'
    uploadError.value = e?.response?.data?.message || e?.message || '服务异常'
    handleErr(e, '上传失败')
  } finally {
    clearTimeout(phaseTimer)
  }
}

onMounted(() => {
  if (isAdmin.value) loadMeta()
})

onBeforeUnmount(() => {
  clearTimeout(toastTimer)
  clearTimeout(phaseTimer)
})
</script>

<style scoped>
.import-page {
  width: 100%;
}
</style>
