package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.UserService;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 管理端控制器 单元测试（遗留D：角色守卫 + 分页 + 参数校验）。
 */
class AdminControllerTest {

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

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute("role", "ADMIN");
        return session;
    }

    @Test
    void orderList_unauthenticated_403() throws Exception {
        mockMvc.perform(get("/api/admin/order/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void orderList_nonAdmin_403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 2L);
        session.setAttribute("role", "USER");

        mockMvc.perform(get("/api/admin/order/list").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void orderList_admin_returnsPaging() throws Exception {
        Paging<Order> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setPageSize(20);
        paging.setTotalCount(1L);
        paging.setData(new ArrayList<>());
        when(orderService.findAdminPage(eq(1), eq(20), nullable(String.class))).thenReturn(paging);

        mockMvc.perform(get("/api/admin/order/list").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    void updateUserRole_invalidRole_400() throws Exception {
        mockMvc.perform(post("/api/admin/user/updateRole")
                        .session(adminSession())
                        .param("id", "1")
                        .param("role", "SUPERUSER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void userList_admin_returnsPaging() throws Exception {
        Paging<User> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setTotalCount(0L);
        paging.setData(new ArrayList<>());
        when(userService.findAdminPage(1, 20)).thenReturn(paging);

        mockMvc.perform(get("/api/admin/user/list").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== 补充用例（P1 覆盖补强） ==========

    @Test
    void userList_removesPwd_fromResponse() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setPwd("plain-password-should-not-leak");
        Paging<User> paging = new Paging<>();
        paging.setData(new ArrayList<>(java.util.List.of(user)));
        when(userService.findAdminPage(1, 20)).thenReturn(paging);

        mockMvc.perform(get("/api/admin/user/list").session(adminSession()))
                .andExpect(jsonPath("$.data.data[0].pwd").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void dashboard_admin_returnsStats() throws Exception {
        Paging<Product> empty = new Paging<>();
        empty.setTotalCount(16L);
        when(productService.queryPage(1, 1, null, null, null)).thenReturn(empty);
        when(orderService.countAll()).thenReturn(42L);
        when(userService.countAll()).thenReturn(7L);
        when(orderService.sumTotalPriceByStatus("TRADE_PAID_SUCCESS")).thenReturn(new java.math.BigDecimal("1999.00"));

        mockMvc.perform(get("/api/admin/dashboard").session(adminSession()))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productCount").value(16))
                .andExpect(jsonPath("$.data.orderCount").value(42))
                .andExpect(jsonPath("$.data.userCount").value(7))
                .andExpect(jsonPath("$.data.totalRevenue").value(1999.00));
    }

    @Test
    void dashboard_nonAdmin_403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("role", "USER");
        mockMvc.perform(get("/api/admin/dashboard").session(session))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void productAdd_admin_returnsCreated() throws Exception {
        Product created = new Product();
        created.setId("p-new");
        when(productService.add(org.mockito.ArgumentMatchers.any(Product.class))).thenReturn(created);

        mockMvc.perform(post("/api/admin/product/add")
                        .session(adminSession())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新品\",\"price\":99}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("p-new"));
    }

    @Test
    void productUpdate_admin_returnsUpdated() throws Exception {
        Product updated = new Product();
        updated.setId("p1");
        when(productService.update(org.mockito.ArgumentMatchers.any(Product.class))).thenReturn(updated);

        mockMvc.perform(post("/api/admin/product/update")
                        .session(adminSession())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"p1\",\"name\":\"改\"}"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void productDelete_admin_returnsSuccess() throws Exception {
        when(productService.delete("p1")).thenReturn(true);
        mockMvc.perform(post("/api/admin/product/delete").session(adminSession()).param("id", "p1"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void productList_admin_returnsPaging() throws Exception {
        Paging<Product> paging = new Paging<>();
        paging.setTotalCount(5L);
        paging.setData(new ArrayList<>());
        when(productService.queryPage(1, 20, "鞋", null, null)).thenReturn(paging);

        mockMvc.perform(get("/api/admin/product/list").session(adminSession()).param("keyword", "鞋"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(5));
    }

    @Test
    void orderUpdateStatus_admin_returnsSuccess() throws Exception {
        mockMvc.perform(post("/api/admin/order/updateStatus")
                        .session(adminSession())
                        .param("orderNumber", "QH1")
                        .param("status", "TRADE_SHIPPED"))
                .andExpect(jsonPath("$.code").value(200));
        org.mockito.Mockito.verify(orderService).updateOrderStatus("QH1", "TRADE_SHIPPED");
    }

    @Test
    void orderShip_admin_returnsSuccess() throws Exception {
        mockMvc.perform(post("/api/admin/order/ship").session(adminSession()).param("orderNumber", "QH1"))
                .andExpect(jsonPath("$.code").value(200));
        org.mockito.Mockito.verify(orderService).shipOrder("QH1");
    }

    @Test
    void orderRefundProcess_admin_returnsSuccess() throws Exception {
        mockMvc.perform(post("/api/admin/order/refund/process")
                        .session(adminSession())
                        .param("orderNumber", "QH1")
                        .param("approve", "false"))
                .andExpect(jsonPath("$.code").value(200));
        org.mockito.Mockito.verify(orderService).processRefund("QH1", false);
    }

    @Test
    void couponCreate_admin_returnsCreated() throws Exception {
        com.qinghe.mall.dataobject.CouponDO coupon = new com.qinghe.mall.dataobject.CouponDO();
        when(couponService.createCoupon(org.mockito.ArgumentMatchers.any(com.qinghe.mall.dataobject.CouponDO.class)))
                .thenReturn(coupon);
        mockMvc.perform(post("/api/admin/coupon/create")
                        .session(adminSession())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"满100减20\"}"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void couponList_admin_returnsPaging() throws Exception {
        Paging<com.qinghe.mall.dataobject.CouponDO> paging = new Paging<>();
        paging.setTotalCount(3L);
        paging.setData(new ArrayList<>());
        when(couponService.listCoupons(1, 10)).thenReturn(paging);

        mockMvc.perform(get("/api/admin/coupon/list").session(adminSession()))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(3));
    }

    @Test
    void couponToggle_admin_returnsSuccess() throws Exception {
        mockMvc.perform(post("/api/admin/coupon/toggle")
                        .session(adminSession())
                        .param("couponId", "c1")
                        .param("status", "ON"))
                .andExpect(jsonPath("$.code").value(200));
        org.mockito.Mockito.verify(couponService).toggle("c1", "ON");
    }

    @Test
    void seckillToggle_admin_returnsSuccess() throws Exception {
        mockMvc.perform(post("/api/admin/seckill/toggle")
                        .session(adminSession())
                        .param("activityId", "act1")
                        .param("status", "CLOSED"))
                .andExpect(jsonPath("$.code").value(200));
        org.mockito.Mockito.verify(seckillService).toggle("act1", "CLOSED");
    }

    @Test
    void merchantAudit_admin_returnsSuccess() throws Exception {
        mockMvc.perform(post("/api/admin/merchant/audit")
                        .session(adminSession())
                        .param("merchantId", "5")
                        .param("approve", "false")
                        .param("reason", "资质不全"))
                .andExpect(jsonPath("$.code").value(200));
        org.mockito.Mockito.verify(merchantService).audit(5L, false, "资质不全");
    }

    @Test
    void merchantAudit_reject_withoutReason() throws Exception {
        mockMvc.perform(post("/api/admin/merchant/audit")
                        .session(adminSession())
                        .param("merchantId", "5")
                        .param("approve", "false"))
                .andExpect(jsonPath("$.code").value(200));
        org.mockito.Mockito.verify(merchantService).audit(eq(5L), eq(false), nullable(String.class));
    }

    @Test
    void updateUserRole_validRole_returnsSuccess() throws Exception {
        mockMvc.perform(post("/api/admin/user/updateRole")
                        .session(adminSession())
                        .param("id", "2")
                        .param("role", "MERCHANT"))
                .andExpect(jsonPath("$.code").value(200));
        org.mockito.Mockito.verify(userService).updateRole(2L, "MERCHANT");
    }

    @Test
    void updateUserRole_unauthenticated_403() throws Exception {
        mockMvc.perform(post("/api/admin/user/updateRole").param("id", "2").param("role", "USER"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void report_admin_returnsDailySales() throws Exception {
        java.util.List<java.util.Map<String, Object>> report = new ArrayList<>();
        java.util.Map<String, Object> day = new java.util.HashMap<>();
        day.put("date", "2026-08-10");
        day.put("amount", 199.0);
        report.add(day);
        when(orderService.dailySalesReport(7)).thenReturn(report);

        mockMvc.perform(get("/api/admin/report").session(adminSession()).param("days", "7"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].date").value("2026-08-10"));
    }
}
