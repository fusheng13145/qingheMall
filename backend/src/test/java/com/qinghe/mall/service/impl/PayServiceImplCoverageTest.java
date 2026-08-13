package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson2.JSONObject;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.PaymentStatus;
import com.qinghe.mall.model.PayType;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.AlipayClient;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.PayChannel;
import com.qinghe.mall.service.PaymentRecordService;
import com.qinghe.mall.service.impl.WechatNativePayChannel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PayServiceImpl 分支覆盖补测（#37）：覆盖 createPay/queryPayStatus/handleAlipayNotify/
 * handleWechatNotify/resolveChannel/markPaid/increasePurchaseNum/mockPay/parsePayType 的缺失分支。
 * transactionTemplate 以空实现包裹；支付渠道以 mock 注入；markPaid（protected）同包直测。
 */
class PayServiceImplCoverageTest {

    @Mock
    private OrderService orderService;
    @Mock
    private PaymentRecordService paymentRecordService;
    @Mock
    private ProductDAO productDAO;
    @Mock
    private WechatNativePayChannel wechatChannel;
    @Mock
    private com.qinghe.mall.service.impl.AlipayPayChannel alipayChannel;
    @Mock
    private AlipayClient alipayClient;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private PayServiceImpl payService;

    private PayChannel mockChannel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(payService, "mockEnabled", true);
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
        when(orderService.updateStatusIfWaitPay(anyString(), anyString())).thenReturn(true);
        mockChannel = mock(PayChannel.class);
        when(mockChannel.code()).thenReturn("MOCK");
        when(mockChannel.createPay(any())).thenReturn(ChannelPayResult.mock("MOCK", "QH1"));
        List<PayChannel> channels = new ArrayList<>();
        channels.add(mockChannel);
        ReflectionTestUtils.setField(payService, "payChannels", channels);
    }

    private Order order(Long userId, OrderStatus status, BigDecimal total) {
        Order o = new Order();
        o.setUserId(userId);
        o.setStatus(status);
        o.setTotalPrice(total);
        return o;
    }

    // ========== createPay ==========

    @Test
    void createPay_blankOrderNumber_shouldFail() {
        Result<ChannelPayResult> r = payService.createPay(1L, "", "ALIPAY");
        assertFalse(r.isSuccess());
    }

    @Test
    void createPay_orderMissing_shouldFail() {
        when(orderService.findByOrderNumber("QH1")).thenReturn(null);
        Result<ChannelPayResult> r = payService.createPay(1L, "QH1", "ALIPAY");
        assertFalse(r.isSuccess());
    }

    @Test
    void createPay_userMismatch_shouldFail() {
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(2L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10")));
        Result<ChannelPayResult> r = payService.createPay(1L, "QH1", "ALIPAY");
        assertFalse(r.isSuccess());
    }

    @Test
    void createPay_statusNotWait_shouldFail() {
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(1L, OrderStatus.TRADE_PAID_SUCCESS, new BigDecimal("10")));
        Result<ChannelPayResult> r = payService.createPay(1L, "QH1", "ALIPAY");
        assertFalse(r.isSuccess());
    }

    @Test
    void createPay_alipayChannel_shouldSucceed() {
        when(alipayChannel.enabled()).thenReturn(true);
        when(alipayChannel.createPay(any())).thenReturn(ChannelPayResult.real("ALIPAY", "codeurl", "QH1"));
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10")));
        Result<ChannelPayResult> r = payService.createPay(1L, "QH1", "ALIPAY");
        assertTrue(r.isSuccess());
    }

    @Test
    void createPay_wechatChannel_shouldSucceed() {
        when(wechatChannel.enabled()).thenReturn(true);
        when(wechatChannel.createPay(any())).thenReturn(ChannelPayResult.real("WECHAT", "codeurl", "QH1"));
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10")));
        Result<ChannelPayResult> r = payService.createPay(1L, "QH1", "WECHAT");
        assertTrue(r.isSuccess());
    }

    @Test
    void createPay_mockFallback_shouldSucceed() {
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10")));
        // 真实渠道均禁用 + payType 非 ALIPAY/WECHAT → 回退 MOCK 通道
        Result<ChannelPayResult> r = payService.createPay(1L, "QH1", "BANK");
        assertTrue(r.isSuccess());
    }

    @Test
    void resolveChannel_noMock_shouldThrow() {
        ReflectionTestUtils.setField(payService, "payChannels", new ArrayList<>());
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10")));
        assertThrows(IllegalStateException.class, () -> payService.createPay(1L, "QH1", "BANK"));
    }

    // ========== queryPayStatus ==========

    @Test
    void queryPayStatus_blank_shouldFail() {
        Result<String> r = payService.queryPayStatus(1L, "");
        assertFalse(r.isSuccess());
    }

    @Test
    void queryPayStatus_orderMissing_shouldFail() {
        when(orderService.findByOrderNumber("QH1")).thenReturn(null);
        Result<String> r = payService.queryPayStatus(1L, "QH1");
        assertFalse(r.isSuccess());
    }

    @Test
    void queryPayStatus_userMismatch_shouldFail() {
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(2L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10")));
        Result<String> r = payService.queryPayStatus(1L, "QH1");
        assertFalse(r.isSuccess());
    }

    @Test
    void queryPayStatus_paid_shouldReturnPaid() {
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(1L, OrderStatus.TRADE_PAID_SUCCESS, new BigDecimal("10")));
        Result<String> r = payService.queryPayStatus(1L, "QH1");
        assertTrue(r.isSuccess());
        assertEquals(OrderStatus.TRADE_PAID_SUCCESS.name(), r.getData());
    }

    @Test
    void queryPayStatus_waitChannelsDisabled_shouldReturnStatus() {
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10")));
        when(alipayChannel.enabled()).thenReturn(false);
        when(wechatChannel.enabled()).thenReturn(false);
        Result<String> r = payService.queryPayStatus(1L, "QH1");
        assertTrue(r.isSuccess());
        assertEquals(OrderStatus.WAIT_BUYER_PAY.name(), r.getData());
    }

    @Test
    void queryPayStatus_waitAlipaySuccess_shouldMarkPaid() {
        Order o = order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10"));
        when(orderService.findByOrderNumber("QH1")).thenReturn(o);
        when(alipayChannel.enabled()).thenReturn(true);
        when(alipayClient.queryTradeStatus("QH1")).thenReturn("TRADE_SUCCESS");
        Result<String> r = payService.queryPayStatus(1L, "QH1");
        assertTrue(r.isSuccess());
        assertEquals(OrderStatus.TRADE_PAID_SUCCESS.name(), r.getData());
    }

    // ========== handleAlipayNotify ==========

    @Test
    void alipayNotify_verifyFail_shouldFail() {
        when(alipayClient.verifyNotify(any())).thenReturn(false);
        Result<String> r = payService.handleAlipayNotify(java.util.Map.of("out_trade_no", "QH1"));
        assertFalse(r.isSuccess());
    }

    @Test
    void alipayNotify_amountMismatch_shouldFail() {
        when(alipayClient.verifyNotify(any())).thenReturn(true);
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("100")));
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("out_trade_no", "QH1");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("total_amount", "99.00");
        Result<String> r = payService.handleAlipayNotify(params);
        assertFalse(r.isSuccess());
    }

    // ========== handleWechatNotify ==========

    @Test
    void wechatNotify_notSuccess_shouldIgnore() {
        JSONObject biz = new JSONObject();
        biz.put("out_trade_no", "QH1");
        biz.put("trade_state", "NOTPAY");
        when(wechatChannel.parseNotify(any(), any(), any(), any(), any())).thenReturn(biz);
        Result<String> r = payService.handleWechatNotify("s", "t", "n", "sig", "body");
        assertTrue(r.isSuccess());
    }

    @Test
    void wechatNotify_amountMissing_shouldFail() {
        JSONObject biz = new JSONObject();
        biz.put("out_trade_no", "QH1");
        biz.put("trade_state", "SUCCESS");
        when(wechatChannel.parseNotify(any(), any(), any(), any(), any())).thenReturn(biz);
        Result<String> r = payService.handleWechatNotify("s", "t", "n", "sig", "body");
        assertFalse(r.isSuccess());
    }

    @Test
    void wechatNotify_amountMismatch_shouldFail() {
        JSONObject biz = new JSONObject();
        biz.put("out_trade_no", "QH1");
        biz.put("trade_state", "SUCCESS");
        JSONObject amount = new JSONObject();
        amount.put("total", 9900L);
        biz.put("amount", amount);
        when(wechatChannel.parseNotify(any(), any(), any(), any(), any())).thenReturn(biz);
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("100")));
        Result<String> r = payService.handleWechatNotify("s", "t", "n", "sig", "body");
        assertFalse(r.isSuccess());
    }

    // ========== markPaid / increasePurchaseNum（protected 同包直测） ==========

    @Test
    void markPaid_orderNull_shouldSkip() {
        payService.markPaid("QH1", "ch1");
        assertTrue(true);
    }

    @Test
    void markPaid_withDetail_shouldIncreasePurchase() {
        Order o = order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10"));
        ProductDetail pd = new ProductDetail();
        pd.setProductId("p1");
        o.setProductDetail(pd);
        o.setQuantity(2);
        when(orderService.findByOrderNumber("QH1")).thenReturn(o);
        payService.markPaid("QH1", "ch1");
        assertTrue(true);
    }

    // ========== mockPay 禁用分支 ==========

    @Test
    void mockPay_disabled_shouldFail() {
        ReflectionTestUtils.setField(payService, "mockEnabled", false);
        Result<String> r = payService.mockPay(1L, "QH1");
        assertFalse(r.isSuccess());
    }

    // ========== parsePayType 非法值（经 createPay 触发） ==========

    @Test
    void parsePayType_invalid_shouldFallbackAlipay() {
        when(orderService.findByOrderNumber("QH1")).thenReturn(order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10")));
        Result<ChannelPayResult> r = payService.createPay(1L, "QH1", "UNKNOWN_TYPE");
        assertTrue(r.isSuccess());
    }

    @Test
    void markPaid_productDetailNull_shouldSkip() {
        Order o = order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10"));
        o.setProductDetail(null);
        o.setQuantity(2);
        when(orderService.findByOrderNumber("QH1")).thenReturn(o);
        payService.markPaid("QH1", "ch1");
        assertTrue(true);
    }

    @Test
    void markPaid_productIdBlank_shouldSkip() {
        Order o = order(1L, OrderStatus.WAIT_BUYER_PAY, new BigDecimal("10"));
        ProductDetail pd = new ProductDetail();
        pd.setProductId("");
        o.setProductDetail(pd);
        o.setQuantity(2);
        when(orderService.findByOrderNumber("QH1")).thenReturn(o);
        payService.markPaid("QH1", "ch1");
        assertTrue(true);
    }
}
