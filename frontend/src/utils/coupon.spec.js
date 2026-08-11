import { describe, it, expect } from 'vitest'
import { calcCouponDiscount, couponTypeText, couponRuleText } from './coupon'

describe('calcCouponDiscount 满减券', () => {
  const fullReduction = { couponType: 'FULL_REDUCTION', threshold: 100, couponAmount: 20 }

  it('达到门槛按券面额优惠', () => {
    expect(calcCouponDiscount(fullReduction, 300)).toBe(20)
  })

  it('未达门槛优惠 0', () => {
    expect(calcCouponDiscount(fullReduction, 99)).toBe(0)
  })

  it('门槛为 0 视为无门槛', () => {
    expect(calcCouponDiscount({ couponType: 'FULL_REDUCTION', threshold: 0, couponAmount: 5 }, 3)).toBe(3)
    expect(calcCouponDiscount({ couponType: 'FULL_REDUCTION', couponAmount: 5 }, 3)).toBe(3)
  })

  it('优惠不超整单金额', () => {
    expect(calcCouponDiscount({ couponType: 'FULL_REDUCTION', threshold: 0, couponAmount: 100 }, 30)).toBe(30)
  })

  it('支持兼容字段 amount/couponThreshold', () => {
    expect(calcCouponDiscount({ couponType: 'FULL_REDUCTION', couponThreshold: 200, amount: 50 }, 250)).toBe(50)
  })
})

describe('calcCouponDiscount 折扣券', () => {
  const discount = { couponType: 'DISCOUNT', threshold: 0, couponDiscount: 0.85 }

  it('按折扣率计算优惠额', () => {
    expect(calcCouponDiscount(discount, 200)).toBe(30)
  })

  it('有封顶时取封顶值', () => {
    const capped = { couponType: 'DISCOUNT', threshold: 0, couponDiscount: 0.5, couponMaxDiscount: 10 }
    expect(calcCouponDiscount(capped, 100)).toBe(10)
    expect(calcCouponDiscount(capped, 10)).toBe(5)
  })

  it('兼容 maxDiscount 字段', () => {
    expect(calcCouponDiscount({ couponType: 'DISCOUNT', discount: 0.9, maxDiscount: 5 }, 100)).toBe(5)
  })

  it('浮点误差规避：0.85 折 300 元 → 45.0000000001 → 45', () => {
    expect(calcCouponDiscount({ couponType: 'DISCOUNT', couponDiscount: 0.85 }, 300)).toBe(45)
  })

  it('折扣不低于 0（rate 异常输入防护）', () => {
    expect(calcCouponDiscount({ couponType: 'DISCOUNT', couponDiscount: 1.5 }, 100)).toBe(0)
  })
})

describe('calcCouponDiscount 边界', () => {
  it('orderTotal 缺省按 0 处理', () => {
    expect(calcCouponDiscount({ couponType: 'FULL_REDUCTION', threshold: 0, couponAmount: 10 })).toBe(0)
  })

  it('未知券类型优惠 0', () => {
    expect(calcCouponDiscount({ couponType: 'XXX', threshold: 0 }, 100)).toBe(0)
  })

  it('null 券对象不抛异常', () => {
    expect(calcCouponDiscount(null, 100)).toBe(0)
  })
})

describe('CouponDO 形态兼容（领券中心/管理端，type/amount/discount 字段）', () => {
  it('满减券按 type 字段识别并计算', () => {
    expect(calcCouponDiscount({ type: 'FULL_REDUCTION', threshold: 100, amount: 20 }, 300)).toBe(20)
  })

  it('折扣券按 type 字段识别并计算', () => {
    expect(calcCouponDiscount({ type: 'DISCOUNT', threshold: 0, discount: 0.85 }, 200)).toBe(30)
  })

  it('couponRuleText 渲染满减/折扣规则', () => {
    expect(couponRuleText({ type: 'FULL_REDUCTION', threshold: 100, amount: 20 })).toBe('满100减20')
    expect(couponRuleText({ type: 'DISCOUNT', discount: 0.85, maxDiscount: 50 })).toBe('85折（封顶50元）')
  })

  it('couponType 优先于 type（UserCouponDO 同时携带时不被覆盖', () => {
    expect(calcCouponDiscount(
      { couponType: 'FULL_REDUCTION', type: 'DISCOUNT', threshold: 0, couponAmount: 10 }, 100)).toBe(10)
  })
})

describe('couponTypeText 类型文案', () => {
  it('映射满减与折扣', () => {
    expect(couponTypeText('FULL_REDUCTION')).toBe('满减券')
    expect(couponTypeText('DISCOUNT')).toBe('折扣券')
    expect(couponTypeText('OTHER')).toBe('OTHER')
  })
})

describe('couponRuleText 规则描述', () => {
  it('满减券：有门槛/无门槛', () => {
    expect(couponRuleText({ couponType: 'FULL_REDUCTION', threshold: 100, couponAmount: 20 })).toBe('满100减20')
    expect(couponRuleText({ couponType: 'FULL_REDUCTION', threshold: 0, couponAmount: 5 })).toBe('无门槛减5')
  })

  it('折扣券：含封顶', () => {
    expect(couponRuleText({ couponType: 'DISCOUNT', couponDiscount: 0.85 })).toBe('85折')
    expect(couponRuleText({ couponType: 'DISCOUNT', couponDiscount: 0.85, couponMaxDiscount: 50 })).toBe('85折（封顶50元）')
  })

  it('未知类型返回空串', () => {
    expect(couponRuleText({ couponType: 'XXX' })).toBe('')
  })
})
