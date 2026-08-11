import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// vi.hoisted：保证 mock 工厂与测试体内共享同一组 api 引用
const {
  countCart,
  listCart,
  updateCartQuantity,
  updateCartSelected,
  removeCart,
  clearSelectedCart
} = vi.hoisted(() => ({
  countCart: vi.fn(),
  listCart: vi.fn(),
  updateCartQuantity: vi.fn(),
  updateCartSelected: vi.fn(),
  removeCart: vi.fn(),
  clearSelectedCart: vi.fn()
}))

vi.mock('../api/cart', () => ({
  countCart: (...args) => countCart(...args),
  listCart: (...args) => listCart(...args),
  updateCartQuantity: (...args) => updateCartQuantity(...args),
  updateCartSelected: (...args) => updateCartSelected(...args),
  removeCart: (...args) => removeCart(...args),
  clearSelectedCart: (...args) => clearSelectedCart(...args)
}))

import { useCartStore } from './cart'

function makeItem(overrides = {}) {
  return { id: 1, productName: 'A', price: 100, quantity: 1, selected: false, ...overrides }
}

describe('cart store 购物车集中管理（P2-15）', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchItems 拉取列表并同步角标', async () => {
    listCart.mockResolvedValue({ data: [makeItem(), makeItem({ id: 2 })] })
    const store = useCartStore()
    await store.fetchItems()
    expect(store.items).toHaveLength(2)
    expect(store.count).toBe(2)
    expect(store.loading).toBe(false)
  })

  it('fetchItems 失败时 loading 复位且异常继续上抛', async () => {
    listCart.mockRejectedValue(new Error('network'))
    const store = useCartStore()
    await expect(store.fetchItems()).rejects.toThrow('network')
    expect(store.loading).toBe(false)
  })

  it('refreshCount 成功时写入条目数', async () => {
    countCart.mockResolvedValue({ data: 5 })
    const store = useCartStore()
    await store.refreshCount()
    expect(store.count).toBe(5)
  })

  it('refreshCount 失败时回退为 0（角标降级不报错）', async () => {
    countCart.mockRejectedValue(new Error('network'))
    const store = useCartStore()
    store.count = 3
    await store.refreshCount()
    expect(store.count).toBe(0)
  })

  it('selectedItems/allSelected/selectedTotalPrice 派生正确', async () => {
    listCart.mockResolvedValue({
      data: [makeItem({ selected: true, quantity: 2 }), makeItem({ id: 2, price: 50 })]
    })
    const store = useCartStore()
    await store.fetchItems()
    expect(store.selectedItems).toHaveLength(1)
    expect(store.selectedTotalPrice).toBe(200)
    expect(store.allSelected).toBe(false)
  })

  it('toggleSelected 乐观更新成功', async () => {
    updateCartSelected.mockResolvedValue({})
    const store = useCartStore()
    const item = makeItem()
    store.items = [item]
    await store.toggleSelected(item)
    expect(item.selected).toBe(true)
    expect(updateCartSelected).toHaveBeenCalledWith(1, true)
  })

  it('toggleSelected 失败回滚勾选态并抛出异常', async () => {
    updateCartSelected.mockRejectedValue(new Error('boom'))
    const store = useCartStore()
    const item = makeItem()
    store.items = [item]
    await expect(store.toggleSelected(item)).rejects.toThrow('boom')
    expect(item.selected).toBe(false)
  })

  it('setAllSelected 批量成功', async () => {
    updateCartSelected.mockResolvedValue({})
    const store = useCartStore()
    store.items = [makeItem(), makeItem({ id: 2 })]
    await store.setAllSelected(true)
    expect(store.allSelected).toBe(true)
    expect(updateCartSelected).toHaveBeenCalledTimes(2)
  })

  it('setAllSelected 任一失败整体回滚', async () => {
    updateCartSelected
      .mockResolvedValueOnce({})
      .mockRejectedValueOnce(new Error('boom'))
    const store = useCartStore()
    const a = makeItem({ selected: true })
    const b = makeItem({ id: 2, selected: false })
    store.items = [a, b]
    await expect(store.setAllSelected(true)).rejects.toThrow('boom')
    expect(a.selected).toBe(true)
    expect(b.selected).toBe(false)
  })

  it('updateQuantity 成功写入新数量', async () => {
    updateCartQuantity.mockResolvedValue({})
    const store = useCartStore()
    const item = makeItem()
    store.items = [item]
    await store.updateQuantity(item, 3)
    expect(item.quantity).toBe(3)
    expect(updateCartQuantity).toHaveBeenCalledWith(1, 3)
  })

  it('updateQuantity 失败回滚数量', async () => {
    updateCartQuantity.mockRejectedValue(new Error('超出库存'))
    const store = useCartStore()
    const item = makeItem({ quantity: 2 })
    store.items = [item]
    await expect(store.updateQuantity(item, 9)).rejects.toThrow('超出库存')
    expect(item.quantity).toBe(2)
  })

  it('removeItem 成功后就地移除并同步角标', async () => {
    removeCart.mockResolvedValue({})
    const store = useCartStore()
    store.items = [makeItem(), makeItem({ id: 2 })]
    store.count = 2
    await store.removeItem(1)
    expect(store.items).toHaveLength(1)
    expect(store.items[0].id).toBe(2)
    expect(store.count).toBe(1)
  })

  it('clearSelected 仅移除已勾选条目并同步角标', async () => {
    clearSelectedCart.mockResolvedValue({})
    const store = useCartStore()
    store.items = [makeItem({ selected: true }), makeItem({ id: 2 })]
    store.count = 2
    await store.clearSelected()
    expect(store.items).toHaveLength(1)
    expect(store.items[0].id).toBe(2)
    expect(store.count).toBe(1)
  })

  it('reset 清空列表与角标（登出/会话失效）', () => {
    const store = useCartStore()
    store.items = [makeItem()]
    store.count = 9
    store.reset()
    expect(store.items).toHaveLength(0)
    expect(store.count).toBe(0)
  })
})
