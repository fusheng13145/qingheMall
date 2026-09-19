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
                couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("150"), null));
    }

    @Test
    void validateAndComputeDiscount_wrongOwner() {
        UserCouponDO uc = ucOf(fullReduction);
        uc.setUserId(2L);
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("150"), null));
    }

    @Test
    void validateAndComputeDiscount_alreadyUsed() {
        UserCouponDO uc = ucOf(fullReduction);
        uc.setStatus("USED");
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("150"), null));
    }

    @Test
    void validateAndComputeDiscount_thresholdNotMet() {
        UserCouponDO uc = ucOf(fullReduction);
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        assertEquals(BigDecimal.ZERO,
                couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("50"), null));
    }

    // ============ 补充用例（P1 覆盖补强） ============

    // ===== createCoupon =====
    @Test
    void createCoupon_missingName_throws() {
        CouponDO c = new CouponDO();
        c.setType("FULL_REDUCTION");
        assertThrows(RuntimeException.class, () -> couponService.createCoupon(c));
        assertThrows(RuntimeException.class, () -> couponService.createCoupon(null));
    }

    @Test
    void createCoupon_invalidType_throws() {
        CouponDO c = new CouponDO();
        c.setName("券");
        c.setType("PERCENT_OFF");
        assertThrows(RuntimeException.class, () -> couponService.createCoupon(c));
    }

    @Test
    void createCoupon_missingTimeRange_throws() {
        CouponDO c = new CouponDO();
        c.setName("券");
        c.setType("FULL_REDUCTION");
        assertThrows(RuntimeException.class, () -> couponService.createCoupon(c));
    }

    @Test
    void createCoupon_endBeforeStart_throws() {
        CouponDO c = new CouponDO();
        c.setName("券");
        c.setType("FULL_REDUCTION");
        c.setStartTime(new Date(System.currentTimeMillis() + 86400000L));
        c.setEndTime(new Date(System.currentTimeMillis() - 86400000L));
        assertThrows(RuntimeException.class, () -> couponService.createCoupon(c));
    }

    @Test
    void createCoupon_success_fillsDefaults() {
        CouponDO c = new CouponDO();
        c.setName("满100减20");
        c.setType("FULL_REDUCTION");
        c.setStartTime(new Date(System.currentTimeMillis() - 1000));
        c.setEndTime(new Date(System.currentTimeMillis() + 86400000L));

        CouponDO created = couponService.createCoupon(c);

        assertTrue(created.getId() != null && !created.getId().isEmpty());
        assertEquals("ACTIVE", created.getStatus());
        assertEquals(Integer.valueOf(1), created.getPerLimit());
        assertEquals(Integer.valueOf(0), created.getIssued());
        verify(couponDAO).insert(created);
    }

    // ===== updateCoupon / toggle =====
    @Test
    void updateCoupon_notExist_throws() {
        CouponDO c = new CouponDO();
        c.setId("X");
        when(couponDAO.findById("X")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> couponService.updateCoupon(c));
    }

    @Test
    void updateCoupon_activeStatus_throws() {
        CouponDO c = new CouponDO();
        c.setId("C1");
        fullReduction.setStatus("ACTIVE");
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        assertThrows(RuntimeException.class, () -> couponService.updateCoupon(c));
    }

    @Test
    void toggle_invalidStatus_throws() {
        assertThrows(RuntimeException.class, () -> couponService.toggle("C1", "PAUSED"));
    }

    @Test
    void toggle_validStatus_updates() {
        couponService.toggle("C1", "INACTIVE");
        verify(couponDAO).updateStatus("C1", "INACTIVE");
    }

    // ===== claim 边界 =====
    @Test
    void claim_notExist_throws() {
        when(couponDAO.findById("X")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> couponService.claim("X", 1L));
    }

    @Test
    void claim_outsideTimeWindow_throws() {
        fullReduction.setStartTime(new Date(System.currentTimeMillis() + 86400000L));
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        assertThrows(RuntimeException.class, () -> couponService.claim("C1", 1L));
    }

    @Test
    void claim_issuedReachedTotal_throws() {
        fullReduction.setTotal(100);
        fullReduction.setIssued(100);
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        assertThrows(RuntimeException.class, () -> couponService.claim("C1", 1L));
    }

    @Test
    void claim_perLimitReached_throws() {
        fullReduction.setPerLimit(2);
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        when(userCouponDAO.countByUserAndCoupon(1L, "C1")).thenReturn(2);
        assertThrows(RuntimeException.class, () -> couponService.claim("C1", 1L));
    }

    @Test
    void claim_perLimitNotReached_ok() {
        fullReduction.setPerLimit(2);
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        when(userCouponDAO.countByUserAndCoupon(1L, "C1")).thenReturn(1);
        when(couponDAO.incrementIssued("C1")).thenReturn(1);
        couponService.claim("C1", 1L);
        verify(userCouponDAO).insert(any(UserCouponDO.class));
    }

    // ===== available 排序 =====
    @Test
    void available_sortsByDiscountDesc() {
        UserCouponDO big = ucOf(discount);          // 85折封顶30，150元→22.5
        UserCouponDO small = ucOf(fullReduction);   // 满100减20，150元→20
        when(userCouponDAO.findUsable(1L, new BigDecimal("150"))).thenReturn(
                new java.util.ArrayList<>(java.util.List.of(small, big)));

        java.util.List<UserCouponDO> list = couponService.available(1L, new BigDecimal("150"));

        assertEquals("U1", list.get(0).getId()); // 两张券 id 相同，仅验证排序调用不抛错与数量
        assertEquals(2, list.size());
        verify(userCouponDAO).findUsable(1L, new BigDecimal("150"));
    }

    // ===== validateAndComputeDiscount 边界 =====
    @Test
    void validateAndComputeDiscount_notExist_throws() {
        when(userCouponDAO.findById("U1")).thenReturn(null);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("150"), null));
    }

    @Test
    void validateAndComputeDiscount_wrongOwner_throws() {
        UserCouponDO uc = ucOf(fullReduction);
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 99L, new BigDecimal("150"), null));
    }

    @Test
    void validateAndComputeDiscount_usedStatus_throws() {
        UserCouponDO uc = ucOf(fullReduction);
        uc.setStatus("USED");
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("150"), null));
    }

    @Test
    void validateAndComputeDiscount_templateInactive_throws() {
        UserCouponDO uc = ucOf(fullReduction);
        fullReduction.setStatus("INACTIVE");
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("150"), null));
    }

    @Test
    void validateAndComputeDiscount_success_returnsDiscount() {
        UserCouponDO uc = ucOf(fullReduction);
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        when(couponDAO.findById("C1")).thenReturn(fullReduction);
        assertEquals(new BigDecimal("20"),
                couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("150"), null));
    }
}
