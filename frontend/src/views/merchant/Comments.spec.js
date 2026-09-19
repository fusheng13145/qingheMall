import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Comments from './Comments.vue'
import { listMerchantComments, replyMerchantComment } from '../../api/merchant'
import { toast } from '../../utils/toast'

vi.mock('../../api/merchant', () => ({
  listMerchantComments: vi.fn(),
  replyMerchantComment: vi.fn()
}))
vi.mock('../../utils/toast', () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

function commentPayload() {
  return {
    data: {
      data: [
        { id: 'C1', userNickName: '买家甲', rating: 5, content: '很好', productName: '商品A', gmtCreated: '2026-09-19T10:00:00', replyContent: null },
        { id: 'C2', userNickName: '买家乙', rating: 3, content: '一般', productName: '商品B', gmtCreated: '2026-09-18T10:00:00', replyContent: '感谢反馈' }
      ]
    }
  }
}

describe('merchant/Comments 评价管理', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染评价列表与已回复标记', async () => {
    listMerchantComments.mockResolvedValue(commentPayload())
    const wrapper = mount(Comments)
    await flushPromises()

    expect(wrapper.text()).toContain('买家甲')
    expect(wrapper.text()).toContain('商品A')
    expect(wrapper.text()).toContain('感谢反馈') // C2 的商家回复
    // C1 未回复 → 有回复输入框；C2 已回复 → 无
    const inputs = wrapper.findAll('.reply-input')
    expect(inputs).toHaveLength(1)
  })

  it('回复成功后展示回复并清空输入', async () => {
    listMerchantComments.mockResolvedValue(commentPayload())
    replyMerchantComment.mockResolvedValue({})
    const wrapper = mount(Comments)
    await flushPromises()

    const input = wrapper.find('.reply-input')
    await input.setValue('感谢支持')
    await wrapper.find('.reply-btn').trigger('click')
    await flushPromises()

    expect(replyMerchantComment).toHaveBeenCalledWith('C1', '感谢支持')
    expect(toast.success).toHaveBeenCalledWith('回复成功')
    expect(wrapper.text()).toContain('感谢支持')
  })

  it('空回复被拦截', async () => {
    listMerchantComments.mockResolvedValue(commentPayload())
    const wrapper = mount(Comments)
    await flushPromises()

    await wrapper.find('.reply-btn').trigger('click')
    expect(toast.error).toHaveBeenCalledWith('回复内容不能为空')
    expect(replyMerchantComment).not.toHaveBeenCalled()
  })

  it('回复失败 toast 提示', async () => {
    listMerchantComments.mockResolvedValue(commentPayload())
    replyMerchantComment.mockRejectedValue(new Error('fail'))
    const wrapper = mount(Comments)
    await flushPromises()

    const input = wrapper.find('.reply-input')
    await input.setValue('回复内容')
    await wrapper.find('.reply-btn').trigger('click')
    await flushPromises()

    expect(toast.error).toHaveBeenCalledWith('回复失败')
  })

  it('空评价列表显示空态', async () => {
    listMerchantComments.mockResolvedValue({ data: { data: [] } })
    const wrapper = mount(Comments)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无本店评价')
  })
})
