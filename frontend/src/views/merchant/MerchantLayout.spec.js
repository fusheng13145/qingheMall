import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { reactive } from 'vue'
import MerchantLayout from './MerchantLayout.vue'

// vi.hoisted：mock 工厂与测试体共享同一组可控引用
const ctx = vi.hoisted(() => ({ user: null }))

vi.mock('../../stores/user', () => ({ useUserStore: () => ctx.user }))

const NAV_ITEMS = ['店铺概览', '商品管理', '订单管理']

function mountLayout(routeName = 'MerchantOverview') {
  return mount(MerchantLayout, {
    global: {
      // 模板中通过 $route.name 判断导航高亮，直接以 mocks 注入
      mocks: { $route: { name: routeName } },
      stubs: {
        'router-link': { template: '<a><slot /></a>' },
        'router-view': { template: '<div class="rv-stub" />' }
      }
    }
  })
}

function setInnerWidth(width) {
  Object.defineProperty(window, 'innerWidth', { value: width, writable: true, configurable: true })
}

let originalInnerWidth

beforeEach(() => {
  ctx.user = reactive({ nickName: '商家小李' })
  originalInnerWidth = window.innerWidth
})

afterEach(() => {
  setInnerWidth(originalInnerWidth)
})

describe('merchant/MerchantLayout 商家工作台布局', () => {
  it('渲染控制台标题与全部侧边导航项', () => {
    const wrapper = mountLayout()
    expect(wrapper.find('.console-sidebar-title').text()).toBe('商家工作台')
    expect(wrapper.findAll('.nav-item')).toHaveLength(NAV_ITEMS.length)
    NAV_ITEMS.forEach(name => expect(wrapper.text()).toContain(name))
  })

  it('当前路由对应的导航项高亮（active）', () => {
    const wrapper = mountLayout('MerchantOrders')
    const activeItems = wrapper.findAll('.nav-item').filter(i => i.classes().includes('active'))
    expect(activeItems).toHaveLength(1)
    expect(activeItems[0].text()).toContain('订单管理')
  })

  it('无匹配路由时所有导航项均不高亮', () => {
    const wrapper = mountLayout('OtherRoute')
    const activeItems = wrapper.findAll('.nav-item').filter(i => i.classes().includes('active'))
    expect(activeItems).toHaveLength(0)
  })

  it('顶栏显示商家昵称与返回商城入口', () => {
    const wrapper = mountLayout()
    expect(wrapper.find('.merchant-name').text()).toBe('商家小李')
    expect(wrapper.find('.back-link').text()).toContain('返回商城')
  })

  it('主内容区渲染 router-view 子路由出口', () => {
    const wrapper = mountLayout()
    expect(wrapper.find('.merchant-content .rv-stub').exists()).toBe(true)
  })

  it('移动端点击菜单按钮展开侧栏，点击导航项后自动收起', async () => {
    setInnerWidth(500)
    const wrapper = mountLayout()
    const sidebar = wrapper.find('.console-sidebar')
    expect(sidebar.classes()).not.toContain('open')

    await wrapper.find('.menu-toggle').trigger('click')
    expect(sidebar.classes()).toContain('open')

    await wrapper.find('.nav-item').trigger('click')
    expect(sidebar.classes()).not.toContain('open')
  })

  it('桌面宽度下点击导航项不收起侧栏', async () => {
    setInnerWidth(1024)
    const wrapper = mountLayout()
    await wrapper.find('.menu-toggle').trigger('click')
    expect(wrapper.find('.console-sidebar').classes()).toContain('open')

    await wrapper.find('.nav-item').trigger('click')
    expect(wrapper.find('.console-sidebar').classes()).toContain('open')
  })

  it('点击侧栏关闭按钮收起侧栏', async () => {
    const wrapper = mountLayout()
    await wrapper.find('.menu-toggle').trigger('click')
    expect(wrapper.find('.console-sidebar').classes()).toContain('open')

    await wrapper.find('.console-close').trigger('click')
    expect(wrapper.find('.console-sidebar').classes()).not.toContain('open')
  })
})
