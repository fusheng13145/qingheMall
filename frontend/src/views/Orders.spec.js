import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Orders from './Orders.vue'
import { toasts } from '../utils/toast'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
const { listOrders, cancelOrder, confirmReceipt, applyRefund } = vi.hoisted(() => ({
  listOrders: vi.fn(),
  cancelOrder: vi.fn(),
  confirmReceipt: vi.fn(),
  applyRefund: vi.fn()
}))
const { addComment } = vi.hoisted(() => ({ addComment: vi.fn() }))
const { trackLogistics } = vi.hoisted(() => ({ trackLogistics: vi.fn() }))
const { listOrderRefunds } = vi.hoisted(() => ({ listOrderRefunds: vi.fn() }))

vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../api/order', () => ({ listOrders, cancelOrder, confirmReceipt, applyRefund }))
vi.mock('../api/comment', () => ({ addComment }))
vi.mock('../api/logistics', () => ({ trackLogistics }))
vi.mock('../api/refund', () => ({ listOrderRefunds }))

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
  window.scrollTo = vi.fn()
  toasts.splice(0, toasts.length)
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

  it('tab 包含退款中与已退款状态', async () => {
    mockPage([])
    const wrapper = mountPage()
    await flushPromises()

    const labels = wrapper.findAll('.tab-btn').map(b => b.text())
    expect(labels).toContain('退款中')
    expect(labels).toContain('已退款')
  })

  it('取消订单：确认后调接口并刷新列表', async () => {
    mockPage([makeOrder()])
    cancelOrder.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-cancel').trigger('click')
    await flushPromises()

    expect(cancelOrder).toHaveBeenCalledWith('QH20260810001')
    expect(toasts.some(t => t.type === 'success' && t.message === '订单已取消')).toBe(true)
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

  describe('退款申请弹窗（P2-18）', () => {
    it('已发货订单点击申请退货退款打开弹窗并展示类型', async () => {
      mockPage([makeOrder({ status: 'TRADE_SHIPPED' })])
      const wrapper = mountPage()
      await flushPromises()

      const refundBtn = wrapper.findAll('.btn-refund').find(b => b.text() === '申请退货退款')
      await refundBtn.trigger('click')

      expect(wrapper.find('.modal-header h3').text()).toBe('申请退货退款')
      expect(wrapper.text()).toContain('退货退款')
      expect(wrapper.text()).toContain('QH20260810001')
    })

    it('未填写原因提交时仅提示不调接口', async () => {
      mockPage([makeOrder({ status: 'TRADE_PAID_SUCCESS' })])
      const wrapper = mountPage()
      await flushPromises()

      await wrapper.find('.btn-refund').trigger('click')
      await wrapper.find('.modal .btn-submit').trigger('click')
      await flushPromises()

      expect(applyRefund).not.toHaveBeenCalled()
      expect(toasts.some(t => t.type === 'warning' && t.message === '请填写退款原因')).toBe(true)
    })

    it('待发货订单提交后按仅退款类型调用接口并刷新列表', async () => {
      mockPage([makeOrder({ status: 'TRADE_PAID_SUCCESS' })])
      applyRefund.mockResolvedValue({})
      const wrapper = mountPage()
      await flushPromises()

      await wrapper.find('.btn-refund').trigger('click')
      await wrapper.find('.modal .comment-textarea').setValue('不想要了')
      await wrapper.find('.modal .btn-submit').trigger('click')
      await flushPromises()

      expect(applyRefund).toHaveBeenCalledWith('QH20260810001', '不想要了', 'REFUND_ONLY')
      expect(toasts.some(t => t.type === 'success' && t.message === '退款申请已提交，等待商家处理')).toBe(true)
    })

    it('已发货订单提交后按退货退款类型调用接口', async () => {
      mockPage([makeOrder({ status: 'TRADE_SHIPPED' })])
      applyRefund.mockResolvedValue({})
      const wrapper = mountPage()
      await flushPromises()

      const refundBtn = wrapper.findAll('.btn-refund').find(b => b.text() === '申请退货退款')
      await refundBtn.trigger('click')
      await wrapper.find('.modal .comment-textarea').setValue('尺码不合适')
      await wrapper.find('.modal .btn-submit').trigger('click')
      await flushPromises()

      expect(applyRefund).toHaveBeenCalledWith('QH20260810001', '尺码不合适', 'RETURN_REFUND')
    })

    it('提交失败展示错误提示且弹窗保留', async () => {
      mockPage([makeOrder({ status: 'TRADE_PAID_SUCCESS' })])
      applyRefund.mockRejectedValue(new Error('已存在待审核申请'))
      const wrapper = mountPage()
      await flushPromises()

      await wrapper.find('.btn-refund').trigger('click')
      await wrapper.find('.modal .comment-textarea').setValue('不想要了')
      await wrapper.find('.modal .btn-submit').trigger('click')
      await flushPromises()

      expect(toasts.some(t => t.type === 'error' && t.message.includes('申请失败'))).toBe(true)
      expect(wrapper.find('.modal-header h3').text()).toBe('申请退款')
    })
  })

  describe('物流信息弹窗（P2-18）', () => {
    it('已发货订单可查看物流并渲染时间线', async () => {
      mockPage([makeOrder({ status: 'TRADE_SHIPPED' })])
      trackLogistics.mockResolvedValue({
        data: {
          orderNumber: 'QH20260810001',
          company: '顺丰速运',
          trackingNumber: 'SF123',
          status: 'IN_TRANSIT',
          traces: [{ description: '包裹运输中，正发往收货地址', traceTime: '2026-08-10T12:00:00' }]
        }
      })
      const wrapper = mountPage()
      await flushPromises()

      const logisticsBtn = wrapper.findAll('.btn-comment').find(b => b.text() === '查看物流')
      await logisticsBtn.trigger('click')
      await flushPromises()

      expect(trackLogistics).toHaveBeenCalledWith('QH20260810001')
      expect(wrapper.text()).toContain('物流信息')
      expect(wrapper.text()).toContain('顺丰速运')
      expect(wrapper.text()).toContain('SF123')
      expect(wrapper.text()).toContain('包裹运输中，正发往收货地址')
    })

    it('后端返回空数据时展示暂无物流信息', async () => {
      mockPage([makeOrder({ status: 'TRADE_COMPLETED' })])
      trackLogistics.mockResolvedValue({ data: null })
      const wrapper = mountPage()
      await flushPromises()

      const logisticsBtn = wrapper.findAll('.btn-comment').find(b => b.text() === '查看物流')
      await logisticsBtn.trigger('click')
      await flushPromises()

      expect(wrapper.find('.modal-empty').text()).toBe('暂无物流信息')
    })

    it('物流查询失败展示错误提示', async () => {
      mockPage([makeOrder({ status: 'TRADE_SHIPPED' })])
      trackLogistics.mockRejectedValue(new Error('订单不存在'))
      const wrapper = mountPage()
      await flushPromises()

      const logisticsBtn = wrapper.findAll('.btn-comment').find(b => b.text() === '查看物流')
      await logisticsBtn.trigger('click')
      await flushPromises()

      expect(toasts.some(t => t.type === 'error' && t.message.includes('物流信息加载失败'))).toBe(true)
      expect(wrapper.find('.modal-empty').text()).toBe('暂无物流信息')
    })

    it('待发货订单不展示查看物流按钮', async () => {
      mockPage([makeOrder({ status: 'TRADE_PAID_SUCCESS' })])
      const wrapper = mountPage()
      await flushPromises()

      expect(wrapper.findAll('.btn-comment').some(b => b.text() === '查看物流')).toBe(false)
    })
  })

  describe('退款详情弹窗（P2-18）', () => {
    it('退款中订单可查看退款申请历史', async () => {
      mockPage([makeOrder({ status: 'TRADE_REFUNDING' })])
      listOrderRefunds.mockResolvedValue({
        data: [
          { id: 1, type: 'REFUND_ONLY', status: 'PENDING', reason: '不想要了', gmtCreated: '2026-08-10T10:00:00' }
        ]
      })
      const wrapper = mountPage()
      await flushPromises()

      await wrapper.find('.btn-refund').trigger('click')
      await flushPromises()

      expect(listOrderRefunds).toHaveBeenCalledWith('QH20260810001')
      expect(wrapper.text()).toContain('退款详情')
      expect(wrapper.text()).toContain('仅退款')
      expect(wrapper.text()).toContain('待商家审核')
      expect(wrapper.text()).toContain('不想要了')
    })

    it('已拒绝的退款展示商家备注', async () => {
      mockPage([makeOrder({ status: 'TRADE_REFUNDING' })])
      listOrderRefunds.mockResolvedValue({
        data: [
          { id: 2, type: 'RETURN_REFUND', status: 'REJECTED', reason: '质量问题', reviewComment: '请提供凭证', gmtCreated: '2026-08-10T10:00:00' }
        ]
      })
      const wrapper = mountPage()
      await flushPromises()

      await wrapper.find('.btn-refund').trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('退货退款')
      expect(wrapper.text()).toContain('已拒绝')
      expect(wrapper.text()).toContain('请提供凭证')
    })

    it('无退款记录时展示空态', async () => {
      mockPage([makeOrder({ status: 'TRADE_REFUNDED' })])
      listOrderRefunds.mockResolvedValue({ data: [] })
      const wrapper = mountPage()
      await flushPromises()

      const detailBtn = wrapper.findAll('.btn-refund').find(b => b.text() === '退款详情')
      await detailBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('暂无退款记录')
    })

    it('退款详情加载失败展示错误提示', async () => {
      mockPage([makeOrder({ status: 'TRADE_REFUNDING' })])
      listOrderRefunds.mockRejectedValue(new Error('boom'))
      const wrapper = mountPage()
      await flushPromises()

      await wrapper.find('.btn-refund').trigger('click')
      await flushPromises()

      expect(toasts.some(t => t.type === 'error' && t.message.includes('退款详情加载失败'))).toBe(true)
    })
  })
})
