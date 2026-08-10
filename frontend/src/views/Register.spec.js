import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Register from './Register.vue'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
const { reg } = vi.hoisted(() => ({ reg: vi.fn() }))

vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../api/user', () => ({ reg }))

async function fillForm(wrapper, { name = 'alice', pwd = '123456', confirm = '123456', shop = '' } = {}) {
  if (name !== null) await wrapper.find('input[placeholder*="用户名"]').setValue(name)
  await wrapper.find('input[placeholder*="密码"]').setValue(pwd)
  await wrapper.find('input[placeholder*="再次输入"]').setValue(confirm)
  if (shop) await wrapper.find('input[placeholder*="店铺"]').setValue(shop)
}

function switchToMerchant(wrapper) {
  return wrapper.findAll('.role-option')[1].trigger('click')
}

beforeEach(() => {
  vi.clearAllMocks()
  push.mockClear()
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('Register 注册页', () => {
  it('空用户名提示', async () => {
    const wrapper = mount(Register)
    await wrapper.find('.btn-submit').trigger('click')
    expect(wrapper.text()).toContain('请输入用户名')
  })

  it('密码过短提示', async () => {
    const wrapper = mount(Register)
    await fillForm(wrapper, { pwd: '123' })
    await wrapper.find('.btn-submit').trigger('click')
    expect(wrapper.text()).toContain('密码长度不能少于6位')
  })

  it('两次密码不一致提示', async () => {
    const wrapper = mount(Register)
    await fillForm(wrapper, { confirm: '654321' })
    await wrapper.find('.btn-submit').trigger('click')
    expect(wrapper.text()).toContain('两次输入的密码不一致')
  })

  it('商家注册未填店名提示', async () => {
    reg.mockResolvedValue({})
    const wrapper = mount(Register)
    // 切换到商家角色（role select）
    await switchToMerchant(wrapper)
    await fillForm(wrapper, { shop: '' })
    await wrapper.find('.btn-submit').trigger('click')
    expect(wrapper.text()).toContain('商家注册请填写店铺名称')
    expect(reg).not.toHaveBeenCalled()
  })

  it('普通用户注册成功跳转登录页', async () => {
    reg.mockResolvedValue({})
    const wrapper = mount(Register)
    await fillForm(wrapper)
    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(reg).toHaveBeenCalledWith('alice', '123456', 'USER', '')
    expect(wrapper.text()).toContain('注册成功')

    // 1.8s 后跳转登录页
    vi.advanceTimersByTime(1800)
    expect(push).toHaveBeenCalledWith('/login')
  })

  it('商家注册成功提示入驻审核', async () => {
    reg.mockResolvedValue({})
    const wrapper = mount(Register)
    await switchToMerchant(wrapper)
    await fillForm(wrapper, { shop: '青禾小铺' })
    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(reg).toHaveBeenCalledWith('alice', '123456', 'MERCHANT', '青禾小铺')
    expect(wrapper.text()).toContain('入驻申请已提交')
  })

  it('注册失败显示后端错误', async () => {
    reg.mockRejectedValue(new Error('用户名已存在'))
    const wrapper = mount(Register)
    await fillForm(wrapper)
    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('用户名已存在')
  })
})
