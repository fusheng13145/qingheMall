import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getOrderList, getOrderDetail, cancelOrder, confirmReceive, deleteOrder, type OrderItem } from '@/api/modules/order'

export const useOrderStore = defineStore('order', () => {
  const orderList = ref<OrderItem[]>([])
  const currentOrder = ref<OrderItem | null>(null)
  const total = ref(0)
  const loading = ref(false)

  const fetchOrderList = async (params: { page?: number; pageSize?: number; status?: number } = {}) => {
    loading.value = true
    try {
      const result = await getOrderList(params)
      orderList.value = result.list
      total.value = result.total
    } catch (error) {
      console.error('获取订单列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  const fetchOrderDetail = async (id: number) => {
    loading.value = true
    try {
      const detail = await getOrderDetail(id)
      currentOrder.value = detail
      return detail
    } catch (error) {
      console.error('获取订单详情失败:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  const cancelOrderById = async (id: number, reason: string) => {
    await cancelOrder(id, reason)
    const order = orderList.value.find(o => o.id === id)
    if (order) {
      order.status = -1
      order.statusText = '已取消'
    }
  }

  const confirmReceiveOrder = async (id: number) => {
    await confirmReceive(id)
    const order = orderList.value.find(o => o.id === id)
    if (order) {
      order.status = 4
      order.statusText = '已完成'
    }
  }

  const removeOrder = async (id: number) => {
    await deleteOrder(id)
    orderList.value = orderList.value.filter(o => o.id !== id)
  }

  const updateOrderInList = (updatedOrder: OrderItem) => {
    const index = orderList.value.findIndex(o => o.id === updatedOrder.id)
    if (index !== -1) {
      orderList.value[index] = updatedOrder
    }
  }

  return {
    orderList,
    currentOrder,
    total,
    loading,
    fetchOrderList,
    fetchOrderDetail,
    cancelOrderById,
    confirmReceiveOrder,
    removeOrder,
    updateOrderInList
  }
})
