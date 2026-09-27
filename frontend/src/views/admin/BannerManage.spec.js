import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import BannerManage from './BannerManage.vue'
import {
  adminListBanners,
  adminCreateBanner,
  adminUpdateBanner,
  adminDeleteBanner,
  adminToggleBanner
} from '../../api/banner'
import { toast } from '../../utils/toast'

vi.mock('../../api/banner', () => ({
  adminListBanners: vi.fn(),
  adminCreateBanner: vi.fn(),
  adminUpdateBanner: vi.fn(),
  adminDeleteBanner: vi.fn(),
  adminToggleBanner: vi.fn()
}))
vi.mock('../../utils/toast', () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

function bannerPayload() {
  return {
    data: {
      data: [
        { id: 'B1', title: '开学季大促', image: '/uploads/b1.jpg', linkUrl: '/products', sortOrder: 0, status: 'ON' },
        { id: 'B2', title: '新品首发', image: '/uploads/b2.jpg', linkUrl: null, sortOrder: 2, status: 'OFF' }
      ]
    }
  }
}

describe('admin/BannerManage 运营位管理', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染运营位列表与状态标签', async () => {
    adminListBanners.mockResolvedValue(bannerPayload())
    const wrapper = mount(BannerManage)
    await flushPromises()

    expect(wrapper.text()).toContain('开学季大促')
    expect(wrapper.text()).toContain('新品首发')
    expect(wrapper.findAll('.status-tag.on')).toHaveLength(1)
    expect(wrapper.findAll('.status-tag.off')).toHaveLength(1)
  })

  it('空列表显示空态', async () => {
    adminListBanners.mockResolvedValue({ data: { data: [] } })
    const wrapper = mount(BannerManage)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无运营位')
  })

  it('新建：缺少标题/图片被拦截，填写完整后调用创建接口', async () => {
    adminListBanners.mockResolvedValue({ data: { data: [] } })
    adminCreateBanner.mockResolvedValue({ data: { id: 'B9' } })
    const wrapper = mount(BannerManage)
    await flushPromises()

    await wrapper.find('.btn-new').trigger('click')
    const inputs = wrapper.findAll('.banner-form input')
    // 缺标题
    await wrapper.find('form.banner-form').trigger('submit')
    expect(adminCreateBanner).not.toHaveBeenCalled()
    expect(toast.error).toHaveBeenCalledWith('请填写运营位标题')
    // 补标题、缺图片
    await inputs[0].setValue('618 大促')
    await wrapper.find('form.banner-form').trigger('submit')
    expect(adminCreateBanner).not.toHaveBeenCalled()
    expect(toast.error).toHaveBeenCalledWith('请填写图片 URL')
    // 补图片 → 创建成功并刷新列表
    await inputs[1].setValue('/uploads/b.jpg')
    await wrapper.find('form.banner-form').trigger('submit')
    expect(adminCreateBanner).toHaveBeenCalledWith(expect.objectContaining({ title: '618 大促', image: '/uploads/b.jpg' }))
    expect(toast.success).toHaveBeenCalledWith('运营位已创建')
  })

  it('编辑：回填表单后保存调用更新接口', async () => {
    adminListBanners.mockResolvedValue(bannerPayload())
    adminUpdateBanner.mockResolvedValue({ data: {} })
    const wrapper = mount(BannerManage)
    await flushPromises()

    await wrapper.findAll('.btn-edit')[0].trigger('click')
    const inputs = wrapper.findAll('.banner-form input')
    expect(inputs[0].element.value).toBe('开学季大促')

    await inputs[0].setValue('开学季大促（改）')
    await wrapper.find('form.banner-form').trigger('submit')
    expect(adminUpdateBanner).toHaveBeenCalledWith(expect.objectContaining({ id: 'B1', title: '开学季大促（改）' }))
  })

  it('上下架与删除透传运营位 ID', async () => {
    adminListBanners.mockResolvedValue(bannerPayload())
    adminToggleBanner.mockResolvedValue({})
    adminDeleteBanner.mockResolvedValue({})
    const wrapper = mount(BannerManage)
    await flushPromises()

    await wrapper.findAll('.btn-toggle')[0].trigger('click')
    expect(adminToggleBanner).toHaveBeenCalledWith('B1', 'OFF')
    expect(toast.success).toHaveBeenCalledWith('已下架')

    await wrapper.findAll('.btn-delete')[1].trigger('click')
    expect(adminDeleteBanner).toHaveBeenCalledWith('B2')
    expect(toast.success).toHaveBeenCalledWith('已删除')
  })

  it('加载失败提示错误', async () => {
    adminListBanners.mockRejectedValue(new Error('network down'))
    mount(BannerManage)
    await flushPromises()

    expect(toast.error).toHaveBeenCalledWith('运营位加载失败')
  })
})
