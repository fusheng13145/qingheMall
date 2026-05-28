package com.qinghe.mall.user.service;

import com.qinghe.mall.user.dto.LoginDTO;
import com.qinghe.mall.user.dto.RegisterDTO;
import com.qinghe.mall.user.dto.UserInfoDTO;
import com.qinghe.mall.user.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    void register(RegisterDTO registerDTO);

    /**
     * 用户登录
     */
    User login(LoginDTO loginDTO);

    /**
     * 获取用户信息
     */
    UserInfoDTO getUserInfo(Long userId);

    /**
     * 更新用户信息
     */
    void updateUserInfo(Long userId, UserInfoDTO userInfoDTO);

    /**
     * 根据ID查询用户
     */
    User findById(Long id);
}
