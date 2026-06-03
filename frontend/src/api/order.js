import request from '../utils/request'
import { getMockOrders } from './mock'

const USE_MOCK = true

export function addOrder(productDetailId) {
  return request.post('/order/add', {
    productDetailId
  })
}

export function queryRecentPaySuccess(param) {
  if (USE_MOCK) {
    return Promise.resolve({ data: getMockOrders() })
  }
  return request.post('/order/queryrecentpaysuccess', param)
}
