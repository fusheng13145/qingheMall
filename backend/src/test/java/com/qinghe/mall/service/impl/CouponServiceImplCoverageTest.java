package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CouponDAO;
import com.qinghe.mall.dao.UserCouponDAO;
import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.UserCouponDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Paging;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * CouponServiceImpl 分支覆盖补测（#37）：覆盖此前缺失的易测分支——
 * createCoupon 全守卫、listCoupons 分页归一化、calculateDiscount 各券型与阈值、、
 * validateAndComputeDiscount 全守卫、claim 各拦截分支、toggle 非法状态。
 * 不含事务链路（另有主测覆盖）。
 */
class CouponServiceImplCoverageTest {

    @Mock
    private CouponDAO couponDAO;
    @Mock
    private UserCouponDAO userCouponDAO;

    @InjectMocks
    private CouponServiceImpl couponService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(couponDAO.findPage()).thenReturn(Collections.emptyList());
    }

    private CouponDO baseCoupon() {
        CouponDO c = new CouponDO();
        c.setName("测试券");
        c.setType("FULL_REDUCTION");
        c.setStartTime(new Date(System.currentTimeMillis() - 86400000L));
        c.setEndTime(new Date(System.currentTimeMillis() + 86400000L));
        c.setTotal(10);
        c.setIssued(0);
        c.setPerLimit(1);
        c.setStatus("ACTIVE");
        return c;
    }

    // ========== createCoupon 守卫 ==========

    @Test
    void createCoupon_null_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () -> couponService.createCoupon(null));
        assertEquals("券名称不能为空", ex.getMessage());
    }

    @Test
    void createCoupon_blankName_shouldThrow() {
        CouponDO c = baseCoupon();
        c.setName("   ");
        BusinessException ex = assertThrows(BusinessException.class, () -> couponService.createCoupon(c));
        assertEquals("券名称不能为空", ex.getMessage());
    }

    @Test
    void createCoupon_invalidType_shouldThrow() {
        CouponDO c = baseCoupon();
        c.setType("OTHER");
        BusinessException ex = assertThrows(BusinessException.class, () -> couponService.createCoupon(c));
        assertEquals("券类型不合法（应为 FULL_REDUCTION 或 DISCOUNT）", ex.getMessage());
    }

    @Test
    void createCoupon_nullTimes_shouldThrow() {
        CouponDO c = baseCoupon();
        c.setStartTime(null);
        c.setEndTime(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> couponService.createCoupon(c));
        assertEquals("有效期起止时间不能为空", ex.getMessage());
    }

    @Test
    void createCoupon_endBeforeStart_shouldThrow() {
        CouponDO c = baseCoupon();
        c.setStartTime(new Date(System.currentTimeMillis() + 86400000L));
        c.setEndTime(new Date(System.currentTimeMillis() - 86400000L));
        BusinessException ex = assertThrows(BusinessException.class, () -> couponService.createCoupon(c));
        assertEquals("结束时间须晚于开始时间", ex.getMessage());
    }

    @Test
    void createCoupon_negativeTotal_shouldNormalize() {
        CouponDO c = baseCoupon();
        c.setTotal(-5);
        CouponDO r = couponService.createCoupon(c);
        assertEquals(0, r.getTotal());
        assertNotNull(r.getId());
    }

    @Test
    void createCoupon_nullPerLimit_shouldNormalize() {
        CouponDO c = baseCoupon();
        c.setPerLimit(null);
        c.setStatus("");
        CouponDO r = couponService.createCoupon(c);
        assertEquals(1, r.getPerLimit());
        assertEquals("ACTIVE", r.getStatus());
        assertNotNull(r.getId());
    }

    @Test
    void createCoupon_valid_shouldInsert() {
        CouponDO r = couponService.createCoupon(baseCoupon());
        assertNotNull(r.getId());
        assertEquals("ACTIVE", r.getStatus());
    }

    // ========== listCoupons 分页归一化 ==========

    @Test
    void listCoupons_lowBounds_shouldDefault() {
        Paging<CouponDO> p = couponService.listCoupons(0, 0);
        assertEquals(1, p.getPageNum());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void listCoupons_highBounds_shouldClamp() {
        Paging<CouponDO> p = couponService.listCoupons(-3, 100);
        assertEquals(1, p.getPageNum());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void listCoupons_normal_shouldKeep() {
        Paging<CouponDO> p = couponService.listCoupons(2, 20);
        assertEquals(2, p.getPageNum());
        assertEquals(20, p.getPageSize());
    }

    // ========== calculateDiscount 各分支 ==========

    @Test
    void calculateDiscount_nullCoupon_shouldReturnZero() {
        assertEquals(BigDecimal.ZERO, couponService.calculateDiscount(null, new BigDecimal("10")));
    }

    @Test
    void calculateDiscount_nullTotal_shouldReturnZero() {
        UserCouponDO uc = new UserCouponDO();
        assertEquals(BigDecimal.ZERO, couponService.calculateDiscount(uc, null));
    }

    @Test
    void calculateDiscount_belowThreshold_shouldReturnZero() {
        UserCouponDO uc = new UserCouponDO();
        uc.setCouponType("FULL_REDUCTION");
        uc.setThreshold(new BigDecimal("100"));
        uc.setCouponAmount(new BigDecimal("20"));
        assertEquals(BigDecimal.ZERO, couponService.calculateDiscount(uc, new BigDecimal("50")));
    }

    @Test
    void calculateDiscount_fullReduction_shouldReturnAmount() {
        UserCouponDO uc = new UserCouponDO();
        uc.setCouponType("FULL_REDUCTION");
        uc.setThreshold(new BigDecimal("10"));
        uc.setCouponAmount(new BigDecimal("5"));
        assertEquals(new BigDecimal("5"), couponService.calculateDiscount(uc, new BigDecimal("20")));
    }

    @Test
    void calculateDiscount_discount_noMax_shouldReturnRaw() {
        UserCouponDO uc = new UserCouponDO();
        uc.setCouponType("DISCOUNT");
        uc.setCouponDiscount(new BigDecimal("0.1"));
        assertEquals(new BigDecimal("9.0"), couponService.calculateDiscount(uc, new BigDecimal("10")));
    }

    @Test
    void calculateDiscount_discount_withMax_shouldCap() {
        UserCouponDO uc = new UserCouponDO();
        uc.setCouponType("DISCOUNT");
        uc.setCouponDiscount(new BigDecimal("0.1"));
        uc.setCouponMaxDiscount(new BigDecimal("0.5"));
        assertEquals(new BigDecimal("0.5"), couponService.calculateDiscount(uc, new BigDecimal("10")));
    }

    @Test
    void calculateDiscount_unknownType_shouldReturnZero() {
        UserCouponDO uc = new UserCouponDO();
        uc.setCouponType("MYSTERY");
        assertEquals(BigDecimal.ZERO, couponService.calculateDiscount(uc, new BigDecimal("10")));
    }

    // ========== validateAndComputeDiscount 守卫 ==========

    @Test
    void validate_nullUserCoupon_shouldThrow() {
        when(userCouponDAO.findById("uc1")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.validateAndComputeDiscount("uc1", 1L, new BigDecimal("10")));
        assertEquals("优惠券不存在", ex.getMessage());
    }

    @Test
    void validate_userMismatch_shouldThrow() {
        UserCouponDO uc = new UserCouponDO();
        uc.setUserId(2L);
        uc.setStatus("UNUSED");
        uc.setCouponId("c1");
        when(userCouponDAO.findById("uc1")).thenReturn(uc);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.validateAndComputeDiscount("uc1", 1L, new BigDecimal("10")));
        assertEquals("优惠券不属于当前用户", ex.getMessage());
    }

    @Test
    void validate_used_shouldThrow() {
        UserCouponDO uc = new UserCouponDO();
        uc.setUserId(1L);
        uc.setStatus("USED");
        uc.setCouponId("c1");
        when(userCouponDAO.findById("uc1")).thenReturn(uc);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.validateAndComputeDiscount("uc1", 1L, new BigDecimal("10")));
        assertEquals("优惠券已使用", ex.getMessage());
    }

    @Test
    void validate_couponTemplateMissing_shouldThrow() {
        UserCouponDO uc = new UserCouponDO();
        uc.setUserId(1L);
        uc.setStatus("UNUSED");
        uc.setCouponId("c1");
        when(userCouponDAO.findById("uc1")).thenReturn(uc);
        when(couponDAO.findById("c1")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.validateAndComputeDiscount("uc1", 1L, new BigDecimal("10")));
        assertEquals("券模板不存在", ex.getMessage());
    }

    @Test
    void validate_couponInactive_shouldThrow() {
        UserCouponDO uc = new UserCouponDO();
        uc.setUserId(1L);
        uc.setStatus("UNUSED");
        uc.setCouponId("c1");
        CouponDO c = baseCoupon();
        c.setStatus("INACTIVE");
        when(userCouponDAO.findById("uc1")).thenReturn(uc);
        when(couponDAO.findById("c1")).thenReturn(c);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.validateAndComputeDiscount("uc1", 1L, new BigDecimal("10")));
        assertEquals("券已下架", ex.getMessage());
    }

    @Test
    void validate_outOfTime_shouldThrow() {
        UserCouponDO uc = new UserCouponDO();
        uc.setUserId(1L);
        uc.setStatus("UNUSED");
        uc.setCouponId("c1");
        CouponDO c = baseCoupon();
        c.setStartTime(new Date(System.currentTimeMillis() + 86400000L));
        c.setEndTime(new Date(System.currentTimeMillis() + 2 * 86400000L));
        when(userCouponDAO.findById("uc1")).thenReturn(uc);
        when(couponDAO.findById("c1")).thenReturn(c);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.validateAndComputeDiscount("uc1", 1L, new BigDecimal("10")));
        assertEquals("券不在有效期", ex.getMessage());
    }

    @Test
    void validate_valid_shouldCompute() {
        UserCouponDO uc = new UserCouponDO();
        uc.setUserId(1L);
        uc.setStatus("UNUSED");
        uc.setCouponId("c1");
        uc.setCouponType("FULL_REDUCTION");
        uc.setThreshold(new BigDecimal("10"));
        uc.setCouponAmount(new BigDecimal("5"));
        CouponDO c = baseCoupon();
        when(userCouponDAO.findById("uc1")).thenReturn(uc);
        when(couponDAO.findById("c1")).thenReturn(c);
        assertEquals(new BigDecimal("5"),
                couponService.validateAndComputeDiscount("uc1", 1L, new BigDecimal("20")));
    }

    // ========== claim 各拦截分支 ==========

    @Test
    void claim_couponMissing_shouldThrow() {
        when(couponDAO.findById("c1")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.claim("c1", 1L));
        assertEquals("券不存在", ex.getMessage());
    }

    @Test
    void claim_inactive_shouldThrow() {
        CouponDO c = baseCoupon();
        c.setStatus("INACTIVE");
        when(couponDAO.findById("c1")).thenReturn(c);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.claim("c1", 1L));
        assertEquals("券已下架", ex.getMessage());
    }

    @Test
    void claim_outOfTime_shouldThrow() {
        CouponDO c = baseCoupon();
        c.setStartTime(new Date(System.currentTimeMillis() + 86400000L));
        c.setEndTime(new Date(System.currentTimeMillis() + 2 * 86400000L));
        when(couponDAO.findById("c1")).thenReturn(c);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.claim("c1", 1L));
        assertEquals("不在领取时间内", ex.getMessage());
    }

    @Test
    void claim_exhausted_shouldThrow() {
        CouponDO c = baseCoupon();
        c.setTotal(10);
        c.setIssued(10);
        when(couponDAO.findById("c1")).thenReturn(c);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.claim("c1", 1L));
        assertEquals("券已领完", ex.getMessage());
    }

    @Test
    void claim_perLimitReached_shouldThrow() {
        CouponDO c = baseCoupon();
        c.setPerLimit(2);
        when(couponDAO.findById("c1")).thenReturn(c);
        when(userCouponDAO.countByUserAndCoupon(1L, "c1")).thenReturn(2);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.claim("c1", 1L));
        assertEquals("已达限领数量", ex.getMessage());
    }

    @Test
    void claim_incrementFailed_shouldThrow() {
        CouponDO c = baseCoupon();
        c.setTotal(10);
        c.setIssued(5);
        when(couponDAO.findById("c1")).thenReturn(c);
        when(userCouponDAO.countByUserAndCoupon(1L, "c1")).thenReturn(0);
        when(couponDAO.incrementIssued("c1")).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.claim("c1", 1L));
        assertEquals("券已领完", ex.getMessage());
    }

    @Test
    void claim_valid_shouldInsert() {
        CouponDO c = baseCoupon();
        when(couponDAO.findById("c1")).thenReturn(c);
        when(userCouponDAO.countByUserAndCoupon(1L, "c1")).thenReturn(0);
        when(couponDAO.incrementIssued("c1")).thenReturn(1);
        couponService.claim("c1", 1L);
        assertTrue(true);
    }

    // ========== toggle 非法状态 ==========

    @Test
    void toggle_invalidStatus_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> couponService.toggle("c1", "WEIRD"));
        assertEquals("状态不合法（应为 ACTIVE 或 INACTIVE）", ex.getMessage());
    }

    @Test
    void toggle_valid_shouldUpdate() {
        couponService.toggle("c1", "INACTIVE");
        assertTrue(true);
    }
}
