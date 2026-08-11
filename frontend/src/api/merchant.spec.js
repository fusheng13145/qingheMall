import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import {
  getMerchantInfo,
  applyMerchant,
  listMerchantProducts,
  saveMerchantProduct,
  toggleMerchantProduct,
  listMerchantOrders,
  merchantShip,
  merchantAdvanceLogistics,
  merchantProcessRefund,
  getMerchantStats
} from './merchant'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/merchant', () => {
  it('getMerchantInfo 查询店铺信息与入驻状态并透传返回值', async () => {
    request.get.mockResolvedValue({ data: { id: 'm1', status: 'ACTIVE' } })
    const res = await getMerchantInfo()

    expect(request.get).toHaveBeenCalledWith('/merchant/info')
    expect(res).toEqual({ data: { id: 'm1', status: 'ACTIVE' } })
  })

  it('applyMerchant 以表单提交店铺名（选填项为空不携带）', async () => {
    request.post.mockResolvedValue({ data: { id: 'm1' } })
    const res = await applyMerchant('青禾小铺')

    expect(request.post).toHaveBeenCalledTimes(1)
    const [url, params] = request.post.mock.calls[0]
    expect(url).toBe('/merchant/apply')
    expect(params).toBeInstanceOf(URLSearchParams)
    expect(params.get('shopName')).toBe('青禾小铺')
    expect(params.get('shopLogo')).toBeNull()
    expect(params.get('shopDesc')).toBeNull()
    expect(res).toEqual({ data: { id: 'm1' } })
  })

  it('applyMerchant 携带选填的 logo 与简介', async () => {
    request.post.mockResolvedValue({ data: {} })
    await applyMerchant('青禾小铺', '/uploads/logo.jpg', '好物集合')

    const params = request.post.mock.calls[0][1]
    expect(params.get('shopLogo')).toBe('/uploads/logo.jpg')
    expect(params.get('shopDesc')).toBe('好物集合')
  })

  it('listMerchantProducts 默认分页参数', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    const res = await listMerchantProducts()

    expect(request.get).toHaveBeenCalledWith('/merchant/products', {
      params: { pageNum: 1, pageSize: 10, keyword: '', status: '' }
    })
    expect(res).toEqual({ data: { data: [] } })
  })

  it('listMerchantProducts 携带筛选参数', async () => {
    request.get.mockResolvedValue({ data: {} })
    await listMerchantProducts(2, 5, '青禾', 'ON')

    expect(request.get).toHaveBeenCalledWith('/merchant/products', {
      params: { pageNum: 2, pageSize: 5, keyword: '青禾', status: 'ON' }
    })
  })

  it('saveMerchantProduct 以请求体保存商品', async () => {
    const data = { name: '青禾 Tee', price: 99 }
    request.post.mockResolvedValue({ code: 200 })
    const res = await saveMerchantProduct(data)

    expect(request.post).toHaveBeenCalledWith('/merchant/product/save', data)
    expect(res).toEqual({ code: 200 })
  })

  it('toggleMerchantProduct 以 query 参数上下架', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await toggleMerchantProduct(8, 'OFF')

    expect(request.post).toHaveBeenCalledWith('/merchant/product/toggle', null, {
      params: { productId: 8, status: 'OFF' }
    })
  })

  it('listMerchantOrders 默认分页参数', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    const res = await listMerchantOrders()

    expect(request.get).toHaveBeenCalledWith('/merchant/orders', {
      params: { pageNum: 1, pageSize: 10, status: '' }
    })
    expect(res).toEqual({ data: { data: [] } })
  })

  it('merchantShip 不带物流参数时仅携带订单号（向后兼容纯发货）', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await merchantShip('QH1')

    expect(request.post).toHaveBeenCalledWith('/merchant/order/ship', null, { params: { orderNumber: 'QH1' } })
  })

  it('merchantShip 携带承运商与运单号建立物流档案', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await merchantShip('QH1', '顺丰速运', 'SF1234567890')

    expect(request.post).toHaveBeenCalledWith('/merchant/order/ship', null, {
      params: { orderNumber: 'QH1', company: '顺丰速运', trackingNumber: 'SF1234567890' }
    })
  })

  it('merchantAdvanceLogistics 以 query 参数推进物流状态', async () => {
    request.post.mockResolvedValue({ data: { status: 'IN_TRANSIT' } })
    const res = await merchantAdvanceLogistics('QH1')

    expect(request.post).toHaveBeenCalledWith('/merchant/logistics/advance', null, { params: { orderNumber: 'QH1' } })
    expect(res).toEqual({ data: { status: 'IN_TRANSIT' } })
  })

  it('merchantProcessRefund 默认同意退款且不带审核意见', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await merchantProcessRefund('QH1')

    expect(request.post).toHaveBeenCalledWith('/merchant/order/refund/process', null, {
      params: { orderNumber: 'QH1', approve: true }
    })
  })

  it('merchantProcessRefund 可拒绝退款', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await merchantProcessRefund('QH1', false)

    expect(request.post).toHaveBeenCalledWith('/merchant/order/refund/process', null, {
      params: { orderNumber: 'QH1', approve: false }
    })
  })

  it('merchantProcessRefund 携带审核意见', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await merchantProcessRefund('QH1', false, '商品已拆封，不支持退款')

    expect(request.post).toHaveBeenCalledWith('/merchant/order/refund/process', null, {
      params: { orderNumber: 'QH1', approve: false, comment: '商品已拆封，不支持退款' }
    })
  })

  it('getMerchantStats 查询店铺统计并透传返回值', async () => {
    request.get.mockResolvedValue({ data: { productCount: 3 } })
    const res = await getMerchantStats()

    expect(request.get).toHaveBeenCalledWith('/merchant/stats')
    expect(res).toEqual({ data: { productCount: 3 } })
  })
})
