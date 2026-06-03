import request from '../utils/request'
import { getMockProductPage, getMockProduct, getMockProductDetails } from './mock'

const USE_MOCK = true

export function pageQuery(pagination, pageSize) {
  if (USE_MOCK) {
    return Promise.resolve({ data: getMockProductPage(pagination, pageSize) })
  }
  return request.get('/product/page', {
    params: {
      pagination,
      pageSize
    }
  })
}

export function get(productId) {
  if (USE_MOCK) {
    return Promise.resolve({ data: getMockProduct(productId) })
  }
  return request.get('/product/get', {
    params: {
      productId
    }
  })
}

export function getProductDetails(productId) {
  if (USE_MOCK) {
    return Promise.resolve({ data: getMockProductDetails(productId) })
  }
  return request.get('/productdetail/productId', {
    params: {
      productId
    }
  })
}
