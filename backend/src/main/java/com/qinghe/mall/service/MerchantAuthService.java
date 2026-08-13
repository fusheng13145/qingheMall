package com.qinghe.mall.service;

import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 商家权限守卫（P1-8 / #39）。
 *
 * 校验链：登录 + role=MERCHANT + 店铺存在且 ACTIVE；未通过抛 {@link AuthException}（401/403）。
 * 从 MerchantController 抽取为独立组件，供商家端各控制器（商品/订单/营销）统一复用，避免守卫逻辑漂移。
 */
@Component
public class MerchantAuthService {

    @Autowired
    private MerchantService merchantService;

    /** 商家权限守卫：返回当前商家；未登录/非商家/店铺未激活则抛 AuthException */
    public MerchantDO checkMerchant(HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            throw AuthException.unauthorized("未登录");
        }
        // 显式断言角色（P2-6：仅 role=MERCHANT 可进入商家经营接口）
        if (!UserDO.ROLE_MERCHANT.equals(request.getSession().getAttribute("role"))) {
            throw AuthException.forbidden("无商家权限");
        }
        MerchantDO merchant = merchantService.getByUserId((Long) userIdObj);
        if (merchant == null) {
            throw AuthException.forbidden("您还不是入驻商家，请先申请开店");
        }
        if (MerchantDO.STATUS_PENDING.equals(merchant.getStatus())) {
            throw AuthException.forbidden("入驻申请审核中，通过后即可经营");
        }
        if (MerchantDO.STATUS_REJECTED.equals(merchant.getStatus())) {
            throw AuthException.forbidden("入驻申请未通过：" + (merchant.getRejectReason() == null ? "" : merchant.getRejectReason()));
        }
        if (!MerchantDO.STATUS_ACTIVE.equals(merchant.getStatus())) {
            throw AuthException.forbidden("店铺已被禁用，请联系平台");
        }
        return merchant;
    }
}
