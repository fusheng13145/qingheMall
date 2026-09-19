import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Settlement from './Settlement.vue'
import {
  adminListSettlementBills,
  adminGenerateSettlementBill,
  adminReviewSettlementBill
} from '../../api/settlement'
import { toast } from '../../utils/toast'

vi.mock('../../api/settlement', () => ({
  adminListSettlementBills: vi.fn(),
  adminGenerateSettlementBill: vi.fn(),
  adminReviewSettlementBill: vi.fn()
}))
vi.mock('../../utils/toast', () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

function billPayload() {
  return {
    data: {
      data: [
        { id: 'B1', merchantId: 5, totalGross: 180, totalCommission: 9, totalNet: 171, entryCount: 2, status: 'PENDING', reviewNote: null },
        { id: 'B2', merchantId: 6, totalGross: 100, totalCommission: 5, totalNet: 95, entryCount: 1, status: 'PAID', reviewNote: '对账无误' }
      ]
    }
  }
}

describe('admin/Settlement 结算管理', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染结算单列表与操作按钮', async () => {
    adminListSettlementBills.mockResolvedValue(billPayload())
    const wrapper = mount(Settlement)
    await flushPromises()

    expect(wrapper.text()).toContain('B1')
    expect(wrapper.text()).toContain('¥171.00')
    // PENDING 行有放款/驳回按钮，PAID 行显示审核备注
    expect(wrapper.findAll('.btn-approve')).toHaveLength(1)
    expect(wrapper.findAll('.btn-reject')).toHaveLength(1)
    expect(wrapper.text()).toContain('对账无误')
  })

  it('生成结算单：无效商家 ID 拦截', async () => {
    adminListSettlementBills.mockResolvedValue({ data: { data: [] } })
    const wrapper = mount(Settlement)
    await flushPromises()

    await wrapper.find('.btn-generate').trigger('click')
    expect(toast.error).toHaveBeenCalledWith('请输入有效的商家 ID')
    expect(adminGenerateSettlementBill).not.toHaveBeenCalled()
  })

  it('生成结算单成功并刷新列表', async () => {
    adminListSettlementBills.mockResolvedValue(billPayload())
    adminGenerateSettlementBill.mockResolvedValue({})
    const wrapper = mount(Settlement)
    await flushPromises()

    await wrapper.find('.merchant-input').setValue('5')
    await wrapper.find('.btn-generate').trigger('click')
    await flushPromises()

    expect(adminGenerateSettlementBill).toHaveBeenCalledWith(5)
    expect(toast.success).toHaveBeenCalledWith('结算单已生成')
  })

  it('放款审核调用并刷新', async () => {
    adminListSettlementBills.mockResolvedValue(billPayload())
    adminReviewSettlementBill.mockResolvedValue({})
    const wrapper = mount(Settlement)
    await flushPromises()

    await wrapper.find('.btn-approve').trigger('click')
    await flushPromises()

    expect(adminReviewSettlementBill).toHaveBeenCalledWith('B1', true, '对账无误，同意放款')
    expect(toast.success).toHaveBeenCalledWith('已放款')
  })

  it('驳回审核调用', async () => {
    adminListSettlementBills.mockResolvedValue(billPayload())
    adminReviewSettlementBill.mockResolvedValue({})
    const wrapper = mount(Settlement)
    await flushPromises()

    await wrapper.find('.btn-reject').trigger('click')
    await flushPromises()

    expect(adminReviewSettlementBill).toHaveBeenCalledWith('B1', false, '金额存疑，请人工复核')
    expect(toast.success).toHaveBeenCalledWith('已驳回')
  })
})
