import request from '../utils/request'

// 加入购物车：POST /api/cart/add { productDetailId, quantity }
export function addCart(productDetailId, quantity) {
  return request.post('/cart/add', { productDetailId, quantity })
}

// 修改数量：POST /api/cart/updateQuantity { id, quantity }
export function updateCartQuantity(id, quantity) {
  return request.post('/cart/updateQuantity', { id, quantity })
}

// 修改勾选：POST /api/cart/updateSelected { id, selected }
export function updateCartSelected(id, selected) {
  return request.post('/cart/updateSelected', { id, selected })
}

// 删除条目：POST /api/cart/remove?id=
export function removeCart(id) {
  return request.post('/cart/remove', null, { params: { id } })
}

// 购物车列表：GET /api/cart/list
export function listCart() {
  return request.get('/cart/list')
}

// 购物车条目数：GET /api/cart/count
export function countCart() {
  return request.get('/cart/count')
}

// 清空已勾选条目（结算完成后）：POST /api/cart/clearSelected
export function clearSelectedCart() {
  return request.post('/cart/clearSelected')
}
