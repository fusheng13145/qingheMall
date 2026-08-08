package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.MerchantDAO;
import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 入驻商家服务 单元测试（M6 平台化）。
 * 覆盖：apply（幂等/校验/创建 PENDING）、registerMerchant、getByUserId、getById、audit（通过/驳回/越权）。
 */
class MerchantServiceImplTest {

    @Mock
    private MerchantDAO merchantDAO;

    @Mock
    private UserService userService;

    @InjectMocks
    private MerchantServiceImpl merchantService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private MerchantDO mockMerchant(Long id, Long userId, String status) {
        MerchantDO m = new MerchantDO();
        m.setId(id);
        m.setUserId(userId);
        m.setShopName("青禾小铺");
        m.setStatus(status);
        return m;
    }

    // ---------- apply ----------

    @Test
    void apply_nullUserId_shouldThrow() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> merchantService.apply(null, "店铺", "", ""));
        assertEquals("未登录", ex.getMessage());
        verify(merchantDAO, never()).findByUserId(anyLong());
        verify(merchantDAO, never()).insert(any());
    }

    @Test
    void apply_existingMerchant_shouldReturnExistingIdempotent() {
        MerchantDO exist = mockMerchant(10L, 7L, MerchantDO.STATUS_PENDING);
        when(merchantDAO.findByUserId(7L)).thenReturn(exist);

        MerchantDO result = merchantService.apply(7L, "新店名", "", "");

        assertEquals(exist, result);
        verify(merchantDAO, never()).insert(any());
    }

    @Test
    void apply_blankShopName_shouldThrow() {
        when(merchantDAO.findByUserId(7L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> merchantService.apply(7L, "   ", "", ""));
        assertEquals("请填写店铺名称", ex.getMessage());
        verify(merchantDAO, never()).insert(any());
    }

    @Test
    void apply_valid_shouldCreatePendingAndInsert() {
        when(merchantDAO.findByUserId(7L)).thenReturn(null);
        when(merchantDAO.insert(any(MerchantDO.class))).thenReturn(1);

        MerchantDO result = merchantService.apply(7L, "  青禾小铺  ", "logo.png", "描述");

        assertNotNull(result);
        assertEquals(MerchantDO.STATUS_PENDING, result.getStatus());
        // shopName 应被 trim
        assertEquals("青禾小铺", result.getShopName());
        assertEquals("logo.png", result.getShopLogo());
        assertEquals("描述", result.getShopDesc());

        ArgumentCaptor<MerchantDO> captor = ArgumentCaptor.forClass(MerchantDO.class);
        verify(merchantDAO).insert(captor.capture());
        assertEquals(MerchantDO.STATUS_PENDING, captor.getValue().getStatus());
        assertEquals("青禾小铺", captor.getValue().getShopName());
    }

    // ---------- registerMerchant ----------

    @Test
    void registerMerchant_blankShopName_shouldThrowBeforeRegister() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> merchantService.registerMerchant("merchant1", "123456", "  ", "", ""));
        assertEquals("请填写店铺名称", ex.getMessage());
        verify(userService, never()).register(anyString(), anyString(), anyString());
    }

    @Test
    void registerMerchant_valid_shouldRegisterAndApply() {
        User user = new User();
        user.setId(7L);
        when(userService.register(anyString(), anyString(), eq(UserDO.ROLE_MERCHANT))).thenReturn(user);
        when(merchantDAO.findByUserId(7L)).thenReturn(null);
        when(merchantDAO.insert(any(MerchantDO.class))).thenReturn(1);

        User result = merchantService.registerMerchant("merchant1", "123456", "青禾小铺", "", "");

        assertNotNull(result);
        assertEquals(7L, result.getId());
        verify(userService).register(anyString(), anyString(), eq(UserDO.ROLE_MERCHANT));
        verify(merchantDAO).insert(any(MerchantDO.class));
    }

    // ---------- getByUserId / getById ----------

    @Test
    void getByUserId_null_shouldReturnNull() {
        assertNull(merchantService.getByUserId(null));
        verify(merchantDAO, never()).findByUserId(anyLong());
    }

    @Test
    void getByUserId_valid_shouldDelegate() {
        MerchantDO m = mockMerchant(10L, 3L, MerchantDO.STATUS_ACTIVE);
        when(merchantDAO.findByUserId(3L)).thenReturn(m);
        assertEquals(m, merchantService.getByUserId(3L));
    }

    @Test
    void getById_null_shouldReturnNull() {
        assertNull(merchantService.getById(null));
        verify(merchantDAO, never()).findById(anyLong());
    }

    @Test
    void getById_valid_shouldDelegate() {
        MerchantDO m = mockMerchant(10L, 3L, MerchantDO.STATUS_ACTIVE);
        when(merchantDAO.findById(10L)).thenReturn(m);
        assertEquals(m, merchantService.getById(10L));
    }

    // ---------- audit ----------

    @Test
    void audit_merchantNotFound_shouldThrow() {
        when(merchantDAO.findById(100L)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> merchantService.audit(100L, true, null));
        assertEquals("商家不存在", ex.getMessage());
        verify(merchantDAO, never()).updateStatus(anyLong(), anyString(), any());
    }

    @Test
    void audit_approve_shouldActivateAndPromoteRole() {
        MerchantDO m = mockMerchant(100L, 5L, MerchantDO.STATUS_PENDING);
        when(merchantDAO.findById(100L)).thenReturn(m);
        // 模拟 updateStatus 的数据库语义：执行后 findById 返回更新后的状态
        when(merchantDAO.updateStatus(anyLong(), anyString(), any()))
                .thenAnswer(inv -> {
                    m.setStatus((String) inv.getArgument(1));
                    return 1;
                });

        MerchantDO result = merchantService.audit(100L, true, null);

        assertEquals(MerchantDO.STATUS_ACTIVE, result.getStatus());
        verify(merchantDAO).updateStatus(100L, MerchantDO.STATUS_ACTIVE, null);
        // 审核通过应同步将账号角色提升为商家
        verify(userService).updateRole(5L, UserDO.ROLE_MERCHANT);
    }

    @Test
    void audit_reject_blankReason_shouldThrow() {
        MerchantDO m = mockMerchant(100L, 5L, MerchantDO.STATUS_PENDING);
        when(merchantDAO.findById(100L)).thenReturn(m);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> merchantService.audit(100L, false, "  "));
        assertEquals("驳回必须填写原因", ex.getMessage());
        verify(merchantDAO, never()).updateStatus(anyLong(), anyString(), any());
    }

    @Test
    void audit_reject_withReason_shouldReject() {
        MerchantDO m = mockMerchant(100L, 5L, MerchantDO.STATUS_PENDING);
        when(merchantDAO.findById(100L)).thenReturn(m);
        // 模拟 updateStatus 的数据库语义：执行后 findById 返回更新后的状态
        when(merchantDAO.updateStatus(anyLong(), anyString(), any()))
                .thenAnswer(inv -> {
                    m.setStatus((String) inv.getArgument(1));
                    return 1;
                });

        MerchantDO result = merchantService.audit(100L, false, "资质不全");

        assertEquals(MerchantDO.STATUS_REJECTED, result.getStatus());
        verify(merchantDAO).updateStatus(100L, MerchantDO.STATUS_REJECTED, "资质不全");
        verify(userService, never()).updateRole(anyLong(), anyString());
    }
}
