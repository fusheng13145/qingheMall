import request from '../utils/request'

// 领取券：POST /api/coupon/claim?couponId=
export function claimCoupon(couponId) {
  return request.post('/coupon/claim', null, { params: { couponId } })
}

// 我的券：GET /api/coupon/mine?status=（UNUSED/USED/EXPIRED/RELEASED，空串查全部）
export function myCoupons(status = '') {
  return request.get('/coupon/mine', { params: { status } })
}

// 领券中心：GET /api/coupon/list（上架且在有效期内的券）
export function listCoupons() {
  return request.get('/coupon/list')
}

// 结算可用券：GET /api/coupon/available?amount=（整单原价，返回按优惠额降序）
export function availableCoupons(amount) {
  return request.get('/coupon/available', { params: { amount } })
}
