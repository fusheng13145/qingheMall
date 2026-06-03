package com.qinghe.mall.controller;

import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/add")
    public Result<Order> addOrder(@RequestBody Order order, HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        Long userId = (Long) userIdObj;
        order.setUserId(userId);
        try {
            Order result = orderService.createOrder(order);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<Order>> listOrders(@RequestParam(value = "status", required = false) String status,
                                          HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        Long userId = (Long) userIdObj;
        List<Order> orders = orderService.findByUserIdAndStatus(userId, status);
        return Result.success(orders);
    }

    @GetMapping("/get")
    public Result<Order> getByOrderNumber(@RequestParam("orderNumber") String orderNumber) {
        Order order = orderService.findByOrderNumber(orderNumber);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        return Result.success(order);
    }
}
