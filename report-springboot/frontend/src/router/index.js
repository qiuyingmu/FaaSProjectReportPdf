import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'

const routes = [
  {
    path: '/login',
    component: Login
  },
  {
    path: '/',
    component: Dashboard,
    // 路由守卫：进入后台前检查 Session 是否有效
    beforeEnter: async (to, from, next) => {
      try {
        const res = await fetch('/api/admin/session', { credentials: 'include' })
        const d = await res.json()
        if (!d.valid) {
          next('/login')
          return
        }
      } catch {
        next('/login')
        return
      }
      next()
    }
  }
]

const router = createRouter({ history: createWebHistory(), routes })

export default router
