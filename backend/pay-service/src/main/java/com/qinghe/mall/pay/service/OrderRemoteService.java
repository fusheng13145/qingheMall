package com.qinghe.mall.pay.service;

import com.qinghe.mall.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 订单服务远程调用
 */
@FeignClient(name = "order-service", path = "/api/order")
public interface OrderRemoteService {

    /**
     * 根据订单ID获取订单信息
     */
    @GetMapping("/getById")
    R<Map<String, Object>> getOrderById(@RequestParam("orderId") Long orderId);

    /**
     * 更新订单状态
     * @param orderId 订单ID
     * @param status 订单状态
     */
    @PostMapping("/updateStatus")
    R<?> updateOrderStatus(@RequestParam("orderId") Long orderId, @RequestParam("status") Integer status);
}