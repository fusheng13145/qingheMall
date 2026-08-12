package com.qinghe.mall.config;

import com.alibaba.fastjson2.JSON;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * ProductCacheService 分支覆盖补充（T3 续补）。
 * 覆盖：getProduct/putProduct/getDetails/putDetails/evict 的正常、null 与 Redis 异常降级分支。
 */
@ExtendWith(MockitoExtension.class)
class ProductCacheServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RMapCache<String, String> mapCache;

    private ProductCacheService service;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().doReturn(mapCache).when(redissonClient).getMapCache(anyString());
        service = new ProductCacheService();
        // 注入 mock 的 redissonClient 字段
        org.springframework.test.util.ReflectionTestUtils.setField(service, "redissonClient", redissonClient);
    }

    @Test
    void getProduct_cacheMiss_returnsNull() {
        when(mapCache.get(any())).thenReturn(null);
        assertNull(service.getProduct("p1"));
    }

    @Test
    void getProduct_cacheHit_returnsProduct() {
        Product p = new Product();
        p.setId("p1");
        when(mapCache.get(eq("product:p1"))).thenReturn(JSON.toJSONString(p));
        Product got = service.getProduct("p1");
        assertEquals("p1", got.getId());
    }

    @Test
    void getProduct_redisException_fallsThrough() {
        when(mapCache.get(any())).thenThrow(new RuntimeException("redis down"));
        assertNull(service.getProduct("p1"));
    }

    @Test
    void putProduct_nullOrIdNull_skipped() {
        service.putProduct(null);
        Product p = new Product(); // id == null
        service.putProduct(p);
    }

    @Test
    void putProduct_normal_puts() {
        Product p = new Product();
        p.setId("p1");
        service.putProduct(p);
    }

    @Test
    void putProduct_redisException_swallowed() {
        Product p = new Product();
        p.setId("p1");
        doThrow(new RuntimeException("boom")).when(mapCache).put(any(), any(), anyLong(), any());
        service.putProduct(p);
    }

    @Test
    void getDetails_cacheMiss_returnsNull() {
        when(mapCache.get(any())).thenReturn(null);
        assertNull(service.getDetails("p1"));
    }

    @Test
    void getDetails_cacheHit_returnsList() {
        List<ProductDetail> details = Collections.singletonList(new ProductDetail());
        when(mapCache.get(eq("details:p1"))).thenReturn(JSON.toJSONString(details));
        List<ProductDetail> got = service.getDetails("p1");
        assertEquals(1, got.size());
    }

    @Test
    void getDetails_redisException_fallsThrough() {
        when(mapCache.get(any())).thenThrow(new RuntimeException("redis down"));
        assertNull(service.getDetails("p1"));
    }

    @Test
    void putDetails_normal_puts() {
        service.putDetails("p1", Collections.singletonList(new ProductDetail()));
    }

    @Test
    void putDetails_redisException_swallowed() {
        doThrow(new RuntimeException("boom")).when(mapCache).put(any(), any(), anyLong(), any());
        service.putDetails("p1", Collections.singletonList(new ProductDetail()));
    }

    @Test
    void evict_normal_removes() {
        service.evict("p1");
    }

    @Test
    void evict_redisException_swallowed() {
        doThrow(new RuntimeException("boom")).when(mapCache).fastRemove(any());
        service.evict("p1");
    }
}
