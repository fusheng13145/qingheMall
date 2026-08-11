import request from '../utils/request'

// 创建订单：POST /api/order/add  { productDetailId, quantity, receiverName, receiverPhone, receiverAddress }
export function addOrder(payload) {
  return request.post('/order/add', payload)
}

// 批量下单（购物车结算）：POST /api/order/batchAdd  [{ productDetailId, quantity, receiver* }]
export function batchAddOrders(items) {
  return request.post('/order/batchAdd', items)
}

// 取消订单：POST /api/order/cancel?orderNumber=
export function cancelOrder(orderNumber) {
  return request.post('/order/cancel', null, { params: { orderNumber } })
}

// 订单列表（分页 + 状态筛选）：GET /api/order/list?status=&pageNum=&pageSize=（P2-12 统一 pageNum）
// status 为 OrderStatus 枚举值（如 WAIT_BUYER_PAY），缺省返回当前用户全部订单
// 返回 Paging<Order>：{ pageNum, pageSize, totalPage, totalCount, data }
export function listOrders(status, pageNum = 1, pageSize = 10) {
  return request.get('/order/list', {
    params: {
      pageNum,
      pageSize,
      ...(status ? { status } : {})
    }
  })
}

// 订单详情：GET /api/order/get?orderNumber=
export function getOrder(orderNumber) {
  return request.get('/order/get', {
    params: {
      orderNumber
    }
  })
}

// 确认收货：POST /api/order/confirmReceipt?orderNumber=（归属用户，仅已发货可确认）
export function confirmReceipt(orderNumber) {
  return request.post('/order/confirmReceipt', null, { params: { orderNumber } })
}

// 申请退款/退货：POST /api/order/refund/apply?orderNumber=&reason=&type=（P2-18）
// 已付款未发货 → 仅退款（REFUND_ONLY）；已发货/已完成 → 退货退款（RETURN_REFUND）
// type 缺省时后端按订单状态自动推导；reason 必填
export function applyRefund(orderNumber, reason, type) {
  return request.post('/order/refund/apply', null, {
    params: {
      orderNumber,
      reason,
      ...(type ? { type } : {})
    }
  })
}
