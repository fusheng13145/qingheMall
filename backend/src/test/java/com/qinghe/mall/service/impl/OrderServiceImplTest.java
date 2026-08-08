package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.StockLogService;
import com.qinghe.mall.service.UserService;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 订单服务 单元测试（M3-5）
 * 覆盖：下单扣库存与金额计算、库存不足拒绝、取消订单状态校验与库存回滚。
 */
class OrderServiceImplTest {

    @Mock
    private OrderDAO orderDAO;

    @Mock
    private ProductDetailService productDetailService;

    @Mock
    private ProductService productService;

    @Mock
    private UserService userService;

    @Mock
    private StockLogService stockLogService;

    @Mock
    private com.qinghe.mall.dao.CommentDAO commentDAO;

    @Mock
    private com.qinghe.mall.config.OrderTimeoutQueue orderTimeoutQueue;

    @Mock
    private com.qinghe.mall.service.SeckillService seckillService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 事务模板：直接执行回调（模拟事务提交成功）
        org.springframework.test.util.ReflectionTestUtils.setField(orderService, "transactionTemplate",
                new TransactionTemplate() {
                    @Override
                    public <T> T execute(TransactionCallback<T> action) {
                        return action.doInTransaction(null);
                    }
                });
        // 分布式锁：可获取、可释放
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        try {
            when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        // 订单号原子自增
        java.util.concurrent.atomic.AtomicLong seq = new java.util.concurrent.atomic.AtomicLong(1);
        org.redisson.api.RAtomicLong atomicLong = org.mockito.Mockito.mock(org.redisson.api.RAtomicLong.class);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenAnswer(inv -> seq.incrementAndGet());
    }

    private ProductDetail mockDetail(String id, BigDecimal price, int stock) {
        ProductDetail pd = new ProductDetail();
        pd.setId(id);
        pd.setProductId("p001");
        pd.setPrice(price);
        pd.setStock(stock);
        return pd;
    }

    private Order newOrder(Long userId, String detailId, Integer quantity) {
        Order order = new Order();
        order.setUserId(userId);
        order.setProductDetailId(detailId);
        order.setQuantity(quantity);
        return order;
    }

    @Test
    void createOrder_shouldDeductStockAndComputeTotalPrice() {
        ProductDetail pd = mockDetail("pd001", new BigDecimal("799.00"), 50);
        when(productDetailService.findById("pd001")).thenReturn(pd);
        when(productDetailService.decreaseStock("pd001", 2)).thenReturn(true);
        when(orderDAO.insert(any(OrderDO.class))).thenReturn(1);

        Order result = orderService.createOrder(newOrder(1L, "pd001", 2));

        verify(productDetailService).decreaseStock("pd001", 2);
        assertEquals(new BigDecimal("1598.00"), result.getTotalPrice(), "金额应为单价×数量（精确）");
        assertEquals(OrderStatus.WAIT_BUYER_PAY, result.getStatus());
    }

    @Test
    void createOrder_stockNotEnough_shouldThrow() {
        ProductDetail pd = mockDetail("pd001", new BigDecimal("799.00"), 1);
        when(productDetailService.findById("pd001")).thenReturn(pd);
        when(productDetailService.decreaseStock("pd001", 3)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.createOrder(newOrder(1L, "pd001", 3)));
        assertEquals("库存不足", ex.getMessage());
        verify(orderDAO, never()).insert(any());
    }

    @Test
    void createOrder_detailNotExist_shouldThrow() {
        when(productDetailService.findById("pd999")).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.createOrder(newOrder(1L, "pd999", 1)));
        assertEquals("商品规格不存在", ex.getMessage());
    }

    @Test
    void cancelOrder_notOwner_shouldThrow() {
        OrderDO orderDO = new OrderDO();
        orderDO.setOrderNumber("QH001");
        orderDO.setUserId(2L);
        orderDO.setStatus(OrderStatus.WAIT_BUYER_PAY.name());
        orderDO.setProductDetailId("pd001");
        orderDO.setQuantity(1);
        when(orderDAO.findByOrderNumber("QH001")).thenReturn(orderDO);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.cancelOrder("QH001", 1L));
        assertEquals("无权操作此订单", ex.getMessage());
    }

    @Test
    void cancelOrder_alreadyPaid_shouldThrow() {
        OrderDO orderDO = new OrderDO();
        orderDO.setOrderNumber("QH001");
        orderDO.setUserId(1L);
        orderDO.setStatus(OrderStatus.TRADE_PAID_SUCCESS.name());
        orderDO.setProductDetailId("pd001");
        orderDO.setQuantity(1);
        when(orderDAO.findByOrderNumber("QH001")).thenReturn(orderDO);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.cancelOrder("QH001", 1L));
        assertEquals("订单状态异常，无法关闭", ex.getMessage());
        verify(orderDAO, never()).updateStatusIfWaitPay(anyString(), anyString());
    }

    @Test
    void cancelOrder_shouldRestoreStock() {
        OrderDO orderDO = new OrderDO();
        orderDO.setOrderNumber("QH001");
        orderDO.setUserId(1L);
        orderDO.setStatus(OrderStatus.WAIT_BUYER_PAY.name());
        orderDO.setProductDetailId("pd001");
        orderDO.setQuantity(3);
        when(orderDAO.findByOrderNumber("QH001")).thenReturn(orderDO);
        when(orderDAO.updateStatusIfWaitPay("QH001", OrderStatus.TRADE_CLOSED.name())).thenReturn(1);
        when(productDetailService.increaseStock("pd001", 3)).thenReturn(true);

        boolean cancelled = orderService.cancelOrder("QH001", 1L);

        assertTrue(cancelled);
        verify(productDetailService).increaseStock("pd001", 3);
        verify(orderDAO).updateStatusIfWaitPay("QH001", OrderStatus.TRADE_CLOSED.name());
    }

    @Test
    void cancelOrder_concurrentStatusChanged_shouldThrow() {
        OrderDO orderDO = new OrderDO();
        orderDO.setOrderNumber("QH001");
        orderDO.setUserId(1L);
        orderDO.setStatus(OrderStatus.WAIT_BUYER_PAY.name());
        orderDO.setProductDetailId("pd001");
        orderDO.setQuantity(1);
        when(orderDAO.findByOrderNumber("QH001")).thenReturn(orderDO);
        // 模拟并发下订单已被支付：原子更新失败
        when(orderDAO.updateStatusIfWaitPay("QH001", OrderStatus.TRADE_CLOSED.name())).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.cancelOrder("QH001", 1L));
        assertEquals("订单状态已变化，请刷新后重试", ex.getMessage());
        verify(productDetailService, never()).increaseStock(anyString(), anyInt());
    }

    @Test
    void updateOrderStatus_shouldReturnResult() {
        when(orderDAO.updateStatus("QH001", OrderStatus.TRADE_CLOSED.name())).thenReturn(1);
        assertTrue(orderService.updateOrderStatus("QH001", OrderStatus.TRADE_CLOSED.name()));
        when(orderDAO.updateStatus("QH001", OrderStatus.TRADE_CLOSED.name())).thenReturn(0);
        assertFalse(orderService.updateOrderStatus("QH001", OrderStatus.TRADE_CLOSED.name()));
    }

    // ========== 退款 / 发货状态机（M6） ==========

    private OrderDO paidOrderDO(String orderNumber, Long userId) {
        OrderDO orderDO = new OrderDO();
        orderDO.setOrderNumber(orderNumber);
        orderDO.setUserId(userId);
        orderDO.setStatus(OrderStatus.TRADE_PAID_SUCCESS.name());
        orderDO.setProductDetailId(null); // 跳过 fillExtra 商品查询
        return orderDO;
    }

    @Test
    void shipOrder_paidToShipped_shouldSucceed() {
        when(orderDAO.updateStatusWithGuard("QH100", OrderStatus.TRADE_PAID_SUCCESS.name(), OrderStatus.TRADE_SHIPPED.name()))
                .thenReturn(1);
        assertTrue(orderService.shipOrder("QH100"));
    }

    @Test
    void shipOrder_notPaid_shouldThrow() {
        when(orderDAO.updateStatusWithGuard("QH100", OrderStatus.TRADE_PAID_SUCCESS.name(), OrderStatus.TRADE_SHIPPED.name()))
                .thenReturn(0);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.shipOrder("QH100"));
        assertEquals("订单状态异常，无法发货（仅已付款订单可发货）", ex.getMessage());
    }

    @Test
    void confirmReceipt_shippedToCompleted_shouldSucceed() {
        OrderDO orderDO = new OrderDO();
        orderDO.setOrderNumber("QH200");
        orderDO.setUserId(1L);
        orderDO.setStatus(OrderStatus.TRADE_SHIPPED.name());
        orderDO.setProductDetailId(null);
        when(orderDAO.findByOrderNumber("QH200")).thenReturn(orderDO);
        when(orderDAO.updateStatusWithGuard("QH200", OrderStatus.TRADE_SHIPPED.name(), OrderStatus.TRADE_COMPLETED.name()))
                .thenReturn(1);
        assertTrue(orderService.confirmReceipt("QH200", 1L));
    }

    @Test
    void confirmReceipt_notOwner_shouldThrow() {
        OrderDO orderDO = new OrderDO();
        orderDO.setOrderNumber("QH200");
        orderDO.setUserId(2L);
        orderDO.setStatus(OrderStatus.TRADE_SHIPPED.name());
        orderDO.setProductDetailId(null);
        when(orderDAO.findByOrderNumber("QH200")).thenReturn(orderDO);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.confirmReceipt("QH200", 1L));
        assertEquals("无权操作此订单", ex.getMessage());
    }

    @Test
    void applyRefund_paidToRefunding_shouldSucceed() {
        when(orderDAO.findByOrderNumber("QH300")).thenReturn(paidOrderDO("QH300", 1L));
        when(orderDAO.updateStatusWithGuard("QH300", OrderStatus.TRADE_PAID_SUCCESS.name(), OrderStatus.TRADE_REFUNDING.name()))
                .thenReturn(1);
        assertTrue(orderService.applyRefund("QH300", 1L));
    }

    @Test
    void applyRefund_alreadyShipped_shouldThrow() {
        OrderDO orderDO = new OrderDO();
        orderDO.setOrderNumber("QH300");
        orderDO.setUserId(1L);
        orderDO.setStatus(OrderStatus.TRADE_SHIPPED.name());
        orderDO.setProductDetailId(null);
        when(orderDAO.findByOrderNumber("QH300")).thenReturn(orderDO);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.applyRefund("QH300", 1L));
        assertEquals("订单状态异常，无法申请退款（仅未发货的已付款订单可申请）", ex.getMessage());
    }

    @Test
    void processRefund_approve_shouldRefund() {
        when(orderDAO.updateStatusWithGuard("QH400", OrderStatus.TRADE_REFUNDING.name(), OrderStatus.TRADE_REFUNDED.name()))
                .thenReturn(1);
        assertTrue(orderService.processRefund("QH400", true));
    }

    @Test
    void processRefund_reject_shouldRevertToPaid() {
        when(orderDAO.updateStatusWithGuard("QH400", OrderStatus.TRADE_REFUNDING.name(), OrderStatus.TRADE_PAID_SUCCESS.name()))
                .thenReturn(1);
        assertTrue(orderService.processRefund("QH400", false));
    }

    @Test
    void processRefund_notRefunding_shouldThrow() {
        when(orderDAO.updateStatusWithGuard("QH400", OrderStatus.TRADE_REFUNDING.name(), OrderStatus.TRADE_REFUNDED.name()))
                .thenReturn(0);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.processRefund("QH400", true));
        assertEquals("订单状态异常，无法处理退款（仅退款中订单可处理）", ex.getMessage());
    }
}
