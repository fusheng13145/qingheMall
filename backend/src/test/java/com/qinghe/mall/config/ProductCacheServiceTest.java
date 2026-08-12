package com.qinghe.mall.config;

import com.alibaba.fastjson2.JSON;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
        // 注入真实 MeterRegistry 并手动触发 @PostConstruct 的指标注册（单元测试不走 Spring 容器）
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "meterRegistry", registry);
        service.init();
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

    @Test
    void metrics_hitAndMissCounted() {
        // 独立 registry，避免与 setUp 的 registry 串扰
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "meterRegistry", reg);
        service.init();

        // 两次未命中：getProduct miss + getDetails miss
        when(mapCache.get(any())).thenReturn(null);
        service.getProduct("p1");
        service.getDetails("p1");

        // 一次命中：product 回填后再次读取
        Product p = new Product();
        p.setId("p1");
        when(mapCache.get(eq("product:p1"))).thenReturn(JSON.toJSONString(p));
        service.getProduct("p1");

        // 命中/未命中本地计数
        assertEquals(1L, service.getHits());
        assertEquals(2L, service.getMisses());

        // Prometheus 计数器（经 MeterRegistry 暴露）
        double hitsMetric = reg.find("qinghe.product.cache.hits").counter().count();
        double missesMetric = reg.find("qinghe.product.cache.misses").counter().count();
        assertEquals(1.0, hitsMetric, 0.0001);
        assertEquals(2.0, missesMetric, 0.0001);

        // 命中率 Gauge ≈ 1/3
        double ratio = reg.find("qinghe.product.cache.hitratio").gauge().value();
        assertEquals(1.0 / 3.0, ratio, 0.0001);
    }
}
