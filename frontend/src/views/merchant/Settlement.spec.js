import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Settlement from './Settlement.vue'
import { getSettlementSummary, listSettlementLedger, listSettlementBills } from '../../api/settlement'
import { toast } from '../../utils/toast'

vi.mock('../../api/settlement', () => ({
  getSettlementSummary: vi.fn(),
  listSettlementLedger: vi.fn(),
  listSettlementBills: vi.fn()
}))
vi.mock('../../utils/toast', () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

function payload() {
  return {
    data: {
      balance: 171,
      totalNet: 180,
      totalCommission: 9,
      paidBills: 9
    }
  }
}

describe('merchant/Settlement 结算中心', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染概览四卡与流水/结算单', async () => {
    getSettlementSummary.mockResolvedValue(payload())
    listSettlementLedger.mockResolvedValue({
      data: { data: [{ id: 'L1', orderNumber: 'QH1', type: 'EARN', gross: 100, commission: 5, net: 95, billId: 'B1' }] }
    })
    listSettlementBills.mockResolvedValue({
      data: { data: [{ id: 'B1', totalGross: 100, totalCommission: 5, totalNet: 95, entryCount: 1, status: 'PAID' }] }
    })
    const wrapper = mount(Settlement)
    await flushPromises()

    expect(wrapper.text()).toContain('¥171.00')
    expect(wrapper.text()).toContain('¥9.00')
    expect(wrapper.text()).toContain('QH1')
    expect(wrapper.text()).toContain('已放款')
    expect(wrapper.text()).toContain('B1')
  })

  it('冲销流水显示负数与冲销类型', async () => {
    getSettlementSummary.mockResolvedValue(payload())
    listSettlementLedger.mockResolvedValue({
      data: { data: [{ id: 'L2', orderNumber: 'QH2', type: 'REVERSAL', gross: 100, commission: 5, net: -95, billId: null }] }
    })
    listSettlementBills.mockResolvedValue({ data: { data: [] } })
    const wrapper = mount(Settlement)
    await flushPromises()

    expect(wrapper.text()).toContain('冲销')
    expect(wrapper.find('.net-negative').exists()).toBe(true)
  })

  it('空流水显示空态', async () => {
    getSettlementSummary.mockResolvedValue(payload())
    listSettlementLedger.mockResolvedValue({ data: { data: [] } })
    listSettlementBills.mockResolvedValue({ data: { data: [] } })
    const wrapper = mount(Settlement)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无分账流水')
    expect(wrapper.text()).toContain('暂无结算单')
  })

  it('加载失败 toast 提示', async () => {
    getSettlementSummary.mockRejectedValue(new Error('fail'))
    listSettlementLedger.mockRejectedValue(new Error('fail'))
    listSettlementBills.mockRejectedValue(new Error('fail'))
    mount(Settlement)
    await flushPromises()

    expect(toast.error).toHaveBeenCalledWith('结算数据加载失败')
  })
})
