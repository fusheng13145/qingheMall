import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// vi.hoisted：保证 mock 工厂与测试体内共享同一个 countCart 引用
const { countCart } = vi.hoisted(() => ({ countCart: vi.fn() }))

vi.mock('../api/cart', () => ({
  countCart: (...args) => countCart(...args)
}))

import { useCartStore } from './cart'

describe('cart store 购物车状态', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    countCart.mockReset()
  })

  it('refreshCount 成功时写入条目数', async () => {
    countCart.mockResolvedValue({ data: 5 })
    const store = useCartStore()
    await store.refreshCount()
    expect(store.count).toBe(5)
    expect(countCart).toHaveBeenCalledTimes(1)
  })

  it('refreshCount 请求失败时回退为 0（不影响顶部角标）', async () => {
    countCart.mockRejectedValue(new Error('network'))
    const store = useCartStore()
    store.count = 3
    await store.refreshCount()
    expect(store.count).toBe(0)
  })

  it('reset 将条目数归零', () => {
    const store = useCartStore()
    store.count = 9
    store.reset()
    expect(store.count).toBe(0)
  })
})
