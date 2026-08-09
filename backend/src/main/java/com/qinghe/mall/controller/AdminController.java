package com.qinghe.mall.controller;

import com.qinghe.mall.model.*;
import com.qinghe.mall.service.*;
import com.qinghe.mall.dataobject.CouponDO;
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
        stats.put("totalRevenue", orderService.sumTotalPriceByStatus(OrderStatus.TRADE_PAID_SUCCESS.name()));
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
            @RequestParam(value = "pagination", defaultValue = "1") Integer pagination,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        return Result.success(productService.queryPage(pagination, pageSize, keyword, null, null));
    }

    // ========== 订单管理 ==========
    @GetMapping("/order/list")
    public Result<Paging<Order>> listAllOrders(
            @RequestParam(value = "pagination", defaultValue = "1") Integer pagination,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        // P1-11：管理端订单分页（原全表捞取，数据量大时超时/OOM）
        return Result.success(orderService.findAdminPage(pagination, pageSize, status));
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
     * 发货：仅已付款订单可发货，状态机守卫（管理员操作）。
     */
    @PostMapping("/order/ship")
    public Result<Void> shipOrder(@RequestParam("orderNumber") String orderNumber, HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        orderService.shipOrder(orderNumber);
        return Result.success();
    }

    /**
     * 处理退款：退款中订单 → 已退款(approve) 或回退已付款(reject)，状态机守卫（管理员操作）。
     */
    @PostMapping("/order/refund/process")
    public Result<Void> processRefund(@RequestParam("orderNumber") String orderNumber,
                                      @RequestParam(value = "approve", defaultValue = "true") boolean approve,
                                      HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        orderService.processRefund(orderNumber, approve);
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
            @RequestParam(value = "pagination", defaultValue = "1") Integer pagination,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        // P1-11：管理端用户分页（原全表捞取）
        Paging<User> paging = userService.findAdminPage(pagination, pageSize);
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
