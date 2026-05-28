import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import { getToken } from '@/utils/auth'

// 白名单：不需要登录即可访问的路由
const whiteList = ['/', '/goods/list', '/login', '/register']

// 检查路由是否在白名单中（支持通配符匹配）
const isWhiteList = (path: string): boolean => {
  return whiteList.some(item => {
    if (item.endsWith('*')) {
      return path.startsWith(item.slice(0, -1))
    }
    if (item.includes(':')) {
      const pattern = item.replace(/:[^/]+/g, '[^/]+')
      return new RegExp(`^${pattern}$`).test(path)
    }
    return path === item || path.startsWith(item + '/')
  })
}

export const setupRouterGuard = (router: any) => {
  router.beforeEach(async (to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
    const title = to.meta.title as string
    if (title) {
      document.title = `${title} - 青禾商城`
    }

    const token = getToken()
    const path = to.path

    // 白名单路由直接放行
    if (isWhiteList(path)) {
      // 已登录用户访问登录/注册页则跳转到首页
      if ((path === '/login' || path === '/register') && token) {
        next('/')
        return
      }
      next()
      return
    }

    // 需要登录的页面检查token
    const requiresAuth = to.meta.requiresAuth as boolean
    if (requiresAuth && !token) {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }

    next()
  })

  router.afterEach((to: RouteLocationNormalized) => {
    // 路由切换后的处理
  })
}
