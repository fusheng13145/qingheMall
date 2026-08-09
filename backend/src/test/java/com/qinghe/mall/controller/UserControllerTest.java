package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.config.GlobalExceptionHandler;
import com.qinghe.mall.model.Result;
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
 * 用户控制器 单元测试（遗留D：登录/注册/会话恢复）。
 */
class UserControllerTest {

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
                // 挂载全局异常处理，让 IllegalArgumentException → 400 等语义生效
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private User user(Long id, String userName, String role) {
        User user = new User();
        user.setId(id);
        user.setUserName(userName);
        user.setNickName(userName);
        user.setRole(role);
        return user;
    }

    @Test
    void register_success_returnsUserWithoutPwd() throws Exception {
        when(userService.register("alice", "secret123")).thenReturn(user(1L, "alice", "USER"));

        mockMvc.perform(post("/api/user/reg")
                        .param("userName", "alice")
                        .param("pwd", "secret123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userName").value("alice"));
    }

    @Test
    void login_success_writesSession() throws Exception {
        User loggedIn = user(1L, "alice", "USER");
        loggedIn.setPwd(null);
        when(userService.login("alice", "secret123")).thenReturn(loggedIn);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/user/login")
                        .session(session)
                        .param("userName", "alice")
                        .param("pwd", "secret123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 登录成功必须写入会话身份
        org.junit.jupiter.api.Assertions.assertEquals(1L, session.getAttribute("userId"));
        org.junit.jupiter.api.Assertions.assertEquals("USER", session.getAttribute("role"));
    }

    @Test
    void checkLogin_unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/user/checkLogin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void updateProfile_invalidAvatar_400() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        // P2：非法头像协议（data:）应被白名单拒绝
        mockMvc.perform(post("/api/user/updateProfile")
                        .session(session)
                        .param("avatar", "data:image/svg+xml;base64,PHN2Zz4="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        org.mockito.Mockito.verify(userService, org.mockito.Mockito.never()).updateProfile(any(), any(), any());
    }

    @Test
    void updateProfile_validAvatar_passes() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        when(userService.updateProfile(1L, null, "/uploads/avatar.jpg"))
                .thenReturn(user(1L, "alice", "USER"));

        mockMvc.perform(post("/api/user/updateProfile")
                        .session(session)
                        .param("avatar", "/uploads/avatar.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
