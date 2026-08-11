import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Orders from './Orders.vue'
import { toasts } from '../../utils/toast'

// vi.hoisted：mock 工厂与测试体共享同一组 api 引用
const { listMerchantOrders, merchantShip, merchantProcessRefund, merchantAdvanceLogistics } = vi.hoisted(() => ({
  listMerchantOrders: vi.fn(),
  merchantShip: vi.fn(),
  merchantProcessRefund: vi.fn(),
  merchantAdvanceLogistics: vi.fn()
}))
const { trackLogistics } = vi.hoisted(() => ({ trackLogistics: vi.fn() }))
const { listOrderRefunds } = vi.hoisted(() => ({ listOrderRefunds: vi.fn() }))

vi.mock('../../api/merchant', () => ({
  listMerchantOrders: (...a) => listMerchantOrders(...a),
  merchantShip: (...a) => merchantShip(...a),
  merchantProcessRefund: (...a) => merchantProcessRefund(...a),
  merchantAdvanceLogistics: (...a) => merchantAdvanceLogistics(...a)
}))
vi.mock('../../api/logistics', () => ({ trackLogistics: (...a) => trackLogistics(...a) }))
vi.mock('../../api/refund', () => ({ listOrderRefunds: (...a) => listOrderRefunds(...a) }))

function makeOrder(overrides = {}) {
  return {
    orderNumber: 'NO20240101001',
    productName: '青禾 Tee',
    productImg: '',
    productDetail: { size: 40 },
    quantity: 2,
    user: { nickName: '小何', userName: 'he001' },
    totalPrice: 199,
    status: 'TRADE_PAID_SUCCESS',
    gmtCreated: '2024-01-01T12:34:56',
    ...overrides
  }
}

function makeLogistics(overrides = {}) {
  return {
    orderNumber: 'NO20240101001',
    company: '顺丰速运',
    trackingNumber: 'SF123456',
    status: 'IN_TRANSIT',
    traces: [{ description: '包裹运输中，正发往收货地址', traceTime: '2024-01-02T08:00:00' }],
    ...overrides
  }
}

/** res.data 为 Paging<Order>：列表在 data 字段，另带 totalPage / totalCount */
function mockPage(list, paging = {}) {
  listMerchantOrders.mockResolvedValue({
    data: { data: list, totalPage: 1, totalCount: list.length, ...paging }
  })
}

function mountPage() {
  return mount(Orders)
}

function expectToast(type, message) {
  expect(toasts.some((t) => t.type === type && t.message === message)).toBe(true)
}

function actionButton(wrapper, text) {
  return wrapper.findAll('.row-actions .link-btn').find((b) => b.text() === text)
}

function modalButton(wrapper, text) {
  return wrapper.findAll('.modal-actions button').find((b) => b.text() === text)
}

beforeEach(() => {
  vi.clearAllMocks()
  toasts.splice(0, toasts.length)
})

describe('merchant/Orders 订单管理', () => {
  it('加载并渲染订单列表', async () => {
    mockPage([makeOrder()])
    const wrapper = mountPage()
    await flushPromises()

    expect(listMerchantOrders).toHaveBeenCalledWith(1, 10, '')
    expect(wrapper.text()).toContain('NO20240101001')
    expect(wrapper.text()).toContain('青禾 Tee')
    expect(wrapper.text()).toContain('x2')
    expect(wrapper.text()).toContain('规格40')
    expect(wrapper.text()).toContain('小何')
    expect(wrapper.text()).toContain('199.00')
    expect(wrapper.text()).toContain('待发货')
    // 时间格式化：T 替换为空格并截取到分钟
    expect(wrapper.text()).toContain('2024-01-01 12:34')
    expect(actionButton(wrapper, '发货')).toBeTruthy()
  })

  it('买家昵称缺失时回退用户名，用户为空显示占位符', async () => {
    mockPage([
      makeOrder({ orderNumber: 'A1', user: { nickName: '', userName: 'he001' } }),
      makeOrder({ orderNumber: 'A2', user: null })
    ])
    const wrapper = mountPage()
    await flushPromises()

    const rows = wrapper.findAll('tbody tr')
    expect(rows[0].text()).toContain('he001')
    // 买家列是第 3 列
    expect(rows[1].findAll('td')[2].text()).toBe('-')
  })

  it('下单时间缺失显示占位符', async () => {
    mockPage([makeOrder({ gmtCreated: null })])
    const wrapper = mountPage()
    await flushPromises()

    // 下单时间列是第 6 列
    const row = wrapper.find('tbody tr')
    expect(row.findAll('td')[5].text()).toBe('-')
  })

  it('空列表显示空态文案', async () => {
    mockPage([])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无订单')
  })

  it('加载失败 toast 提示', async () => {
    listMerchantOrders.mockRejectedValue(new Error('net'))
    mountPage()
    await flushPromises()

    expectToast('error', '加载失败：net')
  })

  it('状态页签切换：带状态重新加载并高亮当前页签', async () => {
    mockPage([])
    const wrapper = mountPage()
    await flushPromises()

    const tab = wrapper.findAll('.tab').find((t) => t.text() === '待发货')
    await tab.trigger('click')
    await flushPromises()

    expect(listMerchantOrders).toHaveBeenLastCalledWith(1, 10, 'TRADE_PAID_SUCCESS')
    expect(tab.classes()).toContain('active')
  })

  it('已发货订单展示物流入口，待付款订单无操作', async () => {
    mockPage([makeOrder({ status: 'TRADE_SHIPPED' })])
    const wrapper = mountPage()
    await flushPromises()

    expect(actionButton(wrapper, '物流')).toBeTruthy()
  })

  it('待付款订单无操作按钮显示占位符', async () => {
    mockPage([makeOrder({ status: 'WAIT_BUYER_PAY' })])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.findAll('.row-actions .link-btn').length).toBe(0)
    expect(wrapper.find('.row-actions .muted').text()).toBe('-')
  })

  describe('发货弹窗（P2-18）', () => {
    it('点击发货打开弹窗并录入运单号后提交', async () => {
      mockPage([makeOrder()])
      merchantShip.mockResolvedValue({})
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '发货').trigger('click')
      expect(wrapper.find('.modal-title').text()).toBe('订单发货')
      expect(wrapper.text()).toContain('NO20240101001')

      await wrapper.find('.modal input.form-input').setValue('SF1234567890')
      await modalButton(wrapper, '确认发货').trigger('click')
      await flushPromises()

      expect(merchantShip).toHaveBeenCalledWith('NO20240101001', '顺丰速运', 'SF1234567890')
      expectToast('success', '发货成功，物流信息已录入')
      expect(listMerchantOrders).toHaveBeenCalledTimes(2)
    })

    it('切换承运商后提交携带所选承运商', async () => {
      mockPage([makeOrder()])
      merchantShip.mockResolvedValue({})
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '发货').trigger('click')
      await wrapper.find('.modal select.form-input').setValue('中通快递')
      await wrapper.find('.modal input.form-input').setValue('ZT98765')
      await modalButton(wrapper, '确认发货').trigger('click')
      await flushPromises()

      expect(merchantShip).toHaveBeenCalledWith('NO20240101001', '中通快递', 'ZT98765')
    })

    it('未填写运单号提交仅提示不调接口', async () => {
      mockPage([makeOrder()])
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '发货').trigger('click')
      await modalButton(wrapper, '确认发货').trigger('click')
      await flushPromises()

      expect(merchantShip).not.toHaveBeenCalled()
      expectToast('warning', '请填写运单号')
    })

    it('发货失败 toast 提示且弹窗保留', async () => {
      mockPage([makeOrder()])
      merchantShip.mockRejectedValue(new Error('物流异常'))
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '发货').trigger('click')
      await wrapper.find('.modal input.form-input').setValue('SF1')
      await modalButton(wrapper, '确认发货').trigger('click')
      await flushPromises()

      expectToast('error', '发货失败：物流异常')
      expect(wrapper.find('.modal-title').text()).toBe('订单发货')
    })
  })

  describe('物流弹窗（P2-18）', () => {
    it('打开物流弹窗渲染时间线', async () => {
      mockPage([makeOrder({ status: 'TRADE_SHIPPED' })])
      trackLogistics.mockResolvedValue({ data: makeLogistics() })
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '物流').trigger('click')
      await flushPromises()

      expect(trackLogistics).toHaveBeenCalledWith('NO20240101001')
      expect(wrapper.find('.modal-title').text()).toBe('物流跟踪')
      expect(wrapper.text()).toContain('顺丰速运')
      expect(wrapper.text()).toContain('SF123456')
      expect(wrapper.text()).toContain('包裹运输中，正发往收货地址')
      expect(modalButton(wrapper, '更新物流状态')).toBeTruthy()
    })

    it('无物流信息时展示占位提示', async () => {
      mockPage([makeOrder({ status: 'TRADE_SHIPPED' })])
      trackLogistics.mockResolvedValue({ data: null })
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '物流').trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('暂无物流信息')
      expect(modalButton(wrapper, '更新物流状态')).toBeUndefined()
    })

    it('已签收物流不展示推进按钮', async () => {
      mockPage([makeOrder({ status: 'TRADE_COMPLETED' })])
      trackLogistics.mockResolvedValue({ data: makeLogistics({ status: 'SIGNED' }) })
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '物流').trigger('click')
      await flushPromises()

      expect(modalButton(wrapper, '更新物流状态')).toBeUndefined()
    })

    it('推进物流状态成功后刷新数据与列表', async () => {
      mockPage([makeOrder({ status: 'TRADE_SHIPPED' })])
      trackLogistics.mockResolvedValue({ data: makeLogistics({ status: 'SHIPPED' }) })
      merchantAdvanceLogistics.mockResolvedValue({ data: makeLogistics({ status: 'IN_TRANSIT' }) })
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '物流').trigger('click')
      await flushPromises()
      await modalButton(wrapper, '更新物流状态').trigger('click')
      await flushPromises()

      expect(merchantAdvanceLogistics).toHaveBeenCalledWith('NO20240101001')
      expectToast('success', '物流状态已更新')
      expect(wrapper.text()).toContain('运输中')
      expect(listMerchantOrders).toHaveBeenCalledTimes(2)
    })

    it('推进物流失败 toast 提示', async () => {
      mockPage([makeOrder({ status: 'TRADE_SHIPPED' })])
      trackLogistics.mockResolvedValue({ data: makeLogistics({ status: 'SHIPPED' }) })
      merchantAdvanceLogistics.mockRejectedValue(new Error('已签收'))
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '物流').trigger('click')
      await flushPromises()
      await modalButton(wrapper, '更新物流状态').trigger('click')
      await flushPromises()

      expectToast('error', '物流状态更新失败：已签收')
    })

    it('物流查询失败 toast 提示', async () => {
      mockPage([makeOrder({ status: 'TRADE_SHIPPED' })])
      trackLogistics.mockRejectedValue(new Error('订单不存在'))
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '物流').trigger('click')
      await flushPromises()

      expectToast('error', '获取物流信息失败：订单不存在')
    })
  })

  describe('退款审核弹窗（P2-18）', () => {
    function mockPendingRefund(overrides = {}) {
      listOrderRefunds.mockResolvedValue({
        data: [
          { id: 1, type: 'RETURN_REFUND', status: 'PENDING', reason: '质量问题', gmtCreated: '2024-01-03T09:00:00', ...overrides }
        ]
      })
    }

    it('打开退款审核弹窗展示待审核申请', async () => {
      mockPage([makeOrder({ status: 'TRADE_REFUNDING' })])
      mockPendingRefund()
      const wrapper = mountPage()
      await flushPromises()

      expect(wrapper.text()).toContain('退款中')
      await actionButton(wrapper, '退款审核').trigger('click')
      await flushPromises()

      expect(listOrderRefunds).toHaveBeenCalledWith('NO20240101001')
      expect(wrapper.find('.modal-title').text()).toBe('退款审核')
      expect(wrapper.text()).toContain('退货退款')
      expect(wrapper.text()).toContain('质量问题')
    })

    it('同意退款以 approve=true 调用接口并刷新', async () => {
      mockPage([makeOrder({ status: 'TRADE_REFUNDING' })])
      mockPendingRefund()
      merchantProcessRefund.mockResolvedValue({})
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '退款审核').trigger('click')
      await flushPromises()
      await modalButton(wrapper, '同意退款').trigger('click')
      await flushPromises()

      expect(merchantProcessRefund).toHaveBeenCalledWith('NO20240101001', true, null)
      expectToast('success', '已通过退款，库存已回补')
      expect(listMerchantOrders).toHaveBeenCalledTimes(2)
    })

    it('驳回未填写审核意见时仅提示不调接口', async () => {
      mockPage([makeOrder({ status: 'TRADE_REFUNDING' })])
      mockPendingRefund()
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '退款审核').trigger('click')
      await flushPromises()
      await modalButton(wrapper, '驳回').trigger('click')
      await flushPromises()

      expect(merchantProcessRefund).not.toHaveBeenCalled()
      expectToast('warning', '驳回时请填写审核意见')
    })

    it('驳回填写审核意见后以 approve=false 调用接口', async () => {
      mockPage([makeOrder({ status: 'TRADE_REFUNDING' })])
      mockPendingRefund()
      merchantProcessRefund.mockResolvedValue({})
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '退款审核').trigger('click')
      await flushPromises()
      await wrapper.find('.modal .comment-textarea').setValue('请提供商品问题凭证')
      await modalButton(wrapper, '驳回').trigger('click')
      await flushPromises()

      expect(merchantProcessRefund).toHaveBeenCalledWith('NO20240101001', false, '请提供商品问题凭证')
      expectToast('success', '已驳回退款申请')
    })

    it('无待审核申请时按钮禁用并展示提示', async () => {
      mockPage([makeOrder({ status: 'TRADE_REFUNDING' })])
      listOrderRefunds.mockResolvedValue({ data: [] })
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '退款审核').trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('未找到待审核的退款申请')
      expect(modalButton(wrapper, '同意退款').attributes('disabled')).toBeDefined()
      expect(modalButton(wrapper, '驳回').attributes('disabled')).toBeDefined()
    })

    it('退款申请加载失败 toast 提示', async () => {
      mockPage([makeOrder({ status: 'TRADE_REFUNDING' })])
      listOrderRefunds.mockRejectedValue(new Error('系统繁忙'))
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '退款审核').trigger('click')
      await flushPromises()

      expectToast('error', '获取退款申请失败：系统繁忙')
    })

    it('退款处理失败 toast 提示', async () => {
      mockPage([makeOrder({ status: 'TRADE_REFUNDING' })])
      mockPendingRefund()
      merchantProcessRefund.mockRejectedValue(new Error('系统繁忙'))
      const wrapper = mountPage()
      await flushPromises()

      await actionButton(wrapper, '退款审核').trigger('click')
      await flushPromises()
      await modalButton(wrapper, '同意退款').trigger('click')
      await flushPromises()

      expectToast('error', '操作失败：系统繁忙')
    })
  })
})
