import request from '../utils/request'

// ===================== 店铺主页（A3，v1.5） =====================

// 店铺聚合页：GET /api/shop/{merchantId}?pageNum=&pageSize=&keyword=
// { shop: {id,shopName,shopLogo,shopDesc}, stats: {productCount,avgRating,totalSales},
//   products: Paging<Product>, coupons: [CouponDO], seckills: [SeckillActivityDO] }
export function getShopHome(merchantId, pageNum = 1, pageSize = 12, keyword = '') {
  return request.get(`/shop/${merchantId}`, { params: { pageNum, pageSize, keyword } })
}
