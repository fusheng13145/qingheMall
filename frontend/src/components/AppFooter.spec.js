import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppFooter from './AppFooter.vue'

describe('AppFooter 页脚', () => {
  it('渲染品牌名称与标语', () => {
    const wrapper = mount(AppFooter)
    expect(wrapper.find('.footer-logo').text()).toContain('青禾商城')
    expect(wrapper.find('.footer-desc').text()).toBe('品质生活，尽在青禾')
  })

  it('渲染三组链接栏目与全部链接项', () => {
    const wrapper = mount(AppFooter)
    const titles = wrapper.findAll('.footer-col-title').map(el => el.text())
    expect(titles).toEqual(['服务', '关于', '帮助'])

    const links = wrapper.findAll('.footer-link')
    expect(links).toHaveLength(9)
    expect(wrapper.text()).toContain('售后服务')
    expect(wrapper.text()).toContain('关于我们')
    expect(wrapper.text()).toContain('常见问题')
    expect(wrapper.text()).toContain('用户协议')
  })

  it('渲染版权年份与权利声明', () => {
    const wrapper = mount(AppFooter)
    const copyright = wrapper.find('.footer-copyright').text()
    expect(copyright).toContain('2024')
    expect(copyright).toContain('青禾商城')
    expect(copyright).toContain('保留所有权利')
  })
})
