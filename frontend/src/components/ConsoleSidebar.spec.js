import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ConsoleSidebar from './common/ConsoleSidebar.vue'

function mountSidebar(sidebarOpen = false) {
  return mount(ConsoleSidebar, {
    props: { title: '管理后台', sidebarOpen },
    slots: {
      logo: '<span class="logo-mark">◆</span>',
      default: '<a class="nav-item">仪表盘</a>'
    },
    global: {
      stubs: {
        // teleport 无使用；保留默认渲染
      }
    }
  })
}

describe('common/ConsoleSidebar 控制台侧栏', () => {
  it('渲染标题、logo 插槽与默认插槽导航', () => {
    const wrapper = mountSidebar()

    expect(wrapper.text()).toContain('管理后台')
    expect(wrapper.find('.logo-mark').exists()).toBe(true)
    expect(wrapper.text()).toContain('仪表盘')
  })

  it('关闭态：遮罩不渲染（v-if）', () => {
    const wrapper = mountSidebar(false)

    expect(wrapper.find('.console-overlay').exists()).toBe(false)
    expect(wrapper.find('.console-sidebar').classes()).not.toContain('open')
  })

  it('打开态：遮罩渲染、侧栏携带 open 类', () => {
    const wrapper = mountSidebar(true)

    expect(wrapper.find('.console-overlay').exists()).toBe(true)
    expect(wrapper.find('.console-sidebar').classes()).toContain('open')
  })

  it('遮罩与关闭按钮触发 close 事件', async () => {
    const wrapper = mountSidebar(true)

    await wrapper.find('.console-overlay').trigger('click')
    await wrapper.find('.console-close').trigger('click')

    expect(wrapper.emitted('close')).toHaveLength(2)
  })

  it('title 缺失抛出 prop 校验警告（required 契约）', () => {
    const spy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    mount(ConsoleSidebar, { props: { sidebarOpen: false } })
    expect(spy).toHaveBeenCalled()
    spy.mockRestore()
  })
})
