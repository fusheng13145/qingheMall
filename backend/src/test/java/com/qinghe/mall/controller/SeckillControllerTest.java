package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.SeckillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 秒杀控制器 单元测试（遗留D 第二批）。
 */
class SeckillControllerTest {

    @Mock
    private SeckillService seckillService;

    @InjectMocks
    private SeckillController seckillController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(seckillController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createOrder_unauthenticated_401() throws Exception {
        mockMvc.perform(post("/api/seckill/a1/createOrder")
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
        verify(seckillService, never()).createOrder(anyString(), anyLong(), anyInt(), any(), any(), any());
    }

    @Test
    void createOrder_authenticated_success() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        when(seckillService.createOrder("a1", 1L, 1, null, null, null)).thenReturn("QH1");

        mockMvc.perform(post("/api/seckill/a1/createOrder")
                        .session(session)
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void listOngoing_public_returnsActivities() throws Exception {
        SeckillActivityDO activity = new SeckillActivityDO();
        activity.setId("a1");
        activity.setStatus("ONGOING");
        when(seckillService.listOngoing()).thenReturn(java.util.Collections.singletonList(activity));

        mockMvc.perform(get("/api/seckill/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
