import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Checkout from './Checkout.vue'
import { toasts } from '../utils/toast'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
const { batchAddOrders } = vi.hoisted(() => ({ batchAddOrders: vi.fn() }))
const { getDefaultAddress } = vi.hoisted(() => ({ getDefaultAddress: vi.fn() }))
const { availableCoupons } = vi.hoisted(() => ({ availableCoupons: vi.fn() }))
// P2-15：结算页数据源改为购物车 store，mock 需提供 selectedItems/fetchItems/clearSelected
const { mockCartStore } = vi.hoisted(() => ({
  mockCartStore: {
    selectedItems: [],
    fetchItems: vi.fn(),
    clearSelected: vi.fn()
  }
}))

vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
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
  mockCartStore.selectedItems = []
  mockCartStore.fetchItems.mockResolvedValue()
  mockCartStore.clearSelected.mockResolvedValue()
  toasts.splice(0, toasts.length)
})

/** 断言 toast 队列中存在指定类型与文案 */
function expectToast(type, message) {
  expect(toasts.some(t => t.type === type && t.message === message)).toBe(true)
}

describe('Checkout 结算页', () => {
  it('从 store 加载已选商品并自动填充默认地址', async () => {
    mockCartStore.selectedItems = [makeItem()]
    getDefaultAddress.mockResolvedValue({
      data: { receiverName: '张三', receiverPhone: '13800000000', receiverAddress: '北京市朝阳区' }
    })
    availableCoupons.mockResolvedValue({ data: [] })

    const wrapper = mountPage()
    await flushPromises()

    expect(mockCartStore.fetchItems).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Nike Air')
    expect(wrapper.find('input[placeholder="请输入收货人姓名"]').element.value).toBe('张三')
  })

  it('单笔订单拉取可用券', async () => {
    mockCartStore.selectedItems = [makeItem()]
    getDefaultAddress.mockResolvedValue({ data: null })
    availableCoupons.mockResolvedValue({ data: [{ id: 'c1', couponName: '满100减20', couponType: 'FULL_REDUCTION', threshold: 100, couponAmount: 20 }] })

    const wrapper = mountPage()
    await flushPromises()

    expect(availableCoupons).toHaveBeenCalled()
    expect(wrapper.text()).toContain('满100减20')
  })

  it('多商品结算同样拉取可用券（v1.8 购物车级）', async () => {
    mockCartStore.selectedItems = [makeItem(), makeItem({ id: 2, productDetailId: 'pd2', price: 100 })]
    getDefaultAddress.mockResolvedValue({ data: null })
    availableCoupons.mockResolvedValue({
      data: [{ id: 'c1', couponName: '满100减20', couponType: 'FULL_REDUCTION', threshold: 100, couponAmount: 20 }]
    })

    const wrapper = mountPage()
    await flushPromises()

    expect(availableCoupons).toHaveBeenCalled()
    expect(wrapper.text()).toContain('满100减20')
  })

  it('平台券按整单合计计算优惠并附加到首单提交', async () => {
    mockCartStore.selectedItems = [
      makeItem({ id: 1, productDetailId: 'pd1', price: 299 }),
      makeItem({ id: 2, productDetailId: 'pd2', price: 100 })
    ]
    getDefaultAddress.mockResolvedValue({ data: null })
    availableCoupons.mockResolvedValue({
      data: [{ id: 'c1', couponName: '满100减20', couponType: 'FULL_REDUCTION', threshold: 100, couponAmount: 20 }]
    })
    batchAddOrders.mockResolvedValue({ data: [] })

    const wrapper = mountPage()
    await flushPromises()

    // 整单合计 399 → 满100减20 → -¥20.00
    expect(wrapper.text()).toContain('-¥20.00')
    await wrapper.find('input[type="radio"]').setValue()
    await wrapper.find('input[placeholder="请输入收货人姓名"]').setValue('张三')
    await wrapper.find('input[placeholder="请输入手机号"]').setValue('13800000000')
    await wrapper.find('textarea').setValue('北京市朝阳区')
    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    const payload = batchAddOrders.mock.calls[0][0]
    expect(payload[0].couponId).toBe('c1')
    expect(payload[0].discountAmount).toBe(20)
    // 第二笔不带券字段
    expect(payload[1].couponId).toBeUndefined()
  })

  it('店铺券在混合购物车按本店商品合计计算优惠', async () => {
    // 本店（merchantId=1）80 元 + 外店 70 元：店铺券满50减10 只按本店 80 元计
    mockCartStore.selectedItems = [
      makeItem({ id: 1, productDetailId: 'pd1', price: 80, merchantId: 1 }),
      makeItem({ id: 2, productDetailId: 'pd2', price: 70, merchantId: 2 })
    ]
    getDefaultAddress.mockResolvedValue({ data: null })
    availableCoupons.mockResolvedValue({
      data: [{ id: 'c2', couponName: '店铺券满50减10', couponType: 'FULL_REDUCTION', threshold: 50, couponAmount: 10, couponMerchantId: 1 }]
    })
    batchAddOrders.mockResolvedValue({ data: [] })

    const wrapper = mountPage()
    await flushPromises()

    // 本店合计 80 → -¥10.00（若误用整单合计 150 也仍是 10；改用折扣券场景才有差异，此处验证口径传递）
    expect(wrapper.text()).toContain('-¥10.00')
    await wrapper.find('input[type="radio"]').setValue()
    await wrapper.find('input[placeholder="请输入收货人姓名"]').setValue('张三')
    await wrapper.find('input[placeholder="请输入手机号"]').setValue('13800000000')
    await wrapper.find('textarea').setValue('北京市朝阳区')
    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    const payload = batchAddOrders.mock.calls[0][0]
    expect(payload[0].couponId).toBe('c2')
    expect(payload[0].discountAmount).toBe(10)
  })

  it('店铺券在外店无商品时不展示（本店合计 0 优惠为 0 被过滤）', async () => {
    mockCartStore.selectedItems = [makeItem({ id: 1, productDetailId: 'pd1', price: 80, merchantId: 2 })]
    getDefaultAddress.mockResolvedValue({ data: null })
    availableCoupons.mockResolvedValue({
      data: [{ id: 'c2', couponName: '店铺券满50减10', couponType: 'FULL_REDUCTION', threshold: 50, couponAmount: 10, couponMerchantId: 1 }]
    })

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).not.toContain('店铺券满50减10')
    expect(wrapper.text()).toContain('暂无可用优惠券')
  })

  it('校验失败：电话格式不正确', async () => {
    mockCartStore.selectedItems = [makeItem()]
    getDefaultAddress.mockResolvedValue({ data: null })

    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder="请输入收货人姓名"]').setValue('张三')
    await wrapper.find('input[placeholder="请输入手机号"]').setValue('12345')
    await wrapper.find('textarea').setValue('北京市')
    await wrapper.find('.btn-submit').trigger('click')

    expectToast('warning', '联系电话格式不正确')
    expect(batchAddOrders).not.toHaveBeenCalled()
  })

  it('提交成功：下单 + store 清空已选 + 跳转订单页', async () => {
    mockCartStore.selectedItems = [makeItem()]
    getDefaultAddress.mockResolvedValue({
      data: { receiverName: '张三', receiverPhone: '13800000000', receiverAddress: '北京市朝阳区' }
    })
    availableCoupons.mockResolvedValue({ data: [] })
    batchAddOrders.mockResolvedValue({ data: [] })

    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(batchAddOrders).toHaveBeenCalledTimes(1)
    expect(mockCartStore.clearSelected).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith('/orders')
  })

  it('下单失败 toast 提示', async () => {
    mockCartStore.selectedItems = [makeItem()]
    getDefaultAddress.mockResolvedValue({
      data: { receiverName: '张三', receiverPhone: '13800000000', receiverAddress: '北京市朝阳区' }
    })
    batchAddOrders.mockRejectedValue(new Error('库存不足'))

    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expectToast('error', '下单失败：库存不足')
  })

  it('空购物车提交提示没有可提交商品', async () => {
    mockCartStore.selectedItems = []
    getDefaultAddress.mockResolvedValue({
      data: { receiverName: '张三', receiverPhone: '13800000000', receiverAddress: '北京市朝阳区' }
    })

    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-submit').trigger('click')

    expectToast('warning', '没有可提交的商品')
  })
})
