package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CouponDAO;
import com.qinghe.mall.dataobject.CouponDO;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 券创建/更新 discount 非空列缺省防御测试（v1.11，复盘 §11.7-P1）。
 *
 * 背景：discount 为 NOT NULL 列，裸 API 未传时 insert/update 触发非空约束 500
 * （v7.5 复盘发现）。修复口径：创建缺省补 1.00（满减券中性值），更新沿用旧值。
 */
@ExtendWith(MockitoExtension.class)
class CouponDiscountDefaultTest {

    @Mock
    private CouponDAO couponDAO;

    @Mock
    private com.qinghe.mall.dao.UserCouponDAO userCouponDAO;

    @InjectMocks
    private CouponServiceImpl couponService;

    private CouponDO base() {
        CouponDO c = new CouponDO();
        c.setName("满100减10");
        c.setType("FULL_REDUCTION");
        c.setThreshold(new BigDecimal("100"));
        c.setAmount(new BigDecimal("10"));
        c.setTotal(100);
        java.util.Date now = new java.util.Date();
        c.setStartTime(new java.util.Date(now.getTime() - 60_000));
        c.setEndTime(new java.util.Date(now.getTime() + 3_600_000));
        return c;
    }

    @Test
    @DisplayName("平台创建未传 discount：缺省补 1.00 落库")
    void create_platform_discountDefaultsToOne() {
        when(couponDAO.insert(any(CouponDO.class))).thenReturn(1);

        couponService.createCoupon(base());

        ArgumentCaptor<CouponDO> captor = ArgumentCaptor.forClass(CouponDO.class);
        verify(couponDAO).insert(captor.capture());
        assertEquals(0, new BigDecimal("1.00").compareTo(captor.getValue().getDiscount()));
    }

    @Test
    @DisplayName("商家创建未传 discount：同样缺省补 1.00（validateCoupon 共用）")
    void createMerchant_discountDefaultsToOne() {
        when(couponDAO.insert(any(CouponDO.class))).thenReturn(1);

        couponService.createMerchantCoupon(7L, base());

        ArgumentCaptor<CouponDO> captor = ArgumentCaptor.forClass(CouponDO.class);
        verify(couponDAO).insert(captor.capture());
        assertEquals(0, new BigDecimal("1.00").compareTo(captor.getValue().getDiscount()));
        assertEquals(7L, captor.getValue().getMerchantId());
    }

    @Test
    @DisplayName("更新未传 discount：沿用库中旧值")
    void update_nullDiscount_keepsExisting() {
        CouponDO existing = base();
        existing.setId("c1");
        existing.setDiscount(new BigDecimal("0.85"));
        existing.setStatus("INACTIVE");
        when(couponDAO.findById("c1")).thenReturn(existing);
        when(couponDAO.findById("c1")).thenReturn(existing);
        when(couponDAO.update(any(CouponDO.class))).thenReturn(1);

        CouponDO patch = base();
        patch.setId("c1");
        patch.setDiscount(null);
        couponService.updateCoupon(patch);

        ArgumentCaptor<CouponDO> captor = ArgumentCaptor.forClass(CouponDO.class);
        verify(couponDAO).update(captor.capture());
        assertEquals(0, new BigDecimal("0.85").compareTo(captor.getValue().getDiscount()));
    }

    @Test
    @DisplayName("更新显式传入 discount：按传入值落库")
    void update_explicitDiscount_kept() {
        CouponDO existing = base();
        existing.setId("c1");
        existing.setStatus("INACTIVE");
        when(couponDAO.findById("c1")).thenReturn(existing);
        when(couponDAO.update(any(CouponDO.class))).thenReturn(1);

        CouponDO patch = base();
        patch.setId("c1");
        patch.setDiscount(new BigDecimal("0.90"));
        couponService.updateCoupon(patch);

        ArgumentCaptor<CouponDO> captor = ArgumentCaptor.forClass(CouponDO.class);
        verify(couponDAO).update(captor.capture());
        assertEquals(0, new BigDecimal("0.90").compareTo(captor.getValue().getDiscount()));
    }
}
