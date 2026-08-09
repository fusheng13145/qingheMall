package com.qinghe.mall.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.qinghe.mall.config.AlipayProperties;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 支付宝开放平台客户端（M2-9 真实支付）。
 *
 * 基于支付宝开放平台 API v2（网关 openapi.alipaydev.com / openapi.alipay.com）：
 * - alipay.trade.precreate 当面付-预下单 → 返回二维码内容
 * - alipay.trade.query    查询交易状态
 * - 异步通知验签（RSA2）
 */
@Component
public class AlipayClient {

    private static final Logger log = LoggerFactory.getLogger(AlipayClient.class);

    @Autowired
    private AlipayProperties properties;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 当面付-预下单，返回收款二维码内容。
     *
     * @param outTradeNo 商户订单号
     * @param totalAmount 金额（元，如 "799.00"）
     * @param subject 商品描述
     */
    public String precreate(String outTradeNo, String totalAmount, String subject) {
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", totalAmount);
        bizContent.put("subject", truncate(subject, 256));
        bizContent.put("timeout_express", "30m"); // 30 分钟未支付自动关闭（与订单超时关单一致）

        JSONObject resp = request("alipay.trade.precreate", bizContent.toJSONString());
        JSONObject biz = resp.getJSONObject("alipay_trade_precreate_response");
        checkBiz(biz);
        return biz.getString("qr_code");
    }

    /**
     * 查询交易状态。
     *
     * @return trade_status：WAIT_BUYER_PAY / TRADE_SUCCESS / TRADE_CLOSED 等；查询失败返回 null
     */
    public String queryTradeStatus(String outTradeNo) {
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);
        try {
            JSONObject resp = request("alipay.trade.query", bizContent.toJSONString());
            JSONObject biz = resp.getJSONObject("alipay_trade_query_response");
            if (biz == null) {
                return null;
            }
            return biz.getString("trade_status");
        } catch (RuntimeException e) {
            log.warn("支付宝查询失败 outTradeNo={}, reason={}", outTradeNo, e.getMessage());
            return null;
        }
    }

    /**
     * 异步通知验签：对除 sign/sign_type 外的参数按 key 升序拼接后 RSA2 验签。
     */
    public boolean verifyNotify(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        String sign = params.get("sign");
        if (StringUtils.isBlank(sign)) {
            return false;
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if ("sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            if (StringUtils.isNotBlank(entry.getValue())) {
                sorted.put(key, entry.getValue());
            }
        }
        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            content.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        if (content.length() > 0) {
            content.setLength(content.length() - 1);
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(loadPublicKey(properties.getAlipayPublicKey()));
            signature.update(content.toString().getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            log.warn("支付宝回调验签异常", e);
            return false;
        }
    }

    // ============ 内部实现 ============

    /** 发起 API 请求并返回响应 JSON（公共参数 + RSA2 签名） */
    private JSONObject request(String method, String bizContent) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("app_id", properties.getAppId());
        params.put("method", method);
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        params.put("version", "1.0");
        params.put("notify_url", properties.getNotifyUrl());
        params.put("biz_content", bizContent);
        params.put("sign", sign(params));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(properties.getGateway(),
                    new HttpEntity<>(form, headers), String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("支付宝网关返回异常(" + resp.getStatusCodeValue() + "): " + resp.getBody());
            }
            if (StringUtils.isBlank(resp.getBody())) {
                throw new IllegalStateException("支付宝网关返回为空");
            }
            return JSON.parseObject(resp.getBody());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("支付宝接口调用失败: " + e.getMessage());
        }
    }

    /** 校验 biz 响应体 code == 10000（成功） */
    private void checkBiz(JSONObject biz) {
        if (biz == null) {
            throw new IllegalStateException("支付宝响应缺少业务体");
        }
        if (!"10000".equals(biz.getString("code"))) {
            throw new IllegalStateException("支付宝业务失败: " + biz.getString("code") + " " + biz.getString("sub_msg"));
        }
    }

    /** 构造待签名串并按 RSA2 签名 */
    private String sign(TreeMap<String, String> params) {
        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            content.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        content.setLength(content.length() - 1);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(loadPrivateKey(properties.getPrivateKey()));
            signature.update(content.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("支付宝签名失败", e);
        }
    }

    /**
     * 加载应用私钥：支持 PEM 内容（-----BEGIN 开头）或文件路径；PKCS8 / PKCS1 均可。
     */
    private PrivateKey loadPrivateKey(String privateKey) {
        try {
            String pem = readPem(privateKey);
            if (pem.contains("BEGIN RSA PRIVATE KEY")) {
                // PKCS1：用 BouncyCastle 解析
                String base64 = pem
                        .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                        .replace("-----END RSA PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
                RSAPrivateKey pkcs1 = RSAPrivateKey.getInstance(Base64.getDecoder().decode(base64));
                RSAPrivateCrtKeyParameters keyParams =
                        (RSAPrivateCrtKeyParameters) PrivateKeyFactory.createKey(pkcs1.getEncoded());
                java.security.spec.RSAPrivateCrtKeySpec spec =
                        new java.security.spec.RSAPrivateCrtKeySpec(
                                keyParams.getModulus(),
                                keyParams.getPublicExponent(),
                                keyParams.getExponent(),
                                keyParams.getP(),
                                keyParams.getQ(),
                                keyParams.getDP(),
                                keyParams.getDQ(),
                                keyParams.getQInv());
                return KeyFactory.getInstance("RSA").generatePrivate(spec);
            }
            // PKCS8
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (Exception e) {
            throw new IllegalStateException("加载支付宝应用私钥失败", e);
        }
    }

    /** 加载支付宝公钥（PEM 内容或文件路径） */
    private PublicKey loadPublicKey(String alipayPublicKey) {
        try {
            String pem = readPem(alipayPublicKey);
            String base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (Exception e) {
            throw new IllegalStateException("加载支付宝公钥失败", e);
        }
    }

    /** PEM 内容或文件路径 → PEM 字符串 */
    private String readPem(String source) {
        if (source.startsWith("-----BEGIN")) {
            return source;
        }
        try {
            return new String(Files.readAllBytes(new File(source).toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("读取密钥文件失败: " + source, e);
        }
    }

    private String truncate(String s, int maxBytes) {
        if (s == null) {
            return "";
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return s;
        }
        int end = 0;
        for (int i = 0; i < maxBytes && end < s.length(); ) {
            int cp = s.codePointAt(end);
            i += Character.charCount(cp) > 1 ? 3 : 1;
            end += Character.charCount(cp);
        }
        return s.substring(0, end);
    }
}
