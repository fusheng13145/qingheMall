package com.qinghe.mall.order.controller;

import com.github.pagehelper.PageInfo;
import com.qinghe.mall.common.R;
import com.qinghe.mall.order.dto.OrderCreateDTO;
import com.qinghe.mall.order.service.OrderService;
import com.qinghe.mall.order.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public R<Long> createOrder(@RequestBody OrderCreateDTO dto) {
        // TODO: 从登录用户获取userId，这里暂时使用模拟数据
        Long userId = 1L;
        Long orderId = orderService.createOrder(userId, dto);
        return R.success(orderId);
    }

    /**
     * 查询订单列表
     */
    @GetMapping("/list")
    public R<PageInfo<OrderVO>> getOrderList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        // TODO: 从登录用户获取userId，这里暂时使用模拟数据
        Long userId = 1L;
        PageInfo<OrderVO> pageInfo = orderService.getOrderList(userId, status, pageNum, pageSize);
        return R.success(pageInfo);
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{id}")
    public R<OrderVO> getOrderDetail(@PathVariable("id") Long orderId) {
        OrderVO orderVO = orderService.getOrderDetail(orderId);
        return R.success(orderVO);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{orderId}")
    public R<Void> cancelOrder(@PathVariable("orderId") Long orderId) {
        orderService.cancelOrder(orderId);
        return R.success();
    }

    /**
     * 确认收货
     */
    @PostMapping("/confirm/{orderId}")
    public R<Void> confirmReceive(@PathVariable("orderId") Long orderId) {
        orderService.confirmReceive(orderId);
        return R.success();
    }
}
