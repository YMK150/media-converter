import { createRouter, createWebHistory } from 'vue-router'
import Home from './views/Home.vue'
import Login from './views/Login.vue'
import Setup from './views/Setup.vue'
import { useAuthStore } from './stores/auth'
import { checkSetup } from './utils/authApi'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { guest: true }
  },
  {
    path: '/setup',
    name: 'Setup',
    component: Setup,
    meta: { guest: true }
  }
]

const router = createRouter({
  history: createWebHistory('/'),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 如果路由需要认证
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      // 未登录，检查是否已有用户
      try {
        const hasUsers = await checkSetup()
        if (hasUsers) {
          // 有用户，跳转到登录页
          next('/login')
        } else {
          // 没有用户，跳转到设置页面
          next('/setup')
        }
      } catch (error) {
        console.error('检查设置状态失败:', error)
        next('/setup')
      }
      return
    }
  }

  // 如果路由是游客页面（已登录用户不能访问）
  if (to.meta.guest && authStore.isAuthenticated) {
    next('/')
    return
  }

  next()
})

export default router