import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import { reg, loginApi, logoutApi, checkLoginApi, updateProfileApi } from './user'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/user', () => {
  it('reg 以表单提交注册参数（默认 USER 角色，不带 shopName）', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const res = await reg('alice', '123456')

    expect(request.post).toHaveBeenCalledTimes(1)
    const [url, params] = request.post.mock.calls[0]
    expect(url).toBe('/user/reg')
    expect(params).toBeInstanceOf(URLSearchParams)
    expect(params.get('userName')).toBe('alice')
    expect(params.get('pwd')).toBe('123456')
    expect(params.get('role')).toBe('USER')
    expect(params.get('shopName')).toBeNull()
    expect(res).toEqual({ code: 200 })
  })

  it('reg 商家注册附带 MERCHANT 角色与 shopName', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await reg('bob', '654321', 'MERCHANT', '青禾小铺')

    const params = request.post.mock.calls[0][1]
    expect(params.get('role')).toBe('MERCHANT')
    expect(params.get('shopName')).toBe('青禾小铺')
  })

  it('loginApi 以表单提交登录参数并透传返回值', async () => {
    request.post.mockResolvedValue({ code: 200, data: { id: 1 } })
    const res = await loginApi('alice', '123456')

    expect(request.post).toHaveBeenCalledTimes(1)
    const [url, params] = request.post.mock.calls[0]
    expect(url).toBe('/user/login')
    expect(params).toBeInstanceOf(URLSearchParams)
    expect(params.get('userName')).toBe('alice')
    expect(params.get('pwd')).toBe('123456')
    expect(res).toEqual({ code: 200, data: { id: 1 } })
  })

  it('logoutApi 调用登出接口并透传返回值', async () => {
    request.get.mockResolvedValue({ code: 200 })
    const res = await logoutApi()

    expect(request.get).toHaveBeenCalledWith('/user/logout')
    expect(res).toEqual({ code: 200 })
  })

  it('checkLoginApi 调用登录态校验接口并透传返回值', async () => {
    request.get.mockResolvedValue({ code: 200, data: { id: 1 } })
    const res = await checkLoginApi()

    expect(request.get).toHaveBeenCalledWith('/user/checkLogin')
    expect(res).toEqual({ code: 200, data: { id: 1 } })
  })

  it('updateProfileApi 以 query 参数提交昵称与头像', async () => {
    request.post.mockResolvedValue({ code: 200, data: {} })
    const res = await updateProfileApi('小禾', '/uploads/a.jpg')

    expect(request.post).toHaveBeenCalledWith('/user/updateProfile', null, {
      params: { nickName: '小禾', avatar: '/uploads/a.jpg' }
    })
    expect(res).toEqual({ code: 200, data: {} })
  })
})
