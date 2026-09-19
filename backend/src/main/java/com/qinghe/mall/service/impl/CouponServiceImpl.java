package com.qinghe.mall.service.impl;

import com.qinghe.mall.exception.BusinessException;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghe.mall.dao.CouponDAO;
import com.qinghe.mall.dao.UserCouponDAO;
import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.UserCouponDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.util.UUIDUtils;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponServiceImpl implements CouponService {

    @Autowired
    private CouponDAO couponDAO;

    @Autowired
    private UserCouponDAO userCouponDAO;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // ===================== 管理端 =====================

    /** 平台/商家券通用校验（#39：提取复用，确保两类券规则一致） */
    private void validateCoupon(CouponDO coupon) {
        if (coupon == null || StringUtils.isBlank(coupon.getName())) {
            throw new BusinessException("券名称不能为空");
        }
        if (!"FULL_REDUCTION".equals(coupon.getType()) && !"DISCOUNT".equals(coupon.getType())) {
            throw new BusinessException("券类型不合法（应为 FULL_REDUCTION 或 DISCOUNT）");
        }
        if (coupon.getStartTime() == null || coupon.getEndTime() == null) {
            throw new BusinessException("有效期起止时间不能为空");
        }
        if (coupon.getEndTime().before(coupon.getStartTime())) {
            throw new BusinessException("结束时间须晚于开始时间");
        }
        if (coupon.getTotal() == null || coupon.getTotal() < 0) {
            coupon.setTotal(0);
        }
        if (coupon.getIssued() == null) {
            coupon.setIssued(0);
        }
        if (coupon.getPerLimit() == null || coupon.getPerLimit() < 1) {
            coupon.setPerLimit(1);
        }
        if (StringUtils.isBlank(coupon.getStatus())) {
            coupon.setStatus("ACTIVE");
        }
    }

    @Override
    public CouponDO createCoupon(CouponDO coupon) {
        validateCoupon(coupon);
        coupon.setId(UUIDUtils.uuid());
        coupon.setGmtCreated(new Date());
        coupon.setGmtModified(new Date());
        couponDAO.insert(coupon);
        return coupon;
    }

    @Override
    public List<CouponDO> listActive() {
        return couponDAO.findActive();
    }

    @Override
    public Paging<CouponDO> listCoupons(int pageNum, int pageSize) {        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        Page<CouponDO> page = PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> couponDAO.findPage());
        Paging<CouponDO> paging = new Paging<>();
        paging.setPageNum(pageNum);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());
        paging.setData(page.getResult());
        return paging;
    }

    @Override
    public CouponDO updateCoupon(CouponDO coupon) {
        CouponDO existing = couponDAO.findById(coupon.getId());
        if (existing == null) {
            throw new BusinessException("券不存在");
        }
        if ("ACTIVE".equals(existing.getStatus())) {
            throw new BusinessException("上架中的券不可修改，请先下架");
        }
        couponDAO.update(coupon);
        return couponDAO.findById(coupon.getId());
    }

    @Override
    public void toggle(String couponId, String status) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new BusinessException("状态不合法（应为 ACTIVE 或 INACTIVE）");
        }
        couponDAO.updateStatus(couponId, status);
    }

    // ===================== 商家端（#39） =====================

    @Override
    public CouponDO createMerchantCoupon(Long merchantId, CouponDO coupon) {
        if (merchantId == null) {
            throw new BusinessException("商家身份缺失");
        }
        validateCoupon(coupon);
        coupon.setId(UUIDUtils.uuid());
        coupon.setMerchantId(merchantId);
        coupon.setGmtCreated(new Date());
        coupon.setGmtModified(new Date());
        couponDAO.insert(coupon);
        return coupon;
    }

    @Override
    public Paging<CouponDO> listMerchantCoupons(Long merchantId, int pageNum, int pageSize) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        Page<CouponDO> page = PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> couponDAO.findByMerchant(merchantId));
        Paging<CouponDO> paging = new Paging<>();
        paging.setPageNum(pageNum);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());
        paging.setData(page.getResult());
        return paging;
    }

    @Override
    public CouponDO updateMerchantCoupon(Long merchantId, CouponDO coupon) {
        CouponDO existing = couponDAO.findById(coupon.getId());
        if (existing == null) {
            throw new BusinessException("券不存在");
        }
        if (!merchantId.equals(existing.getMerchantId())) {
            throw new BusinessException("无权操作其他店铺的营销活动");
        }
        if ("ACTIVE".equals(existing.getStatus())) {
            throw new BusinessException("上架中的券不可修改，请先下架");
        }
        couponDAO.update(coupon);
        return couponDAO.findById(coupon.getId());
    }

    @Override
    public void toggleMerchantCoupon(Long merchantId, String couponId, String status) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new BusinessException("状态不合法（应为 ACTIVE 或 INACTIVE）");
        }
        CouponDO existing = couponDAO.findById(couponId);
        if (existing == null) {
            throw new BusinessException("券不存在");
        }
        if (!merchantId.equals(existing.getMerchantId())) {
            throw new BusinessException("无权操作其他店铺的营销活动");
        }
        couponDAO.updateStatus(couponId, status);
    }

    @Override
    public List<CouponDO> listActiveByMerchant(Long merchantId) {
        return couponDAO.findActiveByMerchant(merchantId);
    }

    // ===================== 用户端 =====================

    @Override
    @Transactional
    public void claim(String couponId, Long userId) {
        CouponDO coupon = couponDAO.findById(couponId);
        if (coupon == null) {
            throw new BusinessException("券不存在");
        }
        if (!"ACTIVE".equals(coupon.getStatus())) {
            throw new BusinessException("券已下架");
        }
        Date now = new Date();
        if (now.before(coupon.getStartTime()) || now.after(coupon.getEndTime())) {
            throw new BusinessException("不在领取时间内");
        }
        if (coupon.getTotal() != null && coupon.getIssued() != null && coupon.getIssued() >= coupon.getTotal()) {
            throw new BusinessException("券已领完");
        }
        // P2：每人限领数量（perLimit > 1 时按已领数拦截；uk_user_coupon 唯一约束仍是 1 张底线）
        if (coupon.getPerLimit() != null && coupon.getPerLimit() > 1) {
            int claimed = userCouponDAO.countByUserAndCoupon(userId, couponId);
            if (claimed >= coupon.getPerLimit()) {
                throw new BusinessException("已达限领数量");
            }
        }
        // 原子自增 issued（DB 层 WHERE issued < total 保证不超发）
        int inc = couponDAO.incrementIssued(couponId);
        if (inc <= 0) {
            throw new BusinessException("券已领完");
        }
        UserCouponDO uc = new UserCouponDO();
        uc.setId(UUIDUtils.uuid());
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus("UNUSED");
        uc.setGmtCreated(new Date());
        uc.setGmtModified(new Date());
        try {
            userCouponDAO.insert(uc);
        } catch (DuplicateKeyException e) {
            // P2：仅捕获唯一约束冲突；DB 故障等真实异常重新抛出，避免误报业务失败
            throw new BusinessException("您已领取过该券");
        }
    }

    @Override
    public List<UserCouponDO> myCoupons(Long userId, String status) {
        return userCouponDAO.findByUserIdAndStatus(userId, status);
    }

    @Override
    public List<UserCouponDO> available(Long userId, BigDecimal amount) {
        final BigDecimal amt = amount == null ? ZERO : amount;
        List<UserCouponDO> list = userCouponDAO.findUsable(userId, amt);
        // 按优惠额降序，前端默认推荐最优券
        list.sort((a, b) -> calculateDiscount(b, amt).compareTo(calculateDiscount(a, amt)));
        return list;
    }

    // ===================== 规则引擎与核销 =====================

    @Override
    public BigDecimal calculateDiscount(UserCouponDO uc, BigDecimal orderTotal) {
        if (uc == null || orderTotal == null) {
            return ZERO;
        }
        BigDecimal threshold = uc.getThreshold() == null ? ZERO : uc.getThreshold();
        if (orderTotal.compareTo(threshold) < 0) {
            return ZERO; // 不满门槛
        }
        if ("FULL_REDUCTION".equals(uc.getCouponType())) {
            BigDecimal amt = uc.getCouponAmount() == null ? ZERO : uc.getCouponAmount();
            return amt.min(orderTotal);
        } else if ("DISCOUNT".equals(uc.getCouponType())) {
            BigDecimal rate = uc.getCouponDiscount() == null ? BigDecimal.ONE : uc.getCouponDiscount();
            BigDecimal raw = orderTotal.multiply(BigDecimal.ONE.subtract(rate));
            if (uc.getCouponMaxDiscount() != null) {
                return raw.min(uc.getCouponMaxDiscount()).min(orderTotal);
            }
            return raw.min(orderTotal);
        }
        return ZERO;
    }

    @Override
    @Transactional
    public void lockCoupon(String userCouponId, Long userId, String orderNumber) {
        int updated = userCouponDAO.lock(userCouponId, userId, orderNumber);
        if (updated <= 0) {
            throw new BusinessException("优惠券不可用或已被使用");
        }
    }

    @Override
    public BigDecimal validateAndComputeDiscount(String userCouponId, Long userId, BigDecimal orderTotal,
                                                 Long productMerchantId) {
        UserCouponDO uc = userCouponDAO.findById(userCouponId);
        if (uc == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (!userId.equals(uc.getUserId())) {
            throw new BusinessException("优惠券不属于当前用户");
        }
        if (!"UNUSED".equals(uc.getStatus())) {
            throw new BusinessException("优惠券已使用");
        }
        CouponDO coupon = couponDAO.findById(uc.getCouponId());
        if (coupon == null) {
            throw new BusinessException("券模板不存在");
        }
        if (!"ACTIVE".equals(coupon.getStatus())) {
            throw new BusinessException("券已下架");
        }
        Date now = new Date();
        if (now.before(coupon.getStartTime()) || now.after(coupon.getEndTime())) {
            throw new BusinessException("券不在有效期");
        }
        // A1：店铺券归属一致性——商家券仅可核销于本店商品（跨店/平台自营商品一律拒绝），
        // merchant_id 为 NULL 的平台券全站可用
        if (coupon.getMerchantId() != null && !coupon.getMerchantId().equals(productMerchantId)) {
            throw new BusinessException("店铺券仅可用于本店商品");
        }
        return calculateDiscount(uc, orderTotal);
    }

    @Override
    @Transactional
    public void releaseCoupon(String userCouponId) {
        userCouponDAO.release(userCouponId);
    }
}
