package com.qinghe.mall.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.model.Logistics;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.service.LogisticsService;
import com.qinghe.mall.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 物流控制器 单元测试（P2-18）：越权守卫 + 轨迹查询。
 */
class LogisticsControllerTest {

    @Mock
    private LogisticsService logisticsService;

    @Mock
    private OrderService orderService;

    @Mock
    private com.qinghe.mall.service.MerchantService merchantService;

    @InjectMocks
    private LogisticsController logisticsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(logisticsController).build();
    }

    private MockHttpSession session(Long userId, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        if (role != null) {
            session.setAttribute("role", role);
        }
        return session;
    }

    private Order order(Long userId) {
        Order order = new Order();
        order.setOrderNumber("O1");
        order.setUserId(userId);
        return order;
    }

    @Test
    void track_unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/logistics/track").param("orderNumber", "O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
        verify(logisticsService, never()).track(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void track_orderNotFound_fails() throws Exception {
        when(orderService.findByOrderNumber("O1")).thenReturn(null);
        mockMvc.perform(get("/api/logistics/track").session(session(1L, null)).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void track_notOwner_forbidden() throws Exception {
        when(orderService.findByOrderNumber("O1")).thenReturn(order(99L));
        mockMvc.perform(get("/api/logistics/track").session(session(1L, null)).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(403));
        verify(logisticsService, never()).track(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void track_owner_returnsTimeline() throws Exception {
        when(orderService.findByOrderNumber("O1")).thenReturn(order(1L));
        Logistics logistics = new Logistics();
        logistics.setStatus("DELIVERING");
        logistics.setCompany("顺丰速运");
        when(logisticsService.track("O1")).thenReturn(logistics);

        mockMvc.perform(get("/api/logistics/track").session(session(1L, null)).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("DELIVERING"))
                .andExpect(jsonPath("$.data.company").value("顺丰速运"));
    }

    @Test
    void track_admin_canViewAnyOrder() throws Exception {
        when(orderService.findByOrderNumber("O1")).thenReturn(order(99L));
        Logistics logistics = new Logistics();
        logistics.setStatus("SIGNED");
        when(logisticsService.track("O1")).thenReturn(logistics);

        mockMvc.perform(get("/api/logistics/track").session(session(1L, "ADMIN")).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("SIGNED"));
    }

    @Test
    void track_owningMerchant_canView() throws Exception {
        Order merchantOrder = order(99L);
        merchantOrder.setMerchantId(10L);
        when(orderService.findByOrderNumber("O1")).thenReturn(merchantOrder);
        com.qinghe.mall.dataobject.MerchantDO merchant = new com.qinghe.mall.dataobject.MerchantDO();
        merchant.setId(10L);
        when(merchantService.getByUserId(1L)).thenReturn(merchant);
        Logistics logistics = new Logistics();
        logistics.setStatus("SHIPPED");
        when(logisticsService.track("O1")).thenReturn(logistics);

        mockMvc.perform(get("/api/logistics/track").session(session(1L, "MERCHANT")).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));
    }

    @Test
    void track_unrelatedMerchant_forbidden() throws Exception {
        Order merchantOrder = order(99L);
        merchantOrder.setMerchantId(77L);
        when(orderService.findByOrderNumber("O1")).thenReturn(merchantOrder);
        com.qinghe.mall.dataobject.MerchantDO merchant = new com.qinghe.mall.dataobject.MerchantDO();
        merchant.setId(10L);
        when(merchantService.getByUserId(1L)).thenReturn(merchant);

        mockMvc.perform(get("/api/logistics/track").session(session(1L, "MERCHANT")).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void track_noLogisticsYet_fails() throws Exception {
        when(orderService.findByOrderNumber("O1")).thenReturn(order(1L));
        when(logisticsService.track("O1")).thenReturn(null);

        mockMvc.perform(get("/api/logistics/track").session(session(1L, null)).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(500));
    }
}
