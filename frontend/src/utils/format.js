/**
 * 通用格式化工具（从页面组件中提取的纯函数，便于单测）。
 */

/** 金额格式化：转数字并保留两位小数，非法输入返回 0.00 */
export function formatPrice(price) {
  const n = Number(price || 0)
  if (Number.isNaN(n)) return '0.00'
  return n.toFixed(2)
}

/** 规格尺寸格式化：38.0 / 38.5 → "38" / "38.5"，空值返回 '' */
export function formatSize(size) {
  if (size === null || size === undefined || size === '') return ''
  const n = Number(size)
  if (Number.isNaN(n)) return String(size)
  return String(n)
}

/**
 * 时间格式化：输入时间/日期串 → "YYYY-MM-DD HH:mm:ss"
 * 非法输入原样返回。
 */
export function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return String(time)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/** 图片串拆分：兼容空格与分号分隔，过滤空项 */
export function splitImgs(str) {
  if (!str) return []
  return str
    .split(/[;\s]+/)
    .map(s => s.trim())
    .filter(Boolean)
}

/** 取图片串第一张图（列表卡片用），无图返回 null */
export function firstImg(str) {
  const list = splitImgs(str)
  return list.length > 0 ? list[0] : null
}

/** 订单状态 → 中文文案（与后端 OrderStatus 枚举一致） */
export function statusText(status) {
  const map = {
    WAIT_BUYER_PAY: '待付款',
    TRADE_PAID_SUCCESS: '待发货',
    TRADE_CLOSED: '已关闭',
    TRADE_PAID_FAILED: '支付失败',
    TRADE_SHIPPED: '待收货',
    TRADE_COMPLETED: '已完成',
    TRADE_REFUNDING: '退款中',
    TRADE_REFUNDED: '已退款'
  }
  return map[status] || '未知'
}
