import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ProductDetail from './ProductDetail.vue'

const { push, route } = vi.hoisted(() => ({
  push: vi.fn(),
  route: { params: { id: 'p001' }, fullPath: '/product/p001' }
}))
const { get, getProductDetails, pageQuery } = vi.hoisted(() => ({
  get: vi.fn(),
  getProductDetails: vi.fn(),
  pageQuery: vi.fn()
}))
const { addOrder } = vi.hoisted(() => ({ addOrder: vi.fn() }))
const { addCart } = vi.hoisted(() => ({ addCart: vi.fn() }))
const { getCommentSummary, listProductComments } = vi.hoisted(() => ({
  getCommentSummary: vi.fn(),
  listProductComments: vi.fn()
}))
// #39「本店优惠」区块新增的店铺券/秒杀 API——不 mock 会经 utils/request 引入真实
// router（顶层 createRouter）撞上 vue-router mock，导致整个 suite 加载失败
const { listShopCoupons, claimCoupon } = vi.hoisted(() => ({
  listShopCoupons: vi.fn(),
  claimCoupon: vi.fn()
}))
const { listShopSeckillActivities } = vi.hoisted(() => ({
  listShopSeckillActivities: vi.fn()
}))
const { mockUserStore, mockCartStore } = vi.hoisted(() => ({
  mockUserStore: { isLoggedIn: false },
  mockCartStore: { refreshCount: vi.fn() }
}))

vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ push })
}))
vi.mock('../api/product', () => ({ get, getProductDetails, pageQuery }))
vi.mock('../api/order', () => ({ addOrder }))
vi.mock('../api/cart', () => ({ addCart }))
vi.mock('../api/comment', () => ({ getCommentSummary, listProductComments }))
vi.mock('../api/coupon', () => ({ listShopCoupons, claimCoupon }))
vi.mock('../api/seckill', () => ({ listShopSeckillActivities }))
vi.mock('../stores/user', () => ({ useUserStore: () => mockUserStore }))
vi.mock('../stores/cart', () => ({ useCartStore: () => mockCartStore }))

function mockBaseData() {
  get.mockResolvedValue({ data: { id: 'p001', name: 'Nike Air', brand: 'Nike', productImgs: 'a.jpg b.jpg' } })
  getProductDetails.mockResolvedValue({ data: [{ id: 'pd1', size: 38, stock: 10, price: 799 }] })
  getCommentSummary.mockResolvedValue({ data: { avgRating: 4.5, count: 12 } })
  listProductComments.mockResolvedValue({ data: { data: [{ id: 'c1', content: '很好' }], totalPage: 1 } })
  pageQuery.mockResolvedValue({ data: { data: [] } })
}

function mountPage() {
  return mount(ProductDetail, { global: { stubs: { 'router-link': true } } })
}

beforeEach(() => {
  vi.clearAllMocks()
  Object.assign(mockUserStore, { isLoggedIn: false })
  mockCartStore.refreshCount.mockClear()
  push.mockClear()
})

describe('ProductDetail 商品详情页', () => {
  it('加载商品、规格与评价并渲染', async () => {
    mockBaseData()
    const wrapper = mountPage()
    await flushPromises()

    expect(get).toHaveBeenCalledWith('p001')
    expect(getProductDetails).toHaveBeenCalledWith('p001')
    expect(wrapper.text()).toContain('Nike Air')
    expect(wrapper.text()).toContain('很好')
  })

  it('单规格自动选中', async () => {
    mockBaseData()
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('加入购物车')
  })

  it('未登录点击「立即购买」跳转登录页带 redirect', async () => {
    mockBaseData()
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find(".btn-buy").trigger('click')
    expect(push).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/product/p001' } })
  })

  it('已登录且选中规格可下单并跳转收银台', async () => {
    mockBaseData()
    mockUserStore.isLoggedIn = true
    addOrder.mockResolvedValue({ data: { orderNumber: 'QH1' } })
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find(".btn-buy").trigger('click')
    await flushPromises()

    expect(addOrder).toHaveBeenCalledWith({ productDetailId: 'pd1', quantity: 1 })
    expect(push).toHaveBeenCalledWith({ name: 'Pay', query: { orderNumber: 'QH1' } })
  })

  it('未登录加入购物车跳转登录页', async () => {
    mockBaseData()
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find(".btn-cart").trigger('click')
    expect(push).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/product/p001' } })
  })

  it('已登录加入购物车成功后刷新购物车角标', async () => {
    mockBaseData()
    mockUserStore.isLoggedIn = true
    addCart.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find(".btn-cart").trigger('click')
    await flushPromises()

    expect(addCart).toHaveBeenCalledWith('pd1', 1)
    expect(mockCartStore.refreshCount).toHaveBeenCalled()
  })
})
