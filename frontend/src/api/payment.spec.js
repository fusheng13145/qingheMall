import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import { createPay, mockPay, queryPay } from './payment'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/payment', () => {
  it('createPay 以请求体提交订单号与支付方式并透传返回值', async () => {
    request.post.mockResolvedValue({ data: { channel: 'NATIVE', mock: true } })
    const res = await createPay('QH1', 'NATIVE')

    expect(request.post).toHaveBeenCalledWith('/pay/create', { orderNumber: 'QH1', payType: 'NATIVE' })
    expect(res).toEqual({ data: { channel: 'NATIVE', mock: true } })
  })

  it('mockPay 以请求体提交订单号', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const res = await mockPay('QH1')

    expect(request.post).toHaveBeenCalledWith('/pay/mockPay', { orderNumber: 'QH1' })
    expect(res).toEqual({ code: 200 })
  })

  it('queryPay 以 query 参数查询支付状态并透传返回值', async () => {
    request.get.mockResolvedValue({ data: 'TRADE_PAID_SUCCESS' })
    const res = await queryPay('QH1')

    expect(request.get).toHaveBeenCalledWith('/pay/query', { params: { orderNumber: 'QH1' } })
    expect(res).toEqual({ data: 'TRADE_PAID_SUCCESS' })
  })
})
