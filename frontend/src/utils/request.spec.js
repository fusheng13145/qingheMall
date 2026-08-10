import { describe, it, expect, vi, beforeEach } from 'vitest'

const { mockStore, mockPush, mockCurrentRoute } = vi.hoisted(() => ({
  mockStore: {
    isLoggedIn: false,
    userId: null,
    userName: '',
    nickName: '',
    role: ''
  },
  mockPush: vi.fn(),
  mockCurrentRoute: { value: { name: 'Cart', fullPath: '/cart' } }
}))

vi.mock('../stores/user', () => ({
  useUserStore: () => mockStore
}))
vi.mock('../router', () => ({
  default: {
    currentRoute: mockCurrentRoute,
    push: mockPush
  }
}))

import request from './request'

const { fulfilled, rejected } = request.interceptors.response.handlers[0]

beforeEach(() => {
  Object.assign(mockStore, { isLoggedIn: true, userId: 1, userName: 'a', nickName: 'a', role: 'USER' })
  mockPush.mockClear()
  mockCurrentRoute.value = { name: 'Cart', fullPath: '/cart' }
})

describe('request 响应拦截器：业务结果解析', () => {
  it('code 200 直接返回 Result', () => {
    const res = fulfilled({ data: { code: 200, data: { id: 1 } } })
    expect(res.code).toBe(200)
    expect(res.data.id).toBe(1)
  })

  it('业务失败 reject Error 并携带 code/data', async () => {
    await expect(fulfilled({ data: { code: 400, message: '库存不足', data: null } }))
      .rejects.toMatchObject({ code: 400, data: null, message: '库存不足' })
  })

  it('非 Result 结构原样透传', () => {
    const res = fulfilled({ data: 'plain' })
    expect(res).toBe('plain')
  })
})

describe('request 响应拦截器：401 处理', () => {
  it('清空登录态并跳转登录页（带 redirect）', async () => {
    const error = { response: { status: 401, data: { message: '未登录' } } }
    await expect(rejected(error)).rejects.toMatchObject({ handled: true, message: '未登录' })
    expect(mockStore.isLoggedIn).toBe(false)
    expect(mockStore.userId).toBeNull()
    expect(mockPush).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/cart' } })
  })

  it('已在登录页时不重复跳转', async () => {
    mockCurrentRoute.value = { name: 'Login', fullPath: '/login' }
    const error = { response: { status: 401, data: {} } }
    await expect(rejected(error)).rejects.toMatchObject({ handled: true })
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('无服务端 message 时回退默认文案', async () => {
    const error = { response: { status: 401, data: {} } }
    await expect(rejected(error)).rejects.toMatchObject({ message: '登录已过期，请重新登录' })
  })
})

describe('request 响应拦截器：403 处理', () => {
  it('仅 reject 不跳转、不置 handled', async () => {
    const error = { response: { status: 403, data: { message: '无管理员权限' } } }
    await expect(rejected(error)).rejects.toMatchObject({ code: 403, message: '无管理员权限' })
    expect(mockPush).not.toHaveBeenCalled()
    expect(mockStore.isLoggedIn).toBe(true)
  })

  it('无服务端 message 回退默认', async () => {
    const error = { response: { status: 403, data: {} } }
    await expect(rejected(error)).rejects.toMatchObject({ message: '无权限访问' })
  })
})

describe('request 响应拦截器：网络错误', () => {
  it('无 response 的错误原样 reject', async () => {
    const error = { message: 'Network Error' }
    await expect(rejected(error)).rejects.toMatchObject({ message: 'Network Error' })
  })
})
