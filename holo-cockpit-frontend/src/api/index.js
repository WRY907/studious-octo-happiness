import axios from 'axios'

/* ========== Axios 实例 ========== */
const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截：附带 JWT
http.interceptors.request.use(config => {
  const token = localStorage.getItem('hw_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：统一解包 + 401 跳登录（blob 响应直接透传）
http.interceptors.response.use(
  res => {
    // blob 响应（文件下载）不解包
    if (res.config.responseType === 'blob' || res.data instanceof Blob) return res.data
    const body = res.data
    if (body && body.code === 200) return body.data
    return Promise.reject(new Error(body?.message || '接口异常'))
  },
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('hw_token')
      localStorage.removeItem('hw_role')
      if (location.hash.includes('/admin')) location.hash = '#/login'
    }
    return Promise.reject(err)
  }
)

/* ========== 驾驶舱数据 ========== */
export const cockpitApi = {
  getAll: () => http.get('/cockpit/all'),
  getRegion: (date) => http.get('/cockpit/region', { params: { date } }),
  getCity: (date) => http.get('/cockpit/city', { params: { date } })
}

/* ========== AI ========== */
export const aiApi = {
  // 对话（SSE 流式，用 fetch 手动读流）
  chat: (message, history = []) => fetch('/api/ai/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, history })
  }),
  // 自然语言查询
  query: (question) => http.post('/ai/query', { question }),
  // 预警
  getAlerts: () => http.get('/ai/alerts'),
  checkAlerts: () => http.post('/ai/alerts/check'),
  // 报告
  getReport: () => http.post('/ai/report')
}

/* ========== 认证 ========== */
export const authApi = {
  login: (username, password) => http.post('/auth/login', { username, password })
}

/* ========== 管理后台 ========== */
export const adminApi = {
  // 手机型号
  getPhones: (params) => http.get('/admin/phones', { params }),
  addPhone: (data) => http.post('/admin/phones', data),
  updatePhone: (data) => http.put('/admin/phones', data),
  deletePhone: (id) => http.delete(`/admin/phones/${id}`),
  // 订单
  getOrders: (params) => http.get('/admin/orders', { params }),
  updateOrderStatus: (id, status) => http.put(`/admin/orders/${id}/status`, { status }),
  deleteOrder: (id) => http.delete(`/admin/orders/${id}`),
  // 用户
  getUsers: (params) => http.get('/admin/users', { params }),
  // 后台统计
  getStats: () => http.get('/admin/stats')
}

/* ========== 数据导入（Excel） ========== */
export const importApi = {
  // 各数据表类型：phone/region/city/order/hot/traffic/profile/trend
  upload: (table, file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post(`/admin/import/${table}`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  },
  // 下载导入模板（返回 blob）
  downloadTemplate: (table) => http.get(`/admin/import/${table}/template`, { responseType: 'blob' }),
  // 表元信息（列定义）
  getMeta: () => http.get('/admin/import/meta')
}
