package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.config.GlobalExceptionHandler;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.CartService;
import com.qinghe.mall.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 购物车 + 优惠券控制器 单元测试（遗留D 第二批：参数校验与鉴权）。
 */
class CartCouponControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CartController cartController;

    @InjectMocks
    private CouponController couponController;

    private MockMvc cartMvc;
    private MockMvc couponMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cartMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        couponMvc = MockMvcBuilders.standaloneSetup(couponController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private MockHttpSession session(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        return session;
    }

    // ============ 购物车：参数校验（P2 400 语义） ============

    @Test
    void cartAdd_unauthenticated_401() throws Exception {
        cartMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productDetailId\":\"pd001\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void cartUpdateQuantity_missingId_400() throws Exception {
        cartMvc.perform(post("/api/cart/updateQuantity")
                        .session(session(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        verify(cartService, never()).updateQuantity(anyLong(), anyLong(), any());
    }

    @Test
    void cartUpdateQuantity_invalidId_400() throws Exception {
        cartMvc.perform(post("/api/cart/updateQuantity")
                        .session(session(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"abc\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void cartUpdateQuantity_valid_delegates() throws Exception {
        cartMvc.perform(post("/api/cart/updateQuantity")
                        .session(session(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":10,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(cartService).updateQuantity(1L, 10L, 2);
    }

    // ============ 优惠券：鉴权 ============

    @Test
    void couponClaim_unauthenticated_401() throws Exception {
        couponMvc.perform(post("/api/coupon/claim").param("couponId", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void couponClaim_authenticated_delegates() throws Exception {
        couponMvc.perform(post("/api/coupon/claim").session(session(1L)).param("couponId", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(couponService).claim(anyString(), anyLong());
    }
}
