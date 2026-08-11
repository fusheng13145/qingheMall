import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Coupons from './Coupons.vue'
import { toasts } from '../../utils/toast'

// vi.hoisted：mock 工厂与测试体共享同一组 api 引用
const { createCoupon, listCoupons, toggleCoupon } = vi.hoisted(() => ({
  createCoupon: vi.fn(),
  listCoupons: vi.fn(),
  toggleCoupon: vi.fn()
}))

vi.mock('../../api/admin', () => ({
  createCoupon: (...a) => createCoupon(...a),
  listCoupons: (...a) => listCoupons(...a),
  toggleCoupon: (...a) => toggleCoupon(...a)
}))

// 字段同时覆盖模板取值（type/threshold）与规则文案取值（couponType/couponAmount）
function makeCoupon(overrides = {}) {
  return {
    id: 1,
    name: '新人满减券',
    type: 'FULL_REDUCTION',
    couponType: 'FULL_REDUCTION',
    threshold: 100,
    amount: 20,
    couponAmount: 20,
    issued: 12,
    total: 100,
    startTime: '2024-05-01 00:00:00',
    endTime: '2024-06-01 00:00:00',
    status: 'ACTIVE',
    ...overrides
  }
}

/** res.data 为 Paging<Coupon>，列表在 data 字段 */
function mockList(list) {
  listCoupons.mockResolvedValue({ data: { data: list } })
}

function mountPage() {
  return mount(Coupons)
}

function expectToast(type, message) {
  expect(toasts.some(t => t.type === type && t.message === message)).toBe(true)
}

/** 满减表单下 number 输入依次为：门槛 / 面额 / 总量 / 限领 */
function numberInputs(wrapper) {
  return wrapper.findAll('input[type="number"]')
}

beforeEach(() => {
  vi.clearAllMocks()
  toasts.splice(0, toasts.length)
})

describe('admin/Coupons 优惠券管理', () => {
  it('加载并渲染优惠券列表', async () => {
    mockList([
      makeCoupon(),
      makeCoupon({
        id: 2,
        name: '会员折扣券',
        type: 'DISCOUNT',
        couponType: 'DISCOUNT',
        discount: 0.85,
        couponDiscount: 0.85,
        couponAmount: null,
        status: 'INACTIVE'
      })
    ])
    const wrapper = mountPage()
    await flushPromises()

    expect(listCoupons).toHaveBeenCalledTimes(1)
    expect(listCoupons).toHaveBeenCalledWith(1, 50)
    expect(wrapper.text()).toContain('新人满减券')
    expect(wrapper.text()).toContain('满减')
    expect(wrapper.text()).toContain('满100减20')
    expect(wrapper.text()).toContain('12/100')
    expect(wrapper.text()).toContain('上架')
    expect(wrapper.text()).toContain('会员折扣券')
    expect(wrapper.text()).toContain('折扣')
    expect(wrapper.text()).toContain('85折')
    expect(wrapper.text()).toContain('下架')
  })

  it('空列表显示空态文案', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无优惠券')
  })

  it('加载失败 toast 提示', async () => {
    listCoupons.mockRejectedValue(new Error('net'))
    mountPage()
    await flushPromises()

    expectToast('error', '加载列表失败：net')
  })

  it('创建成功：提交满减券并刷新列表、重置表单', async () => {
    mockList([])
    createCoupon.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder="如：新人满减券"]').setValue('五月满减')
    await numberInputs(wrapper)[1].setValue('20')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expect(createCoupon).toHaveBeenCalledTimes(1)
    const payload = createCoupon.mock.calls[0][0]
    expect(payload).toMatchObject({
      name: '五月满减',
      type: 'FULL_REDUCTION',
      threshold: 0,
      amount: 20,
      total: 100,
      perLimit: 1,
      status: 'ACTIVE'
    })
    expect(payload.startTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
    expect(payload.endTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
    expectToast('success', '创建成功')
    // 提交后刷新列表（onMounted 1 次 + 成功后 1 次）
    expect(listCoupons).toHaveBeenCalledTimes(2)
    // 表单重置
    expect(wrapper.find('input[placeholder="如：新人满减券"]').element.value).toBe('')
  })

  it('创建校验：缺少券名称不调用接口', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expectToast('warning', '请填写券名称')
    expect(createCoupon).not.toHaveBeenCalled()
  })

  it('创建校验：缺少生效起止时间不调用接口', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder="如：新人满减券"]').setValue('缺时间')
    await wrapper.findAll('input[type="datetime-local"]')[0].setValue('')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expectToast('warning', '请选择生效起止时间')
    expect(createCoupon).not.toHaveBeenCalled()
  })

  it('创建校验：结束时间不晚于开始时间不调用接口', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder="如：新人满减券"]').setValue('时间倒挂')
    const times = wrapper.findAll('input[type="datetime-local"]')
    await times[0].setValue('2026-01-02T10:00')
    await times[1].setValue('2026-01-01T10:00')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expectToast('warning', '结束时间须晚于开始时间')
    expect(createCoupon).not.toHaveBeenCalled()
  })

  it('创建校验：发放总量为负不调用接口', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder="如：新人满减券"]').setValue('负总量')
    await numberInputs(wrapper)[2].setValue('-1')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expectToast('warning', '发放总量不能为负')
    expect(createCoupon).not.toHaveBeenCalled()
  })

  it('创建失败 toast 提示且按钮恢复可用', async () => {
    mockList([])
    createCoupon.mockRejectedValue(new Error('服务端错误'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder="如：新人满减券"]').setValue('失败券')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expectToast('error', '创建失败：服务端错误')
    expect(wrapper.find('.btn-create').text()).toBe('创建优惠券')
  })

  it('下架优惠券：ACTIVE → INACTIVE 并刷新列表', async () => {
    listCoupons
      .mockResolvedValueOnce({ data: { data: [makeCoupon()] } })
      .mockResolvedValueOnce({ data: { data: [makeCoupon({ status: 'INACTIVE' })] } })
    toggleCoupon.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.btn-toggle').text()).toBe('下架')
    await wrapper.find('.btn-toggle').trigger('click')
    await flushPromises()

    expect(toggleCoupon).toHaveBeenCalledWith(1, 'INACTIVE')
    expect(listCoupons).toHaveBeenCalledTimes(2)
    expect(wrapper.find('.badge').text()).toBe('下架')
    expect(wrapper.find('.btn-toggle').text()).toBe('上架')
  })

  it('上架优惠券：INACTIVE → ACTIVE', async () => {
    listCoupons
      .mockResolvedValueOnce({ data: { data: [makeCoupon({ status: 'INACTIVE' })] } })
      .mockResolvedValueOnce({ data: { data: [makeCoupon()] } })
    toggleCoupon.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.btn-toggle').text()).toBe('上架')
    await wrapper.find('.btn-toggle').trigger('click')
    await flushPromises()

    expect(toggleCoupon).toHaveBeenCalledWith(1, 'ACTIVE')
    expect(wrapper.find('.badge').text()).toBe('上架')
  })

  it('上下架失败 toast 提示且不刷新列表', async () => {
    mockList([makeCoupon()])
    toggleCoupon.mockRejectedValue(new Error('网络异常'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-toggle').trigger('click')
    await flushPromises()

    expectToast('error', '操作失败：网络异常')
    expect(listCoupons).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.badge').text()).toBe('上架')
  })
})
