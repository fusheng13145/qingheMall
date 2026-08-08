package com.qinghe.mall.controller;

import com.qinghe.mall.model.Address;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.AddressService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 收货地址管理（M3-3 用户中心）。
 */
@RestController
@RequestMapping("/api/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    private Long requireUserId(HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return null;
        }
        return (Long) userIdObj;
    }

    @GetMapping("/list")
    public Result<List<Address>> list(HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(addressService.list(userId));
    }

    @GetMapping("/default")
    public Result<Address> defaultAddress(HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(addressService.findDefault(userId));
    }

    @PostMapping("/add")
    public Result<Address> add(@RequestBody Address address, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        // 业务异常由 GlobalExceptionHandler 统一转为 Result.fail
        return Result.success(addressService.add(userId, address));
    }

    @PostMapping("/update")
    public Result<Address> update(@RequestBody Address address, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(addressService.update(userId, address));
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam("id") Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        addressService.delete(userId, id);
        return Result.success();
    }

    @PostMapping("/setDefault")
    public Result<Void> setDefault(@RequestParam("id") Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        addressService.setDefault(userId, id);
        return Result.success();
    }
}
