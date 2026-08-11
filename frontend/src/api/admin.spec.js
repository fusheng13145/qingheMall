import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import {
  getDashboard,
  getSalesReport,
  getProductList,
  addProduct,
  updateProduct,
  deleteProduct,
  getOrderList,
  updateOrderStatus,
  shipOrder,
  processRefund,
  getUserList,
  updateUserRole,
  createCoupon,
  listCoupons,
  updateCoupon,
  toggleCoupon,
  createSeckill,
  listSeckills,
  toggleSeckill,
  listMerchants,
  auditMerchant
} from './admin'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/admin', () => {
  it('getDashboard 查询数据看板并透传返回值', async () => {
    request.get.mockResolvedValue({ data: { productCount: 1 } })
    const res = await getDashboard()

    expect(request.get).toHaveBeenCalledWith('/admin/dashboard')
    expect(res).toEqual({ data: { productCount: 1 } })
  })

  it('getSalesReport 默认查询 7 天日报', async () => {
    request.get.mockResolvedValue({ data: [] })
    const res = await getSalesReport()

    expect(request.get).toHaveBeenCalledWith('/admin/report', { params: { days: 7 } })
    expect(res).toEqual({ data: [] })
  })

  it('getSalesReport 自定义天数', async () => {
    request.get.mockResolvedValue({ data: [] })
    await getSalesReport(30)

    expect(request.get).toHaveBeenCalledWith('/admin/report', { params: { days: 30 } })
  })

  it('getProductList 默认分页参数', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    const res = await getProductList()

    expect(request.get).toHaveBeenCalledWith('/admin/product/list', {
      params: { pageNum: 1, pageSize: 20, keyword: '' }
    })
    expect(res).toEqual({ data: { data: [] } })
  })

  it('getProductList 携带关键字与分页', async () => {
    request.get.mockResolvedValue({ data: {} })
    await getProductList(2, 10, '青禾')

    expect(request.get).toHaveBeenCalledWith('/admin/product/list', {
      params: { pageNum: 2, pageSize: 10, keyword: '青禾' }
    })
  })

  it('addProduct 以请求体新增商品并透传返回值', async () => {
    const data = { name: '青禾 Tee', price: 99 }
    request.post.mockResolvedValue({ code: 200 })
    const res = await addProduct(data)

    expect(request.post).toHaveBeenCalledWith('/admin/product/add', data)
    expect(res).toEqual({ code: 200 })
  })

  it('updateProduct 以请求体更新商品', async () => {
    const data = { id: 1, price: 88 }
    request.post.mockResolvedValue({ code: 200 })
    await updateProduct(data)

    expect(request.post).toHaveBeenCalledWith('/admin/product/update', data)
  })

  it('deleteProduct 以 query 参数删除商品', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await deleteProduct(7)

    expect(request.post).toHaveBeenCalledWith('/admin/product/delete', null, { params: { id: 7 } })
  })

  it('getOrderList 默认分页参数', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    const res = await getOrderList()

    expect(request.get).toHaveBeenCalledWith('/admin/order/list', {
      params: { pageNum: 1, pageSize: 20, status: '' }
    })
    expect(res).toEqual({ data: { data: [] } })
  })

  it('getOrderList 携带状态筛选', async () => {
    request.get.mockResolvedValue({ data: {} })
    await getOrderList(1, 20, 'TRADE_PAID_SUCCESS')

    expect(request.get).toHaveBeenCalledWith('/admin/order/list', {
      params: { pageNum: 1, pageSize: 20, status: 'TRADE_PAID_SUCCESS' }
    })
  })

  it('updateOrderStatus 以 query 参数更新订单状态', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await updateOrderStatus('QH1', 'TRADE_SHIPPED')

    expect(request.post).toHaveBeenCalledWith('/admin/order/updateStatus', null, {
      params: { orderNumber: 'QH1', status: 'TRADE_SHIPPED' }
    })
  })

  it('shipOrder 以 query 参数发货', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await shipOrder('QH1')

    expect(request.post).toHaveBeenCalledWith('/admin/order/ship', null, { params: { orderNumber: 'QH1' } })
  })

  it('processRefund 默认同意退款', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await processRefund('QH1')

    expect(request.post).toHaveBeenCalledWith('/admin/order/refund/process', null, {
      params: { orderNumber: 'QH1', approve: true }
    })
  })

  it('processRefund 可拒绝退款', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await processRefund('QH1', false)

    expect(request.post).toHaveBeenCalledWith('/admin/order/refund/process', null, {
      params: { orderNumber: 'QH1', approve: false }
    })
  })

  it('getUserList 默认分页参数并透传返回值', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    const res = await getUserList()

    expect(request.get).toHaveBeenCalledWith('/admin/user/list', { params: { pageNum: 1, pageSize: 20 } })
    expect(res).toEqual({ data: { data: [] } })
  })

  it('updateUserRole 以 query 参数更新用户角色', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await updateUserRole(3, 'ADMIN')

    expect(request.post).toHaveBeenCalledWith('/admin/user/updateRole', null, { params: { id: 3, role: 'ADMIN' } })
  })

  it('createCoupon 以请求体创建券模板', async () => {
    const data = { name: '新人券', type: 'FULL_REDUCTION' }
    request.post.mockResolvedValue({ code: 200 })
    const res = await createCoupon(data)

    expect(request.post).toHaveBeenCalledWith('/admin/coupon/create', data)
    expect(res).toEqual({ code: 200 })
  })

  it('listCoupons 默认分页参数', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    const res = await listCoupons()

    expect(request.get).toHaveBeenCalledWith('/admin/coupon/list', { params: { pageNum: 1, pageSize: 10 } })
    expect(res).toEqual({ data: { data: [] } })
  })

  it('updateCoupon 以请求体修改券', async () => {
    const data = { id: 'c1', name: '改名券' }
    request.post.mockResolvedValue({ code: 200 })
    await updateCoupon(data)

    expect(request.post).toHaveBeenCalledWith('/admin/coupon/update', data)
  })

  it('toggleCoupon 以 query 参数上下架券', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await toggleCoupon('c1', 'OFF')

    expect(request.post).toHaveBeenCalledWith('/admin/coupon/toggle', null, {
      params: { couponId: 'c1', status: 'OFF' }
    })
  })

  it('createSeckill 以请求体创建秒杀活动', async () => {
    const data = { productId: 1, seckillPrice: 9.9 }
    request.post.mockResolvedValue({ code: 200 })
    const res = await createSeckill(data)

    expect(request.post).toHaveBeenCalledWith('/admin/seckill/create', data)
    expect(res).toEqual({ code: 200 })
  })

  it('listSeckills 默认分页参数', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    const res = await listSeckills()

    expect(request.get).toHaveBeenCalledWith('/admin/seckill/list', {
      params: { pageNum: 1, pageSize: 10, status: '' }
    })
    expect(res).toEqual({ data: { data: [] } })
  })

  it('toggleSeckill 以 query 参数切换活动状态', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await toggleSeckill('a1', 'CLOSED')

    expect(request.post).toHaveBeenCalledWith('/admin/seckill/toggle', null, {
      params: { activityId: 'a1', status: 'CLOSED' }
    })
  })

  it('listMerchants 默认分页参数', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    const res = await listMerchants()

    expect(request.get).toHaveBeenCalledWith('/admin/merchant/list', {
      params: { pageNum: 1, pageSize: 10, status: '' }
    })
    expect(res).toEqual({ data: { data: [] } })
  })

  it('auditMerchant 默认通过审核', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await auditMerchant('m1')

    expect(request.post).toHaveBeenCalledWith('/admin/merchant/audit', null, {
      params: { merchantId: 'm1', approve: true, reason: '' }
    })
  })

  it('auditMerchant 驳回并携带原因', async () => {
    request.post.mockResolvedValue({ code: 200 })
    await auditMerchant('m1', false, '资料不全')

    expect(request.post).toHaveBeenCalledWith('/admin/merchant/audit', null, {
      params: { merchantId: 'm1', approve: false, reason: '资料不全' }
    })
  })
})
