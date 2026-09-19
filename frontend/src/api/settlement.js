import request from '../utils/request'

// ===================== 商家结算分账（A2，v1.5） =====================

// 商家端结算概览：GET /api/merchant/settlement/summary
// { balance 余额, totalNet 累计净入, totalCommission 累计佣金, paidBills 已放款合计 }
export function getSettlementSummary() {
  return request.get('/merchant/settlement/summary')
}

// 商家端分账流水分页：GET /api/merchant/settlement/ledger
export function listSettlementLedger(pageNum = 1, pageSize = 10) {
  return request.get('/merchant/settlement/ledger', { params: { pageNum, pageSize } })
}

// 商家端结算单分页：GET /api/merchant/settlement/bills
export function listSettlementBills(pageNum = 1, pageSize = 10) {
  return request.get('/merchant/settlement/bills', { params: { pageNum, pageSize } })
}

// 管理端：为商家生成结算单：POST /api/admin/settlement/generate?merchantId=
export function adminGenerateSettlementBill(merchantId) {
  return request.post('/admin/settlement/generate', null, { params: { merchantId } })
}

// 管理端：结算单分页：GET /api/admin/settlement/list?status=
export function adminListSettlementBills(pageNum = 1, pageSize = 10, status = '') {
  return request.get('/admin/settlement/list', { params: { pageNum, pageSize, status } })
}

// 管理端：审核结算单（approve=true 放款 / false 驳回）：POST /api/admin/settlement/review
export function adminReviewSettlementBill(billId, approve, note = '') {
  return request.post('/admin/settlement/review', null, { params: { billId, approve, note } })
}

// 管理端：结算单流水明细：GET /api/admin/settlement/entries?billId=
export function adminSettlementEntries(billId) {
  return request.get('/admin/settlement/entries', { params: { billId } })
}
