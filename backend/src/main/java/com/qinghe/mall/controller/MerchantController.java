package com.qinghe.mall.controller;

import com.qinghe.mall.config.RateLimit;
import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductService;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端（M6 平台化）：商品管理 / 订单处理 / 店铺统计。
 * 所有经营接口先经 {@link #checkMerchant}：登录 + role=MERCHANT + 店铺 ACTIVE。
 */
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    /** 店铺信息与入驻状态（无需 ACTIVE，供前端展示审核中/驳回原因） */
    @GetMapping("/info")
    public Result<MerchantDO> info(HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        MerchantDO merchant = merchantService.getByUserId((Long) userIdObj);
        // 审核通过后，若会话角色仍为 USER（申请开店前是普通用户），自动提升为 MERCHANT，免去重新登录
        if (merchant != null && MerchantDO.STATUS_ACTIVE.equals(merchant.getStatus())
                && !UserDO.ROLE_MERCHANT.equals(request.getSession().getAttribute("role"))) {
            request.getSession().setAttribute("role", UserDO.ROLE_MERCHANT);
        }
        return Result.success(merchant);
    }

    /**
     * 申请开店（已有账号升级为商家，幂等）：创建 PENDING 入驻申请。
     * 面向已注册普通用户；shopName 必填。账号角色提升由平台审核 {@link #audit} 完成，
     * 审核通过后经 {@link #info} 自动刷新会话角色，无需重新登录。
     */
    @RateLimit(rate = 5, message = "操作过于频繁，请稍后再试")
    @PostMapping("/apply")
    public Result<MerchantDO> apply(@RequestParam(value = "shopName", required = false) String shopName,
                                    @RequestParam(value = "shopLogo", required = false) String shopLogo,
                                    @RequestParam(value = "shopDesc", required = false) String shopDesc,
                                    HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        MerchantDO merchant = merchantService.apply((Long) userIdObj, shopName, shopLogo, shopDesc);
        return Result.success(merchant);
    }

    /** 本店商品分页（含关键词/状态过滤） */
    @GetMapping("/products")
    public Result<Paging<Product>> products(@RequestParam(value = "keyword", required = false) String keyword,
                                            @RequestParam(value = "status", required = false) String status,
                                            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                            HttpServletRequest request) {
        MerchantDO merchant = checkMerchant(request);
        return Result.success(productService.queryMerchantPage(merchant.getId(), keyword, status, pageNum, pageSize));
    }

    /** 新增/编辑本店商品（含 SKU 整体替换；新增默认上架 ON） */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/product/save")
    public Result<Product> saveProduct(@RequestBody Product product, HttpServletRequest request) {
        MerchantDO merchant = checkMerchant(request);
        if (StringUtils.isBlank(product.getName())) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("商品价格必须大于 0");
        }
        if (StringUtils.isBlank(product.getId())) {
            // 新增：默认上架
            if (StringUtils.isBlank(product.getStatus())) {
                product.setStatus("ON");
            }
        } else {
            Product existing = productService.findById(product.getId());
            if (existing == null) {
                throw new RuntimeException("商品不存在");
            }
            if (!merchant.getId().equals(existing.getMerchantId())) {
                throw new RuntimeException("无权操作其他店铺的商品");
            }
        }
        product.setMerchantId(merchant.getId());
        // 商品 + SKU 整体替换在同一事务内（saveWithDetails @Transactional），失败整体回滚
        Product saved = productService.saveWithDetails(product, product.getDetails());
        return Result.success(saved);
    }

    /** 上/下架：POST /api/merchant/product/toggle?productId=&status=ON|OFF */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/product/toggle")
    public Result<Void> toggleProduct(@RequestParam("productId") String productId,
                                      @RequestParam("status") String status,
                                      HttpServletRequest request) {
        MerchantDO merchant = checkMerchant(request);
        if (!"ON".equals(status) && !"OFF".equals(status)) {
            throw new RuntimeException("非法的商品状态");
        }
        Product existing = productService.findById(productId);
        if (existing == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!merchant.getId().equals(existing.getMerchantId())) {
            throw new RuntimeException("无权操作其他店铺的商品");
        }
        Product update = new Product();
        update.setId(productId);
        update.setStatus(status);
        productService.update(update);
        return Result.success();
    }

    /** 本店订单分页（status 可空） */
    @GetMapping("/orders")
    public Result<Paging<Order>> orders(@RequestParam(value = "status", required = false) String status,
                                        @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                        HttpServletRequest request) {
        MerchantDO merchant = checkMerchant(request);
        return Result.success(orderService.listByMerchant(merchant.getId(), status, pageNum, pageSize));
    }

    /** 发货：仅本店 PAID_SUCCESS → SHIPPED */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/order/ship")
    public Result<Void> ship(@RequestParam("orderNumber") String orderNumber, HttpServletRequest request) {
        MerchantDO merchant = checkMerchant(request);
        orderService.shipMerchantOrder(merchant.getId(), orderNumber);
        return Result.success();
    }

    /** 处理退款：仅本店 REFUNDING → REFUNDED(approve) / 回退 PAID_SUCCESS */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/order/refund/process")
    public Result<Void> processRefund(@RequestParam("orderNumber") String orderNumber,
                                      @RequestParam(value = "approve", defaultValue = "true") boolean approve,
                                      HttpServletRequest request) {
        MerchantDO merchant = checkMerchant(request);
        orderService.processMerchantRefund(merchant.getId(), orderNumber, approve);
        return Result.success();
    }

    /** 店铺统计：{ productCount, orderCount, todayOrderCount, paidRevenue, todayRevenue } */
    @GetMapping("/stats")
    public Result<java.util.Map<String, Object>> stats(HttpServletRequest request) {
        MerchantDO merchant = checkMerchant(request);
        return Result.success(orderService.merchantStats(merchant.getId()));
    }

    /** 商家权限守卫：登录 + role=MERCHANT + 店铺存在且 ACTIVE */
    private MerchantDO checkMerchant(HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            throw new RuntimeException("未登录");
        }
        // 显式断言角色（P2-6：仅 role=MERCHANT 可进入商家经营接口）
        if (!com.qinghe.mall.dataobject.UserDO.ROLE_MERCHANT.equals(request.getSession().getAttribute("role"))) {
            throw new RuntimeException("无商家权限");
        }
        MerchantDO merchant = merchantService.getByUserId((Long) userIdObj);
        if (merchant == null) {
            throw new RuntimeException("您还不是入驻商家，请先申请开店");
        }
        if (MerchantDO.STATUS_PENDING.equals(merchant.getStatus())) {
            throw new RuntimeException("入驻申请审核中，通过后即可经营");
        }
        if (MerchantDO.STATUS_REJECTED.equals(merchant.getStatus())) {
            throw new RuntimeException("入驻申请未通过：" + (merchant.getRejectReason() == null ? "" : merchant.getRejectReason()));
        }
        if (!MerchantDO.STATUS_ACTIVE.equals(merchant.getStatus())) {
            throw new RuntimeException("店铺已被禁用，请联系平台");
        }
        return merchant;
    }
}
