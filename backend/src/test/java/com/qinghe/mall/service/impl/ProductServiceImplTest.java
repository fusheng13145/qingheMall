package com.qinghe.mall.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghe.mall.config.ProductCacheService;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.dataobject.ProductDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 商品服务单元测试（原零覆盖）。
 *
 * 覆盖：三种分页查询、缓存命中/回源回填、findByIds 空/非空、
 * add 默认值、update 不存在/成功、saveWithDetails 新增/更新、delete 成败。
 */
class ProductServiceImplTest {

    private ProductServiceImpl service;
    private ProductDAO productDAO;
    private ProductDetailService productDetailService;
    private ProductCacheService cacheService;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl();
        productDAO = org.mockito.Mockito.mock(ProductDAO.class);
        productDetailService = org.mockito.Mockito.mock(ProductDetailService.class);
        cacheService = org.mockito.Mockito.mock(ProductCacheService.class);
        ReflectionTestUtils.setField(service, "productDAO", productDAO);
        ReflectionTestUtils.setField(service, "productDetailService", productDetailService);
        ReflectionTestUtils.setField(service, "productCacheService", cacheService);
    }

    @AfterEach
    void tearDown() {
        PageHelper.clearPage();
    }

    private ProductDO productDO(String id, String name) {
        ProductDO do_ = new ProductDO();
        do_.setId(id);
        do_.setName(name);
        do_.setBrand("Nike");
        do_.setStatus("ON");
        do_.setPrice(new BigDecimal("299.00"));
        do_.setPurchaseNum(10);
        return do_;
    }

    private Product product(String id) {
        Product p = new Product();
        p.setId(id);
        p.setName("商品" + id);
        p.setBrand("Nike");
        p.setStatus("ON");
        p.setPrice(new BigDecimal("299.00"));
        return p;
    }

    // ============ 分页查询 ============

    @Test
    @DisplayName("queryPage 分页参数透传与字段回填")
    void queryPagePaginates() {
        List<ProductDO> list = new ArrayList<>();
        list.add(productDO("p1", "A"));
        list.add(productDO("p2", "B"));
        when(productDAO.queryAll(anyString(), anyString(), anyString(), any())).thenReturn(list);

        Paging<Product> paging = service.queryPage(1, 10, "鞋", "Nike", "price_asc");

        // 纯 mock 下 PageHelper 不填充 result（依赖 MyBatis 拦截器，由集成测试验证），
        // 此处验证 Service 层分页参数透传与字段回填
        assertThat(paging.getPageNum()).isEqualTo(1);
        assertThat(paging.getPageSize()).isEqualTo(10);
        verify(productDAO).queryAll("鞋", "Nike", "price_asc", null);
    }

    @Test
    @DisplayName("queryOnSalePage 只查上架商品（status=ON）")
    void queryOnSalePageFiltersOn() {
        List<ProductDO> list = new ArrayList<>();
        list.add(productDO("p1", "A"));
        when(productDAO.queryAll(anyString(), anyString(), anyString(), any())).thenReturn(list);

        Paging<Product> paging = service.queryOnSalePage(1, 20, null, null, null);

        assertThat(paging.getPageNum()).isEqualTo(1);
        assertThat(paging.getPageSize()).isEqualTo(20);
        verify(productDAO).queryAll(null, null, null, "ON");
    }

    @Test
    @DisplayName("queryMerchantPage 按商家+状态查询")
    void queryMerchantPageByMerchant() {
        List<ProductDO> list = new ArrayList<>();
        list.add(productDO("p1", "A"));
        when(productDAO.queryByMerchantId(any(), anyString(), anyString())).thenReturn(list);

        Paging<Product> paging = service.queryMerchantPage(42L, "鞋", "ON", 1, 10);

        assertThat(paging.getPageNum()).isEqualTo(1);
        assertThat(paging.getPageSize()).isEqualTo(10);
        verify(productDAO).queryByMerchantId(42L, "鞋", "ON");
    }

    @Test
    @DisplayName("listBrands 透传 DAO")
    void listBrandsDelegates() {
        when(productDAO.listBrands()).thenReturn(List.of("Nike", "Adidas"));
        assertThat(service.listBrands()).containsExactly("Nike", "Adidas");
    }

    // ============ findById / findByIds ============

    @Test
    @DisplayName("findById 缓存命中直接返回")
    void findByIdCacheHit() {
        Product cached = product("p1");
        when(cacheService.getProduct("p1")).thenReturn(cached);

        Product result = service.findById("p1");

        assertThat(result).isSameAs(cached);
        verify(productDAO, never()).findById(anyString());
    }

    @Test
    @DisplayName("findById 缓存未命中回源 DB 并回填缓存")
    void findByIdCacheMissRefills() {
        when(cacheService.getProduct("p1")).thenReturn(null);
        when(productDAO.findById("p1")).thenReturn(productDO("p1", "A"));

        Product result = service.findById("p1");

        assertThat(result.getName()).isEqualTo("A");
        verify(cacheService).putProduct(any(Product.class));
    }

    @Test
    @DisplayName("findById 缓存与 DB 均无返回 null")
    void findByIdNotFound() {
        when(cacheService.getProduct("p1")).thenReturn(null);
        when(productDAO.findById("p1")).thenReturn(null);

        assertThat(service.findById("p1")).isNull();
        verify(cacheService, never()).putProduct(any(Product.class));
    }

    @Test
    @DisplayName("findByIds 空入参返回空列表不查库")
    void findByIdsEmptyInput() {
        assertThat(service.findByIds(null)).isEmpty();
        assertThat(service.findByIds(Collections.emptyList())).isEmpty();
        verify(productDAO, never()).findByIds(anyList());
    }

    @Test
    @DisplayName("findByIds 非空转换结果")
    void findByIdsConverts() {
        when(productDAO.findByIds(List.of("p1"))).thenReturn(List.of(productDO("p1", "A")));
        assertThat(service.findByIds(List.of("p1"))).hasSize(1);
    }

    // ============ add / update ============

    @Test
    @DisplayName("add 生成 id、填充默认 status/purchaseNum 并驱逐缓存")
    void addFillsDefaults() {
        when(productDAO.insert(any(ProductDO.class))).thenAnswer(inv -> 1);
        Product input = product(null);
        input.setStatus(null);
        input.setPurchaseNum(null);

        Product created = service.add(input);

        assertThat(created.getId()).isNotBlank();
        assertThat(created.getStatus()).isEqualTo("ON");
        assertThat(created.getPurchaseNum()).isZero();
        verify(cacheService).evict(created.getId());
    }

    @Test
    @DisplayName("update 商品不存在抛异常")
    void updateMissingThrows() {
        when(productDAO.findById("p1")).thenReturn(null);

        assertThatThrownBy(() -> service.update(product("p1")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("商品不存在");
    }

    @Test
    @DisplayName("update 成功更新并驱逐缓存")
    void updateSuccessEvicts() {
        when(productDAO.findById("p1")).thenReturn(productDO("p1", "A"));
        when(productDAO.update(any(ProductDO.class))).thenAnswer(inv -> 1);
        when(productDAO.findById("p1")).thenReturn(productDO("p1", "B"));

        Product updated = service.update(product("p1"));

        assertThat(updated.getName()).isEqualTo("B");
        verify(cacheService).evict("p1");
    }

    // ============ saveWithDetails ============

    @Test
    @DisplayName("saveWithDetails 新增：add 生成 id 后替换 SKU")
    void saveWithDetailsNewProduct() {
        when(productDAO.insert(any(ProductDO.class))).thenAnswer(inv -> 1);
        when(cacheService.getProduct(anyString())).thenReturn(null);
        when(productDAO.findById(anyString())).thenAnswer(inv -> productDO(inv.getArgument(0), "A"));
        List<ProductDetail> details = List.of(new ProductDetail());

        Product saved = service.saveWithDetails(product(null), details);

        assertThat(saved.getId()).isNotBlank();
        verify(productDetailService).replaceByProductId(anyString(), anyList());
    }

    @Test
    @DisplayName("saveWithDetails 更新：id 存在走 update")
    void saveWithDetailsExisting() {
        when(productDAO.findById("p1")).thenReturn(productDO("p1", "A"));
        when(productDAO.update(any(ProductDO.class))).thenAnswer(inv -> 1);
        when(cacheService.getProduct("p1")).thenReturn(null);
        List<ProductDetail> details = new ArrayList<>();

        Product saved = service.saveWithDetails(product("p1"), details);

        assertThat(saved.getId()).isEqualTo("p1");
        verify(productDetailService).replaceByProductId("p1", details);
        verify(productDAO).update(any(ProductDO.class));
    }

    // ============ delete ============

    @Test
    @DisplayName("delete 成功驱逐缓存")
    void deleteSuccessEvicts() {
        when(productDAO.deleteById("p1")).thenReturn(1);
        assertThat(service.delete("p1")).isTrue();
        verify(cacheService).evict("p1");
    }

    @Test
    @DisplayName("delete 失败不驱逐缓存")
    void deleteFailureNoEvict() {
        when(productDAO.deleteById("p1")).thenReturn(0);
        assertThat(service.delete("p1")).isFalse();
        verify(cacheService, never()).evict("p1");
    }
}
