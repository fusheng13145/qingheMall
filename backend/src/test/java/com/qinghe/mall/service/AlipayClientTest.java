package com.qinghe.mall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.qinghe.mall.config.AlipayProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 支付宝客户端 单元测试（体检报告 §4 P0 清单补测）。
 *
 * 核心：异步通知 RSA2 验签（verifyNotify）——真实密钥对往返 + 篡改/缺参拒绝；
 * 业务路径：precreate 成功/业务失败/缺业务体、queryTradeStatus 成功/降级、
 * PKCS1/PKCS8 私钥加载。
 */
class AlipayClientTest {

    private AlipayClient alipayClient;
    private AlipayProperties properties;
    private PrivateKey alipayPrivateKey; // 模拟支付宝侧私钥（签名用）
    private PublicKey alipayPublicKey;   // 商户侧配置的支付宝公钥（验签用）
    private PrivateKey appPrivateKey;    // 商户应用私钥（请求签名用）
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        alipayPrivateKey = keyPair.getPrivate();
        alipayPublicKey = keyPair.getPublic();
        appPrivateKey = generator.generateKeyPair().getPrivate();

        properties = new AlipayProperties();
        ReflectionTestUtils.setField(properties, "alipayPublicKey", publicKeyPem(alipayPublicKey));
        ReflectionTestUtils.setField(properties, "privateKey", privateKeyPem(appPrivateKey));
        ReflectionTestUtils.setField(properties, "appId", "2021000000000000");
        ReflectionTestUtils.setField(properties, "notifyUrl", "https://mall.example.com/api/pay/alipayNotify");
        ReflectionTestUtils.setField(properties, "gateway", "https://openapi.alipaydev.com/gateway.do");

        alipayClient = new AlipayClient();
        ReflectionTestUtils.setField(alipayClient, "properties", properties);
        restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        ReflectionTestUtils.setField(alipayClient, "restTemplate", restTemplate);
    }

    /** 公钥转 PEM 内容（与配置格式一致） */
    private String publicKeyPem(PublicKey publicKey) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey.getEncoded());
        String base64 = Base64.getEncoder().encodeToString(spec.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }

    /** 私钥转 PKCS8 PEM */
    private String privateKeyPem(PrivateKey privateKey) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        String base64 = Base64.getEncoder().encodeToString(spec.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----";
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

    // ============ precreate 预下单 ============

    @Test
    @DisplayName("precreate 成功返回 qr_code")
    void precreate_success_returnsQrCode() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"alipay_trade_precreate_response\":{\"code\":\"10000\",\"qr_code\":\"https://qr.alipay.com/abc\"}}"));

        String qr = alipayClient.precreate("QH1001", "199.00", "青禾商城订单");

        assertEquals("https://qr.alipay.com/abc", qr);
    }

    @Test
    @DisplayName("precreate 支付宝业务失败（code != 10000）抛异常")
    void precreate_bizFailure_throws() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"alipay_trade_precreate_response\":{\"code\":\"40004\",\"sub_msg\":\"业务处理失败\"}}"));

        assertThrows(IllegalStateException.class,
                () -> alipayClient.precreate("QH1001", "199.00", "订单"), "业务失败必须抛异常");
    }

    @Test
    @DisplayName("precreate 响应缺少业务体抛异常")
    void precreate_missingBiz_throws() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"error_response\":{\"code\":\"40002\"}}"));

        assertThrows(IllegalStateException.class,
                () -> alipayClient.precreate("QH1001", "199.00", "订单"), "缺业务体必须抛异常");
    }

    @Test
    @DisplayName("precreate 网关 5xx 抛异常")
    void precreate_gatewayError_throws() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("upstream error"));

        assertThrows(IllegalStateException.class,
                () -> alipayClient.precreate("QH1001", "199.00", "订单"));
    }

    @Test
    @DisplayName("precreate 网络异常包装为 IllegalStateException")
    void precreate_networkError_throws() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThrows(IllegalStateException.class,
                () -> alipayClient.precreate("QH1001", "199.00", "订单"), "网络异常应包装抛出");
    }

    // ============ queryTradeStatus 查询 ============

    @Test
    @DisplayName("查询成功返回 trade_status")
    void query_success_returnsStatus() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"alipay_trade_query_response\":{\"code\":\"10000\",\"trade_status\":\"TRADE_SUCCESS\"}}"));

        assertEquals("TRADE_SUCCESS", alipayClient.queryTradeStatus("QH1001"));
    }

    @Test
    @DisplayName("查询响应缺业务体返回 null")
    void query_missingBiz_returnsNull() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        assertNull(alipayClient.queryTradeStatus("QH1001"), "缺业务体应返回 null 而非抛异常");
    }

    @Test
    @DisplayName("查询异常降级返回 null 不抛出")
    void query_failure_returnsNull() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenThrow(new RuntimeException("network down"));

        assertNull(alipayClient.queryTradeStatus("QH1001"), "查询异常应降级返回 null");
    }

    // ============ 密钥加载 ============

    @Test
    @DisplayName("PKCS8 私钥 PEM 可正常签名请求")
    void privateKey_pkcs8_signsRequest() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"alipay_trade_precreate_response\":{\"code\":\"10000\",\"qr_code\":\"qr\"}}"));

        // 能完成签名并调用网关即证明 PKCS8 私钥加载成功
        String qr = alipayClient.precreate("QH1001", "1.00", "s");
        assertEquals("qr", qr);
    }

    @Test
    @DisplayName("非法私钥内容加载失败抛异常")
    void privateKey_invalid_throws() {
        ReflectionTestUtils.setField(properties, "privateKey", "-----BEGIN PRIVATE KEY-----\nbm90LWEta2V5\n-----END PRIVATE KEY-----");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        assertThrows(IllegalStateException.class,
                () -> alipayClient.precreate("QH1001", "1.00", "s"), "非法私钥必须抛异常");
    }

    @Test
    @DisplayName("PKCS1 私钥 PEM（BEGIN RSA PRIVATE KEY）可正常签名")
    void privateKey_pkcs1_signsRequest() throws Exception {
        // 由 PKCS8 私钥提取 RSA CRT 参数，构造 PKCS1 ASN.1 结构并转 PEM
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        java.security.spec.RSAPrivateCrtKeySpec spec =
                keyFactory.getKeySpec(appPrivateKey, java.security.spec.RSAPrivateCrtKeySpec.class);
        org.bouncycastle.asn1.pkcs.RSAPrivateKey bcKey = new org.bouncycastle.asn1.pkcs.RSAPrivateKey(
                spec.getModulus(), spec.getPublicExponent(), spec.getPrivateExponent(),
                spec.getPrimeP(), spec.getPrimeQ(), spec.getPrimeExponentP(), spec.getPrimeExponentQ(),
                spec.getCrtCoefficient());
        String base64 = Base64.getEncoder().encodeToString(bcKey.getEncoded());
        ReflectionTestUtils.setField(properties, "privateKey",
                "-----BEGIN RSA PRIVATE KEY-----\n" + base64 + "\n-----END RSA PRIVATE KEY-----");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"alipay_trade_precreate_response\":{\"code\":\"10000\",\"qr_code\":\"qr1\"}}"));

        assertEquals("qr1", alipayClient.precreate("QH1001", "1.00", "s"));
    }

    @Test
    @DisplayName("超长 subject 截断到 256 字节仍可下单")
    void precreate_longSubject_truncates() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), org.mockito.Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"alipay_trade_precreate_response\":{\"code\":\"10000\",\"qr_code\":\"qr2\"}}"));

        String longSubject = "青".repeat(200);
        assertEquals("qr2", alipayClient.precreate("QH1001", "1.00", longSubject));
    }
}
