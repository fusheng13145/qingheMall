import { describe, it, expect } from 'vitest'
import { formatPrice, formatSize, formatTime, splitImgs, firstImg, statusText } from './format'

describe('formatPrice 金额格式化', () => {
  it('数字保留两位小数', () => {
    expect(formatPrice(799)).toBe('799.00')
    expect(formatPrice(1899.5)).toBe('1899.50')
  })

  it('BigDecimal 字符串输入', () => {
    expect(formatPrice('2397.00')).toBe('2397.00')
    expect(formatPrice('0')).toBe('0.00')
  })

  it('非法输入回退 0.00', () => {
    expect(formatPrice(null)).toBe('0.00')
    expect(formatPrice(undefined)).toBe('0.00')
    expect(formatPrice('abc')).toBe('0.00')
  })
})

describe('formatSize 规格格式化', () => {
  it('去掉多余的 .0', () => {
    expect(formatSize(38)).toBe('38')
    expect(formatSize(38.0)).toBe('38')
    expect(formatSize('38.0')).toBe('38')
  })

  it('保留半码', () => {
    expect(formatSize(38.5)).toBe('38.5')
  })

  it('空值返回空串', () => {
    expect(formatSize(null)).toBe('')
    expect(formatSize(undefined)).toBe('')
    expect(formatSize('')).toBe('')
  })
})

describe('formatTime 时间格式化', () => {
  it('标准时间串 → YYYY-MM-DD HH:mm:ss', () => {
    expect(formatTime('2024-01-01 10:00:00')).toBe('2024-01-01 10:00:00')
  })

  it('非法输入原样返回', () => {
    expect(formatTime('')).toBe('')
    expect(formatTime(null)).toBe('')
    expect(formatTime('abc')).toBe('abc')
  })
})

describe('splitImgs / firstImg 图片串处理', () => {
  it('空格与分号分隔均支持', () => {
    expect(splitImgs('a.jpg b.jpg')).toEqual(['a.jpg', 'b.jpg'])
    expect(splitImgs('a.jpg;b.jpg')).toEqual(['a.jpg', 'b.jpg'])
    expect(splitImgs('a.jpg; b.jpg c.jpg')).toEqual(['a.jpg', 'b.jpg', 'c.jpg'])
  })

  it('空值返回空数组', () => {
    expect(splitImgs(null)).toEqual([])
    expect(splitImgs('')).toEqual([])
  })

  it('firstImg 取第一张图', () => {
    expect(firstImg('a.jpg b.jpg')).toBe('a.jpg')
    expect(firstImg('')).toBeNull()
    expect(firstImg(null)).toBeNull()
  })
})

describe('statusText 订单状态文案', () => {
  it('映射后端枚举', () => {
    expect(statusText('WAIT_BUYER_PAY')).toBe('待付款')
    expect(statusText('TRADE_PAID_SUCCESS')).toBe('待发货')
    expect(statusText('TRADE_CLOSED')).toBe('已关闭')
    expect(statusText('TRADE_PAID_FAILED')).toBe('支付失败')
    expect(statusText('TRADE_SHIPPED')).toBe('待收货')
    expect(statusText('TRADE_COMPLETED')).toBe('已完成')
    expect(statusText('TRADE_REFUNDING')).toBe('退款中')
    expect(statusText('TRADE_REFUNDED')).toBe('已退款')
    expect(statusText('UNKNOWN')).toBe('未知')
  })
})
