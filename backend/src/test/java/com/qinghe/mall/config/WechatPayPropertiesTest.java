package com.qinghe.mall.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WechatPayProperties 分支覆盖补充（T3 续补）。
 * 覆盖 enabled() 六元短路校验的全部分支组合。
 */
class WechatPayPropertiesTest {

    private WechatPayProperties newProps() {
        return new WechatPayProperties();
    }

    private void fillAll(WechatPayProperties p) {
        ReflectionTestUtils.setField(p, "appid", "wx");
        ReflectionTestUtils.setField(p, "mchid", "mch");
        ReflectionTestUtils.setField(p, "apiV3Key", "key32xxxxxxxxxxxxxxxxxxxxxxxxx");
        ReflectionTestUtils.setField(p, "privateKeyPath", "/p/em.pem");
        ReflectionTestUtils.setField(p, "serialNo", "sn");
        ReflectionTestUtils.setField(p, "notifyUrl", "https://cb");
    }

    @Test
    void enabled_allBlank_false() {
        assertFalse(newProps().enabled());
    }

    @Test
    void enabled_missingMchid_false() {
        WechatPayProperties p = newProps();
        ReflectionTestUtils.setField(p, "appid", "wx");
        assertFalse(p.enabled());
    }

    @Test
    void enabled_missingApiV3Key_false() {
        WechatPayProperties p = newProps();
        ReflectionTestUtils.setField(p, "appid", "wx");
        ReflectionTestUtils.setField(p, "mchid", "mch");
        assertFalse(p.enabled());
    }

    @Test
    void enabled_missingNotifyUrl_false() {
        WechatPayProperties p = newProps();
        ReflectionTestUtils.setField(p, "appid", "wx");
        ReflectionTestUtils.setField(p, "mchid", "mch");
        ReflectionTestUtils.setField(p, "apiV3Key", "key");
        ReflectionTestUtils.setField(p, "privateKeyPath", "/p/em.pem");
        ReflectionTestUtils.setField(p, "serialNo", "sn");
        assertFalse(p.enabled());
    }

    @Test
    void enabled_allSet_true() {
        WechatPayProperties p = newProps();
        fillAll(p);
        assertTrue(p.enabled());
    }

    @Test
    void getters_returnInjectedValues() {
        WechatPayProperties p = newProps();
        fillAll(p);
        ReflectionTestUtils.setField(p, "platformCertPath", "/c/cert.pem");
        ReflectionTestUtils.setField(p, "apiBase", "https://api");
        org.junit.jupiter.api.Assertions.assertEquals("wx", p.getAppid());
        org.junit.jupiter.api.Assertions.assertEquals("mch", p.getMchid());
        org.junit.jupiter.api.Assertions.assertEquals("key32xxxxxxxxxxxxxxxxxxxxxxxxx", p.getApiV3Key());
        org.junit.jupiter.api.Assertions.assertEquals("/c/cert.pem", p.getPlatformCertPath());
        org.junit.jupiter.api.Assertions.assertEquals("https://api", p.getApiBase());
    }
}
