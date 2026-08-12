package com.qinghe.mall.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.config.ProductCacheService;
import com.qinghe.mall.config.elasticsearch.ProductIndexService;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.dataobject.ProductDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.service.ProductDetailService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * ProductServiceImpl 的 ES 搜索集成路径测试（优先级 + 降级双路径）。
 * 覆盖：queryOnSalePage / queryMerchantPage 优先走 ES、ES 异常时降级 MySQL（status=ON 透传）。
 */
class ProductServiceImplEsTest {

    private ProductServiceImpl service;
    private ProductDAO productDAO;
    private ProductIndexService productIndexService;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl();
        productDAO = mock(ProductDAO.class);
        productIndexService = mock(ProductIndexService.class);
        ProductDetailService detailService = mock(ProductDetailService.class);
        ProductCacheService cache = mock(ProductCacheService.class);
        ReflectionTestUtils.setField(service, "productDAO", productDAO);
        ReflectionTestUtils.setField(service, "productDetailService", detailService);
        ReflectionTestUtils.setField(service, "productCacheService", cache);
        ReflectionTestUtils.setField(service, "productIndexService", productIndexService);
    }

    private Paging<Product> esPaging(int total) {
        Paging<Product> p = new Paging<>();
        p.setPageNum(1);
        p.setPageSize(10);
        p.setTotalCount((long) total);
        p.setTotalPage(1);
        List<Product> list = new ArrayList<>();
        Product prod = new Product();
        prod.setId("es1");
        prod.setName("ES商品");
        list.add(prod);
        p.setData(list);
        return p;
    }

    @Test
    @DisplayName("queryOnSalePage 优先走 ES 搜索，不查 MySQL")
    void queryOnSalePageUsesEs() throws Exception {
        doReturn(esPaging(1)).when(productIndexService)
                .search(any(), any(), any(), any(), any(), anyInt(), anyInt());

        Paging<Product> result = service.queryOnSalePage(1, 10, "手机", "Nike", "price_asc");

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getData().get(0).getId()).isEqualTo("es1");
        verify(productIndexService).search(eq("手机"), eq("Nike"), eq("ON"), any(), eq("price_asc"), eq(1), eq(10));
        verify(productDAO, never()).queryAll(any(), any(), any(), any());
    }

    @Test
    @DisplayName("queryOnSalePage ES 异常降级 MySQL（status=ON 透传）")
    void queryOnSalePageFallsBackToMysql() throws Exception {
        doThrow(new RuntimeException("ES down")).when(productIndexService)
                .search(any(), any(), any(), any(), any(), anyInt(), anyInt());
        when(productDAO.queryAll(any(), any(), any(), any())).thenReturn(new ArrayList<>());

        Paging<Product> result = service.queryOnSalePage(1, 10, "手机", "Nike", null);

        assertThat(result).isNotNull();
        verify(productDAO).queryAll(eq("手机"), eq("Nike"), eq((String) null), eq("ON"));
    }

    @Test
    @DisplayName("queryMerchantPage 优先走 ES 并按 merchantId 过滤，不查 MySQL")
    void queryMerchantPageUsesEs() throws Exception {
        doReturn(esPaging(2)).when(productIndexService)
                .search(any(), any(), any(), any(), any(), anyInt(), anyInt());

        Paging<Product> result = service.queryMerchantPage(42L, "鞋", "ON", 1, 10);

        assertThat(result.getTotalCount()).isEqualTo(2);
        verify(productIndexService).search(eq("鞋"), any(), eq("ON"), eq(42L), any(), eq(1), eq(10));
        verify(productDAO, never()).queryByMerchantId(any(), any(), any());
    }
}
