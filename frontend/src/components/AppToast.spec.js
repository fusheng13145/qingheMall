import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import AppToast from './common/AppToast.vue'
import { toasts, toast } from '../utils/toast'

describe('common/AppToast 全局提示容器', () => {
  beforeEach(() => {
    // 清空真实 toast 队列
    toasts.splice(0, toasts.length)
  })

  it('空队列时不渲染任何提示条', () => {
    const wrapper = mount(AppToast)

    expect(wrapper.findAll('.toast-item')).toHaveLength(0)
    expect(wrapper.find('.toast-stack').exists()).toBe(true)
  })

  it('渲染队列中的提示条并携带类型样式', async () => {
    toast.success('保存成功')
    toast.error('加载失败')
    const wrapper = mount(AppToast)

    await wrapper.vm.$nextTick()
    const items = wrapper.findAll('.toast-item')
    expect(items).toHaveLength(2)
    expect(items[0].classes()).toContain('toast-success')
    expect(items[1].classes()).toContain('toast-error')
    expect(items[0].text()).toBe('保存成功')
  })

  it('点击提示条可提前关闭（dismissToast 移除队列项）', async () => {
    toast.warning('点我关闭')
    const wrapper = mount(AppToast)
    await wrapper.vm.$nextTick()

    expect(toasts).toHaveLength(1)
    await wrapper.findAll('.toast-item')[0].trigger('click')

    expect(toasts).toHaveLength(0)
  })
})
