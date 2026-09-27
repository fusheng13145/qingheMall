import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'

const { recommend, listBanners } = vi.hoisted(() => ({
  recommend: vi.fn(),
  listBanners: vi.fn()
}))

vi.mock('../api/product.js', () => ({
  recommend: (...args) => recommend(...args)
}))
vi.mock('../api/banner.js', () => ({
  listBanners: (...args) => listBanners(...args)
}))
vi.mock('../stores/theme', () => ({
  useThemeStore: () => ({ getEffectiveTheme: () => 'light' })
}))
const { mockUserStore } = vi.hoisted(() => ({
  mockUserStore: { isLoggedIn: false }
}))
vi.mock('../stores/user', () => ({
  useUserStore: () => mockUserStore
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

function productPage(n) {
  // 拦截器约定：resolve 的是 Result 包络，data 为业务数组（非 axios response）
  const data = []
  for (let i = 0; i < n; i++) {
    data.push({ id: 'p' + i, name: '商品' + i, price: 100, purchaseNum: i, productImgs: 'a.jpg' })
  }
  return { data }
}

beforeEach(() => {
  recommend.mockReset().mockResolvedValue(productPage(0))
  listBanners.mockReset().mockResolvedValue({ data: { data: [] } })
})

describe('Home 首页（D2 v1.7：运营位 + 推荐位）', () => {
  it('加载成功后渲染热销与新品两个推荐位', async () => {
    recommend.mockImplementation((scene) => Promise.resolve(productPage(scene === 'hot' ? 2 : 1)))
    const wrapper = makeHome()
    await flushPromises()

    expect(recommend).toHaveBeenCalledWith('hot', 8)
    expect(recommend).toHaveBeenCalledWith('new', 8)
    expect(wrapper.text()).toContain('热销推荐')
    expect(wrapper.text()).toContain('新品上架')
    expect(wrapper.findAllComponents({ name: 'ProductCard' })).toHaveLength(3)
  })

  it('推荐位加载失败时显示空态而非崩溃', async () => {
    recommend.mockRejectedValue(new Error('network down'))
    const wrapper = makeHome()
    await flushPromises()

    expect(wrapper.findAll('.empty-state')).toHaveLength(2)
    expect(wrapper.text()).toContain('暂无商品')
    expect(wrapper.text()).toContain('暂无新品')
  })

  it('运营位：有上架位时渲染轮播区（多张显示指示点），无位时隐藏', async () => {
    listBanners.mockResolvedValue({
      data: [
        { id: 'b1', title: '开学季', image: '/uploads/b1.jpg', linkUrl: '/products' },
        { id: 'b2', title: '618', image: '/uploads/b2.jpg', linkUrl: 'https://example.com' }
      ]
    })
    const wrapper = makeHome()
    await flushPromises()

    expect(wrapper.find('.banner-zone').exists()).toBe(true)
    expect(wrapper.findAll('.banner-slide')).toHaveLength(2)
    expect(wrapper.findAll('.banner-dot')).toHaveLength(2)

    // 用例内隔离：切换为无运营位
    listBanners.mockResolvedValue({ data: { data: [] } })
    const empty = makeHome()
    await flushPromises()
    expect(empty.find('.banner-zone').exists()).toBe(false)
  })

  it('运营位加载失败不影响推荐位渲染', async () => {
    // hot 1 件 + new 0 件 → 仅 1 张商品卡
    recommend.mockImplementation((scene) => Promise.resolve(productPage(scene === 'hot' ? 1 : 0)))
    listBanners.mockRejectedValue(new Error('network down'))
    const wrapper = makeHome()
    await flushPromises()

    expect(wrapper.find('.banner-zone').exists()).toBe(false)
    expect(wrapper.findAllComponents({ name: 'ProductCard' })).toHaveLength(1)
  })

  it('登录用户第一区块走个性化推荐（为你推荐）', async () => {
    mockUserStore.isLoggedIn = true
    recommend.mockImplementation((scene) => Promise.resolve(productPage(scene === 'personal' ? 2 : 1)))
    const wrapper = makeHome()
    await flushPromises()

    expect(recommend).toHaveBeenCalledWith('personal', 8)
    expect(recommend).toHaveBeenCalledWith('new', 8)
    expect(wrapper.text()).toContain('为你推荐')
    mockUserStore.isLoggedIn = false
  })

  it('渲染品牌标语与特性区域', () => {
    const wrapper = makeHome()
    expect(wrapper.text()).toContain('青禾商城')
    expect(wrapper.text()).toContain('品质保障')
    expect(wrapper.text()).toContain('极速配送')
  })
})
