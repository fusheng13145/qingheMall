import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LogisticsTimeline from './LogisticsTimeline.vue'

function makeLogistics(overrides = {}) {
  return {
    orderNumber: 'QH2026001',
    company: '顺丰速运',
    trackingNumber: 'SF1234567890',
    status: 'IN_TRANSIT',
    traces: [
      { description: '商家已发货', traceTime: '2026-08-01T09:00:00' },
      { description: '包裹运输中，正发往收货地址', traceTime: '2026-08-01T15:30:00' }
    ],
    ...overrides
  }
}

describe('LogisticsTimeline 物流时间线', () => {
  it('渲染承运商、运单号与状态文案', () => {
    const wrapper = mount(LogisticsTimeline, { props: { logistics: makeLogistics() } })
    expect(wrapper.text()).toContain('顺丰速运')
    expect(wrapper.text()).toContain('SF1234567890')
    expect(wrapper.text()).toContain('运输中')
  })

  it('按时间倒序展示轨迹且最新节点带 latest 标记', () => {
    const wrapper = mount(LogisticsTimeline, { props: { logistics: makeLogistics() } })
    const traces = wrapper.findAll('.lg-trace')
    expect(traces).toHaveLength(2)
    // 后端正序返回，页面第一条应为最新轨迹
    expect(traces[0].text()).toContain('包裹运输中，正发往收货地址')
    expect(traces[0].classes()).toContain('latest')
    expect(traces[1].text()).toContain('商家已发货')
    expect(traces[1].classes()).not.toContain('latest')
  })

  it('轨迹时间格式化为 MM-DD HH:mm', () => {
    const wrapper = mount(LogisticsTimeline, { props: { logistics: makeLogistics() } })
    expect(wrapper.text()).toContain('08-01 15:30')
  })

  it('无轨迹时展示空提示', () => {
    const wrapper = mount(LogisticsTimeline, { props: { logistics: makeLogistics({ traces: [] }) } })
    expect(wrapper.text()).toContain('暂无轨迹信息')
  })

  it('traces 缺省时不报错且展示空提示', () => {
    const wrapper = mount(LogisticsTimeline, { props: { logistics: makeLogistics({ traces: undefined }) } })
    expect(wrapper.text()).toContain('暂无轨迹信息')
  })

  it.each([
    ['SHIPPED', '已发货'],
    ['IN_TRANSIT', '运输中'],
    ['DELIVERING', '派送中'],
    ['SIGNED', '已签收']
  ])('状态 %s 展示文案 %s', (status, text) => {
    const wrapper = mount(LogisticsTimeline, { props: { logistics: makeLogistics({ status }) } })
    expect(wrapper.find('.lg-status').text()).toBe(text)
  })

  it('状态徽标 class 随状态小写切换', () => {
    const wrapper = mount(LogisticsTimeline, { props: { logistics: makeLogistics({ status: 'SIGNED' }) } })
    expect(wrapper.find('.lg-status').classes()).toContain('lg-status-signed')
  })

  it('未知状态回退展示原始值且缺省字段显示占位符', () => {
    const wrapper = mount(LogisticsTimeline, {
      props: { logistics: { orderNumber: 'QH1', status: 'CUSTOM' } }
    })
    expect(wrapper.find('.lg-status').text()).toBe('CUSTOM')
    expect(wrapper.text()).toContain('-')
  })

  it('非法时间字符串原样展示不抛错', () => {
    const wrapper = mount(LogisticsTimeline, {
      props: { logistics: makeLogistics({ traces: [{ description: '节点', traceTime: 'not-a-time' }] }) }
    })
    expect(wrapper.text()).toContain('not-a-time')
  })
})
