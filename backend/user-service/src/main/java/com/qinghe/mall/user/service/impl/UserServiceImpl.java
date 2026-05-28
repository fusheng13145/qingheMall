package com.qinghe.mall.user.service.impl;

import com.qinghe.mall.common.util.JwtUtil;
import com.qinghe.mall.user.dto.LoginDTO;
import com.qinghe.mall.user.dto.RegisterDTO;
import com.qinghe.mall.user.dto.UserInfoDTO;
import com.qinghe.mall.user.entity.User;
import com.qinghe.mall.user.mapper.UserMapper;
import com.qinghe.mall.user.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterDTO registerDTO) {
        // 校验验证码（实际项目中应连接Redis或短信服务验证）
        String cachedCode = redisTemplate.opsForValue().get("register:code:" + registerDTO.getPhone());
        if (!registerDTO.getCode().equals(cachedCode)) {
            throw new RuntimeException("验证码错误");
        }

        // 校验用户名是否已存在
        User existUser = userMapper.findByUsername(registerDTO.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setPhone(registerDTO.getPhone());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);

        // 删除验证码
        redisTemplate.delete("register:code:" + registerDTO.getPhone());
    }

    @Override
    public User login(LoginDTO loginDTO) {
        User user = userMapper.findByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }

        return user;
    }

    @Override
    public UserInfoDTO getUserInfo(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserInfoDTO userInfoDTO = new UserInfoDTO();
        BeanUtils.copyProperties(user, userInfoDTO);
        return userInfoDTO;
    }

    @Override
    public void updateUserInfo(Long userId, UserInfoDTO userInfoDTO) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (StringUtils.hasText(userInfoDTO.getNickname())) {
            user.setNickname(userInfoDTO.getNickname());
        }
        if (StringUtils.hasText(userInfoDTO.getEmail())) {
            user.setEmail(userInfoDTO.getEmail());
        }
        if (StringUtils.hasText(userInfoDTO.getPhone())) {
            user.setPhone(userInfoDTO.getPhone());
        }
        if (StringUtils.hasText(userInfoDTO.getAvatar())) {
            user.setAvatar(userInfoDTO.getAvatar());
        }
        if (userInfoDTO.getGender() != null) {
            user.setGender(userInfoDTO.getGender());
        }

        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }
}
