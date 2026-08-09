import request from '../utils/request'

// 数据看板统计：GET /api/admin/dashboard
// 返回 { productCount, orderCount, userCount, totalRevenue }
export function getDashboard() {
  return request.get('/admin/dashboard')
}

// 销售日报（P3 报表）：GET /api/admin/report?days=7
// 返回 [{ day, orderCount, salesAmount }]
export function getSalesReport(days = 7) {
  return request.get('/admin/report', { params: { days } })
}

// 商品列表（管理端全量分页接口，含下架商品）
export function getProductList(pageNum = 1, pageSize = 20, keyword = '') {
  return request.get('/admin/product/list', { params: { pagination: pageNum, pageSize, keyword } })
}

export function addProduct(data) {
  return request.post('/admin/product/add', data)
}

export function updateProduct(data) {
  return request.post('/admin/product/update', data)
}

export function deleteProduct(id) {
  return request.post('/admin/product/delete', null, { params: { id } })
}

// 订单列表（分页，P1-11）：GET /api/admin/order/list?pagination=&pageSize=&status=
export function getOrderList(pagination = 1, pageSize = 20, status = '') {
  return request.get('/admin/order/list', { params: { pagination, pageSize, status } })
}

export function updateOrderStatus(orderNumber, status) {
  return request.post('/admin/order/updateStatus', null, { params: { orderNumber, status } })
}

// 发货：POST /api/admin/order/ship?orderNumber=（管理员，仅已付款可发货）
export function shipOrder(orderNumber) {
  return request.post('/admin/order/ship', null, { params: { orderNumber } })
}

// 处理退款：POST /api/admin/order/refund/process?orderNumber=&approve=（管理员，退款中→已退款/回退已付款）
export function processRefund(orderNumber, approve = true) {
  return request.post('/admin/order/refund/process', null, { params: { orderNumber, approve } })
}

// 用户列表（分页，P1-11）：GET /api/admin/user/list?pagination=&pageSize=
export function getUserList(pagination = 1, pageSize = 20) {
  return request.get('/admin/user/list', { params: { pagination, pageSize } })
}

export function updateUserRole(id, role) {
  return request.post('/admin/user/updateRole', null, { params: { id, role } })
}

// ===================== 优惠券管理（A2） =====================
// 创建券模板：POST /api/admin/coupon/create
export function createCoupon(data) {
  return request.post('/admin/coupon/create', data)
}

// 券模板分页列表：GET /api/admin/coupon/list?pageNum=&pageSize=
export function listCoupons(pageNum = 1, pageSize = 10) {
  return request.get('/admin/coupon/list', { params: { pageNum, pageSize } })
}

// 修改券（仅下架态可改）：POST /api/admin/coupon/update
export function updateCoupon(data) {
  return request.post('/admin/coupon/update', data)
}

// 上/下架：POST /api/admin/coupon/toggle?couponId=&status=
export function toggleCoupon(couponId, status) {
  return request.post('/admin/coupon/toggle', null, { params: { couponId, status } })
}

// ===================== 秒杀管理（A3） =====================
// 创建秒杀活动：POST /api/admin/seckill/create（@RequestBody SeckillActivityDO）
export function createSeckill(data) {
  return request.post('/admin/seckill/create', data)
}

// 秒杀活动分页列表：GET /api/admin/seckill/list?status=&pageNum=&pageSize=
// 返回 Paging<SeckillActivityDO>，前端取 res.data.data
export function listSeckills(pageNum = 1, pageSize = 10, status = '') {
  return request.get('/admin/seckill/list', { params: { pageNum, pageSize, status } })
}

// 上/下架/结束活动：POST /api/admin/seckill/toggle?activityId=&status=（ONGOING/CLOSED/ENDED）
export function toggleSeckill(activityId, status) {
  return request.post('/admin/seckill/toggle', null, { params: { activityId, status } })
}

// ===================== 商家入驻审核（M6） =====================
// 商家列表：GET /api/admin/merchant/list?status=&pageNum=&pageSize=
export function listMerchants(pageNum = 1, pageSize = 10, status = '') {
  return request.get('/admin/merchant/list', { params: { pageNum, pageSize, status } })
}

// 审核商家：POST /api/admin/merchant/audit?merchantId=&approve=&reason=（approve=false 必须填 reason）
export function auditMerchant(merchantId, approve = true, reason = '') {
  return request.post('/admin/merchant/audit', null, { params: { merchantId, approve, reason } })
}
