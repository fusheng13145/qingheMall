package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.util.UUIDUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private ProductDetailService productDetailService;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public Order createOrder(Order order) {
        String productDetailId = order.getProductDetailId();
        if (productDetailId == null) {
            throw new RuntimeException("商品规格ID不能为空");
        }

        ProductDetail productDetail = productDetailService.findById(productDetailId);
        if (productDetail == null) {
            throw new RuntimeException("商品规格不存在");
        }
        if (productDetail.getStock() <= 0) {
            throw new RuntimeException("库存不足");
        }

        // 使用分布式锁保证库存安全
        String lockKey = "order:lock:" + productDetailId;
        try {
            boolean locked = redissonClient.getLock(lockKey).tryLock(3, 5, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
            try {
                // 再次检查库存
                productDetail = productDetailService.findById(productDetailId);
                if (productDetail == null || productDetail.getStock() <= 0) {
                    throw new RuntimeException("库存不足");
                }

                // 扣减库存
                productDetailService.updateStock(productDetailId, productDetail.getStock() - 1);

                // 生成订单号
                String orderNumber = generateOrderNumber();

                OrderDO orderDO = new OrderDO();
                orderDO.setId(UUIDUtils.uuid());
                orderDO.setOrderNumber(orderNumber);
                orderDO.setUserId(order.getUserId());
                orderDO.setProductDetailId(productDetailId);
                orderDO.setTotalPrice(productDetail.getPrice());
                orderDO.setStatus(OrderStatus.WAIT_BUYER_PAY.name());
                orderDO.setGmtCreated(new Date());
                orderDO.setGmtModified(new Date());
                orderDAO.insert(orderDO);

                Order result = orderDO.convertToModel();
                result.setProductDetail(productDetail);
                return result;
            } finally {
                redissonClient.getLock(lockKey).unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("创建订单失败");
        }
    }

    @Override
    public Order findByOrderNumber(String orderNumber) {
        OrderDO orderDO = orderDAO.findByOrderNumber(orderNumber);
        if (orderDO == null) {
            return null;
        }
        Order order = orderDO.convertToModel();
        // 填充商品详情
        if (order.getProductDetailId() != null) {
            ProductDetail productDetail = productDetailService.findById(order.getProductDetailId());
            order.setProductDetail(productDetail);
        }
        return order;
    }

    @Override
    public List<Order> findByUserIdAndStatus(Long userId, String status) {
        List<OrderDO> orderDOs = orderDAO.findByUserIdAndStatus(userId, status);
        List<Order> orders = new ArrayList<>();
        for (OrderDO orderDO : orderDOs) {
            Order order = orderDO.convertToModel();
            if (order.getProductDetailId() != null) {
                ProductDetail productDetail = productDetailService.findById(order.getProductDetailId());
                order.setProductDetail(productDetail);
            }
            orders.add(order);
        }
        return orders;
    }

    @Override
    public boolean updateOrderStatus(String orderNumber, String status) {
        return orderDAO.updateStatus(orderNumber, status) > 0;
    }

    @Override
    public List<Order> findAll() {
        List<OrderDO> orderDOs = orderDAO.findAll();
        List<Order> orders = new ArrayList<>();
        for (OrderDO orderDO : orderDOs) {
            Order order = orderDO.convertToModel();
            if (order.getProductDetailId() != null) {
                ProductDetail productDetail = productDetailService.findById(order.getProductDetailId());
                order.setProductDetail(productDetail);
            }
            orders.add(order);
        }
        return orders;
    }

    private String generateOrderNumber() {
        // 使用 Redisson 的原子自增生成订单号
        long sequence = redissonClient.getAtomicLong("order:seq").incrementAndGet();
        return "QH" + System.currentTimeMillis() + String.format("%04d", sequence % 10000);
    }
}
