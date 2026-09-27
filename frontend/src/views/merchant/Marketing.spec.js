import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Marketing from './Marketing.vue'
import {
  createMerchantCoupon,
  listMerchantCoupons,
  toggleMerchantCoupon,
  listMerchantSeckills,
  listMerchantProducts
} from '../../api/merchant'
import { getProductDetails } from '../../api/product'
import { toasts } from '../../utils/toast'

vi.mock('../../api/merchant', () => ({
  createMerchantCoupon: vi.fn(),
  listMerchantCoupons: vi.fn(),
  toggleMerchantCoupon: vi.fn(),
  createMerchantSeckill: vi.fn(),
  listMerchantSeckills: vi.fn(),
  toggleMerchantSeckill: vi.fn(),
  listMerchantProducts: vi.fn()
}))
vi.mock('../../api/product', () => ({ getProductDetails: vi.fn() }))

function couponPayload() {
  return {
    data: {
      data: [
        { id: 'C1', name: '满100减10', type: 'FULL_REDUCTION', threshold: 100, amount: 10, total: 50, issued: 5, status: 'ACTIVE' },
        { id: 'C2', name: '8折券', type: 'DISCOUNT', threshold: 0, discount: 0.8, total: 20, issued: 20, status: 'INACTIVE' }
      ]
    }
  }
}

function mountPage() {
  return mount(Marketing)
}

beforeEach(() => {
  vi.clearAllMocks()
  toasts.splice(0, toasts.length)
  listMerchantCoupons.mockResolvedValue({ data: { data: [] } })
  listMerchantSeckills.mockResolvedValue({ data: { data: [] } })
  listMerchantProducts.mockResolvedValue({ data: { data: [] } })
  getProductDetails.mockResolvedValue({ data: [] })
})

/** 断言 toast 队列中存在指定类型与文案 */
function expectToast(type, message) {
  expect(toasts.some(t => t.type === type && t.message === message)).toBe(true)
}

describe('merchant/Marketing 营销管理（#39）', () => {
  it('挂载加载券与秒杀两列表，默认优惠券页签', async () => {
    listMerchantCoupons.mockResolvedValue(couponPayload())
    const wrapper = mountPage()
    await flushPromises()

    expect(listMerchantCoupons).toHaveBeenCalled()
    expect(listMerchantSeckills).toHaveBeenCalled()
    expect(wrapper.text()).toContain('满100减10')
    expect(wrapper.findAll('.tab')).toHaveLength(2)
  })

  it('券表单校验：缺名称拦截', async () => {
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.findAll('.btn-create')[0].trigger('click')
    expect(createMerchantCoupon).not.toHaveBeenCalled()
    expectToast('warning', '请填写券名称')
  })

  it('表单预填默认时间窗（now → +7 天），仅填名称即可创建', async () => {
    createMerchantCoupon.mockResolvedValue({ data: { id: 'C9' } })
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[type="text"]').setValue('新客立减券')
    await wrapper.findAll('.btn-create')[0].trigger('click')
    await flushPromises()

    const payload = createMerchantCoupon.mock.calls[0][0]
    const start = new Date(payload.startTime.replace(' ', 'T'))
    const end = new Date(payload.endTime.replace(' ', 'T'))
    // 默认窗口：开始≈当前时间，结束≈7 天后
    expect(start.getTime()).toBeGreaterThan(Date.now() - 60_000)
    expect(end.getTime() - start.getTime()).toBeGreaterThan(6.9 * 24 * 3600 * 1000)
    expectToast('success', '创建成功')
  })

  it('券表单校验：结束时间早于开始时间拦截', async () => {
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[type="text"]').setValue('新客立减券')
    const timeInputs = wrapper.findAll('input[type="datetime-local"]')
    await timeInputs[0].setValue('2026-10-01T10:00')
    await timeInputs[1].setValue('2026-09-01T10:00')
    await wrapper.findAll('.btn-create')[0].trigger('click')
    expect(createMerchantCoupon).not.toHaveBeenCalled()
    if (!toasts.some(t => t.message.includes('结束时间须晚于开始时间'))) {
      console.log('toasts dump:', JSON.stringify(toasts.map(t => t.message)))
      console.log('time values:', JSON.stringify([timeInputs[0].element.value, timeInputs[1].element.value]))
    }
    expect(toasts.some(t => t.message.includes('结束时间须晚于开始时间'))).toBe(true)
  })

  it('创建合法券：payload 含中性 discount 与 ACTIVE 状态，成功后刷新', async () => {
    createMerchantCoupon.mockResolvedValue({ data: { id: 'C9' } })
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input').setValue('新客立减券')
    const timeInputs = wrapper.findAll('input[type="datetime-local"]')
    await timeInputs[0].setValue('2026-10-01T10:00')
    await timeInputs[1].setValue('2026-11-01T10:00')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expect(createMerchantCoupon).toHaveBeenCalledTimes(1)
    const payload = createMerchantCoupon.mock.calls[0][0]
    expect(payload.name).toBe('新客立减券')
    expect(payload.type).toBe('FULL_REDUCTION')
    expect(payload.status).toBe('ACTIVE')
    expect(Number(payload.discount)).toBe(1)
    expectToast('success', '创建成功')
  })

  it('券上下架切换状态并刷新列表', async () => {
    listMerchantCoupons.mockResolvedValue(couponPayload())
    toggleMerchantCoupon.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.findAll('.btn-toggle')[0].trigger('click')
    await flushPromises()

    expect(toggleMerchantCoupon).toHaveBeenCalledWith('C1', 'INACTIVE')
  })

  it('切到秒杀页签渲染秒杀面板', async () => {
    listMerchantSeckills.mockResolvedValue({
      data: { data: [{ id: 'S1', productName: '秒杀鞋', seckillPrice: 99, totalStock: 10, remainStock: 8, status: 'ONGOING' }] }
    })
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.findAll('.tab')[1].trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('秒杀鞋')
    expect(wrapper.text()).toContain('限时秒杀')
  })
})
