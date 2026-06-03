import request from '../utils/request'

export function payOrder(paymentParam) {
  return request.post('/pay/pay', paymentParam)
}
