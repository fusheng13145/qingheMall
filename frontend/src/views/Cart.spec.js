import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Cart from './Cart.vue'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
const { listCart, updateCartQuantity, updateCartSelected, removeCart } = vi.hoisted(() => ({
  listCart: vi.fn(),
  updateCartQuantity: vi.fn(),
  updateCartSelected: vi.fn(),
  removeCart: vi.fn()
}))
const { mockCartStore } = vi.hoisted(() => ({ mockCartStore: { refreshCount: vi.fn() } }))

vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../api/cart', () => ({ listCart, updateCartQuantity, updateCartSelected, removeCart }))
vi.mock('../stores/cart', () => ({ useCartStore: () => mockCartStore }))

function makeItem(overrides = {}) {
  return {
    id: 1,
    productName: 'Nike Air',
    productImg: 'a.jpg',
    price: 299,
    quantity: 2,
    stock: 10,
    selected: false,
    ...overrides
  }
}

function mountPage() {
  return mount(Cart, {
    global: { stubs: { 'router-link': { template: '<a><slot /></a>' } } }
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  push.mockClear()
  mockCartStore.refreshCount.mockClear()
  global.confirm = vi.fn(() => true)
})

describe('Cart 购物车页', () => {
  it('加载并渲染条目与合计', async () => {
    listCart.mockResolvedValue({ data: [makeItem({ selected: true }), makeItem({ id: 2, price: 100, selected: true })] })
    const wrapper = mountPage()
    await flushPromises()

    expect(listCart).toHaveBeenCalled()
    expect(wrapper.text()).toContain('Nike Air')
    // 299*2 + 100*2 = 798
    expect(wrapper.text()).toContain('798.00')
  })

  it('全部选中时全选框为勾选态', async () => {
    listCart.mockResolvedValue({ data: [makeItem({ selected: true }), makeItem({ id: 2, selected: true })] })
    const wrapper = mountPage()
    await flushPromises()

    // 底部结算栏的全选 checkbox 带 checked class
    const allCheckbox = wrapper.findAll('.checkbox').at(-1)
    expect(allCheckbox.classes()).toContain('checked')
  })

  it('单选切换调用更新接口，失败回滚', async () => {
    listCart.mockResolvedValue({ data: [makeItem()] })
    updateCartSelected.mockRejectedValue(new Error('net'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.findAll('.checkbox').at(0).trigger('click')
    await flushPromises()

    expect(updateCartSelected).toHaveBeenCalledWith(1, true)
    // 回滚为未选中
    expect(wrapper.findAll('.checkbox').at(0).classes()).not.toContain('checked')
  })

  it('数量增加调用更新接口', async () => {
    listCart.mockResolvedValue({ data: [makeItem()] })
    updateCartQuantity.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    // 第一个 item 的 + 按钮（qty-btn 第 2 个）
    await wrapper.findAll('.qty-btn')[1].trigger('click')
    await flushPromises()

    expect(updateCartQuantity).toHaveBeenCalledWith(1, 3)
  })

  it('数量到达库存上限时 + 按钮禁用', async () => {
    listCart.mockResolvedValue({ data: [makeItem({ quantity: 10, stock: 10 })] })
    const wrapper = mountPage()
    await flushPromises()
    updateCartQuantity.mockClear()

    const plusBtn = wrapper.findAll('.qty-btn')[1]
    expect(plusBtn.attributes('disabled')).toBeDefined()
    await plusBtn.trigger('click')
    await flushPromises()

    expect(updateCartQuantity).not.toHaveBeenCalled()
  })

  it('删除条目：确认后移除并刷新角标', async () => {
    listCart.mockResolvedValue({ data: [makeItem()] })
    removeCart.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.item-del').trigger('click')
    await flushPromises()

    expect(removeCart).toHaveBeenCalledWith(1)
    expect(mockCartStore.refreshCount).toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('Nike Air')
  })

  it('删除取消确认则不调用接口', async () => {
    global.confirm = vi.fn(() => false)
    listCart.mockResolvedValue({ data: [makeItem()] })
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.item-del').trigger('click')
    await flushPromises()

    expect(removeCart).not.toHaveBeenCalled()
  })

  it('点击去结算跳转 Checkout', async () => {
    listCart.mockResolvedValue({ data: [makeItem({ selected: true })] })
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-checkout').trigger('click')

    expect(push).toHaveBeenCalledWith({ name: 'Checkout' })
  })

  it('空购物车显示空态', async () => {
    listCart.mockResolvedValue({ data: [] })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('购物车还是空的')
  })
})
