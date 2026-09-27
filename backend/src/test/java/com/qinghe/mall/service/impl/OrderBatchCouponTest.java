package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.StockLogService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 购物车级优惠券（v1.8）批量下单单元测试。
 *
 * 覆盖：多单平台券跨店按比例分摊（分币守恒）、门槛按可核销合计口径、
 * 店铺券仅分摊本店订单（混合购物车）、锁券仅一次、防伪造合计校验、
 * 店铺券空集拒绝、多张券拒绝；取消订单的「最后一张持券在途单才还券」守卫。
 */
class OrderBatchCouponTest {

    @Mock
    private OrderDAO orderDAO;

    @Mock
    private ProductDetailService productDetailService;

    @Mock
    private ProductService productService;

    @Mock
    private com.qinghe.mall.service.CouponService couponService;

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

    @Mock
    private com.qinghe.mall.service.UserService userService;

    @Mock
    private com.qinghe.mall.service.SettlementService settlementService;

    @Mock
    private com.qinghe.mall.config.SnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        org.springframework.test.util.ReflectionTestUtils.setField(orderService, "transactionTemplate",
                new TransactionTemplate() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> T execute(TransactionCallback<T> action) {
                        return action.doInTransaction(null);
                    }
                });
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        org.redisson.api.RAtomicLong seq = org.mockito.Mockito.mock(org.redisson.api.RAtomicLong.class);
        when(seq.incrementAndGet()).thenReturn(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(seq);
        try {
            when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        } catch (Exception ignored) {
            // Mockito stub 的受检异常不会真正抛出
        }
        when(snowflakeIdGenerator.nextId()).thenReturn(1L, 2L, 3L, 4L, 5L, 6L);
        when(orderDAO.insert(any(OrderDO.class))).thenReturn(1);
    }

    // ========== 造数工具 ==========

    private Order orderOf(String detailId, int qty) {
        Order o = new Order();
        o.setUserId(9L);
        o.setProductDetailId(detailId);
        o.setQuantity(qty);
        o.setReceiverName("收货人");
        o.setReceiverPhone("13900000000");
        o.setReceiverAddress("北京市海淀区");
        return o;
    }

    private void mockDetail(String detailId, String productId, String price, int stock) {
        ProductDetail pd = new ProductDetail();
        pd.setId(detailId);
        pd.setProductId(productId);
        pd.setPrice(new BigDecimal(price));
        pd.setStock(stock);
        when(productDetailService.findById(detailId)).thenReturn(pd);
        when(productDetailService.decreaseStock(detailId, 1)).thenReturn(true);
    }

    private void mockProduct(String productId, Long merchantId) {
        Product p = new Product();
        p.setId(productId);
        p.setMerchantId(merchantId);
        when(productService.findById(productId)).thenReturn(p);
    }

    private CouponDO platformCoupon(String threshold, String amount) {
        CouponDO c = new CouponDO();
        c.setId("cpt1");
        c.setMerchantId(null);
        c.setThreshold(new BigDecimal(threshold));
        c.setAmount(new BigDecimal(amount));
        return c;
    }

    // ========== 分摊行为 ==========

    @Test
    @DisplayName("多单平台券跨店：按原价比例分摊且分币守恒，锁券仅一次，两单均关联券")
    void batch_platformCoupon_multiOrder_allocatedProportionally() {
        // 满 100 减 30：两单 60 + 50 = 110（分属不同商家）
        mockDetail("dA", "pA", "60.00", 10);
        mockDetail("dB", "pB", "50.00", 10);
        mockProduct("pA", 1L);
        mockProduct("pB", 2L);
        when(couponService.findCouponByUserCouponId("uc1")).thenReturn(platformCoupon("100", "30"));
        when(couponService.validateAndComputeDiscount(eq("uc1"), eq(9L),
                eq(new BigDecimal("110.00")), isNull())).thenReturn(new BigDecimal("30"));

        Order first = orderOf("dA", 1);
        first.setCouponId("uc1");
        first.setDiscountAmount(new BigDecimal("30"));
        List<Order> created = orderService.batchCreateOrders(Arrays.asList(
                first, orderOf("dB", 1)));

        assertEquals(2, created.size());
        // Hamilton（分）：total=3000c，O=[6000c,5000c] → base [1636,1363] + 余数大者(1363.64)+1 → [1636,1364]
        assertEquals(new BigDecimal("16.36"), created.get(0).getDiscountAmount());
        assertEquals(new BigDecimal("13.64"), created.get(1).getDiscountAmount());
        // 分币守恒
        assertEquals(0, created.get(0).getDiscountAmount().add(created.get(1).getDiscountAmount())
                .compareTo(new BigDecimal("30")));
        // 实付 = 原价 − 分摊
        assertEquals(new BigDecimal("43.64"), created.get(0).getTotalPrice());
        assertEquals(new BigDecimal("36.36"), created.get(1).getTotalPrice());
        // 两单均绑定券实例（退款释放守卫/追溯依赖），锁券仅一次
        assertEquals("uc1", created.get(0).getCouponId());
        assertEquals("uc1", created.get(1).getCouponId());
        verify(couponService, times(1)).lockCoupon(eq("uc1"), eq(9L), anyString());
        // 门槛按可核销合计（110）而非单笔
        verify(couponService).validateAndComputeDiscount(eq("uc1"), eq(9L),
                eq(new BigDecimal("110.00")), isNull());
    }

    @Test
    @DisplayName("门槛按整批合计：单笔均不满门槛的购物车可用券（旧版不支持）")
    void batch_thresholdOnBatchTotal() {
        // 满 100 减 30：40 + 70 = 110，单笔均 < 100
        mockDetail("dA", "pA", "40.00", 10);
        mockDetail("dB", "pB", "70.00", 10);
        mockProduct("pA", 1L);
        mockProduct("pB", 1L);
        when(couponService.findCouponByUserCouponId("uc1")).thenReturn(platformCoupon("100", "30"));
        when(couponService.validateAndComputeDiscount(eq("uc1"), eq(9L),
                eq(new BigDecimal("110.00")), isNull())).thenReturn(new BigDecimal("30"));

        Order first = orderOf("dA", 1);
        first.setCouponId("uc1");
        first.setDiscountAmount(new BigDecimal("30"));
        List<Order> created = orderService.batchCreateOrders(Arrays.asList(
                first, orderOf("dB", 1)));

        // Hamilton（分）：total=3000c，O=[4000c,7000c] → base [1090,1909] + 余数大者(1091)+1 → [1091,1909]
        assertEquals(new BigDecimal("10.91"), created.get(0).getDiscountAmount());
        assertEquals(new BigDecimal("19.09"), created.get(1).getDiscountAmount());
        assertEquals(0, created.get(0).getDiscountAmount().add(created.get(1).getDiscountAmount())
                .compareTo(new BigDecimal("30")));
    }

    @Test
    @DisplayName("三单分摊守恒：余数分配后 Σ == 总优惠额")
    void batch_threeOrders_conservation() {
        mockDetail("dA", "pA", "33.33", 10);
        mockDetail("dB", "pB", "33.33", 10);
        mockDetail("dC", "pC", "33.34", 10);
        mockProduct("pA", 1L);
        mockProduct("pB", 1L);
        mockProduct("pC", 1L);
        when(couponService.findCouponByUserCouponId("uc1")).thenReturn(platformCoupon("99", "50"));
        when(couponService.validateAndComputeDiscount(eq("uc1"), eq(9L),
                eq(new BigDecimal("100.00")), isNull())).thenReturn(new BigDecimal("50"));

        Order first = orderOf("dA", 1);
        first.setCouponId("uc1");
        first.setDiscountAmount(new BigDecimal("50"));
        List<Order> created = orderService.batchCreateOrders(Arrays.asList(
                first, orderOf("dB", 1), orderOf("dC", 1)));

        BigDecimal sum = created.stream().map(Order::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(new BigDecimal("50")));
        // 每笔实付非负
        created.forEach(o -> assertTrue(o.getTotalPrice().compareTo(BigDecimal.ZERO) >= 0));
    }

    @Test
    @DisplayName("店铺券混合购物车：仅本店订单分摊，外店订单按原价下单且不绑券")
    void batch_shopCoupon_onlyOwnOrdersDiscounted() {
        // 店铺券（merchantId=1）满 50 减 10：本店 80 + 外店 70
        mockDetail("dOwn", "pOwn", "80.00", 10);
        mockDetail("dOther", "pOther", "70.00", 10);
        mockProduct("pOwn", 1L);
        mockProduct("pOther", 2L);
        CouponDO shopCoupon = new CouponDO();
        shopCoupon.setId("cpt2");
        shopCoupon.setMerchantId(1L);
        when(couponService.findCouponByUserCouponId("uc2")).thenReturn(shopCoupon);
        when(couponService.validateAndComputeDiscount(eq("uc2"), eq(9L),
                eq(new BigDecimal("80.00")), eq(1L))).thenReturn(new BigDecimal("10"));

        Order foreign = orderOf("dOther", 1);
        foreign.setCouponId("uc2");
        foreign.setDiscountAmount(new BigDecimal("10"));
        List<Order> created = orderService.batchCreateOrders(Arrays.asList(
                foreign, orderOf("dOwn", 1)));

        assertEquals(2, created.size());
        // 第一笔（外店）：无优惠、不绑券
        assertNull(created.get(0).getCouponId());
        assertEquals(0, new BigDecimal("0").compareTo(created.get(0).getDiscountAmount()));
        assertEquals(new BigDecimal("70.00"), created.get(0).getTotalPrice());
        // 第二笔（本店）：独享优惠
        assertEquals(new BigDecimal("10.00"), created.get(1).getDiscountAmount());
        assertEquals("uc2", created.get(1).getCouponId());
        assertEquals(new BigDecimal("70.00"), created.get(1).getTotalPrice());
        // 门槛与一致性校验用本店合计与商家 ID
        verify(couponService).validateAndComputeDiscount(eq("uc2"), eq(9L),
                eq(new BigDecimal("80.00")), eq(1L));
        verify(couponService, times(1)).lockCoupon(eq("uc2"), eq(9L), anyString());
    }

    // ========== 守卫与契约 ==========

    @Test
    @DisplayName("防伪造：前端合计优惠额与后端不一致拒绝")
    void batch_providedMismatch_rejected() {
        mockDetail("dA", "pA", "60.00", 10);
        mockProduct("pA", 1L);
        when(couponService.findCouponByUserCouponId("uc1")).thenReturn(platformCoupon("100", "30"));
        when(couponService.validateAndComputeDiscount(anyString(), anyLong(),
                any(BigDecimal.class), any())).thenReturn(new BigDecimal("30"));

        Order o = orderOf("dA", 1);
        o.setCouponId("uc1");
        o.setDiscountAmount(new BigDecimal("25"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.batchCreateOrders(List.of(o)));
        assertEquals("优惠金额校验失败", ex.getMessage());
    }

    @Test
    @DisplayName("店铺券无可核销订单（整批均为外店）拒绝")
    void batch_shopCoupon_noEligible_rejected() {
        mockDetail("dOther", "pOther", "70.00", 10);
        mockProduct("pOther", 2L);
        CouponDO shopCoupon = new CouponDO();
        shopCoupon.setMerchantId(1L);
        when(couponService.findCouponByUserCouponId("uc2")).thenReturn(shopCoupon);

        Order o = orderOf("dOther", 1);
        o.setCouponId("uc2");
        o.setDiscountAmount(new BigDecimal("10"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.batchCreateOrders(List.of(o)));
        assertEquals("店铺券仅可用于本店商品", ex.getMessage());
    }

    @Test
    @DisplayName("一次结算仅可使用一张优惠券")
    void batch_twoCoupons_rejected() {
        Order a = orderOf("dA", 1);
        a.setCouponId("uc1");
        Order b = orderOf("dB", 1);
        b.setCouponId("uc2");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.batchCreateOrders(Arrays.asList(a, b)));
        assertEquals("一次结算仅可使用一张优惠券", ex.getMessage());
    }

    // ========== 取消订单释放守卫 ==========

    @Test
    @DisplayName("取消订单：同券仍有其它在途订单时不归还")
    void cancel_couponStillHeldByOtherOrder_notReleased() {
        OrderDO orderDO = new OrderDO();
        orderDO.setOrderNumber("QH001");
        orderDO.setUserId(9L);
        orderDO.setStatus(OrderStatus.WAIT_BUYER_PAY.name());
        orderDO.setProductDetailId("pd001");
        orderDO.setQuantity(1);
        orderDO.setCouponId("uc1");
        when(orderDAO.findByOrderNumber("QH001")).thenReturn(orderDO);
        when(orderDAO.updateStatusIfWaitPay("QH001", OrderStatus.TRADE_CLOSED.name())).thenReturn(1);
        when(productDetailService.increaseStock("pd001", 1)).thenReturn(true);
        // 另一单仍持有同券
        when(orderDAO.countActiveByCouponExcluding("uc1", 9L, "QH001")).thenReturn(1);

        assertTrue(orderService.cancelOrder("QH001", 9L));

        verify(couponService, never()).releaseCoupon("uc1");
    }

    @Test
    @DisplayName("取消订单：本单是最后一张持券在途单时归还")
    void cancel_lastCouponHolder_released() {
        OrderDO orderDO = new OrderDO();
        orderDO.setOrderNumber("QH001");
        orderDO.setUserId(9L);
        orderDO.setStatus(OrderStatus.WAIT_BUYER_PAY.name());
        orderDO.setProductDetailId("pd001");
        orderDO.setQuantity(1);
        orderDO.setCouponId("uc1");
        when(orderDAO.findByOrderNumber("QH001")).thenReturn(orderDO);
        when(orderDAO.updateStatusIfWaitPay("QH001", OrderStatus.TRADE_CLOSED.name())).thenReturn(1);
        when(productDetailService.increaseStock("pd001", 1)).thenReturn(true);
        when(orderDAO.countActiveByCouponExcluding("uc1", 9L, "QH001")).thenReturn(0);

        assertTrue(orderService.cancelOrder("QH001", 9L));

        verify(couponService).releaseCoupon("uc1");
    }
}
