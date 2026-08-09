package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.OrderService;
import java.util.ArrayList;
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
 * 订单控制器 单元测试（遗留D：鉴权与核心流程）。
 */
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
    }

    private MockHttpSession session(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        return session;
    }

    @Test
    void addOrder_unauthenticated_401() throws Exception {
        mockMvc.perform(post("/api/order/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productDetailId\":\"pd001\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void addOrder_authenticated_success() throws Exception {
        Order created = new Order();
        created.setId("o1");
        created.setStatus(OrderStatus.WAIT_BUYER_PAY);
        when(orderService.createOrder(any(Order.class))).thenReturn(created);

        mockMvc.perform(post("/api/order/add")
                        .session(session(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productDetailId\":\"pd001\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void cancelOrder_unauthenticated_401() throws Exception {
        mockMvc.perform(post("/api/order/cancel").param("orderNumber", "O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void cancelOrder_authenticated_delegates() throws Exception {
        when(orderService.cancelOrder("O1", 1L)).thenReturn(true);

        mockMvc.perform(post("/api/order/cancel").session(session(1L)).param("orderNumber", "O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(orderService).cancelOrder(anyString(), anyLong());
    }

    @Test
    void list_authenticated_returnsPaging() throws Exception {
        Paging<Order> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setTotalCount(0L);
        paging.setData(new ArrayList<>());
        when(orderService.findPageByUserIdAndStatus(1L, null, 1, 10)).thenReturn(paging);

        mockMvc.perform(get("/api/order/list").session(session(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
