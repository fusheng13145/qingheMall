import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Users from './Users.vue'
import { toasts } from '../../utils/toast'

// vi.hoisted：mock 工厂与测试体共享同一组 api 引用
const { getUserList, updateUserRole } = vi.hoisted(() => ({
  getUserList: vi.fn(),
  updateUserRole: vi.fn()
}))

vi.mock('../../api/admin', () => ({
  getUserList: (...a) => getUserList(...a),
  updateUserRole: (...a) => updateUserRole(...a)
}))

function makeUser(overrides = {}) {
  return { id: 1, userName: 'user01', nickName: '小禾', role: 'USER', gmtCreated: '2026-01-02T03:04:00', ...overrides }
}

/** res.data 为 Paging<User>，列表在 data 字段 */
function mockPaging(list, extra = {}) {
  getUserList.mockResolvedValue({ data: { data: list, totalCount: list.length, totalPage: 1, ...extra } })
}

function mountPage() {
  return mount(Users)
}

function expectToast(type, message) {
  expect(toasts.some(t => t.type === type && t.message === message)).toBe(true)
}

beforeEach(() => {
  vi.clearAllMocks()
  global.confirm = vi.fn(() => true)
  toasts.splice(0, toasts.length)
})

describe('admin/Users 用户管理', () => {
  it('加载并渲染用户列表（含角色徽章与时间格式化）', async () => {
    mockPaging([makeUser(), makeUser({ id: 2, userName: 'admin01', nickName: '管理员阿青', role: 'ADMIN' })])
    const wrapper = mountPage()
    await flushPromises()

    expect(getUserList).toHaveBeenCalledTimes(1)
    expect(getUserList).toHaveBeenCalledWith(1, 20)
    expect(wrapper.text()).toContain('user01')
    expect(wrapper.text()).toContain('小禾')
    expect(wrapper.text()).toContain('admin01')
    // ADMIN 渲染为「管理员」徽章，USER 渲染为「普通用户」
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).toContain('普通用户')
    // ISO 时间格式化为 YYYY-MM-DD HH:mm
    expect(wrapper.text()).toContain('2026-01-02 03:04')
    // USER 行显示「设为管理员」，ADMIN 行显示「设为普通用户」
    expect(wrapper.find('.btn-promote').exists()).toBe(true)
    expect(wrapper.find('.btn-demote').exists()).toBe(true)
  })

  it('空列表显示空态文案', async () => {
    mockPaging([])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无用户数据')
  })

  it('加载失败 toast 提示', async () => {
    getUserList.mockRejectedValue(new Error('net'))
    mountPage()
    await flushPromises()

    expectToast('error', '加载用户列表失败：net')
  })

  it('设为管理员：确认后调用接口并就地更新角色', async () => {
    mockPaging([makeUser()])
    updateUserRole.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-promote').trigger('click')
    await flushPromises()

    expect(global.confirm).toHaveBeenCalled()
    expect(updateUserRole).toHaveBeenCalledWith(1, 'ADMIN')
    // 成功后不重新拉列表，直接就地更新该行角色
    expect(getUserList).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.role-badge').text()).toBe('管理员')
    expect(wrapper.find('.btn-demote').exists()).toBe(true)
  })

  it('角色修改：取消确认不调用接口', async () => {
    global.confirm = vi.fn(() => false)
    mockPaging([makeUser()])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-promote').trigger('click')
    await flushPromises()

    expect(updateUserRole).not.toHaveBeenCalled()
    expect(wrapper.find('.role-badge').text()).toBe('普通用户')
  })

  it('角色修改失败 toast 提示且角色保持不变', async () => {
    mockPaging([makeUser()])
    updateUserRole.mockRejectedValue(new Error('服务端错误'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-promote').trigger('click')
    await flushPromises()

    expectToast('error', '角色修改失败：服务端错误')
    expect(wrapper.find('.role-badge').text()).toBe('普通用户')
    expect(wrapper.find('.btn-promote').exists()).toBe(true)
  })

  it('管理员降级为普通用户：调用 updateUserRole 传 USER', async () => {
    mockPaging([makeUser({ role: 'ADMIN' })])
    updateUserRole.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-demote').trigger('click')
    await flushPromises()

    expect(updateUserRole).toHaveBeenCalledWith(1, 'USER')
    expect(wrapper.find('.role-badge').text()).toBe('普通用户')
    expect(wrapper.find('.btn-promote').exists()).toBe(true)
  })

  it('多页时显示分页栏且第一页「上一页」禁用', async () => {
    mockPaging([makeUser()], { totalCount: 45, totalPage: 3 })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.pagination-bar').exists()).toBe(true)
    expect(wrapper.find('.page-info').text()).toBe('第 1 / 3 页（共 45 条）')
    const buttons = wrapper.findAll('.page-btn')
    expect(buttons[0].attributes('disabled')).toBeDefined()
    expect(buttons[1].attributes('disabled')).toBeUndefined()
  })

  it('分页切换：下一页/上一页携带正确页码重新请求', async () => {
    const page1 = { data: { data: [makeUser()], totalCount: 45, totalPage: 3 } }
    const page2 = { data: { data: [makeUser({ id: 21 })], totalCount: 45, totalPage: 3 } }
    getUserList.mockResolvedValue(page1)
    getUserList.mockResolvedValueOnce(page1)
    getUserList.mockResolvedValueOnce(page2)
    const wrapper = mountPage()
    await flushPromises()

    const buttons = wrapper.findAll('.page-btn')
    await buttons[1].trigger('click')
    await flushPromises()
    expect(getUserList).toHaveBeenCalledWith(2, 20)
    expect(wrapper.find('.page-info').text()).toBe('第 2 / 3 页（共 45 条）')

    await wrapper.findAll('.page-btn')[0].trigger('click')
    await flushPromises()
    expect(getUserList).toHaveBeenLastCalledWith(1, 20)
    expect(wrapper.find('.page-info').text()).toBe('第 1 / 3 页（共 45 条）')
  })

  it('仅一页时不显示分页栏', async () => {
    mockPaging([makeUser()], { totalCount: 1, totalPage: 1 })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.pagination-bar').exists()).toBe(false)
  })
})
