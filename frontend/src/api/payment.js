import request from '../utils/request'

// 创建支付：POST /api/pay/create { orderNumber, payType }
// 返回 { channel, mock, codeUrl, orderNumber }：mock=true 走模拟支付；否则 codeUrl 为二维码 base64
export function createPay(orderNumber, payType) {
  return request.post('/pay/create', { orderNumber, payType })
}

// 模拟支付：POST /api/pay/mockPay { orderNumber }
export function mockPay(orderNumber) {
  return request.post('/pay/mockPay', { orderNumber })
}

// 查询支付状态：GET /api/pay/query?orderNumber= → 订单状态枚举
export function queryPay(orderNumber) {
  return request.get('/pay/query', { params: { orderNumber } })
}
