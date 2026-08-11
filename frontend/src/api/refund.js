import request from '../utils/request'

// 我的退款申请列表（倒序）：GET /api/refund/mine
// 返回 RefundRequest[]：{ id, orderNumber, type, reason, status, reviewComment, gmtCreated }
export function listMyRefunds() {
  return request.get('/refund/mine')
}

// 指定订单的退款申请历史：GET /api/refund/order?orderNumber=（本人/归属商家/管理员）
export function listOrderRefunds(orderNumber) {
  return request.get('/refund/order', {
    params: {
      orderNumber
    }
  })
}
