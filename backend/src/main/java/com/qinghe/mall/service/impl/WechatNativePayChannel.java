package com.qinghe.mall.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qinghe.mall.config.WechatPayProperties;
import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.service.PayChannel;
import com.qinghe.mall.util.QrCodeUtil;
import com.qinghe.mall.util.WechatSigner;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 微信支付 Native 扫码支付渠道（M2-9）。
 *
 * 流程：创建订单 → 调微信「Native 下单」获取 code_url → 渲染二维码 →
 * 用户扫码支付 → 微信异步通知 notify_url → 验签+解密 → 幂等更新订单。
 *
 * 仅在 WechatPayProperties 配置齐全时启用；未配置时支付回退模拟通道。
 */
@Component
public class WechatNativePayChannel implements PayChannel {

    private static final Logger log = LoggerFactory.getLogger(WechatNativePayChannel.class);

    @Autowired
    private WechatPayProperties properties;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String code() {
        return "WECHAT";
    }

    @Override
    public boolean enabled() {
        return properties.enabled();
    }

    @Override
    public ChannelPayResult createPay(Order order) {
        if (!enabled()) {
            throw new IllegalStateException("微信支付未配置，请先完成商户配置");
        }
        JSONObject amount = new JSONObject();
        amount.put("total", yuanToFen(order.getTotalPrice()));
        amount.put("currency", "CNY");

        JSONObject body = new JSONObject();
        body.put("appid", properties.getAppid());
        body.put("mchid", properties.getMchid());
        body.put("description", truncate(order.getProductName() != null ? order.getProductName() : "青禾商城订单", 127));
        body.put("out_trade_no", order.getOrderNumber());
        body.put("notify_url", properties.getNotifyUrl());
        body.put("amount", amount);

        String path = "/v3/pay/transactions/native";
        JSONObject resp = executeJson(HttpMethod.POST, path, body.toJSONString(), null);
        String codeUrl = resp.getString("code_url");
        if (StringUtils.isBlank(codeUrl)) {
            throw new IllegalStateException("微信下单未返回二维码: " + resp.toJSONString());
        }
        // 返回二维码 Base64 图片，前端直接展示
        return ChannelPayResult.real("WECHAT", "data:image/png;base64," + QrCodeUtil.toBase64Png(codeUrl), order.getOrderNumber());
    }

    /**
     * 查询订单支付状态。
     *
     * @return trade_state：SUCCESS / NOTPAY / CLOSED / USERPAYING 等；查询失败返回 null
     */
    public String queryTradeState(String orderNumber) {
        if (!enabled()) {
            return null;
        }
        String path = "/v3/pay/transactions/out-trade-no/" + orderNumber + "?mchid=" + properties.getMchid();
        try {
            JSONObject resp = executeJson(HttpMethod.GET, path, "", null);
            return resp.getString("trade_state");
        } catch (RuntimeException e) {
            log.warn("微信支付查询失败 orderNumber={}, reason={}", orderNumber, e.getMessage());
            return null;
        }
    }

    /**
     * 校验微信回调签名并解密出业务 JSON。
     *
     * @param serialHeader  Wechatpay-Serial
     * @param timestampHeader Wechatpay-Timestamp
     * @param nonceHeader   Wechatpay-Nonce
     * @param signatureHeader Wechatpay-Signature
     * @param body          回调原始请求体
     * @return 解密后的业务 JSON（含 out_trade_no / transaction_id / trade_state）
     */
    public JSONObject parseNotify(String serialHeader, String timestampHeader, String nonceHeader,
                                  String signatureHeader, String body) {
        if (StringUtils.isAnyBlank(timestampHeader, nonceHeader, signatureHeader, body)) {
            throw new IllegalArgumentException("微信回调参数不完整");
        }
        // 配置了平台证书则验签（更安全）；未配置时无法验证回调来源真实性，
        // 按 fail-closed 直接拒绝，避免伪造回调在未验签情况下篡改订单状态
        if (StringUtils.isNotBlank(properties.getPlatformCertPath())) {
            String message = timestampHeader + "\n" + nonceHeader + "\n" + body + "\n";
            PublicKey platformKey = WechatSigner.loadPlatformPublicKey(properties.getPlatformCertPath());
            if (!WechatSigner.verify(platformKey, message, signatureHeader)) {
                throw new IllegalArgumentException("微信回调签名验证失败");
            }
        } else {
            throw new IllegalArgumentException("微信平台证书未配置，无法验签，回调已拒绝");
        }
        JSONObject notify = JSON.parseObject(body);
        JSONObject resource = notify.getJSONObject("resource");
        if (resource == null) {
            throw new IllegalArgumentException("微信回调缺少 resource");
        }
        String plain = WechatSigner.decryptResource(properties.getApiV3Key(),
                resource.getString("associated_data"),
                resource.getString("nonce"),
                resource.getString("ciphertext"));
        return JSON.parseObject(plain);
    }

    // ============ 内部工具 ============

    /** 执行带 v3 签名的 HTTP 请求并解析 JSON */
    private JSONObject executeJson(HttpMethod method, String path, String body, String query) {
        String url = properties.getApiBase() + path;
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = UUID.randomUUID().toString().replace("-", "");
        PrivateKey privateKey = WechatSigner.loadPrivateKey(properties.getPrivateKeyPath());
        String message = WechatSigner.buildMessage(method.name(), path, timestamp, nonce, body);
        String signature = WechatSigner.sign(privateKey, message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, buildAuthorization(timestamp, nonce, signature));

        ResponseEntity<String> resp;
        try {
            resp = restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            throw new IllegalStateException("微信支付接口调用失败: " + e.getMessage());
        }
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("微信支付接口返回异常(" + resp.getStatusCodeValue() + "): " + resp.getBody());
        }
        String respBody = resp.getBody();
        if (StringUtils.isBlank(respBody)) {
            throw new IllegalStateException("微信支付接口返回为空");
        }
        return JSON.parseObject(respBody);
    }

    private String buildAuthorization(String timestamp, String nonce, String signature) {
        return "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + properties.getMchid() + "\","
                + "nonce_str=\"" + nonce + "\","
                + "signature=\"" + signature + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + properties.getSerialNo() + "\"";
    }

    /** 元转分（微信金额单位为分） */
    private int yuanToFen(java.math.BigDecimal yuan) {
        if (yuan == null) {
            return 0;
        }
        return yuan.multiply(java.math.BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP).intValueExact();
    }

    /** 截断 description（微信限制 ≤127 字符） */
    private String truncate(String s, int max) {
        if (s == null) {
            return "青禾商城订单";
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= max) {
            return s;
        }
        int end = 0;
        for (int i = 0; i < max && end < s.length(); ) {
            int cp = s.codePointAt(end);
            i += Character.charCount(cp) > 1 ? 3 : 1;
            end += Character.charCount(cp);
        }
        return s.substring(0, end);
    }
}
