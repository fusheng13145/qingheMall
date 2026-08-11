import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import Cart from './Cart.vue'
import { useCartStore } from '../stores/cart'
import { toasts } from '../utils/toast'

// P2-15：页面改为消费集中式 store，测试用真实 store + mock api 层，
// 以同时覆盖「页面交互 → store 动作（乐观更新/回滚）」整条链路
const { push } = vi.hoisted(() => ({ push: vi.fn() }))
const { listCart, updateCartQuantity, updateCartSelected, removeCart, countCart, clearSelectedCart } =
  vi.hoisted(() => ({
    listCart: vi.fn(),
    updateCartQuantity: vi.fn(),
    updateCartSelected: vi.fn(),
    removeCart: vi.fn(),
    countCart: vi.fn(),
    clearSelectedCart: vi.fn()
  }))

vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../api/cart', () => ({
  listCart: (...a) => listCart(...a),
  updateCartQuantity: (...a) => updateCartQuantity(...a),
  updateCartSelected: (...a) => updateCartSelected(...a),
  removeCart: (...a) => removeCart(...a),
  countCart: (...a) => countCart(...a),
  clearSelectedCart: (...a) => clearSelectedCart(...a)
}))

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
  setActivePinia(createPinia())
  vi.clearAllMocks()
  push.mockClear()
  global.confirm = vi.fn(() => true)
  toasts.splice(0, toasts.length)
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

  it('加载失败 toast 提示', async () => {
    listCart.mockRejectedValue(new Error('net'))
    mountPage()
    await flushPromises()

    expect(toasts.some(t => t.type === 'error' && t.message.includes('加载购物车失败'))).toBe(true)
  })

  it('全部选中时全选框为勾选态', async () => {
    listCart.mockResolvedValue({ data: [makeItem({ selected: true }), makeItem({ id: 2, selected: true })] })
    const wrapper = mountPage()
    await flushPromises()

    // 底部结算栏的全选 checkbox 带 checked class
    const allCheckbox = wrapper.findAll('.checkbox').at(-1)
    expect(allCheckbox.classes()).toContain('checked')
  })

  it('单选切换调用 store 更新，失败回滚并提示', async () => {
    listCart.mockResolvedValue({ data: [makeItem()] })
    updateCartSelected.mockRejectedValue(new Error('net'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.findAll('.checkbox').at(0).trigger('click')
    await flushPromises()

    expect(updateCartSelected).toHaveBeenCalledWith(1, true)
    // store 回滚为未选中
    expect(wrapper.findAll('.checkbox').at(0).classes()).not.toContain('checked')
    expect(toasts.some(t => t.type === 'error' && t.message.includes('操作失败'))).toBe(true)
  })

  it('数量增加调用 store 更新', async () => {
    listCart.mockResolvedValue({ data: [makeItem()] })
    updateCartQuantity.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    // 第一个 item 的 + 按钮（qty-btn 第 2 个）
    await wrapper.findAll('.qty-btn')[1].trigger('click')
    await flushPromises()

    expect(updateCartQuantity).toHaveBeenCalledWith(1, 3)
    expect(wrapper.text()).toContain('3')
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

  it('删除条目：确认后移除并同步角标', async () => {
    listCart.mockResolvedValue({ data: [makeItem()] })
    removeCart.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.item-del').trigger('click')
    await flushPromises()

    expect(removeCart).toHaveBeenCalledWith(1)
    expect(wrapper.text()).not.toContain('Nike Air')
    expect(useCartStore().count).toBe(0)
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
