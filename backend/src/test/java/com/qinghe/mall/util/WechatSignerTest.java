package com.qinghe.mall.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * 微信支付 v3 签名/验签/加解密 单元测试（资金安全链路补测，对应原评估 P0 清单）。
 *
 * 覆盖：RSA 签名验签往返、篡改检测、AES-256-GCM 解密（含 AAD）、密钥加载错误路径。
 */
class WechatSignerTest {

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Test
    void signAndVerify_roundTrip_success() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        String message = WechatSigner.buildMessage("POST", "/v3/pay/transactions/native",
                "1786000000", "nonce123", "{\"amount\":100}");

        String signature = WechatSigner.sign(keyPair.getPrivate(), message);

        assertTrue(WechatSigner.verify(keyPair.getPublic(), message, signature),
                "合法签名应验签通过");
    }

    @Test
    void verify_tamperedMessage_fails() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        String message = "POST\n/v3/pay/transactions/native\n1786000000\nnonce123\n{}\n";
        String signature = WechatSigner.sign(keyPair.getPrivate(), message);

        // 篡改消息（金额/时间戳任一变更都应验签失败）
        String tampered = message.replace("nonce123", "nonce999");
        assertFalse(WechatSigner.verify(keyPair.getPublic(), tampered, signature),
                "篡改后的消息必须验签失败");
    }

    @Test
    void verify_wrongKey_fails() throws Exception {
        KeyPair keyPairA = generateRsaKeyPair();
        KeyPair keyPairB = generateRsaKeyPair();
        String message = "POST\n/v3/pay\n1\n2\n{}\n";
        String signature = WechatSigner.sign(keyPairA.getPrivate(), message);

        // 用错误的公钥（keyPairB）验签必须失败
        assertFalse(WechatSigner.verify(keyPairB.getPublic(), message, signature),
                "错误公钥验签必须失败");
    }

    @Test
    void verify_invalidBase64_returnsFalse() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        assertFalse(WechatSigner.verify(keyPair.getPublic(), "message", "!!not-base64!!"));
    }

    @Test
    void decryptResource_aes256Gcm_roundTrip() throws Exception {
        // APIv3 密钥为 32 字节 ASCII（微信签发格式），此处用固定 32 字符可打印串
        String apiV3Key = "0123456789abcdef0123456789abcdef";
        String associatedData = "transaction";
        String nonce = "123456789012"; // 12 字节
        String plaintext = "{\"out_trade_no\":\"QH1\",\"transaction_id\":\"4200001\"}";

        // 加密（测试侧用 JDK 原生实现）
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        String decrypted = WechatSigner.decryptResource(
                apiV3Key, associatedData, nonce, Base64.getEncoder().encodeToString(ciphertext));

        assertEquals(plaintext, decrypted, "AES-256-GCM 解密应还原明文");
    }

    @Test
    void decryptResource_wrongKey_throws() throws Exception {
        String apiV3Key = "0123456789abcdef0123456789abcdef";
        String wrongKey = "fedcba9876543210fedcba9876543210";
        String nonce = "123456789012";
        String plaintext = "{\"out_trade_no\":\"QH1\"}";

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // 错误密钥解密（GCM 认证失败）应抛异常而非返回脏数据
        assertThrows(IllegalStateException.class, () -> WechatSigner.decryptResource(
                wrongKey, "transaction", nonce, Base64.getEncoder().encodeToString(ciphertext)));
    }

    @Test
    void buildMessage_format() {
        String message = WechatSigner.buildMessage("POST", "/url", "1", "n", "body");
        assertEquals("POST\n/url\n1\nn\nbody\n", message);
        // body 为 null 时按空串处理
        assertEquals("GET\n/u\n1\nn\n\n", WechatSigner.buildMessage("GET", "/u", "1", "n", null));
    }

    @Test
    void loadPrivateKey_nonExistentPath_throws() {
        assertThrows(IllegalStateException.class, () -> WechatSigner.loadPrivateKey("/nonexistent/key.pem"));
        assertThrows(IllegalStateException.class, () -> WechatSigner.loadPlatformPublicKey("/nonexistent/cert.pem"));
    }
}
