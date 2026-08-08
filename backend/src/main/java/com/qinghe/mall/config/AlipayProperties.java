package com.qinghe.mall.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 支付宝（沙箱/正式）配置（M2-9 真实支付，优先级高于微信）。
 *
 * 全部通过环境变量注入，未配置时渠道自动禁用，支付回退模拟通道。
 * 申请指引见 docs/支付宝沙箱接入.md。
 */
@Component
public class AlipayProperties {

    /** 应用 AppID（沙箱/正式） */
    @Value("${alipay.app-id:}")
    private String appId;

    /**
     * 应用私钥：支持 PEM 内容（以 -----BEGIN 开头）或文件路径。
     * RSA2（SHA256withRSA），PKCS1 / PKCS8 均可。
     */
    @Value("${alipay.private-key:}")
    private String privateKey;

    /**
     * 支付宝公钥：PEM 内容或文件路径（用于响应/回调验签）。
     */
    @Value("${alipay.alipay-public-key:}")
    private String alipayPublicKey;

    /** 异步回调地址（必须公网 HTTPS 可达，本地联调可用内网穿透） */
    @Value("${alipay.notify-url:}")
    private String notifyUrl;

    /**
     * 网关：沙箱 https://openapi.alipaydev.com/gateway.do
     *      正式 https://openapi.alipay.com/gateway.do
     */
    @Value("${alipay.gateway:https://openapi.alipaydev.com/gateway.do}")
    private String gateway;

    /** 配置是否齐全（齐全才启用真实支付宝渠道） */
    public boolean enabled() {
        return StringUtils.isNotBlank(appId)
                && StringUtils.isNotBlank(privateKey)
                && StringUtils.isNotBlank(alipayPublicKey)
                && StringUtils.isNotBlank(notifyUrl);
    }

    public String getAppId() {
        return appId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public String getGateway() {
        return gateway;
    }
}
