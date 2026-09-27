package com.qinghe.mall.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.dataobject.BannerDO;
import com.qinghe.mall.service.BannerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 顾客端运营位公开接口单元测试（D2，v1.7）。
 */
class BannerControllerTest {

    @Mock
    private BannerService bannerService;

    @InjectMocks
    private BannerController bannerController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(bannerController).build();
    }

    @Test
    @DisplayName("公开列表：返回上架运营位且无需登录")
    void list_returnsActiveBanners() throws Exception {
        BannerDO banner = new BannerDO();
        banner.setId("b1");
        banner.setTitle("开学季大促");
        banner.setStatus("ON");
        when(bannerService.listActive()).thenReturn(List.of(banner));

        mockMvc.perform(get("/api/banner/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("b1"))
                .andExpect(jsonPath("$.data[0].title").value("开学季大促"));
    }

    @Test
    @DisplayName("公开列表：无运营位时返回空数组")
    void list_empty_returnsEmptyArray() throws Exception {
        when(bannerService.listActive()).thenReturn(List.of());

        mockMvc.perform(get("/api/banner/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
