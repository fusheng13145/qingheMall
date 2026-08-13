package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.SeckillActivityDAO;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;

/** #39 商家端秒杀：SKU 本店归属校验、跨店拒绝、本店列表与店铺进行中活动。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MerchantSeckillServiceImplTest {

    @Mock
    private SeckillActivityDAO activityDAO;
    @Mock
    private ProductDetailService productDetailService;
    @Mock
    private ProductService productService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RAtomicLong stockCounter;

    @InjectMocks
    private SeckillServiceImpl seckillService;

    private SeckillActivityDO baseActivity() {
        SeckillActivityDO a = new SeckillActivityDO();
        a.setProductDetailId("PD1");
        a.setSeckillPrice(new BigDecimal("50"));
        a.setTotalStock(100);
        a.setStartTime(new Date(System.currentTimeMillis() - 1000));
        a.setEndTime(new Date(System.currentTimeMillis() + 86400000L));
        return a;
    }

    @BeforeEach
    void setUp() {
        when(redissonClient.getAtomicLong(anyString())).thenReturn(stockCounter);
    }

    @Test
    void createMerchantActivity_sameMerchant_ok() {
        ProductDetail pd = new ProductDetail();
        pd.setProductId("P1");
        when(productDetailService.findById("PD1")).thenReturn(pd);
        Product p = new Product();
        p.setId("P1");
        p.setMerchantId(10L);
        when(productService.findById("P1")).thenReturn(p);

        SeckillActivityDO created = seckillService.createMerchantActivity(10L, baseActivity());
        assertEquals(10L, created.getMerchantId());
        assertNotNull(created.getId());
        assertEquals(Integer.valueOf(100), created.getRemainStock());
    }

    @Test
    void createMerchantActivity_crossMerchant_rejected() {
        ProductDetail pd = new ProductDetail();
        pd.setProductId("P1");
        when(productDetailService.findById("PD1")).thenReturn(pd);
        Product p = new Product();
        p.setId("P1");
        p.setMerchantId(99L);
        when(productService.findById("P1")).thenReturn(p);
        assertThrows(RuntimeException.class, () -> seckillService.createMerchantActivity(10L, baseActivity()));
    }

    @Test
    void createMerchantActivity_productMissing_rejected() {
        ProductDetail pd = new ProductDetail();
        pd.setProductId("P1");
        when(productDetailService.findById("PD1")).thenReturn(pd);
        when(productService.findById("P1")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> seckillService.createMerchantActivity(10L, baseActivity()));
    }

    @Test
    void toggleMerchantActivity_crossMerchant_rejected() {
        SeckillActivityDO existing = baseActivity();
        existing.setId("A1");
        existing.setMerchantId(99L);
        when(activityDAO.findById("A1")).thenReturn(existing);
        assertThrows(RuntimeException.class, () -> seckillService.toggleMerchantActivity(10L, "A1", "ONGOING"));
    }

    @Test
    void toggleMerchantActivity_sameMerchant_ok() {
        SeckillActivityDO existing = baseActivity();
        existing.setId("A1");
        existing.setMerchantId(10L);
        existing.setRemainStock(100);
        when(activityDAO.findById("A1")).thenReturn(existing);
        seckillService.toggleMerchantActivity(10L, "A1", "ONGOING");
        verify(activityDAO).updateStatus("A1", "ONGOING");
    }

    @Test
    void listMerchantActivities_callsFindByMerchant() {
        when(activityDAO.findByMerchant(anyLong(), any())).thenReturn(Collections.emptyList());
        seckillService.listMerchantActivities(10L, null, 1, 10);
        verify(activityDAO).findByMerchant(anyLong(), any());
    }

    @Test
    void listOngoingByMerchant_callsFindActiveByMerchant() {
        when(activityDAO.findActiveByMerchant(anyLong())).thenReturn(Collections.emptyList());
        seckillService.listOngoingByMerchant(10L);
        verify(activityDAO).findActiveByMerchant(10L);
    }

    // ===== validateActivity / createMerchantActivity 边界分支 =====

    @Test
    void createMerchantActivity_nullActivity_rejected() {
        // 覆盖 validateActivity 内 activity == null 守卫
        assertThrows(RuntimeException.class, () -> seckillService.createMerchantActivity(10L, null));
    }

    @Test
    void createMerchantActivity_nullDetailId_rejected() {
        // 覆盖 validateActivity 内 productDetailId == null 守卫
        SeckillActivityDO a = new SeckillActivityDO();
        a.setSeckillPrice(new BigDecimal("50"));
        a.setTotalStock(100);
        a.setStartTime(new Date(System.currentTimeMillis() - 1000));
        a.setEndTime(new Date(System.currentTimeMillis() + 86400000L));
        assertThrows(RuntimeException.class, () -> seckillService.createMerchantActivity(10L, a));
    }

    @Test
    void createMerchantActivity_detailMissing_rejected() {
        // 覆盖 validateActivity 内 detail == null 分支
        SeckillActivityDO a = baseActivity();
        when(productDetailService.findById("PD1")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> seckillService.createMerchantActivity(10L, a));
    }

    @Test
    void createMerchantActivity_nullMerchant_throws() {
        // 覆盖 createMerchantActivity 内 merchantId == null 守卫分支
        assertThrows(RuntimeException.class, () -> seckillService.createMerchantActivity(null, baseActivity()));
    }

    @Test
    void createMerchantActivity_pastEndTime_ok() {
        // 覆盖 preheatStock 内 ttlMs<=0 兜底分支（endTime 在过去）
        SeckillActivityDO a = new SeckillActivityDO();
        a.setProductDetailId("PD1");
        a.setSeckillPrice(new BigDecimal("50"));
        a.setTotalStock(100);
        a.setStartTime(new Date(System.currentTimeMillis() - 2 * 86400000L));
        a.setEndTime(new Date(System.currentTimeMillis() - 86400000L));
        ProductDetail pd = new ProductDetail();
        pd.setProductId("P1");
        when(productDetailService.findById("PD1")).thenReturn(pd);
        Product p = new Product();
        p.setId("P1");
        p.setMerchantId(10L);
        when(productService.findById("P1")).thenReturn(p);
        SeckillActivityDO created = seckillService.createMerchantActivity(10L, a);
        assertEquals(10L, created.getMerchantId());
    }

    // ===== listMerchantActivities 分页兜底分支 =====

    @Test
    void listMerchantActivities_normalizesPagingBounds() {
        when(activityDAO.findByMerchant(anyLong(), any())).thenReturn(Collections.emptyList());
        seckillService.listMerchantActivities(10L, null, 0, 0);
        seckillService.listMerchantActivities(10L, null, 1, 100);
        verify(activityDAO, org.mockito.Mockito.atLeast(2)).findByMerchant(anyLong(), any());
    }

    // ===== toggleMerchantActivity 全状态/空守卫分支 =====

    @Test
    void toggleMerchantActivity_activityIdNull_rejected() {
        assertThrows(RuntimeException.class, () -> seckillService.toggleMerchantActivity(10L, null, "ONGOING"));
    }

    @Test
    void toggleMerchantActivity_activityIdEmpty_rejected() {
        assertThrows(RuntimeException.class, () -> seckillService.toggleMerchantActivity(10L, "", "ONGOING"));
    }

    @Test
    void toggleMerchantActivity_invalidStatus_rejected() {
        // 覆盖 !ONGOING && !CLOSED && !ENDED 非法状态分支
        assertThrows(RuntimeException.class, () -> seckillService.toggleMerchantActivity(10L, "A1", "XXX"));
    }

    @Test
    void toggleMerchantActivity_closed_sameMerchant_ok() {
        // 覆盖 ONGOING 之外状态分支（不预热 Redis）
        SeckillActivityDO existing = baseActivity();
        existing.setId("A1");
        existing.setMerchantId(10L);
        when(activityDAO.findById("A1")).thenReturn(existing);
        seckillService.toggleMerchantActivity(10L, "A1", "CLOSED");
        verify(activityDAO).updateStatus("A1", "CLOSED");
    }

    @Test
    void toggleMerchantActivity_ended_sameMerchant_ok() {
        SeckillActivityDO existing = baseActivity();
        existing.setId("A1");
        existing.setMerchantId(10L);
        when(activityDAO.findById("A1")).thenReturn(existing);
        seckillService.toggleMerchantActivity(10L, "A1", "ENDED");
        verify(activityDAO).updateStatus("A1", "ENDED");
    }

    @Test
    void toggleMerchantActivity_notFound_rejected() {
        when(activityDAO.findById("Ax")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> seckillService.toggleMerchantActivity(10L, "Ax", "ONGOING"));
    }
}
