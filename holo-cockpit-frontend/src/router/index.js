import { createRouter, createWebHashHistory } from 'vue-router'

/* 路由守卫：登录态 + 角色校验 */
function guard(to, from, next) {
  const token = localStorage.getItem('hw_token')
  const role = localStorage.getItem('hw_role')

  if (to.meta.requiresAuth && !token) {
    return next('/login')
  }
  if (to.meta.adminOnly && role !== 'ADMIN') {
    return next('/admin/dashboard')
  }
  // 已登录访问登录页 → 跳大屏
  if (to.path === '/login' && token) {
    return next('/')
  }
  next()
}

const routes = [
  {
    path: '/',
    name: 'Cockpit',
    component: () => import('@/views/Cockpit.vue'),
    meta: { requiresAuth: true } // 大屏需登录后查看
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue')
      },
      {
        path: 'phones',
        name: 'PhoneManage',
        component: () => import('@/views/admin/PhoneManage.vue')
      },
      {
        path: 'orders',
        name: 'OrderManage',
        component: () => import('@/views/admin/OrderManage.vue')
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { adminOnly: true }
      },
      {
        path: 'import',
        name: 'DataImport',
        component: () => import('@/views/admin/DataImport.vue'),
        meta: { adminOnly: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(guard)

export default router
