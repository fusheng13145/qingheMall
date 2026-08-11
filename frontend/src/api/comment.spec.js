import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import { addComment, listProductComments, getCommentSummary, getOrderCommentStatus } from './comment'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/comment', () => {
  it('addComment 以请求体提交评价并透传返回值', async () => {
    const payload = { productId: 1, orderNumber: 'QH1', rating: 5, content: '好评' }
    request.post.mockResolvedValue({ code: 200 })
    const res = await addComment(payload)

    expect(request.post).toHaveBeenCalledWith('/comment/add', payload)
    expect(res).toEqual({ code: 200 })
  })

  it('listProductComments 默认分页参数', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    const res = await listProductComments(6)

    expect(request.get).toHaveBeenCalledWith('/comment/product', {
      params: { productId: 6, pageNum: 1, pageSize: 10 }
    })
    expect(res).toEqual({ data: { data: [] } })
  })

  it('listProductComments 自定义分页参数', async () => {
    request.get.mockResolvedValue({ data: {} })
    await listProductComments(6, 3, 5)

    expect(request.get).toHaveBeenCalledWith('/comment/product', {
      params: { productId: 6, pageNum: 3, pageSize: 5 }
    })
  })

  it('getCommentSummary 查询评分汇总并透传返回值', async () => {
    request.get.mockResolvedValue({ data: { avgRating: 4.8, ratingCount: 10 } })
    const res = await getCommentSummary(6)

    expect(request.get).toHaveBeenCalledWith('/comment/summary', { params: { productId: 6 } })
    expect(res).toEqual({ data: { avgRating: 4.8, ratingCount: 10 } })
  })

  it('getOrderCommentStatus 查询订单评价状态', async () => {
    request.get.mockResolvedValue({ data: { commented: true } })
    const res = await getOrderCommentStatus('QH1')

    expect(request.get).toHaveBeenCalledWith('/comment/orderStatus', { params: { orderNumber: 'QH1' } })
    expect(res).toEqual({ data: { commented: true } })
  })
})
