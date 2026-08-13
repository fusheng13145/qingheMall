package com.qinghe.mall.service;

import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.UserCouponDO;
import com.qinghe.mall.model.Paging;
import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券子系统：规则引擎 + 领取/核销/释放 + 管理端 CRUD。
 */
public interface CouponService {

    // ===== 管理端 =====
    CouponDO createCoupon(CouponDO coupon);

    Paging<CouponDO> listCoupons(int pageNum, int pageSize);

    /** 领券中心：上架且在有效期内的券列表 */
    List<CouponDO> listActive();

    CouponDO updateCoupon(CouponDO coupon);

    void toggle(String couponId, String status);

    // ===== 用户端 =====
    /** 领取券：校验上架/时间窗/总量，原子自增 issued + 写入持有记录（uk 防重复领） */
    void claim(String couponId, Long userId);

    /** 我的券：可按状态过滤（UNUSED/USED/EXPIRED/RELEASED），空串查全部 */
    List<UserCouponDO> myCoupons(Long userId, String status);

    /** 结算可用券：未用 + 上架 + 时间窗内 + 门槛 ≤ amount，按优惠额降序 */
    List<UserCouponDO> available(Long userId, BigDecimal amount);

    // ===== 规则引擎与核销 =====
    /** 计算优惠额：不满门槛返回 0；满减 min(amount, total)；折扣 min(total*(1-rate), maxDiscount) */
    BigDecimal calculateDiscount(UserCouponDO userCoupon, BigDecimal orderTotal);

    /**
     * 后端权威校验并计算优惠额（不锁定）：校验归属/未用/上架/时间窗/门槛，返回优惠额。
     * 用于下单前校验前端传入的优惠额未被伪造；锁定由 lockCoupon 在订单事务内完成。
     */
    BigDecimal validateAndComputeDiscount(String userCouponId, Long userId, BigDecimal orderTotal);

    /** 核销锁定：CAS 将 UNUSED→USED 并绑定订单号（事务内调用） */
    void lockCoupon(String userCouponId, Long userId, String orderNumber);

    /** 释放：USED→UNUSED 并清空订单绑定（取消订单/退款拒绝时调用） */
    void releaseCoupon(String userCouponId);

    // ===== 商家端（#39 商家自建） =====
    /** 商家自建券：强制 merchantId 归属，复用平台券校验规则 */
    CouponDO createMerchantCoupon(Long merchantId, CouponDO coupon);

    /** 本店券分页（仅返回该商家） */
    Paging<CouponDO> listMerchantCoupons(Long merchantId, int pageNum, int pageSize);

    /** 商家编辑本店券（须归属校验；上架中不可改策略同平台券） */
    CouponDO updateMerchantCoupon(Long merchantId, CouponDO coupon);

    /** 商家上下架本店券（须归属校验） */
    void toggleMerchantCoupon(Long merchantId, String couponId, String status);

    /** 店铺可用券（上架+有效期+该商家），供顾客端店铺页展示 */
    List<CouponDO> listActiveByMerchant(Long merchantId);
}
