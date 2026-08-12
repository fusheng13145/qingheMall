package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.config.GlobalExceptionHandler;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * UserController 分支覆盖补充（T3 续补）。
 * 覆盖：reg 的 MERCHANT 分支；updateProfile 头像协议白名单（非法/站内/https/http）分支；
 * logout 会话存在/不存在两分支；checkLogin / updateProfile 未登录守卫。
 */
class UserControllerExtraTest {

    @Mock
    private UserService userService;
    @Mock
    private MerchantService merchantService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession session(Long userId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("userId", userId);
        return s;
    }

    @Test
    void reg_merchant_role_delegatesMerchant() throws Exception {
        User u = new User();
        u.setId(99L);
        when(merchantService.registerMerchant(anyString(), anyString(), any(), any(), any())).thenReturn(u);
        mockMvc.perform(post("/api/user/reg")
                        .param("userName", "shop1").param("pwd", "pwd123456").param("role", "MERCHANT").param("shopName", "店铺A"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateProfile_unauth_401() throws Exception {
        mockMvc.perform(post("/api/user/updateProfile").param("nickName", "新昵称"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void updateProfile_invalidAvatar_400() throws Exception {
        mockMvc.perform(post("/api/user/updateProfile").session(session(1L))
                        .param("avatar", "data:image/png;base64,xxxx"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updateProfile_relativeAvatar_ok() throws Exception {
        User u = new User();
        u.setId(1L);
        u.setNickName("n");
        when(userService.updateProfile(any(), any(), any())).thenReturn(u);
        mockMvc.perform(post("/api/user/updateProfile").session(session(1L))
                        .param("avatar", "/uploads/avatar.png"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateProfile_httpsAvatar_ok() throws Exception {
        User u = new User();
        u.setId(1L);
        when(userService.updateProfile(any(), any(), any())).thenReturn(u);
        mockMvc.perform(post("/api/user/updateProfile").session(session(1L))
                        .param("avatar", "https://cdn.example.com/a.png"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateProfile_httpAvatar_ok() throws Exception {
        User u = new User();
        u.setId(1L);
        when(userService.updateProfile(any(), any(), any())).thenReturn(u);
        mockMvc.perform(post("/api/user/updateProfile").session(session(1L))
                        .param("avatar", "http://cdn.example.com/a.png"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void logout_noSession_success() throws Exception {
        // 未登录：getSession(false) 为 null → 不 invalidate，直接 success（覆盖 if(session!=null) false 分支）
        mockMvc.perform(get("/api/user/logout"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void logout_withSession_success() throws Exception {
        mockMvc.perform(get("/api/user/logout").session(session(1L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void checkLogin_unauth_401() throws Exception {
        mockMvc.perform(get("/api/user/checkLogin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }
}
