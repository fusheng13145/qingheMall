package com.qinghe.mall.service;

import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Result;

public interface PayService {

    /**
     * 创建支付：按渠道返回二维码/模拟标记。
     * 渠道未配置时自动回退模拟通道（mock=true）。
     */
    Result<ChannelPayResult> createPay(Long userId, String orderNumber, String payType);

    /**
     * 模拟支付：直接置订单支付成功（仅 mock 通道使用）。
     */
    Result<String> mockPay(Long userId, String orderNumber);

    /**
     * 查询支付状态：返回订单当前状态（前端轮询用）。
     */
    Result<String> queryPayStatus(Long userId, String orderNumber);

    /**
     * 微信异步回调处理：验签/解密后幂等更新订单、流水与销量。
     */
    Result<String> handleWechatNotify(String serial, String timestamp, String nonce, String signature, String body);

    /**
     * 支付宝异步回调处理：验签 + trade_status 判断后幂等更新订单、流水与销量。
     */
    Result<String> handleAlipayNotify(java.util.Map<String, String> params);
}
