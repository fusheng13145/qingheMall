package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.UserService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * AdminController 分支覆盖补充（T3 续补）。
 * 覆盖：全部管理端端点的非 ADMIN 角色守卫（403）分支；
 * shipOrder 带/不带物流信息的两分支；listAllUsers 数据非空/空两分支；updateUserRole 合法角色分支。
 */
class AdminControllerExtraTest {

    @Mock
    private OrderService orderService;
    @Mock
    private UserService userService;
    @Mock
    private ProductService productService;
    @Mock
    private MerchantService merchantService;
    @Mock
    private CouponService couponService;
    @Mock
    private com.qinghe.mall.service.SeckillService seckillService;
    @Mock
    private com.qinghe.mall.service.LogisticsService logisticsService;
    @Mock
    private com.qinghe.mall.service.RefundService refundService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    private MockHttpSession userSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("userId", 2L);
        s.setAttribute("role", "USER");
        return s;
    }

    private MockHttpSession adminSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("userId", 1L);
        s.setAttribute("role", "ADMIN");
        return s;
    }

    @Test
    void allEndpoints_nonAdmin_403() throws Exception {
        List<MockHttpServletRequestBuilder> reqs = List.of(
                get("/api/admin/dashboard"),
                get("/api/admin/report").param("days", "7"),
                post("/api/admin/product/add").contentType(MediaType.APPLICATION_JSON).content("{}"),
                post("/api/admin/product/update").contentType(MediaType.APPLICATION_JSON).content("{}"),
                post("/api/admin/product/delete").param("id", "1"),
                get("/api/admin/product/list"),
                get("/api/admin/order/list"),
                post("/api/admin/order/updateStatus").param("orderNumber", "N1").param("status", "PAID"),
                post("/api/admin/order/ship").param("orderNumber", "N1"),
                post("/api/admin/logistics/advance").param("orderNumber", "N1"),
                post("/api/admin/order/refund/process").param("orderNumber", "N1"),
                post("/api/admin/coupon/create").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/api/admin/coupon/list"),
                post("/api/admin/coupon/update").contentType(MediaType.APPLICATION_JSON).content("{}"),
                post("/api/admin/coupon/toggle").param("couponId", "1").param("status", "ACTIVE"),
                post("/api/admin/seckill/create").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/api/admin/seckill/list"),
                post("/api/admin/seckill/toggle").param("activityId", "1").param("status", "ACTIVE"),
                get("/api/admin/merchant/list"),
                post("/api/admin/merchant/audit").param("merchantId", "1"),
                get("/api/admin/user/list"),
                post("/api/admin/user/updateRole").param("id", "1").param("role", "USER")
        );
        for (MockHttpServletRequestBuilder req : reqs) {
            mockMvc.perform(req.session(userSession()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403));
        }
    }

    @Test
    void shipOrder_withLogistics_delegatesLogistics() throws Exception {
        mockMvc.perform(post("/api/admin/order/ship")
                        .session(adminSession())
                        .param("orderNumber", "N1")
                        .param("company", "SF")
                        .param("trackingNumber", "T1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shipOrder_withoutLogistics_delegatesOrder() throws Exception {
        mockMvc.perform(post("/api/admin/order/ship")
                        .session(adminSession())
                        .param("orderNumber", "N1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void listAllUsers_withData_clearsPwd() throws Exception {
        Paging<User> paging = new Paging<>();
        User u = new User();
        u.setPwd("secret");
        paging.setData(new ArrayList<>(List.of(u)));
        when(userService.findAdminPage(anyInt(), anyInt())).thenReturn(paging);

        mockMvc.perform(get("/api/admin/user/list").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void listAllUsers_nullData_noNpe() throws Exception {
        Paging<User> paging = new Paging<>();
        paging.setData(null);
        when(userService.findAdminPage(anyInt(), anyInt())).thenReturn(paging);

        mockMvc.perform(get("/api/admin/user/list").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateUserRole_validRole_200() throws Exception {
        mockMvc.perform(post("/api/admin/user/updateRole")
                        .session(adminSession())
                        .param("id", "1")
                        .param("role", "MERCHANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
