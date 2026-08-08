import request from '../utils/request'

// 进行中秒杀活动列表（含商品名/图/原价、剩余库存、起止时间）：GET /api/seckill/activities
export function listSeckillActivities() {
  return request.get('/seckill/activities')
}

// 活动详情：GET /api/seckill/activity/{id}
export function getSeckillActivity(id) {
  return request.get('/seckill/activity/' + id)
}

// 秒杀下单：POST /api/seckill/{id}/createOrder
export function createSeckillOrder(id, params) {
  return request.post('/seckill/' + id + '/createOrder', null, { params })
}
