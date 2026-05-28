import dayjs from 'dayjs'

export const formatDate = (date: string | Date, format: string = 'YYYY-MM-DD'): string => {
  return dayjs(date).format(format)
}

export const formatDateTime = (date: string | Date, format: string = 'YYYY-MM-DD HH:mm:ss'): string => {
  return dayjs(date).format(format)
}

export const formatPrice = (price: number): string => {
  return `¥${price.toFixed(2)}`
}

export const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return `${(num / 10000).toFixed(1)}万`
  }
  return num.toString()
}

export const formatPhone = (phone: string): string => {
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

export const formatAddress = (address: { province: string; city: string; district: string; detail: string }): string => {
  return `${address.province}${address.city}${address.district}${address.detail}`
}

export const getRelativeTime = (date: string | Date): string => {
  const now = dayjs()
  const target = dayjs(date)
  const diffMinutes = now.diff(target, 'minute')
  const diffHours = now.diff(target, 'hour')
  const diffDays = now.diff(target, 'day')

  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 7) return `${diffDays}天前`
  return target.format('YYYY-MM-DD')
}
