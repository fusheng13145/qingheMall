package com.qinghe.mall.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * 二维码工具 单元测试（微信 Native 支付 code_url → Base64 PNG）。
 */
class QrCodeUtilTest {

    @Test
    void toBase64Png_returnsDecodablePng() {
        String base64 = QrCodeUtil.toBase64Png("weixin://wxpay/bizpayurl?pr=abc123");

        byte[] bytes = Base64.getDecoder().decode(base64);
        // PNG 魔数：89 50 4E 47
        assertTrue(bytes.length > 100, "PNG 应有一定体积");
        assertTrue((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G',
                "输出应为 PNG 格式");
    }

    @Test
    void toBase64Png_emptyContent_throws() {
        assertThrows(IllegalStateException.class, () -> QrCodeUtil.toBase64Png(""));
    }
}
