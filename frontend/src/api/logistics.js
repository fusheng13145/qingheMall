import request from '../utils/request'

// 物流跟踪：GET /api/logistics/track?orderNumber=
// 返回 Logistics：{ orderNumber, company, trackingNumber, status, gmtCreated, traces: [{ description, traceTime }] }
// 状态机：SHIPPED → IN_TRANSIT → DELIVERING → SIGNED
export function trackLogistics(orderNumber) {
  return request.get('/logistics/track', {
    params: {
      orderNumber
    }
  })
}
