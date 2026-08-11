import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { toasts, toast, dismissToast, apiError } from './toast'

/**
 * 全局 Toast 工具测试（P2-13：替换 alert 的消息基础设施）。
 */
describe('utils/toast', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    toasts.splice(0, toasts.length)
  })

  afterEach(() => {
    vi.useRealTimers()
    toasts.splice(0, toasts.length)
  })

  it('toast 推入队列并携带类型，超时自动移除', () => {
    toast('操作完成', 'success')
    expect(toasts).toHaveLength(1)
    expect(toasts[0].message).toBe('操作完成')
    expect(toasts[0].type).toBe('success')

    vi.advanceTimersByTime(2000) // success 默认 2000ms
    expect(toasts).toHaveLength(0)
  })

  it('toast.success / error / warning 语法糖写入对应类型', () => {
    toast.success('成功')
    toast.error('失败')
    toast.warning('请注意')
    expect(toasts.map(t => t.type)).toEqual(['success', 'error', 'warning'])
  })

  it('空消息不推入队列', () => {
    toast('')
    toast(null)
    expect(toasts).toHaveLength(0)
  })

  it('dismissToast 手动移除指定条目', () => {
    toast('第一条')
    toast('第二条')
    expect(toasts).toHaveLength(2)
    dismissToast(toasts[0].id)
    expect(toasts).toHaveLength(1)
    expect(toasts[0].message).toBe('第二条')
  })

  it('超过上限时丢弃最早条目，防止无限堆积', () => {
    for (let i = 0; i < 8; i++) toast(`msg-${i}`)
    expect(toasts).toHaveLength(5)
    expect(toasts[0].message).toBe('msg-3')
  })

  it('apiError 展示「前缀：后端消息」', () => {
    apiError(new Error('库存不足'), '下单失败')
    expect(toasts).toHaveLength(1)
    expect(toasts[0].type).toBe('error')
    expect(toasts[0].message).toBe('下单失败：库存不足')
  })

  it('apiError 对已全局处理的错误（如 401 跳转）不重复提示', () => {
    const handled = new Error('登录已过期')
    handled.handled = true
    apiError(handled, '加载失败')
    expect(toasts).toHaveLength(0)
  })

  it('apiError 无消息时使用兜底文案', () => {
    apiError(new Error(''), '操作失败')
    expect(toasts[0].message).toBe('操作失败：请稍后重试')
  })
})
