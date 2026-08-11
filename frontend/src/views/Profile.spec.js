import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Profile from './Profile.vue'
import { toasts } from '../utils/toast'

const { updateProfileApi } = vi.hoisted(() => ({ updateProfileApi: vi.fn() }))
const { getMerchantInfo, applyMerchant } = vi.hoisted(() => ({
  getMerchantInfo: vi.fn(),
  applyMerchant: vi.fn()
}))
const { listAddress, addAddress, updateAddress, deleteAddress, setDefaultAddress } = vi.hoisted(() => ({
  listAddress: vi.fn(),
  addAddress: vi.fn(),
  updateAddress: vi.fn(),
  deleteAddress: vi.fn(),
  setDefaultAddress: vi.fn()
}))
const { mockUserStore } = vi.hoisted(() => ({
  mockUserStore: { userName: 'alice', nickName: '小禾', role: 'USER' }
}))

vi.mock('../stores/user', () => ({ useUserStore: () => mockUserStore }))
vi.mock('../api/user', () => ({ updateProfileApi }))
vi.mock('../api/merchant', () => ({ getMerchantInfo, applyMerchant }))
vi.mock('../api/address', () => ({ listAddress, addAddress, updateAddress, deleteAddress, setDefaultAddress }))

function makeAddress(overrides = {}) {
  return {
    id: 'ad1',
    receiverName: '张三',
    receiverPhone: '13800138000',
    receiverAddress: '杭州市西湖区文一路 1 号',
    isDefault: false,
    ...overrides
  }
}

/** 页面挂载时默认拉取地址列表与商家入驻状态 */
function mockInit({ addresses = [], merchant = null } = {}) {
  listAddress.mockResolvedValue({ data: addresses })
  getMerchantInfo.mockResolvedValue({ data: merchant })
}

function mountPage() {
  return mount(Profile, { global: { stubs: { 'router-link': { template: '<a><slot /></a>' } } } })
}

function findButtonByText(wrapper, text) {
  return wrapper.findAll('button').find(b => b.text().includes(text))
}

function expectToast(type, message) {
  expect(toasts.some(t => t.type === type && t.message === message)).toBe(true)
}

beforeEach(() => {
  vi.clearAllMocks()
  Object.assign(mockUserStore, { userName: 'alice', nickName: '小禾', role: 'USER' })
  global.confirm = vi.fn(() => true)
  toasts.splice(0, toasts.length)
})

describe('Profile 个人中心页', () => {
  it('渲染个人资料与地址列表', async () => {
    mockInit({ addresses: [makeAddress({ isDefault: true }), makeAddress({ id: 'ad2', receiverName: '李四' })] })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('alice')
    expect(wrapper.text()).toContain('普通用户')
    expect(wrapper.find('input[placeholder="请输入昵称"]').element.value).toBe('小禾')
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('李四')
    expect(wrapper.find('.addr-default').text()).toBe('默认')
    // 默认地址不展示「设为默认」按钮
    const cards = wrapper.findAll('.address-card')
    expect(cards[0].text()).not.toContain('设为默认')
    expect(cards[1].text()).toContain('设为默认')
    // 未入驻商家展示开店入口
    expect(findButtonByText(wrapper, '我要开店').exists()).toBe(true)
  })

  it('保存昵称与头像成功更新资料', async () => {
    mockInit()
    updateProfileApi.mockResolvedValue({ data: { nickName: '新昵称', avatar: '/uploads/a.jpg' } })
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('input[placeholder="请输入昵称"]').setValue('新昵称')
    await wrapper.find('input[placeholder^="请输入头像图片"]').setValue('/uploads/a.jpg')
    await findButtonByText(wrapper, '保存昵称').trigger('click')
    await flushPromises()

    expect(updateProfileApi).toHaveBeenCalledWith('新昵称', '/uploads/a.jpg')
    expectToast('success', '资料已更新')
    expect(mockUserStore.nickName).toBe('新昵称')
    expect(wrapper.find('.avatar-img').attributes('src')).toBe('/uploads/a.jpg')
  })

  it('保存资料失败 toast 提示', async () => {
    mockInit()
    updateProfileApi.mockRejectedValue(new Error('服务端错误'))
    const wrapper = mountPage()
    await flushPromises()

    await findButtonByText(wrapper, '保存昵称').trigger('click')
    await flushPromises()

    expectToast('error', '保存失败：服务端错误')
  })

  it('新增地址成功后关闭弹窗并刷新列表', async () => {
    mockInit()
    addAddress.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await findButtonByText(wrapper, '新增地址').trigger('click')
    expect(wrapper.find('.modal-header h3').text()).toBe('新增地址')

    await wrapper.find('input[placeholder="请输入收货人姓名"]').setValue('王五')
    await wrapper.find('input[placeholder="请输入手机号"]').setValue('13900139000')
    await wrapper.find('textarea[placeholder^="省市区"]').setValue('北京市朝阳区')
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expect(addAddress).toHaveBeenCalledWith({
      receiverName: '王五',
      receiverPhone: '13900139000',
      receiverAddress: '北京市朝阳区',
      isDefault: false
    })
    expect(wrapper.find('.modal-overlay').exists()).toBe(false)
    // onMounted 1 次 + 保存后刷新 1 次
    expect(listAddress).toHaveBeenCalledTimes(2)
  })

  it('新增地址校验：缺少收货人/手机号格式/缺少地址', async () => {
    mockInit()
    const wrapper = mountPage()
    await flushPromises()

    await findButtonByText(wrapper, '新增地址').trigger('click')
    const nameInput = wrapper.find('input[placeholder="请输入收货人姓名"]')
    const phoneInput = wrapper.find('input[placeholder="请输入手机号"]')
    const confirmBtn = wrapper.find('.btn-confirm')

    await confirmBtn.trigger('click')
    expectToast('warning', '请填写收货人')

    await nameInput.setValue('王五')
    await phoneInput.setValue('123')
    await confirmBtn.trigger('click')
    expectToast('warning', '请输入正确的手机号')

    await phoneInput.setValue('13900139000')
    await confirmBtn.trigger('click')
    expectToast('warning', '请填写收货地址')

    expect(addAddress).not.toHaveBeenCalled()
  })

  it('编辑地址回填表单并调用更新接口', async () => {
    mockInit({ addresses: [makeAddress()] })
    updateAddress.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await findButtonByText(wrapper, '编辑').trigger('click')
    expect(wrapper.find('.modal-header h3').text()).toBe('编辑地址')
    expect(wrapper.find('input[placeholder="请输入收货人姓名"]').element.value).toBe('张三')

    await wrapper.find('textarea[placeholder^="省市区"]').setValue('杭州市余杭区')
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expect(updateAddress).toHaveBeenCalledWith({
      id: 'ad1',
      receiverName: '张三',
      receiverPhone: '13800138000',
      receiverAddress: '杭州市余杭区',
      isDefault: false
    })
    expect(wrapper.find('.modal-overlay').exists()).toBe(false)
  })

  it('删除地址：确认后调用接口并刷新', async () => {
    mockInit({ addresses: [makeAddress()] })
    deleteAddress.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await findButtonByText(wrapper, '删除').trigger('click')
    await flushPromises()

    expect(global.confirm).toHaveBeenCalled()
    expect(deleteAddress).toHaveBeenCalledWith('ad1')
    expect(listAddress).toHaveBeenCalledTimes(2)
  })

  it('删除地址：取消确认不调用接口', async () => {
    global.confirm = vi.fn(() => false)
    mockInit({ addresses: [makeAddress()] })
    const wrapper = mountPage()
    await flushPromises()

    await findButtonByText(wrapper, '删除').trigger('click')
    await flushPromises()

    expect(deleteAddress).not.toHaveBeenCalled()
  })

  it('设为默认调用接口', async () => {
    mockInit({ addresses: [makeAddress({ id: 'ad2', receiverName: '李四' })] })
    setDefaultAddress.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await findButtonByText(wrapper, '设为默认').trigger('click')
    await flushPromises()

    expect(setDefaultAddress).toHaveBeenCalledWith('ad2')
    expect(listAddress).toHaveBeenCalledTimes(2)
  })

  it('加载地址失败 toast 提示', async () => {
    listAddress.mockRejectedValue(new Error('net'))
    getMerchantInfo.mockResolvedValue({ data: null })
    mountPage()
    await flushPromises()

    expectToast('error', '加载地址失败：net')
  })

  it('商家入驻审核中展示审核中徽标', async () => {
    mockInit({ merchant: { status: 'PENDING', shopName: '青禾小铺' } })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('青禾小铺')
    expect(wrapper.find('.merchant-badge').text()).toBe('审核中')
  })

  it('商家入驻通过展示工作台入口并同步商家角色', async () => {
    mockInit({ merchant: { status: 'ACTIVE', shopName: '青禾小铺' } })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.merchant-box a').text()).toContain('进入商家工作台')
    expect(mockUserStore.role).toBe('MERCHANT')
  })

  it('商家入驻被驳回展示原因与重新申请入口', async () => {
    mockInit({ merchant: { status: 'REJECTED', rejectReason: '资料不全' } })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('资料不全')
    expect(findButtonByText(wrapper, '重新申请').exists()).toBe(true)
  })

  it('提交开店申请成功后刷新入驻状态', async () => {
    mockInit()
    applyMerchant.mockResolvedValue({ data: { status: 'PENDING' } })
    const wrapper = mountPage()
    await flushPromises()

    await findButtonByText(wrapper, '我要开店').trigger('click')
    expect(wrapper.find('.modal-header h3').text()).toBe('我要开店')

    await wrapper.find('input[placeholder="请输入店铺名称"]').setValue('青禾小铺')
    await wrapper.find('textarea[placeholder^="简要描述您的店铺"]').setValue('好物集合')
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expect(applyMerchant).toHaveBeenCalledWith('青禾小铺', '', '好物集合')
    expectToast('success', '开店申请已提交，平台审核通过后即可经营')
    expect(wrapper.find('.modal-overlay').exists()).toBe(false)
    // onMounted 1 次 + 提交后刷新 1 次
    expect(getMerchantInfo).toHaveBeenCalledTimes(2)
  })

  it('开店弹窗未填店铺名时提交按钮禁用', async () => {
    mockInit()
    const wrapper = mountPage()
    await flushPromises()

    await findButtonByText(wrapper, '我要开店').trigger('click')
    expect(wrapper.find('.btn-confirm').attributes('disabled')).toBeDefined()
  })
})
