import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Checkout from './Checkout.vue'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
const { listCart, clearSelectedCart } = vi.hoisted(() => ({
  listCart: vi.fn(),
  clearSelectedCart: vi.fn()
}))
const { batchAddOrders } = vi.hoisted(() => ({ batchAddOrders: vi.fn() }))
const { getDefaultAddress } = vi.hoisted(() => ({ getDefaultAddress: vi.fn() }))
const { availableCoupons } = vi.hoisted(() => ({ availableCoupons: vi.fn() }))
const { mockCartStore } = vi.hoisted(() => ({ mockCartStore: { refreshCount: vi.fn() } }))

vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../api/cart', () => ({ listCart, clearSelectedCart }))
vi.mock('../api/order', () => ({ batchAddOrders }))
vi.mock('../api/address', () => ({ getDefaultAddress }))
vi.mock('../api/coupon', () => ({ availableCoupons }))
vi.mock('../stores/cart', () => ({ useCartStore: () => mockCartStore }))

function makeItem(overrides = {}) {
  return {
    id: 1,
    productDetailId: 'pd1',
    productName: 'Nike Air',
    price: 299,
    quantity: 1,
    selected: true,
    ...overrides
  }
}

function mountPage() {
  return mount(Checkout, { global: { stubs: { 'router-link': { template: '<a><slot /></a>' } } } })
}

beforeEach(() => {
  vi.clearAllMocks()
  push.mockClear()
  mockCartStore.refreshCount.mockClear()
  global.alert = vi.fn()
})

describe('Checkout 结算页', () => {
  it('加载已选商品并自动填充默认地址', async () => {
    listCart.mockResolvedValue({ data: [makeItem()] })
    getDefaultAddress.mockResolvedValue({
      data: { receiverName: '张三', receiverPhone: '13800000000', receiverAddress: '北京市朝阳区' }
    })
    availableCoupons.mockResolvedValue({ data: [] })

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Nike Air')
    expect(wrapper.find('input[placeholder="请输入收货人姓名"]').element.value).toBe('张三')
  })

  it('单笔订单拉取可用券', async () => {
    listCart.mockResolvedValue({ data: [makeItem()] })
    getDefaultAddress.mockResolvedValue({ data: null })
    availableCoupons.mockResolvedValue({ data: [{ id: 'c1', couponName: '满100减20', threshold: 100, amount: 20 }] })

    const wrapper = mountPage()
    await flushPromises()

    expect(availableCoupons).toHaveBeenCalled()
    expect(wrapper.text()).toContain('满100减20')
  })

  it('多商品不拉取优惠券', async () => {
    listCart.mockResolvedValue({ data: [makeItem(), makeItem({ id: 2, productDetailId: 'pd2' })] })
    getDefaultAddress.mockResolvedValue({ data: null })

    mountPage()
    await flushPromises()

    expect(availableCoupons).not.toHaveBeenCalled()
  })

  it('校验失败：电话格式不正确', async () => {
    listCart.mockResolvedValue({ data: [makeItem()] })
    getDefaultAddress.mockResolvedValue({ data: null })

    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder="请输入收货人姓名"]').setValue('张三')
    await wrapper.find('input[placeholder="请输入手机号"]').setValue('12345')
    await wrapper.find('textarea').setValue('北京市')
    await wrapper.find('.btn-submit').trigger('click')

    expect(global.alert).toHaveBeenCalledWith('联系电话格式不正确')
    expect(batchAddOrders).not.toHaveBeenCalled()
  })

  it('提交成功：下单 + 清空已选 + 刷新角标 + 跳转订单页', async () => {
    listCart.mockResolvedValue({ data: [makeItem()] })
    getDefaultAddress.mockResolvedValue({
      data: { receiverName: '张三', receiverPhone: '13800000000', receiverAddress: '北京市朝阳区' }
    })
    availableCoupons.mockResolvedValue({ data: [] })
    batchAddOrders.mockResolvedValue({ data: [] })
    clearSelectedCart.mockResolvedValue({})

    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(batchAddOrders).toHaveBeenCalledTimes(1)
    expect(clearSelectedCart).toHaveBeenCalled()
    expect(mockCartStore.refreshCount).toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith('/orders')
  })

  it('下单失败 alert 提示', async () => {
    listCart.mockResolvedValue({ data: [makeItem()] })
    getDefaultAddress.mockResolvedValue({
      data: { receiverName: '张三', receiverPhone: '13800000000', receiverAddress: '北京市朝阳区' }
    })
    batchAddOrders.mockRejectedValue(new Error('库存不足'))

    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(global.alert).toHaveBeenCalledWith('下单失败：库存不足')
  })

  it('空购物车提交提示没有可提交商品', async () => {
    listCart.mockResolvedValue({ data: [] })
    getDefaultAddress.mockResolvedValue({
      data: { receiverName: '张三', receiverPhone: '13800000000', receiverAddress: '北京市朝阳区' }
    })

    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-submit').trigger('click')

    expect(global.alert).toHaveBeenCalledWith('没有可提交的商品')
  })
})
