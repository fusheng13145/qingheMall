package com.qinghe.mall.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dao.RefundRequestDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.StockLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 购物车级优惠券（v1.8）退款释放守卫单元测试。
 *
 * 口径：退款审核通过时，仅当本单是最后一张持券在途单（同用户同券无其它未终态订单）才归还优惠券；
 * 多单共享一张券时部分退款不还整券，防止其余订单的优惠分摊失去依据。
 */
class RefundCouponReleaseGuardTest {

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
        org.springframework.test.util.ReflectionTestUtils.setField(refundService, "transactionTemplate",
                new TransactionTemplate() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> T execute(TransactionCallback<T> action) {
                        return action.doInTransaction(null);
                    }
                });
    }

    private OrderDO refundingOrderWithCoupon() {
        OrderDO order = new OrderDO();
        order.setOrderNumber("QH100");
        order.setUserId(9L);
        order.setStatus(OrderStatus.TRADE_REFUNDING.name());
        order.setProductDetailId("pd001");
        order.setQuantity(1);
        order.setCouponId("uc1");
        return order;
    }

    @Test
    @DisplayName("部分退款：同券仍有其它在途订单时不归还优惠券")
    void review_partialRefund_couponNotReleased() {
        OrderDO order = refundingOrderWithCoupon();
        when(refundRequestDAO.findPendingByOrderNumber("QH100")).thenReturn(null);
        when(orderDAO.findByOrderNumber("QH100")).thenReturn(order);
        when(orderDAO.updateStatusWithGuard("QH100",
                OrderStatus.TRADE_REFUNDING.name(), OrderStatus.TRADE_REFUNDED.name())).thenReturn(1);
        when(productDetailService.increaseStock("pd001", 1)).thenReturn(true);
        // 另一单仍持有同券
        when(orderDAO.countActiveByCouponExcluding("uc1", 9L, "QH100")).thenReturn(1);

        refundService.review("QH100", true, null);

        verify(couponService, never()).releaseCoupon("uc1");
        // 分账冲销仍按本单执行（该单实付已含分摊优惠）
        verify(settlementService).recordReversal(order);
    }

    @Test
    @DisplayName("最后一张持券单退款：归还优惠券")
    void review_lastCouponHolder_couponReleased() {
        OrderDO order = refundingOrderWithCoupon();
        when(refundRequestDAO.findPendingByOrderNumber("QH100")).thenReturn(null);
        when(orderDAO.findByOrderNumber("QH100")).thenReturn(order);
        when(orderDAO.updateStatusWithGuard("QH100",
                OrderStatus.TRADE_REFUNDING.name(), OrderStatus.TRADE_REFUNDED.name())).thenReturn(1);
        when(productDetailService.increaseStock("pd001", 1)).thenReturn(true);
        when(orderDAO.countActiveByCouponExcluding("uc1", 9L, "QH100")).thenReturn(0);

        refundService.review("QH100", true, null);

        verify(couponService).releaseCoupon("uc1");
    }

    @Test
    @DisplayName("无券订单退款：不触碰券释放")
    void review_noCoupon_noReleaseInteraction() {
        OrderDO order = refundingOrderWithCoupon();
        order.setCouponId(null);
        when(refundRequestDAO.findPendingByOrderNumber("QH100")).thenReturn(null);
        when(orderDAO.findByOrderNumber("QH100")).thenReturn(order);
        when(orderDAO.updateStatusWithGuard(anyString(), anyString(), anyString())).thenReturn(1);
        when(productDetailService.increaseStock(anyString(), anyInt())).thenReturn(true);

        refundService.review("QH100", true, null);

        verify(couponService, never()).releaseCoupon(anyString());
        verify(orderDAO, never()).countActiveByCouponExcluding(anyString(), any(), anyString());
    }
}
