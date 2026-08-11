import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Seckill from './Seckill.vue'
import { toasts } from '../utils/toast'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
const { listSeckillActivities, createSeckillOrder } = vi.hoisted(() => ({
  listSeckillActivities: vi.fn(),
  createSeckillOrder: vi.fn()
}))
const { getDefaultAddress } = vi.hoisted(() => ({ getDefaultAddress: vi.fn() }))
const { mockUserStore } = vi.hoisted(() => ({ mockUserStore: { isLoggedIn: true } }))

vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../api/seckill', () => ({ listSeckillActivities, createSeckillOrder }))
vi.mock('../api/address', () => ({ getDefaultAddress }))
vi.mock('../stores/user', () => ({ useUserStore: () => mockUserStore }))

function makeActivity(overrides = {}) {
  return {
    id: 'a1',
    productName: '青禾秒杀 Tee',
    productImg: 'a.jpg',
    seckillPrice: 19.9,
    originalPrice: 99,
    totalStock: 100,
    remainStock: 50,
    startTime: '2026-01-01T00:00:00',
    endTime: '2099-01-01T00:00:00',
    ...overrides
  }
}

function expectToast(type, message) {
  expect(toasts.some(t => t.type === type && t.message === message)).toBe(true)
}

let wrapper

function mountPage() {
  wrapper = mount(Seckill)
  return wrapper
}

async function openDetail() {
  await wrapper.find('.btn-grab').trigger('click')
}

beforeEach(() => {
  vi.clearAllMocks()
  push.mockClear()
  mockUserStore.isLoggedIn = true
  toasts.splice(0, toasts.length)
})

// 组件 onMounted 启动了每秒刷新的 interval，卸载时清理
afterEach(() => {
  if (wrapper) wrapper.unmount()
})

describe('Seckill 限时秒杀页', () => {
  it('加载并渲染活动卡片', async () => {
    listSeckillActivities.mockResolvedValue({ data: [makeActivity()] })
    mountPage()
    await flushPromises()

    expect(listSeckillActivities).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('青禾秒杀 Tee')
    expect(wrapper.text()).toContain('¥19.90')
    expect(wrapper.text()).toContain('已抢 50/100')
    expect(wrapper.text()).toContain('立即抢购')
    expect(wrapper.find('.card-countdown').text()).toContain('剩')
  })

  it('空活动显示空态且全局倒计时占位', async () => {
    listSeckillActivities.mockResolvedValue({ data: [] })
    mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无进行中的秒杀活动')
    expect(wrapper.find('.clock-value').text()).toBe('--:--:--')
  })

  it('加载失败 toast 提示', async () => {
    listSeckillActivities.mockRejectedValue(new Error('net'))
    mountPage()
    await flushPromises()

    expectToast('error', '加载秒杀活动失败：net')
  })

  it('已抢光活动按钮禁用且不弹窗', async () => {
    listSeckillActivities.mockResolvedValue({ data: [makeActivity({ remainStock: 0 })] })
    mountPage()
    await flushPromises()

    expect(wrapper.find('.sold-out').exists()).toBe(true)
    const btn = wrapper.find('.btn-grab')
    expect(btn.text()).toBe('已抢光')
    expect(btn.attributes('disabled')).toBeDefined()

    await btn.trigger('click')
    expect(wrapper.find('.modal-mask').exists()).toBe(false)
  })

  it('点击立即抢购打开详情弹窗并可调整数量', async () => {
    listSeckillActivities.mockResolvedValue({ data: [makeActivity()] })
    mountPage()
    await flushPromises()

    await openDetail()
    expect(wrapper.find('.modal-name').text()).toBe('青禾秒杀 Tee')
    expect(wrapper.text()).toContain('剩余 50/100')
    expect(wrapper.text()).toContain('确认抢购并支付')

    const stepperBtns = wrapper.findAll('.qty-stepper button')
    expect(stepperBtns[0].attributes('disabled')).toBeDefined() // 数量 1 时不可减
    await stepperBtns[1].trigger('click')
    expect(wrapper.find('.qty-stepper span').text()).toBe('2')
  })

  it('关闭弹窗', async () => {
    listSeckillActivities.mockResolvedValue({ data: [makeActivity()] })
    mountPage()
    await flushPromises()

    await openDetail()
    await wrapper.find('.modal-close').trigger('click')
    expect(wrapper.find('.modal-mask').exists()).toBe(false)
  })

  it('未登录抢购跳转登录页', async () => {
    mockUserStore.isLoggedIn = false
    listSeckillActivities.mockResolvedValue({ data: [makeActivity()] })
    mountPage()
    await flushPromises()

    await openDetail()
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expect(push).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/seckill' } })
    expect(createSeckillOrder).not.toHaveBeenCalled()
  })

  it('无默认地址时提示去个人中心设置', async () => {
    listSeckillActivities.mockResolvedValue({ data: [makeActivity()] })
    getDefaultAddress.mockResolvedValue({ data: null })
    mountPage()
    await flushPromises()

    await openDetail()
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expectToast('warning', '请先在「个人中心」设置默认收货地址后再抢购')
    expect(push).toHaveBeenCalledWith('/profile')
    expect(createSeckillOrder).not.toHaveBeenCalled()
  })

  it('抢购成功携带默认地址下单并跳转支付页', async () => {
    listSeckillActivities.mockResolvedValue({ data: [makeActivity()] })
    getDefaultAddress.mockResolvedValue({
      data: { receiverName: '张三', receiverPhone: '13800138000', receiverAddress: '杭州市西湖区' }
    })
    createSeckillOrder.mockResolvedValue({ data: 'QH20260101' })
    mountPage()
    await flushPromises()

    await openDetail()
    // 数量 +1，验证下单数量透传
    await wrapper.findAll('.qty-stepper button')[1].trigger('click')
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expect(createSeckillOrder).toHaveBeenCalledWith('a1', {
      quantity: 2,
      receiverName: '张三',
      receiverPhone: '13800138000',
      receiverAddress: '杭州市西湖区'
    })
    expect(push).toHaveBeenCalledWith({ path: '/pay', query: { orderNumber: 'QH20260101' } })
    expect(wrapper.find('.modal-mask').exists()).toBe(false)
  })

  it('抢购失败 toast 提示且弹窗保留', async () => {
    listSeckillActivities.mockResolvedValue({ data: [makeActivity()] })
    getDefaultAddress.mockResolvedValue({
      data: { receiverName: '张三', receiverPhone: '13800138000', receiverAddress: '杭州市西湖区' }
    })
    createSeckillOrder.mockRejectedValue(new Error('手慢了，已售罄'))
    mountPage()
    await flushPromises()

    await openDetail()
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expectToast('error', '抢购失败：手慢了，已售罄')
    expect(wrapper.find('.modal-mask').exists()).toBe(true)
  })
})
