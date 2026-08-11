package com.qinghe.mall.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.RefundRequest;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.RefundService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 退款查询控制器 单元测试（P2-18）：我的申请 + 订单维度历史（越权守卫）。
 */
class RefundControllerTest {

    @Mock
    private RefundService refundService;

    @Mock
    private OrderService orderService;

    @Mock
    private com.qinghe.mall.service.MerchantService merchantService;

    @InjectMocks
    private RefundController refundController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(refundController).build();
    }

    private MockHttpSession session(Long userId, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        if (role != null) {
            session.setAttribute("role", role);
        }
        return session;
    }

    private RefundRequest request(String id, String orderNumber) {
        RefundRequest req = new RefundRequest();
        req.setId(id);
        req.setOrderNumber(orderNumber);
        req.setStatus("PENDING");
        return req;
    }

    @Test
    void mine_unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/refund/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
        verify(refundService, never()).listByUser(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void mine_authenticated_returnsList() throws Exception {
        when(refundService.listByUser(1L))
                .thenReturn(Collections.singletonList(request("r1", "O1")));

        mockMvc.perform(get("/api/refund/mine").session(session(1L, null)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("r1"))
                .andExpect(jsonPath("$.data[0].orderNumber").value("O1"));
    }

    @Test
    void byOrder_unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/refund/order").param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void byOrder_orderNotFound_fails() throws Exception {
        when(orderService.findByOrderNumber("O1")).thenReturn(null);
        mockMvc.perform(get("/api/refund/order").session(session(1L, null)).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void byOrder_notOwner_forbidden() throws Exception {
        Order order = new Order();
        order.setOrderNumber("O1");
        order.setUserId(99L);
        when(orderService.findByOrderNumber("O1")).thenReturn(order);

        mockMvc.perform(get("/api/refund/order").session(session(1L, null)).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(403));
        verify(refundService, never()).listByOrder(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void byOrder_owner_returnsHistory() throws Exception {
        Order order = new Order();
        order.setOrderNumber("O1");
        order.setUserId(1L);
        when(orderService.findByOrderNumber("O1")).thenReturn(order);
        when(refundService.listByOrder("O1"))
                .thenReturn(Collections.singletonList(request("r1", "O1")));

        mockMvc.perform(get("/api/refund/order").session(session(1L, null)).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("r1"));
    }

    @Test
    void byOrder_admin_canViewAnyOrder() throws Exception {
        Order order = new Order();
        order.setOrderNumber("O1");
        order.setUserId(99L);
        when(orderService.findByOrderNumber("O1")).thenReturn(order);
        when(refundService.listByOrder("O1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/refund/order").session(session(1L, "ADMIN")).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void byOrder_owningMerchant_canView() throws Exception {
        Order order = new Order();
        order.setOrderNumber("O1");
        order.setUserId(99L);
        order.setMerchantId(10L);
        when(orderService.findByOrderNumber("O1")).thenReturn(order);
        com.qinghe.mall.dataobject.MerchantDO merchant = new com.qinghe.mall.dataobject.MerchantDO();
        merchant.setId(10L);
        when(merchantService.getByUserId(1L)).thenReturn(merchant);
        when(refundService.listByOrder("O1"))
                .thenReturn(Collections.singletonList(request("r1", "O1")));

        mockMvc.perform(get("/api/refund/order").session(session(1L, "MERCHANT")).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("r1"));
    }
}
