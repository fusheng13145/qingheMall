import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Orders from './Orders.vue'
import { toasts } from '../../utils/toast'

// vi.hoisted：mock 工厂与测试体共享同一组 api 引用
const { getOrderList, updateOrderStatus, shipOrder, processRefund } = vi.hoisted(() => ({
  getOrderList: vi.fn(),
  updateOrderStatus: vi.fn(),
  shipOrder: vi.fn(),
  processRefund: vi.fn()
}))

vi.mock('../../api/admin', () => ({
  getOrderList: (...a) => getOrderList(...a),
  updateOrderStatus: (...a) => updateOrderStatus(...a),
  shipOrder: (...a) => shipOrder(...a),
  processRefund: (...a) => processRefund(...a)
}))

function makeOrder(overrides = {}) {
  return {
    id: 1,
    orderNumber: 'N2024050100001',
    user: { userName: '张三' },
    productName: '青禾 Tee',
    totalPrice: 199,
    status: 'WAIT_BUYER_PAY',
    gmtCreated: '2024-05-01T10:20:30',
    ...overrides
  }
}

/** res.data 为 Paging<Order>，列表在 data 字段 */
function mockList(list, paging = {}) {
  getOrderList.mockResolvedValue({
    data: { data: list, totalCount: list.length, totalPage: 1, ...paging }
  })
}

function mountPage() {
  return mount(Orders)
}

function expectToast(type, message) {
  expect(toasts.some(t => t.type === type && t.message === message)).toBe(true)
}

beforeEach(() => {
  vi.clearAllMocks()
  global.confirm = vi.fn(() => true)
  toasts.splice(0, toasts.length)
})

describe('admin/Orders 订单管理', () => {
  it('加载并渲染订单列表', async () => {
    mockList([
      makeOrder(),
      makeOrder({ id: 2, orderNumber: 'N2024050100002', productName: '青禾帽', totalPrice: 59.5, status: 'TRADE_PAID_SUCCESS' })
    ])
    const wrapper = mountPage()
    await flushPromises()

    expect(getOrderList).toHaveBeenCalledTimes(1)
    expect(getOrderList).toHaveBeenCalledWith(1, 20, '')
    expect(wrapper.text()).toContain('N2024050100001')
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('青禾 Tee')
    expect(wrapper.text()).toContain('199.00')
    expect(wrapper.text()).toContain('59.50')
    expect(wrapper.text()).toContain('待付款')
    expect(wrapper.text()).toContain('待发货')
    expect(wrapper.text()).toContain('2024-05-01 10:20:30')
  })

  it('空列表显示空态文案', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无订单数据')
  })

  it('加载失败 toast 提示', async () => {
    getOrderList.mockRejectedValue(new Error('net'))
    mountPage()
    await flushPromises()

    expectToast('error', '加载订单列表失败：net')
  })

  it('状态筛选切换：重置页码并按状态重新拉取', async () => {
    mockList([makeOrder()])
    const wrapper = mountPage()
    await flushPromises()

    const target = wrapper.findAll('.filter-btn').find(b => b.text() === '待发货')
    await target.trigger('click')
    await flushPromises()

    expect(getOrderList).toHaveBeenCalledTimes(2)
    expect(getOrderList).toHaveBeenLastCalledWith(1, 20, 'TRADE_PAID_SUCCESS')
    expect(target.classes()).toContain('active')
  })

  it('点击当前筛选按钮不重复请求', async () => {
    mockList([makeOrder()])
    const wrapper = mountPage()
    await flushPromises()

    const current = wrapper.findAll('.filter-btn').find(b => b.text() === '全部')
    await current.trigger('click')
    await flushPromises()

    expect(getOrderList).toHaveBeenCalledTimes(1)
  })

  it('分页：展示分页信息并可翻页', async () => {
    mockList([makeOrder()], { totalCount: 25, totalPage: 2 })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.page-info').text()).toBe('第 1 / 2 页（共 25 条）')
    const buttons = wrapper.findAll('.page-btn')
    expect(buttons[0].element.disabled).toBe(true)

    await buttons[1].trigger('click')
    await flushPromises()

    expect(getOrderList).toHaveBeenCalledTimes(2)
    expect(getOrderList).toHaveBeenLastCalledWith(2, 20, '')
    expect(wrapper.find('.page-info').text()).toBe('第 2 / 2 页（共 25 条）')
    expect(wrapper.findAll('.page-btn')[1].element.disabled).toBe(true)
  })

  it('发货：确认后调用接口并更新状态为待收货', async () => {
    mockList([makeOrder({ status: 'TRADE_PAID_SUCCESS' })])
    shipOrder.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.btn-ship').exists()).toBe(true)
    await wrapper.find('.btn-ship').trigger('click')
    await flushPromises()

    expect(global.confirm).toHaveBeenCalled()
    expect(shipOrder).toHaveBeenCalledWith('N2024050100001')
    expect(wrapper.find('.status-badge').text()).toBe('待收货')
    expect(wrapper.find('.btn-ship').exists()).toBe(false)
  })

  it('发货：取消确认不调用接口', async () => {
    global.confirm = vi.fn(() => false)
    mockList([makeOrder({ status: 'TRADE_PAID_SUCCESS' })])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-ship').trigger('click')
    await flushPromises()

    expect(shipOrder).not.toHaveBeenCalled()
    expect(wrapper.find('.btn-ship').exists()).toBe(true)
  })

  it('发货失败 toast 提示且状态不变', async () => {
    mockList([makeOrder({ status: 'TRADE_PAID_SUCCESS' })])
    shipOrder.mockRejectedValue(new Error('库存异常'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-ship').trigger('click')
    await flushPromises()

    expectToast('error', '发货失败：库存异常')
    expect(wrapper.find('.status-badge').text()).toBe('待发货')
  })

  it('状态修改：下拉切换成功后更新订单状态', async () => {
    mockList([makeOrder()])
    updateOrderStatus.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.status-select').setValue('TRADE_CLOSED')
    await flushPromises()

    expect(global.confirm).toHaveBeenCalled()
    expect(updateOrderStatus).toHaveBeenCalledWith('N2024050100001', 'TRADE_CLOSED')
    expect(wrapper.find('.status-badge').text()).toBe('已关闭')
  })

  it('状态修改：取消确认不调用接口并回退下拉值', async () => {
    global.confirm = vi.fn(() => false)
    mockList([makeOrder()])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.status-select').setValue('TRADE_CLOSED')
    await flushPromises()

    expect(updateOrderStatus).not.toHaveBeenCalled()
    expect(wrapper.find('.status-select').element.value).toBe('WAIT_BUYER_PAY')
    expect(wrapper.find('.status-badge').text()).toBe('待付款')
  })

  it('状态修改失败 toast 提示并回退下拉值', async () => {
    mockList([makeOrder()])
    updateOrderStatus.mockRejectedValue(new Error('无权限'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.status-select').setValue('TRADE_CLOSED')
    await flushPromises()

    expectToast('error', '状态修改失败：无权限')
    expect(wrapper.find('.status-select').element.value).toBe('WAIT_BUYER_PAY')
    expect(wrapper.find('.status-badge').text()).toBe('待付款')
  })

  it('退款审核：同意退款后状态变为已退款', async () => {
    mockList([makeOrder({ status: 'TRADE_REFUNDING' })])
    processRefund.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-approve').trigger('click')
    await flushPromises()

    expect(global.confirm).toHaveBeenCalled()
    expect(processRefund).toHaveBeenCalledWith('N2024050100001', true)
    expect(wrapper.find('.status-badge').text()).toBe('已退款')
    expect(wrapper.find('.btn-approve').exists()).toBe(false)
  })

  it('退款审核：拒绝退款后状态回退为待发货', async () => {
    mockList([makeOrder({ status: 'TRADE_REFUNDING' })])
    processRefund.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-reject').trigger('click')
    await flushPromises()

    expect(processRefund).toHaveBeenCalledWith('N2024050100001', false)
    expect(wrapper.find('.status-badge').text()).toBe('待发货')
    expect(wrapper.find('.btn-ship').exists()).toBe(true)
  })

  it('退款审核失败 toast 提示', async () => {
    mockList([makeOrder({ status: 'TRADE_REFUNDING' })])
    processRefund.mockRejectedValue(new Error('系统繁忙'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-approve').trigger('click')
    await flushPromises()

    expectToast('error', '操作失败：系统繁忙')
    expect(wrapper.find('.status-badge').text()).toBe('退款中')
  })
})
