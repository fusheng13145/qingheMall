import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import {
  getSettlementSummary,
  listSettlementLedger,
  listSettlementBills,
  adminGenerateSettlementBill,
  adminListSettlementBills,
  adminReviewSettlementBill,
  adminSettlementEntries
} from './settlement'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/settlement（A2，v1.5）', () => {
  it('getSettlementSummary 查询商家结算概览并透传返回值', async () => {
    const summary = { balance: 121.6, totalNet: 500, totalCommission: 25, paidBills: 378.4 }
    request.get.mockResolvedValue({ data: summary })
    const res = await getSettlementSummary()

    expect(request.get).toHaveBeenCalledWith('/merchant/settlement/summary')
    expect(res).toEqual({ data: summary })
  })

  it('listSettlementLedger 以分页参数查询分账流水', async () => {
    request.get.mockResolvedValue({ data: { data: [], totalCount: 0 } })
    const res = await listSettlementLedger(2, 20)

    expect(request.get).toHaveBeenCalledWith('/merchant/settlement/ledger', { params: { pageNum: 2, pageSize: 20 } })
    expect(res).toEqual({ data: { data: [], totalCount: 0 } })
  })

  it('listSettlementLedger 默认第 1 页每页 10 条', async () => {
    await listSettlementLedger()

    expect(request.get).toHaveBeenCalledWith('/merchant/settlement/ledger', { params: { pageNum: 1, pageSize: 10 } })
  })

  it('listSettlementBills 以分页参数查询结算单', async () => {
    request.get.mockResolvedValue({ data: { data: [] } })
    await listSettlementBills(3, 5)

    expect(request.get).toHaveBeenCalledWith('/merchant/settlement/bills', { params: { pageNum: 3, pageSize: 5 } })
  })

  it('adminGenerateSettlementBill 以 query 参数提交商家 ID 生成结算单', async () => {
    request.post.mockResolvedValue({ data: { id: 'bill1', status: 'PENDING' } })
    const res = await adminGenerateSettlementBill(18)

    expect(request.post).toHaveBeenCalledWith('/admin/settlement/generate', null, { params: { merchantId: 18 } })
    expect(res).toEqual({ data: { id: 'bill1', status: 'PENDING' } })
  })

  it('adminListSettlementBills 携带状态过滤查询结算单分页', async () => {
    await adminListSettlementBills(1, 10, 'PAID')

    expect(request.get).toHaveBeenCalledWith('/admin/settlement/list', { params: { pageNum: 1, pageSize: 10, status: 'PAID' } })
  })

  it('adminReviewSettlementBill 以 query 参数提交审核结论（放款/驳回）', async () => {
    await adminReviewSettlementBill('bill1', true, '冒烟放款')
    expect(request.post).toHaveBeenCalledWith('/admin/settlement/review', null, { params: { billId: 'bill1', approve: true, note: '冒烟放款' } })

    await adminReviewSettlementBill('bill2', false)
    expect(request.post).toHaveBeenCalledWith('/admin/settlement/review', null, { params: { billId: 'bill2', approve: false, note: '' } })
  })

  it('adminSettlementEntries 以 billId 查询结算单流水明细', async () => {
    const entries = [{ type: 'EARN', net: 121.6 }]
    request.get.mockResolvedValue({ data: entries })
    const res = await adminSettlementEntries('bill1')

    expect(request.get).toHaveBeenCalledWith('/admin/settlement/entries', { params: { billId: 'bill1' } })
    expect(res).toEqual({ data: entries })
  })
})
