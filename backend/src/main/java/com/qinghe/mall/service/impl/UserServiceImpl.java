package com.qinghe.mall.service.impl;

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
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private static final String PWD_SALT = "_qh2050";

    @Autowired
    private UserDAO userDAO;

    @Override
    public User register(String userName, String pwd) {
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(pwd)) {
            throw new RuntimeException("用户名或密码不能为空");
        }
        UserDO existUser = userDAO.findByUserName(userName);
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        UserDO userDO = new UserDO();
        userDO.setUserName(userName);
        userDO.setPwd(CommonUtils.md5(pwd + PWD_SALT));
        userDO.setNickName(userName);
        userDO.setAvatar("");
        userDO.setRole("USER");
        userDO.setGmtCreated(LocalDateTime.now());
        userDO.setGmtModified(LocalDateTime.now());
        userDAO.insert(userDO);
        return userDO.convertToModel();
    }

    @Override
    public User login(String userName, String pwd) {
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(pwd)) {
            throw new RuntimeException("用户名或密码不能为空");
        }
        UserDO userDO = userDAO.findByUserName(userName);
        if (userDO == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!userDO.getPwd().equals(CommonUtils.md5(pwd + PWD_SALT))) {
            throw new RuntimeException("密码错误");
        }
        return userDO.convertToModel();
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
    public User findById(Long id) {
        UserDO userDO = userDAO.findById(id);
        if (userDO == null) {
            return null;
        }
        return userDO.convertToModel();
    }

    @Override
    public boolean updateRole(Long id, String role) {
        return userDAO.updateRole(id, role) > 0;
    }
}
