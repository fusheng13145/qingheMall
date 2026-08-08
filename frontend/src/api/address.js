import request from '../utils/request'

// 地址列表：GET /api/address/list
export function listAddress() {
  return request.get('/address/list')
}

// 默认地址：GET /api/address/default
export function getDefaultAddress() {
  return request.get('/address/default')
}

// 新增地址：POST /api/address/add
export function addAddress(data) {
  return request.post('/address/add', data)
}

// 更新地址：POST /api/address/update
export function updateAddress(data) {
  return request.post('/address/update', data)
}

// 删除地址：POST /api/address/delete?id=
export function deleteAddress(id) {
  return request.post('/address/delete', null, { params: { id } })
}

// 设为默认：POST /api/address/setDefault?id=
export function setDefaultAddress(id) {
  return request.post('/address/setDefault', null, { params: { id } })
}
