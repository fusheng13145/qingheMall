import { defineStore } from 'pinia'
import { ref } from 'vue'
import { countCart } from '../api/cart'

export const useCartStore = defineStore('cart', () => {
  // 购物车条目数（顶部角标）
  const count = ref(0)

  async function refreshCount() {
    try {
      const res = await countCart()
      count.value = res.data || 0
    } catch (e) {
      count.value = 0
    }
  }

  function reset() {
    count.value = 0
  }

  return {
    count,
    refreshCount,
    reset
  }
})
