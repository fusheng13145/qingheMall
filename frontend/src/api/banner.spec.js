import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import {
  listBanners,
  adminListBanners,
  adminCreateBanner,
  adminUpdateBanner,
  adminDeleteBanner,
  adminToggleBanner
} from './banner'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/banner（D2，v1.7）', () => {
  it('listBanners 请求顾客端公开列表', async () => {
    const list = [{ id: 'b1', title: '开学季', status: 'ON' }]
    request.get.mockResolvedValue({ data: list })
    const res = await listBanners()

    expect(request.get).toHaveBeenCalledWith('/banner/list')
    expect(res).toEqual({ data: list })
  })

  it('adminListBanners 携带分页参数', async () => {
    await adminListBanners(2, 50)

    expect(request.get).toHaveBeenCalledWith('/admin/banner/list', { params: { pageNum: 2, pageSize: 50 } })
  })

  it('adminCreateBanner 以请求体提交运营位', async () => {
    const banner = { title: '开学季', image: '/uploads/b.jpg', sortOrder: 1 }
    request.post.mockResolvedValue({ data: { id: 'b1', ...banner } })
    const res = await adminCreateBanner(banner)

    expect(request.post).toHaveBeenCalledWith('/admin/banner/create', banner)
    expect(res.data.id).toBe('b1')
  })

  it('adminUpdateBanner 以请求体提交更新', async () => {
    const banner = { id: 'b1', title: '新标题', image: '/uploads/b2.jpg' }
    await adminUpdateBanner(banner)

    expect(request.post).toHaveBeenCalledWith('/admin/banner/update', banner)
  })

  it('adminDeleteBanner 以 query 参数提交 bannerId', async () => {
    await adminDeleteBanner('b1')

    expect(request.post).toHaveBeenCalledWith('/admin/banner/delete', null, { params: { bannerId: 'b1' } })
  })

  it('adminToggleBanner 以 query 参数提交上下架状态', async () => {
    await adminToggleBanner('b1', 'OFF')
    expect(request.post).toHaveBeenCalledWith('/admin/banner/toggle', null, { params: { bannerId: 'b1', status: 'OFF' } })

    await adminToggleBanner('b1', 'ON')
    expect(request.post).toHaveBeenCalledWith('/admin/banner/toggle', null, { params: { bannerId: 'b1', status: 'ON' } })
  })
})
