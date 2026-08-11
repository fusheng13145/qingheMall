/**
 * 优惠券折扣计算（前端展示 + 提交用）。
 * 必须与后端 CouponServiceImpl.calculateDiscount 保持同一公式；
 * 后端在下单时会用 BigDecimal 重新计算并校验，二者不一致将被拒绝。
 *
 * @param {Object} c 券（含 couponType/threshold/couponAmount/couponDiscount/couponMaxDiscount）
 * @param {number} orderTotal 整单原价
 * @returns {number} 优惠额（保留 2 位小数）
 */
export function calcCouponDiscount(c, orderTotal) {
  if (!c) return 0
  const total = Number(orderTotal || 0)
  const threshold = Number(c.threshold != null ? c.threshold : c.couponThreshold || 0)
  if (total < threshold) return 0
  // 类型字段兼容两种形态：UserCouponDO 为 couponType，CouponDO 为 type
  const type = c.couponType || c.type
  let discount = 0
  if (type === 'FULL_REDUCTION') {
    discount = Math.min(Number(c.couponAmount != null ? c.couponAmount : c.amount || 0), total)
  } else if (type === 'DISCOUNT') {
    const rate = Number(c.couponDiscount != null ? c.couponDiscount : c.discount || 1)
    let raw = total * (1 - rate)
    const max = c.couponMaxDiscount != null ? c.couponMaxDiscount : c.maxDiscount
    if (max != null) raw = Math.min(raw, Number(max))
    discount = Math.min(raw, total)
  }
  // 折扣不允许为负（rate 异常 >1 或数据错误时置 0，避免前端展示负优惠）
  if (discount < 0) return 0
  // 规避浮点误差（如 0.15*300=45.0000000001），统一保留 2 位
  return Math.round(discount * 100) / 100
}

/** 券类型中文 */
export function couponTypeText(type) {
  return type === 'FULL_REDUCTION' ? '满减券' : type === 'DISCOUNT' ? '折扣券' : type
}

/** 券规则描述（展示用） */
export function couponRuleText(c) {
  // 类型字段兼容两种形态：UserCouponDO 为 couponType，CouponDO 为 type
  const type = (c && (c.couponType || c.type)) || ''
  if (type === 'FULL_REDUCTION') {
    const amt = Number(c.couponAmount != null ? c.couponAmount : c.amount || 0)
    const th = Number(c.threshold != null ? c.threshold : c.couponThreshold || 0)
    return th > 0 ? `满${th}减${amt}` : `无门槛减${amt}`
  }
  if (type === 'DISCOUNT') {
    const rate = Number(c.couponDiscount != null ? c.couponDiscount : c.discount || 1)
    const percent = Math.round(rate * 100)
    const max = c.couponMaxDiscount != null ? c.couponMaxDiscount : c.maxDiscount
    return max != null ? `${percent}折（封顶${max}元）` : `${percent}折`
  }
  return ''
}
