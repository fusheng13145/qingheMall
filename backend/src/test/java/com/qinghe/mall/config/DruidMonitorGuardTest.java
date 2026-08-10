package com.qinghe.mall.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Druid 监控台安全守卫 单元测试（P0-7 纵深防御）。
 *
 * 覆盖：关闭直接放行；开启 + 弱口令（黑名单/过短）拒绝启动；开启 + 强口令放行。
 */
class DruidMonitorGuardTest {

    private DruidMonitorGuard guard() {
        return new DruidMonitorGuard();
    }

    @Test
    void disabled_returnsNormally() {
        DruidMonitorGuard g = guard();
        ReflectionTestUtils.setField(g, "monitorEnabled", false);
        ReflectionTestUtils.setField(g, "loginPassword", "druid");

        assertDoesNotThrow(g::guard);
    }

    @Test
    void enabled_defaultPassword_throws() {
        DruidMonitorGuard g = guard();
        ReflectionTestUtils.setField(g, "monitorEnabled", true);
        ReflectionTestUtils.setField(g, "loginPassword", "druid");

        assertThrows(IllegalStateException.class, g::guard, "默认弱口令必须拒绝启动");
    }

    @Test
    void enabled_blacklistPassword_throws() {
        DruidMonitorGuard g = guard();
        ReflectionTestUtils.setField(g, "monitorEnabled", true);
        ReflectionTestUtils.setField(g, "loginPassword", "change_me_strong");

        assertThrows(IllegalStateException.class, g::guard);
    }

    @Test
    void enabled_shortPassword_throws() {
        DruidMonitorGuard g = guard();
        ReflectionTestUtils.setField(g, "monitorEnabled", true);
        ReflectionTestUtils.setField(g, "loginPassword", "1234567");

        assertThrows(IllegalStateException.class, g::guard, "过短口令必须拒绝");
    }

    @Test
    void enabled_strongPassword_returnsNormally() {
        DruidMonitorGuard g = guard();
        ReflectionTestUtils.setField(g, "monitorEnabled", true);
        ReflectionTestUtils.setField(g, "loginPassword", "S3cure_P@ss_2026!");
        ReflectionTestUtils.setField(g, "loginUsername", "ops");

        assertDoesNotThrow(g::guard);
    }

    @Test
    void enabled_nullPassword_throws() {
        DruidMonitorGuard g = guard();
        ReflectionTestUtils.setField(g, "monitorEnabled", true);
        ReflectionTestUtils.setField(g, "loginPassword", null);

        assertThrows(IllegalStateException.class, g::guard);
    }
}
