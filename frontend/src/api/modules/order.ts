import request from '../request'

export interface Address {
  id: number
  name: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
}

export interface OrderItem {
  id: number
  orderNo: string
  status: number
  statusText: string
  totalAmount: number
  payAmount: number
  freight: number
  createdAt: string
  payAt?: string
  shipAt?: string
  receiveAt?: string
  items: OrderGoodsItem[]
  address: Address
}

export interface OrderGoodsItem {
  id: number
  goodsId: number
  goodsName: string
  image: string
  price: number
  quantity: number
}

export interface CreateOrderParams {
  addressId: number
  items: { goodsId: number; quantity: number; cartId?: number }[]
  remark?: string
}

export const createOrder = (data: CreateOrderParams) => {
  return request.post<{ orderId: number; orderNo: string }>('/order/create', data)
}

export const getOrderList = (params: { page?: number; pageSize?: number; status?: number }) => {
  return request.get<{ list: OrderItem[]; total: number }>('/order/list', params)
}

export const getOrderDetail = (id: number) => {
  return request.get<OrderItem>(`/order/detail/${id}`)
}

export const getOrderDetailByNo = (orderNo: string) => {
  return request.get<OrderItem>(`/order/detail/${orderNo}`)
}

export const cancelOrder = (id: number, reason: string) => {
  return request.post(`/order/cancel/${id}`, { reason })
}

export const confirmReceive = (id: number) => {
  return request.post(`/order/confirmReceive/${id}`)
}

export const deleteOrder = (id: number) => {
  return request.delete(`/order/delete/${id}`)
}

export const getAddressList = () => {
  return request.get<Address[]>('/address/list')
}

export const addAddress = (data: Omit<Address, 'id'>) => {
  return request.post('/address/add', data)
}

export const updateAddress = (data: Address) => {
  return request.put('/address/update', data)
}

export const deleteAddress = (id: number) => {
  return request.delete(`/address/delete/${id}`)
}

export const setDefaultAddress = (id: number) => {
  return request.post(`/address/setDefault/${id}`)
}
