package com.qinghe.mall.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CommonUtils 单元测试（T3 分支覆盖补测）。
 * 覆盖 isEmpty（null / 空白 / 空串 / 非空）、isNull（null / 非 null）、md5 已知值。
 */
class CommonUtilsTest {

    @Test
    @DisplayName("isEmpty: null 视为空")
    void isEmpty_null() {
        assertTrue(CommonUtils.isEmpty(null));
    }

    @Test
    @DisplayName("isEmpty: 纯空白视为空")
    void isEmpty_blank() {
        assertTrue(CommonUtils.isEmpty("   \t "));
    }

    @Test
    @DisplayName("isEmpty: 空串视为空")
    void isEmpty_empty() {
        assertTrue(CommonUtils.isEmpty(""));
    }

    @Test
    @DisplayName("isEmpty: 非空串视为非空")
    void isEmpty_notEmpty() {
        assertFalse(CommonUtils.isEmpty("abc"));
    }

    @Test
    @DisplayName("isNull: null 为真")
    void isNull_null() {
        assertTrue(CommonUtils.isNull(null));
    }

    @Test
    @DisplayName("isNull: 非 null 为假")
    void isNull_notNull() {
        assertFalse(CommonUtils.isNull("x"));
    }

    @Test
    @DisplayName("md5: 已知明文得到预期大写摘要")
    void md5_knownValue() {
        assertEquals("098F6BCD4621D373CADE4E832627B4F6", CommonUtils.md5("test"));
    }
}
