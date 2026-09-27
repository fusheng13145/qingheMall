package com.qinghe.mall.config.elasticsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * ES 排序构建分支补测（v1.12，复盘 §11.7 清单）。
 *
 * 覆盖：price_asc / price_desc / sales_desc / 默认（gmtModified 降序）四分支。
 */
class ProductIndexBuildSortTest {

    private List<SortOptions> build(String sort) {
        ProductIndexService service = new ProductIndexService();
        return (List<SortOptions>) ReflectionTestUtils.invokeMethod(service, "buildSort", sort);
    }

    private String fieldOf(SortOptions o) {
        return o.field().field();
    }

    private SortOrder orderOf(SortOptions o) {
        return o.field().order();
    }

    @Test
    @DisplayName("price_asc：价格升序")
    void buildSort_priceAsc() {
        List<SortOptions> sorts = build("price_asc");
        assertEquals(1, sorts.size());
        assertEquals("price", fieldOf(sorts.get(0)));
        assertEquals(SortOrder.Asc, orderOf(sorts.get(0)));
    }

    @Test
    @DisplayName("price_desc：价格降序")
    void buildSort_priceDesc() {
        List<SortOptions> sorts = build("price_desc");
        assertEquals("price", fieldOf(sorts.get(0)));
        assertEquals(SortOrder.Desc, orderOf(sorts.get(0)));
    }

    @Test
    @DisplayName("sales_desc：销量降序")
    void buildSort_salesDesc() {
        List<SortOptions> sorts = build("sales_desc");
        assertEquals("purchaseNum", fieldOf(sorts.get(0)));
        assertEquals(SortOrder.Desc, orderOf(sorts.get(0)));
    }

    @Test
    @DisplayName("默认与未知排序：上架时间降序")
    void buildSort_defaultAndUnknown() {
        for (String sort : new String[]{null, "", "unknown"}) {
            List<SortOptions> sorts = build(sort);
            assertEquals(1, sorts.size());
            assertEquals("gmtModified", fieldOf(sorts.get(0)));
            assertEquals(SortOrder.Desc, orderOf(sorts.get(0)));
        }
        assertTrue(true);
    }
}
