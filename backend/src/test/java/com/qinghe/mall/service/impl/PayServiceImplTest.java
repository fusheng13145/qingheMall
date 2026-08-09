package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson2.JSONObject;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.PaymentStatus;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.AlipayClient;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.PayChannel;
import com.qinghe.mall.service.PaymentRecordService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 支付服务 单元测试（P0-6 补齐：资金安全核心逻辑）。
 *
 * 覆盖：支付宝/微信回调验签、金额强制核对（P0-2）、支付成功落库幂等（P0-1 事务边界）、
 * mockPay 幂等与越权、生产环境模拟支付禁用。
 */
class PayServiceImplTest {

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentRecordService paymentRecordService;

    @Mock
    private ProductDAO productDAO;

    @Mock
    private WechatNativePayChannel wechatChannel;

    @Mock
    private AlipayPayChannel alipayChannel;

    @Mock
    private AlipayClient alipayClient;

    @InjectMocks
    private PayServiceImpl payService;

    private static final String ORDER_NUMBER = "O202608090001";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 事务模板：直接执行回调（与 OrderServiceImplTest 一致，模拟事务提交）
        ReflectionTestUtils.setField(payService, "transactionTemplate", new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(null);
            }
        });
        // 默认开启模拟支付（dev 语义）
        ReflectionTestUtils.setField(payService, "mockEnabled", true);
        // 真实支付渠道默认不可用（避免测试依赖外部凭证）
        when(alipayChannel.enabled()).thenReturn(false);
        when(wechatChannel.enabled()).thenReturn(false);
        // 模拟支付通道
        PayChannel mockChannel = mock(PayChannel.class);
        when(mockChannel.code()).thenReturn("MOCK");
        when(mockChannel.enabled()).thenReturn(true);
        when(mockChannel.createPay(any())).thenReturn(new ChannelPayResult());
        ReflectionTestUtils.setField(payService, "payChannels", Collections.singletonList(mockChannel));
    }

    private Order mockOrder(OrderStatus status, String totalPrice) {
        Order order = mock(Order.class);
        when(order.getUserId()).thenReturn(1L);
        when(order.getStatus()).thenReturn(status);
        when(order.getTotalPrice()).thenReturn(new BigDecimal(totalPrice));
        when(order.getQuantity()).thenReturn(1);
        ProductDetail detail = mock(ProductDetail.class);
        when(detail.getProductId()).thenReturn("p001");
        when(order.getProductDetail()).thenReturn(detail);
        return order;
    }

    private Map<String, String> alipayParams(String outTradeNo, String tradeStatus, String totalAmount) {
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", outTradeNo);
        params.put("trade_status", tradeStatus);
        params.put("trade_no", "ALI_TRADE_001");
        if (totalAmount != null) {
            params.put("total_amount", totalAmount);
        }
        return params;
    }

    // ==================== 支付宝回调 ====================

    @Test
    void alipayNotify_success_marksPaid() {
        when(alipayClient.verifyNotify(any())).thenReturn(true);
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);
        when(orderService.updateStatusIfWaitPay(eq(ORDER_NUMBER), eq(OrderStatus.TRADE_PAID_SUCCESS.name()))).thenReturn(true);

        Result<String> result = payService.handleAlipayNotify(alipayParams(ORDER_NUMBER, "TRADE_SUCCESS", "199.00"));

        assertTrue(result.isSuccess());
        verify(paymentRecordService).updatePayStatus(eq(ORDER_NUMBER), eq(PaymentStatus.SUCCESS.name()), anyString());
        verify(productDAO).increasePurchaseNum("p001", 1);
    }

    @Test
    void alipayNotify_verifyFail_rejected() {
        when(alipayClient.verifyNotify(any())).thenReturn(false);

        Result<String> result = payService.handleAlipayNotify(alipayParams(ORDER_NUMBER, "TRADE_SUCCESS", "199.00"));

        assertFalse(result.isSuccess());
        assertEquals("签名验证失败", result.getMessage());
        verify(orderService, never()).updateStatusIfWaitPay(anyString(), anyString());
    }

    @Test
    void alipayNotify_missingOrderNumber_rejected() {
        when(alipayClient.verifyNotify(any())).thenReturn(true);

        Result<String> result = payService.handleAlipayNotify(alipayParams("", "TRADE_SUCCESS", "199.00"));

        assertFalse(result.isSuccess());
        assertEquals("回调缺少订单号", result.getMessage());
    }

    @Test
    void alipayNotify_nonSuccessStatus_ignored() {
        when(alipayClient.verifyNotify(any())).thenReturn(true);

        Result<String> result = payService.handleAlipayNotify(alipayParams(ORDER_NUMBER, "TRADE_WAIT", "199.00"));

        assertTrue(result.isSuccess());
        assertEquals("已忽略", result.getData());
        verify(orderService, never()).updateStatusIfWaitPay(anyString(), anyString());
    }

    @Test
    void alipayNotify_missingAmount_rejected() {
        // P0-2：金额缺失必须拒绝，不再跳过核对
        when(alipayClient.verifyNotify(any())).thenReturn(true);
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);

        Result<String> result = payService.handleAlipayNotify(alipayParams(ORDER_NUMBER, "TRADE_SUCCESS", null));

        assertFalse(result.isSuccess());
        assertEquals("回调缺少金额字段", result.getMessage());
        verify(orderService, never()).updateStatusIfWaitPay(anyString(), anyString());
    }

    @Test
    void alipayNotify_amountMismatch_rejected() {
        when(alipayClient.verifyNotify(any())).thenReturn(true);
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);

        Result<String> result = payService.handleAlipayNotify(alipayParams(ORDER_NUMBER, "TRADE_SUCCESS", "0.01"));

        assertFalse(result.isSuccess());
        assertEquals("回调金额不一致", result.getMessage());
        verify(orderService, never()).updateStatusIfWaitPay(anyString(), anyString());
    }

    @Test
    void alipayNotify_orderNotFound_rejected() {
        when(alipayClient.verifyNotify(any())).thenReturn(true);
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(null);

        Result<String> result = payService.handleAlipayNotify(alipayParams(ORDER_NUMBER, "TRADE_SUCCESS", "199.00"));

        assertFalse(result.isSuccess());
        assertEquals("订单不存在", result.getMessage());
    }

    @Test
    void alipayNotify_duplicateCallback_idempotent() {
        // 重复回调：updateStatusIfWaitPay 返回 false → 不再更新流水/销量
        when(alipayClient.verifyNotify(any())).thenReturn(true);
        Order order = mockOrder(OrderStatus.TRADE_PAID_SUCCESS, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);
        when(orderService.updateStatusIfWaitPay(eq(ORDER_NUMBER), eq(OrderStatus.TRADE_PAID_SUCCESS.name()))).thenReturn(false);

        Result<String> result = payService.handleAlipayNotify(alipayParams(ORDER_NUMBER, "TRADE_SUCCESS", "199.00"));

        assertTrue(result.isSuccess());
        verify(paymentRecordService, never()).updatePayStatus(anyString(), anyString(), anyString());
        verify(productDAO, never()).increasePurchaseNum(anyString(), any());
    }

    // ==================== 微信回调 ====================

    private JSONObject wechatBiz(String outTradeNo, String tradeState, Long totalFen) {
        JSONObject biz = new JSONObject();
        biz.put("out_trade_no", outTradeNo);
        biz.put("transaction_id", "WX_TRADE_001");
        biz.put("trade_state", tradeState);
        if (totalFen != null) {
            JSONObject amount = new JSONObject();
            amount.put("total", totalFen);
            biz.put("amount", amount);
        } else {
            biz.put("amount", new JSONObject());
        }
        return biz;
    }

    @Test
    void wechatNotify_success_marksPaid() {
        when(wechatChannel.parseNotify(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(wechatBiz(ORDER_NUMBER, "SUCCESS", 19900L));
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);
        when(orderService.updateStatusIfWaitPay(eq(ORDER_NUMBER), eq(OrderStatus.TRADE_PAID_SUCCESS.name()))).thenReturn(true);

        Result<String> result = payService.handleWechatNotify("serial", "ts", "nonce", "sig", "{}");

        assertTrue(result.isSuccess());
        assertEquals("处理成功", result.getData());
        verify(paymentRecordService).updatePayStatus(eq(ORDER_NUMBER), eq(PaymentStatus.SUCCESS.name()), anyString());
        verify(productDAO).increasePurchaseNum("p001", 1);
    }

    @Test
    void wechatNotify_missingAmount_rejected() {
        // P0-2：金额缺失必须拒绝（amount.total 为 null）
        when(wechatChannel.parseNotify(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(wechatBiz(ORDER_NUMBER, "SUCCESS", null));
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);

        Result<String> result = payService.handleWechatNotify("serial", "ts", "nonce", "sig", "{}");

        assertFalse(result.isSuccess());
        assertEquals("回调缺少金额字段", result.getMessage());
        verify(orderService, never()).updateStatusIfWaitPay(anyString(), anyString());
    }

    @Test
    void wechatNotify_amountMismatch_rejected() {
        when(wechatChannel.parseNotify(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(wechatBiz(ORDER_NUMBER, "SUCCESS", 1L));
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);

        Result<String> result = payService.handleWechatNotify("serial", "ts", "nonce", "sig", "{}");

        assertFalse(result.isSuccess());
        assertEquals("回调金额不一致", result.getMessage());
        verify(orderService, never()).updateStatusIfWaitPay(anyString(), anyString());
    }

    @Test
    void wechatNotify_verifyFail_rejected() {
        // parseNotify 抛 IllegalArgumentException（验签失败/参数不完整）→ 回调校验失败
        when(wechatChannel.parseNotify(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("微信回调签名验证失败"));

        Result<String> result = payService.handleWechatNotify("serial", "ts", "nonce", "sig", "{}");

        assertFalse(result.isSuccess());
        assertEquals("回调校验失败", result.getMessage());
        verify(orderService, never()).updateStatusIfWaitPay(anyString(), anyString());
    }

    @Test
    void wechatNotify_nonSuccessStatus_ignored() {
        when(wechatChannel.parseNotify(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(wechatBiz(ORDER_NUMBER, "CLOSED", 19900L));

        Result<String> result = payService.handleWechatNotify("serial", "ts", "nonce", "sig", "{}");

        assertTrue(result.isSuccess());
        assertEquals("已忽略", result.getData());
        verify(orderService, never()).updateStatusIfWaitPay(anyString(), anyString());
    }

    // ==================== mockPay ====================

    @Test
    void mockPay_success_marksPaidAndIncreaseSales() {
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);
        when(orderService.updateStatusIfWaitPay(eq(ORDER_NUMBER), eq(OrderStatus.TRADE_PAID_SUCCESS.name()))).thenReturn(true);

        Result<String> result = payService.mockPay(1L, ORDER_NUMBER);

        assertTrue(result.isSuccess());
        assertEquals("支付成功", result.getData());
        verify(paymentRecordService).updatePayStatus(eq(ORDER_NUMBER), eq(PaymentStatus.SUCCESS.name()), anyString());
        verify(productDAO).increasePurchaseNum("p001", 1);
    }

    @Test
    void mockPay_orderNotWaitPay_rejected() {
        Order order = mockOrder(OrderStatus.TRADE_PAID_SUCCESS, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);

        Result<String> result = payService.mockPay(1L, ORDER_NUMBER);

        assertFalse(result.isSuccess());
        assertEquals("订单状态异常，无法支付", result.getMessage());
        verify(orderService, never()).updateStatusIfWaitPay(anyString(), anyString());
    }

    @Test
    void mockPay_unauthorized_rejected() {
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(order.getUserId()).thenReturn(2L);
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);

        Result<String> result = payService.mockPay(1L, ORDER_NUMBER);

        assertFalse(result.isSuccess());
        assertEquals("无权操作此订单", result.getMessage());
    }

    @Test
    void mockPay_orderNotFound_rejected() {
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(null);

        Result<String> result = payService.mockPay(1L, ORDER_NUMBER);

        assertFalse(result.isSuccess());
        assertEquals("订单不存在", result.getMessage());
    }

    @Test
    void mockPay_casFail_rejected() {
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);
        when(orderService.updateStatusIfWaitPay(anyString(), anyString())).thenReturn(false);

        Result<String> result = payService.mockPay(1L, ORDER_NUMBER);

        assertFalse(result.isSuccess());
        assertEquals("订单状态已变化，请刷新后重试", result.getMessage());
        verify(productDAO, never()).increasePurchaseNum(anyString(), any());
    }

    @Test
    void mockPay_disabledInProd_rejected() {
        // 生产禁模拟：mockEnabled=false
        ReflectionTestUtils.setField(payService, "mockEnabled", false);

        Result<String> result = payService.mockPay(1L, ORDER_NUMBER);

        assertFalse(result.isSuccess());
        assertEquals("模拟支付已禁用（生产环境请配置真实支付渠道）", result.getMessage());
    }

    // ==================== createPay ====================

    @Test
    void createPay_prodForbidMock_rejected() {
        // 生产禁模拟且无真实渠道 → 拒绝创建支付
        ReflectionTestUtils.setField(payService, "mockEnabled", false);
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);

        Result<ChannelPayResult> result = payService.createPay(1L, ORDER_NUMBER, "ALIPAY");

        assertFalse(result.isSuccess());
        assertEquals("未配置真实支付渠道（生产环境模拟支付已禁用）", result.getMessage());
    }

    @Test
    void createPay_orderNotWaitPay_rejected() {
        Order order = mockOrder(OrderStatus.TRADE_CLOSED, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);

        Result<ChannelPayResult> result = payService.createPay(1L, ORDER_NUMBER, "ALIPAY");

        assertFalse(result.isSuccess());
        assertEquals("订单状态异常，无法支付", result.getMessage());
    }

    @Test
    void createPay_unauthorized_rejected() {
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(order.getUserId()).thenReturn(2L);
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);

        Result<ChannelPayResult> result = payService.createPay(1L, ORDER_NUMBER, "ALIPAY");

        assertFalse(result.isSuccess());
        assertEquals("无权操作此订单", result.getMessage());
    }

    @Test
    void createPay_success_recordsPayingRecord() {
        Order order = mockOrder(OrderStatus.WAIT_BUYER_PAY, "199.00");
        when(orderService.findByOrderNumber(ORDER_NUMBER)).thenReturn(order);

        Result<ChannelPayResult> result = payService.createPay(1L, ORDER_NUMBER, "ALIPAY");

        assertTrue(result.isSuccess());
        verify(paymentRecordService).insert(any());
    }
}
