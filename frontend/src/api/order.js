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

// 订单列表（分页 + 状态筛选）：GET /api/order/list?status=&pagination=&pageSize=
// status 为 OrderStatus 枚举值（如 WAIT_BUYER_PAY），缺省返回当前用户全部订单
// 返回 Paging<Order>：{ pageNum, pageSize, totalPage, totalCount, data }
export function listOrders(status, pagination = 1, pageSize = 10) {
  return request.get('/order/list', {
    params: {
      pagination,
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

// 申请退款：POST /api/order/refund/apply?orderNumber=（归属用户，仅未发货的已付款可发起）
export function applyRefund(orderNumber) {
  return request.post('/order/refund/apply', null, { params: { orderNumber } })
}
