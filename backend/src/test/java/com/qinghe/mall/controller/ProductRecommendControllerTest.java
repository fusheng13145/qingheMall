package com.qinghe.mall.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 推荐位端点单元测试（D2，v1.7）。
 *
 * 覆盖：hot 场景走销量排序、new 场景走默认上架时间降序、limit 收敛与缺省、
 * 非法场景拒绝、空数据兜底空数组。
 */
class ProductRecommendControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductDetailService productDetailService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Paging<Product> pagingOf(int n) {
        Paging<Product> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setTotalCount((long) n);
        List<Product> data = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Product p = new Product();
            p.setId("p" + i);
            data.add(p);
        }
        paging.setData(data);
        return paging;
    }

    @Test
    @DisplayName("hot 场景：走销量排序并取前 N 条")
    void recommend_hot_usesSalesSort() {
        when(productService.queryOnSalePage(1, 8, null, null, "sales_desc")).thenReturn(pagingOf(2));

        Result<List<Product>> result = productController.recommend("hot", null, new MockHttpServletRequest());

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
        verify(productService).queryOnSalePage(1, 8, null, null, "sales_desc");
    }

    @Test
    @DisplayName("new 场景：走默认上架时间降序（sort=null）")
    void recommend_new_usesDefaultSort() {
        when(productService.queryOnSalePage(1, 6, null, null, null)).thenReturn(pagingOf(1));

        Result<List<Product>> result = productController.recommend("new", 6, new MockHttpServletRequest());

        assertEquals(1, result.getData().size());
        verify(productService).queryOnSalePage(1, 6, null, null, null);
    }

    @Test
    @DisplayName("limit 收敛：小于 1 取 1，大于 20 取 20")
    void recommend_limitClamped() {
        when(productService.queryOnSalePage(1, 1, null, null, "sales_desc")).thenReturn(pagingOf(0));
        when(productService.queryOnSalePage(1, 20, null, null, "sales_desc")).thenReturn(pagingOf(0));

        productController.recommend("hot", 0, new MockHttpServletRequest());
        productController.recommend("hot", 99, new MockHttpServletRequest());

        verify(productService).queryOnSalePage(1, 1, null, null, "sales_desc");
        verify(productService).queryOnSalePage(1, 20, null, null, "sales_desc");
    }

    @Test
    @DisplayName("非法场景拒绝")
    void recommend_invalidScene_rejected() {
        assertThrows(BusinessException.class, () -> productController.recommend("cheap", 8, new MockHttpServletRequest()));
    }

    @Test
    @DisplayName("空数据兜底空数组")
    void recommend_emptyData_returnsEmptyList() {
        Paging<Product> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setData(null);
        when(productService.queryOnSalePage(1, 8, null, null, "sales_desc")).thenReturn(paging);

        Result<List<Product>> result = productController.recommend("hot", null, new MockHttpServletRequest());

        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("personal 场景：会话 userId 透传给服务端个性化排序")
    void recommend_personal_passesSessionUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("userId", 9L);

        productController.recommend("personal", 8, request);

        verify(productService).recommendForUser(9L, 8);
    }

    @Test
    @DisplayName("personal 场景：未登录 userId 为 null 透传（服务端降级热销）")
    void recommend_personal_anonymous_nullUserId() {
        productController.recommend("personal", 8, new MockHttpServletRequest());

        verify(productService).recommendForUser(null, 8);
    }
}
