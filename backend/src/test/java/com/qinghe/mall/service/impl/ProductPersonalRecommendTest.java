package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.config.elasticsearch.ProductIndexService;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.UserAffinityService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 个性化推荐单元测试（v1.13 重构：协作服务直 mock，去 spy 自调用绕行）。
 *
 * 覆盖：未登录/无画像降级热销、品牌偏好命中优先重排（组内保持热销次序）、
 * 已购排除与不足回补、limit 收敛与候选池口径、空池兜底。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductPersonalRecommendTest {

    @Mock
    private UserAffinityService userAffinityService;

    @Mock
    private ProductIndexService productIndexService;

    @Mock
    private ProductDAO productDAO;

    @Mock
    private ProductDetailService productDetailService;

    private ProductServiceImpl service;

    private List<Product> pool;

    @BeforeEach
    void setUp() throws Exception {
        service = new ProductServiceImpl();
        ReflectionTestUtils.setField(service, "userAffinityService", userAffinityService);
        ReflectionTestUtils.setField(service, "productIndexService", productIndexService);
        ReflectionTestUtils.setField(service, "productDAO", productDAO);
        ReflectionTestUtils.setField(service, "productDetailService", productDetailService);

        // 候选池：A 品牌 3 款 + B 品牌 4 款 + C 品牌 3 款（池内为热销次序）
        pool = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Product p = new Product();
            p.setId("p" + i);
            p.setBrand(i <= 3 ? "A" : (i <= 7 ? "B" : "C"));
            pool.add(p);
        }
        stubPool(24, pool);
        stubPool(8, pool); // 降级热销 hotTop(8)
        // 无画像缺省
        when(userAffinityService.preferredBrands(any(), anyInt())).thenReturn(List.of());
        when(userAffinityService.purchasedProductIds(any())).thenReturn(Set.of());
    }

    /** ES 检索桩：真实 queryOnSalePage 直连（无需 spy 自调用绕行） */
    private void stubPool(int pageSize, List<Product> data) throws Exception {
        Paging<Product> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setTotalCount((long) data.size());
        paging.setData(new ArrayList<>(data));
        when(productIndexService.search(isNull(), isNull(), eq("ON"), isNull(), eq("sales_desc"),
                eq(1), eq(pageSize))).thenReturn(paging);
    }

    @Test
    @DisplayName("未登录：降级热销且不查画像")
    void personal_notLogged_fallbackToHot() {
        List<Product> result = service.recommendForUser(null, 8);

        assertEquals(8, result.size());
        assertEquals("p1", result.get(0).getId());
        verify(userAffinityService).preferredBrands(null, 5);
        verify(userAffinityService).purchasedProductIds(null);
    }

    @Test
    @DisplayName("无画像（无偏好且无已购）：降级热销")
    void personal_noProfile_fallbackToHot() {
        List<Product> result = service.recommendForUser(9L, 8);

        assertEquals(8, result.size());
        assertEquals("p1", result.get(0).getId());
    }

    @Test
    @DisplayName("品牌偏好命中优先重排：B 品牌用户 → B 款在前且组内保持热销次序")
    void personal_brandAffinity_reordered() {
        when(userAffinityService.preferredBrands(9L, 5)).thenReturn(List.of("B"));

        List<Product> result = service.recommendForUser(9L, 8);

        for (int i = 0; i < 4; i++) {
            assertEquals("B", result.get(i).getBrand());
            assertEquals("p" + (4 + i), result.get(i).getId());
        }
        assertEquals("A", result.get(4).getBrand());
        assertEquals(8, result.size());
    }

    @Test
    @DisplayName("已购排除：B 款全买过 → 剩余按品牌序，末尾回补已购保持密度")
    void personal_purchasedExcluded_thenBackfilled() {
        when(userAffinityService.preferredBrands(9L, 5)).thenReturn(List.of("B"));
        when(userAffinityService.purchasedProductIds(9L)).thenReturn(Set.of("p4", "p5", "p6", "p7"));

        List<Product> result = service.recommendForUser(9L, 8);

        // 前 6 = 未购的 A/C（热销次序），后 2 = 回补的已购 B 款
        assertEquals("p1", result.get(0).getId());
        assertEquals("p2", result.get(1).getId());
        assertEquals("p3", result.get(2).getId());
        assertEquals("p8", result.get(3).getId());
        assertEquals("p9", result.get(4).getId());
        assertEquals("p10", result.get(5).getId());
        assertEquals("p4", result.get(6).getId());
        assertEquals("p5", result.get(7).getId());
    }

    @Test
    @DisplayName("已购排除 + 品牌命中：未购的偏好品牌置顶")
    void personal_purchasedAndAffinity_combined() {
        when(userAffinityService.preferredBrands(9L, 5)).thenReturn(List.of("A"));
        when(userAffinityService.purchasedProductIds(9L)).thenReturn(Set.of("p1"));

        List<Product> result = service.recommendForUser(9L, 8);

        // p1 已购被排除；A 品牌未购的 p2/p3 置顶
        assertEquals("p2", result.get(0).getId());
        assertEquals("p3", result.get(1).getId());
        assertEquals(8, result.size());
        assertTrue(result.stream().noneMatch(p -> "p1".equals(p.getId())));
    }

    @Test
    @DisplayName("limit 收敛：有画像时候选池按 3×limit 取数")
    void personal_limitClamped() throws Exception {
        when(userAffinityService.preferredBrands(9L, 5)).thenReturn(List.of("B"));
        when(userAffinityService.purchasedProductIds(9L)).thenReturn(Set.of("p4"));
        stubPool(60, pool);

        // size 钳到 20 → 候选池 60，但池内仅 10 款 → 返回全部 10 款
        List<Product> result = service.recommendForUser(9L, 99);

        assertEquals(10, result.size());
        verify(productIndexService).search(isNull(), isNull(), eq("ON"), isNull(), eq("sales_desc"),
                eq(1), eq(60));
    }

    @Test
    @DisplayName("候选池空：返回空列表不抛错")
    void personal_emptyPool_returnsEmpty() throws Exception {
        stubPool(24, new ArrayList<>());
        when(userAffinityService.preferredBrands(9L, 5)).thenReturn(List.of("B"));

        assertTrue(service.recommendForUser(9L, 8).isEmpty());
    }

}
