package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.pagehelper.PageHelper;
import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.StockLogService;
import com.qinghe.mall.service.UserService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * OrderServiceImpl 分支覆盖补测（#37）：覆盖此前缺失的易测分支——
 * 空白参数守卫（IllegalArgumentException/BusinessException）、分页归一化（pageNum/pageSize 边界）、
 * sumTotalPriceByStatuses（0%→全分支）、dailySalesReport 天数钳制、assertMerchantOwnership 归属校验、
 * fillExtraBatch 多分支、firstImg 图片解析。不含 createOrder/batchCreateOrders 事务链路（另有主测覆盖）。
 */
class OrderServiceImplCoverageTest {

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
    private CommentDAO commentDAO;
    @Mock
    private com.qinghe.mall.config.OrderTimeoutQueue orderTimeoutQueue;
    @Mock
    private com.qinghe.mall.service.SeckillService seckillService;
    @Mock
    private com.qinghe.mall.service.CouponService couponService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(orderService, "transactionTemplate",
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
        // fillExtraBatch 批量查询全部返回空，保证任意订单列表组装安全
        when(productDetailService.findByIds(anyList())).thenReturn(Collections.emptyList());
        when(productService.findByIds(anyList())).thenReturn(Collections.emptyList());
        when(userService.findByIds(anyList())).thenReturn(Collections.emptyList());
        when(commentDAO.findCommentedOrderNumbers(anyList())).thenReturn(Collections.emptyList());
    }

    private OrderDO orderDO(String num, Long userId, String status, Long merchantId, String detailId) {
        OrderDO o = new OrderDO();
        o.setOrderNumber(num);
        o.setUserId(userId);
        o.setStatus(status);
        o.setMerchantId(merchantId);
        o.setProductDetailId(detailId);
        o.setQuantity(1);
        return o;
    }

    // ========== 空白/非法参数守卫 ==========

    @Test
    void shipOrder_blankOrderNumber_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> orderService.shipOrder(""));
        assertEquals("订单号不能为空", ex.getMessage());
    }

    @Test
    void shipOrder_nullOrderNumber_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> orderService.shipOrder(null));
        assertEquals("订单号不能为空", ex.getMessage());
    }

    @Test
    void confirmReceipt_blankOrderNumber_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> orderService.confirmReceipt("", 1L));
        assertEquals("订单号不能为空", ex.getMessage());
    }

    @Test
    void applyRefund_blankOrderNumber_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> orderService.applyRefund("", 1L));
        assertEquals("订单号不能为空", ex.getMessage());
    }

    @Test
    void processRefund_blankOrderNumber_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> orderService.processRefund("", true));
        assertEquals("订单号不能为空", ex.getMessage());
    }

    @Test
    void cancelOrder_blankOrderNumber_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.cancelOrder("", 1L));
        assertEquals("订单号不能为空", ex.getMessage());
    }

    @Test
    void updateOrderStatus_blankOrderNumber_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.updateOrderStatus("", OrderStatus.TRADE_SHIPPED.name()));
        assertEquals("订单号不能为空", ex.getMessage());
    }

    @Test
    void updateOrderStatus_invalidStatus_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.updateOrderStatus("QH001", "NOT_A_STATUS"));
        assertTrue(ex.getMessage().contains("订单状态不合法"));
    }

    @Test
    void updateOrderStatus_nullStatus_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.updateOrderStatus("QH001", null));
        assertTrue(ex.getMessage().contains("订单状态不合法"));
    }

    // ========== sumTotalPriceByStatuses（0% → 全分支） ==========

    @Test
    void sumTotalPriceByStatuses_null_shouldReturnZero() {
        assertEquals(BigDecimal.ZERO, orderService.sumTotalPriceByStatuses(null));
    }

    @Test
    void sumTotalPriceByStatuses_empty_shouldReturnZero() {
        assertEquals(BigDecimal.ZERO, orderService.sumTotalPriceByStatuses(Collections.emptyList()));
    }

    @Test
    void sumTotalPriceByStatuses_nonEmpty_shouldReturnSum() {
        when(orderDAO.sumTotalPriceByStatuses(anyList())).thenReturn(new BigDecimal("258.50"));
        assertEquals(new BigDecimal("258.50"), orderService.sumTotalPriceByStatuses(List.of("WAIT_BUYER_PAY")));
    }

    @Test
    void sumTotalPriceByStatuses_daoReturnsNull_shouldReturnZero() {
        when(orderDAO.sumTotalPriceByStatuses(anyList())).thenReturn(null);
        assertEquals(BigDecimal.ZERO, orderService.sumTotalPriceByStatuses(List.of("TRADE_PAID_SUCCESS")));
    }

    // ========== dailySalesReport 天数钳制 ==========

    @Test
    void dailySalesReport_normalDays_shouldCallDao() {
        when(orderDAO.dailySalesReportByStatuses(anyInt(), anyList())).thenReturn(new ArrayList<>());
        List<Map<String, Object>> r = orderService.dailySalesReport(7);
        assertNotNull(r);
        verify(orderDAO).dailySalesReportByStatuses(eq(7), anyList());
    }

    @Test
    void dailySalesReport_tooSmall_shouldClampTo7() {
        when(orderDAO.dailySalesReportByStatuses(anyInt(), anyList())).thenReturn(new ArrayList<>());
        orderService.dailySalesReport(0);
        verify(orderDAO).dailySalesReportByStatuses(eq(7), anyList());
    }

    @Test
    void dailySalesReport_tooLarge_shouldClampTo7() {
        when(orderDAO.dailySalesReportByStatuses(anyInt(), anyList())).thenReturn(new ArrayList<>());
        orderService.dailySalesReport(100);
        verify(orderDAO).dailySalesReportByStatuses(eq(7), anyList());
    }


    // ========== 分页归一化（findPageByUserIdAndStatus / findAdminPage / listByMerchant） ==========

    @Test
    void findPageByUserIdAndStatus_nullPagination_shouldDefault() {
        when(orderDAO.findByUserIdAndStatus(anyLong(), any())).thenReturn(Collections.emptyList());
        Paging<Order> p = orderService.findPageByUserIdAndStatus(1L, null, null, null);
        assertEquals(1, p.getPageNum());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void findPageByUserIdAndStatus_outOfRange_shouldDefault() {
        when(orderDAO.findByUserIdAndStatus(anyLong(), any())).thenReturn(Collections.emptyList());
        Paging<Order> p = orderService.findPageByUserIdAndStatus(1L, "ALL", 0, 100);
        assertEquals(1, p.getPageNum());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void findAdminPage_nullPagination_shouldDefault() {
        when(orderDAO.queryAdminPage(any())).thenReturn(Collections.emptyList());
        Paging<Order> p = orderService.findAdminPage(null, null, null);
        assertEquals(1, p.getPageNum());
        assertEquals(20, p.getPageSize());
    }

    @Test
    void findAdminPage_outOfRange_shouldDefault() {
        when(orderDAO.queryAdminPage(any())).thenReturn(Collections.emptyList());
        Paging<Order> p = orderService.findAdminPage(-5, 0, "ALL");
        assertEquals(1, p.getPageNum());
        assertEquals(20, p.getPageSize());
    }

    @Test
    void listByMerchant_nullMerchant_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.listByMerchant(null, null, 1, 10));
        assertEquals("商家信息缺失", ex.getMessage());
    }

    @Test
    void listByMerchant_outOfRangePageSize_shouldDefault() {
        when(orderDAO.findByMerchantId(anyLong(), any())).thenReturn(Collections.emptyList());
        Paging<Order> p = orderService.listByMerchant(5L, null, 0, 100);
        assertEquals(1, p.getPageNum());
        assertEquals(10, p.getPageSize());
    }

    // ========== assertMerchantOwnership（经 shipMerchantOrder 触发） ==========

    @Test
    void shipMerchantOrder_nullMerchant_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.shipMerchantOrder(null, "QH001"));
        assertEquals("商家信息缺失", ex.getMessage());
    }

    @Test
    void shipMerchantOrder_blankOrderNumber_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.shipMerchantOrder(5L, ""));
        assertEquals("订单号不能为空", ex.getMessage());
    }

    @Test
    void shipMerchantOrder_orderNotExist_shouldThrow() {
        when(orderDAO.findByOrderNumber("QH001")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.shipMerchantOrder(5L, "QH001"));
        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    void shipMerchantOrder_mismatchMerchant_shouldThrow() {
        when(orderDAO.findByOrderNumber("QH001")).thenReturn(orderDO("QH001", 1L, "WAIT_BUYER_PAY", 9L, null));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.shipMerchantOrder(5L, "QH001"));
        assertEquals("无权操作其他店铺的订单", ex.getMessage());
    }

    @Test
    void shipMerchantOrder_owner_shouldSucceed() {
        when(orderDAO.findByOrderNumber("QH001")).thenReturn(orderDO("QH001", 1L, "TRADE_PAID_SUCCESS", 5L, null));
        when(orderDAO.updateStatusWithGuard("QH001", OrderStatus.TRADE_PAID_SUCCESS.name(), OrderStatus.TRADE_SHIPPED.name()))
                .thenReturn(1);
        assertTrue(orderService.shipMerchantOrder(5L, "QH001"));
    }

    // ========== fillExtraBatch / firstImg ==========

    @Test
    void fillExtraBatch_emptyList_shouldReturnSame() {
        // 通过 listByMerchant 触发空结果路径：orders 空 -> fillExtraBatch 短路
        when(orderDAO.findByMerchantId(anyLong(), any())).thenReturn(Collections.emptyList());
        Paging<Order> p = orderService.listByMerchant(5L, null, 1, 10);
        assertTrue(p.getData().isEmpty());
    }

    @Test
    void fillExtraBatch_withProductImg_shouldParseFirstImg() {
        // 经 findByUserIdAndStatus（不经 PageHelper）触发 fillExtraBatch 有商品分支
        ProductDetail pd = new ProductDetail();
        pd.setId("pd1");
        pd.setProductId("p1");
        Product product = new Product();
        product.setId("p1");
        product.setName("测试商品");
        product.setProductImgs("a.jpg; b.jpg");
        OrderDO o = orderDO("QH001", 1L, "TRADE_PAID_SUCCESS", null, "pd1");
        when(orderDAO.findByUserIdAndStatus(anyLong(), any())).thenReturn(List.of(o));
        when(productDetailService.findByIds(anyList())).thenReturn(List.of(pd));
        when(productService.findByIds(anyList())).thenReturn(List.of(product));
        List<Order> orders = orderService.findByUserIdAndStatus(1L, null);
        assertEquals("a.jpg", orders.get(0).getProductImg());
    }

    @Test
    void firstImg_blankImgs_shouldReturnNull() {
        ProductDetail pd = new ProductDetail();
        pd.setId("pd1");
        pd.setProductId("p1");
        Product product = new Product();
        product.setId("p1");
        product.setProductImgs("   ");
        OrderDO o = orderDO("QH001", 1L, "TRADE_PAID_SUCCESS", null, "pd1");
        when(orderDAO.findByUserIdAndStatus(anyLong(), any())).thenReturn(List.of(o));
        when(productDetailService.findByIds(anyList())).thenReturn(List.of(pd));
        when(productService.findByIds(anyList())).thenReturn(List.of(product));
        List<Order> orders = orderService.findByUserIdAndStatus(1L, null);
        assertNull(orders.get(0).getProductImg());
    }
}
