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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.service.MerchantService;
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

    @InjectMocks
    private MerchantController merchantController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(merchantController).build();
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
}
