package com.qinghe.mall.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 微信支付 v3 配置（M2-9 真实支付）。
 *
 * 全部通过环境变量注入，未配置时（默认）渠道自动禁用，支付回退模拟通道。
 * 申请指引见 docs/微信支付接入.md。
 */
@Component
public class WechatPayProperties {

    /** 小程序/公众号/开放平台 AppID（Native 支付必需） */
    @Value("${wechat.pay.appid:}")
    private String appid;

    /** 微信支付商户号 */
    @Value("${wechat.pay.mchid:}")
    private String mchid;

    /** APIv3 密钥（商户平台设置，32 位） */
    @Value("${wechat.pay.api-v3-key:}")
    private String apiV3Key;

    /** 商户 API 私钥文件路径（apiclient_key.pem，PKCS8） */
    @Value("${wechat.pay.private-key-path:}")
    private String privateKeyPath;

    /** 商户 API 证书序列号 */
    @Value("${wechat.pay.serial-no:}")
    private String serialNo;

    /** 支付回调地址（必须公网 HTTPS 可达，本地联调可用内网穿透） */
    @Value("${wechat.pay.notify-url:}")
    private String notifyUrl;

    /** 微信支付平台证书文件路径（可选，配置后启用回调签名验证，更安全） */
    @Value("${wechat.pay.platform-cert-path:}")
    private String platformCertPath;

    /** 微信支付 API 网关（默认生产，测试可指向沙箱网关） */
    @Value("${wechat.pay.api-base:https://api.mch.weixin.qq.com}")
    private String apiBase;

    /** 配置是否齐全（齐全才启用真实渠道） */
    public boolean enabled() {
        return StringUtils.isNotBlank(appid)
                && StringUtils.isNotBlank(mchid)
                && StringUtils.isNotBlank(apiV3Key)
                && StringUtils.isNotBlank(privateKeyPath)
                && StringUtils.isNotBlank(serialNo)
                && StringUtils.isNotBlank(notifyUrl);
    }

    public String getAppid() {
        return appid;
    }

    public String getMchid() {
        return mchid;
    }

    public String getApiV3Key() {
        return apiV3Key;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public String getPlatformCertPath() {
        return platformCertPath;
    }

    public String getApiBase() {
        return apiBase;
    }
}
