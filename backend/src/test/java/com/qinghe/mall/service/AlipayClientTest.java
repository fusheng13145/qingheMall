package com.qinghe.mall.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qinghe.mall.config.AlipayProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 支付宝客户端 单元测试（体检报告 §4 P0 清单补测）。
 *
 * 核心：异步通知 RSA2 验签（verifyNotify）——真实密钥对往返 + 篡改/缺参拒绝。
 */
class AlipayClientTest {

    private AlipayClient alipayClient;
    private PrivateKey alipayPrivateKey; // 模拟支付宝侧私钥（签名用）
    private PublicKey alipayPublicKey;   // 商户侧配置的支付宝公钥（验签用）

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        alipayPrivateKey = keyPair.getPrivate();
        alipayPublicKey = keyPair.getPublic();

        AlipayProperties properties = new AlipayProperties();
        ReflectionTestUtils.setField(properties, "alipayPublicKey", publicKeyPem(alipayPublicKey));
        alipayClient = new AlipayClient();
        ReflectionTestUtils.setField(alipayClient, "properties", properties);
    }

    /** 公钥转 PEM 内容（与配置格式一致） */
    private String publicKeyPem(PublicKey publicKey) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey.getEncoded());
        String base64 = Base64.getEncoder().encodeToString(spec.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }

    /** 按支付宝规则对参数签名（除 sign/sign_type，key 升序，值非空，& 连接） */
    private String signParams(Map<String, String> params) throws Exception {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if ("sign".equals(key) || "sign_type".equals(key) || entry.getValue() == null) {
                continue;
            }
            sorted.put(key, entry.getValue());
        }
        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            content.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        if (content.length() > 0) {
            content.setLength(content.length() - 1);
        }
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(alipayPrivateKey);
        signature.update(content.toString().getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    @Test
    void verifyNotify_validSignature_passes() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "QH123");
        params.put("total_amount", "100.00");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("app_id", "2021000000000000");
        params.put("sign", signParams(params));

        assertTrue(alipayClient.verifyNotify(params), "合法支付宝回调应验签通过");
    }

    @Test
    void verifyNotify_tamperedAmount_fails() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "QH123");
        params.put("total_amount", "100.00");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("sign", signParams(params));

        // 篡改金额：验签必须失败（P0-2 金额核对前置防线）
        params.put("total_amount", "999999.99");
        assertFalse(alipayClient.verifyNotify(params), "篡改金额后的回调必须验签失败");
    }

    @Test
    void verifyNotify_missingSign_fails() {
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "QH123");
        assertFalse(alipayClient.verifyNotify(params), "缺 sign 必须拒绝");
    }

    @Test
    void verifyNotify_nullOrEmpty_fails() {
        assertFalse(alipayClient.verifyNotify(null));
        assertFalse(alipayClient.verifyNotify(new HashMap<>()));
    }
}
