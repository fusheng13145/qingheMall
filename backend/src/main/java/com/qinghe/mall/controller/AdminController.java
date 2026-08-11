package com.qinghe.mall.controller;

import com.qinghe.mall.model.*;
import com.qinghe.mall.service.*;
import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.util.PageParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private ProductService productService;
    @Autowired private OrderService orderService;
    @Autowired private UserService userService;
    @Autowired private CouponService couponService;
    @Autowired private com.qinghe.mall.service.SeckillService seckillService;
    @Autowired private com.qinghe.mall.service.MerchantService merchantService;
    @Autowired private com.qinghe.mall.service.LogisticsService logisticsService;
    @Autowired private com.qinghe.mall.service.RefundService refundService;

    // 检查管理员权限的私有方法
    private boolean checkAdmin(HttpServletRequest request) {
        Object role = request.getSession().getAttribute("role");
        return "ADMIN".equals(role);
    }

    // ========== Dashboard 统计 ==========
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        // 全部走 SQL 聚合，避免全表捞取与内存计算（M3-8）
        Map<String, Object> stats = new HashMap<>();
        stats.put("productCount", productService.queryPage(1, 1, null, null, null).getTotalCount());
        stats.put("orderCount", orderService.countAll());
        stats.put("userCount", userService.countAll());
        // P1-7：统一已付营收口径（已付款/已发货/已完成），与销售日报、商家统计一致
        stats.put("totalRevenue", orderService.sumTotalPriceByStatuses(OrderStatus.paidRevenueStatuses()));
        return Result.success(stats);
    }

    /**
     * 销售日报（P3 报表）：GET /api/admin/report?days=7
     * 返回近 N 天每天已支付订单的销售额与订单数（SQL 按天聚合）。
     */
    @GetMapping("/report")
    public Result<List<Map<String, Object>>> salesReport(@RequestParam(value = "days", defaultValue = "7") Integer days,
                                                         HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        return Result.success(orderService.dailySalesReport(days));
    }

    // ========== 商品管理 ==========
    @PostMapping("/product/add")
    public Result<Product> addProduct(@RequestBody Product product, HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        // 业务异常由 GlobalExceptionHandler 统一转为 Result.fail
        Product result = productService.add(product);
        return Result.success(result);
    }

    @PostMapping("/product/update")
    public Result<Product> updateProduct(@RequestBody Product product, HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        Product result = productService.update(product);
        return Result.success(result);
    }

    @PostMapping("/product/delete")
    public Result<Void> deleteProduct(@RequestParam("id") String id, HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        productService.delete(id);
        return Result.success();
    }

    /** 商品管理列表（全量含下架，分页；顾客端在售列表走 /api/product/page） */
    @GetMapping("/product/list")
    public Result<com.qinghe.mall.model.Paging<com.qinghe.mall.model.Product>> listProducts(
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pagination", required = false) Integer legacyPagination,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        // P2-12：页码参数统一 pageNum，pagination 仅作兼容别名
        int page = PageParams.resolve(pageNum, legacyPagination);
        return Result.success(productService.queryPage(page, pageSize, keyword, null, null));
    }

    // ========== 订单管理 ==========
    @GetMapping("/order/list")
    public Result<Paging<Order>> listAllOrders(
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pagination", required = false) Integer legacyPagination,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        // P1-11：管理端订单分页（原全表捞取，数据量大时超时/OOM）
        // P2-12：页码参数统一 pageNum，pagination 仅作兼容别名
        int page = PageParams.resolve(pageNum, legacyPagination);
        return Result.success(orderService.findAdminPage(page, pageSize, status));
    }

    @PostMapping("/order/updateStatus")
    public Result<Void> updateOrderStatus(@RequestParam("orderNumber") String orderNumber,
                                          @RequestParam("status") String status,
                                          HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        orderService.updateOrderStatus(orderNumber, status);
        return Result.success();
    }

    /**
     * 发货（管理员操作，P2-18 升级）：携带 company + trackingNumber 时建立物流档案，
     * 缺省时保留旧的纯状态发货（向后兼容）。
     */
    @PostMapping("/order/ship")
    public Result<Void> shipOrder(@RequestParam("orderNumber") String orderNumber,
                                  @RequestParam(value = "company", required = false) String company,
                                  @RequestParam(value = "trackingNumber", required = false) String trackingNumber,
                                  HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(company)
                && org.apache.commons.lang3.StringUtils.isNotBlank(trackingNumber)) {
            logisticsService.ship(orderNumber, company, trackingNumber);
        } else {
            orderService.shipOrder(orderNumber);
        }
        return Result.success();
    }

    /** 推进物流状态（管理员操作，P2-18）：SHIPPED→IN_TRANSIT→DELIVERING→SIGNED */
    @PostMapping("/logistics/advance")
    public Result<Logistics> advanceLogistics(@RequestParam("orderNumber") String orderNumber,
                                              HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        return Result.success(logisticsService.advance(orderNumber));
    }

    /**
     * 处理退款（管理员操作，P2-18 升级）：经 RefundService 审核——
     * 通过：订单置已退款 + 库存回补 + 释放优惠券；驳回：订单回退申请前状态。
     */
    @PostMapping("/order/refund/process")
    public Result<Void> processRefund(@RequestParam("orderNumber") String orderNumber,
                                      @RequestParam(value = "approve", defaultValue = "true") boolean approve,
                                      @RequestParam(value = "comment", required = false) String comment,
                                      HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        refundService.review(orderNumber, approve, comment);
        return Result.success();
    }

    // ========== 优惠券管理 ==========
    @PostMapping("/coupon/create")
    public Result<CouponDO> createCoupon(@RequestBody CouponDO coupon, HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        return Result.success(couponService.createCoupon(coupon));
    }

    @GetMapping("/coupon/list")
    public Result<com.qinghe.mall.model.Paging<CouponDO>> listCoupons(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        return Result.success(couponService.listCoupons(pageNum, pageSize));
    }

    @PostMapping("/coupon/update")
    public Result<CouponDO> updateCoupon(@RequestBody CouponDO coupon, HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        return Result.success(couponService.updateCoupon(coupon));
    }

    @PostMapping("/coupon/toggle")
    public Result<Void> toggleCoupon(@RequestParam("couponId") String couponId,
                                     @RequestParam("status") String status,
                                     HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        couponService.toggle(couponId, status);
        return Result.success();
    }

    // ========== 秒杀活动管理 ==========
    @PostMapping("/seckill/create")
    public Result<com.qinghe.mall.dataobject.SeckillActivityDO> createSeckill(
            @RequestBody com.qinghe.mall.dataobject.SeckillActivityDO activity, HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        return Result.success(seckillService.createActivity(activity));
    }

    @GetMapping("/seckill/list")
    public Result<com.qinghe.mall.model.Paging<com.qinghe.mall.dataobject.SeckillActivityDO>> listSeckills(
            @RequestParam(value = "status", required = false, defaultValue = "") String status,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        return Result.success(seckillService.listActivities(status, pageNum, pageSize));
    }

    @PostMapping("/seckill/toggle")
    public Result<Void> toggleSeckill(@RequestParam("activityId") String activityId,
                                      @RequestParam("status") String status,
                                      HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        seckillService.toggle(activityId, status);
        return Result.success();
    }

    // ========== 商家入驻审核（M6 平台化） ==========
    @GetMapping("/merchant/list")
    public Result<com.qinghe.mall.model.Paging<com.qinghe.mall.dataobject.MerchantDO>> listMerchants(
            @RequestParam(value = "status", required = false, defaultValue = "") String status,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        return Result.success(merchantService.list(status, pageNum, pageSize));
    }

    /**
     * 审核商家入驻：POST /api/admin/merchant/audit?merchantId=&amp;approve=&amp;reason=
     * approve=true 通过（ACTIVE），false 驳回（REJECTED + 原因）。
     */
    @PostMapping("/merchant/audit")
    public Result<Void> auditMerchant(@RequestParam("merchantId") Long merchantId,
                                      @RequestParam(value = "approve", defaultValue = "true") boolean approve,
                                      @RequestParam(value = "reason", required = false) String reason,
                                      HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        merchantService.audit(merchantId, approve, reason);
        return Result.success();
    }

    // ========== 用户管理 ==========
    @GetMapping("/user/list")
    public Result<Paging<User>> listAllUsers(
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pagination", required = false) Integer legacyPagination,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        // P1-11：管理端用户分页（原全表捞取）
        // P2-12：页码参数统一 pageNum，pagination 仅作兼容别名
        int page = PageParams.resolve(pageNum, legacyPagination);
        Paging<User> paging = userService.findAdminPage(page, pageSize);
        // 不返回密码
        if (paging.getData() != null) {
            paging.getData().forEach(u -> u.setPwd(null));
        }
        return Result.success(paging);
    }

    @PostMapping("/user/updateRole")
    public Result<Void> updateUserRole(@RequestParam("id") Long id,
                                       @RequestParam("role") String role,
                                       HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        // P2：角色白名单校验，防止写入任意脏数据
        if (!"ADMIN".equals(role) && !"MERCHANT".equals(role) && !"USER".equals(role)) {
            return Result.fail(400, "非法角色，仅支持 ADMIN/MERCHANT/USER");
        }
        userService.updateRole(id, role);
        return Result.success();
    }
}
