import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Login from './Login.vue'

const { push, route } = vi.hoisted(() => ({ push: vi.fn(), route: { query: {} } }))
const { mockUserStore } = vi.hoisted(() => ({
  mockUserStore: { login: vi.fn() }
}))

vi.mock('vue-router', () => ({ useRouter: () => ({ push }), useRoute: () => route }))
vi.mock('../stores/user', () => ({ useUserStore: () => mockUserStore }))

beforeEach(() => {
  vi.clearAllMocks()
  push.mockClear()
  Object.assign(route, { query: {} })
})

describe('Login 登录页', () => {
  it('空用户名提示', async () => {
    const wrapper = mount(Login)
    await wrapper.find('.btn-submit').trigger('click')
    expect(wrapper.text()).toContain('请输入用户名')
  })

  it('空密码提示', async () => {
    const wrapper = mount(Login)
    await wrapper.find('input[type="text"]').setValue('alice')
    await wrapper.find('.btn-submit').trigger('click')
    expect(wrapper.text()).toContain('请输入密码')
  })

  it('登录成功无 redirect 跳转首页', async () => {
    mockUserStore.login.mockResolvedValue({ success: true })
    const wrapper = mount(Login)
    await wrapper.find('input[type="text"]').setValue('alice')
    await wrapper.find('input[type="password"]').setValue('123456')
    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(mockUserStore.login).toHaveBeenCalledWith('alice', '123456')
    expect(push).toHaveBeenCalledWith('/')
  })

  it('登录成功站内 redirect 跳转原页面', async () => {
    route.query = { redirect: '/checkout' }
    mockUserStore.login.mockResolvedValue({ success: true })
    const wrapper = mount(Login)
    await wrapper.find('input[type="text"]').setValue('alice')
    await wrapper.find('input[type="password"]').setValue('123456')
    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(push).toHaveBeenCalledWith('/checkout')
  })

  it('开放重定向防护：外部域名 redirect 拒绝回首页', async () => {
    route.query = { redirect: 'https://evil.com/phish' }
    mockUserStore.login.mockResolvedValue({ success: true })
    const wrapper = mount(Login)
    await wrapper.find('input[type="text"]').setValue('alice')
    await wrapper.find('input[type="password"]').setValue('123456')
    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(push).toHaveBeenCalledWith('/')
  })

  it('开放重定向防护：协议相对 redirect（//）拒绝回首页', async () => {
    route.query = { redirect: '//evil.com' }
    mockUserStore.login.mockResolvedValue({ success: true })
    const wrapper = mount(Login)
    await wrapper.find('input[type="text"]').setValue('alice')
    await wrapper.find('input[type="password"]').setValue('123456')
    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(push).toHaveBeenCalledWith('/')
  })

  it('登录失败显示后端错误', async () => {
    mockUserStore.login.mockResolvedValue({ success: false, message: '用户名或密码错误' })
    const wrapper = mount(Login)
    await wrapper.find('input[type="text"]').setValue('alice')
    await wrapper.find('input[type="password"]').setValue('wrong')
    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('用户名或密码错误')
    expect(push).not.toHaveBeenCalled()
  })
})
