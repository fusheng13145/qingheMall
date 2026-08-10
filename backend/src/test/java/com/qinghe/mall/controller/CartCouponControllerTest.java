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

    // ========== 购物车：全端点（P1 覆盖补强） ==========

    @Test
    void cartAdd_authenticated_success() throws Exception {
        com.qinghe.mall.model.Cart cart = new com.qinghe.mall.model.Cart();
        when(cartService.add(1L, "pd1", 2)).thenReturn(cart);

        cartMvc.perform(post("/api/cart/add")
                        .session(session(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productDetailId\":\"pd1\",\"quantity\":2}"))
                .andExpect(jsonPath("$.code").value(200));
        verify(cartService).add(1L, "pd1", 2);
    }

    @Test
    void cartUpdateSelected_valid_delegates() throws Exception {
        cartMvc.perform(post("/api/cart/updateSelected")
                        .session(session(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":5,\"selected\":true}"))
                .andExpect(jsonPath("$.code").value(200));
        verify(cartService).updateSelected(1L, 5L, true);
    }

    @Test
    void cartRemove_authenticated_delegates() throws Exception {
        cartMvc.perform(post("/api/cart/remove").session(session(1L)).param("id", "7"))
                .andExpect(jsonPath("$.code").value(200));
        verify(cartService).remove(1L, 7L);
    }

    @Test
    void cartList_authenticated_returnsList() throws Exception {
        when(cartService.list(1L)).thenReturn(java.util.List.of(new com.qinghe.mall.model.Cart()));
        cartMvc.perform(get("/api/cart/list").session(session(1L)))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void cartCount_authenticated_returnsCount() throws Exception {
        when(cartService.count(1L)).thenReturn(3);
        cartMvc.perform(get("/api/cart/count").session(session(1L)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void cartClearSelected_authenticated_delegates() throws Exception {
        cartMvc.perform(post("/api/cart/clearSelected").session(session(1L)))
                .andExpect(jsonPath("$.code").value(200));
        verify(cartService).clearSelected(1L);
    }

    // ========== 优惠券：查询（P1 覆盖补强） ==========

    @Test
    void couponList_authenticated_returnsPaging() throws Exception {
        com.qinghe.mall.model.Paging<com.qinghe.mall.dataobject.CouponDO> paging =
                new com.qinghe.mall.model.Paging<>();
        paging.setTotalCount(2L);
        when(couponService.listCoupons(1, 10)).thenReturn(paging);

        couponMvc.perform(get("/api/coupon/list").session(session(1L)))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void couponMyCoupons_authenticated_returnsList() throws Exception {
        when(couponService.myCoupons(1L, "UNUSED")).thenReturn(java.util.List.of());
        couponMvc.perform(get("/api/coupon/mine").session(session(1L)).param("status", "UNUSED"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void couponAvailable_authenticated_returnsList() throws Exception {
        when(couponService.available(1L, new java.math.BigDecimal("200"))).thenReturn(java.util.List.of());
        couponMvc.perform(get("/api/coupon/available").session(session(1L)).param("amount", "200"))
                .andExpect(jsonPath("$.code").value(200));
    }
}
