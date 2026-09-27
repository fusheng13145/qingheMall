import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import NotFound from './NotFound.vue'

describe('views/NotFound 404 兜底页', () => {
  it('渲染 404 标识与文案', () => {
    const wrapper = mount(NotFound, {
      global: { stubs: { 'router-link': { template: '<a><slot /></a>' } } }
    })

    expect(wrapper.text()).toContain('404')
    expect(wrapper.text()).toContain('页面走丢了')
    expect(wrapper.text()).toContain('您访问的页面不存在或已被移除，去别处逛逛吧。')
  })

  it('提供返回首页与浏览商品两个出口', () => {
    const wrapper = mount(NotFound, {
      global: { stubs: { 'router-link': { template: '<a><slot /></a>' } } }
    })

    const links = wrapper.findAll('a')
    expect(links).toHaveLength(2)
    expect(wrapper.text()).toContain('返回首页')
    expect(wrapper.text()).toContain('浏览商品')
  })
})
