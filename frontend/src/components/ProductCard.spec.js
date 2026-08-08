import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ProductCard from './ProductCard.vue'

// vi.hoisted：保证 mock 工厂与测试体内共享同一个 push 引用
const { push } = vi.hoisted(() => ({ push: vi.fn() }))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push })
}))

beforeEach(() => {
  push.mockClear()
})

function makeProduct(overrides = {}) {
  return {
    id: 'p001',
    name: 'Nike Air Force 1',
    price: 799,
    purchaseNum: 100,
    productImgs: 'https://example.com/a.jpg https://example.com/b.jpg',
    ...overrides
  }
}

describe('ProductCard 商品卡片', () => {
  it('渲染商品名称、价格与销量', () => {
    const wrapper = mount(ProductCard, {
      props: { product: makeProduct() }
    })
    expect(wrapper.text()).toContain('Nike Air Force 1')
    expect(wrapper.text()).toContain('¥799')
    expect(wrapper.text()).toContain('100人购买')
  })

  it('销量 > 50 显示「热销」徽标，否则不显示', () => {
    const hot = mount(ProductCard, { props: { product: makeProduct({ purchaseNum: 100 }) } })
    expect(hot.text()).toContain('热销')

    const normal = mount(ProductCard, { props: { product: makeProduct({ purchaseNum: 10 }) } })
    expect(normal.text()).not.toContain('热销')
  })

  it('取图片串第一张作为卡片图', () => {
    const wrapper = mount(ProductCard, { props: { product: makeProduct() } })
    const img = wrapper.find('img')
    expect(img.attributes('src')).toBe('https://example.com/a.jpg')
  })

  it('无图时 img src 为空串', () => {
    const wrapper = mount(ProductCard, { props: { product: makeProduct({ productImgs: '' }) } })
    expect(wrapper.find('img').attributes('src')).toBe('')
  })

  it('点击卡片跳转商品详情', async () => {
    const wrapper = mount(ProductCard, { props: { product: makeProduct() } })
    await wrapper.trigger('click')
    expect(push).toHaveBeenCalledWith('/product/p001')
  })
})
