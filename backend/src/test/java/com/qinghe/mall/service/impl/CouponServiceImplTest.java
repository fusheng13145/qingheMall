package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CouponDAO;
import com.qinghe.mall.dao.UserCouponDAO;
import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.UserCouponDO;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 优惠券服务 单元测试（A2）
 * 覆盖：规则引擎（满减/折扣）、领取限领与防重复、核销锁定、释放、权威校验。
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponDAO couponDAO;

    @Mock
    private UserCouponDAO userCouponDAO;

    @InjectMocks
    private CouponServiceImpl couponService;

    private CouponDO fullReduction;
    private CouponDO discount;

    @BeforeEach
    void setUp() {
        fullReduction = new CouponDO();
        fullReduction.setId("C1");
        fullReduction.setType("FULL_REDUCTION");
        fullReduction.setThreshold(new BigDecimal("100"));
        fullReduction.setAmount(new BigDecimal("20"));
        fullReduction.setStatus("ACTIVE");
        fullReduction.setStartTime(new Date(System.currentTimeMillis() - 86400000L));
        fullReduction.setEndTime(new Date(System.currentTimeMillis() + 86400000L));

        discount = new CouponDO();
        discount.setId("C2");
        discount.setType("DISCOUNT");
        discount.setThreshold(BigDecimal.ZERO);
        discount.setDiscount(new BigDecimal("0.85"));
        discount.setMaxDiscount(new BigDecimal("30"));
        discount.setStatus("ACTIVE");
        discount.setStartTime(new Date(System.currentTimeMillis() - 86400000L));
        discount.setEndTime(new Date(System.currentTimeMillis() + 86400000L));
    }

    private UserCouponDO ucOf(CouponDO c) {
        UserCouponDO uc = new UserCouponDO();
        uc.setId("U1");
        uc.setUserId(1L);
        uc.setCouponId(c.getId());
        uc.setStatus("UNUSED");
        uc.setCouponType(c.getType());
        uc.setThreshold(c.getThreshold());
        uc.setCouponAmount(c.getAmount());
        uc.setCouponDiscount(c.getDiscount());
        uc.setCouponMaxDiscount(c.getMaxDiscount());
        return uc;
    }

    // ===== 规则引擎 =====
    @Test
    void calculateDiscount_fullReduction_met() {
        UserCouponDO uc = ucOf(fullReduction);
        assertEquals(new BigDecimal("20"), couponService.calculateDiscount(uc, new BigDecimal("150")));
    }

    @Test
    void calculateDiscount_fullReduction_notMet() {
        UserCouponDO uc = ucOf(fullReduction);
        assertEquals(BigDecimal.ZERO, couponService.calculateDiscount(uc, new BigDecimal("99")));
    }

    @Test
    void calculateDiscount_fullReduction_cappedByTotal() {
        UserCouponDO uc = ucOf(fullReduction);
        uc.setCouponAmount(new BigDecimal("200"));
        // 金额 150，满减 200 → 不超过订单额，实减 150
        assertEquals(new BigDecimal("150"), couponService.calculateDiscount(uc, new BigDecimal("150")));
    }

    @Test
    void calculateDiscount_discount_withCap() {
        UserCouponDO uc = ucOf(discount);
        // 300 * 0.15 = 45，封顶 30 → 30
        assertEquals(new BigDecimal("30"), couponService.calculateDiscount(uc, new BigDecimal("300")));
    }

    @Test
    void calculateDiscount_discount_noCapExceedTotal() {
        UserCouponDO uc = ucOf(discount);
        uc.setCouponMaxDiscount(null);
        // 100 * 0.15 = 15.00（BigDecimal 保留 2 位精度）
        assertEquals(new BigDecimal("15.00"), couponService.calculateDiscount(uc, new BigDecimal("100")));
    }

    // ===== 领取 =====
    @Test
    void claim_success() {
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        when(couponDAO.incrementIssued("C1")).thenReturn(1);
        couponService.claim("C1", 1L);
        verify(userCouponDAO).insert(any(UserCouponDO.class));
    }

    @Test
    void claim_soldOut() {
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        when(couponDAO.incrementIssued("C1")).thenReturn(0);
        assertThrows(RuntimeException.class, () -> couponService.claim("C1", 1L));
        verify(userCouponDAO, org.mockito.Mockito.never()).insert(any(UserCouponDO.class));
    }

    @Test
    void claim_inactive() {
        fullReduction.setStatus("INACTIVE");
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        assertThrows(RuntimeException.class, () -> couponService.claim("C1", 1L));
    }

    @Test
    void claim_alreadyClaimed() {
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        when(couponDAO.incrementIssued("C1")).thenReturn(1);
        doThrow(new org.springframework.dao.DuplicateKeyException("dup"))
                .when(userCouponDAO).insert(any(UserCouponDO.class));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> couponService.claim("C1", 1L));
        assertTrue(ex.getMessage().contains("已领取过"));
    }

    // ===== 核销与释放 =====
    @Test
    void lockCoupon_success() {
        when(userCouponDAO.lock(eq("U1"), eq(1L), anyString())).thenReturn(1);
        couponService.lockCoupon("U1", 1L, "QH123");
        verify(userCouponDAO).lock(eq("U1"), eq(1L), eq("QH123"));
    }

    @Test
    void lockCoupon_fail() {
        when(userCouponDAO.lock(eq("U1"), eq(1L), anyString())).thenReturn(0);
        assertThrows(RuntimeException.class, () -> couponService.lockCoupon("U1", 1L, "QH123"));
    }

    @Test
    void releaseCoupon_callsDao() {
        couponService.releaseCoupon("U1");
        verify(userCouponDAO).release("U1");
    }

    // ===== 权威校验 =====
    @Test
    void validateAndComputeDiscount_success() {
        UserCouponDO uc = ucOf(fullReduction);
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        assertEquals(new BigDecimal("20"),
                couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("150")));
    }

    @Test
    void validateAndComputeDiscount_wrongOwner() {
        UserCouponDO uc = ucOf(fullReduction);
        uc.setUserId(2L);
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("150")));
    }

    @Test
    void validateAndComputeDiscount_alreadyUsed() {
        UserCouponDO uc = ucOf(fullReduction);
        uc.setStatus("USED");
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("150")));
    }

    @Test
    void validateAndComputeDiscount_thresholdNotMet() {
        UserCouponDO uc = ucOf(fullReduction);
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        assertEquals(BigDecimal.ZERO,
                couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("50")));
    }
}
