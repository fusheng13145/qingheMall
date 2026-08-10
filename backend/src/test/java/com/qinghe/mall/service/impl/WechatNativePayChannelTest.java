package com.qinghe.mall.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson2.JSON;
import com.qinghe.mall.config.WechatPayProperties;
import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.util.WechatSigner;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 微信 Native 支付通道单元测试（体检报告 §3.3 P0 资金链路补测：原 3% 覆盖）。
 *
 * 覆盖：渠道启用判定、createPay 成功/未配置、queryTradeState 成功/失败降级、
 * parseNotify 完整验签解密链路（真实 RSA 密钥对 + AES-256-GCM）+ 全失败路径
 * （参数缺失/时间戳非法/时间戳超时/证书未配置 fail-closed/验签失败）。
 */
class WechatNativePayChannelTest {

    private static final String API_V3_KEY = "0123456789abcdef0123456789abcdef";

    private WechatNativePayChannel channel;
    private WechatPayProperties properties;
    private PrivateKey wechatPrivateKey; // 模拟微信平台侧私钥（签名用）
    private PublicKey wechatPublicKey;   // 写入临时证书，通道加载验签用
    private RestTemplate restTemplate;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        wechatPrivateKey = keyPair.getPrivate();
        wechatPublicKey = keyPair.getPublic();

        properties = new WechatPayProperties();
        ReflectionTestUtils.setField(properties, "apiV3Key", API_V3_KEY);

        channel = new WechatNativePayChannel();
        ReflectionTestUtils.setField(channel, "properties", properties);
        restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        ReflectionTestUtils.setField(channel, "restTemplate", restTemplate);
    }

    /** 平台证书以 X509 格式存储，测试直接用 mockStatic 替换证书加载，返回测试公钥 */
    private org.mockito.MockedStatic<WechatSigner> mockCertLoad() {
        // 先置非空证书路径（进入验签分支），再用 mockStatic 替换证书加载
        ReflectionTestUtils.setField(properties, "platformCertPath", "mock-platform-cert.pem");
        org.mockito.MockedStatic<WechatSigner> mocked = org.mockito.Mockito.mockStatic(WechatSigner.class,
                org.mockito.Mockito.CALLS_REAL_METHODS);
        mocked.when(() -> WechatSigner.loadPlatformPublicKey(anyString())).thenReturn(wechatPublicKey);
        return mocked;
    }

    private Order order() {
        Order o = new Order();
        o.setOrderNumber("QH20260810001");
        o.setProductName("测试商品");
        o.setTotalPrice(new BigDecimal("199.90"));
        return o;
    }

    private void enableChannel() throws Exception {
        ReflectionTestUtils.setField(properties, "appid", "wx-test-appid");
        ReflectionTestUtils.setField(properties, "mchid", "1900000001");
        ReflectionTestUtils.setField(properties, "serialNo", "SERIAL001");
        ReflectionTestUtils.setField(properties, "notifyUrl", "https://mall.example.com/api/pay/wechatNotify");
        // 私钥文件（通道 createPay 需加载商户私钥签名）
        Path key = tempDir.resolve("apiclient_key.pem");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        PrivateKey merchantKey = generator.generateKeyPair().getPrivate();
        String base64 = Base64.getEncoder().encodeToString(merchantKey.getEncoded());
        Files.write(key, ("-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n")
                .getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(properties, "privateKeyPath", key.toString());
    }

    // ============ 渠道启用 ============

    @Test
    @DisplayName("未配置时渠道禁用")
    void disabledWhenNotConfigured() {
        assertThat(channel.enabled()).isFalse();
    }

    @Test
    @DisplayName("配置齐全后渠道启用")
    void enabledWhenConfigured() throws Exception {
        enableChannel();
        assertThat(channel.enabled()).isTrue();
        assertThat(channel.code()).isEqualTo("WECHAT");
    }

    // ============ createPay ============

    @Test
    @DisplayName("未配置时 createPay 抛 IllegalStateException")
    void createPayThrowsWhenDisabled() {
        assertThatThrownBy(() -> channel.createPay(order()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("微信支付未配置");
    }

    @Test
    @DisplayName("createPay 成功：调用微信下单接口并返回二维码")
    void createPaySuccess() throws Exception {
        enableChannel();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code_url\":\"weixin://wxpay/bizpayurl?pr=abc\"}"));

        ChannelPayResult result = channel.createPay(order());

        assertThat(result.getMock()).isFalse();
        assertThat(result.getChannel()).isEqualTo("WECHAT");
        assertThat(result.getOrderNumber()).isEqualTo("QH20260810001");
        assertThat(result.getCodeUrl()).startsWith("data:image/png;base64,");
    }

    @Test
    @DisplayName("createPay 响应缺 code_url 抛异常")
    void createPayMissingCodeUrl() throws Exception {
        enableChannel();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"msg\":\"error\"}"));

        assertThatThrownBy(() -> channel.createPay(order()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未返回二维码");
    }

    @Test
    @DisplayName("createPay 微信接口 5xx 抛异常")
    void createPayServerError() throws Exception {
        enableChannel();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("upstream error"));

        assertThatThrownBy(() -> channel.createPay(order()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("返回异常");
    }

    // ============ queryTradeState ============

    @Test
    @DisplayName("未配置时查询返回 null（调用方走主动查询降级）")
    void queryReturnsNullWhenDisabled() {
        assertThat(channel.queryTradeState("QH1")).isNull();
    }

    @Test
    @DisplayName("查询成功返回 trade_state")
    void querySuccess() throws Exception {
        enableChannel();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"trade_state\":\"SUCCESS\"}"));

        assertThat(channel.queryTradeState("QH1")).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("查询异常降级返回 null 不抛出")
    void queryFailureDegradesToNull() throws Exception {
        enableChannel();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThat(channel.queryTradeState("QH1")).isNull();
    }

    // ============ parseNotify 失败路径 ============

    @Test
    @DisplayName("回调参数缺失拒绝")
    void notifyMissingParamsRejected() {
        assertThatThrownBy(() -> channel.parseNotify("serial", "", "nonce", "sig", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("参数不完整");
    }

    @Test
    @DisplayName("回调时间戳非数字拒绝")
    void notifyBadTimestampRejected() {
        assertThatThrownBy(() -> channel.parseNotify("serial", "abc", "nonce", "sig", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时间戳非法");
    }

    @Test
    @DisplayName("回调时间戳超时（±5 分钟外）拒绝防重放")
    void notifyStaleTimestampRejected() {
        long stale = System.currentTimeMillis() / 1000 - 600;
        assertThatThrownBy(() -> channel.parseNotify("serial", String.valueOf(stale), "nonce", "sig", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时间戳超时");
    }

    @Test
    @DisplayName("平台证书未配置时 fail-closed 拒绝回调")
    void notifyNoCertFailClosed() {
        long now = System.currentTimeMillis() / 1000;
        assertThatThrownBy(() -> channel.parseNotify("serial", String.valueOf(now), "nonce", "sig", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("证书未配置");
    }

    @Test
    @DisplayName("验签失败拒绝（伪造签名）")
    void notifyBadSignatureRejected() {
        try (org.mockito.MockedStatic<WechatSigner> mocked = mockCertLoad()) {
            long now = System.currentTimeMillis() / 1000;
            String body = "{\"resource\":{}}";

            assertThatThrownBy(() -> channel.parseNotify("serial", String.valueOf(now), "nonce", "forged-sig", body))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("签名验证失败");
        }
    }

    // ============ parseNotify 完整链路 ============

    @Test
    @DisplayName("完整回调验签+解密成功返回业务 JSON")
    void notifyFullChainSuccess() throws Exception {
        try (org.mockito.MockedStatic<WechatSigner> mocked = mockCertLoad()) {
            long now = System.currentTimeMillis() / 1000;
            String timestamp = String.valueOf(now);
            String nonce = "nonce123456";
            String plaintext = "{\"out_trade_no\":\"QH20260810001\",\"transaction_id\":\"4200001234\",\"trade_state\":\"SUCCESS\"}";

            // 构造加密 resource（模拟微信平台）
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(API_V3_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            cipher.updateAAD("transaction".getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            String body = JSON.toJSONString(new java.util.LinkedHashMap<>() {{
                put("resource", new java.util.LinkedHashMap<>() {{
                    put("associated_data", "transaction");
                    put("nonce", nonce);
                    put("ciphertext", Base64.getEncoder().encodeToString(ciphertext));
                }});
            }});

            // 签名（模拟微信平台用平台私钥签 message）
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            String signature = WechatSigner.sign(wechatPrivateKey, message);

            com.alibaba.fastjson2.JSONObject result =
                    channel.parseNotify("SERIAL001", timestamp, nonce, signature, body);

            assertThat(result.getString("out_trade_no")).isEqualTo("QH20260810001");
            assertThat(result.getString("transaction_id")).isEqualTo("4200001234");
            assertThat(result.getString("trade_state")).isEqualTo("SUCCESS");
        }
    }
}
