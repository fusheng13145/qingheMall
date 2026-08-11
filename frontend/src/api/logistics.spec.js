import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import { trackLogistics } from './logistics'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/logistics', () => {
  it('trackLogistics 按订单号查询物流轨迹并透传返回值', async () => {
    const logistics = {
      orderNumber: 'QH2026001',
      company: '顺丰速运',
      trackingNumber: 'SF1234567890',
      status: 'IN_TRANSIT',
      traces: [{ description: '包裹运输中，正发往收货地址', traceTime: '2026-08-01T10:00:00' }]
    }
    request.get.mockResolvedValue({ data: logistics })
    const res = await trackLogistics('QH2026001')

    expect(request.get).toHaveBeenCalledWith('/logistics/track', { params: { orderNumber: 'QH2026001' } })
    expect(res).toEqual({ data: logistics })
  })

  it('trackLogistics 未发货订单返回空数据时原样透传', async () => {
    request.get.mockResolvedValue({ data: null })
    const res = await trackLogistics('QH2026002')

    expect(request.get).toHaveBeenCalledTimes(1)
    expect(res).toEqual({ data: null })
  })
})
