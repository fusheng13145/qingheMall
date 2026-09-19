import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Shop from './Shop.vue'
import { getShopHome } from '../api/shop'

vi.mock('../api/shop', () => ({ getShopHome: vi.fn() }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { merchantId: '5' } }),
  useRouter: () => ({ push: vi.fn() })
}))

function shopHomePayload() {
  return {
    data: {
      shop: { id: 5, shopName: '青禾旗舰店', shopDesc: '品质生活' },
      stats: { productCount: 3, avgRating: 4.6, totalSales: 321 },
      products: { data: [{ id: 'p1', name: '商品A', price: 100, productImgs: 'a.jpg' }], totalPage: 1 },
      coupons: [{ id: 'c1', name: '满100减20' }],
      seckills: []
    }
  }
}

describe('Shop 店铺主页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('加载并渲染店铺信息/统计/商品', async () => {
    getShopHome.mockResolvedValue(shopHomePayload())
    const wrapper = mount(Shop)
    await flushPromises()

    expect(getShopHome).toHaveBeenCalledWith('5', 1, 12, '')
    expect(wrapper.text()).toContain('青禾旗舰店')
    expect(wrapper.text()).toContain('品质生活')
    expect(wrapper.text()).toContain('4.6')
    expect(wrapper.text()).toContain('商品A')
    expect(wrapper.text()).toContain('满100减20')
  })

  it('渲染秒杀入口', async () => {
    const payload = shopHomePayload()
    payload.data.seckills = [{ id: 's1', seckillPrice: 49.9 }]
    getShopHome.mockResolvedValue(payload)
    const wrapper = mount(Shop)
    await flushPromises()

    expect(wrapper.text()).toContain('限时秒杀')
  })

  it('空商品显示空态', async () => {
    const payload = shopHomePayload()
    payload.data.products = { data: [], totalPage: 0 }
    getShopHome.mockResolvedValue(payload)
    const wrapper = mount(Shop)
    await flushPromises()

    expect(wrapper.text()).toContain('该店铺暂无在售商品')
  })

  it('加载失败显示错误提示', async () => {
    getShopHome.mockRejectedValue(new Error('fail'))
    const wrapper = mount(Shop)
    await flushPromises()

    expect(wrapper.text()).toContain('店铺不存在或未营业')
  })
})
