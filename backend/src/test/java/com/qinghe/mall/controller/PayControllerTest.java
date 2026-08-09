package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.PayService;
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
 * 支付控制器 单元测试（遗留D：资金链路鉴权与核心流程）。
 */
class PayControllerTest {

    @Mock
    private PayService payService;

    @InjectMocks
    private PayController payController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(payController).build();
    }

    @Test
    void create_unauthenticated_401() throws Exception {
        mockMvc.perform(post("/api/pay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"O1\",\"payType\":\"MOCK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void create_authenticated_success() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        ChannelPayResult payResult = new ChannelPayResult();
        when(payService.createPay(1L, "O1", "MOCK")).thenReturn(Result.success(payResult));

        mockMvc.perform(post("/api/pay/create")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"O1\",\"payType\":\"MOCK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void query_unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/pay/query").param("orderNumber", "O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void mockPay_authenticated_delegatesToService() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        when(payService.mockPay(1L, "O1")).thenReturn(Result.success("支付成功"));

        mockMvc.perform(post("/api/pay/mockPay")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"O1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("支付成功"));
        verify(payService).mockPay(anyLong(), anyString());
    }
}
