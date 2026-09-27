package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.service.ProductDetailService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * 个性化推荐（v1.10）单元测试。
 *
 * 覆盖：未登录降级热销、无购买历史降级热销、品牌偏好命中优先重排（组内保持热销次序）、
 * limit 收敛与候选池取数口径（3×limit）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductPersonalRecommendTest {

    @Mock
    private OrderDAO orderDAO;

    @Mock
    private ProductDetailService productDetailService;

    private ProductServiceImpl service;

    private List<Product> pool;

    @BeforeEach
    void setUp() {
        ProductServiceImpl real = new ProductServiceImpl();
        ReflectionTestUtils.setField(real, "orderDAO", orderDAO);
        ReflectionTestUtils.setField(real, "productDetailService", productDetailService);
        service = org.mockito.Mockito.spy(real);

        // 候选池：A 品牌 3 款 + B 品牌 4 款 + C 品牌 3 款（池内为热销次序）
        pool = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Product p = new Product();
            p.setId("p" + i);
            p.setBrand(i <= 3 ? "A" : (i <= 7 ? "B" : "C"));
            pool.add(p);
        }
        doReturn(pagingOf(pool)).when(service)
                .queryOnSalePage(eq(1), eq(24), eq(null), eq(null), eq("sales_desc"));
    }

    private Paging<Product> pagingOf(List<Product> data) {
        Paging<Product> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setTotalCount((long) data.size());
        paging.setData(new ArrayList<>(data));
        return paging;
    }

    private Map<String, Object> affinity(String brand, int count) {
        Map<String, Object> m = new HashMap<>();
        m.put("brand", brand);
        m.put("orderCount", count);
        return m;
    }

    @Test
    @DisplayName("未登录：降级热销且不查品牌偏好")
    void personal_notLogged_fallbackToHot() {
        List<Product> result = service.recommendForUser(null, 8);

        assertEquals(8, result.size());
        assertEquals("p1", result.get(0).getId());
        verify(orderDAO, never()).brandAffinity(any(), eq(5));
    }

    @Test
    @DisplayName("无购买历史：降级热销")
    void personal_noHistory_fallbackToHot() {
        when(orderDAO.brandAffinity(9L, 5)).thenReturn(new ArrayList<>());

        List<Product> result = service.recommendForUser(9L, 8);

        assertEquals(8, result.size());
        assertEquals("p1", result.get(0).getId());
    }

    @Test
    @DisplayName("品牌偏好命中优先重排：B 品牌用户 → B 款在前且组内保持热销次序")
    void personal_brandAffinity_reordered() {
        when(orderDAO.brandAffinity(9L, 5))
                .thenReturn(List.of(affinity("B", 4)));

        List<Product> result = service.recommendForUser(9L, 8);

        // 前 4 位全部为 B 品牌（池内顺序 p4~p7），其后为其余品牌按热销次序
        for (int i = 0; i < 4; i++) {
            assertEquals("B", result.get(i).getBrand());
            assertEquals("p" + (4 + i), result.get(i).getId());
        }
        assertEquals("A", result.get(4).getBrand());
        assertEquals(8, result.size());
    }

    @Test
    @DisplayName("limit 收敛：候选池按 3×limit 取数，结果不超池大小")
    void personal_limitClamped() {
        when(orderDAO.brandAffinity(9L, 5)).thenReturn(new ArrayList<>());
        doReturn(pagingOf(pool)).when(service)
                .queryOnSalePage(eq(1), eq(60), eq(null), eq(null), eq("sales_desc"));

        // size 钳到 20 → 候选池 60，但池内仅 10 款 → 返回全部 10 款
        List<Product> result = service.recommendForUser(9L, 99);

        assertEquals(10, result.size());
        verify(service).queryOnSalePage(eq(1), eq(60), eq(null), eq(null), eq("sales_desc"));
    }

    @Test
    @DisplayName("候选池空：返回空列表不抛错")
    void personal_emptyPool_returnsEmpty() {
        doReturn(pagingOf(new ArrayList<>())).when(service)
                .queryOnSalePage(eq(1), eq(24), eq(null), eq(null), eq("sales_desc"));
        when(orderDAO.brandAffinity(9L, 5)).thenReturn(List.of(affinity("B", 1)));

        assertTrue(service.recommendForUser(9L, 8).isEmpty());
    }
}
