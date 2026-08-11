package com.qinghe.mall.controller;

import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.model.Logistics;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.LogisticsService;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流查询（P2-18）：轨迹时间线对订单归属用户、归属商家与管理员开放。
 */
@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {

    @Autowired
    private LogisticsService logisticsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MerchantService merchantService;

    /** 物流跟踪：GET /api/logistics/track?orderNumber=（本人 / 归属商家 / 管理员） */
    @GetMapping("/track")
    public Result<Logistics> track(@RequestParam("orderNumber") String orderNumber,
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
            return Result.fail(403, "无权查看该订单的物流");
        }
        Logistics logistics = logisticsService.track(orderNumber);
        if (logistics == null) {
            return Result.fail("暂无物流信息");
        }
        return Result.success(logistics);
    }

    /** 可见性：订单归属用户 / 管理员 / 订单归属商家（MERCHANT 角色 + 店铺匹配） */
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
