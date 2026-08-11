import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from './user'
import { useCartStore } from './cart'

vi.mock('../api/user', () => ({
  loginApi: vi.fn(),
  logoutApi: vi.fn(),
  checkLoginApi: vi.fn()
}))

import { loginApi, logoutApi, checkLoginApi } from '../api/user'

describe('user store 登录态管理', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('applyUser 完整写入登录态', async () => {
    loginApi.mockResolvedValue({ data: { id: 1, userName: 'alice', nickName: '爱丽丝', role: 'ADMIN' } })
    const store = useUserStore()
    const res = await store.login('alice', '123456')
    expect(res.success).toBe(true)
    expect(store.isLoggedIn).toBe(true)
    expect(store.userId).toBe(1)
    expect(store.role).toBe('ADMIN')
    expect(store.isAdmin).toBe(true)
  })

  it('nickName 缺省回退 userName', async () => {
    loginApi.mockResolvedValue({ data: { id: 2, userName: 'bob' } })
    const store = useUserStore()
    await store.login('bob', 'pwd')
    expect(store.nickName).toBe('bob')
  })

  it('登录失败返回 message 且不置登录态', async () => {
    loginApi.mockRejectedValue(new Error('账号或密码错误'))
    const store = useUserStore()
    const res = await store.login('x', 'y')
    expect(res.success).toBe(false)
    expect(res.message).toBe('账号或密码错误')
    expect(store.isLoggedIn).toBe(false)
  })

  it('clearUser 清空登录态（内部辅助，经 logout 路径验证）', async () => {
    loginApi.mockResolvedValue({ data: { id: 1, userName: 'alice', role: 'USER' } })
    logoutApi.mockResolvedValue({})
    const store = useUserStore()
    await store.login('alice', 'pwd')
    expect(store.isLoggedIn).toBe(true)
    await store.logout()
    expect(store.isLoggedIn).toBe(false)
    expect(store.userId).toBeNull()
    expect(store.userName).toBe('')
  })

  it('logout 失败仍清空本地登录态', async () => {
    loginApi.mockResolvedValue({ data: { id: 1, userName: 'alice' } })
    logoutApi.mockRejectedValue(new Error('网络错误'))
    const store = useUserStore()
    await store.login('alice', 'pwd')
    await store.logout()
    expect(store.isLoggedIn).toBe(false)
  })

  it('登出连带清空购物车本地状态（P2-15 防跨用户残留）', async () => {
    loginApi.mockResolvedValue({ data: { id: 1, userName: 'alice', role: 'USER' } })
    logoutApi.mockResolvedValue({})
    const store = useUserStore()
    const cartStore = useCartStore()
    await store.login('alice', 'pwd')
    cartStore.items = [{ id: 1, selected: true }]
    cartStore.count = 1
    await store.logout()
    expect(cartStore.items).toHaveLength(0)
    expect(cartStore.count).toBe(0)
  })

  it('isMerchant 依角色判定', async () => {
    loginApi.mockResolvedValue({ data: { id: 3, userName: 'shop', role: 'MERCHANT' } })
    const store = useUserStore()
    await store.login('shop', 'pwd')
    expect(store.isMerchant).toBe(true)
    expect(store.isAdmin).toBe(false)
  })
})

describe('checkLogin 会话恢复（P0-4）', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('会话有效时恢复登录态', async () => {
    checkLoginApi.mockResolvedValue({ data: { id: 1, userName: 'alice', role: 'USER' } })
    const store = useUserStore()
    await store.checkLogin()
    expect(store.isLoggedIn).toBe(true)
    expect(store.userName).toBe('alice')
  })

  it('会话无效时清空登录态', async () => {
    checkLoginApi.mockResolvedValue({ data: null })
    const store = useUserStore()
    await store.checkLogin()
    expect(store.isLoggedIn).toBe(false)
  })

  it('接口异常时清空登录态（fail-closed）', async () => {
    checkLoginApi.mockRejectedValue(new Error('401'))
    const store = useUserStore()
    await store.checkLogin()
    expect(store.isLoggedIn).toBe(false)
  })

  it('并发调用只发一次请求（Promise 缓存合并）', async () => {
    checkLoginApi.mockResolvedValue({ data: { id: 1, userName: 'alice', role: 'USER' } })
    const store = useUserStore()
    await Promise.all([store.checkLogin(), store.checkLogin(), store.checkLogin()])
    expect(checkLoginApi).toHaveBeenCalledTimes(1)
    expect(store.isLoggedIn).toBe(true)
  })

  it('串行调用各自发请求（缓存已释放）', async () => {
    checkLoginApi.mockResolvedValue({ data: null })
    const store = useUserStore()
    await store.checkLogin()
    await store.checkLogin()
    expect(checkLoginApi).toHaveBeenCalledTimes(2)
  })
})
