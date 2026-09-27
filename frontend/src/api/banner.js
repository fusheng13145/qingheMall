import request from '../utils/request'

// ===================== 首页运营位（D2，v1.7） =====================

// 顾客端公开列表（仅上架，排序小在前）：GET /api/banner/list
export function listBanners() {
  return request.get('/banner/list')
}

// 管理端全量分页（含下架）：GET /api/admin/banner/list
export function adminListBanners(pageNum = 1, pageSize = 20) {
  return request.get('/admin/banner/list', { params: { pageNum, pageSize } })
}

// 管理端新建（默认上架）：POST /api/admin/banner/create
export function adminCreateBanner(banner) {
  return request.post('/admin/banner/create', banner)
}

// 管理端更新（标题/图片/链接/排序）：POST /api/admin/banner/update
export function adminUpdateBanner(banner) {
  return request.post('/admin/banner/update', banner)
}

// 管理端删除：POST /api/admin/banner/delete?bannerId=
export function adminDeleteBanner(bannerId) {
  return request.post('/admin/banner/delete', null, { params: { bannerId } })
}

// 管理端上下架（status: ON | OFF）：POST /api/admin/banner/toggle
export function adminToggleBanner(bannerId, status) {
  return request.post('/admin/banner/toggle', null, { params: { bannerId, status } })
}
