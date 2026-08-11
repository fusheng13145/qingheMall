import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import { claimCoupon, myCoupons, listCoupons, availableCoupons } from './coupon'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/coupon', () => {
  it('claimCoupon 以 query 参数领取券并透传返回值', async () => {
    request.post.mockResolvedValue({ code: 200 })
    const res = await claimCoupon('c1')

    expect(request.post).toHaveBeenCalledWith('/coupon/claim', null, { params: { couponId: 'c1' } })
    expect(res).toEqual({ code: 200 })
  })

  it('myCoupons 缺省查询全部状态（空串）', async () => {
    request.get.mockResolvedValue({ data: [] })
    const res = await myCoupons()

    expect(request.get).toHaveBeenCalledWith('/coupon/mine', { params: { status: '' } })
    expect(res).toEqual({ data: [] })
  })

  it('myCoupons 按状态查询', async () => {
    request.get.mockResolvedValue({ data: [] })
    await myCoupons('UNUSED')

    expect(request.get).toHaveBeenCalledWith('/coupon/mine', { params: { status: 'UNUSED' } })
  })

  it('listCoupons 查询领券中心列表并透传返回值', async () => {
    request.get.mockResolvedValue({ data: [{ id: 'c1' }] })
    const res = await listCoupons()

    expect(request.get).toHaveBeenCalledWith('/coupon/list')
    expect(res).toEqual({ data: [{ id: 'c1' }] })
  })

  it('availableCoupons 按订单金额查询可用券', async () => {
    request.get.mockResolvedValue({ data: [{ id: 'c1' }] })
    const res = await availableCoupons(199.5)

    expect(request.get).toHaveBeenCalledWith('/coupon/available', { params: { amount: 199.5 } })
    expect(res).toEqual({ data: [{ id: 'c1' }] })
  })
})
