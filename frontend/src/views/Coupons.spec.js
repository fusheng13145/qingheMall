import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Coupons from './Coupons.vue'
import { toasts } from '../utils/toast'

const { listCoupons, claimCoupon, myCoupons } = vi.hoisted(() => ({
  listCoupons: vi.fn(),
  claimCoupon: vi.fn(),
  myCoupons: vi.fn()
}))

vi.mock('../api/coupon', () => ({ listCoupons, claimCoupon, myCoupons }))

// 字段与后端 CouponDO 一致（type/amount/discount/threshold）
function makeCoupon(overrides = {}) {
  return {
    id: 'c1',
    name: '新人满减券',
    type: 'FULL_REDUCTION',
    threshold: 100,
    amount: 20,
    discount: null,
    total: 100,
    issued: 5,
    startTime: '2026-01-01T00:00:00',
    endTime: '2026-12-31T23:59:59',
    ...overrides
  }
}

function expectToast(type, message) {
  expect(toasts.some(t => t.type === type && t.message === message)).toBe(true)
}

beforeEach(() => {
  vi.clearAllMocks()
  toasts.splice(0, toasts.length)
})

describe('Coupons 领券中心页', () => {
  it('加载并渲染满减券与折扣券', async () => {
    listCoupons.mockResolvedValue({
      data: [
        makeCoupon(),
        makeCoupon({ id: 'c2', name: '限时折扣券', type: 'DISCOUNT', discount: 0.85, amount: 0 })
      ]
    })
    myCoupons.mockResolvedValue({ data: [] })
    const wrapper = mount(Coupons)
    await flushPromises()

    expect(listCoupons).toHaveBeenCalledTimes(1)
    expect(myCoupons).toHaveBeenCalledWith('UNUSED')
    expect(wrapper.text()).toContain('新人满减券')
    expect(wrapper.text()).toContain('¥')
    expect(wrapper.text()).toContain('20')
    expect(wrapper.text()).toContain('限时折扣券')
    expect(wrapper.text()).toContain('8.5')
    expect(wrapper.text()).toContain('折')
    expect(wrapper.text()).toContain('已领 5/100')
    expect(wrapper.text()).toContain('立即领取')
  })

  it('已领取的券按钮置为「已领取」且禁用', async () => {
    listCoupons.mockResolvedValue({ data: [makeCoupon()] })
    myCoupons.mockResolvedValue({ data: [{ couponId: 'c1' }] })
    const wrapper = mount(Coupons)
    await flushPromises()

    const btn = wrapper.find('.btn-claim')
    expect(btn.text()).toBe('已领取')
    expect(btn.attributes('disabled')).toBeDefined()
    expect(wrapper.find('.coupon-card').classes()).toContain('disabled')
  })

  it('已领完的券按钮置为「已领完」且禁用', async () => {
    listCoupons.mockResolvedValue({ data: [makeCoupon({ issued: 100, total: 100 })] })
    myCoupons.mockResolvedValue({ data: [] })
    const wrapper = mount(Coupons)
    await flushPromises()

    const btn = wrapper.find('.btn-claim')
    expect(btn.text()).toBe('已领完')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('空列表显示空态', async () => {
    listCoupons.mockResolvedValue({ data: [] })
    myCoupons.mockResolvedValue({ data: [] })
    const wrapper = mount(Coupons)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无可用优惠券')
  })

  it('加载失败 toast 提示', async () => {
    listCoupons.mockRejectedValue(new Error('net'))
    mount(Coupons)
    await flushPromises()

    expectToast('error', '加载优惠券失败：net')
  })

  it('领取成功后提示并更新按钮与已领数', async () => {
    listCoupons.mockResolvedValue({ data: [makeCoupon()] })
    myCoupons.mockResolvedValue({ data: [] })
    claimCoupon.mockResolvedValue({})
    const wrapper = mount(Coupons)
    await flushPromises()

    await wrapper.find('.btn-claim').trigger('click')
    await flushPromises()

    expect(claimCoupon).toHaveBeenCalledWith('c1')
    expectToast('success', '领取成功，可在「我的优惠券」中查看')
    expect(wrapper.find('.btn-claim').text()).toBe('已领取')
    expect(wrapper.text()).toContain('已领 6/100')
  })

  it('领取失败 toast 提示', async () => {
    listCoupons.mockResolvedValue({ data: [makeCoupon()] })
    myCoupons.mockResolvedValue({ data: [] })
    claimCoupon.mockRejectedValue(new Error('库存不足'))
    const wrapper = mount(Coupons)
    await flushPromises()

    await wrapper.find('.btn-claim').trigger('click')
    await flushPromises()

    expectToast('error', '领取失败：库存不足')
  })

  it('myCoupons 查询失败不影响券列表展示', async () => {
    listCoupons.mockResolvedValue({ data: [makeCoupon()] })
    myCoupons.mockRejectedValue(new Error('net'))
    const wrapper = mount(Coupons)
    await flushPromises()

    expect(wrapper.text()).toContain('新人满减券')
    expect(wrapper.find('.btn-claim').text()).toBe('立即领取')
  })
})
