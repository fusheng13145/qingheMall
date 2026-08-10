package com.qinghe.mall.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 商家端控制器 单元测试（M6 平台化，standalone MockMvc，无 Spring 上下文）。
 * 重点验证：GET /info 的「审核通过后会话角色自动刷新 USER→MERCHANT」修复，及未登录拦截。
 */
class MerchantControllerTest {

    @Mock
    private MerchantService merchantService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private MerchantController merchantController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 注册全局异常处理器：checkMerchant 抛出的 RuntimeException 统一转 Result.fail(500)
        mockMvc = MockMvcBuilders.standaloneSetup(merchantController)
                .setControllerAdvice(new com.qinghe.mall.config.GlobalExceptionHandler())
                .build();
    }

    private MerchantDO merchant(Long userId, String status) {
        MerchantDO m = new MerchantDO();
        m.setId(10L);
        m.setUserId(userId);
        m.setShopName("青禾小铺");
        m.setStatus(status);
        return m;
    }

    @Test
    void info_unauthenticated_shouldReturnFailAndNotQuery() throws Exception {
        mockMvc.perform(get("/api/merchant/info"))
                .andExpect(status().isOk());
        // 未登录：不应查询商家信息
        verify(merchantService, never()).getByUserId(anyLong());
    }

    @Test
    void info_activeMerchant_shouldRefreshSessionRoleToMerchant() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute("role", UserDO.ROLE_USER);
        when(merchantService.getByUserId(1L)).thenReturn(merchant(1L, MerchantDO.STATUS_ACTIVE));

        mockMvc.perform(get("/api/merchant/info").session(session))
                .andExpect(status().isOk());

        // 核心修复：ACTIVE 商家在 info 时，会话角色应从 USER 自动提升为 MERCHANT（免重新登录）
        assertEquals(UserDO.ROLE_MERCHANT, session.getAttribute("role"));
    }

    @Test
    void info_pendingMerchant_shouldNotRefreshRole() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 2L);
        session.setAttribute("role", UserDO.ROLE_USER);
        when(merchantService.getByUserId(2L)).thenReturn(merchant(2L, MerchantDO.STATUS_PENDING));

        mockMvc.perform(get("/api/merchant/info").session(session))
                .andExpect(status().isOk());

        // 非 ACTIVE：不应刷新角色
        assertEquals(UserDO.ROLE_USER, session.getAttribute("role"));
    }

    @Test
    void apply_unauthenticated_shouldReturnFailAndNotApply() throws Exception {
        mockMvc.perform(post("/api/merchant/apply").param("shopName", "我的店铺"))
                .andExpect(status().isOk());
        verify(merchantService, never()).apply(anyLong(), any(), any(), any());
    }

    @Test
    void apply_authenticated_shouldCallService() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 3L);
        when(merchantService.apply(eq(3L), eq("我的店铺"), any(), any()))
                .thenReturn(merchant(3L, MerchantDO.STATUS_PENDING));

        mockMvc.perform(post("/api/merchant/apply")
                        .session(session)
                        .param("shopName", "我的店铺"))
                .andExpect(status().isOk());

        verify(merchantService).apply(eq(3L), eq("我的店铺"), any(), any());
    }

    // ========== 经营接口：权限守卫 ==========

    private MockHttpSession merchantSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        session.setAttribute("role", UserDO.ROLE_MERCHANT);
        return session;
    }

    private void mockActiveMerchant() {
        when(merchantService.getByUserId(10L)).thenReturn(merchant(10L, MerchantDO.STATUS_ACTIVE));
    }

    @Test
    void products_unauthenticated_throws() throws Exception {
        mockMvc.perform(get("/api/merchant/products"))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void products_roleNotMerchant_throws() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 10L);
        session.setAttribute("role", UserDO.ROLE_USER);
        mockMvc.perform(get("/api/merchant/products").session(session))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void products_notRegistered_throws() throws Exception {
        when(merchantService.getByUserId(10L)).thenReturn(null);
        mockMvc.perform(get("/api/merchant/products").session(merchantSession(10L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void products_pending_throws() throws Exception {
        when(merchantService.getByUserId(10L)).thenReturn(merchant(10L, MerchantDO.STATUS_PENDING));
        mockMvc.perform(get("/api/merchant/products").session(merchantSession(10L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void products_rejected_throws() throws Exception {
        MerchantDO rejected = merchant(10L, MerchantDO.STATUS_REJECTED);
        rejected.setRejectReason("资质不全");
        when(merchantService.getByUserId(10L)).thenReturn(rejected);
        mockMvc.perform(get("/api/merchant/products").session(merchantSession(10L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void products_disabled_throws() throws Exception {
        when(merchantService.getByUserId(10L)).thenReturn(merchant(10L, MerchantDO.STATUS_DISABLED));
        mockMvc.perform(get("/api/merchant/products").session(merchantSession(10L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void products_active_returnsPaging() throws Exception {
        mockActiveMerchant();
        com.qinghe.mall.model.Paging<Product> paging = new com.qinghe.mall.model.Paging<>();
        paging.setTotalCount(3L);
        when(productService.queryMerchantPage(10L, null, null, 1, 10)).thenReturn(paging);

        mockMvc.perform(get("/api/merchant/products").session(merchantSession(10L)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(3));
    }

    // ========== 商品保存 / 上下架 ==========

    @Test
    void saveProduct_new_returnsCreated() throws Exception {
        mockActiveMerchant();
        Product saved = new Product();
        saved.setId("p-new");
        when(productService.saveWithDetails(org.mockito.ArgumentMatchers.any(Product.class),
                org.mockito.ArgumentMatchers.nullable(java.util.List.class))).thenReturn(saved);

        mockMvc.perform(post("/api/merchant/product/save")
                        .session(merchantSession(10L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新品\",\"price\":99}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("p-new"));
    }

    @Test
    void saveProduct_blankName_throws() throws Exception {
        mockActiveMerchant();
        mockMvc.perform(post("/api/merchant/product/save")
                        .session(merchantSession(10L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"price\":99}"))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void saveProduct_editOtherMerchant_throws() throws Exception {
        mockActiveMerchant();
        Product existing = new Product();
        existing.setId("p1");
        existing.setMerchantId(999L);
        when(productService.findById("p1")).thenReturn(existing);

        mockMvc.perform(post("/api/merchant/product/save")
                        .session(merchantSession(10L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"p1\",\"name\":\"改\",\"price\":99}"))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void toggleProduct_invalidStatus_throws() throws Exception {
        mockActiveMerchant();
        mockMvc.perform(post("/api/merchant/product/toggle")
                        .session(merchantSession(10L))
                        .param("productId", "p1")
                        .param("status", "PAUSED"))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void toggleProduct_ownProduct_ok() throws Exception {
        mockActiveMerchant();
        Product existing = new Product();
        existing.setId("p1");
        existing.setMerchantId(10L);
        when(productService.findById("p1")).thenReturn(existing);
        when(productService.update(org.mockito.ArgumentMatchers.any(Product.class))).thenReturn(existing);

        mockMvc.perform(post("/api/merchant/product/toggle")
                        .session(merchantSession(10L))
                        .param("productId", "p1")
                        .param("status", "OFF"))
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== 订单与统计 ==========

    @Test
    void orders_active_returnsPaging() throws Exception {
        mockActiveMerchant();
        com.qinghe.mall.model.Paging<Order> paging = new com.qinghe.mall.model.Paging<>();
        paging.setTotalCount(2L);
        when(orderService.listByMerchant(10L, null, 1, 10)).thenReturn(paging);

        mockMvc.perform(get("/api/merchant/orders").session(merchantSession(10L)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(2));
    }

    @Test
    void ship_active_callsService() throws Exception {
        mockActiveMerchant();
        mockMvc.perform(post("/api/merchant/order/ship")
                        .session(merchantSession(10L))
                        .param("orderNumber", "QH1"))
                .andExpect(jsonPath("$.code").value(200));
        verify(orderService).shipMerchantOrder(10L, "QH1");
    }

    @Test
    void processRefund_active_callsService() throws Exception {
        mockActiveMerchant();
        mockMvc.perform(post("/api/merchant/order/refund/process")
                        .session(merchantSession(10L))
                        .param("orderNumber", "QH1")
                        .param("approve", "false"))
                .andExpect(jsonPath("$.code").value(200));
        verify(orderService).processMerchantRefund(10L, "QH1", false);
    }

    @Test
    void stats_active_returnsMap() throws Exception {
        mockActiveMerchant();
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("orderCount", 5L);
        when(orderService.merchantStats(10L)).thenReturn(stats);

        mockMvc.perform(get("/api/merchant/stats").session(merchantSession(10L)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderCount").value(5));
    }
}
