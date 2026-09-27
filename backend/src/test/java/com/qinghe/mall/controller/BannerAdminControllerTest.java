package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.dataobject.BannerDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.service.BannerService;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 管理端首页运营位接口单元测试（D2，v1.7）。
 *
 * 覆盖：非 ADMIN 守卫 403（create/list/update/delete/toggle 全分支）、
 * ADMIN 会话下 CRUD 与上下架的参数透传。
 */
class BannerAdminControllerTest {

    @Mock
    private BannerService bannerService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
        adminSession = new MockHttpSession();
        adminSession.setAttribute("role", "ADMIN");
        adminSession.setAttribute("userId", 1L);
    }

    @Test
    @DisplayName("非 ADMIN：create 403")
    void create_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/banner/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"image\":\"/uploads/a.jpg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("非 ADMIN：list 403")
    void list_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/banner/list"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("非 ADMIN：delete 403")
    void delete_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/banner/delete").param("bannerId", "b1"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("非 ADMIN：toggle 403")
    void toggle_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/banner/toggle").param("bannerId", "b1").param("status", "OFF"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("ADMIN：create 透传请求体并返回新运营位")
    void create_withAdmin_success() throws Exception {
        BannerDO saved = new BannerDO();
        saved.setId("b1");
        saved.setTitle("开学季大促");
        saved.setStatus("ON");
        when(bannerService.createBanner(any(BannerDO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/admin/banner/create")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"开学季大促\",\"image\":\"/uploads/banner-1.jpg\",\"sortOrder\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("b1"))
                .andExpect(jsonPath("$.data.status").value("ON"));
    }

    @Test
    @DisplayName("ADMIN：list 透传分页参数")
    void list_withAdmin_success() throws Exception {
        Paging<BannerDO> paging = new Paging<>();
        paging.setPageNum(1);
        paging.setTotalCount(0L);
        paging.setData(new ArrayList<>());
        when(bannerService.listBanners(1, 10)).thenReturn(paging);

        mockMvc.perform(get("/api/admin/banner/list").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("ADMIN：update 透传请求体")
    void update_withAdmin_success() throws Exception {
        BannerDO existing = new BannerDO();
        existing.setId("b1");
        existing.setTitle("新标题");
        when(bannerService.updateBanner(any(BannerDO.class))).thenReturn(existing);

        mockMvc.perform(post("/api/admin/banner/update")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"b1\",\"title\":\"新标题\",\"image\":\"/uploads/b.jpg\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("新标题"));
    }

    @Test
    @DisplayName("ADMIN：delete 透传 bannerId")
    void delete_withAdmin_success() throws Exception {
        mockMvc.perform(post("/api/admin/banner/delete").session(adminSession).param("bannerId", "b1"))
                .andExpect(jsonPath("$.code").value(200));

        verify(bannerService).deleteBanner("b1");
    }

    @Test
    @DisplayName("ADMIN：toggle 透传状态")
    void toggle_withAdmin_success() throws Exception {
        mockMvc.perform(post("/api/admin/banner/toggle")
                        .session(adminSession)
                        .param("bannerId", "b1")
                        .param("status", "OFF"))
                .andExpect(jsonPath("$.code").value(200));

        verify(bannerService).toggleBanner("b1", "OFF");
    }
}
