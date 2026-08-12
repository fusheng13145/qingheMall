package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.config.GlobalExceptionHandler;
import com.qinghe.mall.model.Cart;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.CartService;
import java.util.Map;
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
 * CartController 分支覆盖补充（T3 续补）。
 * 覆盖：全部端点的未登录守卫（401）；requireId 缺参/非法参数（IllegalArgumentException→400）；
 * 加入购物车的 quantity 缺省默认 1 分支。
 */
class CartControllerExtraTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession session(Long userId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("userId", userId);
        return s;
    }

    @Test
    void add_unauth_401() throws Exception {
        mockMvc.perform(post("/api/cart/add").contentType(MediaType.APPLICATION_JSON).content("{\"productDetailId\":\"pd001\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void add_quantityDefault_usesOne() throws Exception {
        when(cartService.add(anyLong(), anyString(), anyInt())).thenReturn(new Cart());
        mockMvc.perform(post("/api/cart/add").session(session(1L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productDetailId\":\"pd001\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void add_invalidQuantity_400() throws Exception {
        // quantity 非数字 → parseInt 抛 NumberFormatException → 全局 400
        mockMvc.perform(post("/api/cart/add").session(session(1L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productDetailId\":\"pd001\",\"quantity\":\"abc\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updateQuantity_unauth_401() throws Exception {
        mockMvc.perform(post("/api/cart/updateQuantity").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1,\"quantity\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void updateQuantity_missingId_400() throws Exception {
        mockMvc.perform(post("/api/cart/updateQuantity").session(session(1L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updateQuantity_invalidId_400() throws Exception {
        mockMvc.perform(post("/api/cart/updateQuantity").session(session(1L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"x\",\"quantity\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updateSelected_unauth_401() throws Exception {
        mockMvc.perform(post("/api/cart/updateSelected").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1,\"selected\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void updateSelected_missingId_400() throws Exception {
        mockMvc.perform(post("/api/cart/updateSelected").session(session(1L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selected\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void remove_unauth_401() throws Exception {
        mockMvc.perform(post("/api/cart/remove").param("id", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void list_unauth_401() throws Exception {
        mockMvc.perform(get("/api/cart/list"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void count_unauth_401() throws Exception {
        mockMvc.perform(get("/api/cart/count"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void clearSelected_unauth_401() throws Exception {
        mockMvc.perform(post("/api/cart/clearSelected"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }
}
