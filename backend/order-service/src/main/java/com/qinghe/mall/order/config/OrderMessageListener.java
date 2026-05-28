package com.qinghe.mall.order.config;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.qinghe.mall.order.service.OrderService;

import java.util.Map;

/**
 * RabbitMQ消息监听器
 */
@Component
public class OrderMessageListener {

    @Autowired
    private OrderService orderService;

    /**
     * 监听订单取消消息
     */
    @RabbitListener(queues = "order.delay.queue")
    public void handleOrderCancelMessage(Map<String, Object> message) {
        Long orderId = ((Number) message.get("orderId")).longValue();
        orderService.timeoutCancelOrder(orderId);
    }
}
