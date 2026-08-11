import request from '../utils/request'

// 提交评价：POST /api/comment/add  { productId, orderNumber, rating, content }
export function addComment(payload) {
  return request.post('/comment/add', payload)
}

// 商品评价列表（分页）：GET /api/comment/product?productId=&pageNum=&pageSize=（P2-12 统一 pageNum）
// 返回 Paging<Comment>：{ pageNum, pageSize, totalPage, totalCount, data }
export function listProductComments(productId, pageNum = 1, pageSize = 10) {
  return request.get('/comment/product', {
    params: { productId, pageNum, pageSize }
  })
}

// 商品评分汇总：GET /api/comment/summary?productId=  → { avgRating, ratingCount }
export function getCommentSummary(productId) {
  return request.get('/comment/summary', {
    params: { productId }
  })
}

// 订单是否已评价：GET /api/comment/orderStatus?orderNumber=  → { commented }
export function getOrderCommentStatus(orderNumber) {
  return request.get('/comment/orderStatus', {
    params: { orderNumber }
  })
}
