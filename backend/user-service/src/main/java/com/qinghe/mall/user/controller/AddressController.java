package com.qinghe.mall.user.controller;

import com.qinghe.mall.common.R;
import com.qinghe.mall.common.util.JwtUtil;
import com.qinghe.mall.common.util.UserContext;
import com.qinghe.mall.user.dto.AddressDTO;
import com.qinghe.mall.user.dto.AddressVO;
import com.qinghe.mall.user.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 地址控制器
 */
@RestController
@RequestMapping("/user/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    /**
     * 获取收货地址列表
     */
    @GetMapping("/list")
    public R<List<AddressVO>> getAddressList(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return R.error(401, "未登录");
        }

        UserContext.setUserId(userId);
        try {
            List<AddressVO> list = addressService.getAddressList(userId);
            return R.success(list);
        } finally {
            UserContext.remove();
        }
    }

    /**
     * 添加收货地址
     */
    @PostMapping("/add")
    public R<Void> addAddress(@RequestBody @Validated AddressDTO addressDTO, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return R.error(401, "未登录");
        }

        UserContext.setUserId(userId);
        try {
            addressService.addAddress(userId, addressDTO);
            return R.success();
        } finally {
            UserContext.remove();
        }
    }

    /**
     * 更新收货地址
     */
    @PutMapping("/update")
    public R<Void> updateAddress(@RequestBody @Validated AddressDTO addressDTO, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return R.error(401, "未登录");
        }

        UserContext.setUserId(userId);
        try {
            addressService.updateAddress(userId, addressDTO);
            return R.success();
        } finally {
            UserContext.remove();
        }
    }

    /**
     * 删除收货地址
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteAddress(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return R.error(401, "未登录");
        }

        UserContext.setUserId(userId);
        try {
            addressService.deleteAddress(userId, id);
            return R.success();
        } finally {
            UserContext.remove();
        }
    }

    /**
     * 设置默认地址
     */
    @PutMapping("/default/{id}")
    public R<Void> setDefaultAddress(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return R.error(401, "未登录");
        }

        UserContext.setUserId(userId);
        try {
            addressService.setDefaultAddress(userId, id);
            return R.success();
        } finally {
            UserContext.remove();
        }
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            return null;
        }

        token = token.substring(7);
        return JwtUtil.parseToken(token);
    }
}
