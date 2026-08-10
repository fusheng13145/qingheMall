package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.config.ProductCacheService;
import com.qinghe.mall.dao.ProductDetailDAO;
import com.qinghe.mall.dataobject.ProductDetailDO;
import com.qinghe.mall.model.ProductDetail;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 商品规格服务单元测试（原 63%，补齐缺口）。
 *
 * 覆盖：findByProductId 缓存命中/回源回填、findById/Ids、updateStock/
 * decreaseStock/increaseStock 成败与缓存驱逐、replaceByProductId 全校验。
 */
@ExtendWith(MockitoExtension.class)
class ProductDetailServiceImplTest {

    @Mock
    private ProductDetailDAO productDetailDAO;
    @Mock
    private ProductCacheService cacheService;

    @InjectMocks
    private ProductDetailServiceImpl service;

    private ProductDetailDO detailDO(String id) {
        ProductDetailDO d = new ProductDetailDO();
        d.setId(id);
        d.setProductId("p1");
        d.setPrice(new BigDecimal("299.00"));
        d.setStock(10);
        return d;
    }

    // ============ findByProductId ============

    @Test
    @DisplayName("findByProductId 缓存命中直接返回")
    void findByProductId_cacheHit() {
        List<ProductDetail> cached = List.of(new ProductDetail());
        when(cacheService.getDetails("p1")).thenReturn(cached);

        List<ProductDetail> result = service.findByProductId("p1");

        assertEquals(cached, result);
        verify(productDetailDAO, never()).findByProductId(anyString());
    }

    @Test
    @DisplayName("findByProductId 缓存未命中回源并回填")
    void findByProductId_cacheMissRefills() {
        when(cacheService.getDetails("p1")).thenReturn(null);
        when(productDetailDAO.findByProductId("p1")).thenReturn(List.of(detailDO("d1")));

        List<ProductDetail> result = service.findByProductId("p1");

        assertEquals(1, result.size());
        verify(cacheService).putDetails("p1", result);
    }

    // ============ findById / findByIds ============

    @Test
    @DisplayName("findById 存在/缺失")
    void findById() {
        when(productDetailDAO.findById("d1")).thenReturn(detailDO("d1"));
        when(productDetailDAO.findById("d-x")).thenReturn(null);

        assertNotNull(service.findById("d1"));
        assertNull(service.findById("d-x"));
    }

    @Test
    @DisplayName("findByIds 空入参不查库")
    void findByIds_empty() {
        assertTrue(service.findByIds(null).isEmpty());
        assertTrue(service.findByIds(new ArrayList<>()).isEmpty());
        verify(productDetailDAO, never()).findByIds(anyList());
    }

    // ============ 库存操作 ============

    @Test
    @DisplayName("updateStock 成功驱逐所属商品缓存")
    void updateStock_success_evicts() {
        when(productDetailDAO.updateStock("d1", 5)).thenReturn(1);
        when(productDetailDAO.findById("d1")).thenReturn(detailDO("d1"));

        assertTrue(service.updateStock("d1", 5));
        verify(cacheService).evict("p1");
    }

    @Test
    @DisplayName("updateStock 未命中不驱逐缓存")
    void updateStock_noMatch_noEvict() {
        when(productDetailDAO.updateStock("d1", 5)).thenReturn(0);
        assertFalse(service.updateStock("d1", 5));
        verify(cacheService, never()).evict(anyString());
    }

    @Test
    @DisplayName("decreaseStock 数量缺省按 1")
    void decreaseStock_defaultQty() {
        when(productDetailDAO.decreaseStock("d1", 1)).thenReturn(1);
        when(productDetailDAO.findById("d1")).thenReturn(detailDO("d1"));

        assertTrue(service.decreaseStock("d1", null));
        verify(productDetailDAO).decreaseStock("d1", 1);
        verify(cacheService).evict("p1");
    }

    @Test
    @DisplayName("increaseStock 成功驱逐")
    void increaseStock_success_evicts() {
        when(productDetailDAO.increaseStock("d1", 3)).thenReturn(1);
        when(productDetailDAO.findById("d1")).thenReturn(detailDO("d1"));

        assertTrue(service.increaseStock("d1", 3));
        verify(cacheService).evict("p1");
    }

    // ============ replaceByProductId ============

    @Test
    @DisplayName("replaceByProductId 空规格仅删旧并清缓存")
    void replace_empty_clears() {
        service.replaceByProductId("p1", null);

        verify(productDetailDAO).deleteByProductId("p1");
        verify(productDetailDAO, never()).insert(any(ProductDetailDO.class));
        verify(cacheService).evict("p1");
    }

    @Test
    @DisplayName("replaceByProductId 规格缺价格/库存拒绝")
    void replace_incomplete_throws() {
        ProductDetail bad = new ProductDetail();
        bad.setPrice(null);

        assertThrows(RuntimeException.class, () -> service.replaceByProductId("p1", List.of(bad)),
                "规格信息不完整");
    }

    @Test
    @DisplayName("replaceByProductId 正常替换并清缓存")
    void replace_success() {
        when(productDetailDAO.insert(any(ProductDetailDO.class))).thenReturn(1);
        ProductDetail d = new ProductDetail();
        d.setPrice(new BigDecimal("99.00"));
        d.setStock(5);
        d.setSize(38.5);

        service.replaceByProductId("p1", List.of(d));

        verify(productDetailDAO).deleteByProductId("p1");
        verify(productDetailDAO).insert(any(ProductDetailDO.class));
        verify(cacheService).evict("p1");
    }
}
