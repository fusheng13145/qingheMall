package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.qinghe.mall.config.AlipayProperties;
import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.service.AlipayClient;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 支付宝渠道 单元测试（T3 分支覆盖补测）。
 * 覆盖 createPay 全部分支：!enabled 抛异常、金额为 null 取 0.00、商品名为 null 取默认值、
 * 金额/商品名非空正常下单（验证 Base64 二维码前缀）。
 */
@ExtendWith(MockitoExtension.class)
class AlipayPayChannelTest {

    @Mock
    private AlipayProperties properties;

    @Mock
    private AlipayClient alipayClient;

    private AlipayPayChannel channel;

    @BeforeEach
    void setUp() {
        channel = new AlipayPayChannel();
        ReflectionTestUtils.setField(channel, "properties", properties);
        ReflectionTestUtils.setField(channel, "alipayClient", alipayClient);
    }

    private Order order(BigDecimal total, String productName) {
        Order o = new Order();
        o.setOrderNumber("QH202608110001");
        o.setTotalPrice(total);
        o.setProductName(productName);
        return o;
    }

    @Test
    @DisplayName("code 固定为 ALIPAY")
    void code_isAlipay() {
        assertEquals("ALIPAY", channel.code());
    }

    @Test
    @DisplayName("enabled 透传 AlipayProperties.enabled")
    void enabled_passthrough() {
        when(properties.enabled()).thenReturn(true);
        assertTrue(channel.enabled());
        when(properties.enabled()).thenReturn(false);
        assertFalse(channel.enabled());
    }

    @Test
    @DisplayName("未配置真实支付宝时 createPay 抛 IllegalStateException")
    void createPay_disabled_throws() {
        when(properties.enabled()).thenReturn(false);
        assertThrows(IllegalStateException.class,
                () -> channel.createPay(order(new BigDecimal("10.00"), "商品A")),
                "未配置必须抛异常，避免静默走模拟通道");
    }

    @Test
    @DisplayName("金额为 null 取 0.00、商品名为 null 取默认主题")
    void createPay_nullTotalAndNullName_usesDefaults() {
        when(properties.enabled()).thenReturn(true);
        when(alipayClient.precreate(anyString(), anyString(), anyString())).thenReturn("qrcontent");

        ChannelPayResult r = channel.createPay(order(null, null));

        assertEquals("ALIPAY", r.getChannel());
        assertFalse(r.getMock());
        assertTrue(r.getCodeUrl().startsWith("data:image/png;base64,"), "应返回 Base64 二维码");
        assertEquals("QH202608110001", r.getOrderNumber());
    }

    @Test
    @DisplayName("金额非空按两位精度、商品名非空使用给定值")
    void createPay_withTotalAndName() {
        when(properties.enabled()).thenReturn(true);
        when(alipayClient.precreate(anyString(), anyString(), anyString())).thenReturn("qrcontent");

        ChannelPayResult r = channel.createPay(order(new BigDecimal("19.5"), "iPhone 壳"));

        assertEquals("ALIPAY", r.getChannel());
        assertFalse(r.getMock());
        assertTrue(r.getCodeUrl().startsWith("data:image/png;base64,"));
        assertEquals("QH202608110001", r.getOrderNumber());
    }
}
