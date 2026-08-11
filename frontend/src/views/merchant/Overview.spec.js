import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Overview from './Overview.vue'

// vi.hoisted：mock 工厂与测试体共享同一组 api 引用
const { getMerchantInfo, getMerchantStats } = vi.hoisted(() => ({
  getMerchantInfo: vi.fn(),
  getMerchantStats: vi.fn()
}))

vi.mock('../../api/merchant', () => ({
  getMerchantInfo: (...a) => getMerchantInfo(...a),
  getMerchantStats: (...a) => getMerchantStats(...a)
}))

function makeMerchant(overrides = {}) {
  return { shopName: '青禾小铺', shopDesc: '手作男装', status: 'ACTIVE', ...overrides }
}

function mountPage() {
  return mount(Overview, { global: { stubs: { 'router-link': true } } })
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('merchant/Overview 店铺概览', () => {
  it('ACTIVE 商家渲染店铺信息与经营统计', async () => {
    getMerchantInfo.mockResolvedValue({ data: makeMerchant() })
    getMerchantStats.mockResolvedValue({
      data: { productCount: 3, orderCount: 12, todayOrderCount: 2, paidRevenue: 1234.5, todayRevenue: 88 }
    })
    const wrapper = mountPage()
    await flushPromises()

    expect(getMerchantInfo).toHaveBeenCalledTimes(1)
    expect(getMerchantStats).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('青禾小铺')
    expect(wrapper.text()).toContain('手作男装')
    expect(wrapper.text()).toContain('12')
    expect(wrapper.text()).toContain('¥1234.50')
    expect(wrapper.text()).toContain('¥88.00')
    // 店铺 logo 取店名首字
    expect(wrapper.find('.shop-logo').text()).toBe('青')
    // ACTIVE 状态不显示任何状态横幅
    expect(wrapper.find('.status-banner').exists()).toBe(false)
  })

  it('审核中显示 PENDING 横幅且不请求统计数据', async () => {
    getMerchantInfo.mockResolvedValue({ data: makeMerchant({ status: 'PENDING' }) })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('入驻申请审核中，平台通过后即可经营店铺')
    expect(getMerchantStats).not.toHaveBeenCalled()
  })

  it('审核未通过显示拒绝原因', async () => {
    getMerchantInfo.mockResolvedValue({
      data: makeMerchant({ status: 'REJECTED', rejectReason: '资质不全' })
    })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('入驻申请未通过：资质不全')
    expect(getMerchantStats).not.toHaveBeenCalled()
  })

  it('审核未通过但未填写原因时显示默认文案', async () => {
    getMerchantInfo.mockResolvedValue({ data: makeMerchant({ status: 'REJECTED' }) })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('入驻申请未通过：未填写原因')
  })

  it('禁用状态显示禁用提示', async () => {
    getMerchantInfo.mockResolvedValue({ data: makeMerchant({ status: 'DISABLED' }) })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('店铺已被平台禁用，请联系客服')
    expect(getMerchantStats).not.toHaveBeenCalled()
  })

  it('未入驻商家显示空态与注册入口', async () => {
    getMerchantInfo.mockResolvedValue({ data: null })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('您还不是入驻商家')
    const link = wrapper.find('router-link-stub')
    expect(link.exists()).toBe(true)
    expect(link.attributes('to')).toBe('/register')
    expect(getMerchantStats).not.toHaveBeenCalled()
    // 未入驻不渲染店铺卡片与统计
    expect(wrapper.find('.shop-card').exists()).toBe(false)
  })

  it('商家信息接口失败时静默降级为空态', async () => {
    getMerchantInfo.mockRejectedValue(new Error('net'))
    const wrapper = mountPage()
    await flushPromises()

    // 静默失败：loading 结束后按未入驻展示空态
    expect(wrapper.text()).toContain('您还不是入驻商家')
    expect(getMerchantStats).not.toHaveBeenCalled()
  })

  it('ACTIVE 但统计接口失败时统计项显示占位符', async () => {
    getMerchantInfo.mockResolvedValue({ data: makeMerchant() })
    getMerchantStats.mockRejectedValue(new Error('stats down'))
    const wrapper = mountPage()
    await flushPromises()

    // 店铺信息仍渲染，统计降级为占位符
    expect(wrapper.text()).toContain('青禾小铺')
    const values = wrapper.findAll('.stat-value').map((v) => v.text())
    expect(values[0]).toBe('-')
    expect(values[3]).toBe('¥0.00')
    expect(values[4]).toBe('¥0.00')
  })
})
