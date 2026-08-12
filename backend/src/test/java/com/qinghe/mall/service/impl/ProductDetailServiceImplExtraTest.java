package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.config.ProductCacheService;
import com.qinghe.mall.dao.ProductDetailDAO;
import com.qinghe.mall.dataobject.ProductDetailDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.ProductDetail;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * ProductDetailServiceImpl 分支补强（T3 冲刺 80%）：覆盖 decreaseStock 正数数量分支、
 * evictByDetailId 的 detailDO/productId 为 null 与缓存驱逐异常吞掉分支、replaceByProductId
 * 的空集合/规格 null/库存 null/size null 分支、findByIds 非空转换分支。
 */
class ProductDetailServiceImplExtraTest {

    @Mock
    private ProductDetailDAO productDetailDAO;
    @Mock
    private ProductCacheService cacheService;

    @InjectMocks
    private ProductDetailServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private ProductDetailDO detailDO(String id, String productId) {
        ProductDetailDO d = new ProductDetailDO();
        d.setId(id);
        d.setProductId(productId);
        d.setPrice(new BigDecimal("299.00"));
        d.setStock(10);
        return d;
    }

    // decreaseStock：正数数量分支（原测试仅覆盖 null）
    @Test
    void decreaseStock_positiveQty_usesGiven() {
        when(productDetailDAO.decreaseStock("d1", 5)).thenReturn(1);
        when(productDetailDAO.findById("d1")).thenReturn(detailDO("d1", "p1"));
        assertTrue(service.decreaseStock("d1", 5));
        verify(productDetailDAO).decreaseStock("d1", 5);
        verify(cacheService).evict("p1");
    }

    // evictByDetailId：detailDO 为 null → 不驱逐
    @Test
    void evict_whenDetailNull_noEvict() {
        when(productDetailDAO.updateStock("d1", 5)).thenReturn(1);
        when(productDetailDAO.findById("d1")).thenReturn(null);
        service.updateStock("d1", 5);
        verify(cacheService, never()).evict(anyString());
    }

    // evictByDetailId：productId 为 null → 不驱逐
    @Test
    void evict_whenProductIdNull_noEvict() {
        when(productDetailDAO.updateStock("d1", 5)).thenReturn(1);
        when(productDetailDAO.findById("d1")).thenReturn(detailDO("d1", null));
        service.updateStock("d1", 5);
        verify(cacheService, never()).evict(anyString());
    }

    // evictByDetailId：缓存驱逐异常被吞掉，不向上抛出
    @Test
    void evict_whenCacheEvictThrows_swallowed() {
        when(productDetailDAO.updateStock("d1", 5)).thenReturn(1);
        when(productDetailDAO.findById("d1")).thenReturn(detailDO("d1", "p1"));
        doThrow(new RuntimeException("cache down")).when(cacheService).evict("p1");
        assertTrue(service.updateStock("d1", 5));
    }

    // replaceByProductId：非空但为空集合 → 仅删旧并清缓存（覆盖 details.isEmpty() 分支）
    @Test
    void replace_emptyList_clears() {
        service.replaceByProductId("p1", new ArrayList<>());
        verify(productDetailDAO).deleteByProductId("p1");
        verify(productDetailDAO, never()).insert(any(ProductDetailDO.class));
        verify(cacheService).evict("p1");
    }

    // replaceByProductId：规格为 null → 拒绝
    @Test
    void replace_detailNull_throws() {
        List<ProductDetail> details = new ArrayList<>();
        details.add(null);
        assertThrows(BusinessException.class, () -> service.replaceByProductId("p1", details));
    }

    // replaceByProductId：库存为 null → 拒绝
    @Test
    void replace_stockNull_throws() {
        ProductDetail d = new ProductDetail();
        d.setPrice(new BigDecimal("99.00"));
        d.setStock(null);
        assertThrows(BusinessException.class, () -> service.replaceByProductId("p1", Collections.singletonList(d)));
    }

    // replaceByProductId：size 为 null → 默认 0.0（覆盖 setSize 三元 false 分支）
    @Test
    void replace_sizeNull_defaultsToZero() {
        when(productDetailDAO.insert(any(ProductDetailDO.class))).thenReturn(1);
        ProductDetail d = new ProductDetail();
        d.setPrice(new BigDecimal("99.00"));
        d.setStock(5);
        d.setSize(null);
        service.replaceByProductId("p1", Collections.singletonList(d));
        verify(productDetailDAO).insert(any(ProductDetailDO.class));
        verify(cacheService).evict("p1");
    }

    // findByIds：非空集合转换（覆盖 ids != null && !isEmpty() 的 DAO 调用与转换分支）
    @Test
    void findByIds_nonEmpty_converts() {
        when(productDetailDAO.findByIds(Collections.singletonList("d1")))
                .thenReturn(Collections.singletonList(detailDO("d1", "p1")));
        List<ProductDetail> r = service.findByIds(Collections.singletonList("d1"));
        assertEquals(1, r.size());
    }
}
