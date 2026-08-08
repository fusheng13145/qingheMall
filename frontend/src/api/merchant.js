import request from '../utils/request'

// ===================== 商家端（M6 平台化） =====================

// 店铺信息与入驻状态：GET /api/merchant/info
// 返回 MerchantDO 或 null（未申请开店）
export function getMerchantInfo() {
  return request.get('/merchant/info')
}

// 申请开店（已有账号升级为商家）：POST /api/merchant/apply（form-urlencoded）
// shopName 必填；shopLogo / shopDesc 选填。返回新创建的 MerchantDO（status=PENDING）
// 幂等：若已存在商家记录则直接返回，不重复创建
export function applyMerchant(shopName, shopLogo = '', shopDesc = '') {
  const params = new URLSearchParams()
  params.append('shopName', shopName)
  if (shopLogo) params.append('shopLogo', shopLogo)
  if (shopDesc) params.append('shopDesc', shopDesc)
  return request.post('/merchant/apply', params)
}

// 本店商品分页：GET /api/merchant/products?keyword=&status=&pageNum=&pageSize=
export function listMerchantProducts(pageNum = 1, pageSize = 10, keyword = '', status = '') {
  return request.get('/merchant/products', { params: { pageNum, pageSize, keyword, status } })
}

// 新增/编辑本店商品（含 SKU）：POST /api/merchant/product/save（@RequestBody Product）
export function saveMerchantProduct(data) {
  return request.post('/merchant/product/save', data)
}

// 上/下架：POST /api/merchant/product/toggle?productId=&status=ON|OFF
export function toggleMerchantProduct(productId, status) {
  return request.post('/merchant/product/toggle', null, { params: { productId, status } })
}

// 本店订单分页：GET /api/merchant/orders?status=&pageNum=&pageSize=
export function listMerchantOrders(pageNum = 1, pageSize = 10, status = '') {
  return request.get('/merchant/orders', { params: { pageNum, pageSize, status } })
}

// 发货：POST /api/merchant/order/ship?orderNumber=
export function merchantShip(orderNumber) {
  return request.post('/merchant/order/ship', null, { params: { orderNumber } })
}

// 处理退款：POST /api/merchant/order/refund/process?orderNumber=&approve=
export function merchantProcessRefund(orderNumber, approve = true) {
  return request.post('/merchant/order/refund/process', null, { params: { orderNumber, approve } })
}

// 店铺统计：GET /api/merchant/stats
// 返回 { productCount, orderCount, todayOrderCount, paidRevenue, todayRevenue }
export function getMerchantStats() {
  return request.get('/merchant/stats')
}
