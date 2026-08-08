package com.qinghe.mall.service.impl;

import com.qinghe.mall.config.AlipayProperties;
import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.service.AlipayClient;
import com.qinghe.mall.service.PayChannel;
import com.qinghe.mall.util.QrCodeUtil;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 支付宝渠道（M2-9 真实支付）。
 *
 * 当面付-预下单（alipay.trade.precreate）→ 返回收款二维码 → 前端渲染 →
 * 用户支付宝 App 扫码支付 → 异步通知 notify_url → 验签 + 幂等落库。
 *
 * 仅在 AlipayProperties 配置齐全时启用；未配置时支付回退模拟通道。
 */
@Component
public class AlipayPayChannel implements PayChannel {

    @Autowired
    private AlipayProperties properties;

    @Autowired
    private AlipayClient alipayClient;

    @Override
    public String code() {
        return "ALIPAY";
    }

    @Override
    public boolean enabled() {
        return properties.enabled();
    }

    @Override
    public ChannelPayResult createPay(Order order) {
        if (!enabled()) {
            throw new IllegalStateException("支付宝未配置，请先完成应用配置");
        }
        String totalAmount = order.getTotalPrice() == null
                ? "0.00"
                : order.getTotalPrice().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String subject = order.getProductName() != null ? order.getProductName() : "青禾商城订单";
        String qrCode = alipayClient.precreate(order.getOrderNumber(), totalAmount, subject);
        // 返回二维码 Base64 图片，前端直接展示
        return ChannelPayResult.real("ALIPAY", "data:image/png;base64," + QrCodeUtil.toBase64Png(qrCode), order.getOrderNumber());
    }
}
