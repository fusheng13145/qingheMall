import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import { listMyRefunds, listOrderRefunds } from './refund'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/refund', () => {
  it('listMyRefunds 查询当前用户退款申请列表并透传返回值', async () => {
    const list = [
      { id: 1, orderNumber: 'QH1', type: 'REFUND_ONLY', status: 'PENDING', reason: '不想要了' }
    ]
    request.get.mockResolvedValue({ data: list })
    const res = await listMyRefunds()

    expect(request.get).toHaveBeenCalledWith('/refund/mine')
    expect(res).toEqual({ data: list })
  })

  it('listOrderRefunds 按订单号查询退款申请历史', async () => {
    request.get.mockResolvedValue({ data: [] })
    const res = await listOrderRefunds('QH2026001')

    expect(request.get).toHaveBeenCalledWith('/refund/order', { params: { orderNumber: 'QH2026001' } })
    expect(res).toEqual({ data: [] })
  })
})
