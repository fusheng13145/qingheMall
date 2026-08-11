import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import MyCoupons from './MyCoupons.vue'
import { toasts } from '../utils/toast'

const { myCoupons } = vi.hoisted(() => ({ myCoupons: vi.fn() }))

vi.mock('../api/coupon', () => ({ myCoupons }))

function makeCoupon(overrides = {}) {
  return {
    id: 'uc1',
    couponName: '满100减20',
    status: 'UNUSED',
    couponType: 'FULL_REDUCTION',
    threshold: 100,
    couponAmount: 20,
    endTime: '2026-12-31T23:59:59',
    ...overrides
  }
}

beforeEach(() => {
  vi.clearAllMocks()
  toasts.splice(0, toasts.length)
})

describe('MyCoupons 我的优惠券页', () => {
  it('默认加载未使用券', async () => {
    myCoupons.mockResolvedValue({ data: [makeCoupon()] })
    const wrapper = mount(MyCoupons)
    await flushPromises()

    expect(myCoupons).toHaveBeenCalledWith('UNUSED')
    expect(wrapper.text()).toContain('满100减20')
    expect(wrapper.text()).toContain('未使用')
  })

  it('切换 tab 重新加载', async () => {
    myCoupons.mockResolvedValue({ data: [] })
    const wrapper = mount(MyCoupons)
    await flushPromises()
    myCoupons.mockClear()

    myCoupons.mockResolvedValue({ data: [makeCoupon({ status: 'USED' })] })
    await wrapper.findAll('.tab-btn')[1].trigger('click')
    await flushPromises()

    expect(myCoupons).toHaveBeenCalledWith('USED')
  })

  it('渲染券规则文案', async () => {
    myCoupons.mockResolvedValue({ data: [makeCoupon()] })
    const wrapper = mount(MyCoupons)
    await flushPromises()

    expect(wrapper.text()).toContain('满')
    expect(wrapper.text()).toContain('减')
  })

  it('空列表显示空态', async () => {
    myCoupons.mockResolvedValue({ data: [] })
    const wrapper = mount(MyCoupons)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无')
  })

  it('加载失败 toast 提示', async () => {
    myCoupons.mockRejectedValue(new Error('net'))
    mount(MyCoupons)
    await flushPromises()

    expect(toasts.some(t => t.type === 'error' && t.message.includes('加载失败'))).toBe(true)
  })
})
