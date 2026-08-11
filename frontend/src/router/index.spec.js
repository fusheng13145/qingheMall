import { describe, it, expect, vi, beforeEach } from 'vitest'

const { mockUserStore } = vi.hoisted(() => ({
  mockUserStore: {
    isLoggedIn: false,
    isAdmin: false,
    isMerchant: false,
    checkLogin: vi.fn()
  }
}))

vi.mock('../stores/user.js', () => ({
  useUserStore: () => mockUserStore
}))

import { authGuard } from './index'
import router from './index'

const guard = authGuard

function makeTo(overrides = {}) {
  return {
    meta: {},
    fullPath: '/',
    name: 'Home',
    ...overrides
  }
}

describe('路由守卫：会话恢复（P0-4）', () => {
  beforeEach(() => {
    Object.assign(mockUserStore, { isLoggedIn: false, isAdmin: false, isMerchant: false })
    mockUserStore.checkLogin.mockReset()
    mockUserStore.checkLogin.mockResolvedValue()
  })

  it('未登录访问受保护页：先 await checkLogin 再放行（会话恢复成功）', async () => {
    mockUserStore.checkLogin.mockImplementation(() => {
      mockUserStore.isLoggedIn = true
      return Promise.resolve()
    })
    const next = vi.fn()
    await guard(makeTo({ meta: { requiresAuth: true }, fullPath: '/orders', name: 'Orders' }), {}, next)
    expect(mockUserStore.checkLogin).toHaveBeenCalledTimes(1)
    expect(next).toHaveBeenCalledWith()
  })

  it('恢复失败：跳转登录页并携带 redirect', async () => {
    const next = vi.fn()
    await guard(makeTo({ meta: { requiresAuth: true }, fullPath: '/cart', name: 'Cart' }), {}, next)
    expect(next).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/cart' } })
  })

  it('已登录访问受保护页：不触发 checkLogin 直接放行', async () => {
    mockUserStore.isLoggedIn = true
    const next = vi.fn()
    await guard(makeTo({ meta: { requiresAuth: true } }), {}, next)
    expect(mockUserStore.checkLogin).not.toHaveBeenCalled()
    expect(next).toHaveBeenCalledWith()
  })

  it('公共页无需登录直接放行', async () => {
    const next = vi.fn()
    await guard(makeTo({ fullPath: '/products' }), {}, next)
    expect(mockUserStore.checkLogin).not.toHaveBeenCalled()
    expect(next).toHaveBeenCalledWith()
  })
})

describe('路由守卫：角色控制', () => {
  beforeEach(() => {
    Object.assign(mockUserStore, { isLoggedIn: true, isAdmin: false, isMerchant: false })
    mockUserStore.checkLogin.mockReset()
  })

  it('非管理员访问 /admin 重定向首页', async () => {
    const next = vi.fn()
    await guard(makeTo({ meta: { requiresAuth: true, requiresAdmin: true }, fullPath: '/admin' }), {}, next)
    expect(next).toHaveBeenCalledWith({ name: 'Home' })
  })

  it('管理员访问 /admin 放行', async () => {
    mockUserStore.isAdmin = true
    const next = vi.fn()
    await guard(makeTo({ meta: { requiresAuth: true, requiresAdmin: true } }), {}, next)
    expect(next).toHaveBeenCalledWith()
  })

  it('非商家访问 /merchant 重定向首页', async () => {
    const next = vi.fn()
    await guard(makeTo({ meta: { requiresAuth: true, requiresMerchant: true }, fullPath: '/merchant' }), {}, next)
    expect(next).toHaveBeenCalledWith({ name: 'Home' })
  })

  it('商家访问 /merchant 放行', async () => {
    mockUserStore.isMerchant = true
    const next = vi.fn()
    await guard(makeTo({ meta: { requiresAuth: true, requiresMerchant: true } }), {}, next)
    expect(next).toHaveBeenCalledWith()
  })
})

describe('路由表：404 兜底（P2-14）', () => {
  it('未知路径解析到 NotFound', () => {
    expect(router.resolve('/no-such-page').name).toBe('NotFound')
    expect(router.resolve('/admin/x/y/deep').name).toBe('NotFound')
  })

  it('已知路径不受兜底影响', () => {
    expect(router.resolve('/').name).toBe('Home')
    expect(router.resolve('/products').name).toBe('Products')
    expect(router.resolve('/admin/orders').name).toBe('AdminOrders')
  })
})
