import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCartList, addToCart as addApi, updateCartQuantity as updateApi, removeFromCart as removeApi, selectCartItem as selectApi } from '@/api/modules/cart'

export interface CartItem {
  id: number
  goodsId: number
  goodsName: string
  price: number
  image: string
  quantity: number
  stock: number
  selected: boolean
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const loading = ref(false)

  const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))

  const totalAmount = computed(() => {
    return items.value
      .filter(item => item.selected)
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
  })

  const selectedCount = computed(() => {
    return items.value.filter(item => item.selected).reduce((sum, item) => sum + item.quantity, 0)
  })

  const selectedItems = computed(() => items.value.filter(item => item.selected))

  const fetchCartList = async () => {
    loading.value = true
    try {
      const list = await getCartList()
      items.value = list
    } catch (error) {
      console.error('获取购物车列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  const addItem = async (goodsId: number, quantity: number = 1) => {
    await addApi({ goodsId, quantity })
    await fetchCartList()
  }

  const updateQuantity = async (id: number, quantity: number) => {
    await updateApi({ id, quantity })
    const item = items.value.find(i => i.id === id)
    if (item) {
      item.quantity = quantity
    }
  }

  const removeItem = async (ids: number[]) => {
    await removeApi(ids)
    items.value = items.value.filter(item => !ids.includes(item.id))
  }

  const selectItem = async (id: number, selected: boolean) => {
    await selectApi({ id, selected })
    const item = items.value.find(i => i.id === id)
    if (item) {
      item.selected = selected
    }
  }

  const selectAll = (selected: boolean) => {
    items.value.forEach(item => item.selected = selected)
  }

  const clearCart = () => {
    items.value = []
  }

  return {
    items,
    loading,
    totalCount,
    totalAmount,
    selectedCount,
    selectedItems,
    fetchCartList,
    addItem,
    updateQuantity,
    removeItem,
    selectItem,
    selectAll,
    clearCart
  }
}, {
  persist: {
    key: 'qinghe-cart',
    paths: ['items']
  }
})
