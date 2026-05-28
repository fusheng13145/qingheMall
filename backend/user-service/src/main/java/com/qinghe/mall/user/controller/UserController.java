package com.qinghe.mall.user.controller;

import com.qinghe.mall.common.R;
import com.qinghe.mall.common.util.JwtUtil;
import com.qinghe.mall.common.util.UserContext;
import com.qinghe.mall.user.dto.*;
import com.qinghe.mall.user.entity.User;
import com.qinghe.mall.user.service.AddressService;
import com.qinghe.mall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AddressService addressService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public R<Void> register(@RequestBody @Validated RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return R.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody @Validated LoginDTO loginDTO) {
        User user = userService.login(loginDTO);
        String token = JwtUtil.generateToken(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());

        return R.success(data);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public R<UserInfoDTO> getUserInfo(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            return R.error(401, "未登录");
        }

        token = token.substring(7);
        Long userId = JwtUtil.parseToken(token);
        if (userId == null) {
            return R.error(401, "token无效");
        }

        UserContext.setUserId(userId);
        try {
            UserInfoDTO userInfo = userService.getUserInfo(userId);
            return R.success(userInfo);
        } finally {
            UserContext.remove();
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public R<Void> updateUserInfo(@RequestBody UserInfoDTO userInfoDTO, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            return R.error(401, "未登录");
        }

        token = token.substring(7);
        Long userId = JwtUtil.parseToken(token);
        if (userId == null) {
            return R.error(401, "token无效");
        }

        UserContext.setUserId(userId);
        try {
            userService.updateUserInfo(userId, userInfoDTO);
            return R.success();
        } finally {
            UserContext.remove();
        }
    }
}
