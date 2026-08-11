import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import {
  addOrder,
  batchAddOrders,
  cancelOrder,
  listOrders,
  getOrder,
  confirmReceipt,
  applyRefund
} from './order'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/order', () => {
  it('addOrder 以请求体提交下单参数并透传返回值', async () => {
    const payload = { productDetailId: 1, quantity: 2, receiverName: '张三' }
    request.post.mockResolvedValue({ data: 'QH2026001' })
    const res = await addOrder(payload)

    expect(request.post).toHaveBeenCalledWith('/order/add', payload)
    expect(res).toEqual({ data: 'QH2026001' })
  })

  it('batchAddOrders 以数组请求体批量下单', async () => {
    const items = [{ productDetailId: 1, quantity: 1 }, { productDetailId: 2, quantity: 3 }]
    request.post.mockResolvedValue({ data: ['QH1', 'QH2'] })
    const res = await batchAddOrders(items)

    expect(request.post).toHaveBeenCalledWith('/order/batchAdd', items)
    expect(res).toEqual({ data: ['QH1', 'QH2'] })
  })

  it('cancelOrder 以 query 参数取消订单', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await cancelOrder('QH2026001')

    expect(request.post).toHaveBeenCalledWith('/order/cancel', null, { params: { orderNumber: 'QH2026001' } })
  })

  it('listOrders 默认分页参数且不带 status', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    const res = await listOrders(null)

    expect(request.get).toHaveBeenCalledTimes(1)
    const [url, config] = request.get.mock.calls[0]
    expect(url).toBe('/order/list')
    expect(config.params.pageNum).toBe(1)
    expect(config.params.pageSize).toBe(10)
    expect('status' in config.params).toBe(false)
    expect(res).toEqual({ data: { data: [] } })
  })

  it('listOrders 携带状态筛选与自定义分页', async () => {
    request.get.mockResolvedValue({ data: {} })
    await listOrders('WAIT_BUYER_PAY', 2, 20)

    expect(request.get).toHaveBeenCalledWith('/order/list', {
      params: { pageNum: 2, pageSize: 20, status: 'WAIT_BUYER_PAY' }
    })
  })

  it('getOrder 按订单号查询详情', async () => {
    request.get.mockResolvedValue({ data: { orderNumber: 'QH2026001' } })
    const res = await getOrder('QH2026001')

    expect(request.get).toHaveBeenCalledWith('/order/get', { params: { orderNumber: 'QH2026001' } })
    expect(res).toEqual({ data: { orderNumber: 'QH2026001' } })
  })

  it('confirmReceipt 以 query 参数确认收货', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await confirmReceipt('QH2026001')

    expect(request.post).toHaveBeenCalledWith('/order/confirmReceipt', null, { params: { orderNumber: 'QH2026001' } })
  })

  it('applyRefund 携带原因申请退款且不传 type 时不携带类型参数', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await applyRefund('QH2026001', '不想要了')

    expect(request.post).toHaveBeenCalledWith('/order/refund/apply', null, {
      params: { orderNumber: 'QH2026001', reason: '不想要了' }
    })
  })

  it('applyRefund 显式指定退货退款类型', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await applyRefund('QH2026001', '质量问题', 'RETURN_REFUND')

    expect(request.post).toHaveBeenCalledWith('/order/refund/apply', null, {
      params: { orderNumber: 'QH2026001', reason: '质量问题', type: 'RETURN_REFUND' }
    })
  })
})
