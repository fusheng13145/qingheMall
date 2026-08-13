package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.exception.AuthException;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.MerchantAuthService;
import com.qinghe.mall.service.SeckillService;
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
 * 商家端营销控制器（#39）单元覆盖：复用 MerchantAuthService 守卫（未登录 401），
 * 各接口正确委托 CouponService / SeckillService（跨店归属由 Service 层强制）。
 */
class MerchantMarketingControllerTest {

    @Mock
    private MerchantAuthService merchantAuthService;
    @Mock
    private CouponService couponService;
    @Mock
    private SeckillService seckillService;

    @InjectMocks
    private MerchantMarketingController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.qinghe.mall.config.GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession merchantSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("userId", 10L);
        s.setAttribute("role", UserDO.ROLE_MERCHANT);
        return s;
    }

    private MerchantDO merchant() {
        MerchantDO m = new MerchantDO();
        m.setId(10L);
        return m;
    }

    @Test
    void createCoupon_unauthenticated_returns401() throws Exception {
        when(merchantAuthService.checkMerchant(any()))
                .thenThrow(AuthException.unauthorized("未登录"));
        mockMvc.perform(post("/api/merchant/marketing/coupon/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"券\"}"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void createCoupon_activeMerchant_delegates() throws Exception {
        when(merchantAuthService.checkMerchant(any())).thenReturn(merchant());
        when(couponService.createMerchantCoupon(anyLong(), any())).thenReturn(new CouponDO());
        mockMvc.perform(post("/api/merchant/marketing/coupon/create").session(merchantSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"满100减20\",\"type\":\"FULL_REDUCTION\"}"))
                .andExpect(jsonPath("$.code").value(200));
        verify(couponService).createMerchantCoupon(eq(10L), any());
    }

    @Test
    void listCoupons_activeMerchant_delegates() throws Exception {
        when(merchantAuthService.checkMerchant(any())).thenReturn(merchant());
        when(couponService.listMerchantCoupons(anyLong(), anyInt(), anyInt())).thenReturn(new Paging<>());
        mockMvc.perform(get("/api/merchant/marketing/coupon/list").session(merchantSession()))
                .andExpect(jsonPath("$.code").value(200));
        verify(couponService).listMerchantCoupons(eq(10L), anyInt(), anyInt());
    }

    @Test
    void toggleCoupon_activeMerchant_delegates() throws Exception {
        when(merchantAuthService.checkMerchant(any())).thenReturn(merchant());
        mockMvc.perform(post("/api/merchant/marketing/coupon/toggle").session(merchantSession())
                        .param("couponId", "C1").param("status", "ACTIVE"))
                .andExpect(jsonPath("$.code").value(200));
        verify(couponService).toggleMerchantCoupon(eq(10L), eq("C1"), eq("ACTIVE"));
    }

    @Test
    void createSeckill_activeMerchant_delegates() throws Exception {
        when(merchantAuthService.checkMerchant(any())).thenReturn(merchant());
        when(seckillService.createMerchantActivity(anyLong(), any())).thenReturn(new SeckillActivityDO());
        mockMvc.perform(post("/api/merchant/marketing/seckill/create").session(merchantSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productDetailId\":\"PD1\",\"seckillPrice\":50,\"totalStock\":100}"))
                .andExpect(jsonPath("$.code").value(200));
        verify(seckillService).createMerchantActivity(eq(10L), any());
    }

    @Test
    void listSeckills_activeMerchant_delegates() throws Exception {
        when(merchantAuthService.checkMerchant(any())).thenReturn(merchant());
        when(seckillService.listMerchantActivities(anyLong(), any(), anyInt(), anyInt())).thenReturn(new Paging<>());
        mockMvc.perform(get("/api/merchant/marketing/seckill/list").session(merchantSession()))
                .andExpect(jsonPath("$.code").value(200));
        verify(seckillService).listMerchantActivities(eq(10L), any(), anyInt(), anyInt());
    }

    @Test
    void toggleSeckill_activeMerchant_delegates() throws Exception {
        when(merchantAuthService.checkMerchant(any())).thenReturn(merchant());
        mockMvc.perform(post("/api/merchant/marketing/seckill/toggle").session(merchantSession())
                        .param("activityId", "A1").param("status", "ONGOING"))
                .andExpect(jsonPath("$.code").value(200));
        verify(seckillService).toggleMerchantActivity(eq(10L), eq("A1"), eq("ONGOING"));
    }
}
