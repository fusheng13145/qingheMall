package com.qinghe.mall.service.impl;

import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.dao.UserDAO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.UserService;
import com.qinghe.mall.util.CommonUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    /**
     * 存量用户密码加盐 MD5 的盐值，仅用于登录兼容过渡（MD5 库哈希）。
     * 新注册用户一律使用 BCrypt，存量用户首次登录成功后自动升级为 BCrypt。
     */
    private static final String PWD_SALT = "_qh2050";

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private UserDAO userDAO;

    /**
     * MD5 兼容登录开关（架构级遗留项）：
     * true=允许存量 MD5 用户登录（登录成功自动升级 BCrypt，过渡期默认）；
     * false=拒绝 MD5 哈希登录，仅接受 BCrypt（确认存量库无 MD5 用户后由生产配置关闭）。
     */
    @org.springframework.beans.factory.annotation.Value("${app.security.allow-md5-login:true}")
    private boolean allowMd5Login;

    @Override
    public User register(String userName, String pwd) {
        return register(userName, pwd, UserDO.ROLE_USER);
    }

    @Override
    public User register(String userName, String pwd, String role) {
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(pwd)) {
            throw new BusinessException("用户名或密码不能为空");
        }
        if (!UserDO.ROLE_USER.equals(role) && !UserDO.ROLE_MERCHANT.equals(role)) {
            throw new BusinessException("非法的注册角色");
        }
        UserDO existUser = userDAO.findByUserName(userName);
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }
        UserDO userDO = new UserDO();
        userDO.setUserName(userName);
        // 新用户使用 BCrypt 哈希，不再使用 MD5
        userDO.setPwd(PASSWORD_ENCODER.encode(pwd));
        userDO.setNickName(userName);
        userDO.setAvatar("");
        userDO.setRole(role);
        userDO.setGmtCreated(LocalDateTime.now());
        userDO.setGmtModified(LocalDateTime.now());
        userDAO.insert(userDO);
        return userDO.convertToModel();
    }

    @Override
    public User login(String userName, String pwd) {
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(pwd)) {
            throw new BusinessException("用户名或密码不能为空");
        }
        UserDO userDO = userDAO.findByUserName(userName);
        if (userDO == null) {
            throw new BusinessException("用户不存在");
        }
        if (!matchesPassword(pwd, userDO.getPwd())) {
            throw new BusinessException("密码错误");
        }
        // 存量 MD5 用户登录成功后，立即升级为 BCrypt 哈希，完成安全过渡
        if (!userDO.getPwd().startsWith("$2")) {
            userDAO.updatePwd(userDO.getId(), PASSWORD_ENCODER.encode(pwd));
        }
        return userDO.convertToModel();
    }

    /**
     * 密码校验：优先 BCrypt（新用户），存量 MD5 哈希走兼容分支。
     * 兼容分支校验通过后，立即将密码升级为 BCrypt 哈希，完成安全过渡。
     */
    private boolean matchesPassword(String rawPwd, String storedPwd) {
        if (storedPwd == null || storedPwd.isEmpty()) {
            return false;
        }
        if (storedPwd.startsWith("$2")) {
            return PASSWORD_ENCODER.matches(rawPwd, storedPwd);
        }
        // 存量 MD5 兼容已由开关关闭时，直接拒绝（仅接受 BCrypt）
        if (!allowMd5Login) {
            return false;
        }
        // 存量 MD5（加盐）兼容
        if (storedPwd.equalsIgnoreCase(CommonUtils.md5(rawPwd + PWD_SALT))) {
            return true;
        }
        // 兼容无盐 MD5（历史数据兜底）
        return storedPwd.equalsIgnoreCase(CommonUtils.md5(rawPwd));
    }

    @Override
    public User findByUserName(String userName) {
        UserDO userDO = userDAO.findByUserName(userName);
        if (userDO == null) {
            return null;
        }
        return userDO.convertToModel();
    }

    @Override
    public List<User> findAll() {
        List<UserDO> userDOs = userDAO.findAll();
        List<User> users = new ArrayList<>();
        for (UserDO userDO : userDOs) {
            users.add(userDO.convertToModel());
        }
        return users;
    }

    @Override
    public com.qinghe.mall.model.Paging<User> findAdminPage(Integer pagination, Integer pageSize) {
        if (pagination == null || pagination < 1) {
            pagination = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 50) {
            pageSize = 20;
        }
        // P1-11：管理端用户列表分页，消除全表捞取
        com.github.pagehelper.Page<UserDO> page =
                com.github.pagehelper.PageHelper.startPage(pagination, pageSize)
                        .doSelectPage(() -> userDAO.findAll());

        com.qinghe.mall.model.Paging<User> paging = new com.qinghe.mall.model.Paging<>();
        paging.setPageNum(pagination);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());

        List<User> users = new ArrayList<>();
        for (UserDO userDO : page.getResult()) {
            users.add(userDO.convertToModel());
        }
        paging.setData(users);
        return paging;
    }

    @Override
    public User findById(Long id) {
        UserDO userDO = userDAO.findById(id);
        if (userDO == null) {
            return null;
        }
        return userDO.convertToModel();
    }

    @Override
    public List<User> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<UserDO> userDOs = userDAO.findByIds(ids);
        List<User> users = new ArrayList<>();
        for (UserDO userDO : userDOs) {
            users.add(userDO.convertToModel());
        }
        return users;
    }

    @Override
    public long countAll() {
        return userDAO.countAll();
    }

    @Override
    public boolean updateRole(Long id, String role) {
        return userDAO.updateRole(id, role) > 0;
    }

    @Override
    public User updateProfile(Long id, String nickName, String avatar) {
        if (StringUtils.isBlank(nickName) && StringUtils.isBlank(avatar)) {
            throw new BusinessException("没有需要修改的内容");
        }
        if (StringUtils.isNotBlank(nickName) && nickName.length() > 20) {
            throw new BusinessException("昵称不能超过 20 个字符");
        }
        userDAO.updateProfile(id, nickName, avatar);
        User user = findById(id);
        if (user != null) {
            user.setPwd(null);
        }
        return user;
    }
}
