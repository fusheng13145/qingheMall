package com.qinghe.mall.controller;

import com.qinghe.mall.config.RateLimit;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.UserCouponDO;
import com.qinghe.mall.service.CouponService;
import java.math.BigDecimal;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    /** 领取券（限流 5/s 防批量领券） */
    @RateLimit(rate = 5, message = "领取过于频繁，请稍后再试")
    @PostMapping("/claim")
    public Result<Void> claim(@RequestParam("couponId") String couponId, HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        couponService.claim(couponId, (Long) userIdObj);
        return Result.success();
    }

    /** 领券中心：上架且在有效期内的券 */
    @GetMapping("/list")
    public Result<List<CouponDO>> list(HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(couponService.listActive());
    }

    /** 我的券：status 可选 UNUSED/USED/EXPIRED/RELEASED，空串查全部 */
    @GetMapping("/mine")
    public Result<List<UserCouponDO>> mine(
            @RequestParam(value = "status", required = false, defaultValue = "") String status,
            HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        List<UserCouponDO> list = couponService.myCoupons((Long) userIdObj, status);
        return Result.success(list);
    }

    /** 结算可用券：amount 为整单原价，返回可按优惠额降序使用的券 */
    @GetMapping("/available")
    public Result<List<UserCouponDO>> available(
            @RequestParam(value = "amount", required = false) BigDecimal amount,
            HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        List<UserCouponDO> list = couponService.available((Long) userIdObj, amount);
        return Result.success(list);
    }
}
