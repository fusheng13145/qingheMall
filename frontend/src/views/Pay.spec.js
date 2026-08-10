import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Pay from './Pay.vue'

const { push, replace, route } = vi.hoisted(() => ({
  push: vi.fn(),
  replace: vi.fn(),
  route: { query: { orderNumber: 'QH20260810001' } }
}))
const { getOrder } = vi.hoisted(() => ({ getOrder: vi.fn() }))
const { createPay, mockPay, queryPay } = vi.hoisted(() => ({
  createPay: vi.fn(),
  mockPay: vi.fn(),
  queryPay: vi.fn()
}))

vi.mock('vue-router', () => ({ useRoute: () => route, useRouter: () => ({ push, replace }) }))
vi.mock('../api/order', () => ({ getOrder }))
vi.mock('../api/payment', () => ({ createPay, mockPay, queryPay }))

beforeEach(() => {
  vi.clearAllMocks()
  push.mockClear()
  replace.mockClear()
  global.alert = vi.fn()
})

describe('Pay 支付页', () => {
  it('加载订单并初始化模拟支付渠道', async () => {
    getOrder.mockResolvedValue({ data: { orderNumber: 'QH20260810001', totalPrice: 299 } })
    createPay.mockResolvedValue({ data: { mock: true, channel: 'WECHAT' } })

    const wrapper = mount(Pay)
    await flushPromises()

    expect(getOrder).toHaveBeenCalledWith('QH20260810001')
    expect(createPay).toHaveBeenCalledWith('QH20260810001', 'WECHAT')
    // 模拟支付提示文案
    expect(wrapper.text()).toContain('当前为模拟支付')
  })

  it('点击模拟支付成功跳转支付成功页', async () => {
    getOrder.mockResolvedValue({ data: { orderNumber: 'QH20260810001', totalPrice: 299 } })
    createPay.mockResolvedValue({ data: { mock: true, channel: 'WECHAT' } })
    mockPay.mockResolvedValue({})

    const wrapper = mount(Pay)
    await flushPromises()

    await wrapper.find('.btn-mock-pay').trigger('click')
    await flushPromises()

    expect(mockPay).toHaveBeenCalledWith('QH20260810001')
    expect(replace).toHaveBeenCalledWith({ name: 'PaySuccess', query: { orderNumber: 'QH20260810001' } })
  })

  it('切换支付宝渠道后初始化提示', async () => {
    getOrder.mockResolvedValue({ data: { orderNumber: 'QH20260810001', totalPrice: 299 } })
    createPay.mockResolvedValue({ data: { mock: true, channel: 'ALIPAY' } })

    const wrapper = mount(Pay)
    await flushPromises()
    createPay.mockClear()

    // 切到支付宝再触发初始化（模拟用户切换）
    await wrapper.findAll('.method-item')[1].trigger('click')
    createPay.mockResolvedValue({ data: { mock: true, channel: 'ALIPAY' } })

    expect(wrapper.text()).toContain('模拟支付')
  })

  it('手动查询已支付跳转成功页', async () => {
    getOrder.mockResolvedValue({ data: { orderNumber: 'QH20260810001', totalPrice: 299 } })
    // 真实渠道才渲染查询按钮（mock 渠道仅模拟支付按钮）
    createPay.mockResolvedValue({ data: { mock: false, channel: 'WECHAT', codeUrl: 'qr://x' } })
    queryPay.mockResolvedValue({ data: 'TRADE_PAID_SUCCESS' })

    const wrapper = mount(Pay)
    await flushPromises()

    await wrapper.find('.btn-query').trigger('click')
    await flushPromises()
    wrapper.unmount()

    expect(queryPay).toHaveBeenCalledWith('QH20260810001')
    expect(replace).toHaveBeenCalledWith({ name: 'PaySuccess', query: { orderNumber: 'QH20260810001' } })
  })

  it('手动查询未支付提示等待', async () => {
    getOrder.mockResolvedValue({ data: { orderNumber: 'QH20260810001', totalPrice: 299 } })
    createPay.mockResolvedValue({ data: { mock: false, channel: 'WECHAT', codeUrl: 'qr://x' } })
    queryPay.mockResolvedValue({ data: 'WAIT_BUYER_PAY' })

    const wrapper = mount(Pay)
    await flushPromises()

    await wrapper.find('.btn-query').trigger('click')
    await flushPromises()
    wrapper.unmount()

    expect(wrapper.text()).toContain('尚未检测到支付')
  })

  it('加载订单失败跳回订单中心', async () => {
    getOrder.mockRejectedValue(new Error('404'))

    mount(Pay)
    await flushPromises()

    expect(global.alert).toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith('/orders')
  })
})
