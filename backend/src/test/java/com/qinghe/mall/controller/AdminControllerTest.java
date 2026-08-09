package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.UserService;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 管理端控制器 单元测试（遗留D：角色守卫 + 分页 + 参数校验）。
 */
class AdminControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private MerchantService merchantService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute("role", "ADMIN");
        return session;
    }

    @Test
    void orderList_unauthenticated_403() throws Exception {
        mockMvc.perform(get("/api/admin/order/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void orderList_nonAdmin_403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 2L);
        session.setAttribute("role", "USER");

        mockMvc.perform(get("/api/admin/order/list").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void orderList_admin_returnsPaging() throws Exception {
        Paging<Order> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setPageSize(20);
        paging.setTotalCount(1L);
        paging.setData(new ArrayList<>());
        when(orderService.findAdminPage(eq(1), eq(20), nullable(String.class))).thenReturn(paging);

        mockMvc.perform(get("/api/admin/order/list").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    void updateUserRole_invalidRole_400() throws Exception {
        mockMvc.perform(post("/api/admin/user/updateRole")
                        .session(adminSession())
                        .param("id", "1")
                        .param("role", "SUPERUSER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void userList_admin_returnsPaging() throws Exception {
        Paging<User> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setTotalCount(0L);
        paging.setData(new ArrayList<>());
        when(userService.findAdminPage(1, 20)).thenReturn(paging);

        mockMvc.perform(get("/api/admin/user/list").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
