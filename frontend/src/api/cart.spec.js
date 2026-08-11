import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import {
  addCart,
  updateCartQuantity,
  updateCartSelected,
  removeCart,
  listCart,
  countCart,
  clearSelectedCart
} from './cart'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/cart', () => {
  it('addCart 以请求体提交规格 id 与数量', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const res = await addCart(10, 2)

    expect(request.post).toHaveBeenCalledWith('/cart/add', { productDetailId: 10, quantity: 2 })
    expect(res).toEqual({ code: 200 })
  })

  it('updateCartQuantity 修改条目数量', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await updateCartQuantity(3, 5)

    expect(request.post).toHaveBeenCalledWith('/cart/updateQuantity', { id: 3, quantity: 5 })
  })

  it('updateCartSelected 修改条目勾选状态', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await updateCartSelected(3, false)

    expect(request.post).toHaveBeenCalledWith('/cart/updateSelected', { id: 3, selected: false })
  })

  it('removeCart 以 query 参数删除条目', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await removeCart(3)

    expect(request.post).toHaveBeenCalledWith('/cart/remove', null, { params: { id: 3 } })
  })

  it('listCart 查询购物车列表并透传返回值', async () => {
    request.get.mockResolvedValue({ data: [{ id: 1 }] })
    const res = await listCart()

    expect(request.get).toHaveBeenCalledWith('/cart/list')
    expect(res).toEqual({ data: [{ id: 1 }] })
  })

  it('countCart 查询购物车条目数并透传返回值', async () => {
    request.get.mockResolvedValue({ data: 2 })
    const res = await countCart()

    expect(request.get).toHaveBeenCalledWith('/cart/count')
    expect(res).toEqual({ data: 2 })
  })

  it('clearSelectedCart 清空已勾选条目', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const res = await clearSelectedCart()

    expect(request.post).toHaveBeenCalledWith('/cart/clearSelected')
    expect(res).toEqual({ code: 200 })
  })
})
