package com.qinghe.mall.controller;

import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.config.RateLimit;
import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.exception.AuthException;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.MerchantAuthService;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
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
 * 所有经营接口先经 {@link MerchantAuthService#checkMerchant}：登录 + role=MERCHANT + 店铺 ACTIVE。
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

    @Autowired
    private com.qinghe.mall.service.LogisticsService logisticsService;

    @Autowired
    private com.qinghe.mall.service.RefundService refundService;

    @Autowired
    private MerchantAuthService merchantAuthService;

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
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        return Result.success(productService.queryMerchantPage(merchant.getId(), keyword, status, pageNum, pageSize));
    }

    /** 新增/编辑本店商品（含 SKU 整体替换；新增默认上架 ON） */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/product/save")
    public Result<Product> saveProduct(@RequestBody Product product, HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        if (StringUtils.isBlank(product.getName())) {
            throw new BusinessException("商品名称不能为空");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("商品价格必须大于 0");
        }
        if (StringUtils.isBlank(product.getId())) {
            // 新增：默认上架
            if (StringUtils.isBlank(product.getStatus())) {
                product.setStatus("ON");
            }
        } else {
            Product existing = productService.findById(product.getId());
            if (existing == null) {
                throw new BusinessException("商品不存在");
            }
            if (!merchant.getId().equals(existing.getMerchantId())) {
                throw new BusinessException("无权操作其他店铺的商品");
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
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        if (!"ON".equals(status) && !"OFF".equals(status)) {
            throw new BusinessException("非法的商品状态");
        }
        Product existing = productService.findById(productId);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }
        if (!merchant.getId().equals(existing.getMerchantId())) {
            throw new BusinessException("无权操作其他店铺的商品");
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
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        return Result.success(orderService.listByMerchant(merchant.getId(), status, pageNum, pageSize));
    }

    /**
     * 发货（P2-18 升级）：携带 company + trackingNumber 时建立物流档案（同事务原子），
     * 两者缺省时保留旧的纯状态发货（向后兼容）。仅本店订单。
     */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/order/ship")
    public Result<Void> ship(@RequestParam("orderNumber") String orderNumber,
                             @RequestParam(value = "company", required = false) String company,
                             @RequestParam(value = "trackingNumber", required = false) String trackingNumber,
                             HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        if (StringUtils.isNotBlank(company) && StringUtils.isNotBlank(trackingNumber)) {
            assertOrderOwnership(merchant.getId(), orderNumber);
            logisticsService.ship(orderNumber, company, trackingNumber);
        } else {
            orderService.shipMerchantOrder(merchant.getId(), orderNumber);
        }
        return Result.success();
    }

    /**
     * 推进物流状态（P2-18）：SHIPPED→IN_TRANSIT→DELIVERING→SIGNED，每次追加一条轨迹。
     * 未对接承运商接口，由商家手动更新模拟轨迹推进。仅本店订单。
     */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/logistics/advance")
    public Result<com.qinghe.mall.model.Logistics> advanceLogistics(
            @RequestParam("orderNumber") String orderNumber, HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        assertOrderOwnership(merchant.getId(), orderNumber);
        return Result.success(logisticsService.advance(orderNumber));
    }

    /**
     * 处理退款（P2-18 升级）：经 RefundService 审核申请单——
     * 通过：订单置已退款 + 库存回补 + 释放优惠券；驳回：订单回退申请前状态。
     * comment 为审核意见（驳回时向用户展示）。仅本店订单。
     */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/order/refund/process")
    public Result<Void> processRefund(@RequestParam("orderNumber") String orderNumber,
                                      @RequestParam(value = "approve", defaultValue = "true") boolean approve,
                                      @RequestParam(value = "comment", required = false) String comment,
                                      HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        assertOrderOwnership(merchant.getId(), orderNumber);
        refundService.review(orderNumber, approve, comment);
        return Result.success();
    }

    /** 店铺统计：{ productCount, orderCount, todayOrderCount, paidRevenue, todayRevenue } */
    @GetMapping("/stats")
    public Result<java.util.Map<String, Object>> stats(HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        return Result.success(orderService.merchantStats(merchant.getId()));
    }

    /** 商家订单归属校验（P2-18）：订单必须存在且属于本店，否则拒绝操作 */
    private void assertOrderOwnership(Long merchantId, String orderNumber) {
        Order order = orderService.findByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!merchantId.equals(order.getMerchantId())) {
            throw new BusinessException("无权操作其他店铺的订单");
        }
    }

}
