package com.qinghe.mall.controller;

import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.SeckillService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺主页（A3，v1.5）：顾客视角的店铺聚合页。
 *
 * 聚合店铺信息（名称/Logo/简介）、在售商品分页、店铺评分/销量、
 * 进行中的店铺券与店铺秒杀（复用 #39 的商家营销数据）。
 * 公开接口无需登录；仅 ACTIVE 商家可访问。
 */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private com.qinghe.mall.dao.CommentDAO commentDAO;

    @Autowired
    private com.qinghe.mall.dao.ProductDAO productDAO;

    @GetMapping("/{merchantId}")
    public Result<Map<String, Object>> shopHome(
            @PathVariable("merchantId") Long merchantId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "12") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        if (merchantId == null || merchantId <= 0) {
            throw new BusinessException("店铺不存在");
        }
        MerchantDO merchant = merchantService.getById(merchantId);
        if (merchant == null || !"ACTIVE".equals(merchant.getStatus())) {
            throw new BusinessException("店铺不存在或未营业");
        }
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 || pageSize > 50 ? 12 : pageSize;

        Map<String, Object> shop = new HashMap<>();
        shop.put("id", merchant.getId());
        shop.put("shopName", merchant.getShopName());
        shop.put("shopLogo", merchant.getShopLogo());
        shop.put("shopDesc", merchant.getShopDesc());

        Map<String, Object> stats = new HashMap<>();
        stats.put("productCount", productService.queryMerchantPage(merchantId, null, "ON", 1, 1).getTotalCount());
        Double avgRating = commentDAO.avgRatingByMerchant(merchantId);
        stats.put("avgRating", avgRating == null ? 0 : Math.round(avgRating * 10) / 10.0);
        stats.put("totalSales", productDAO.sumSalesByMerchant(merchantId));

        Map<String, Object> home = new HashMap<>();
        home.put("shop", shop);
        home.put("stats", stats);
        // 在售商品分页（店铺主页只展示 ON 商品）
        Paging<Product> products = productService.queryMerchantPage(merchantId, keyword, "ON", pn, ps);
        home.put("products", products);
        home.put("coupons", couponService.listActiveByMerchant(merchantId));
        home.put("seckills", seckillService.listOngoingByMerchant(merchantId));
        return Result.success(home);
    }
}
