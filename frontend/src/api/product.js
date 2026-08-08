import request from '../utils/request'

// 商品分页查询：GET /api/product/page?pagination=&pageSize=&keyword=&brand=&sort=
// sort: sales_desc(销量) | price_asc(价格↑) | price_desc(价格↓) | 空(默认上新)
export function pageQuery(pagination, pageSize, params = {}) {
  return request.get('/product/page', {
    params: {
      pagination,
      pageSize,
      keyword: params.keyword || undefined,
      brand: params.brand || undefined,
      sort: params.sort || undefined
    }
  })
}

// 品牌列表（筛选下拉）：GET /api/product/brands
export function listBrands() {
  return request.get('/product/brands')
}

// 商品详情：GET /api/product/get?productId=
export function get(productId) {
  return request.get('/product/get', {
    params: {
      productId
    }
  })
}

// 商品规格列表：GET /api/productdetail/productId?productId=
export function getProductDetails(productId) {
  return request.get('/productdetail/productId', {
    params: {
      productId
    }
  })
}
