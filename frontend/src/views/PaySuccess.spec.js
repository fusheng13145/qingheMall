import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import PaySuccess from './PaySuccess.vue'

const { route } = vi.hoisted(() => ({ route: { query: {} } }))

vi.mock('vue-router', () => ({ useRoute: () => route }))

function mountPage() {
  return mount(PaySuccess, { global: { stubs: { 'router-link': { template: '<a><slot /></a>' } } } })
}

beforeEach(() => {
  vi.clearAllMocks()
  route.query = {}
})

describe('PaySuccess 支付成功页', () => {
  it('展示成功文案与订单号', () => {
    route.query = { orderNumber: 'QH2026001' }
    const wrapper = mountPage()

    expect(wrapper.text()).toContain('支付成功')
    expect(wrapper.find('.order-id').text()).toContain('QH2026001')
  })

  it('兼容 orderId 查询参数', () => {
    route.query = { orderId: 'QH2026002' }
    const wrapper = mountPage()

    expect(wrapper.find('.order-id').text()).toContain('QH2026002')
  })

  it('无订单号时不渲染订单号行', () => {
    const wrapper = mountPage()

    expect(wrapper.find('.order-id').exists()).toBe(false)
  })

  it('渲染查看订单与返回首页入口', () => {
    const wrapper = mountPage()
    const links = wrapper.findAll('a')

    expect(links.some(l => l.text().includes('查看订单'))).toBe(true)
    expect(links.some(l => l.text().includes('返回首页'))).toBe(true)
  })
})
