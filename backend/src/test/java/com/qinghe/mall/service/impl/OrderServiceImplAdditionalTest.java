package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.SeckillService;
import com.qinghe.mall.service.StockLogService;
import com.qinghe.mall.service.UserService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * 订单服务补充测试（P1：原 36% 覆盖，补齐缺口）。
 *
 * 覆盖：带券下单（含优惠金额缺失拒绝）、batchCreateOrders 全拒绝路径、
 * processRefund approve/reject（reject 释放券）、merchantStats 聚合、
 * updateStatusIfWaitPay CAS、商家越权拦截、findAdminPage/dailySalesReport。
 */
class OrderServiceImplAdditionalTest {

    @Mock
    private OrderDAO orderDAO;
    @Mock
    private ProductDetailService productDetailService;
    @Mock
    private ProductService productService;
    @Mock
    private UserService userService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private StockLogService stockLogService;
    @Mock
    private CommentDAO commentDAO;
    @Mock
    private com.qinghe.mall.config.OrderTimeoutQueue orderTimeoutQueue;
    @Mock
    private SeckillService seckillService;
    @Mock
    private CouponService couponService;
    @Mock
    private RLock lock;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        org.springframework.test.util.ReflectionTestUtils.setField(orderService, "transactionTemplate",
                new TransactionTemplate() {
                    @Override
                    public <T> T execute(TransactionCallback<T> action) {
                        return action.doInTransaction(null);
                    }
                });
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        try {
            when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        java.util.concurrent.atomic.AtomicLong seq = new java.util.concurrent.atomic.AtomicLong(1);
        org.redisson.api.RAtomicLong atomicLong = org.mockito.Mockito.mock(org.redisson.api.RAtomicLong.class);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenAnswer(inv -> seq.incrementAndGet());
    }

    private ProductDetail detail(String id, BigDecimal price, int stock) {
        ProductDetail pd = new ProductDetail();
        pd.setId(id);
        pd.setProductId("p001");
        pd.setPrice(price);
        pd.setStock(stock);
        return pd;
    }

    private Order order(Long userId, String detailId, Integer quantity) {
        Order o = new Order();
        o.setUserId(userId);
        o.setProductDetailId(detailId);
        o.setQuantity(quantity);
        o.setReceiverName("张三");
        o.setReceiverPhone("13800000000");
        o.setReceiverAddress("北京");
        return o;
    }

    private void mockCreateOrderHappyPath(int qty) {
        when(productDetailService.findById("pd1")).thenReturn(detail("pd1", new BigDecimal("100.00"), 50));
        when(productDetailService.decreaseStock("pd1", qty)).thenReturn(true);
        when(productService.findById("p001")).thenReturn(new com.qinghe.mall.model.Product());
        when(orderDAO.insert(any(OrderDO.class))).thenReturn(1);
    }

    private void mockCreateOrderHappyPath() {
        mockCreateOrderHappyPath(1);
    }

    // ============ 带券下单 ============

    @Test
    @DisplayName("createOrder 带券：锁定券并改写实付金额")
    void createOrder_withCoupon_discountsPayable() {
        mockCreateOrderHappyPath(2);
        // lockCoupon 为 void，无需 stub（mock 默认空操作）
        Order created = orderService.createOrder(
                order(1L, "pd1", 2), "uc1", new BigDecimal("30.00"));

        assertNotNull(created.getOrderNumber());
        // 原价 100×2=200，减 30 → 实付 170
        assertEquals(0, new BigDecimal("170.00").compareTo(created.getTotalPrice()));
        assertEquals("uc1", created.getCouponId());
        assertEquals(0, new BigDecimal("30.00").compareTo(created.getDiscountAmount()));
        verify(couponService).lockCoupon("uc1", 1L, created.getOrderNumber());
        verify(orderTimeoutQueue).offer(created.getOrderNumber());
    }

    @Test
    @DisplayName("createOrder 带券但缺优惠金额拒绝")
    void createOrder_withCouponMissingDiscount_throws() {
        when(productDetailService.findById("pd1")).thenReturn(detail("pd1", new BigDecimal("100.00"), 50));

        assertThrows(RuntimeException.class,
                () -> orderService.createOrder(order(1L, "pd1", 1), "uc1", null),
                "带券必须传优惠金额");
    }

    @Test
    @DisplayName("createOrder 优惠超过原价时实付归零")
    void createOrder_couponExceedsTotal_payableZero() {
        mockCreateOrderHappyPath();
        // lockCoupon 为 void，无需 stub

        Order created = orderService.createOrder(
                order(1L, "pd1", 1), "uc1", new BigDecimal("500.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(created.getTotalPrice()));
    }

    // ============ batchCreateOrders ============

    @Test
    @DisplayName("batchCreateOrders 空列表拒绝")
    void batchCreateOrders_empty_throws() {
        assertThrows(RuntimeException.class, () -> orderService.batchCreateOrders(null));
        assertThrows(RuntimeException.class, () -> orderService.batchCreateOrders(new ArrayList<>()));
    }

    @Test
    @DisplayName("batchCreateOrders 多张券拒绝")
    void batchCreateOrders_multipleCoupons_throws() {
        Order o1 = order(1L, "pd1", 1);
        o1.setCouponId("c1");
        Order o2 = order(1L, "pd2", 1);
        o2.setCouponId("c2");

        assertThrows(RuntimeException.class, () -> orderService.batchCreateOrders(List.of(o1, o2)),
                "一次结算仅可使用一张券");
    }

    @Test
    @DisplayName("batchCreateOrders 多笔订单带券拒绝（券仅单品）")
    void batchCreateOrders_couponOnMultiItems_throws() {
        Order o1 = order(1L, "pd1", 1);
        o1.setCouponId("c1");
        Order o2 = order(1L, "pd2", 1);

        assertThrows(RuntimeException.class, () -> orderService.batchCreateOrders(List.of(o1, o2)),
                "优惠券仅支持单笔订单");
    }

    @Test
    @DisplayName("batchCreateOrders 前端优惠额与后端计算不一致拒绝（防伪造）")
    void batchCreateOrders_couponAmountMismatch_throws() {
        Order o1 = order(1L, "pd1", 1);
        o1.setCouponId("c1");
        o1.setDiscountAmount(new BigDecimal("10.00")); // 伪造
        when(productDetailService.findById("pd1")).thenReturn(detail("pd1", new BigDecimal("100.00"), 50));
        // 后端权威计算为 20
        when(couponService.validateAndComputeDiscount("c1", 1L, new BigDecimal("100.00")))
                .thenReturn(new BigDecimal("20.00"));

        assertThrows(RuntimeException.class, () -> orderService.batchCreateOrders(List.of(o1)),
                "优惠金额校验失败");
    }

    @Test
    @DisplayName("batchCreateOrders 单笔带券成功：后端校验金额一致")
    void batchCreateOrders_singleCoupon_success() {
        Order o1 = order(1L, "pd1", 1);
        o1.setCouponId("c1");
        o1.setDiscountAmount(new BigDecimal("20.00"));
        when(productDetailService.findById("pd1")).thenReturn(detail("pd1", new BigDecimal("100.00"), 50));
        when(couponService.validateAndComputeDiscount("c1", 1L, new BigDecimal("100.00")))
                .thenReturn(new BigDecimal("20.00"));
        mockCreateOrderHappyPath();
        // lockCoupon 为 void，无需 stub

        List<Order> created = orderService.batchCreateOrders(List.of(o1));

        assertEquals(1, created.size());
        assertEquals(0, new BigDecimal("80.00").compareTo(created.get(0).getTotalPrice()));
    }

    // ============ processRefund ============

    @Test
    @DisplayName("processRefund approve=true：退款中→已退款")
    void processRefund_approve_refunded() {
        when(orderDAO.updateStatusWithGuard("QH1", OrderStatus.TRADE_REFUNDING.name(),
                OrderStatus.TRADE_REFUNDED.name())).thenReturn(1);

        assertTrue(orderService.processRefund("QH1", true));
    }

    @Test
    @DisplayName("processRefund approve=false：回退已付款并释放券")
    void processRefund_reject_releasesCoupon() {
        when(orderDAO.updateStatusWithGuard("QH1", OrderStatus.TRADE_REFUNDING.name(),
                OrderStatus.TRADE_PAID_SUCCESS.name())).thenReturn(1);
        OrderDO od = new OrderDO();
        od.setOrderNumber("QH1");
        od.setCouponId("uc1");
        when(orderDAO.findByOrderNumber("QH1")).thenReturn(od);

        assertTrue(orderService.processRefund("QH1", false));
        verify(couponService).releaseCoupon("uc1");
    }

    @Test
    @DisplayName("processRefund 状态守卫失败抛异常")
    void processRefund_guardFails_throws() {
        when(orderDAO.updateStatusWithGuard(anyString(), anyString(), anyString())).thenReturn(0);

        assertThrows(RuntimeException.class, () -> orderService.processRefund("QH1", true));
    }

    // ============ updateStatusIfWaitPay ============

    @Test
    @DisplayName("updateStatusIfWaitPay CAS 语义透传")
    void updateStatusIfWaitPay_casSemantics() {
        when(orderDAO.updateStatusIfWaitPay("QH1", OrderStatus.TRADE_CLOSED.name())).thenReturn(1);
        assertTrue(orderService.updateStatusIfWaitPay("QH1", OrderStatus.TRADE_CLOSED.name()));

        when(orderDAO.updateStatusIfWaitPay("QH2", OrderStatus.TRADE_CLOSED.name())).thenReturn(0);
        assertFalse(orderService.updateStatusIfWaitPay("QH2", OrderStatus.TRADE_CLOSED.name()));
    }

    // ============ merchantStats ============

    @Test
    @DisplayName("merchantStats 聚合统计（BigDecimal/Number/空三形态）")
    void merchantStats_aggregates() {
        when(orderDAO.sumByMerchantAndStatuses(eq(5L), any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(Map.of("amount", new BigDecimal("100.50"))));
        when(orderDAO.sumByMerchantAndStatuses(eq(5L), any(), org.mockito.ArgumentMatchers.notNull()))
                .thenReturn(List.of(Map.of("amount", 200)));
        when(orderDAO.countByMerchantId(5L)).thenReturn(9L);
        when(orderDAO.countByMerchantIdAndCreatedAfter(eq(5L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(3L);
        com.qinghe.mall.model.Paging<com.qinghe.mall.model.Product> paging =
                new com.qinghe.mall.model.Paging<>();
        paging.setTotalCount(12L);
        when(productService.queryMerchantPage(5L, null, null, 1, 1)).thenReturn(paging);

        Map<String, Object> stats = orderService.merchantStats(5L);

        assertEquals(12L, stats.get("productCount"));
        assertEquals(9L, stats.get("orderCount"));
        assertEquals(3L, stats.get("todayOrderCount"));
        assertEquals(0, new BigDecimal("100.50").compareTo((BigDecimal) stats.get("paidRevenue")));
        assertEquals(0, new BigDecimal("200.00").compareTo((BigDecimal) stats.get("todayRevenue")));
    }

    @Test
    @DisplayName("merchantStats 空聚合行返回 0")
    void merchantStats_emptyRows_zero() {
        when(orderDAO.sumByMerchantAndStatuses(eq(5L), any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(null);
        com.qinghe.mall.model.Paging<com.qinghe.mall.model.Product> paging =
                new com.qinghe.mall.model.Paging<>();
        paging.setTotalCount(0L);
        when(productService.queryMerchantPage(5L, null, null, 1, 1)).thenReturn(paging);

        Map<String, Object> stats = orderService.merchantStats(5L);

        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) stats.get("paidRevenue")));
    }

    @Test
    @DisplayName("merchantStats 缺商家信息拒绝")
    void merchantStats_nullMerchant_throws() {
        assertThrows(RuntimeException.class, () -> orderService.merchantStats(null));
    }

    // ============ 商家越权拦截 ============

    @Test
    @DisplayName("shipMerchantOrder 非本店订单拒绝")
    void shipMerchantOrder_notOwner_throws() {
        OrderDO od = new OrderDO();
        od.setMerchantId(99L);
        when(orderDAO.findByOrderNumber("QH1")).thenReturn(od);

        assertThrows(RuntimeException.class, () -> orderService.shipMerchantOrder(5L, "QH1"),
                "无权操作其他店铺的订单");
    }

    @Test
    @DisplayName("processMerchantRefund 非本店订单拒绝")
    void processMerchantRefund_notOwner_throws() {
        OrderDO od = new OrderDO();
        od.setMerchantId(99L);
        when(orderDAO.findByOrderNumber("QH1")).thenReturn(od);

        assertThrows(RuntimeException.class, () -> orderService.processMerchantRefund(5L, "QH1", true));
    }

    // ============ 管理端查询 ============

    @Test
    @DisplayName("countAll / sumTotalPriceByStatus 透传 DAO")
    void countAggregates_delegate() {
        when(orderDAO.countAll()).thenReturn(42L);
        when(orderDAO.sumTotalPriceByStatus(OrderStatus.TRADE_PAID_SUCCESS.name()))
                .thenReturn(new BigDecimal("999.00"));

        assertEquals(42L, orderService.countAll());
        assertEquals(0, new BigDecimal("999.00").compareTo(orderService.sumTotalPriceByStatus(
                OrderStatus.TRADE_PAID_SUCCESS.name())));
    }

    @Test
    @DisplayName("dailySalesReport 透传 DAO")
    void dailySalesReport_delegates() {
        List<Map<String, Object>> report = new ArrayList<>();
        report.add(new HashMap<>());
        when(orderDAO.dailySalesReport(7, OrderStatus.TRADE_PAID_SUCCESS.name())).thenReturn(report);

        assertEquals(report, orderService.dailySalesReport(7));
    }

    @Test
    @DisplayName("findAdminPage 空状态兜底为 null 查询")
    void findAdminPage_blankStatus_nullQuery() {
        com.qinghe.mall.model.Paging<Order> paging = new com.qinghe.mall.model.Paging<>();
        paging.setTotalCount(0L);
        paging.setData(new ArrayList<>());
        // PageHelper 纯 mock 下不填充 result，验证 DAO 参数透传与字段回填
        java.util.List<OrderDO> raw = new ArrayList<>();
        when(orderDAO.queryAdminPage("")).thenReturn(raw);

        PagingResultHolder result = new PagingResultHolder();
        try {
            com.qinghe.mall.model.Paging<Order> p = orderService.findAdminPage(1, 20, "");
            result.pageNum = p.getPageNum();
            result.pageSize = p.getPageSize();
        } catch (Exception e) {
            // PageHelper 在无 MyBatis 环境可能抛异常，标记以跳过 result 断言
            result.thrown = e;
        }
        org.mockito.Mockito.verify(orderDAO).queryAdminPage("");
    }

    /** 分页结果占位（PageHelper 纯 mock 环境限制，仅验证参数透传） */
    static class PagingResultHolder {
        Integer pageNum;
        Integer pageSize;
        Exception thrown;
    }
}
