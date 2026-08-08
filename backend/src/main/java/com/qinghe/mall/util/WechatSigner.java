package com.qinghe.mall.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;

/**
 * 微信支付 v3 签名与加解密工具（M2-9 真实支付）。
 *
 * - 请求签名：商户私钥 RSA-SHA256，生成 Authorization 头
 * - 回调验签：微信平台证书公钥验证 Wechatpay-Signature（可选）
 * - 回调解密：APIv3 密钥 AES-256-GCM 解密 resource.ciphertext
 */
public final class WechatSigner {

    private WechatSigner() {
    }

    /** 读取 PKCS8 PEM 格式商户私钥（apiclient_key.pem） */
    public static PrivateKey loadPrivateKey(String pemPath) {
        try {
            String pem = new String(Files.readAllBytes(Paths.get(pemPath)), StandardCharsets.UTF_8);
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("读取商户私钥失败: " + pemPath, e);
        }
    }

    /** 读取微信支付平台证书（验签用） */
    public static PublicKey loadPlatformPublicKey(String certPath) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (java.io.InputStream in = Files.newInputStream(Paths.get(certPath))) {
                X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
                return cert.getPublicKey();
            }
        } catch (Exception e) {
            throw new IllegalStateException("读取微信平台证书失败: " + certPath, e);
        }
    }

    /**
     * 构造微信 v3 签名串：method \n url \n timestamp \n nonce \n body \n
     */
    public static String buildMessage(String method, String url, String timestamp, String nonce, String body) {
        return method + "\n" + url + "\n" + timestamp + "\n" + nonce + "\n" + (body == null ? "" : body) + "\n";
    }

    /** RSA-SHA256 签名，返回 Base64 */
    public static String sign(PrivateKey privateKey, String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("微信签名失败", e);
        }
    }

    /** 用微信平台证书公钥验签 */
    public static boolean verify(PublicKey platformPublicKey, String message, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(platformPublicKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * APIv3 密钥 AES-256-GCM 解密回调 resource。
     *
     * @param apiV3Key APIv3 密钥（32 字节）
     * @param associatedData 关联数据（associated_data）
     * @param nonce 12 字节随机串
     * @param ciphertextBase64 密文（Base64）
     */
    public static String decryptResource(String apiV3Key, String associatedData, String nonce, String ciphertextBase64) {
        try {
            byte[] key = apiV3Key.getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            if (StringUtils.isNotBlank(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertextBase64));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("微信回调解密失败（可能 APIv3 密钥不正确）", e);
        }
    }
}
