package com.qinghe.mall.controller;

import com.qinghe.mall.config.RateLimit;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.OrderService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    private Result<Order> addOrderInternal(Order order, Long userId) {
        order.setUserId(userId);
        // 业务异常由 GlobalExceptionHandler 统一转为 Result.fail
        Order result = orderService.createOrder(order);
        return Result.success(result);
    }

    /** 下单限流：20 次/秒，容量 30（防刷单） */
    @RateLimit(rate = 20, message = "下单过于频繁，请稍后再试")
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
    @RateLimit(rate = 20, message = "下单过于频繁，请稍后再试")
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
     * 申请退款：仅未发货的已付款订单可申请，归属校验。
     */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/refund/apply")
    public Result<Void> applyRefund(@RequestParam("orderNumber") String orderNumber, HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        orderService.applyRefund(orderNumber, (Long) userIdObj);
        return Result.success();
    }

    /**
     * 订单列表（分页，按状态筛选）：GET /api/order/list?status=&pagination=&pageSize=
     * 返回 Paging<Order>：{ pageNum, pageSize, totalPage, totalCount, data }。
     */
    @GetMapping("/list")
    public Result<com.qinghe.mall.model.Paging<Order>> listOrders(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pagination", defaultValue = "1") Integer pagination,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        Long userId = (Long) userIdObj;
        com.qinghe.mall.model.Paging<Order> paging = orderService.findPageByUserIdAndStatus(userId, status, pagination, pageSize);
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
