package com.qinghe.mall.util;

/**
 * 分页参数解析工具（P2-12）。
 *
 * 历史上部分端点用 {@code pagination} 作为页码参数名，其余用 {@code pageNum}，
 * 命名分裂导致前端调用易错。现统一以 {@code pageNum} 为准，
 * {@code pagination} 仅作为兼容别名保留（旧调用方不受影响）。
 */
public final class PageParams {

    private PageParams() {
    }

    /**
     * 解析页码：{@code pageNum} 优先；缺省时回退兼容参数 {@code pagination}；
     * 两者均缺失或非法（&lt;1）时返回 1。
     */
    public static int resolve(Integer pageNum, Integer legacyPagination) {
        if (pageNum != null && pageNum >= 1) {
            return pageNum;
        }
        if (legacyPagination != null && legacyPagination >= 1) {
            return legacyPagination;
        }
        return 1;
    }
}
