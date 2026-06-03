package com.qinghe.mall.service;

import com.qinghe.mall.model.Order;
import java.util.List;

public interface OrderService {

    Order createOrder(Order order);

    Order findByOrderNumber(String orderNumber);

    List<Order> findByUserIdAndStatus(Long userId, String status);

    boolean updateOrderStatus(String orderNumber, String status);

    List<Order> findAll();
}
