import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  countCart,
  listCart,
  updateCartQuantity,
  updateCartSelected,
  removeCart,
  clearSelectedCart
} from '../api/cart'

/**
 * 购物车状态仓库（P2-15：集中管理）。
 *
 * 此前列表数据分散在 Cart.vue / Checkout.vue 各自请求与维护，
 * 角标与列表易不一致。现将列表、勾选态、角标统一收敛到本 store：
 * - Cart.vue / Checkout.vue 共享同一份 items，不再各自 listCart；
 * - 变更动作（勾选/数量/删除/清空已选）在此实现「乐观更新 + 失败回滚」，
 *   成功后就地同步角标，省去额外的 count 请求；
 * - AppHeader 角标与登出清理（user store / 401 拦截）统一走本 store。
 */
export const useCartStore = defineStore('cart', () => {
  /** 购物车条目列表（服务端为准，fetchItems 拉取） */
  const items = ref([])
  const loading = ref(false)
  /** 顶部角标条目数 */
  const count = ref(0)

  const selectedItems = computed(() => items.value.filter(i => i.selected))
  const allSelected = computed(() => items.value.length > 0 && items.value.every(i => i.selected))
  const selectedTotalPrice = computed(() =>
    selectedItems.value.reduce((sum, i) => sum + (i.price || 0) * i.quantity, 0))

  /** 拉取购物车列表并同步角标（列表与数量一次请求搞定） */
  async function fetchItems() {
    loading.value = true
    try {
      const res = await listCart()
      items.value = res.data || []
      count.value = items.value.length
    } finally {
      loading.value = false
    }
  }

  /** 仅刷新角标（不拉列表的轻量场景，如加购后） */
  async function refreshCount() {
    try {
      const res = await countCart()
      count.value = res.data || 0
    } catch (e) {
      count.value = 0
    }
  }

  /** 切换单条勾选：乐观更新，失败回滚并抛出异常供调用方提示 */
  async function toggleSelected(item) {
    const next = !item.selected
    item.selected = next
    try {
      await updateCartSelected(item.id, next)
    } catch (e) {
      item.selected = !next
      throw e
    }
  }

  /** 全选/全不选：批量乐观更新，任一失败整体回滚 */
  async function setAllSelected(next) {
    const rollback = items.value.map(i => i.selected)
    items.value.forEach(i => { i.selected = next })
    try {
      await Promise.all(items.value.map(i => updateCartSelected(i.id, next)))
    } catch (e) {
      items.value.forEach((i, idx) => { i.selected = rollback[idx] })
      throw e
    }
  }

  /** 修改数量：乐观更新，失败回滚 */
  async function updateQuantity(item, quantity) {
    const old = item.quantity
    item.quantity = quantity
    try {
      await updateCartQuantity(item.id, quantity)
    } catch (e) {
      item.quantity = old
      throw e
    }
  }

  /** 删除条目：成功后就地移除并同步角标 */
  async function removeItem(id) {
    await removeCart(id)
    items.value = items.value.filter(i => i.id !== id)
    count.value = items.value.length
  }

  /** 清空已勾选条目（下单成功后）：成功后就地移除并同步角标 */
  async function clearSelected() {
    await clearSelectedCart()
    items.value = items.value.filter(i => !i.selected)
    count.value = items.value.length
  }

  /** 登出/会话失效时清空本地购物车状态，防止跨用户残留 */
  function reset() {
    items.value = []
    count.value = 0
  }

  return {
    items,
    loading,
    count,
    selectedItems,
    allSelected,
    selectedTotalPrice,
    fetchItems,
    refreshCount,
    toggleSelected,
    setAllSelected,
    updateQuantity,
    removeItem,
    clearSelected,
    reset
  }
})
