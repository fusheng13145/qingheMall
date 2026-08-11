package com.qinghe.mall.controller;

import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.RefundRequest;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.RefundService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款申请查询（P2-18）：申请动作在 /api/order/refund/apply，
 * 审核动作在商家端/管理端订单接口；此处仅提供查询。
 */
@RestController
@RequestMapping("/api/refund")
public class RefundController {

    @Autowired
    private RefundService refundService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MerchantService merchantService;

    /** 我的退款申请列表（倒序）：GET /api/refund/mine */
    @GetMapping("/mine")
    public Result<List<RefundRequest>> mine(HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(refundService.listByUser((Long) userIdObj));
    }

    /** 指定订单的申请历史（本人 / 归属商家 / 管理员）：GET /api/refund/order?orderNumber= */
    @GetMapping("/order")
    public Result<List<RefundRequest>> byOrder(@RequestParam("orderNumber") String orderNumber,
                                               HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        Long userId = (Long) userIdObj;
        Order order = orderService.findByOrderNumber(orderNumber);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!canView(userId, request.getSession().getAttribute("role"), order)) {
            return Result.fail(403, "无权查看该订单的退款申请");
        }
        return Result.success(refundService.listByOrder(orderNumber));
    }

    /** 可见性：订单归属用户 / 管理员 / 订单归属商家（商家审核退款需看到申请原因） */
    private boolean canView(Long userId, Object role, Order order) {
        if ("ADMIN".equals(role)) {
            return true;
        }
        if (userId.equals(order.getUserId())) {
            return true;
        }
        if ("MERCHANT".equals(role) && order.getMerchantId() != null) {
            MerchantDO merchant = merchantService.getByUserId(userId);
            return merchant != null && order.getMerchantId().equals(merchant.getId());
        }
        return false;
    }
}
