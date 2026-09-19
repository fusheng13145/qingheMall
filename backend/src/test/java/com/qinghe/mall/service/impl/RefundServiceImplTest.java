package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dao.RefundRequestDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.dataobject.RefundRequestDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.RefundRequest;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.StockLogService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 退货退款服务 单元测试（P2-18）。
 * 覆盖：申请类型推导/守卫、审核通过与驳回的原子流转、库存回补留痕、兼容通道。
 */
class RefundServiceImplTest {

    @Mock
    private OrderDAO orderDAO;

    @Mock
    private RefundRequestDAO refundRequestDAO;

    @Mock
    private ProductDetailService productDetailService;

    @Mock
    private StockLogService stockLogService;

    @Mock
    private CouponService couponService;

    @Mock
    private com.qinghe.mall.service.SettlementService settlementService;

    @InjectMocks
    private RefundServiceImpl refundService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 事务模板：直接执行回调（模拟事务提交成功）
        org.springframework.test.util.ReflectionTestUtils.setField(refundService, "transactionTemplate",
                new TransactionTemplate() {
                    @Override
                    public <T> T execute(TransactionCallback<T> action) {
                        return action.doInTransaction(null);
                    }
                });
    }

    private OrderDO order(String orderNumber, Long userId, String status) {
        OrderDO od = new OrderDO();
        od.setId("oid-" + orderNumber);
        od.setOrderNumber(orderNumber);
        od.setUserId(userId);
        od.setStatus(status);
        od.setProductDetailId("pd1");
        od.setQuantity(2);
        return od;
    }

    private RefundRequestDO pendingRequest(String orderNumber, String previousStatus) {
        RefundRequestDO req = new RefundRequestDO();
        req.setId("r1");
        req.setOrderNumber(orderNumber);
        req.setUserId(1L);
        req.setType(RefundRequestDO.TYPE_REFUND_ONLY);
        req.setReason("不想要了");
        req.setStatus(RefundRequestDO.STATUS_PENDING);
        req.setPreviousOrderStatus(previousStatus);
        return req;
    }

    // ========== 申请 ==========

    @Test
    void apply_paidOrder_resolvedRefundOnly() {
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_PAID_SUCCESS.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(refundRequestDAO.findPendingByOrderNumber("O1")).thenReturn(null);
        when(orderDAO.updateStatusWithGuardIn(eq("O1"), anyList(),
                eq(OrderStatus.TRADE_REFUNDING.name()))).thenReturn(1);

        RefundRequest created = refundService.apply("O1", 1L, null, " 不想要了 ");

        assertNotNull(created);
        assertEquals(RefundRequestDO.TYPE_REFUND_ONLY, created.getType());
        assertEquals(RefundRequestDO.STATUS_PENDING, created.getStatus());
        // 多来源 CAS：已付款单状态来源
        ArgumentCaptor<List<String>> sources = ArgumentCaptor.forClass(List.class);
        verify(orderDAO).updateStatusWithGuardIn(eq("O1"), sources.capture(),
                eq(OrderStatus.TRADE_REFUNDING.name()));
        assertEquals(Collections.singletonList(OrderStatus.TRADE_PAID_SUCCESS.name()), sources.getValue());
        // 申请单落库：原因去空白、记录申请前状态
        ArgumentCaptor<RefundRequestDO> captor = ArgumentCaptor.forClass(RefundRequestDO.class);
        verify(refundRequestDAO).insert(captor.capture());
        assertEquals("不想要了", captor.getValue().getReason());
        assertEquals(OrderStatus.TRADE_PAID_SUCCESS.name(), captor.getValue().getPreviousOrderStatus());
    }

    @Test
    void apply_shippedOrder_resolvedReturnRefund_multiSources() {
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_SHIPPED.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(orderDAO.updateStatusWithGuardIn(anyString(), anyList(), anyString())).thenReturn(1);

        RefundRequest created = refundService.apply("O1", 1L, null, "商品破损");

        assertEquals(RefundRequestDO.TYPE_RETURN_REFUND, created.getType());
        verify(orderDAO).updateStatusWithGuardIn(eq("O1"),
                eq(Arrays.asList(OrderStatus.TRADE_SHIPPED.name(), OrderStatus.TRADE_COMPLETED.name())),
                eq(OrderStatus.TRADE_REFUNDING.name()));
    }

    @Test
    void apply_completedOrder_resolvedReturnRefund() {
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_COMPLETED.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(orderDAO.updateStatusWithGuardIn(anyString(), anyList(), anyString())).thenReturn(1);

        RefundRequest created = refundService.apply("O1", 1L, "RETURN_REFUND", "七天无理由");

        assertEquals(RefundRequestDO.TYPE_RETURN_REFUND, created.getType());
    }

    @Test
    void apply_explicitTypeMismatch_throws() {
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_PAID_SUCCESS.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.apply("O1", 1L, "RETURN_REFUND", "理由"));
        assertEquals("该订单状态仅支持仅退款申请", ex.getMessage());
        verify(refundRequestDAO, never()).insert(any());
    }

    @Test
    void apply_blankReason_throws() {
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_PAID_SUCCESS.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.apply("O1", 1L, null, "   "));
        assertEquals("请填写退款原因", ex.getMessage());
    }

    @Test
    void apply_reasonTooLong_throws() {
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_PAID_SUCCESS.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);

        String tooLong = "长".repeat(201);
        assertThrows(BusinessException.class, () -> refundService.apply("O1", 1L, null, tooLong));
    }

    @Test
    void apply_orderNotFound_throws() {
        when(orderDAO.findByOrderNumber("O1")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.apply("O1", 1L, null, "理由"));
        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    void apply_notOwner_throws() {
        OrderDO od = order("O1", 99L, OrderStatus.TRADE_PAID_SUCCESS.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.apply("O1", 1L, null, "理由"));
        assertEquals("无权操作此订单", ex.getMessage());
    }

    @Test
    void apply_waitPayOrder_unsupported() {
        OrderDO od = order("O1", 1L, OrderStatus.WAIT_BUYER_PAY.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.apply("O1", 1L, null, "理由"));
        assertEquals("当前订单状态不支持申请退款", ex.getMessage());
    }

    @Test
    void apply_refundingOrder_throws() {
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_REFUNDING.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.apply("O1", 1L, null, "理由"));
        assertEquals("该订单已有退款申请正在审核，请耐心等待", ex.getMessage());
    }

    @Test
    void apply_duplicatePending_throws() {
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_PAID_SUCCESS.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(refundRequestDAO.findPendingByOrderNumber("O1"))
                .thenReturn(pendingRequest("O1", OrderStatus.TRADE_PAID_SUCCESS.name()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.apply("O1", 1L, null, "理由"));
        assertEquals("该订单已有待审核的退款申请，请勿重复提交", ex.getMessage());
        verify(orderDAO, never()).updateStatusWithGuardIn(anyString(), anyList(), anyString());
    }

    @Test
    void apply_casRace_throwsAndNoInsert() {
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_PAID_SUCCESS.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(orderDAO.updateStatusWithGuardIn(anyString(), anyList(), anyString())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.apply("O1", 1L, null, "理由"));
        assertEquals("订单状态已变化，请刷新后重试", ex.getMessage());
        verify(refundRequestDAO, never()).insert(any());
    }

    @Test
    void apply_blankOrderNumber_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> refundService.apply(" ", 1L, null, "理由"));
    }

    @Test
    void apply_nullUserId_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.apply("O1", null, null, "理由"));
        assertEquals("请先登录", ex.getMessage());
    }

    // ========== 审核 ==========

    @Test
    void review_approve_refundsOrderAndRestoresStockAndReleasesCoupon() {
        RefundRequestDO pending = pendingRequest("O1", OrderStatus.TRADE_PAID_SUCCESS.name());
        when(refundRequestDAO.findPendingByOrderNumber("O1")).thenReturn(pending);
        when(refundRequestDAO.reviewWithGuard("r1", RefundRequestDO.STATUS_APPROVED, "同意")).thenReturn(1);
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_REFUNDING.name());
        od.setCouponId("uc1");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(orderDAO.updateStatusWithGuard("O1", OrderStatus.TRADE_REFUNDING.name(),
                OrderStatus.TRADE_REFUNDED.name())).thenReturn(1);
        ProductDetail detail = new ProductDetail();
        detail.setId("pd1");
        detail.setProductId("p1");
        detail.setStock(5);
        when(productDetailService.findById("pd1")).thenReturn(detail);

        refundService.review("O1", true, "同意");

        verify(orderDAO).updateStatusWithGuard("O1",
                OrderStatus.TRADE_REFUNDING.name(), OrderStatus.TRADE_REFUNDED.name());
        verify(productDetailService).increaseStock("pd1", 2);
        verify(stockLogService).record("pd1", "p1", "O1",
                StockLogService.TYPE_REFUND_RESTORE, 2, 5, 7);
        verify(couponService).releaseCoupon("uc1");
    }

    @Test
    void review_reject_restoresPreviousShippedStatus_keepsCoupon() {
        RefundRequestDO pending = pendingRequest("O1", OrderStatus.TRADE_SHIPPED.name());
        when(refundRequestDAO.findPendingByOrderNumber("O1")).thenReturn(pending);
        when(refundRequestDAO.reviewWithGuard("r1", RefundRequestDO.STATUS_REJECTED, "不符合条件")).thenReturn(1);
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_REFUNDING.name());
        od.setCouponId("uc1");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(orderDAO.updateStatusWithGuard("O1", OrderStatus.TRADE_REFUNDING.name(),
                OrderStatus.TRADE_SHIPPED.name())).thenReturn(1);

        refundService.review("O1", false, "不符合条件");

        // 驳回：订单回到申请前的已发货状态；交易继续有效，不释放券、不回补库存
        verify(orderDAO).updateStatusWithGuard("O1",
                OrderStatus.TRADE_REFUNDING.name(), OrderStatus.TRADE_SHIPPED.name());
        verify(productDetailService, never()).increaseStock(anyString(), org.mockito.ArgumentMatchers.anyInt());
        verify(couponService, never()).releaseCoupon(anyString());
    }

    @Test
    void review_reject_blankPreviousStatus_fallsBackToPaid() {
        RefundRequestDO pending = pendingRequest("O1", null);
        when(refundRequestDAO.findPendingByOrderNumber("O1")).thenReturn(pending);
        when(refundRequestDAO.reviewWithGuard(eq("r1"), eq(RefundRequestDO.STATUS_REJECTED), any())).thenReturn(1);
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_REFUNDING.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(orderDAO.updateStatusWithGuard(anyString(), anyString(), anyString())).thenReturn(1);

        refundService.review("O1", false, null);

        verify(orderDAO).updateStatusWithGuard("O1",
                OrderStatus.TRADE_REFUNDING.name(), OrderStatus.TRADE_PAID_SUCCESS.name());
    }

    @Test
    void review_alreadyReviewed_throws() {
        RefundRequestDO pending = pendingRequest("O1", OrderStatus.TRADE_PAID_SUCCESS.name());
        when(refundRequestDAO.findPendingByOrderNumber("O1")).thenReturn(pending);
        when(refundRequestDAO.reviewWithGuard(anyString(), anyString(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.review("O1", true, null));
        assertEquals("该申请已被审核，请刷新后重试", ex.getMessage());
    }

    @Test
    void review_approve_orderStatusChanged_throws() {
        RefundRequestDO pending = pendingRequest("O1", OrderStatus.TRADE_PAID_SUCCESS.name());
        when(refundRequestDAO.findPendingByOrderNumber("O1")).thenReturn(pending);
        when(refundRequestDAO.reviewWithGuard(anyString(), anyString(), any())).thenReturn(1);
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_REFUNDING.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(orderDAO.updateStatusWithGuard(anyString(), anyString(), anyString())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.review("O1", true, null));
        assertEquals("订单状态已变化，无法完成退款", ex.getMessage());
        verify(productDetailService, never()).increaseStock(anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void review_noPending_fallbackChannel_approveRestoresStock() {
        when(refundRequestDAO.findPendingByOrderNumber("O1")).thenReturn(null);
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_REFUNDING.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(orderDAO.updateStatusWithGuard("O1", OrderStatus.TRADE_REFUNDING.name(),
                OrderStatus.TRADE_REFUNDED.name())).thenReturn(1);
        ProductDetail detail = new ProductDetail();
        detail.setId("pd1");
        detail.setStock(0);
        when(productDetailService.findById("pd1")).thenReturn(detail);

        refundService.review("O1", true, null);

        verify(orderDAO).updateStatusWithGuard("O1",
                OrderStatus.TRADE_REFUNDING.name(), OrderStatus.TRADE_REFUNDED.name());
        verify(productDetailService).increaseStock("pd1", 2);
        verify(stockLogService).record("pd1", null, "O1",
                StockLogService.TYPE_REFUND_RESTORE, 2, 0, 2);
    }

    @Test
    void review_noPending_orderNotRefunding_throws() {
        when(refundRequestDAO.findPendingByOrderNumber("O1")).thenReturn(null);
        OrderDO od = order("O1", 1L, OrderStatus.TRADE_PAID_SUCCESS.name());
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.review("O1", true, null));
        assertEquals("该订单没有待审核的退款申请", ex.getMessage());
    }

    @Test
    void review_noPending_orderNotFound_throws() {
        when(refundRequestDAO.findPendingByOrderNumber("O1")).thenReturn(null);
        when(orderDAO.findByOrderNumber("O1")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.review("O1", false, null));
        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    void review_commentTooLong_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.review("O1", false, "长".repeat(201)));
        assertEquals("审核意见不能超过 200 字", ex.getMessage());
        verify(refundRequestDAO, never()).reviewWithGuard(anyString(), anyString(), anyString());
    }

    @Test
    void review_blankOrderNumber_throws() {
        assertThrows(IllegalArgumentException.class, () -> refundService.review("", true, null));
    }

    // ========== 查询 ==========

    @Test
    void listByUser_mapsToModel() {
        RefundRequestDO req = pendingRequest("O1", OrderStatus.TRADE_PAID_SUCCESS.name());
        when(refundRequestDAO.findByUserId(1L)).thenReturn(Collections.singletonList(req));

        List<RefundRequest> list = refundService.listByUser(1L);

        assertEquals(1, list.size());
        assertEquals("r1", list.get(0).getId());
        assertEquals("O1", list.get(0).getOrderNumber());
    }

    @Test
    void listByUser_nullUserId_throws() {
        assertThrows(BusinessException.class, () -> refundService.listByUser(null));
    }

    @Test
    void listByOrder_mapsHistory() {
        RefundRequestDO req = pendingRequest("O1", OrderStatus.TRADE_PAID_SUCCESS.name());
        req.setStatus(RefundRequestDO.STATUS_REJECTED);
        req.setReviewComment("驳回原因");
        when(refundRequestDAO.findByOrderNumber("O1")).thenReturn(Collections.singletonList(req));

        List<RefundRequest> list = refundService.listByOrder("O1");

        assertEquals(1, list.size());
        assertEquals(RefundRequestDO.STATUS_REJECTED, list.get(0).getStatus());
        assertEquals("驳回原因", list.get(0).getReviewComment());
    }

    @Test
    void listByOrder_blankOrderNumber_throws() {
        assertThrows(IllegalArgumentException.class, () -> refundService.listByOrder(" "));
    }
}
