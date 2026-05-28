import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getGoodsList, getGoodsDetail, getRecommendGoods, getHotGoods, getNewGoods, type Goods, type GoodsListParams } from '@/api/modules/goods'

export const useGoodsStore = defineStore('goods', () => {
  const goodsList = ref<Goods[]>([])
  const currentGoods = ref<Goods | null>(null)
  const recommendGoods = ref<Goods[]>([])
  const hotGoods = ref<Goods[]>([])
  const newGoods = ref<Goods[]>([])
  const total = ref(0)
  const loading = ref(false)

  const fetchGoodsList = async (params: GoodsListParams) => {
    loading.value = true
    try {
      const result = await getGoodsList(params)
      goodsList.value = result.list
      total.value = result.total
    } catch (error) {
      console.error('获取商品列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  const fetchGoodsDetail = async (id: number) => {
    loading.value = true
    try {
      const detail = await getGoodsDetail(id)
      currentGoods.value = detail
      return detail
    } catch (error) {
      console.error('获取商品详情失败:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  const fetchRecommendGoods = async (limit: number = 10) => {
    try {
      recommendGoods.value = await getRecommendGoods(limit)
    } catch (error) {
      console.error('获取推荐商品失败:', error)
    }
  }

  const fetchHotGoods = async (limit: number = 10) => {
    try {
      hotGoods.value = await getHotGoods(limit)
    } catch (error) {
      console.error('获取热门商品失败:', error)
    }
  }

  const fetchNewGoods = async (limit: number = 10) => {
    try {
      newGoods.value = await getNewGoods(limit)
    } catch (error) {
      console.error('获取新品商品失败:', error)
    }
  }

  const clearCurrentGoods = () => {
    currentGoods.value = null
  }

  return {
    goodsList,
    currentGoods,
    recommendGoods,
    hotGoods,
    newGoods,
    total,
    loading,
    fetchGoodsList,
    fetchGoodsDetail,
    fetchRecommendGoods,
    fetchHotGoods,
    fetchNewGoods,
    clearCurrentGoods
  }
})
