import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import { listSeckillActivities, getSeckillActivity, createSeckillOrder } from './seckill'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/seckill', () => {
  it('listSeckillActivities 查询活动列表并透传返回值', async () => {
    request.get.mockResolvedValue({ data: [{ id: 'a1' }] })
    const res = await listSeckillActivities()

    expect(request.get).toHaveBeenCalledWith('/seckill/activities')
    expect(res).toEqual({ data: [{ id: 'a1' }] })
  })

  it('getSeckillActivity 拼接活动 id 到路径', async () => {
    request.get.mockResolvedValue({ data: { id: 'a1' } })
    const res = await getSeckillActivity('a1')

    expect(request.get).toHaveBeenCalledWith('/seckill/activity/a1')
    expect(res).toEqual({ data: { id: 'a1' } })
  })

  it('createSeckillOrder 以 query 参数秒杀下单并透传返回值', async () => {
    const params = { quantity: 1, receiverName: '张三', receiverPhone: '138', receiverAddress: '杭州' }
    request.post.mockResolvedValue({ data: 'QH1' })
    const res = await createSeckillOrder('a1', params)

    expect(request.post).toHaveBeenCalledWith('/seckill/a1/createOrder', null, { params })
    expect(res).toEqual({ data: 'QH1' })
  })
})
