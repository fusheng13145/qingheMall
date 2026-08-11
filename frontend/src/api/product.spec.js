import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import { pageQuery, listBrands, get, getProductDetails } from './product'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/product', () => {
  it('pageQuery 携带分页与筛选参数', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    const res = await pageQuery(2, 8, { keyword: '青禾', brand: 'Nike', sort: 'price_asc' })

    expect(request.get).toHaveBeenCalledWith('/product/page', {
      params: { pageNum: 2, pageSize: 8, keyword: '青禾', brand: 'Nike', sort: 'price_asc' }
    })
    expect(res).toEqual({ data: { data: [] } })
  })

  it('pageQuery 缺省筛选参数时字段为 undefined', async () => {
    request.get.mockResolvedValue({ data: {} })
    await pageQuery(1, 10)

    const config = request.get.mock.calls[0][1]
    expect(config.params.pageNum).toBe(1)
    expect(config.params.pageSize).toBe(10)
    expect(config.params.keyword).toBeUndefined()
    expect(config.params.brand).toBeUndefined()
    expect(config.params.sort).toBeUndefined()
  })

  it('listBrands 调用品牌列表接口并透传返回值', async () => {
    request.get.mockResolvedValue({ data: ['Nike'] })
    const res = await listBrands()

    expect(request.get).toHaveBeenCalledWith('/product/brands')
    expect(res).toEqual({ data: ['Nike'] })
  })

  it('get 按 productId 查询商品详情', async () => {
    request.get.mockResolvedValue({ data: { id: 5 } })
    const res = await get(5)

    expect(request.get).toHaveBeenCalledWith('/product/get', { params: { productId: 5 } })
    expect(res).toEqual({ data: { id: 5 } })
  })

  it('getProductDetails 按 productId 查询规格列表', async () => {
    request.get.mockResolvedValue({ data: [] })
    const res = await getProductDetails(5)

    expect(request.get).toHaveBeenCalledWith('/productdetail/productId', { params: { productId: 5 } })
    expect(res).toEqual({ data: [] })
  })
})
