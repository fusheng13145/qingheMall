import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import {
  listAddress,
  getDefaultAddress,
  addAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress
} from './address'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/address', () => {
  it('listAddress 查询地址列表并透传返回值', async () => {
    request.get.mockResolvedValue({ data: [{ id: 1 }] })
    const res = await listAddress()

    expect(request.get).toHaveBeenCalledWith('/address/list')
    expect(res).toEqual({ data: [{ id: 1 }] })
  })

  it('getDefaultAddress 查询默认地址并透传返回值', async () => {
    request.get.mockResolvedValue({ data: { id: 1 } })
    const res = await getDefaultAddress()

    expect(request.get).toHaveBeenCalledWith('/address/default')
    expect(res).toEqual({ data: { id: 1 } })
  })

  it('addAddress 以请求体提交新增地址', async () => {
    const data = { receiverName: '张三', receiverPhone: '13800138000', receiverAddress: '杭州' }
    request.post.mockResolvedValue({ code: 200 })
    const res = await addAddress(data)

    expect(request.post).toHaveBeenCalledWith('/address/add', data)
    expect(res).toEqual({ code: 200 })
  })

  it('updateAddress 以请求体提交更新地址', async () => {
    const data = { id: 1, receiverName: '李四' }
    request.post.mockResolvedValue({ code: 200 })
    await updateAddress(data)

    expect(request.post).toHaveBeenCalledWith('/address/update', data)
  })

  it('deleteAddress 以 query 参数删除地址', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await deleteAddress(9)

    expect(request.post).toHaveBeenCalledWith('/address/delete', null, { params: { id: 9 } })
  })

  it('setDefaultAddress 以 query 参数设置默认地址', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await setDefaultAddress(9)

    expect(request.post).toHaveBeenCalledWith('/address/setDefault', null, { params: { id: 9 } })
  })
})
