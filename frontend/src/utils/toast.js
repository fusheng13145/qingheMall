import { reactive } from 'vue'

/**
 * 全局轻量 Toast 通知（P2-13：替换阻塞式 window.alert）。
 *
 * 用法：
 *   import { toast, apiError } from '../utils/toast'
 *   toast.success('创建成功')
 *   toast.warning('请填写商品名称')
 *   apiError(e, '创建失败')          // catch 块统一提示 API 失败
 *
 * 实现：模块级响应式队列 + 定时自动移除；由 App.vue 中的 <AppToast />
 * 单点渲染。不依赖组件实例，普通 JS 模块（如拦截器）亦可直接调用。
 */

let seq = 0

/** 当前待展示的 toast 队列（AppToast 渲染源） */
export const toasts = reactive([])

const DEFAULT_DURATION = { success: 2000, info: 2500, warning: 2500, error: 3000 }
const MAX_VISIBLE = 5

/**
 * 推送一条 toast。
 * @param {string} message 文案
 * @param {'success'|'error'|'warning'|'info'} type 语义类型（决定配色与默认时长）
 * @param {number} [duration] 展示毫秒数，缺省按类型取默认值
 */
export function toast(message, type = 'info', duration) {
  if (!message) return
  const item = { id: ++seq, message: String(message), type }
  toasts.push(item)
  // 防止异常场景下无限堆积：超出上限移除最早一条
  if (toasts.length > MAX_VISIBLE) {
    toasts.splice(0, toasts.length - MAX_VISIBLE)
  }
  const ms = duration ?? DEFAULT_DURATION[type] ?? 2500
  setTimeout(() => dismissToast(item.id), ms)
}

toast.success = (message, duration) => toast(message, 'success', duration)
toast.error = (message, duration) => toast(message, 'error', duration)
toast.warning = (message, duration) => toast(message, 'warning', duration)
toast.info = (message, duration) => toast(message, 'info', duration)

/** 手动移除指定 toast（点击提前关闭） */
export function dismissToast(id) {
  const idx = toasts.findIndex(t => t.id === id)
  if (idx >= 0) toasts.splice(idx, 1)
}

/**
 * API 失败统一提示（供 catch 块使用）：展示「前缀：后端 message」。
 * 若错误已被全局处理（e.handled=true，如 401 会话过期已跳转登录），
 * 则不再重复弹窗打扰用户。
 * @param {Error} e 拦截器 reject 的错误对象
 * @param {string} prefix 业务动作前缀，如「创建失败」
 */
export function apiError(e, prefix = '操作失败') {
  if (e && e.handled) return
  const detail = (e && e.message) || '请稍后重试'
  toast.error(`${prefix}：${detail}`)
}

export default toast
