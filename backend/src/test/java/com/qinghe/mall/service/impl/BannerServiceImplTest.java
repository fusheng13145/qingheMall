package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.BannerDAO;
import com.qinghe.mall.dataobject.BannerDO;
import com.qinghe.mall.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 首页运营位服务单元测试（D2，v1.7）。
 *
 * 覆盖：创建校验（标题/图片缺失、标题超长）与默认值（排序 0、默认上架）、
 * 更新（ID 缺失/不存在/排序缺省沿用原值）、删除与上下架（不存在/非法状态拒绝）、
 * 顾客端仅上架位透传。
 */
@ExtendWith(MockitoExtension.class)
class BannerServiceImplTest {

    @Mock
    private BannerDAO bannerDAO;

    @InjectMocks
    private BannerServiceImpl bannerService;

    private BannerDO banner;

    @BeforeEach
    void setUp() {
        banner = new BannerDO();
        banner.setTitle("开学季大促");
        banner.setImage("/uploads/banner-1.jpg");
    }

    @Test
    @DisplayName("创建：缺标题拒绝")
    void create_missingTitle_rejected() {
        banner.setTitle(" ");
        assertThrows(BusinessException.class, () -> bannerService.createBanner(banner));
        verify(bannerDAO, never()).insert(any());
    }

    @Test
    @DisplayName("创建：缺图片拒绝")
    void create_missingImage_rejected() {
        banner.setImage(null);
        assertThrows(BusinessException.class, () -> bannerService.createBanner(banner));
    }

    @Test
    @DisplayName("创建：标题超长拒绝")
    void create_titleTooLong_rejected() {
        banner.setTitle("长".repeat(65));
        assertThrows(BusinessException.class, () -> bannerService.createBanner(banner));
    }

    @Test
    @DisplayName("创建：默认排序 0、默认上架、生成 UUID 主键")
    void create_defaultsApplied() {
        when(bannerDAO.insert(any(BannerDO.class))).thenReturn(1);

        BannerDO saved = bannerService.createBanner(banner);

        assertNotNull(saved.getId());
        assertEquals(0, saved.getSortOrder());
        assertEquals("ON", saved.getStatus());
        ArgumentCaptor<BannerDO> captor = ArgumentCaptor.forClass(BannerDO.class);
        verify(bannerDAO).insert(captor.capture());
        assertEquals("开学季大促", captor.getValue().getTitle());
    }

    @Test
    @DisplayName("创建：显式排序与状态被保留")
    void create_explicitValuesKept() {
        banner.setSortOrder(5);
        banner.setStatus("OFF");
        when(bannerDAO.insert(any(BannerDO.class))).thenReturn(1);

        BannerDO saved = bannerService.createBanner(banner);

        assertEquals(5, saved.getSortOrder());
        assertEquals("OFF", saved.getStatus());
    }

    @Test
    @DisplayName("更新：ID 缺失拒绝")
    void update_missingId_rejected() {
        assertThrows(BusinessException.class, () -> bannerService.updateBanner(banner));
    }

    @Test
    @DisplayName("更新：运营位不存在拒绝")
    void update_notExist_rejected() {
        banner.setId("b1");
        when(bannerDAO.findById("b1")).thenReturn(null);
        assertThrows(BusinessException.class, () -> bannerService.updateBanner(banner));
    }

    @Test
    @DisplayName("更新：排序缺省沿用原值并回读最新记录")
    void update_sortOrderFallbackToExisting() {
        banner.setId("b1");
        BannerDO existing = new BannerDO();
        existing.setId("b1");
        existing.setSortOrder(7);
        when(bannerDAO.findById("b1")).thenReturn(existing).thenReturn(existing);

        BannerDO updated = bannerService.updateBanner(banner);

        assertEquals(7, updated.getSortOrder());
        verify(bannerDAO).update(any(BannerDO.class));
    }

    @Test
    @DisplayName("删除：不存在拒绝")
    void delete_notExist_rejected() {
        when(bannerDAO.findById("nope")).thenReturn(null);
        assertThrows(BusinessException.class, () -> bannerService.deleteBanner("nope"));
        verify(bannerDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("删除：存在则执行删除")
    void delete_existing_deleted() {
        BannerDO existing = new BannerDO();
        existing.setId("b1");
        when(bannerDAO.findById("b1")).thenReturn(existing);

        bannerService.deleteBanner("b1");

        verify(bannerDAO).deleteById("b1");
    }

    @Test
    @DisplayName("上下架：非法状态拒绝")
    void toggle_invalidStatus_rejected() {
        BannerDO existing = new BannerDO();
        existing.setId("b1");
        when(bannerDAO.findById("b1")).thenReturn(existing);

        assertThrows(BusinessException.class, () -> bannerService.toggleBanner("b1", "PAUSED"));
        verify(bannerDAO, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("上下架：不存在拒绝")
    void toggle_notExist_rejected() {
        when(bannerDAO.findById("nope")).thenReturn(null);
        assertThrows(BusinessException.class, () -> bannerService.toggleBanner("nope", "ON"));
    }

    @Test
    @DisplayName("上下架：ON/OFF 合法状态放行")
    void toggle_validStatus_passthrough() {
        BannerDO existing = new BannerDO();
        existing.setId("b1");
        when(bannerDAO.findById("b1")).thenReturn(existing);

        bannerService.toggleBanner("b1", "OFF");
        bannerService.toggleBanner("b1", "ON");

        verify(bannerDAO).updateStatus("b1", "OFF");
        verify(bannerDAO).updateStatus("b1", "ON");
    }

    @Test
    @DisplayName("顾客端：listActive 透传 DAO 上架位列表")
    void listActive_passthrough() {
        BannerDO active = new BannerDO();
        active.setId("b1");
        active.setStatus("ON");
        when(bannerDAO.findActive()).thenReturn(List.of(active));

        List<BannerDO> result = bannerService.listActive();

        assertEquals(1, result.size());
        assertEquals("b1", result.get(0).getId());
    }
}
