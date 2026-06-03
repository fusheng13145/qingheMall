import request from '../utils/request'
import { mockProducts } from './mock'

const USE_MOCK = true

// Mock 数据
const mockDashboard = {
  productCount: 12,
  orderCount: 156,
  userCount: 89,
  totalRevenue: 52680.50,
  recentOrders: [
    { id: 'o1', orderNumber: 'QH20240601001', userName: '张三', productName: '陶瓷花瓶', totalPrice: 168.00, status: 'TRADE_PAID_SUCCESS', createTime: '2024-06-01 14:30:00' },
    { id: 'o2', orderNumber: 'QH20240531002', userName: '李四', productName: '羊毛针织毯', totalPrice: 329.00, status: 'WAIT_BUYER_PAY', createTime: '2024-05-31 10:15:00' },
    { id: 'o3', orderNumber: 'QH20240530003', userName: '王五', productName: '手冲咖啡壶', totalPrice: 258.00, status: 'TRADE_PAID_SUCCESS', createTime: '2024-05-30 09:20:00' },
    { id: 'o4', orderNumber: 'QH20240529004', userName: '赵六', productName: '真皮笔记本', totalPrice: 128.00, status: 'TRADE_CLOSED', createTime: '2024-05-29 16:45:00' },
    { id: 'o5', orderNumber: 'QH20240528005', userName: '孙七', productName: '智能香薰机', totalPrice: 199.00, status: 'TRADE_PAID_SUCCESS', createTime: '2024-05-28 20:10:00' }
  ]
}

const mockUsers = [
  { id: 1, userName: 'admin', nickName: '管理员', role: 'ADMIN', gmtCreated: '2024-01-01 00:00:00' },
  { id: 2, userName: 'zhangsan', nickName: '张三', role: 'USER', gmtCreated: '2024-03-15 10:30:00' },
  { id: 3, userName: 'lisi', nickName: '李四', role: 'USER', gmtCreated: '2024-04-02 14:20:00' },
  { id: 4, userName: 'wangwu', nickName: '王五', role: 'USER', gmtCreated: '2024-04-18 09:15:00' },
  { id: 5, userName: 'zhaoliu', nickName: '赵六', role: 'USER', gmtCreated: '2024-05-01 16:45:00' },
  { id: 6, userName: 'sunqi', nickName: '孙七', role: 'USER', gmtCreated: '2024-05-20 11:00:00' }
]

export function getDashboard() {
  if (USE_MOCK) {
    return Promise.resolve({ data: { code: 200, data: mockDashboard } })
  }
  return request.get('/admin/dashboard')
}

export function getProductList() {
  if (USE_MOCK) {
    return Promise.resolve({
      data: {
        code: 200,
        data: {
          records: mockProducts,
          total: mockProducts.length
        }
      }
    })
  }
  return request.get('/product/page', { params: { pagination: 1, pageSize: 50 } })
}

export function addProduct(data) {
  if (USE_MOCK) {
    return Promise.resolve({ data: { code: 200, data: { ...data, id: 'new_' + Date.now() } } })
  }
  return request.post('/admin/product/add', data)
}

export function updateProduct(data) {
  if (USE_MOCK) {
    return Promise.resolve({ data: { code: 200, data } })
  }
  return request.post('/admin/product/update', data)
}

export function deleteProduct(id) {
  if (USE_MOCK) {
    return Promise.resolve({ data: { code: 200 } })
  }
  return request.post('/admin/product/delete', null, { params: { id } })
}

export function getOrderList() {
  if (USE_MOCK) {
    return Promise.resolve({ data: { code: 200, data: mockDashboard.recentOrders } })
  }
  return request.get('/admin/order/list')
}

export function updateOrderStatus(orderNumber, status) {
  if (USE_MOCK) {
    return Promise.resolve({ data: { code: 200 } })
  }
  return request.post('/admin/order/updateStatus', null, { params: { orderNumber, status } })
}

export function getUserList() {
  if (USE_MOCK) {
    return Promise.resolve({ data: { code: 200, data: mockUsers } })
  }
  return request.get('/admin/user/list')
}

export function updateUserRole(id, role) {
  if (USE_MOCK) {
    return Promise.resolve({ data: { code: 200 } })
  }
  return request.post('/admin/user/updateRole', null, { params: { id, role } })
}
