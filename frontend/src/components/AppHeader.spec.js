import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { reactive, nextTick } from 'vue'
import AppHeader from './AppHeader.vue'

// vi.hoisted：mock 工厂与测试体共享同一组可控引用
const ctx = vi.hoisted(() => ({
  user: null,
  theme: null,
  cart: null,
  push: vi.fn()
}))

vi.mock('../stores/user', () => ({ useUserStore: () => ctx.user }))
vi.mock('../stores/theme', () => ({ useThemeStore: () => ctx.theme }))
vi.mock('../stores/cart', () => ({ useCartStore: () => ctx.cart }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: (...a) => ctx.push(...a) }) }))

function setUser(overrides = {}) {
  ctx.user = reactive({
    isLoggedIn: false,
    nickName: '',
    isAdmin: false,
    isMerchant: false,
    logout: vi.fn(),
    ...overrides
  })
}

function setCart(overrides = {}) {
  ctx.cart = reactive({
    count: 0,
    refreshCount: vi.fn(),
    reset: vi.fn(),
    ...overrides
  })
}

/** effective 为可控主题值，getEffectiveTheme 读取它以便 computed 追踪 */
function setTheme(effective = 'light') {
  ctx.theme = reactive({
    effective,
    toggleTheme: vi.fn(),
    getEffectiveTheme() {
      return ctx.theme.effective
    }
  })
}

function mountHeader() {
  return mount(AppHeader, {
    global: {
      stubs: { 'router-link': { template: '<a><slot /></a>' } }
    }
  })
}

beforeEach(() => {
  ctx.push.mockClear()
  setUser()
  setCart()
  setTheme()
})

describe('AppHeader 顶部导航', () => {
  it('渲染品牌与主导航链接', () => {
    const wrapper = mountHeader()
    expect(wrapper.text()).toContain('青禾商城')
    const navText = wrapper.find('.nav-links').text()
    for (const label of ['首页', '商品', '秒杀', '领券中心', '订单', '我的券', '购物车', '我的']) {
      expect(navText).toContain(label)
    }
  })

  it('未登录显示登录/注册入口，不显示用户信息与退出', () => {
    const wrapper = mountHeader()
    expect(wrapper.find('.btn-login').exists()).toBe(true)
    expect(wrapper.find('.btn-register').exists()).toBe(true)
    expect(wrapper.find('.btn-logout').exists()).toBe(false)
    expect(wrapper.find('.user-info').exists()).toBe(false)
  })

  it('登出态挂载触发 cartStore.reset() 而非 refreshCount()', () => {
    mountHeader()
    expect(ctx.cart.reset).toHaveBeenCalledTimes(1)
    expect(ctx.cart.refreshCount).not.toHaveBeenCalled()
  })

  it('登录态挂载显示用户名/头像首字符/退出按钮，并刷新购物车角标', () => {
    setUser({ isLoggedIn: true, nickName: '小青' })
    const wrapper = mountHeader()

    expect(ctx.cart.refreshCount).toHaveBeenCalledTimes(1)
    expect(ctx.cart.reset).not.toHaveBeenCalled()
    expect(wrapper.find('.user-name').text()).toBe('小青')
    expect(wrapper.find('.avatar').text()).toBe('小')
    expect(wrapper.find('.btn-logout').exists()).toBe(true)
    expect(wrapper.find('.btn-login').exists()).toBe(false)
  })

  it('登录状态切换时 watch 同步购物车（登录刷新、登出清零）', async () => {
    mountHeader()
    expect(ctx.cart.reset).toHaveBeenCalledTimes(1)

    ctx.user.isLoggedIn = true
    await nextTick()
    expect(ctx.cart.refreshCount).toHaveBeenCalledTimes(1)

    ctx.user.isLoggedIn = false
    await nextTick()
    expect(ctx.cart.reset).toHaveBeenCalledTimes(2)
  })

  it('管理员登录后显示管理后台入口', () => {
    setUser({ isLoggedIn: true, nickName: '管理员', isAdmin: true })
    const wrapper = mountHeader()
    expect(wrapper.find('.admin-link').text()).toContain('管理后台')
    expect(wrapper.text()).not.toContain('商家工作台')
  })

  it('商家登录后显示商家工作台入口', () => {
    setUser({ isLoggedIn: true, nickName: '商家', isMerchant: true })
    const wrapper = mountHeader()
    expect(wrapper.find('.admin-link').text()).toContain('商家工作台')
    expect(wrapper.text()).not.toContain('管理后台')
  })

  it('购物车角标：0 不显示，正常显示数量，超过 99 显示 99+', async () => {
    const wrapper = mountHeader()
    expect(wrapper.find('.cart-badge').exists()).toBe(false)

    ctx.cart.count = 5
    await nextTick()
    expect(wrapper.find('.cart-badge').text()).toBe('5')

    ctx.cart.count = 120
    await nextTick()
    expect(wrapper.find('.cart-badge').text()).toBe('99+')
  })

  it('主题切换按钮：标题随当前主题变化，点击调用 toggleTheme', async () => {
    const wrapper = mountHeader()
    const btn = wrapper.find('.theme-toggle')
    expect(btn.attributes('title')).toBe('切换到暗色模式')

    await btn.trigger('click')
    expect(ctx.theme.toggleTheme).toHaveBeenCalledTimes(1)

    ctx.theme.effective = 'dark'
    await nextTick()
    expect(btn.attributes('title')).toBe('切换到亮色模式')
  })

  it('点击退出调用 userStore.logout() 并跳转首页', async () => {
    setUser({ isLoggedIn: true, nickName: '小青' })
    const wrapper = mountHeader()

    await wrapper.find('.btn-logout').trigger('click')
    await flushPromises()

    expect(ctx.user.logout).toHaveBeenCalledTimes(1)
    expect(ctx.push).toHaveBeenCalledWith('/')
  })
})
