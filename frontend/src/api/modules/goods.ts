import request from '../request'

export interface Goods {
  id: number
  name: string
  price: number
  originalPrice?: number
  images: string[]
  thumbnail?: string
  description?: string
  stock: number
  sales: number
  categoryId?: number
  categoryName?: string
  tags?: string[]
  rating?: number
}

export interface GoodsListParams {
  page?: number
  pageSize?: number
  categoryId?: number
  keyword?: string
  sortBy?: string
  order?: 'asc' | 'desc'
}

export const getGoodsList = (params: GoodsListParams) => {
  return request.get<{ list: Goods[]; total: number }>('/goods/list', params)
}

export const getGoodsDetail = (id: number) => {
  return request.get<Goods>(`/goods/detail/${id}`)
}

export const getRecommendGoods = (limit: number = 10) => {
  return request.get<Goods[]>('/goods/recommend', { limit })
}

export const getHotGoods = (limit: number = 10) => {
  return request.get<Goods[]>('/goods/hot', { limit })
}

export const getNewGoods = (limit: number = 10) => {
  return request.get<Goods[]>('/goods/new', { limit })
}

export const getCategoryList = () => {
  return request.get('/category/list')
}

export const searchGoods = (params: { keyword: string; page?: number; pageSize?: number }) => {
  return request.get<{ list: Goods[]; total: number }>('/goods/search', params)
}
