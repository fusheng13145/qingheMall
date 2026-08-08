package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.UserDAO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.model.User;
import com.qinghe.mall.util.CommonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 用户注册/登录 单元测试（M3-5）
 * 覆盖：BCrypt 密码哈希、重名注册、登录校验、存量 MD5 兼容与自动升级。
 */
class UserServiceImplTest {

    private static final String SALT = "_qh2050";

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private UserDO mockUserDO(Long id, String userName, String pwd, String role) {
        UserDO userDO = new UserDO();
        userDO.setId(id);
        userDO.setUserName(userName);
        userDO.setNickName(userName);
        userDO.setPwd(pwd);
        userDO.setRole(role);
        return userDO;
    }

    @Test
    void register_shouldEncodeBcryptAndSetUserRole() {
        when(userDAO.findByUserName("zhangsan")).thenReturn(null);
        when(userDAO.insert(any(UserDO.class))).thenAnswer(inv -> {
            UserDO arg = inv.getArgument(0);
            arg.setId(100L);
            return 1;
        });

        User user = userService.register("zhangsan", "123456");

        assertNotNull(user);
        assertEquals("zhangsan", user.getUserName());
        assertEquals("USER", user.getRole());
        // 密码应以 BCrypt 哈希存储（$2 开头），且能通过 BCrypt 校验
        verify(userDAO).insert(any(UserDO.class));
        // 从 insert 捕获的参数验证哈希
        org.mockito.ArgumentCaptor<UserDO> captor = org.mockito.ArgumentCaptor.forClass(UserDO.class);
        verify(userDAO).insert(captor.capture());
        String storedPwd = captor.getValue().getPwd();
        assertTrue(storedPwd.startsWith("$2"), "新用户密码应为 BCrypt 哈希");
        assertTrue(new BCryptPasswordEncoder().matches("123456", storedPwd));
    }

    @Test
    void register_duplicateName_shouldThrow() {
        when(userDAO.findByUserName("zhangsan")).thenReturn(mockUserDO(1L, "zhangsan", "x", "USER"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.register("zhangsan", "123456"));
        assertEquals("用户名已存在", ex.getMessage());
        verify(userDAO, never()).insert(any());
    }

    @Test
    void register_blankInput_shouldThrow() {
        assertThrows(RuntimeException.class, () -> userService.register("", "123456"));
        assertThrows(RuntimeException.class, () -> userService.register("zhangsan", ""));
    }

    @Test
    void login_shouldSucceedWithBcryptPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        when(userDAO.findByUserName("zhangsan"))
                .thenReturn(mockUserDO(1L, "zhangsan", encoder.encode("123456"), "USER"));

        User user = userService.login("zhangsan", "123456");

        assertNotNull(user);
        assertEquals(1L, user.getId());
        // BCrypt 用户不应触发密码升级
        verify(userDAO, never()).updatePwd(any(), anyString());
    }

    @Test
    void login_wrongPassword_shouldThrow() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        when(userDAO.findByUserName("zhangsan"))
                .thenReturn(mockUserDO(1L, "zhangsan", encoder.encode("123456"), "USER"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.login("zhangsan", "wrong"));
        assertEquals("密码错误", ex.getMessage());
    }

    @Test
    void login_userNotExist_shouldThrow() {
        when(userDAO.findByUserName("nobody")).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.login("nobody", "123456"));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void login_legacyMd5_shouldMatchAndUpgradeToBcrypt() {
        // 存量加盐 MD5 密码
        String md5Pwd = CommonUtils.md5("123456" + SALT);
        when(userDAO.findByUserName("legacy"))
                .thenReturn(mockUserDO(9L, "legacy", md5Pwd, "USER"));
        when(userDAO.updatePwd(9L, "anything")).thenReturn(1);

        User user = userService.login("legacy", "123456");

        assertNotNull(user);
        // MD5 用户登录成功应自动升级为 BCrypt
        org.mockito.ArgumentCaptor<String> pwdCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(userDAO).updatePwd(org.mockito.ArgumentMatchers.eq(9L), pwdCaptor.capture());
        assertTrue(pwdCaptor.getValue().startsWith("$2"), "升级后密码应为 BCrypt");
    }

    @Test
    void login_legacyMd5_wrongPassword_shouldThrow() {
        String md5Pwd = CommonUtils.md5("123456" + SALT);
        when(userDAO.findByUserName("legacy")).thenReturn(mockUserDO(9L, "legacy", md5Pwd, "USER"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.login("legacy", "wrong"));
        assertEquals("密码错误", ex.getMessage());
        verify(userDAO, never()).updatePwd(any(), anyString());
    }
}
