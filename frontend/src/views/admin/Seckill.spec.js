import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Seckill from './Seckill.vue'
import { toasts } from '../../utils/toast'

// vi.hoisted：mock 工厂与测试体共享同一组 api 引用
const { createSeckill, listSeckills, toggleSeckill } = vi.hoisted(() => ({
  createSeckill: vi.fn(),
  listSeckills: vi.fn(),
  toggleSeckill: vi.fn()
}))

vi.mock('../../api/admin', () => ({
  createSeckill: (...a) => createSeckill(...a),
  listSeckills: (...a) => listSeckills(...a),
  toggleSeckill: (...a) => toggleSeckill(...a)
}))

function makeSeckill(overrides = {}) {
  return {
    id: 1,
    productDetailId: 'pd001',
    productName: '青禾 Tee',
    seckillPrice: 49,
    originalPrice: 99,
    remainStock: 18,
    totalStock: 20,
    startTime: '2024-05-01 10:00:00',
    endTime: '2024-05-01 12:00:00',
    status: 'NOT_START',
    ...overrides
  }
}

/** res.data 为 Paging<SeckillActivity>，列表在 data 字段 */
function mockList(list) {
  listSeckills.mockResolvedValue({ data: { data: list } })
}

function mountPage() {
  return mount(Seckill)
}

function expectToast(type, message) {
  expect(toasts.some(t => t.type === type && t.message === message)).toBe(true)
}

/** number 输入依次为：秒杀价 / 活动库存 */
function numberInputs(wrapper) {
  return wrapper.findAll('input[type="number"]')
}

beforeEach(() => {
  vi.clearAllMocks()
  toasts.splice(0, toasts.length)
})

describe('admin/Seckill 秒杀管理', () => {
  it('加载并渲染秒杀活动列表', async () => {
    mockList([
      makeSeckill(),
      makeSeckill({ id: 2, productName: '', productDetailId: 'pd002', status: 'ONGOING' })
    ])
    const wrapper = mountPage()
    await flushPromises()

    expect(listSeckills).toHaveBeenCalledTimes(1)
    expect(listSeckills).toHaveBeenCalledWith(1, 50)
    expect(wrapper.text()).toContain('青禾 Tee')
    expect(wrapper.text()).toContain('49.00')
    expect(wrapper.text()).toContain('99.00')
    expect(wrapper.text()).toContain('18/20')
    expect(wrapper.text()).toContain('未开始')
    expect(wrapper.text()).toContain('进行中')
    // 无商品名时回退展示规格 ID
    expect(wrapper.text()).toContain('pd002')
    expect(wrapper.text()).toContain('开始')
    expect(wrapper.text()).toContain('结束')
  })

  it('空列表显示空态文案', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无秒杀活动')
  })

  it('加载失败 toast 提示', async () => {
    listSeckills.mockRejectedValue(new Error('net'))
    mountPage()
    await flushPromises()

    expectToast('error', '加载列表失败：net')
  })

  it('创建成功：提交活动并刷新列表、重置表单', async () => {
    mockList([])
    createSeckill.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder^="如 pd001"]').setValue('pd001')
    await numberInputs(wrapper)[0].setValue('49')
    await numberInputs(wrapper)[1].setValue('20')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expect(createSeckill).toHaveBeenCalledTimes(1)
    const payload = createSeckill.mock.calls[0][0]
    expect(payload).toMatchObject({
      productDetailId: 'pd001',
      seckillPrice: 49,
      totalStock: 20
    })
    expect(payload.startTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
    expect(payload.endTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
    expectToast('success', '创建成功（状态：未开始，可在列表中点击「开始」生效）')
    // 提交后刷新列表（onMounted 1 次 + 成功后 1 次）
    expect(listSeckills).toHaveBeenCalledTimes(2)
    // 表单重置
    expect(wrapper.find('input[placeholder^="如 pd001"]').element.value).toBe('')
  })

  it('创建校验：缺少商品规格ID不调用接口', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expectToast('warning', '请填写商品规格ID')
    expect(createSeckill).not.toHaveBeenCalled()
  })

  it('创建校验：秒杀价不大于 0 不调用接口', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder^="如 pd001"]').setValue('pd001')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expectToast('warning', '秒杀价必须大于 0')
    expect(createSeckill).not.toHaveBeenCalled()
  })

  it('创建校验：活动库存不大于 0 不调用接口', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder^="如 pd001"]').setValue('pd001')
    await numberInputs(wrapper)[0].setValue('49')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expectToast('warning', '活动库存必须大于 0')
    expect(createSeckill).not.toHaveBeenCalled()
  })

  it('创建校验：缺少起止时间不调用接口', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder^="如 pd001"]').setValue('pd001')
    await numberInputs(wrapper)[0].setValue('49')
    await numberInputs(wrapper)[1].setValue('20')
    await wrapper.findAll('input[type="datetime-local"]')[0].setValue('')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expectToast('warning', '请选择起止时间')
    expect(createSeckill).not.toHaveBeenCalled()
  })

  it('创建校验：结束时间不晚于开始时间不调用接口', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder^="如 pd001"]').setValue('pd001')
    await numberInputs(wrapper)[0].setValue('49')
    await numberInputs(wrapper)[1].setValue('20')
    const times = wrapper.findAll('input[type="datetime-local"]')
    await times[0].setValue('2026-01-02T10:00')
    await times[1].setValue('2026-01-01T10:00')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expectToast('warning', '结束时间须晚于开始时间')
    expect(createSeckill).not.toHaveBeenCalled()
  })

  it('创建失败 toast 提示且按钮恢复可用', async () => {
    mockList([])
    createSeckill.mockRejectedValue(new Error('库存不足'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder^="如 pd001"]').setValue('pd001')
    await numberInputs(wrapper)[0].setValue('49')
    await numberInputs(wrapper)[1].setValue('20')
    await wrapper.find('.btn-create').trigger('click')
    await flushPromises()

    expectToast('error', '创建失败：库存不足')
    expect(wrapper.find('.btn-create').text()).toBe('创建活动')
  })

  it('开始活动：NOT_START → ONGOING 并刷新列表', async () => {
    listSeckills
      .mockResolvedValueOnce({ data: { data: [makeSeckill()] } })
      .mockResolvedValueOnce({ data: { data: [makeSeckill({ status: 'ONGOING' })] } })
    toggleSeckill.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.btn-toggle').text()).toBe('开始')
    await wrapper.find('.btn-toggle').trigger('click')
    await flushPromises()

    expect(toggleSeckill).toHaveBeenCalledWith(1, 'ONGOING')
    expect(listSeckills).toHaveBeenCalledTimes(2)
    expect(wrapper.find('.badge').text()).toBe('进行中')
    expect(wrapper.find('.btn-toggle').text()).toBe('结束')
  })

  it('结束活动：ONGOING → CLOSED', async () => {
    listSeckills
      .mockResolvedValueOnce({ data: { data: [makeSeckill({ status: 'ONGOING' })] } })
      .mockResolvedValueOnce({ data: { data: [makeSeckill({ status: 'CLOSED' })] } })
    toggleSeckill.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.btn-toggle').text()).toBe('结束')
    await wrapper.find('.btn-toggle').trigger('click')
    await flushPromises()

    expect(toggleSeckill).toHaveBeenCalledWith(1, 'CLOSED')
    expect(wrapper.find('.badge').text()).toBe('已关闭')
    expect(wrapper.find('.btn-toggle').text()).toBe('开始')
  })

  it('上下线失败 toast 提示且不刷新列表', async () => {
    mockList([makeSeckill()])
    toggleSeckill.mockRejectedValue(new Error('网络异常'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-toggle').trigger('click')
    await flushPromises()

    expectToast('error', '操作失败：网络异常')
    expect(listSeckills).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.badge').text()).toBe('未开始')
  })
})
