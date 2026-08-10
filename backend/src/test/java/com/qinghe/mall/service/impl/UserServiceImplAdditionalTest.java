package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.UserDAO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.model.User;
import com.qinghe.mall.util.CommonUtils;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 用户服务补充测试（P1：补齐角色校验/MD5 开关/查询/分页/资料更新缺口）。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplAdditionalTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private UserServiceImpl service;

    private UserDO userDO;

    @BeforeEach
    void setUp() {
        userDO = new UserDO();
        userDO.setId(1L);
        userDO.setUserName("alice");
        userDO.setNickName("爱丽丝");
        userDO.setRole(UserDO.ROLE_USER);
        userDO.setPwd("$2a$10$abcdefghijklmnopqrstuv");
    }

    // ============ register 角色校验 ============

    @Test
    @DisplayName("register 非法角色拒绝")
    void register_invalidRole_throws() {
        assertThrows(RuntimeException.class, () -> service.register("x", "pwd", "SUPERUSER"),
                "非法的注册角色");
    }

    @Test
    @DisplayName("register 商家角色可注册")
    void register_merchantRole_ok() {
        when(userDAO.findByUserName("shop")).thenReturn(null);
        when(userDAO.insert(any(UserDO.class))).thenReturn(1);

        User created = service.register("shop", "pwd123", UserDO.ROLE_MERCHANT);

        assertEquals(UserDO.ROLE_MERCHANT, created.getRole());
        assertEquals("shop", created.getUserName());
        verify(userDAO).insert(any(UserDO.class));
    }

    // ============ login MD5 开关 ============

    @Test
    @DisplayName("allowMd5Login=false 时拒绝 MD5 存量密码")
    void login_md5Disabled_rejects() {
        ReflectionTestUtils.setField(service, "allowMd5Login", false);
        UserDO legacy = new UserDO();
        legacy.setId(2L);
        legacy.setUserName("old");
        legacy.setPwd(CommonUtils.md5("secret" + "_qh2050"));
        when(userDAO.findByUserName("old")).thenReturn(legacy);

        assertThrows(RuntimeException.class, () -> service.login("old", "secret"), "密码错误");
        // MD5 分支关闭，不应触发升级写库
        verify(userDAO, org.mockito.Mockito.never()).updatePwd(anyLong(), anyString());
    }

    // ============ 查询 ============

    @Test
    @DisplayName("findByUserName 存在/缺失")
    void findByUserName() {
        when(userDAO.findByUserName("alice")).thenReturn(userDO);
        when(userDAO.findByUserName("ghost")).thenReturn(null);

        assertEquals("alice", service.findByUserName("alice").getUserName());
        assertNull(service.findByUserName("ghost"));
    }

    @Test
    @DisplayName("findAll 转换列表")
    void findAll_converts() {
        when(userDAO.findAll()).thenReturn(List.of(userDO));
        assertEquals(1, service.findAll().size());
    }

    @Test
    @DisplayName("findById 存在/缺失")
    void findById() {
        when(userDAO.findById(1L)).thenReturn(userDO);
        when(userDAO.findById(99L)).thenReturn(null);

        assertEquals("alice", service.findById(1L).getUserName());
        assertNull(service.findById(99L));
    }

    @Test
    @DisplayName("findByIds 空入参不查库")
    void findByIds_emptyInput() {
        assertTrue(service.findByIds(null).isEmpty());
        assertTrue(service.findByIds(new ArrayList<>()).isEmpty());
        verify(userDAO, org.mockito.Mockito.never()).findByIds(anyList());
    }

    @Test
    @DisplayName("findByIds 非空转换")
    void findByIds_nonEmpty() {
        when(userDAO.findByIds(List.of(1L))).thenReturn(List.of(userDO));
        assertEquals(1, service.findByIds(List.of(1L)).size());
    }

    // ============ 计数与角色 ============

    @Test
    @DisplayName("countAll 透传")
    void countAll_delegates() {
        when(userDAO.countAll()).thenReturn(7L);
        assertEquals(7L, service.countAll());
    }

    @Test
    @DisplayName("updateRole 透传成败")
    void updateRole_delegates() {
        when(userDAO.updateRole(1L, "MERCHANT")).thenReturn(1);
        assertTrue(service.updateRole(1L, "MERCHANT"));
        when(userDAO.updateRole(1L, "MERCHANT")).thenReturn(0);
        assertFalse(service.updateRole(1L, "MERCHANT"));
    }

    // ============ updateProfile ============

    @Test
    @DisplayName("updateProfile 无修改内容拒绝")
    void updateProfile_nothingToChange_throws() {
        assertThrows(RuntimeException.class, () -> service.updateProfile(1L, "", ""),
                "没有需要修改的内容");
    }

    @Test
    @DisplayName("updateProfile 昵称超长拒绝")
    void updateProfile_nickTooLong_throws() {
        assertThrows(RuntimeException.class,
                () -> service.updateProfile(1L, "一二三四五六七八九十一二三四五六七八九十一二", null));
    }

    @Test
    @DisplayName("updateProfile 成功更新并脱敏密码")
    void updateProfile_success_masksPwd() {
        when(userDAO.updateProfile(1L, "新昵称", null)).thenReturn(1);
        when(userDAO.findById(1L)).thenReturn(userDO);

        User updated = service.updateProfile(1L, "新昵称", null);

        assertNotNull(updated);
        assertNull(updated.getPwd());
        verify(userDAO).updateProfile(1L, "新昵称", null);
    }
}
