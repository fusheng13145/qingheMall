package com.qinghe.mall.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 分页参数解析测试（P2-12：pageNum 统一 + pagination 兼容）。
 */
class PageParamsTest {

    @Test
    @DisplayName("pageNum 优先")
    void pageNumTakesPrecedence() {
        assertEquals(3, PageParams.resolve(3, 7));
    }

    @Test
    @DisplayName("pageNum 缺省时回退兼容参数 pagination")
    void fallbackToLegacyPagination() {
        assertEquals(5, PageParams.resolve(null, 5));
    }

    @Test
    @DisplayName("两者均缺省时默认第 1 页")
    void defaultsToFirstPage() {
        assertEquals(1, PageParams.resolve(null, null));
    }

    @Test
    @DisplayName("非法页码（<1）回退为 1")
    void invalidValuesFallback() {
        assertEquals(1, PageParams.resolve(0, -2));
        assertEquals(1, PageParams.resolve(-1, null));
    }

    @Test
    @DisplayName("pageNum 非法但 pagination 合法时取后者")
    void invalidPageNumFallsBackToLegacy() {
        assertEquals(4, PageParams.resolve(0, 4));
    }
}
