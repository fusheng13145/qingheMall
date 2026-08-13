package com.qinghe.mall.controller;

import com.qinghe.mall.config.RateLimit;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.RefundRequest;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.RefundService;
import com.qinghe.mall.util.PageParams;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RefundService refundService;

    private Result<Order> addOrderInternal(Order order, Long userId) {
        order.setUserId(userId);
        // 业务异常由 GlobalExceptionHandler 统一转为 Result.fail
        Order result = orderService.createOrder(order);
        return Result.success(result);
    }

    /** 下单限流：per-user 20 次/秒（解除全局 20/s 天花板，仍防单用户刷单，#36） */
    @RateLimit(key = "order.add.{userId}", rate = 20, message = "下单过于频繁，请稍后再试")
    @PostMapping("/add")
    public Result<Order> addOrder(@RequestBody Order order, HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        return addOrderInternal(order, (Long) userIdObj);
    }

    /**
     * 批量下单（购物车结算）：接收订单数组 [{productDetailId, quantity, receiverName, ...}]。
     * 部分成功语义：任一笔失败即中断，已创建订单保留。
     */
    @RateLimit(key = "order.batchAdd.{userId}", rate = 20, message = "下单过于频繁，请稍后再试")
    @PostMapping("/batchAdd")
    public Result<List<Order>> batchAdd(@RequestBody List<Order> orders, HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        Long userId = (Long) userIdObj;
        for (Order order : orders) {
            order.setUserId(userId);
        }
        List<Order> created = orderService.batchCreateOrders(orders);
        return Result.success(created);
    }

    /**
     * 取消订单：仅待付款可取消，回滚库存并置 TRADE_CLOSED。
     */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/cancel")
    public Result<Void> cancelOrder(@RequestParam("orderNumber") String orderNumber, HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        orderService.cancelOrder(orderNumber, (Long) userIdObj);
        return Result.success();
    }

    /**
     * 确认收货：仅已发货订单可确认，归属校验。
     */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/confirmReceipt")
    public Result<Void> confirmReceipt(@RequestParam("orderNumber") String orderNumber, HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        orderService.confirmReceipt(orderNumber, (Long) userIdObj);
        return Result.success();
    }

    /**
     * 申请退款/退货（P2-18）：
     * - 已付款未发货 → 仅退款（REFUND_ONLY）
     * - 已发货/已完成 → 退货退款（RETURN_REFUND）
     * 类型缺省时按订单状态自动推导；reason 必填。归属校验 + 重复申请守卫。
     */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/refund/apply")
    public Result<RefundRequest> applyRefund(@RequestParam("orderNumber") String orderNumber,
                                             @RequestParam(value = "type", required = false) String type,
                                             @RequestParam(value = "reason", required = false) String reason,
                                             HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        RefundRequest created = refundService.apply(orderNumber, (Long) userIdObj, type, reason);
        return Result.success(created);
    }

    /**
     * 订单列表（分页，按状态筛选）：GET /api/order/list?status=&pageNum=&pageSize=
     * 返回 Paging<Order>：{ pageNum, pageSize, totalPage, totalCount, data }。
     * （P2-12：页码统一 pageNum，旧参数 pagination 仍兼容）
     */
    @GetMapping("/list")
    public Result<com.qinghe.mall.model.Paging<Order>> listOrders(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pagination", required = false) Integer legacyPagination,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        Long userId = (Long) userIdObj;
        int page = PageParams.resolve(pageNum, legacyPagination);
        com.qinghe.mall.model.Paging<Order> paging = orderService.findPageByUserIdAndStatus(userId, status, page, pageSize);
        return Result.success(paging);
    }

    @GetMapping("/get")
    public Result<Order> getByOrderNumber(@RequestParam("orderNumber") String orderNumber,
                                          HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        Long userId = (Long) userIdObj;
        Object roleObj = request.getSession().getAttribute("role");
        boolean isAdmin = "ADMIN".equals(roleObj);

        Order order = orderService.findByOrderNumber(orderNumber);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        // 越权校验：仅本人或管理员可查看
        if (!isAdmin && !userId.equals(order.getUserId())) {
            return Result.fail(403, "无权查看该订单");
        }
        return Result.success(order);
    }
}
