package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CouponDAO;
import com.qinghe.mall.dao.UserCouponDAO;
import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.UserCouponDO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 优惠券服务补充测试（P1：补齐管理端校验/领取限领/权威校验/排序缺口）。
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceImplAdditionalTest {

    @Mock
    private CouponDAO couponDAO;
    @Mock
    private UserCouponDAO userCouponDAO;

    @InjectMocks
    private CouponServiceImpl couponService;

    private CouponDO activeCoupon;

    @BeforeEach
    void setUp() {
        activeCoupon = new CouponDO();
        activeCoupon.setId("C1");
        activeCoupon.setName("满100减20");
        activeCoupon.setType("FULL_REDUCTION");
        activeCoupon.setAmount(new BigDecimal("20"));
        activeCoupon.setThreshold(new BigDecimal("100"));
        activeCoupon.setStatus("ACTIVE");
        activeCoupon.setStartTime(new Date(System.currentTimeMillis() - 86400000L));
        activeCoupon.setEndTime(new Date(System.currentTimeMillis() + 86400000L));
        activeCoupon.setTotal(100);
        activeCoupon.setIssued(0);
        activeCoupon.setPerLimit(1);
    }

    // ============ createCoupon 管理端校验 ============

    @Test
    @DisplayName("createCoupon 名称缺失拒绝")
    void createCoupon_blankName_throws() {
        CouponDO c = activeCoupon;
        c.setName(null);
        assertThrows(RuntimeException.class, () -> couponService.createCoupon(c), "券名称不能为空");
        assertThrows(RuntimeException.class, () -> couponService.createCoupon(null));
    }

    @Test
    @DisplayName("createCoupon 类型非法拒绝")
    void createCoupon_badType_throws() {
        activeCoupon.setType("PERCENT_OFF");
        assertThrows(RuntimeException.class, () -> couponService.createCoupon(activeCoupon));
    }

    @Test
    @DisplayName("createCoupon 起止时间缺失/倒置拒绝")
    void createCoupon_badTime_throws() {
        activeCoupon.setStartTime(null);
        assertThrows(RuntimeException.class, () -> couponService.createCoupon(activeCoupon));
        activeCoupon.setStartTime(new Date(System.currentTimeMillis() + 86400000L));
        activeCoupon.setEndTime(new Date(System.currentTimeMillis() - 86400000L));
        assertThrows(RuntimeException.class, () -> couponService.createCoupon(activeCoupon));
    }

    @Test
    @DisplayName("createCoupon 成功：填充默认值并落库")
    void createCoupon_success_fillsDefaults() {
        when(couponDAO.insert(any(CouponDO.class))).thenReturn(1);
        CouponDO c = activeCoupon;
        c.setTotal(null);
        c.setIssued(null);
        c.setPerLimit(null);
        c.setStatus(null);

        CouponDO created = couponService.createCoupon(c);

        assertNotNull(created.getId());
        assertEquals(0, created.getTotal());
        assertEquals(0, created.getIssued());
        assertEquals(1, created.getPerLimit());
        assertEquals("ACTIVE", created.getStatus());
        verify(couponDAO).insert(created);
    }

    // ============ updateCoupon / toggle ============

    @Test
    @DisplayName("updateCoupon 券不存在拒绝")
    void updateCoupon_notFound_throws() {
        when(couponDAO.findById("C1")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> couponService.updateCoupon(activeCoupon));
    }

    @Test
    @DisplayName("updateCoupon 上架中不可修改")
    void updateCoupon_active_throws() {
        when(couponDAO.findById("C1")).thenReturn(activeCoupon);
        assertThrows(RuntimeException.class, () -> couponService.updateCoupon(activeCoupon),
                "上架中的券不可修改");
    }

    @Test
    @DisplayName("updateCoupon 下架后可修改")
    void updateCoupon_inactive_success() {
        CouponDO existing = activeCoupon;
        existing.setStatus("INACTIVE");
        when(couponDAO.findById("C1")).thenReturn(existing);
        when(couponDAO.update(any(CouponDO.class))).thenReturn(1);
        when(couponDAO.findById("C1")).thenReturn(existing);

        CouponDO updated = couponService.updateCoupon(activeCoupon);

        assertNotNull(updated);
        verify(couponDAO).update(activeCoupon);
    }

    @Test
    @DisplayName("toggle 非法状态拒绝 / 合法透传")
    void toggle_validatesStatus() {
        assertThrows(RuntimeException.class, () -> couponService.toggle("C1", "PAUSED"));
        couponService.toggle("C1", "INACTIVE");
        verify(couponDAO).updateStatus("C1", "INACTIVE");
    }

    // ============ claim 领取边界 ============

    @Test
    @DisplayName("claim 不在领取时间内拒绝")
    void claim_outsideWindow_throws() {
        activeCoupon.setStartTime(new Date(System.currentTimeMillis() + 86400000L));
        when(couponDAO.findById("C1")).thenReturn(activeCoupon);
        assertThrows(RuntimeException.class, () -> couponService.claim("C1", 1L), "不在领取时间内");
    }

    @Test
    @DisplayName("claim 已领完（issued>=total）拒绝")
    void claim_soldOut_throws() {
        activeCoupon.setIssued(100);
        when(couponDAO.findById("C1")).thenReturn(activeCoupon);
        assertThrows(RuntimeException.class, () -> couponService.claim("C1", 1L), "券已领完");
    }

    @Test
    @DisplayName("claim perLimit>1 达到限领数拒绝")
    void claim_perLimitReached_throws() {
        activeCoupon.setPerLimit(3);
        when(couponDAO.findById("C1")).thenReturn(activeCoupon);
        when(userCouponDAO.countByUserAndCoupon(1L, "C1")).thenReturn(3);

        assertThrows(RuntimeException.class, () -> couponService.claim("C1", 1L), "已达限领数量");
    }

    @Test
    @DisplayName("claim incrementIssued 失败（并发超发）拒绝")
    void claim_incrementFail_throws() {
        when(couponDAO.findById("C1")).thenReturn(activeCoupon);
        when(couponDAO.incrementIssued("C1")).thenReturn(0);

        assertThrows(RuntimeException.class, () -> couponService.claim("C1", 1L), "券已领完");
    }

    @Test
    @DisplayName("claim 成功：自增 issued 并插入用户券")
    void claim_success() {
        when(couponDAO.findById("C1")).thenReturn(activeCoupon);
        when(couponDAO.incrementIssued("C1")).thenReturn(1);
        when(userCouponDAO.insert(any(UserCouponDO.class))).thenReturn(1);

        couponService.claim("C1", 1L);

        verify(userCouponDAO).insert(any(UserCouponDO.class));
    }

    // ============ available 排序 ============

    @Test
    @DisplayName("available 按优惠额降序")
    void available_sortedByDiscountDesc() {
        UserCouponDO ucA = new UserCouponDO();
        ucA.setId("A");
        ucA.setUserId(1L);
        ucA.setCouponType("FULL_REDUCTION");
        ucA.setThreshold(BigDecimal.ZERO);
        ucA.setCouponAmount(new BigDecimal("10"));
        UserCouponDO ucB = new UserCouponDO();
        ucB.setId("B");
        ucB.setUserId(1L);
        ucB.setCouponType("FULL_REDUCTION");
        ucB.setThreshold(BigDecimal.ZERO);
        ucB.setCouponAmount(new BigDecimal("30"));
        List<UserCouponDO> list = new ArrayList<>(List.of(ucA, ucB));
        when(userCouponDAO.findUsable(1L, new BigDecimal("200"))).thenReturn(list);

        List<UserCouponDO> sorted = couponService.available(1L, new BigDecimal("200"));

        assertEquals("B", sorted.get(0).getId());
        assertEquals("A", sorted.get(1).getId());
    }

    @Test
    @DisplayName("available amount 为 null 按 0 处理")
    void available_nullAmount() {
        when(userCouponDAO.findUsable(1L, BigDecimal.ZERO)).thenReturn(new ArrayList<>());
        assertTrue(couponService.available(1L, null).isEmpty());
    }

    // ============ validateAndComputeDiscount 权威校验 ============

    @Test
    @DisplayName("validateAndComputeDiscount 券不存在拒绝")
    void validate_couponNotFound_throws() {
        when(userCouponDAO.findById("U1")).thenReturn(null);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("200"), null));
    }

    @Test
    @DisplayName("validateAndComputeDiscount 非本人券拒绝")
    void validate_wrongOwner_throws() {
        UserCouponDO uc = new UserCouponDO();
        uc.setId("U1");
        uc.setUserId(99L);
        uc.setStatus("UNUSED");
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("200"), null),
                "优惠券不属于当前用户");
    }

    @Test
    @DisplayName("validateAndComputeDiscount 已使用券拒绝")
    void validate_usedCoupon_throws() {
        UserCouponDO uc = new UserCouponDO();
        uc.setId("U1");
        uc.setUserId(1L);
        uc.setStatus("USED");
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("200"), null));
    }

    @Test
    @DisplayName("validateAndComputeDiscount 券模板已下架/过期拒绝")
    void validate_inactiveTemplate_throws() {
        UserCouponDO uc = new UserCouponDO();
        uc.setId("U1");
        uc.setUserId(1L);
        uc.setStatus("UNUSED");
        uc.setCouponId("C1");
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        CouponDO template = activeCoupon;
        template.setStatus("INACTIVE");
        when(couponDAO.findById("C1")).thenReturn(template);
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("200"), null));

        template.setStatus("ACTIVE");
        template.setEndTime(new Date(System.currentTimeMillis() - 1000L));
        assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("200"), null));
    }

    @Test
    @DisplayName("validateAndComputeDiscount 全链路校验通过返回折扣额")
    void validate_success_returnsDiscount() {
        UserCouponDO uc = new UserCouponDO();
        uc.setId("U1");
        uc.setUserId(1L);
        uc.setStatus("UNUSED");
        uc.setCouponId("C1");
        uc.setCouponType("FULL_REDUCTION");
        uc.setThreshold(new BigDecimal("100"));
        uc.setCouponAmount(new BigDecimal("20"));
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        when(couponDAO.findById("C1")).thenReturn(activeCoupon);

        BigDecimal discount = couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("200"), null);

        assertEquals(0, new BigDecimal("20").compareTo(discount));
    }

    // ============ A1：店铺券核销归属一致性（merchant_id 校验） ============

    /** 构造未用券持有记录 + 指定模板存根 */
    private UserCouponDO stubCouponWith(CouponDO template) {
        UserCouponDO uc = new UserCouponDO();
        uc.setId("U1");
        uc.setUserId(1L);
        uc.setStatus("UNUSED");
        uc.setCouponId(template.getId());
        uc.setCouponType("FULL_REDUCTION");
        uc.setThreshold(new BigDecimal("100"));
        uc.setCouponAmount(new BigDecimal("20"));
        when(userCouponDAO.findById("U1")).thenReturn(uc);
        when(couponDAO.findById(template.getId())).thenReturn(template);
        return uc;
    }

    @Test
    @DisplayName("A1 店铺券核销于本店商品：放行")
    void validate_shopCoupon_sameShop_passes() {
        CouponDO template = activeCoupon;
        template.setMerchantId(5L);
        stubCouponWith(template);

        BigDecimal discount = couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("200"), 5L);

        assertEquals(0, new BigDecimal("20").compareTo(discount));
    }

    @Test
    @DisplayName("A1 店铺券核销于他店商品：拒绝")
    void validate_shopCoupon_crossShop_throws() {
        CouponDO template = activeCoupon;
        template.setMerchantId(5L);
        stubCouponWith(template);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("200"), 9L));
        assertEquals("店铺券仅可用于本店商品", ex.getMessage());
    }

    @Test
    @DisplayName("A1 店铺券核销于平台自营/无归属商品：拒绝")
    void validate_shopCoupon_platformProduct_throws() {
        CouponDO template = activeCoupon;
        template.setMerchantId(5L);
        stubCouponWith(template);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("200"), null));
        assertEquals("店铺券仅可用于本店商品", ex.getMessage());
    }

    @Test
    @DisplayName("A1 平台券（merchant_id=NULL）核销于任意店铺商品：放行")
    void validate_platformCoupon_anyShop_passes() {
        // activeCoupon 未设 merchantId（NULL=平台券）
        stubCouponWith(activeCoupon);

        BigDecimal discount = couponService.validateAndComputeDiscount("U1", 1L, new BigDecimal("200"), 9L);

        assertEquals(0, new BigDecimal("20").compareTo(discount));
    }
}
