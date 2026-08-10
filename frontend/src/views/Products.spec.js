import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Products from './Products.vue'

const { pageQuery, listBrands } = vi.hoisted(() => ({
  pageQuery: vi.fn(),
  listBrands: vi.fn()
}))

vi.mock('../api/product.js', () => ({ pageQuery, listBrands }))

function makeProduct(id, name) {
  return {
    id,
    name,
    price: 799,
    purchaseNum: 100,
    productImgs: 'https://example.com/a.jpg'
  }
}

function mockPage(products, totalPage = 1) {
  pageQuery.mockResolvedValue({
    data: { pageNum: 1, pageSize: 12, totalPage, totalCount: products.length, data: products }
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  listBrands.mockResolvedValue({ data: ['Nike', 'Adidas'] })
})

describe('Products 商品列表页', () => {
  it('加载商品并渲染卡片', async () => {
    mockPage([makeProduct('p1', 'Nike Air'), makeProduct('p2', 'Adidas Run')])
    const wrapper = mount(Products)
    await flushPromises()

    expect(pageQuery).toHaveBeenCalledWith(1, 12, { keyword: '', brand: '', sort: '' })
    expect(wrapper.text()).toContain('Nike Air')
    expect(wrapper.text()).toContain('Adidas Run')
  })

  it('加载品牌列表', async () => {
    mockPage([])
    const wrapper = mount(Products)
    await flushPromises()

    expect(listBrands).toHaveBeenCalled()
    const options = wrapper.findAll('select.filter-select').at(0).findAll('option')
    expect(options.map(o => o.text())).toContain('Nike')
  })

  it('空商品显示「暂无商品」', async () => {
    mockPage([])
    const wrapper = mount(Products)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无商品')
  })

  it('搜索：输入关键词回车后按条件重新查询', async () => {
    mockPage([makeProduct('p1', 'Nike')])
    const wrapper = mount(Products)
    await flushPromises()
    pageQuery.mockClear()

    await wrapper.find('input[type="text"]').setValue('跑鞋')
    await wrapper.find('input[type="text"]').trigger('keyup.enter')
    await flushPromises()

    expect(pageQuery).toHaveBeenCalledWith(1, 12, { keyword: '跑鞋', brand: '', sort: '' })
  })

  it('品牌筛选触发搜索', async () => {
    mockPage([makeProduct('p1', 'Nike')])
    const wrapper = mount(Products)
    await flushPromises()
    pageQuery.mockClear()

    await wrapper.findAll('select.filter-select').at(0).setValue('Nike')
    await flushPromises()

    expect(pageQuery).toHaveBeenCalledWith(1, 12, { keyword: '', brand: 'Nike', sort: '' })
  })

  it('多页时显示分页并支持翻页', async () => {
    mockPage([makeProduct('p1', 'A')], 3)
    const wrapper = mount(Products)
    await flushPromises()
    pageQuery.mockClear()

    expect(wrapper.text()).toMatch(/1\s*\/\s*3/)

    const nextBtn = wrapper.findAll('.page-btn').at(1)
    await nextBtn.trigger('click')
    await flushPromises()

    expect(pageQuery).toHaveBeenCalledWith(2, 12, { keyword: '', brand: '', sort: '' })
  })

  it('加载失败时展示空态不崩溃', async () => {
    pageQuery.mockRejectedValue(new Error('network down'))
    const wrapper = mount(Products)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无商品')
  })
})
