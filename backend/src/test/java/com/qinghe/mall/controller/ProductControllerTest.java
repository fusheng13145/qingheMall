package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 商品控制器 单元测试（遗留D：前台公开读接口）。
 */
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductDetailService productDetailService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
    }

    @Test
    void page_query_returnsPaging() throws Exception {
        Paging<Product> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setTotalCount(0L);
        paging.setData(new ArrayList<>());
        when(productService.queryOnSalePage(1, 10, null, null, null)).thenReturn(paging);

        mockMvc.perform(get("/api/product/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void page_pageSizeOutOfRange_fallsBackToDefault() throws Exception {
        Paging<Product> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setTotalCount(0L);
        paging.setData(new ArrayList<>());
        when(productService.queryOnSalePage(1, 10, null, null, null)).thenReturn(paging);

        // pageSize=999 越界 → 回退默认 10
        mockMvc.perform(get("/api/product/page").param("pageSize", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void get_productFound_returnsProduct() throws Exception {
        Product product = new Product();
        product.setId("p001");
        product.setName("测试商品");
        when(productService.findById("p001")).thenReturn(product);

        mockMvc.perform(get("/api/product/get").param("productId", "p001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试商品"));
    }

    @Test
    void get_productNotFound_fail() throws Exception {
        when(productService.findById(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/product/get").param("productId", "p999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
