import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'

const { pageQuery } = vi.hoisted(() => ({ pageQuery: vi.fn() }))

vi.mock('../api/product.js', () => ({
  pageQuery: (...args) => pageQuery(...args)
}))
vi.mock('../stores/theme', () => ({
  useThemeStore: () => ({ getEffectiveTheme: () => 'light' })
}))

import Home from './Home.vue'

function makeHome(options = {}) {
  return shallowMount(Home, {
    global: {
      stubs: { RouterLink: { template: '<a><slot /></a>' } }
    },
    ...options
  })
}

beforeEach(() => {
  pageQuery.mockReset()
})

describe('Home 首页', () => {
  it('加载成功后渲染热门商品列表', async () => {
    pageQuery.mockResolvedValue({
      data: {
        data: [
          { id: 'p1', name: 'Nike 鞋', price: 799, purchaseNum: 10, productImgs: 'a.jpg' },
          { id: 'p2', name: 'Adidas 衣', price: 299, purchaseNum: 5, productImgs: 'b.jpg' }
        ]
      }
    })
    const wrapper = makeHome()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(pageQuery).toHaveBeenCalledWith(1, 8)
    expect(wrapper.findComponent({ name: 'ProductCard' }).exists()).toBe(true)
  })

  it('加载失败时显示空态而非崩溃', async () => {
    pageQuery.mockRejectedValue(new Error('network down'))
    const wrapper = makeHome()
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.empty-state').exists()).toBe(true)
    expect(wrapper.text()).toContain('暂无商品')
  })

  it('渲染品牌标语与特性区域', () => {
    pageQuery.mockResolvedValue({ data: { data: [] } })
    const wrapper = makeHome()
    expect(wrapper.text()).toContain('青禾商城')
    expect(wrapper.text()).toContain('品质保障')
    expect(wrapper.text()).toContain('极速配送')
  })
})
