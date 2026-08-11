import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Merchants from './Merchants.vue'
import { toasts } from '../../utils/toast'

// vi.hoisted：mock 工厂与测试体共享同一组 api 引用
const { listMerchants, auditMerchant } = vi.hoisted(() => ({
  listMerchants: vi.fn(),
  auditMerchant: vi.fn()
}))

vi.mock('../../api/admin', () => ({
  listMerchants: (...a) => listMerchants(...a),
  auditMerchant: (...a) => auditMerchant(...a)
}))

function makeMerchant(overrides = {}) {
  return {
    id: 1,
    shopName: '青禾小铺',
    nickName: '店主阿禾',
    userName: 'shop01',
    shopDesc: '农家好物',
    gmtCreated: '2026-01-02T03:04:00',
    status: 'PENDING',
    rejectReason: '',
    ...overrides
  }
}

/** res.data 为 Paging<Merchant>，列表在 data 字段 */
function mockPaging(list, extra = {}) {
  listMerchants.mockResolvedValue({ data: { data: list, totalCount: list.length, totalPage: 1, ...extra } })
}

function mountPage() {
  return mount(Merchants)
}

function expectToast(type, message) {
  expect(toasts.some(t => t.type === type && t.message === message)).toBe(true)
}

beforeEach(() => {
  vi.clearAllMocks()
  global.confirm = vi.fn(() => true)
  window.prompt = vi.fn(() => '资料不完整')
  toasts.splice(0, toasts.length)
})

describe('admin/Merchants 商家入驻审核', () => {
  it('初始加载默认请求待审核列表并渲染', async () => {
    mockPaging([makeMerchant()])
    const wrapper = mountPage()
    await flushPromises()

    expect(listMerchants).toHaveBeenCalledTimes(1)
    expect(listMerchants).toHaveBeenCalledWith(1, 10, 'PENDING')
    expect(wrapper.text()).toContain('青禾小铺')
    expect(wrapper.text()).toContain('店主阿禾')
    expect(wrapper.text()).toContain('农家好物')
    expect(wrapper.text()).toContain('2026-01-02 03:04')
    expect(wrapper.text()).toContain('待审核')
    // PENDING 行展示通过/驳回按钮
    expect(wrapper.find('.row-actions').exists()).toBe(true)
    // 「待审核」Tab 处于激活态
    const tabs = wrapper.findAll('.tab')
    expect(tabs.find(t => t.text() === '待审核').classes()).toContain('active')
  })

  it('空列表显示空态文案', async () => {
    mockPaging([])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无商家申请')
  })

  it('加载失败 toast 提示', async () => {
    listMerchants.mockRejectedValue(new Error('net'))
    mountPage()
    await flushPromises()

    expectToast('error', '加载失败：net')
  })

  it('切换「全部」Tab 以空状态重新请求第一页', async () => {
    mockPaging([makeMerchant()])
    const wrapper = mountPage()
    await flushPromises()

    const allTab = wrapper.findAll('.tab').find(t => t.text() === '全部')
    await allTab.trigger('click')
    await flushPromises()

    expect(listMerchants).toHaveBeenCalledTimes(2)
    expect(listMerchants).toHaveBeenLastCalledWith(1, 10, '')
    expect(allTab.classes()).toContain('active')
  })

  it('审核通过：确认后调用接口、toast 成功并刷新列表', async () => {
    mockPaging([makeMerchant()])
    auditMerchant.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.row-actions .link-btn').trigger('click')
    await flushPromises()

    expect(global.confirm).toHaveBeenCalled()
    expect(auditMerchant).toHaveBeenCalledWith(1, true, '')
    expectToast('success', '已通过审核，商家可开始经营')
    // 审核后重新拉取当前页列表
    expect(listMerchants).toHaveBeenCalledTimes(2)
    expect(listMerchants).toHaveBeenLastCalledWith(1, 10, 'PENDING')
  })

  it('审核通过：取消确认不调用接口', async () => {
    global.confirm = vi.fn(() => false)
    mockPaging([makeMerchant()])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.row-actions .link-btn').trigger('click')
    await flushPromises()

    expect(auditMerchant).not.toHaveBeenCalled()
    expect(listMerchants).toHaveBeenCalledTimes(1)
  })

  it('审核驳回：填写原因后调用接口并 toast 成功', async () => {
    window.prompt = vi.fn(() => '资质不全')
    mockPaging([makeMerchant()])
    auditMerchant.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.row-actions .link-btn.danger').trigger('click')
    await flushPromises()

    expect(window.prompt).toHaveBeenCalled()
    expect(auditMerchant).toHaveBeenCalledWith(1, false, '资质不全')
    expectToast('success', '已驳回')
    expect(listMerchants).toHaveBeenCalledTimes(2)
  })

  it('审核驳回：原因为空仅警告且不调用接口', async () => {
    window.prompt = vi.fn(() => '   ')
    mockPaging([makeMerchant()])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.row-actions .link-btn.danger').trigger('click')
    await flushPromises()

    expectToast('warning', '驳回必须填写原因')
    expect(auditMerchant).not.toHaveBeenCalled()
  })

  it('审核驳回：取消弹窗（prompt 返回 null）不调用接口', async () => {
    window.prompt = vi.fn(() => null)
    mockPaging([makeMerchant()])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.row-actions .link-btn.danger').trigger('click')
    await flushPromises()

    expect(auditMerchant).not.toHaveBeenCalled()
    expect(toasts.length).toBe(0)
  })

  it('审核失败 toast 提示', async () => {
    mockPaging([makeMerchant()])
    auditMerchant.mockRejectedValue(new Error('服务端错误'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.row-actions .link-btn').trigger('click')
    await flushPromises()

    expectToast('error', '操作失败：服务端错误')
    // 失败后不刷新列表
    expect(listMerchants).toHaveBeenCalledTimes(1)
  })

  it('已驳回/已通过状态渲染文案且无操作按钮', async () => {
    mockPaging([
      makeMerchant({ id: 1, status: 'REJECTED', rejectReason: '资质不全' }),
      makeMerchant({ id: 2, shopName: '已通过小店', status: 'ACTIVE' })
    ])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('已驳回')
    expect(wrapper.text()).toContain('资质不全')
    expect(wrapper.text()).toContain('已通过')
    expect(wrapper.find('.row-actions').exists()).toBe(false)
  })

  it('分页：下一页携带第 2 页重新请求', async () => {
    mockPaging([makeMerchant()], { totalCount: 12, totalPage: 2 })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.page-info').text()).toBe('第 1 / 2 页（共 12 家）')
    const pagerButtons = wrapper.findAll('.pager .btn-ghost')
    expect(pagerButtons[0].attributes('disabled')).toBeDefined()
    await pagerButtons[1].trigger('click')
    await flushPromises()

    expect(listMerchants).toHaveBeenLastCalledWith(2, 10, 'PENDING')
    expect(wrapper.find('.page-info').text()).toBe('第 2 / 2 页（共 12 家）')
  })
})
