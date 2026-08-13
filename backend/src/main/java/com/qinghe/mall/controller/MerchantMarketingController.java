package com.qinghe.mall.controller;

import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.MerchantAuthService;
import com.qinghe.mall.service.SeckillService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端营销管理（#39）：商家自建券 / 秒杀。
 *
 * 所有接口经 {@link MerchantAuthService#checkMerchant} 六重守卫；
 * 写操作（创建/编辑/上下架）在 Service 层强制 merchant_id 归属校验，杜绝跨店水平越权。
 */
@RestController
@RequestMapping("/api/merchant/marketing")
public class MerchantMarketingController {

    @Autowired
    private MerchantAuthService merchantAuthService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private SeckillService seckillService;

    // ===== 商家券 =====

    /** 商家自建券：强制归属当前商家 */
    @PostMapping("/coupon/create")
    public Result<CouponDO> createCoupon(@RequestBody CouponDO coupon, HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        return Result.success(couponService.createMerchantCoupon(merchant.getId(), coupon));
    }

    /** 本店券分页（仅返回该商家全部状态） */
    @GetMapping("/coupon/list")
    public Result<Paging<CouponDO>> listCoupons(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        return Result.success(couponService.listMerchantCoupons(merchant.getId(), pageNum, pageSize));
    }

    /** 商家编辑本店券（须归属校验） */
    @PostMapping("/coupon/update")
    public Result<CouponDO> updateCoupon(@RequestBody CouponDO coupon, HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        return Result.success(couponService.updateMerchantCoupon(merchant.getId(), coupon));
    }

    /** 商家上下架本店券（须归属校验） */
    @PostMapping("/coupon/toggle")
    public Result<Void> toggleCoupon(@RequestParam("couponId") String couponId,
                                     @RequestParam("status") String status,
                                     HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        couponService.toggleMerchantCoupon(merchant.getId(), couponId, status);
        return Result.success();
    }

    // ===== 商家秒杀 =====

    /** 商家自建秒杀：强制归属当前商家，且 SKU 须属本店 */
    @PostMapping("/seckill/create")
    public Result<SeckillActivityDO> createSeckill(@RequestBody SeckillActivityDO activity, HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        return Result.success(seckillService.createMerchantActivity(merchant.getId(), activity));
    }

    /** 本店活动分页（status 为空查全部） */
    @GetMapping("/seckill/list")
    public Result<Paging<SeckillActivityDO>> listSeckills(@RequestParam(value = "status", required = false) String status,
                                                          @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                          HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        return Result.success(seckillService.listMerchantActivities(merchant.getId(), status, pageNum, pageSize));
    }

    /** 商家上下架本店活动（须归属校验） */
    @PostMapping("/seckill/toggle")
    public Result<Void> toggleSeckill(@RequestParam("activityId") String activityId,
                                      @RequestParam("status") String status,
                                      HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        seckillService.toggleMerchantActivity(merchant.getId(), activityId, status);
        return Result.success();
    }
}
