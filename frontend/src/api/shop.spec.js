import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import { getShopHome } from './shop'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/shop（A3，v1.5）', () => {
  it('getShopHome 以路径参数携带店铺 ID 请求聚合页并透传返回值', async () => {
    const home = { shop: { id: 18, shopName: '冒烟店铺' }, stats: { productCount: 3 }, products: { data: [] } }
    request.get.mockResolvedValue({ data: home })
    const res = await getShopHome(18)

    expect(request.get).toHaveBeenCalledWith('/shop/18', { params: { pageNum: 1, pageSize: 12, keyword: '' } })
    expect(res).toEqual({ data: home })
  })

  it('getShopHome 透传分页与关键词参数', async () => {
    await getShopHome(7, 2, 6, '跑鞋')

    expect(request.get).toHaveBeenCalledWith('/shop/7', { params: { pageNum: 2, pageSize: 6, keyword: '跑鞋' } })
  })
})
