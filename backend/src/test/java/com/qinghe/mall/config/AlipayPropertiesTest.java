package com.qinghe.mall.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AlipayProperties 分支覆盖补充（T3 续补）。
 * 覆盖 enabled() 四元短路校验的全部分支组合。
 */
class AlipayPropertiesTest {

    private AlipayProperties newProps() {
        return new AlipayProperties();
    }

    @Test
    void enabled_allBlank_false() {
        assertFalse(newProps().enabled());
    }

    @Test
    void enabled_onlyAppId_false() {
        AlipayProperties p = newProps();
        ReflectionTestUtils.setField(p, "appId", "appid");
        assertFalse(p.enabled());
    }

    @Test
    void enabled_missingPrivateKey_false() {
        AlipayProperties p = newProps();
        ReflectionTestUtils.setField(p, "appId", "appid");
        ReflectionTestUtils.setField(p, "privateKey", "pk");
        assertFalse(p.enabled());
    }

    @Test
    void enabled_missingPublicKey_false() {
        AlipayProperties p = newProps();
        ReflectionTestUtils.setField(p, "appId", "appid");
        ReflectionTestUtils.setField(p, "privateKey", "pk");
        ReflectionTestUtils.setField(p, "alipayPublicKey", "pub");
        assertFalse(p.enabled());
    }

    @Test
    void enabled_allSet_true() {
        AlipayProperties p = newProps();
        ReflectionTestUtils.setField(p, "appId", "appid");
        ReflectionTestUtils.setField(p, "privateKey", "pk");
        ReflectionTestUtils.setField(p, "alipayPublicKey", "pub");
        ReflectionTestUtils.setField(p, "notifyUrl", "https://cb.example.com");
        assertTrue(p.enabled());
    }

    @Test
    void getters_returnInjectedValues() {
        AlipayProperties p = newProps();
        ReflectionTestUtils.setField(p, "appId", "a");
        ReflectionTestUtils.setField(p, "privateKey", "b");
        ReflectionTestUtils.setField(p, "alipayPublicKey", "c");
        ReflectionTestUtils.setField(p, "notifyUrl", "d");
        ReflectionTestUtils.setField(p, "gateway", "https://gw");
        org.junit.jupiter.api.Assertions.assertEquals("a", p.getAppId());
        org.junit.jupiter.api.Assertions.assertEquals("b", p.getPrivateKey());
        org.junit.jupiter.api.Assertions.assertEquals("c", p.getAlipayPublicKey());
        org.junit.jupiter.api.Assertions.assertEquals("d", p.getNotifyUrl());
        org.junit.jupiter.api.Assertions.assertEquals("https://gw", p.getGateway());
    }
}
