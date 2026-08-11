import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Dashboard from './Dashboard.vue'
import { toasts } from '../../utils/toast'

// vi.hoisted：mock 工厂与测试体共享同一组 api 引用
const { getDashboard, getOrderList, getSalesReport } = vi.hoisted(() => ({
  getDashboard: vi.fn(),
  getOrderList: vi.fn(),
  getSalesReport: vi.fn()
}))

vi.mock('../../api/admin', () => ({
  getDashboard: (...a) => getDashboard(...a),
  getOrderList: (...a) => getOrderList(...a),
  getSalesReport: (...a) => getSalesReport(...a)
}))

function mockStats(overrides = {}) {
  getDashboard.mockResolvedValue({
    data: { productCount: 12, orderCount: 34, userCount: 56, totalRevenue: 123456.78, ...overrides }
  })
}

/** getOrderList 返回 Result<Paging<Order>>，列表在 data.data，Dashboard 截取前 5 条 */
function mockOrders(list) {
  getOrderList.mockResolvedValue({ data: { data: list, totalCount: list.length, totalPage: 1 } })
}

function mockReport(list) {
  getSalesReport.mockResolvedValue({ data: list })
}

function makeOrder(overrides = {}) {
  return {
    id: 1,
    orderNumber: 'NO20260102001',
    user: { userName: 'user01' },
    productName: '青禾 Tee',
    totalPrice: 99,
    status: 'TRADE_PAID_SUCCESS',
    gmtCreated: '2026-01-02T03:04:00',
    ...overrides
  }
}

function mountPage() {
  return mount(Dashboard)
}

beforeEach(() => {
  vi.clearAllMocks()
  toasts.splice(0, toasts.length)
  // Dashboard 失败路径仅 console.error，测试中静默
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

describe('admin/Dashboard 仪表盘', () => {
  it('渲染统计卡片关键数字与标签', async () => {
    mockStats()
    mockOrders([])
    mockReport([])
    const wrapper = mountPage()
    await flushPromises()

    expect(getDashboard).toHaveBeenCalledTimes(1)
    expect(getOrderList).toHaveBeenCalledTimes(1)
    expect(getSalesReport).toHaveBeenCalledWith(7)
    expect(wrapper.find('.stat-products .stat-value').text()).toBe('12')
    expect(wrapper.find('.stat-orders .stat-value').text()).toBe('34')
    expect(wrapper.find('.stat-users .stat-value').text()).toBe('56')
    // 总收入千分位格式化
    expect(wrapper.find('.stat-revenue .stat-value').text()).toBe('¥123,456.78')
    expect(wrapper.text()).toContain('商品总数')
    expect(wrapper.text()).toContain('订单总数')
    expect(wrapper.text()).toContain('用户总数')
    expect(wrapper.text()).toContain('总收入')
  })

  it('最近订单最多渲染 5 条并展示状态文案与金额', async () => {
    mockStats()
    mockOrders([
      makeOrder(),
      makeOrder({ id: 2, orderNumber: 'NO20260102002', status: 'WAIT_BUYER_PAY', totalPrice: 59.5 }),
      makeOrder({ id: 3, orderNumber: 'NO20260102003' }),
      makeOrder({ id: 4, orderNumber: 'NO20260102004' }),
      makeOrder({ id: 5, orderNumber: 'NO20260102005' }),
      makeOrder({ id: 6, orderNumber: 'NO20260102006' })
    ])
    mockReport([])
    const wrapper = mountPage()
    await flushPromises()

    const rows = wrapper.findAll('.recent-section tbody tr')
    expect(rows.length).toBe(5)
    expect(wrapper.text()).toContain('NO20260102001')
    expect(wrapper.text()).toContain('user01')
    expect(wrapper.text()).toContain('青禾 Tee')
    expect(wrapper.text()).toContain('¥99.00')
    expect(wrapper.text()).toContain('¥59.50')
    // 状态映射：TRADE_PAID_SUCCESS → 待发货，WAIT_BUYER_PAY → 待付款
    expect(wrapper.text()).toContain('待发货')
    expect(wrapper.text()).toContain('待付款')
    expect(wrapper.text()).toContain('2026-01-02 03:04')
    // 第 6 条被截断
    expect(wrapper.text()).not.toContain('NO20260102006')
  })

  it('渲染销售报表表格与柱状图高度', async () => {
    mockStats()
    mockOrders([])
    mockReport([
      { day: '2026-08-01', orderCount: 3, salesAmount: 120 },
      { day: '2026-08-02', orderCount: 5, salesAmount: 60.5 }
    ])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.report-body').exists()).toBe(true)
    expect(wrapper.text()).toContain('2026-08-01')
    expect(wrapper.text()).toContain('¥120.00')
    expect(wrapper.text()).toContain('¥60.50')
    // 柱状图日期只取 MM-DD
    expect(wrapper.find('.bar-day').text()).toBe('08-01')
    // 柱高按最大值归一：120 → 100%，60.5 → 50%
    const bars = wrapper.findAll('.bar-fill')
    expect(bars.length).toBe(2)
    expect(bars[0].element.style.height).toBe('100%')
    expect(bars[1].element.style.height).toBe('50%')
  })

  it('报表为空显示空态文案', async () => {
    mockStats()
    mockOrders([])
    mockReport([])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.report-body').exists()).toBe(false)
    expect(wrapper.text()).toContain('近 7 天暂无已支付订单数据')
  })

  it('切换到近 30 天重新请求报表', async () => {
    mockStats()
    mockOrders([])
    mockReport([])
    const wrapper = mountPage()
    await flushPromises()

    const rangeButtons = wrapper.findAll('.range-btn')
    expect(rangeButtons[0].classes()).toContain('active')
    await rangeButtons[1].trigger('click')
    await flushPromises()

    expect(getSalesReport).toHaveBeenCalledTimes(2)
    expect(getSalesReport).toHaveBeenLastCalledWith(30)
    expect(rangeButtons[1].classes()).toContain('active')
    expect(wrapper.text()).toContain('近 30 天暂无已支付订单数据')
  })

  it('统计数据加载失败不崩溃且卡片保持默认 0', async () => {
    getDashboard.mockRejectedValue(new Error('net'))
    mockOrders([])
    mockReport([])
    const wrapper = mountPage()
    await flushPromises()

    expect(console.error).toHaveBeenCalled()
    expect(wrapper.find('.stat-products .stat-value').text()).toBe('0')
    expect(wrapper.find('.stat-revenue .stat-value').text()).toBe('¥0')
    expect(wrapper.findAll('.recent-section tbody tr').length).toBe(0)
  })

  it('销售报表加载失败保持空态', async () => {
    mockStats()
    mockOrders([])
    getSalesReport.mockRejectedValue(new Error('报表服务异常'))
    const wrapper = mountPage()
    await flushPromises()

    expect(console.error).toHaveBeenCalled()
    expect(wrapper.text()).toContain('近 7 天暂无已支付订单数据')
  })

  it('订单字段缺失时展示兜底占位符', async () => {
    mockStats()
    mockOrders([makeOrder({ user: null, productName: '', totalPrice: null })])
    mockReport([])
    const wrapper = mountPage()
    await flushPromises()

    const row = wrapper.find('.recent-section tbody tr')
    expect(row.text()).toContain('-')
    expect(row.text()).toContain('¥0.00')
  })
})
