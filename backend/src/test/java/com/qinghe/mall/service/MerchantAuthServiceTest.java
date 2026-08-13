package com.qinghe.mall.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * MerchantAuthService 六重守卫单元覆盖（从 MerchantController 抽取，#39 复用）。
 * 校验链：未登录(401) / 非商家(403) / 未入驻(403) / 审核中(403) / 驳回(403) / 禁用(403) / ACTIVE 放行。
 */
@ExtendWith(MockitoExtension.class)
class MerchantAuthServiceTest {

    @Mock
    private MerchantService merchantService;

    @InjectMocks
    private MerchantAuthService authService;

    private MerchantDO merchant(String status) {
        MerchantDO m = new MerchantDO();
        m.setId(10L);
        m.setUserId(1L);
        m.setStatus(status);
        return m;
    }

    private MockHttpServletRequest sessionReq(String role) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.getSession().setAttribute("userId", 1L);
        req.getSession().setAttribute("role", role);
        return req;
    }

    @Test
    void unauthenticated_throws401() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthException ex = assertThrows(AuthException.class, () -> authService.checkMerchant(req));
        assertEquals(401, ex.getCode());
    }

    @Test
    void roleNotMerchant_throws403() {
        MockHttpServletRequest req = sessionReq(UserDO.ROLE_USER);
        AuthException ex = assertThrows(AuthException.class, () -> authService.checkMerchant(req));
        assertEquals(403, ex.getCode());
    }

    @Test
    void pending_throws403() {
        MockHttpServletRequest req = sessionReq(UserDO.ROLE_MERCHANT);
        when(merchantService.getByUserId(1L)).thenReturn(merchant(MerchantDO.STATUS_PENDING));
        AuthException ex = assertThrows(AuthException.class, () -> authService.checkMerchant(req));
        assertEquals(403, ex.getCode());
    }

    @Test
    void rejected_throws403() {
        MockHttpServletRequest req = sessionReq(UserDO.ROLE_MERCHANT);
        MerchantDO m = merchant(MerchantDO.STATUS_REJECTED);
        m.setRejectReason("资质不全");
        when(merchantService.getByUserId(1L)).thenReturn(m);
        assertThrows(AuthException.class, () -> authService.checkMerchant(req));
    }

    @Test
    void disabled_throws403() {
        MockHttpServletRequest req = sessionReq(UserDO.ROLE_MERCHANT);
        when(merchantService.getByUserId(1L)).thenReturn(merchant(MerchantDO.STATUS_DISABLED));
        assertThrows(AuthException.class, () -> authService.checkMerchant(req));
    }

    @Test
    void active_returnsMerchant() {
        MockHttpServletRequest req = sessionReq(UserDO.ROLE_MERCHANT);
        when(merchantService.getByUserId(1L)).thenReturn(merchant(MerchantDO.STATUS_ACTIVE));
        MerchantDO m = authService.checkMerchant(req);
        assertEquals(10L, m.getId());
    }

    @Test
    void notYetMerchant_throws403() {
        // 会话角色为 MERCHANT 但商家记录不存在：覆盖 merchant == null 守卫分支
        MockHttpServletRequest req = sessionReq(UserDO.ROLE_MERCHANT);
        when(merchantService.getByUserId(1L)).thenReturn(null);
        AuthException ex = assertThrows(AuthException.class, () -> authService.checkMerchant(req));
        assertEquals(403, ex.getCode());
    }

    @Test
    void rejected_nullReason_throws403() {
        // 驳回且无驳回原因：覆盖 getRejectReason() == null 三元分支
        MockHttpServletRequest req = sessionReq(UserDO.ROLE_MERCHANT);
        MerchantDO m = merchant(MerchantDO.STATUS_REJECTED);
        m.setRejectReason(null);
        when(merchantService.getByUserId(1L)).thenReturn(m);
        assertThrows(AuthException.class, () -> authService.checkMerchant(req));
    }
}
