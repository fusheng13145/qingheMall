import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Orders from './Orders.vue'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
const { listOrders, cancelOrder, confirmReceipt, applyRefund } = vi.hoisted(() => ({
  listOrders: vi.fn(),
  cancelOrder: vi.fn(),
  confirmReceipt: vi.fn(),
  applyRefund: vi.fn()
}))
const { addComment } = vi.hoisted(() => ({ addComment: vi.fn() }))

vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../api/order', () => ({ listOrders, cancelOrder, confirmReceipt, applyRefund }))
vi.mock('../api/comment', () => ({ addComment }))

function makeOrder(overrides = {}) {
  return {
    id: 'o1',
    orderNumber: 'QH20260810001',
    status: 'WAIT_BUYER_PAY',
    productName: 'Nike Air',
    productImg: 'a.jpg',
    totalPrice: 299,
    quantity: 1,
    ...overrides
  }
}

function mockPage(orders, totalPage = 1) {
  listOrders.mockResolvedValue({ data: { totalPage, totalCount: orders.length, data: orders } })
}

function mountPage() {
  return mount(Orders, { global: { stubs: { 'router-link': { template: '<a><slot /></a>' } } } })
}

beforeEach(() => {
  vi.clearAllMocks()
  push.mockClear()
  global.confirm = vi.fn(() => true)
  global.alert = vi.fn()
})

describe('Orders 订单中心页', () => {
  it('加载订单并渲染状态文本', async () => {
    mockPage([makeOrder()])
    const wrapper = mountPage()
    await flushPromises()

    expect(listOrders).toHaveBeenCalledWith(null, 1, 10)
    expect(wrapper.text()).toContain('QH20260810001')
    expect(wrapper.text()).toContain('待付款')
  })

  it('切换状态 tab 按状态重新查询', async () => {
    mockPage([makeOrder()])
    const wrapper = mountPage()
    await flushPromises()
    listOrders.mockClear()

    await wrapper.findAll('.tab-btn')[1].trigger('click')
    await flushPromises()

    expect(listOrders).toHaveBeenCalledWith('WAIT_BUYER_PAY', 1, 10)
  })

  it('取消订单：确认后调接口并刷新列表', async () => {
    mockPage([makeOrder()])
    cancelOrder.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-cancel').trigger('click')
    await flushPromises()

    expect(cancelOrder).toHaveBeenCalledWith('QH20260810001')
    expect(global.alert).toHaveBeenCalledWith('订单已取消')
  })

  it('取消订单：取消确认则不调接口', async () => {
    global.confirm = vi.fn(() => false)
    mockPage([makeOrder()])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-cancel').trigger('click')
    await flushPromises()

    expect(cancelOrder).not.toHaveBeenCalled()
  })

  it('确认收货调接口', async () => {
    mockPage([makeOrder({ status: 'TRADE_SHIPPED' })])
    confirmReceipt.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expect(confirmReceipt).toHaveBeenCalledWith('QH20260810001')
  })

  it('申请退款调接口', async () => {
    mockPage([makeOrder({ status: 'TRADE_PAID_SUCCESS' })])
    applyRefund.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-refund').trigger('click')
    await flushPromises()

    expect(applyRefund).toHaveBeenCalledWith('QH20260810001')
  })

  it('待付款订单点击去支付跳转支付页', async () => {
    mockPage([makeOrder()])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-pay').trigger('click')

    expect(push).toHaveBeenCalledWith({ name: 'Pay', query: { orderNumber: 'QH20260810001' } })
  })

  it('多页时翻页', async () => {
    mockPage([makeOrder()], 3)
    const wrapper = mountPage()
    await flushPromises()
    listOrders.mockClear()

    await wrapper.findAll('.page-btn')[1].trigger('click')
    await flushPromises()

    expect(listOrders).toHaveBeenCalledWith(null, 2, 10)
  })

  it('空订单显示空态', async () => {
    mockPage([])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无订单')
  })
})
