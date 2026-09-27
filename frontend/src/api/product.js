import request from '../utils/request'

// 商品分页查询：GET /api/product/page?pageNum=&pageSize=&keyword=&brand=&sort=（P2-12 统一 pageNum）
// sort: sales_desc(销量) | price_asc(价格↑) | price_desc(价格↓) | 空(默认上新)
export function pageQuery(pageNum, pageSize, params = {}) {
  return request.get('/product/page', {
    params: {
      pageNum,
      pageSize,
      keyword: params.keyword || undefined,
      brand: params.brand || undefined,
      sort: params.sort || undefined
    }
  })
}

// 推荐位（D2，v1.7）：GET /api/product/recommend?scene=&limit=
// scene: hot(热销，销量降序) | new(新品，上架时间降序)
export function recommend(scene, limit = 8) {
  return request.get('/product/recommend', { params: { scene, limit } })
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
