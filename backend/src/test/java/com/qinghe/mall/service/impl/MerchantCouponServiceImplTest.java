package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CouponDAO;
import com.qinghe.mall.dataobject.CouponDO;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** #39 商家端券：归属强制、跨店越权拒绝、本店列表与店铺可用券查询。 */
@ExtendWith(MockitoExtension.class)
class MerchantCouponServiceImplTest {

    @Mock
    private CouponDAO couponDAO;

    @InjectMocks
    private CouponServiceImpl couponService;

    private CouponDO baseCoupon() {
        CouponDO c = new CouponDO();
        c.setName("满100减20");
        c.setType("FULL_REDUCTION");
        c.setThreshold(new BigDecimal("100"));
        c.setAmount(new BigDecimal("20"));
        c.setStartTime(new Date(System.currentTimeMillis() - 1000));
        c.setEndTime(new Date(System.currentTimeMillis() + 86400000L));
        return c;
    }

    @Test
    void createMerchantCoupon_setsMerchantId_andInserts() {
        CouponDO c = baseCoupon();
        CouponDO created = couponService.createMerchantCoupon(10L, c);
        assertEquals(10L, created.getMerchantId());
        assertNotNull(created.getId());
        assertEquals("ACTIVE", created.getStatus());
        verify(couponDAO).insert(c);
    }

    @Test
    void createMerchantCoupon_nullMerchant_throws() {
        assertThrows(RuntimeException.class, () -> couponService.createMerchantCoupon(null, baseCoupon()));
    }

    @Test
    void createMerchantCoupon_nullCoupon_throws() {
        // 覆盖 validateCoupon 内 coupon == null 守卫分支
        assertThrows(RuntimeException.class, () -> couponService.createMerchantCoupon(10L, null));
    }

    @Test
    void createMerchantCoupon_negativeDefaults_appliesFallback() {
        // 覆盖 validateCoupon 内 total<0 / perLimit<1 默认兜底分支
        CouponDO c = baseCoupon();
        c.setTotal(-5);
        c.setPerLimit(0);
        CouponDO created = couponService.createMerchantCoupon(10L, c);
        assertEquals(Integer.valueOf(0), created.getTotal());
        assertEquals(Integer.valueOf(1), created.getPerLimit());
    }

    @Test
    void updateMerchantCoupon_crossMerchant_rejected() {
        CouponDO existing = baseCoupon();
        existing.setId("C1");
        existing.setMerchantId(99L);
        when(couponDAO.findById("C1")).thenReturn(existing);
        CouponDO req = new CouponDO();
        req.setId("C1");
        assertThrows(RuntimeException.class, () -> couponService.updateMerchantCoupon(10L, req));
    }

    @Test
    void updateMerchantCoupon_sameMerchant_ok() {
        CouponDO existing = baseCoupon();
        existing.setId("C1");
        existing.setMerchantId(10L);
        existing.setStatus("INACTIVE");
        when(couponDAO.findById("C1")).thenReturn(existing);
        when(couponDAO.update(any())).thenReturn(1);
        CouponDO req = new CouponDO();
        req.setId("C1");
        req.setName("改");
        CouponDO updated = couponService.updateMerchantCoupon(10L, req);
        assertEquals("C1", updated.getId());
        verify(couponDAO).update(req);
    }

    @Test
    void updateMerchantCoupon_notFound_throws() {
        // 覆盖 existing == null 守卫分支
        when(couponDAO.findById("Cx")).thenReturn(null);
        CouponDO req = new CouponDO();
        req.setId("Cx");
        assertThrows(RuntimeException.class, () -> couponService.updateMerchantCoupon(10L, req));
    }

    @Test
    void updateMerchantCoupon_active_throws() {
        // 覆盖 "ACTIVE".equals(existing.getStatus()) 上架中不可改分支
        CouponDO existing = baseCoupon();
        existing.setId("C1");
        existing.setMerchantId(10L);
        existing.setStatus("ACTIVE");
        when(couponDAO.findById("C1")).thenReturn(existing);
        CouponDO req = new CouponDO();
        req.setId("C1");
        assertThrows(RuntimeException.class, () -> couponService.updateMerchantCoupon(10L, req));
    }

    @Test
    void toggleMerchantCoupon_crossMerchant_rejected() {
        CouponDO existing = baseCoupon();
        existing.setId("C1");
        existing.setMerchantId(99L);
        when(couponDAO.findById("C1")).thenReturn(existing);
        assertThrows(RuntimeException.class, () -> couponService.toggleMerchantCoupon(10L, "C1", "ACTIVE"));
    }

    @Test
    void toggleMerchantCoupon_sameMerchant_ok() {
        CouponDO existing = baseCoupon();
        existing.setId("C1");
        existing.setMerchantId(10L);
        when(couponDAO.findById("C1")).thenReturn(existing);
        couponService.toggleMerchantCoupon(10L, "C1", "ACTIVE");
        verify(couponDAO).updateStatus("C1", "ACTIVE");
    }

    @Test
    void toggleMerchantCoupon_invalidStatus_throws() {
        // 覆盖 !"ACTIVE" && !"INACTIVE" 非法状态分支（status 校验先于查库，故无需 stub）
        assertThrows(RuntimeException.class, () -> couponService.toggleMerchantCoupon(10L, "C1", "XXX"));
    }

    @Test
    void toggleMerchantCoupon_notFound_throws() {
        // 覆盖 existing == null 守卫分支
        when(couponDAO.findById("Cx")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> couponService.toggleMerchantCoupon(10L, "Cx", "ACTIVE"));
    }

    @Test
    void listMerchantCoupons_callsFindByMerchant() {
        when(couponDAO.findByMerchant(anyLong())).thenReturn(Collections.emptyList());
        couponService.listMerchantCoupons(10L, 1, 10);
        verify(couponDAO).findByMerchant(10L);
    }

    @Test
    void listMerchantCoupons_normalizesPagingBounds() {
        // 覆盖 pageNum<1 / pageSize<1 / pageSize>50 三个分页兜底分支
        when(couponDAO.findByMerchant(anyLong())).thenReturn(Collections.emptyList());
        couponService.listMerchantCoupons(10L, 0, 0);
        couponService.listMerchantCoupons(10L, 1, 100);
        verify(couponDAO, org.mockito.Mockito.atLeast(2)).findByMerchant(10L);
    }

    @Test
    void listActiveByMerchant_callsFindActiveByMerchant() {
        when(couponDAO.findActiveByMerchant(anyLong())).thenReturn(Collections.emptyList());
        couponService.listActiveByMerchant(10L);
        verify(couponDAO).findActiveByMerchant(10L);
    }
}
