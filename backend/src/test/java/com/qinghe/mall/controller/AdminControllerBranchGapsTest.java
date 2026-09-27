package com.qinghe.mall.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dataobject.BannerDO;
import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.dataobject.SettlementBillDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.service.BannerService;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.LogisticsService;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.RefundService;
import com.qinghe.mall.service.SeckillService;
import com.qinghe.mall.service.SettlementService;
import com.qinghe.mall.service.UserService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;

/**
 * 管理端分支缺口补测（v1.12，复盘 §11.7 清单）。
 *
 * 覆盖：settlement 四端点与 banner/秒杀/券/商家/订单发货的非 ADMIN 403 守卫、
 * updateUserRole 角色白名单 400、settlement 与 banner 的 ADMIN 透传分支。
 */
class AdminControllerBranchGapsTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private MerchantService merchantService;

    @Mock
    private CouponService couponService;

    @Mock
    private SeckillService seckillService;

    @Mock
    private LogisticsService logisticsService;

    @Mock
    private RefundService refundService;

    @Mock
    private SettlementService settlementService;

    @Mock
    private BannerService bannerService;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /** 构造带/不带 ADMIN 会话的请求 */
    private jakarta.servlet.http.HttpServletRequest req(String role) {
        org.springframework.mock.web.MockHttpServletRequest r =
                new org.springframework.mock.web.MockHttpServletRequest();
        if (role != null) {
            r.getSession().setAttribute("role", role);
            r.getSession().setAttribute("userId", 1L);
        }
        return r;
    }

    // ========== 非 ADMIN 403 守卫 ==========

    @Test
    @DisplayName("shipOrder 非 ADMIN 403")
    void shipOrder_nonAdmin_forbidden() {
        assertEquals(403, adminController.shipOrder("QH1", null, null, req(null)).getCode());
    }

    @Test
    @DisplayName("updateCoupon 非 ADMIN 403")
    void updateCoupon_nonAdmin_forbidden() {
        assertEquals(403, adminController.updateCoupon(new CouponDO(), req(null)).getCode());
    }

    @Test
    @DisplayName("createSeckill 非 ADMIN 403")
    void createSeckill_nonAdmin_forbidden() {
        assertEquals(403, adminController.createSeckill(new SeckillActivityDO(), req(null)).getCode());
    }

    @Test
    @DisplayName("listSeckills 非 ADMIN 403")
    void listSeckills_nonAdmin_forbidden() {
        assertEquals(403, adminController.listSeckills("", 1, 10, req(null)).getCode());
    }

    @Test
    @DisplayName("listMerchants 非 ADMIN 403")
    void listMerchants_nonAdmin_forbidden() {
        assertEquals(403, adminController.listMerchants("", 1, 10, req(null)).getCode());
    }

    @Test
    @DisplayName("generateSettlementBill 非 ADMIN 403")
    void generateSettlementBill_nonAdmin_forbidden() {
        assertEquals(403, adminController.generateSettlementBill(1L, req(null)).getCode());
    }

    @Test
    @DisplayName("listSettlementBills 非 ADMIN 403")
    void listSettlementBills_nonAdmin_forbidden() {
        assertEquals(403, adminController.listSettlementBills("", 1, 10, req(null)).getCode());
    }

    @Test
    @DisplayName("reviewSettlementBill 非 ADMIN 403")
    void reviewSettlementBill_nonAdmin_forbidden() {
        assertEquals(403, adminController.reviewSettlementBill("b1", true, null, req(null)).getCode());
    }

    @Test
    @DisplayName("settlementEntries 非 ADMIN 403")
    void settlementEntries_nonAdmin_forbidden() {
        assertEquals(403, adminController.settlementEntries("b1", req(null)).getCode());
    }

    @Test
    @DisplayName("updateBanner 非 ADMIN 403")
    void updateBanner_nonAdmin_forbidden() {
        assertEquals(403, adminController.updateBanner(new BannerDO(), req(null)).getCode());
    }

    // ========== ADMIN 透传分支 ==========

    @Test
    @DisplayName("generateSettlementBill：会话 userId 作为操作人透传")
    void generateSettlementBill_withAdmin_passesOperator() {
        SettlementBillDO bill = new SettlementBillDO();
        bill.setId("b1");
        when(settlementService.generateBill(1L, 1L)).thenReturn(bill);

        var result = adminController.generateSettlementBill(1L, req("ADMIN"));

        assertEquals("b1", result.getData().getId());
        verify(settlementService).generateBill(eq(1L), eq(1L));
    }

    @Test
    @DisplayName("listSettlementBills：状态过滤透传")
    void listSettlementBills_withAdmin_passesStatus() {
        Paging<SettlementBillDO> paging = new Paging<>();
        paging.setData(new ArrayList<>());
        when(settlementService.adminBills("PAID", 2, 20)).thenReturn(paging);

        var result = adminController.listSettlementBills("PAID", 2, 20, req("ADMIN"));

        assertEquals(paging, result.getData());
        verify(settlementService).adminBills("PAID", 2, 20);
    }

    @Test
    @DisplayName("reviewSettlementBill：审核人透传")
    void reviewSettlementBill_withAdmin_passesReviewer() {
        var result = adminController.reviewSettlementBill("b1", true, "ok", req("ADMIN"));

        assertEquals(200, result.getCode());
        verify(settlementService).reviewBill("b1", true, "ok", 1L);
    }

    @Test
    @DisplayName("settlementEntries：billId 透传")
    void settlementEntries_withAdmin_passesBillId() {
        when(settlementService.billEntries("b1")).thenReturn(new ArrayList<>());

        var result = adminController.settlementEntries("b1", req("ADMIN"));

        assertEquals(200, result.getCode());
        verify(settlementService).billEntries("b1");
    }

    @Test
    @DisplayName("updateBanner：ADMIN 透传请求体")
    void updateBanner_withAdmin_passesBody() {
        BannerDO saved = new BannerDO();
        saved.setId("b1");
        when(bannerService.updateBanner(any(BannerDO.class))).thenReturn(saved);

        var result = adminController.updateBanner(saved, req("ADMIN"));

        assertEquals("b1", result.getData().getId());
        verify(bannerService).updateBanner(saved);
    }

    // ========== updateUserRole 角色白名单 ==========

    @Test
    @DisplayName("updateUserRole：非法角色 400")
    void updateUserRole_invalidRole_badRequest() {
        var result = adminController.updateUserRole(2L, "SUPERGOD", req("ADMIN"));

        assertEquals(400, result.getCode());
        verify(userService, org.mockito.Mockito.never())
                .updateRole(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }
}
