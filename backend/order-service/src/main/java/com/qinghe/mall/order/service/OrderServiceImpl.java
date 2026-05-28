package com.qinghe.mall.order.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qinghe.mall.common.R;
import com.qinghe.mall.order.client.CartFeignClient;
import com.qinghe.mall.order.client.GoodsFeignClient;
import com.qinghe.mall.order.client.UserFeignClient;
import com.qinghe.mall.order.dto.OrderCreateDTO;
import com.qinghe.mall.order.entity.Order;
import com.qinghe.mall.order.entity.OrderItem;
import com.qinghe.mall.order.mapper.OrderItemMapper;
import com.qinghe.mall.order.mapper.OrderMapper;
import com.qinghe.mall.order.vo.OrderItemVO;
import com.qinghe.mall.order.vo.OrderVO;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private GoodsFeignClient goodsFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${order.timeout.cancel:30}")
    private int orderTimeoutMinutes;

    /**
     * 订单状态常量
     */
    private static final int STATUS_PENDING_PAYMENT = 0;   // 待付款
    private static final int STATUS_PAID = 1;              // 已付款
    private static final int STATUS_DELIVERED = 2;         // 已发货
    private static final int STATUS_COMPLETED = 3;          // 已完成
    private static final int STATUS_CANCELLED = 4;          // 已取消

    /**
     * 消息队列交换器名称
     */
    private static final String ORDER_EXCHANGE = "order.exchange";
    private static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    private static final String ORDER_ROUTING_KEY = "order.cancel";

    @Override
    @GlobalTransactional(name = "order-create", rollbackFor = Exception.class)
    public Long createOrder(Long userId, OrderCreateDTO dto) {
        log.info("创建订单，用户ID：{}，地址ID：{}，购物车ID：{}", userId, dto.getAddressId(), dto.getCartIds());

        // 1. 获取用户地址信息
        R<Map<String, Object>> addressResult = userFeignClient.getAddress(dto.getAddressId());
        if (addressResult == null || addressResult.getCode() != 200 || addressResult.getData() == null) {
            throw new RuntimeException("获取收货地址失败");
        }
        Map<String, Object> address = addressResult.getData();

        // 2. 获取选中的购物车商品
        R<List<Map<String, Object>>> cartResult = cartFeignClient.getSelectedItems(dto.getCartIds());
        if (cartResult == null || cartResult.getCode() != 200 || cartResult.getData() == null) {
            throw new RuntimeException("获取购物车商品失败");
        }
        List<Map<String, Object>> cartItems = cartResult.getData();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("购物车为空");
        }

        // 3. 构建订单商品列表并扣减库存
        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, Integer> goodsQuantityMap = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map<String, Object> cartItem : cartItems) {
            OrderItem item = new OrderItem();
            item.setGoodsId(((Number) cartItem.get("goodsId")).longValue());
            item.setGoodsName((String) cartItem.get("goodsName"));
            item.setGoodsImage((String) cartItem.get("goodsImage"));
            item.setPrice(new BigDecimal(cartItem.get("price").toString()));
            item.setQuantity(((Number) cartItem.get("quantity")).intValue());
            item.setCreateTime(LocalDateTime.now());
            orderItems.add(item);

            goodsQuantityMap.put(item.getGoodsId(), item.getQuantity());
            totalAmount = totalAmount.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }

        // 4. 远程调用扣减库存
        R<Void> deductResult = goodsFeignClient.deductStock(goodsQuantityMap);
        if (deductResult == null || deductResult.getCode() != 200) {
            throw new RuntimeException("扣减库存失败");
        }

        // 5. 创建订单
        Order order = new Order();
        order.setOrderSn(generateOrderSn());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setCouponAmount(BigDecimal.ZERO);
        order.setStatus(STATUS_PENDING_PAYMENT);
        order.setReceiver((String) address.get("receiver"));
        order.setReceiverPhone((String) address.get("phone"));
        order.setReceiverAddress((String) address.get("address"));
        order.setRemark(dto.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.insert(order);

        // 6. 保存订单明细
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.insertBatch(orderItems);

        // 7. 删除购物车商品
        cartFeignClient.deleteByCartIds(dto.getCartIds());

        // 8. 发送延迟消息，实现订单超时取消
        sendDelayMessage(order.getId());

        log.info("订单创建成功，订单ID：{}，订单编号：{}", order.getId(), order.getOrderSn());
        return order.getId();
    }

    @Override
    public PageInfo<OrderVO> getOrderList(Long userId, Integer status, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Order> orders = orderMapper.selectList(userId, status);
        PageInfo<Order> orderPageInfo = new PageInfo<>(orders);

        PageInfo<OrderVO> result = new PageInfo<>();
        result.setTotal(orderPageInfo.getTotal());
        result.setPages(orderPageInfo.getPages());
        result.setPageNum(orderPageInfo.getPageNum());
        result.setPageSize(orderPageInfo.getPageSize());

        List<OrderVO> voList = new ArrayList<>();
        for (Order order : orders) {
            voList.add(convertToOrderVO(order));
        }
        result.setList(voList);

        return result;
    }

    @Override
    public OrderVO getOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        return convertToOrderVO(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != STATUS_PENDING_PAYMENT) {
            throw new RuntimeException("订单状态不允许取消");
        }

        order.setStatus(STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.update(order);

        log.info("订单已取消，订单ID：{}", orderId);
    }

    @Override
    @Transactional
    public void confirmReceive(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != STATUS_DELIVERED) {
            throw new RuntimeException("订单状态不允许确认收货");
        }

        order.setStatus(STATUS_COMPLETED);
        order.setFinishTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.update(order);

        log.info("订单已确认收货，订单ID：{}", orderId);
    }

    @Override
    @Transactional
    public void payOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != STATUS_PENDING_PAYMENT) {
            throw new RuntimeException("订单状态不允许支付");
        }

        order.setStatus(STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.update(order);

        log.info("订单已支付，订单ID：{}", orderId);
    }

    @Override
    @Transactional
    public void deliverOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != STATUS_PAID) {
            throw new RuntimeException("订单状态不允许发货");
        }

        order.setStatus(STATUS_DELIVERED);
        order.setDeliveryTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.update(order);

        log.info("订单已发货，订单ID：{}", orderId);
    }

    @Override
    @Transactional
    public void timeoutCancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("超时取消订单失败，订单不存在，订单ID：{}", orderId);
            return;
        }
        if (order.getStatus() != STATUS_PENDING_PAYMENT) {
            log.info("超时取消订单跳过，订单状态不是待付款，订单ID：{}，状态：{}", orderId, order.getStatus());
            return;
        }

        order.setStatus(STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.update(order);

        log.info("订单超时已自动取消，订单ID：{}", orderId);
    }

    /**
     * 生成订单编号：时间戳+随机数
     */
    private String generateOrderSn() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return timestamp + random;
    }

    /**
     * 发送延迟消息
     */
    private void sendDelayMessage(Long orderId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("orderId", orderId);
            message.put("timestamp", System.currentTimeMillis());
            rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_ROUTING_KEY, message);
            log.info("订单延迟消息已发送，订单ID：{}，延迟时间：{}分钟", orderId, orderTimeoutMinutes);
        } catch (Exception e) {
            log.error("发送订单延迟消息失败，订单ID：{}", orderId, e);
        }
    }

    /**
     * 转换为OrderVO
     */
    private OrderVO convertToOrderVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderSn(order.getOrderSn());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setCouponAmount(order.getCouponAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(getStatusDesc(order.getStatus()));
        vo.setReceiver(order.getReceiver());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setRemark(order.getRemark());
        vo.setPayTime(order.getPayTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCreateTime(order.getCreateTime());

        // 查询订单明细
        List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
        List<OrderItemVO> itemVOList = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setOrderId(item.getOrderId());
            itemVO.setGoodsId(item.getGoodsId());
            itemVO.setGoodsName(item.getGoodsName());
            itemVO.setGoodsImage(item.getGoodsImage());
            itemVO.setPrice(item.getPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotal(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
            itemVOList.add(itemVO);
        }
        vo.setItems(itemVOList);

        return vo;
    }

    /**
     * 获取订单状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case STATUS_PENDING_PAYMENT:
                return "待付款";
            case STATUS_PAID:
                return "已付款";
            case STATUS_DELIVERED:
                return "已发货";
            case STATUS_COMPLETED:
                return "已完成";
            case STATUS_CANCELLED:
                return "已取消";
            default:
                return "未知";
        }
    }
}
