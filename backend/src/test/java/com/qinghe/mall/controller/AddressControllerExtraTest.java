package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.config.GlobalExceptionHandler;
import com.qinghe.mall.model.Address;
import com.qinghe.mall.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * AddressController 分支覆盖补强（T3 续补）：补齐 /default 端点及
 * update/delete/setDefault 的未登录 401 守卫分支（AddressCommentFileControllerTest 已覆盖认证成功路径）。
 */
class AddressControllerExtraTest {

    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController addressController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(addressController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession session(Long userId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("userId", userId);
        return s;
    }

    @Test
    void default_unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/address/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void default_authenticated_returnsAddress() throws Exception {
        Address addr = new Address();
        addr.setId(1L);
        when(addressService.findDefault(1L)).thenReturn(addr);

        mockMvc.perform(get("/api/address/default").session(session(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void default_authenticated_noDefault_returnsNull() throws Exception {
        when(addressService.findDefault(1L)).thenReturn(null);

        mockMvc.perform(get("/api/address/default").session(session(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void update_unauthenticated_401() throws Exception {
        mockMvc.perform(post("/api/address/update")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"receiverName\":\"张三\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void delete_unauthenticated_401() throws Exception {
        mockMvc.perform(post("/api/address/delete").param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void setDefault_unauthenticated_401() throws Exception {
        mockMvc.perform(post("/api/address/setDefault").param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
