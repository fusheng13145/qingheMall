import request from '../request'

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

export const getCartList = () => {
  return request.get<CartItem[]>('/cart/list')
}

export const addToCart = (data: { goodsId: number; quantity: number }) => {
  return request.post('/cart/add', data)
}

export const updateCartQuantity = (data: { id: number; quantity: number }) => {
  return request.put('/cart/quantity', data)
}

export const removeFromCart = (ids: number[]) => {
  return request.delete('/cart/remove', { ids })
}

export const selectCartItem = (data: { id: number; selected: boolean }) => {
  return request.put('/cart/select', data)
}

export const selectAllCart = (selected: boolean) => {
  return request.post('/cart/selectAll', { selected })
}

export const clearCart = () => {
  return request.delete('/cart/clear')
}

export const getCartCount = () => {
  return request.get<{ count: number }>('/cart/count')
}
